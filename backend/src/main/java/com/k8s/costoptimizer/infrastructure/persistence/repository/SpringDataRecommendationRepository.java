package com.k8s.costoptimizer.infrastructure.persistence.repository;

import com.k8s.costoptimizer.domain.model.RecommendationStatus;
import com.k8s.costoptimizer.infrastructure.persistence.entity.RecommendationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for Cost Recommendations.
 */
@Repository
public interface SpringDataRecommendationRepository extends JpaRepository<RecommendationEntity, Long> {
    List<RecommendationEntity> findByStatus(RecommendationStatus status);
    List<RecommendationEntity> findByNamespace(String namespace);
}
