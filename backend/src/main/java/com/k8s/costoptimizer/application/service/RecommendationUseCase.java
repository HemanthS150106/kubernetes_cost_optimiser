package com.k8s.costoptimizer.application.service;

import com.k8s.costoptimizer.domain.model.Recommendation;
import com.k8s.costoptimizer.domain.model.RecommendationStatus;
import com.k8s.costoptimizer.domain.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service (Use Case) that manages querying, updating, dismissing and triggering
 * scans for recommendations.
 */
@Service
@RequiredArgsConstructor
public class RecommendationUseCase {

    private final RecommendationRepository recommendationRepository;
    private final ClusterAnalysisUseCase analysisUseCase;

    /**
     * Retrieves all recommendations, filtered by status or namespace if provided.
     */
    public List<Recommendation> getRecommendations(RecommendationStatus status, String namespace) {
        List<Recommendation> list = recommendationRepository.findAll();

        if (status != null) {
            list = list.stream().filter(r -> r.getStatus() == status).collect(Collectors.toList());
        }
        if (namespace != null && !namespace.trim().isEmpty() && !"all".equalsIgnoreCase(namespace)) {
            list = list.stream().filter(r -> namespace.equalsIgnoreCase(r.getNamespace())).collect(Collectors.toList());
        }

        return list;
    }

    /**
     * Updates the status of a specific recommendation (e.g. Dismissing or Implementing).
     */
    @Transactional
    public Recommendation updateStatus(Long id, RecommendationStatus status) {
        Recommendation recommendation = recommendationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recommendation not found with ID: " + id));

        recommendation.setStatus(status);
        recommendation.setUpdatedAt(LocalDateTime.now());
        return recommendationRepository.save(recommendation);
    }

    /**
     * Triggers an on-demand cluster optimization scan and returns the active recommendation list.
     */
    @Transactional
    public List<Recommendation> triggerManualScan() {
        analysisUseCase.analyzeAndSaveRecommendations();
        return recommendationRepository.findByStatus(RecommendationStatus.ACTIVE);
    }

    /**
     * Deletes a specific recommendation.
     */
    @Transactional
    public void deleteRecommendation(Long id) {
        recommendationRepository.deleteById(id);
    }
}
