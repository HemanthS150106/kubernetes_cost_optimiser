package com.k8s.costoptimizer.infrastructure.persistence.mapper;

import com.k8s.costoptimizer.domain.model.AuditLog;
import com.k8s.costoptimizer.infrastructure.persistence.entity.AuditLogEntity;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct converter for AuditLog models.
 */
@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    AuditLog toDomain(AuditLogEntity entity);

    AuditLogEntity toEntity(AuditLog domain);

    List<AuditLog> toDomainList(List<AuditLogEntity> entities);

    List<AuditLogEntity> toEntityList(List<AuditLog> domains);
}
