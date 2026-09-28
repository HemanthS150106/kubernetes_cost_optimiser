package com.k8s.costoptimizer.infrastructure.persistence.entity;

import com.k8s.costoptimizer.domain.model.K8sResourceType;
import com.k8s.costoptimizer.domain.model.RecommendationStatus;
import com.k8s.costoptimizer.domain.model.RecommendationType;
import com.k8s.costoptimizer.domain.model.Severity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity representing cost recommendations in the PostgreSQL database.
 */
@Entity
@Table(name = "recommendations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cluster_id")
    private String clusterId;

    @Column(name = "namespace", nullable = false)
    private String namespace;

    @Column(name = "resource_name", nullable = false)
    private String resourceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    private K8sResourceType resourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_type", nullable = false)
    private RecommendationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private Severity severity;

    @Column(name = "current_cpu_request")
    private Double currentCpuRequest;

    @Column(name = "recommended_cpu_request")
    private Double recommendedCpuRequest;

    @Column(name = "current_memory_request_gb")
    private Double currentMemoryRequestGb;

    @Column(name = "recommended_memory_request_gb")
    private Double recommendedMemoryRequestGb;

    @Column(name = "current_replicas")
    private Integer currentReplicas;

    @Column(name = "recommended_replicas")
    private Integer recommendedReplicas;

    @Column(name = "estimated_monthly_savings", nullable = false)
    private Double estimatedMonthlySavings;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RecommendationStatus status;

    @Column(name = "details", length = 1000)
    private String details;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = RecommendationStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
