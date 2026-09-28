package com.k8s.costoptimizer.presentation.controller;

import com.k8s.costoptimizer.application.service.ClusterAnalysisUseCase;
import com.k8s.costoptimizer.domain.model.TemporalProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for exposing temporal usage profiles computed by the
 * Temporal Anomaly Detection engine.
 *
 * These endpoints serve the frontend's "Temporal Profiles" dashboard page,
 * providing per-container percentile statistics, hourly heatmap data,
 * burstiness metrics, and workload pattern classifications.
 */
@RestController
@RequestMapping("/api/v1/temporal")
@RequiredArgsConstructor
@Tag(name = "Temporal Analysis", description = "Endpoints for time-series-aware workload usage profiling and anomaly detection")
@SecurityRequirement(name = "Bearer Authentication")
public class TemporalProfileController {

    private final ClusterAnalysisUseCase clusterAnalysisUseCase;

    @GetMapping("/profiles")
    @Operation(summary = "Retrieve all temporal usage profiles with percentile statistics and workload classifications")
    public ResponseEntity<List<TemporalProfile>> getTemporalProfiles() {
        return ResponseEntity.ok(clusterAnalysisUseCase.getTemporalProfiles());
    }

    @GetMapping("/summary")
    @Operation(summary = "Retrieve a summary of temporal analysis status and tracked resource count")
    public ResponseEntity<Map<String, Object>> getTemporalSummary() {
        Map<String, Object> summary = new HashMap<>();
        List<TemporalProfile> profiles = clusterAnalysisUseCase.getTemporalProfiles();

        summary.put("trackedResources", clusterAnalysisUseCase.getTrackedResourceCount());
        summary.put("profilesAvailable", profiles.size());

        // Count by workload pattern
        Map<String, Long> patternCounts = new HashMap<>();
        for (TemporalProfile p : profiles) {
            String pattern = p.getPattern() != null ? p.getPattern().name() : "UNKNOWN";
            patternCounts.merge(pattern, 1L, Long::sum);
        }
        summary.put("patternDistribution", patternCounts);

        // Average burstiness
        double avgCpuBurst = profiles.stream().mapToDouble(TemporalProfile::getCpuBurstiness).average().orElse(0.0);
        double avgMemBurst = profiles.stream().mapToDouble(TemporalProfile::getMemBurstiness).average().orElse(0.0);
        summary.put("avgCpuBurstiness", Math.round(avgCpuBurst * 100.0) / 100.0);
        summary.put("avgMemBurstiness", Math.round(avgMemBurst * 100.0) / 100.0);

        // Count high-confidence vs low-confidence profiles
        long highConfidence = profiles.stream().filter(p -> p.getSnapshotCount() >= 12).count();
        summary.put("highConfidenceProfiles", highConfidence);
        summary.put("lowConfidenceProfiles", profiles.size() - highConfidence);

        return ResponseEntity.ok(summary);
    }
}
