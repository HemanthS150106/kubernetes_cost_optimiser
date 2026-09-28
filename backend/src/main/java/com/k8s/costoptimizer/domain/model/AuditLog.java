package com.k8s.costoptimizer.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Domain entity representing an audit entry for Kubernetes cluster write actions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    private Long id;
    private LocalDateTime timestamp;
    private String username;
    private String actionType; // e.g., SCALE_DEPLOYMENT, UPDATE_RESOURCES, DELETE_POD, DRAIN_NODE
    private String resourceName;
    private String resourceType;
    private String namespace;
    private String previousConfiguration; // Spec description before write
    private String newConfiguration;      // Spec description after write
    private Double estimatedMonthlySavings;
}
