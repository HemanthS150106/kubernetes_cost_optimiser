package com.k8s.costoptimizer.presentation.controller;

import com.k8s.costoptimizer.application.dto.ClusterOverviewResponse;
import com.k8s.costoptimizer.application.service.ClusterAnalysisUseCase;
import com.k8s.costoptimizer.domain.repository.ClusterRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for retrieving general cluster configurations, namespaces, nodes and live usage stats.
 */
@RestController
@RequestMapping("/api/v1/cluster")
@RequiredArgsConstructor
@Tag(name = "Cluster Analytics", description = "Endpoints for fetching Kubernetes cluster nodes, pods and aggregate usage")
@SecurityRequirement(name = "Bearer Authentication")
public class ClusterController {

    private final ClusterAnalysisUseCase clusterAnalysisUseCase;
    private final ClusterRepository clusterRepository;

    @GetMapping("/overview")
    @Operation(summary = "Retrieve aggregated cluster usage dashboard metrics and costs")
    public ResponseEntity<ClusterOverviewResponse> getOverview() {
        return ResponseEntity.ok(clusterAnalysisUseCase.getClusterOverview());
    }

    @GetMapping("/namespaces")
    @Operation(summary = "Retrieve all active namespaces in the Kubernetes cluster")
    public ResponseEntity<List<String>> getNamespaces() {
        return ResponseEntity.ok(clusterRepository.getNamespaces());
    }
}
