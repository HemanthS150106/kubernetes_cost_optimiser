package com.k8s.costoptimizer.application.dto;

import com.k8s.costoptimizer.domain.model.NodeResourceInfo;
import com.k8s.costoptimizer.domain.model.PodResourceInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * REST API response DTO for the dashboard cluster overview metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterOverviewResponse {
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
    private Double cpuUtilizationPercentage;
    
    private Double totalMemoryCapacityGb;
    private Double totalMemoryRequestGb;
    private Double totalMemoryLimitGb;
    private Double totalMemoryUsageGb;
    private Double memoryUtilizationPercentage;
    
    private Double currentMonthlyCost;
    private Double potentialMonthlySavings;
    private Double optimizedMonthlyCost;

    private List<NodeResourceInfo> nodes;
    private List<PodResourceInfo> pods;
}
