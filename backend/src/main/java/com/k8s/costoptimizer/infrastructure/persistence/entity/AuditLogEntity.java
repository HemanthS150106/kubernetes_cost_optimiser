package com.k8s.costoptimizer.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity representing cluster mutation logs in the PostgreSQL database.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(name = "resource_name", nullable = false)
    private String resourceName;

    @Column(name = "resource_type", nullable = false)
    private String resourceType;

    @Column(name = "namespace")
    private String namespace;

    @Column(name = "previous_configuration", length = 2000)
    private String previousConfiguration;

    @Column(name = "new_configuration", length = 2000)
    private String newConfiguration;

    @Column(name = "estimated_monthly_savings")
    private Double estimatedMonthlySavings;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
