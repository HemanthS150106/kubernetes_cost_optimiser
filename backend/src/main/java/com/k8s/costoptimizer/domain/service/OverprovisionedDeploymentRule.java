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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Strategy implementation to find deployments with excessive replicas relative to actual aggregated workload usage.
 */
public class OverprovisionedDeploymentRule implements RecommendationRule {

    private static final double HOURS_PER_MONTH = 730.0;

    @Override
    public List<Recommendation> evaluate(List<PodResourceInfo> pods, List<NodeResourceInfo> nodes, double cpuRate, double memoryRate) {
        List<Recommendation> recommendations = new ArrayList<>();

        // Group pods by namespace and controller name (where controller is a Deployment)
        Map<String, List<PodResourceInfo>> deploymentPods = pods.stream()
                .filter(p -> "Deployment".equalsIgnoreCase(p.getControllerType()) && p.getControllerName() != null)
                .collect(Collectors.groupingBy(p -> p.getNamespace() + "/" + p.getControllerName()));

        for (Map.Entry<String, List<PodResourceInfo>> entry : deploymentPods.entrySet()) {
            String key = entry.getKey();
            List<PodResourceInfo> pList = entry.getValue();
            int currentReplicas = pList.size();

            // We only optimize if replica count > 1
            if (currentReplicas <= 1) {
                continue;
            }

            String namespace = pList.get(0).getNamespace();
            String deploymentName = pList.get(0).getControllerName();

            // Skip system namespaces
            if ("kube-system".equals(namespace) || "kubernetes-dashboard".equals(namespace)) {
                continue;
            }

            double totalCpuReq = pList.stream().mapToDouble(PodResourceInfo::getTotalCpuRequest).sum();
            double totalCpuUse = pList.stream().mapToDouble(PodResourceInfo::getTotalCpuUsage).sum();
            double totalMemReq = pList.stream().mapToDouble(PodResourceInfo::getTotalMemoryRequestGb).sum();
            double totalMemUse = pList.stream().mapToDouble(PodResourceInfo::getTotalMemoryUsageGb).sum();

            if (totalCpuReq == 0.0 || totalMemReq == 0.0) {
                continue;
            }

            double avgCpuUtil = totalCpuUse / totalCpuReq;
            double avgMemUtil = totalMemUse / totalMemReq;

            // If utilization is very low (<20% CPU and <25% Mem), recommend downscaling replicas
            if (avgCpuUtil < 0.20 && avgMemUtil < 0.25) {
                int recommendedReplicas = Math.max(1, currentReplicas / 2); // Downscale by half, floor at 1
                int replicaDiff = currentReplicas - recommendedReplicas;

                if (replicaDiff > 0) {
                    double cpuPerPod = totalCpuReq / currentReplicas;
                    double memPerPod = totalMemReq / currentReplicas;
                    
                    double monthlySavings = replicaDiff * (cpuPerPod * cpuRate + memPerPod * memoryRate) * HOURS_PER_MONTH;

                    if (monthlySavings > 1.0) {
                        recommendations.add(Recommendation.builder()
                                .namespace(namespace)
                                .resourceName(deploymentName)
                                .resourceType(K8sResourceType.DEPLOYMENT)
                                .type(RecommendationType.REDUCE_REPLICAS)
                                .severity(monthlySavings > 25.0 ? Severity.HIGH : Severity.MEDIUM)
                                .currentReplicas(currentReplicas)
                                .recommendedReplicas(recommendedReplicas)
                                .estimatedMonthlySavings(monthlySavings)
                                .status(RecommendationStatus.ACTIVE)
                                .details(String.format("Deployment '%s' has low utilization (CPU: %.1f%%, Memory: %.1f%%) across %d replicas. Reduce replica count to %d.",
                                        deploymentName, avgCpuUtil * 100, avgMemUtil * 100, currentReplicas, recommendedReplicas))
                                .createdAt(LocalDateTime.now())
                                .build());
                    }
                }
            }
        }

        return recommendations;
    }
}
