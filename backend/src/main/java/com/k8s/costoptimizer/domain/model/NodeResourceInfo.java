package com.k8s.costoptimizer.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Domain model representing a Kubernetes Node resource status and details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeResourceInfo {
    private String name;
    private String status;
    private String role;
    private Map<String, String> labels;
    
    private Double cpuCapacity;
    private Double cpuAllocatable;
    private Double cpuUsage;
    
    private Double memoryCapacityGb;
    private Double memoryAllocatableGb;
    private Double memoryUsageGb;
    
    private Integer podCount;
    private Double estimatedMonthlyCost;
}
