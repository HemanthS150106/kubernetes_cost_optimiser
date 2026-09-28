package com.k8s.costoptimizer.infrastructure.persistence.repository;

import com.k8s.costoptimizer.infrastructure.persistence.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for Audit Log transactions.
 */
@Repository
public interface SpringDataAuditLogRepository extends JpaRepository<AuditLogEntity, Long> {
    List<AuditLogEntity> findAllByOrderByTimestampDesc();
}
