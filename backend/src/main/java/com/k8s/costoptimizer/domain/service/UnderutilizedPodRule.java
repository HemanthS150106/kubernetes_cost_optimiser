package com.k8s.costoptimizer.domain.service;

import com.k8s.costoptimizer.domain.model.K8sResourceType;
import com.k8s.costoptimizer.domain.model.NodeResourceInfo;
import com.k8s.costoptimizer.domain.model.PodResourceInfo;
import com.k8s.costoptimizer.domain.model.Recommendation;
import com.k8s.costoptimizer.domain.model.RecommendationStatus;
import com.k8s.costoptimizer.domain.model.RecommendationType;
import com.k8s.costoptimizer.domain.model.Severity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Strategy implementation to find pods that are over-provisioned compared to actual live usage.
 */
public class UnderutilizedPodRule implements RecommendationRule {

    private static final double CPU_UNDERUTILIZED_THRESHOLD = 0.25; // 25% CPU usage vs request
    private static final double MEM_UNDERUTILIZED_THRESHOLD = 0.30; // 30% Memory usage vs request
    private static final double HOURS_PER_MONTH = 730.0;

    @Override
    public List<Recommendation> evaluate(List<PodResourceInfo> pods, List<NodeResourceInfo> nodes, double cpuRate, double memoryRate) {
        List<Recommendation> recommendations = new ArrayList<>();

        for (PodResourceInfo pod : pods) {
            // Skip system namespaces
            if (pod.getNamespace().equals("kube-system") || pod.getNamespace().equals("kubernetes-dashboard")) {
                continue;
            }

            pod.getContainers().forEach(container -> {
                double cpuReq = container.getCpuRequest() != null ? container.getCpuRequest() : 0.0;
                double cpuUse = container.getCpuUsage() != null ? container.getCpuUsage() : 0.0;
                double memReq = container.getMemoryRequestGb() != null ? container.getMemoryRequestGb() : 0.0;
                double memUse = container.getMemoryUsageGb() != null ? container.getMemoryUsageGb() : 0.0;

                // Evaluate CPU underutilization
                if (cpuReq > 0.05 && cpuUse > 0.0 && (cpuUse / cpuReq) < CPU_UNDERUTILIZED_THRESHOLD) {
                    double recommendedCpu = Math.max(cpuUse * 1.5, 0.01); // 50% headroom
                    double cpuSavings = (cpuReq - recommendedCpu);
                    double monthlySavings = cpuSavings * cpuRate * HOURS_PER_MONTH;

                    if (monthlySavings > 0.5) { // Only recommend if monthly savings are notable
                        recommendations.add(Recommendation.builder()
                                .namespace(pod.getNamespace())
                                .resourceName(pod.getName() + "/" + container.getName())
                                .resourceType(K8sResourceType.POD)
                                .type(RecommendationType.REDUCE_CPU_REQUEST)
                                .severity(monthlySavings > 15.0 ? Severity.HIGH : Severity.MEDIUM)
                                .currentCpuRequest(cpuReq)
                                .recommendedCpuRequest(recommendedCpu)
                                .currentMemoryRequestGb(memReq)
                                .recommendedMemoryRequestGb(memReq)
                                .estimatedMonthlySavings(monthlySavings)
                                .status(RecommendationStatus.ACTIVE)
                                .details(String.format("CPU utilization is low (%.1f%% of requested %.2f cores). Reduce request to %.2f cores.",
                                        (cpuUse / cpuReq) * 100, cpuReq, recommendedCpu))
                                .createdAt(LocalDateTime.now())
                                .build());
                    }
                }

                // Evaluate Memory underutilization
                if (memReq > 0.1 && memUse > 0.0 && (memUse / memReq) < MEM_UNDERUTILIZED_THRESHOLD) {
                    double recommendedMem = Math.max(memUse * 1.3, 0.032); // 30% headroom, 32MB floor
                    double memSavings = (memReq - recommendedMem);
                    double monthlySavings = memSavings * memoryRate * HOURS_PER_MONTH;

                    if (monthlySavings > 0.5) {
                        recommendations.add(Recommendation.builder()
                                .namespace(pod.getNamespace())
                                .resourceName(pod.getName() + "/" + container.getName())
                                .resourceType(K8sResourceType.POD)
                                .type(RecommendationType.REDUCE_MEM_REQUEST)
                                .severity(monthlySavings > 15.0 ? Severity.HIGH : Severity.MEDIUM)
                                .currentCpuRequest(cpuReq)
                                .recommendedCpuRequest(cpuReq)
                                .currentMemoryRequestGb(memReq)
                                .recommendedMemoryRequestGb(recommendedMem)
                                .estimatedMonthlySavings(monthlySavings)
                                .status(RecommendationStatus.ACTIVE)
                                .details(String.format("Memory utilization is low (%.1f%% of requested %.2f GB). Reduce request to %.2f GB.",
                                        (memUse / memReq) * 100, memReq, recommendedMem))
                                .createdAt(LocalDateTime.now())
                                .build());
                    }
                }
            });
        }

        return recommendations;
    }
}
