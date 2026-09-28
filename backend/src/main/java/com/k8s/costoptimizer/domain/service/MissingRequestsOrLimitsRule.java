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
 * Strategy implementation to detect pods that lack CPU or Memory requests/limits.
 */
public class MissingRequestsOrLimitsRule implements RecommendationRule {

    @Override
    public List<Recommendation> evaluate(List<PodResourceInfo> pods, List<NodeResourceInfo> nodes, double cpuRate, double memoryRate) {
        List<Recommendation> recommendations = new ArrayList<>();

        for (PodResourceInfo pod : pods) {
            // Skip system namespaces to focus on user workloads
            if (pod.getNamespace().equals("kube-system") || pod.getNamespace().equals("kubernetes-dashboard")) {
                continue;
            }

            pod.getContainers().forEach(container -> {
                // Check CPU requests
                if (container.getCpuRequest() == null || container.getCpuRequest() == 0.0) {
                    recommendations.add(createRecommendation(
                            pod,
                            container.getName(),
                            RecommendationType.ADD_CPU_REQUEST,
                            Severity.MEDIUM,
                            "Container '" + container.getName() + "' is missing CPU request. Set CPU request to establish scheduling guarantee.",
                            0.1, 0.1, null, null, 0.0
                    ));
                }

                // Check Memory requests
                if (container.getMemoryRequestGb() == null || container.getMemoryRequestGb() == 0.0) {
                    recommendations.add(createRecommendation(
                            pod,
                            container.getName(),
                            RecommendationType.ADD_MEM_REQUEST,
                            Severity.MEDIUM,
                            "Container '" + container.getName() + "' is missing Memory request. Set memory request (e.g. 128Mi) to avoid scheduling latency.",
                            null, null, 0.125, 0.125, 0.0
                    ));
                }

                // Check CPU Limits
                if (container.getCpuLimit() == null || container.getCpuLimit() == 0.0) {
                    recommendations.add(createRecommendation(
                            pod,
                            container.getName(),
                            RecommendationType.ADD_CPU_LIMIT,
                            Severity.LOW,
                            "Container '" + container.getName() + "' has no CPU limit. Add CPU limit to prevent runaway containers from starving the host node.",
                            null, null, null, null, 0.0
                    ));
                }

                // Check Memory Limits
                if (container.getMemoryLimitGb() == null || container.getMemoryLimitGb() == 0.0) {
                    recommendations.add(createRecommendation(
                            pod,
                            container.getName(),
                            RecommendationType.ADD_MEM_LIMIT,
                            Severity.MEDIUM,
                            "Container '" + container.getName() + "' has no Memory limit. Add limit to prevent the container from consuming all node memory and getting OOM-killed.",
                            null, null, null, null, 0.0
                    ));
                }
            });
        }

        return recommendations;
    }

    private Recommendation createRecommendation(
            PodResourceInfo pod,
            String containerName,
            RecommendationType type,
            Severity severity,
            String details,
            Double currentCpu,
            Double recommendedCpu,
            Double currentMem,
            Double recommendedMem,
            double savings) {
        return Recommendation.builder()
                .namespace(pod.getNamespace())
                .resourceName(pod.getName() + "/" + containerName)
                .resourceType(K8sResourceType.POD)
                .type(type)
                .severity(severity)
                .currentCpuRequest(currentCpu)
                .recommendedCpuRequest(recommendedCpu)
                .currentMemoryRequestGb(currentMem)
                .recommendedMemoryRequestGb(recommendedMem)
                .estimatedMonthlySavings(savings)
                .status(RecommendationStatus.ACTIVE)
                .details(details)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
