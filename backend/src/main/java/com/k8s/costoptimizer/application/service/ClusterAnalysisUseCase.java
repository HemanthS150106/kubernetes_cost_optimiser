package com.k8s.costoptimizer.application.service;

import com.k8s.costoptimizer.application.dto.ClusterOverviewResponse;
import com.k8s.costoptimizer.domain.model.*;
import com.k8s.costoptimizer.domain.repository.ClusterRepository;
import com.k8s.costoptimizer.domain.repository.RecommendationRepository;
import com.k8s.costoptimizer.domain.service.CostOptimizationEngine;
import com.k8s.costoptimizer.domain.service.MetricHistoryStore;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Coordinating service (Use Case) that manages the cluster analysis workflow,
 * computes overall cost metrics, and aggregates recommendations.
 *
 * <h2>Temporal Analysis Integration</h2>
 * On each scan interval, this use case now collects metric snapshots from all pods
 * and feeds them into the {@link MetricHistoryStore} maintained by the
 * {@link CostOptimizationEngine}. This enables the {@link com.k8s.costoptimizer.domain.service.TemporalAnomalyRule}
 * to build percentile-based temporal profiles and produce spike-aware recommendations.
 */
@Service
@RequiredArgsConstructor
public class ClusterAnalysisUseCase {

    private static final Logger log = LoggerFactory.getLogger(ClusterAnalysisUseCase.class);

    private final ClusterRepository clusterRepository;
    private final RecommendationRepository recommendationRepository;
    private final CostOptimizationEngine optimizationEngine = new CostOptimizationEngine();

    @Value("${app.pricing.cpu-rate-per-hour:0.0475}")
    private double cpuRate;

    @Value("${app.pricing.memory-rate-per-gb-hour:0.0063}")
    private double memoryRate;

    /**
     * Executes cluster optimization analysis every 5 minutes.
     */
    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void runScheduledAnalysis() {
        log.info("Starting scheduled Kubernetes cluster cost optimization scan...");
        analyzeAndSaveRecommendations();
        log.info("Scheduled cost optimization scan completed successfully.");
    }

    /**
     * Executes the analysis engine on current cluster data and saves/merges recommendations.
     * Now also records metric snapshots into the temporal history store for each scan.
     */
    @Transactional
    public List<Recommendation> analyzeAndSaveRecommendations() {
        List<PodResourceInfo> pods = clusterRepository.getPods();
        List<NodeResourceInfo> nodes = clusterRepository.getNodes();

        // --- NOVELTY: Record metric snapshots for temporal analysis ---
        recordMetricSnapshots(pods);

        List<Recommendation> newRecommendations = optimizationEngine.generateRecommendations(
                pods, nodes, cpuRate, memoryRate
        );

        // Fetch existing recommendations to avoid duplicate creation
        List<Recommendation> existingRecommendations = recommendationRepository.findAll();

        for (Recommendation newRec : newRecommendations) {
            Optional<Recommendation> existingOpt = existingRecommendations.stream()
                    .filter(e -> e.getResourceName().equals(newRec.getResourceName())
                            && e.getNamespace().equals(newRec.getNamespace())
                            && e.getType() == newRec.getType()
                            && e.getStatus() == RecommendationStatus.ACTIVE)
                    .findFirst();

            if (existingOpt.isPresent()) {
                // Update savings and timestamp
                Recommendation existing = existingOpt.get();
                existing.setEstimatedMonthlySavings(newRec.getEstimatedMonthlySavings());
                existing.setDetails(newRec.getDetails());
                existing.setUpdatedAt(LocalDateTime.now());
                recommendationRepository.save(existing);
            } else {
                // Insert new recommendation
                newRec.setClusterId("default-cluster");
                recommendationRepository.save(newRec);
            }
        }

        // Complete/Delete recommendations that are no longer active
        for (Recommendation existing : existingRecommendations) {
            if (existing.getStatus() == RecommendationStatus.ACTIVE) {
                boolean stillActive = newRecommendations.stream()
                        .anyMatch(n -> n.getResourceName().equals(existing.getResourceName())
                                && n.getNamespace().equals(existing.getNamespace())
                                && n.getType() == existing.getType());

                if (!stillActive) {
                    existing.setStatus(RecommendationStatus.IMPLEMENTED);
                    existing.setUpdatedAt(LocalDateTime.now());
                    recommendationRepository.save(existing);
                }
            }
        }

        return recommendationRepository.findAll();
    }

