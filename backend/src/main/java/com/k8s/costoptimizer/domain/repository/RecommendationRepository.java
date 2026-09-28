package com.k8s.costoptimizer.domain.repository;

import com.k8s.costoptimizer.domain.model.Recommendation;
import com.k8s.costoptimizer.domain.model.RecommendationStatus;

import java.util.List;
import java.util.Optional;

/**
 * Domain boundary port for persisting and querying cost optimization recommendations.
 */
public interface RecommendationRepository {
    Recommendation save(Recommendation recommendation);
    List<Recommendation> saveAll(List<Recommendation> recommendations);
    Optional<Recommendation> findById(Long id);
    List<Recommendation> findAll();
    List<Recommendation> findByStatus(RecommendationStatus status);
    List<Recommendation> findByNamespace(String namespace);
    void deleteById(Long id);
    void deleteAll();
}
