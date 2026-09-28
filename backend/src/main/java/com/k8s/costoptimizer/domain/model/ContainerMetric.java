package com.k8s.costoptimizer.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain model representing CPU and Memory allocations and live usage of a container.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerMetric {
    private String name;
    private Double cpuRequest;
    private Double cpuLimit;
    private Double cpuUsage;
    private Double memoryRequestGb;
    private Double memoryLimitGb;
    private Double memoryUsageGb;
}
