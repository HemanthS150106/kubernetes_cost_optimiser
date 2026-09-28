package com.k8s.costoptimizer.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Domain model representing a Kubernetes Pod and its aggregated resource information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PodResourceInfo {
    private String name;
    private String namespace;
    private String nodeName;
    private String status;
    private String controllerType;
    private String controllerName;
    private Map<String, String> labels;
    private List<ContainerMetric> containers;
    private Long ageInSeconds;

    public Double getTotalCpuRequest() {
        return containers.stream().mapToDouble(c -> c.getCpuRequest() != null ? c.getCpuRequest() : 0.0).sum();
    }

    public Double getTotalCpuLimit() {
        return containers.stream().mapToDouble(c -> c.getCpuLimit() != null ? c.getCpuLimit() : 0.0).sum();
    }

    public Double getTotalCpuUsage() {
        return containers.stream().mapToDouble(c -> c.getCpuUsage() != null ? c.getCpuUsage() : 0.0).sum();
    }

    public Double getTotalMemoryRequestGb() {
        return containers.stream().mapToDouble(c -> c.getMemoryRequestGb() != null ? c.getMemoryRequestGb() : 0.0).sum();
    }

    public Double getTotalMemoryLimitGb() {
        return containers.stream().mapToDouble(c -> c.getMemoryLimitGb() != null ? c.getMemoryLimitGb() : 0.0).sum();
    }

    public Double getTotalMemoryUsageGb() {
        return containers.stream().mapToDouble(c -> c.getMemoryUsageGb() != null ? c.getMemoryUsageGb() : 0.0).sum();
    }
}
