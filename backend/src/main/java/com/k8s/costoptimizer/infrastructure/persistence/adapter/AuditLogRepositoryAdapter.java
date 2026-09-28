package com.k8s.costoptimizer.infrastructure.persistence.adapter;

import com.k8s.costoptimizer.domain.model.AuditLog;
import com.k8s.costoptimizer.domain.repository.AuditLogRepository;
import com.k8s.costoptimizer.infrastructure.persistence.entity.AuditLogEntity;
import com.k8s.costoptimizer.infrastructure.persistence.mapper.AuditLogMapper;
import com.k8s.costoptimizer.infrastructure.persistence.repository.SpringDataAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Persistence adapter bridging the clean domain port and Hibernate Spring Data.
 */
@Component
@RequiredArgsConstructor
public class AuditLogRepositoryAdapter implements AuditLogRepository {

    private final SpringDataAuditLogRepository jpaRepo;
    private final AuditLogMapper mapper;

    @Override
    public AuditLog save(AuditLog auditLog) {
        AuditLogEntity entity = mapper.toEntity(auditLog);
        AuditLogEntity saved = jpaRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<AuditLog> findAll() {
        return mapper.toDomainList(jpaRepo.findAllByOrderByTimestampDesc());
    }
}
