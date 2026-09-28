package com.k8s.costoptimizer.presentation.controller;

import com.k8s.costoptimizer.application.service.RecommendationUseCase;
import com.k8s.costoptimizer.domain.model.Recommendation;
import com.k8s.costoptimizer.domain.model.RecommendationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller exposing recommendation-related endpoints.
 */
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@Tag(name = "Optimization Recommendations", description = "Endpoints for managing cost optimization recommendations")
@SecurityRequirement(name = "Bearer Authentication")
public class RecommendationController {

    private final RecommendationUseCase recommendationUseCase;

    @GetMapping
    @Operation(summary = "Get list of active or dismissed recommendations, optional filters for status & namespace")
    public ResponseEntity<List<Recommendation>> getRecommendations(
            @RequestParam(required = false) RecommendationStatus status,
            @RequestParam(required = false) String namespace) {
        return ResponseEntity.ok(recommendationUseCase.getRecommendations(status, namespace));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update the status of an existing cost recommendation (e.g. Dismiss it)")
    public ResponseEntity<Recommendation> updateStatus(
            @PathVariable Long id,
            @RequestParam RecommendationStatus status) {
        return ResponseEntity.ok(recommendationUseCase.updateStatus(id, status));
    }

    @PostMapping("/scan")
    @Operation(summary = "Manually trigger a fresh cluster metrics scan and evaluate cost opportunities")
    public ResponseEntity<List<Recommendation>> triggerScan() {
        return ResponseEntity.ok(recommendationUseCase.triggerManualScan());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a recommendation from database history")
    public ResponseEntity<Void> deleteRecommendation(@PathVariable Long id) {
        recommendationUseCase.deleteRecommendation(id);
        return ResponseEntity.noContent().build();
    }
}
