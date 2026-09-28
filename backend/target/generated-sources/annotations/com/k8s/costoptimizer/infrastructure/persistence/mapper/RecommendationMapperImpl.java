package com.k8s.costoptimizer.infrastructure.persistence.mapper;

import com.k8s.costoptimizer.domain.model.Recommendation;
import com.k8s.costoptimizer.infrastructure.persistence.entity.RecommendationEntity;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-22T22:14:42+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.18 (Eclipse Adoptium)"
)
@Component
public class RecommendationMapperImpl implements RecommendationMapper {

    @Override
    public Recommendation toDomain(RecommendationEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Recommendation.RecommendationBuilder recommendation = Recommendation.builder();

        recommendation.id( entity.getId() );
        recommendation.clusterId( entity.getClusterId() );
        recommendation.namespace( entity.getNamespace() );
        recommendation.resourceName( entity.getResourceName() );
        recommendation.resourceType( entity.getResourceType() );
        recommendation.type( entity.getType() );
        recommendation.severity( entity.getSeverity() );
        recommendation.currentCpuRequest( entity.getCurrentCpuRequest() );
        recommendation.recommendedCpuRequest( entity.getRecommendedCpuRequest() );
        recommendation.currentMemoryRequestGb( entity.getCurrentMemoryRequestGb() );
        recommendation.recommendedMemoryRequestGb( entity.getRecommendedMemoryRequestGb() );
        recommendation.currentReplicas( entity.getCurrentReplicas() );
        recommendation.recommendedReplicas( entity.getRecommendedReplicas() );
        recommendation.estimatedMonthlySavings( entity.getEstimatedMonthlySavings() );
        recommendation.status( entity.getStatus() );
        recommendation.details( entity.getDetails() );
        recommendation.createdAt( entity.getCreatedAt() );
        recommendation.updatedAt( entity.getUpdatedAt() );

        return recommendation.build();
    }

    @Override
    public RecommendationEntity toEntity(Recommendation domain) {
        if ( domain == null ) {
            return null;
        }

        RecommendationEntity.RecommendationEntityBuilder recommendationEntity = RecommendationEntity.builder();

        recommendationEntity.id( domain.getId() );
        recommendationEntity.clusterId( domain.getClusterId() );
        recommendationEntity.namespace( domain.getNamespace() );
        recommendationEntity.resourceName( domain.getResourceName() );
        recommendationEntity.resourceType( domain.getResourceType() );
        recommendationEntity.type( domain.getType() );
        recommendationEntity.severity( domain.getSeverity() );
        recommendationEntity.currentCpuRequest( domain.getCurrentCpuRequest() );
        recommendationEntity.recommendedCpuRequest( domain.getRecommendedCpuRequest() );
        recommendationEntity.currentMemoryRequestGb( domain.getCurrentMemoryRequestGb() );
        recommendationEntity.recommendedMemoryRequestGb( domain.getRecommendedMemoryRequestGb() );
        recommendationEntity.currentReplicas( domain.getCurrentReplicas() );
        recommendationEntity.recommendedReplicas( domain.getRecommendedReplicas() );
        recommendationEntity.estimatedMonthlySavings( domain.getEstimatedMonthlySavings() );
        recommendationEntity.status( domain.getStatus() );
        recommendationEntity.details( domain.getDetails() );
        recommendationEntity.createdAt( domain.getCreatedAt() );
        recommendationEntity.updatedAt( domain.getUpdatedAt() );

        return recommendationEntity.build();
    }

    @Override
    public List<Recommendation> toDomainList(List<RecommendationEntity> entities) {
        if ( entities == null ) {
            return null;
        }

        List<Recommendation> list = new ArrayList<Recommendation>( entities.size() );
        for ( RecommendationEntity recommendationEntity : entities ) {
            list.add( toDomain( recommendationEntity ) );
        }

        return list;
    }

    @Override
    public List<RecommendationEntity> toEntityList(List<Recommendation> domains) {
        if ( domains == null ) {
            return null;
        }

        List<RecommendationEntity> list = new ArrayList<RecommendationEntity>( domains.size() );
        for ( Recommendation recommendation : domains ) {
            list.add( toEntity( recommendation ) );
        }

        return list;
    }
}