    /**
     * Records per-container metric snapshots from the current scan into the temporal history store.
     * Each container in each pod produces one MetricSnapshot with the current timestamp.
     */
    private void recordMetricSnapshots(List<PodResourceInfo> pods) {
        List<MetricSnapshot> snapshots = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (PodResourceInfo pod : pods) {
            if ("kube-system".equals(pod.getNamespace()) || "kubernetes-dashboard".equals(pod.getNamespace())) {
                continue;
            }
            for (ContainerMetric container : pod.getContainers()) {
                String resourceKey = pod.getNamespace() + "/" + pod.getName() + "/" + container.getName();
                snapshots.add(MetricSnapshot.builder()
                        .resourceKey(resourceKey)
                        .timestamp(now)
                        .cpuUsage(container.getCpuUsage() != null ? container.getCpuUsage() : 0.0)
                        .memoryUsageGb(container.getMemoryUsageGb() != null ? container.getMemoryUsageGb() : 0.0)
                        .cpuRequest(container.getCpuRequest() != null ? container.getCpuRequest() : 0.0)
                        .memoryRequestGb(container.getMemoryRequestGb() != null ? container.getMemoryRequestGb() : 0.0)
                        .build());
            }
        }

        if (!snapshots.isEmpty()) {
            optimizationEngine.getMetricHistoryStore().recordSnapshots(snapshots);
            log.info("Recorded {} metric snapshots into temporal history store. Tracked resources: {}",
                    snapshots.size(), optimizationEngine.getMetricHistoryStore().getTrackedResourceCount());
        }
    }

    /**
     * Exposes temporal profiles for the frontend visualization layer.
     */
    public List<TemporalProfile> getTemporalProfiles() {
        return optimizationEngine.getMetricHistoryStore().getAllProfiles();
    }

    /**
     * Returns the number of resources currently tracked in the temporal history.
     */
    public int getTrackedResourceCount() {
        return optimizationEngine.getMetricHistoryStore().getTrackedResourceCount();
    }

    /**
     * Compiles full cluster capacity, requests, limits and active cost savings.
     */
    public ClusterOverviewResponse getClusterOverview() {
        List<NodeResourceInfo> nodes = clusterRepository.getNodes();
        List<PodResourceInfo> pods = clusterRepository.getPods();
        List<String> namespaces = clusterRepository.getNamespaces();

        int totalNodes = nodes.size();
        int totalPods = pods.size();
        int totalNamespaces = namespaces.size();

        double totalCpuCap = nodes.stream().mapToDouble(n -> n.getCpuAllocatable() != null ? n.getCpuAllocatable() : 0.0).sum();
        double totalCpuUse = nodes.stream().mapToDouble(n -> n.getCpuUsage() != null ? n.getCpuUsage() : 0.0).sum();
        double totalMemCap = nodes.stream().mapToDouble(n -> n.getMemoryAllocatableGb() != null ? n.getMemoryAllocatableGb() : 0.0).sum();
        double totalMemUse = nodes.stream().mapToDouble(n -> n.getMemoryUsageGb() != null ? n.getMemoryUsageGb() : 0.0).sum();

        double totalCpuReq = pods.stream().mapToDouble(PodResourceInfo::getTotalCpuRequest).sum();
        double totalCpuLim = pods.stream().mapToDouble(PodResourceInfo::getTotalCpuLimit).sum();
        double totalMemReq = pods.stream().mapToDouble(PodResourceInfo::getTotalMemoryRequestGb).sum();
        double totalMemLim = pods.stream().mapToDouble(PodResourceInfo::getTotalMemoryLimitGb).sum();

        // Node monthly baseline cost
        double currentMonthlyCost = nodes.stream()
                .mapToDouble(n -> n.getEstimatedMonthlyCost() != null ? n.getEstimatedMonthlyCost() : 0.0)
                .sum();

        // Calculate potential savings based on active ACTIVE recommendations
        double potentialMonthlySavings = recommendationRepository.findByStatus(RecommendationStatus.ACTIVE)
                .stream()
                .mapToDouble(Recommendation::getEstimatedMonthlySavings)
                .sum();

        // Optimized monthly cost
        double optimizedMonthlyCost = Math.max(0.0, currentMonthlyCost - potentialMonthlySavings);

        double cpuUtilPct = totalCpuCap > 0 ? (totalCpuUse / totalCpuCap) * 100 : 0.0;
        double memUtilPct = totalMemCap > 0 ? (totalMemUse / totalMemCap) * 100 : 0.0;

        return ClusterOverviewResponse.builder()
                .totalNodes(totalNodes)
                .totalPods(totalPods)
                .totalNamespaces(totalNamespaces)
                .totalDeployments(clusterRepository.getDeploymentCount())
                .totalDaemonSets(clusterRepository.getDaemonSetCount())
                .totalStatefulSets(clusterRepository.getStatefulSetCount())
                .totalServices(clusterRepository.getServiceCount())
                .totalCpuCapacity(totalCpuCap)
                .totalCpuRequest(totalCpuReq)
                .totalCpuLimit(totalCpuLim)
                .totalCpuUsage(totalCpuUse)
                .cpuUtilizationPercentage(cpuUtilPct)
                .totalMemoryCapacityGb(totalMemCap)
                .totalMemoryRequestGb(totalMemReq)
                .totalMemoryLimitGb(totalMemLim)
                .totalMemoryUsageGb(totalMemUse)
                .memoryUtilizationPercentage(memUtilPct)
                .currentMonthlyCost(currentMonthlyCost)
                .potentialMonthlySavings(potentialMonthlySavings)
                .optimizedMonthlyCost(optimizedMonthlyCost)
                .nodes(nodes)
                .pods(pods)
                .build();
    }
}
