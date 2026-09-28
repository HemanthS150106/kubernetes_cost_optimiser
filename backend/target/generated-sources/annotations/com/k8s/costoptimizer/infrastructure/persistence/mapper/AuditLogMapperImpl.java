package com.k8s.costoptimizer.infrastructure.persistence.mapper;

import com.k8s.costoptimizer.domain.model.AuditLog;
import com.k8s.costoptimizer.infrastructure.persistence.entity.AuditLogEntity;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-22T22:14:43+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.18 (Eclipse Adoptium)"
)
@Component
public class AuditLogMapperImpl implements AuditLogMapper {

    @Override
    public AuditLog toDomain(AuditLogEntity entity) {
        if ( entity == null ) {
            return null;
        }

        AuditLog.AuditLogBuilder auditLog = AuditLog.builder();

        auditLog.id( entity.getId() );
        auditLog.timestamp( entity.getTimestamp() );
        auditLog.username( entity.getUsername() );
        auditLog.actionType( entity.getActionType() );
        auditLog.resourceName( entity.getResourceName() );
        auditLog.resourceType( entity.getResourceType() );
        auditLog.namespace( entity.getNamespace() );
        auditLog.previousConfiguration( entity.getPreviousConfiguration() );
        auditLog.newConfiguration( entity.getNewConfiguration() );
        auditLog.estimatedMonthlySavings( entity.getEstimatedMonthlySavings() );

        return auditLog.build();
    }

    @Override
    public AuditLogEntity toEntity(AuditLog domain) {
        if ( domain == null ) {
            return null;
        }

        AuditLogEntity.AuditLogEntityBuilder auditLogEntity = AuditLogEntity.builder();

        auditLogEntity.id( domain.getId() );
        auditLogEntity.timestamp( domain.getTimestamp() );
        auditLogEntity.username( domain.getUsername() );
        auditLogEntity.actionType( domain.getActionType() );
        auditLogEntity.resourceName( domain.getResourceName() );
        auditLogEntity.resourceType( domain.getResourceType() );
        auditLogEntity.namespace( domain.getNamespace() );
        auditLogEntity.previousConfiguration( domain.getPreviousConfiguration() );
        auditLogEntity.newConfiguration( domain.getNewConfiguration() );
        auditLogEntity.estimatedMonthlySavings( domain.getEstimatedMonthlySavings() );

        return auditLogEntity.build();
    }

    @Override
    public List<AuditLog> toDomainList(List<AuditLogEntity> entities) {
        if ( entities == null ) {
            return null;
        }

        List<AuditLog> list = new ArrayList<AuditLog>( entities.size() );
        for ( AuditLogEntity auditLogEntity : entities ) {
            list.add( toDomain( auditLogEntity ) );
        }

        return list;
    }

    @Override
    public List<AuditLogEntity> toEntityList(List<AuditLog> domains) {
        if ( domains == null ) {
            return null;
        }

        List<AuditLogEntity> list = new ArrayList<AuditLogEntity>( domains.size() );
        for ( AuditLog auditLog : domains ) {
            list.add( toEntity( auditLog ) );
        }

        return list;
    }
}
