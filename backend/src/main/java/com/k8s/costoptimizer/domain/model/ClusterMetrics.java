package com.k8s.costoptimizer.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Domain model representing aggregated metrics and optimization savings for a cluster.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterMetrics {
    private int totalNodes;
    private int totalPods;
    private int totalNamespaces;
    private int totalDeployments;
    private int totalDaemonSets;
    private int totalStatefulSets;
    private int totalServices;
    
    private Double totalCpuCapacity;
    private Double totalCpuRequest;
    private Double totalCpuLimit;
    private Double totalCpuUsage;
    
    private Double totalMemoryCapacityGb;
    private Double totalMemoryRequestGb;
    private Double totalMemoryLimitGb;
    private Double totalMemoryUsageGb;
    
    private Double currentMonthlyCost;
    private Double potentialMonthlySavings;
    private Double optimizedMonthlyCost;

    private List<NodeResourceInfo> nodes;
    private List<PodResourceInfo> pods;
}
