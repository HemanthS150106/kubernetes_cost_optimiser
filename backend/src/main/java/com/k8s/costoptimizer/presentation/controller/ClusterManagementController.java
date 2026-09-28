package com.k8s.costoptimizer.presentation.controller;

import com.k8s.costoptimizer.application.service.ClusterManagementUseCase;
import com.k8s.costoptimizer.domain.model.AuditLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller exposing endpoints for active cluster mutations and operations history.
 */
@RestController
@RequestMapping("/api/v1/management")
@RequiredArgsConstructor
@Tag(name = "Cluster Management", description = "Endpoints for active cluster write modifications and scaling options")
@SecurityRequirement(name = "Bearer Authentication")
public class ClusterManagementController {

    private final ClusterManagementUseCase managementUseCase;

    @PostMapping("/scale")
    @Operation(summary = "Scale a deployment to target replica counts")
    public ResponseEntity<String> scaleDeployment(
            @RequestParam String namespace,
            @RequestParam String name,
            @RequestParam int replicas) {
        managementUseCase.scaleDeployment(namespace, name, replicas);
        return ResponseEntity.ok("Scale operation completed successfully.");
    }

    @PostMapping("/restart")
    @Operation(summary = "Trigger a rolling rollout restart of a Deployment template")
    public ResponseEntity<String> restartDeployment(
            @RequestParam String namespace,
            @RequestParam String name) {
        managementUseCase.restartDeployment(namespace, name);
        return ResponseEntity.ok("Rolling restart triggered successfully.");
    }

    @DeleteMapping("/pods")
    @Operation(summary = "Delete an active running pod by name")
    public ResponseEntity<String> deletePod(
            @RequestParam String namespace,
            @RequestParam String name) {
        managementUseCase.deletePod(namespace, name);
        return ResponseEntity.ok("Pod deletion requested.");
    }

    @PostMapping("/nodes/cordon")
    @Operation(summary = "Cordon or uncordon a node")
    public ResponseEntity<String> cordonNode(
            @RequestParam String name,
            @RequestParam boolean cordon) {
        managementUseCase.cordonNode(name, cordon);
        return ResponseEntity.ok(cordon ? "Node cordoned successfully." : "Node uncordoned successfully.");
    }

    @PostMapping("/nodes/drain")
    @Operation(summary = "Cordon a node and evict all user workloads")
    public ResponseEntity<String> drainNode(@RequestParam String name) {
        managementUseCase.drainNode(name);
        return ResponseEntity.ok("Node drain operation completed.");
    }

    @PostMapping("/recommendations/{id}/apply")
    @Operation(summary = "Automatically execute and apply an optimization recommendation to the cluster")
    public ResponseEntity<String> applyRecommendation(@PathVariable Long id) {
        managementUseCase.applyRecommendation(id);
        return ResponseEntity.ok("Recommendation applied successfully.");
    }

    @PostMapping("/recommendations/rollback")
    @Operation(summary = "Revert resource optimizations and roll back specs to prior states")
    public ResponseEntity<String> rollbackRecommendation(@RequestParam Long auditLogId) {
        managementUseCase.rollbackRecommendation(auditLogId);
        return ResponseEntity.ok("Rollback operation completed.");
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Retrieve cluster operation history ledger")
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        return ResponseEntity.ok(managementUseCase.getAuditLogs());
    }
}
