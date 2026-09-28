package com.k8s.costoptimizer.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Domain entity representing a Cost Optimization Recommendation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recommendation {
    private Long id;
    private String clusterId;
    private String namespace;
    private String resourceName;
    private K8sResourceType resourceType;
    private RecommendationType type;
    private Severity severity;
    private Double currentCpuRequest;
    private Double recommendedCpuRequest;
    private Double currentMemoryRequestGb;
    private Double recommendedMemoryRequestGb;
    private Integer currentReplicas;
    private Integer recommendedReplicas;
    private Double estimatedMonthlySavings;
    private RecommendationStatus status;
    private String details;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
