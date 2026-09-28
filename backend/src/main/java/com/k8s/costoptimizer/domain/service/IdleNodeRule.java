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
 * Strategy implementation to find nodes with low allocations that can be consolidated.
 */
public class IdleNodeRule implements RecommendationRule {

    private static final double HOURS_PER_MONTH = 730.0;

    @Override
    public List<Recommendation> evaluate(List<PodResourceInfo> pods, List<NodeResourceInfo> nodes, double cpuRate, double memoryRate) {
        List<Recommendation> recommendations = new ArrayList<>();

        // If there's only 1 node in the cluster, don't recommend draining it (single-node development or base cluster)
        if (nodes.size() <= 1) {
            return recommendations;
        }

        for (NodeResourceInfo node : nodes) {
            double cpuAlloc = node.getCpuAllocatable() != null ? node.getCpuAllocatable() : 0.0;
            double cpuUse = node.getCpuUsage() != null ? node.getCpuUsage() : 0.0;
            double memAlloc = node.getMemoryAllocatableGb() != null ? node.getMemoryAllocatableGb() : 0.0;
            double memUse = node.getMemoryUsageGb() != null ? node.getMemoryUsageGb() : 0.0;
            int podCount = node.getPodCount() != null ? node.getPodCount() : 0;

            if (cpuAlloc == 0.0 || memAlloc == 0.0) {
                continue;
            }

            double cpuUtilPct = cpuUse / cpuAlloc;
            double memUtilPct = memUse / memAlloc;

            // Flag as idle if utilization is low (CPU < 15%, Memory < 20%) and has few workloads (pod count <= 5)
            if (cpuUtilPct < 0.15 && memUtilPct < 0.20 && podCount <= 5) {
                // Node monthly cost calculation
                double nodeCost = (cpuAlloc * cpuRate + memAlloc * memoryRate) * HOURS_PER_MONTH;

                recommendations.add(Recommendation.builder()
                        .namespace("all")
                        .resourceName(node.getName())
                        .resourceType(K8sResourceType.NODE)
                        .type(RecommendationType.DRAIN_NODE)
                        .severity(Severity.HIGH)
                        .currentCpuRequest(cpuAlloc)
                        .recommendedCpuRequest(0.0)
                        .currentMemoryRequestGb(memAlloc)
                        .recommendedMemoryRequestGb(0.0)
                        .estimatedMonthlySavings(nodeCost)
                        .status(RecommendationStatus.ACTIVE)
                        .details(String.format("Node has low utilization (CPU: %.1f%%, Memory: %.1f%%) hosting only %d pods. Drain workloads to consolidate resources.",
                                cpuUtilPct * 100, memUtilPct * 100, podCount))
                        .createdAt(LocalDateTime.now())
                        .build());
            }
        }

        return recommendations;
    }
}
