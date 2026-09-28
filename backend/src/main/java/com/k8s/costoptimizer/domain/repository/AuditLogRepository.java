package com.k8s.costoptimizer.domain.repository;

import com.k8s.costoptimizer.domain.model.AuditLog;

import java.util.List;

/**
 * Domain boundary port for saving and querying audit log entries.
 */
public interface AuditLogRepository {
    AuditLog save(AuditLog auditLog);
    List<AuditLog> findAll();
}
