package com.k8s.costoptimizer.infrastructure.persistence.mapper;

import com.k8s.costoptimizer.domain.model.Recommendation;
import com.k8s.costoptimizer.infrastructure.persistence.entity.RecommendationEntity;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper for converting between Recommendation domain model and JPA Entity.
 */
@Mapper(componentModel = "spring")
public interface RecommendationMapper {

    Recommendation toDomain(RecommendationEntity entity);

    RecommendationEntity toEntity(Recommendation domain);

    List<Recommendation> toDomainList(List<RecommendationEntity> entities);

    List<RecommendationEntity> toEntityList(List<Recommendation> domains);
}
