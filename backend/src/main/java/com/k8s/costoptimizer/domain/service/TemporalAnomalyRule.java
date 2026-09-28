package com.k8s.costoptimizer.domain.service;

import com.k8s.costoptimizer.domain.model.*;
import com.k8s.costoptimizer.domain.model.TemporalProfile.WorkloadPattern;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * <h1>Temporal Anomaly Detection Rule — THE NOVELTY</h1>
 *
 * <h2>The Problem with Static-Threshold Optimization</h2>
 * Every existing rule in this engine (UnderutilizedPodRule, OverprovisionedDeploymentRule,
 * IdleNodeRule, MissingRequestsOrLimitsRule) evaluates a SINGLE point-in-time metric
 * snapshot against a hardcoded threshold. For example, UnderutilizedPodRule triggers at
 * 25% CPU utilization — if the scan happens to run when a cron job is sleeping between
 * its hourly bursts, the system recommends downsizing resources that the workload
 * actually NEEDS during its burst window.
 *
 * <h2>Real-World Consequences</h2>
 * <ul>
 *   <li><b>False Positive → OOMKill</b>: Memory recommendation based on 2 AM snapshot
 *       leads to OOMKill during 9 AM traffic spike</li>
 *   <li><b>False Positive → CPU Throttling</b>: CPU rightsize recommendation based on
 *       weekend snapshot causes latency spikes on Monday morning</li>
 *   <li><b>False Negative</b>: Truly idle resources are obscured by a single spike</li>
 * </ul>
 *
 * <h2>The Solution: Percentile-Based Temporal Analysis</h2>
 * This rule consumes a sliding window of historical metric snapshots (maintained by
 * {@link MetricHistoryStore}) and computes statistical profiles including:
 * <ul>
 *   <li><b>P95 Utilization</b>: Used as the "safe" value for rightsizing (95th percentile
 *       means the recommendation survives 95% of observed workload behavior)</li>
 *   <li><b>Burstiness Coefficient</b>: Peak-to-mean ratio. Bursty workloads (coefficient > 3x)
 *       get a safety multiplier or are excluded entirely</li>
 *   <li><b>Workload Pattern Classification</b>: STABLE, DIURNAL, BURSTY, GROWING, IDLE —
 *       each pattern gets a different optimization strategy</li>
 * </ul>
 *
 * <h2>Research Basis</h2>
 * This approach is inspired by Google Autopilot's percentile-based vertical scaling
 * (Autopilot, EuroSys'20) and the Kubernetes VPA (Vertical Pod Autoscaler) exponential
 * histogram algorithm, but simplified for a FinOps advisory tool rather than an
 * auto-scaling controller.
 */
public class TemporalAnomalyRule implements RecommendationRule {

    private static final double HOURS_PER_MONTH = 730.0;

    /**
     * P95 utilization thresholds: only recommend downsizing if the 95th percentile
     * usage is below these thresholds (i.e., the workload is underutilized even
     * accounting for spikes).
     */
    private static final double CPU_P95_THRESHOLD = 0.35;
    private static final double MEM_P95_THRESHOLD = 0.40;

    /**
     * Safety headroom multiplier applied on top of P95 usage when computing
     * the recommended resource request. Higher for bursty workloads.
     */
    private static final double STABLE_HEADROOM = 1.3;   // 30% headroom for stable workloads
    private static final double BURSTY_HEADROOM = 1.8;   // 80% headroom for bursty workloads
    private static final double DIURNAL_HEADROOM = 1.5;  // 50% headroom for diurnal workloads

    private final MetricHistoryStore historyStore;

    public TemporalAnomalyRule(MetricHistoryStore historyStore) {
        this.historyStore = historyStore;
    }

    @Override
    public List<Recommendation> evaluate(List<PodResourceInfo> pods, List<NodeResourceInfo> nodes, double cpuRate, double memoryRate) {
        List<Recommendation> recommendations = new ArrayList<>();

        for (PodResourceInfo pod : pods) {
            // Skip system namespaces
            if ("kube-system".equals(pod.getNamespace()) || "kubernetes-dashboard".equals(pod.getNamespace())) {
                continue;
            }

            for (ContainerMetric container : pod.getContainers()) {
                String resourceKey = pod.getNamespace() + "/" + pod.getName() + "/" + container.getName();
                Optional<TemporalProfile> profileOpt = historyStore.computeProfile(resourceKey);

                if (profileOpt.isEmpty()) {
                    continue; // Insufficient history — defer to static rules
                }

                TemporalProfile profile = profileOpt.get();

                // Skip GROWING workloads — they need MORE resources, not less
                if (profile.getPattern() == WorkloadPattern.GROWING) {
                    continue;
                }

                // Skip highly bursty workloads (burstiness > 5x) — too risky to rightsize
                if (profile.getCpuBurstiness() > 5.0 || profile.getMemBurstiness() > 5.0) {
                    continue;
                }

                double headroom = getHeadroomMultiplier(profile.getPattern());

                // --- CPU Temporal Analysis ---
                double cpuReq = profile.getCpuRequest();
                if (cpuReq > 0.05) {
                    double cpuP95Ratio = profile.getCpuP95() / cpuReq;

                    if (cpuP95Ratio < CPU_P95_THRESHOLD) {
                        // Even at the 95th percentile, usage is well below request
                        double recommendedCpu = Math.max(profile.getCpuP95() * headroom, 0.01);
                        double cpuSavings = cpuReq - recommendedCpu;
                        double monthlySavings = cpuSavings * cpuRate * HOURS_PER_MONTH;

                        if (monthlySavings > 0.5 && cpuSavings > 0) {
                            recommendations.add(Recommendation.builder()
                                    .namespace(pod.getNamespace())
                                    .resourceName(pod.getName() + "/" + container.getName())
                                    .resourceType(K8sResourceType.POD)
                                    .type(RecommendationType.TEMPORAL_CPU_RIGHTSIZE)
                                    .severity(determineSeverity(monthlySavings, profile))
                                    .currentCpuRequest(cpuReq)
                                    .recommendedCpuRequest(recommendedCpu)
                                    .currentMemoryRequestGb(profile.getMemRequestGb())
                                    .recommendedMemoryRequestGb(profile.getMemRequestGb())
                                    .estimatedMonthlySavings(monthlySavings)
                                    .status(RecommendationStatus.ACTIVE)
                                    .details(buildCpuDetail(profile, recommendedCpu, headroom))
                                    .createdAt(LocalDateTime.now())
                                    .build());
                        }
                    }
                }

                // --- Memory Temporal Analysis ---
                double memReq = profile.getMemRequestGb();
                if (memReq > 0.1) {
                    double memP95Ratio = profile.getMemP95() / memReq;

                    if (memP95Ratio < MEM_P95_THRESHOLD) {
                        double recommendedMem = Math.max(profile.getMemP95() * headroom, 0.032);
                        double memSavings = memReq - recommendedMem;
                        double monthlySavings = memSavings * memoryRate * HOURS_PER_MONTH;

                        if (monthlySavings > 0.5 && memSavings > 0) {
                            recommendations.add(Recommendation.builder()
                                    .namespace(pod.getNamespace())
                                    .resourceName(pod.getName() + "/" + container.getName())
                                    .resourceType(K8sResourceType.POD)
                                    .type(RecommendationType.TEMPORAL_MEM_RIGHTSIZE)
                                    .severity(determineSeverity(monthlySavings, profile))
                                    .currentCpuRequest(profile.getCpuRequest())
                                    .recommendedCpuRequest(profile.getCpuRequest())
                                    .currentMemoryRequestGb(memReq)
                                    .recommendedMemoryRequestGb(recommendedMem)
                                    .estimatedMonthlySavings(monthlySavings)
                                    .status(RecommendationStatus.ACTIVE)
                                    .details(buildMemDetail(profile, recommendedMem, headroom))
                                    .createdAt(LocalDateTime.now())
                                    .build());
                        }
                    }
                }
            }
        }

        return recommendations;
    }

    /**
     * Selects the safety headroom multiplier based on classified workload pattern.
     */
    private double getHeadroomMultiplier(WorkloadPattern pattern) {
        return switch (pattern) {
            case BURSTY -> BURSTY_HEADROOM;
            case DIURNAL -> DIURNAL_HEADROOM;
            default -> STABLE_HEADROOM;
        };
    }

    /**
     * Determines recommendation severity considering both savings magnitude and confidence.
     * Temporal recommendations with more snapshots are higher confidence → can be higher severity.
     */
    private Severity determineSeverity(double monthlySavings, TemporalProfile profile) {
        if (profile.getSnapshotCount() >= 24 && monthlySavings > 25.0 &&
                profile.getPattern() == WorkloadPattern.STABLE) {
            return Severity.CRITICAL; // High confidence + high savings
        }
        if (monthlySavings > 15.0) return Severity.HIGH;
        if (monthlySavings > 5.0) return Severity.MEDIUM;
        return Severity.LOW;
    }

    private String buildCpuDetail(TemporalProfile profile, double recommended, double headroom) {
        return String.format(
                "[TEMPORAL] CPU P95 usage: %.3f cores (%.1f%% of %.2f req). " +
                "Pattern: %s (burstiness: %.1fx). " +
                "Recommended: %.3f cores (P95 × %.1f headroom). " +
                "Based on %d snapshots over 24h window.",
                profile.getCpuP95(),
                (profile.getCpuP95() / profile.getCpuRequest()) * 100,
                profile.getCpuRequest(),
                profile.getPattern().name(),
                profile.getCpuBurstiness(),
                recommended,
                headroom,
                profile.getSnapshotCount()
        );
    }

    private String buildMemDetail(TemporalProfile profile, double recommended, double headroom) {
        return String.format(
                "[TEMPORAL] Memory P95 usage: %.3f GB (%.1f%% of %.2f GB req). " +
                "Pattern: %s (burstiness: %.1fx). " +
                "Recommended: %.3f GB (P95 × %.1f headroom). " +
                "Based on %d snapshots over 24h window.",
                profile.getMemP95(),
                (profile.getMemP95() / profile.getMemRequestGb()) * 100,
                profile.getMemRequestGb(),
                profile.getPattern().name(),
                profile.getMemBurstiness(),
                recommended,
                headroom,
                profile.getSnapshotCount()
        );
    }
}
