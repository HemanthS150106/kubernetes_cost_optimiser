package com.k8s.costoptimizer.application.service;

import com.k8s.costoptimizer.domain.model.AuditLog;
import com.k8s.costoptimizer.domain.model.Recommendation;
import com.k8s.costoptimizer.domain.model.RecommendationStatus;
import com.k8s.costoptimizer.domain.repository.ClusterRepository;
import com.k8s.costoptimizer.domain.repository.AuditLogRepository;
import com.k8s.costoptimizer.domain.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service orchestrating active Kubernetes write modifications,
 * recording transactions in the audit logs and tracking FinOps savings actions.
 */
@Service
@RequiredArgsConstructor
public class ClusterManagementUseCase {

    private static final Logger log = LoggerFactory.getLogger(ClusterManagementUseCase.class);

    private final ClusterRepository clusterRepository;
    private final AuditLogRepository auditLogRepository;
    private final RecommendationRepository recommendationRepository;

    private String getCurrentUser() {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        }
        return "system-scheduler";
    }

    @Transactional
    public void scaleDeployment(String namespace, String name, int replicas) {
        log.info("User {} requested scaling deployment {} in namespace {} to {} replicas", getCurrentUser(), name, namespace, replicas);
        
        // Audit log previous configuration spec (simulated summary for simplicity)
        String prevSpec = String.format("{\"replicas\": \"unknown\"}");
        String newSpec = String.format("{\"replicas\": %d}", replicas);

        clusterRepository.scaleDeployment(namespace, name, replicas);

        auditLogRepository.save(AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .username(getCurrentUser())
                .actionType("SCALE_DEPLOYMENT")
                .resourceName(name)
                .resourceType("Deployment")
                .namespace(namespace)
                .previousConfiguration(prevSpec)
                .newConfiguration(newSpec)
                .estimatedMonthlySavings(0.0)
                .build());
    }

    @Transactional
    public void restartDeployment(String namespace, String name) {
        log.info("User {} requested rolling restart of deployment {}/{}", getCurrentUser(), namespace, name);
        clusterRepository.restartDeployment(namespace, name);

        auditLogRepository.save(AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .username(getCurrentUser())
                .actionType("RESTART_DEPLOYMENT")
                .resourceName(name)
                .resourceType("Deployment")
                .namespace(namespace)
                .previousConfiguration("{}")
                .newConfiguration("{\"triggeredRestart\": true}")
                .estimatedMonthlySavings(0.0)
                .build());
    }

    @Transactional
    public void deletePod(String namespace, String name) {
        log.info("User {} requested deletion of pod {}/{}", getCurrentUser(), namespace, name);
        clusterRepository.deletePod(namespace, name);

        auditLogRepository.save(AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .username(getCurrentUser())
                .actionType("DELETE_POD")
                .resourceName(name)
                .resourceType("Pod")
                .namespace(namespace)
                .previousConfiguration("{\"state\": \"Running\"}")
                .newConfiguration("{\"state\": \"Deleted\"}")
                .estimatedMonthlySavings(0.0)
                .build());
    }

    @Transactional
    public void cordonNode(String nodeName, boolean cordon) {
        log.info("User {} requested cordon={} on node {}", getCurrentUser(), cordon, nodeName);
        clusterRepository.cordonNode(nodeName, cordon);

        auditLogRepository.save(AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .username(getCurrentUser())
                .actionType(cordon ? "CORDON_NODE" : "UNCORDON_NODE")
                .resourceName(nodeName)
                .resourceType("Node")
                .namespace("all")
                .previousConfiguration(String.format("{\"unschedulable\": %b}", !cordon))
                .newConfiguration(String.format("{\"unschedulable\": %b}", cordon))
                .estimatedMonthlySavings(0.0)
                .build());
    }

    @Transactional
    public void drainNode(String nodeName) {
        log.info("User {} requested drain on node {}", getCurrentUser(), nodeName);
        clusterRepository.drainNode(nodeName);

        auditLogRepository.save(AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .username(getCurrentUser())
                .actionType("DRAIN_NODE")
                .resourceName(nodeName)
                .resourceType("Node")
                .namespace("all")
                .previousConfiguration("{\"schedulable\": true}")
                .newConfiguration("{\"drained\": true}")
                .estimatedMonthlySavings(0.0) // Node group scaling down is handled as downstream
                .build());
    }

    @Transactional
    public void applyRecommendation(Long id) {
        Recommendation rec = recommendationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recommendation not found with ID: " + id));

        if (rec.getStatus() != RecommendationStatus.ACTIVE) {
            throw new IllegalStateException("Only active recommendations can be applied.");
        }

        log.info("User {} is applying recommendation ID {} on resource {}", getCurrentUser(), id, rec.getResourceName());

        String prevConfig = "";
        String newConfig = "";

        switch (rec.getType()) {
            case REDUCE_CPU_REQUEST:
            case REDUCE_MEM_REQUEST:
            case ADD_CPU_REQUEST:
            case ADD_MEM_REQUEST:
                // Extract deployment name and container name from resourceName (format: deploymentName/containerName)
                String[] parts = rec.getResourceName().split("/");
                String deploymentName = parts[0];
                String containerName = parts.length > 1 ? parts[1] : deploymentName;
                
                prevConfig = String.format("{\"cpuRequest\": %.3f, \"memoryRequestGb\": %.3f}", 
                        rec.getCurrentCpuRequest() != null ? rec.getCurrentCpuRequest() : 0.0,
                        rec.getCurrentMemoryRequestGb() != null ? rec.getCurrentMemoryRequestGb() : 0.0);
                
                newConfig = String.format("{\"cpuRequest\": %.3f, \"memoryRequestGb\": %.3f}", 
                        rec.getRecommendedCpuRequest() != null ? rec.getRecommendedCpuRequest() : 0.0,
                        rec.getRecommendedMemoryRequestGb() != null ? rec.getRecommendedMemoryRequestGb() : 0.0);

                // Execute modification in the cluster
                clusterRepository.updateDeploymentResources(
                        rec.getNamespace(),
                        deploymentName,
                        containerName,
                        rec.getRecommendedCpuRequest(),
                        null, // CPU limit unchanged
                        rec.getRecommendedMemoryRequestGb(),
                        null  // Memory limit unchanged
                );
                break;

            case REDUCE_REPLICAS:
                prevConfig = String.format("{\"replicas\": %d}", rec.getCurrentReplicas());
                newConfig = String.format("{\"replicas\": %d}", rec.getRecommendedReplicas());
                
                clusterRepository.scaleDeployment(
                        rec.getNamespace(),
                        rec.getResourceName(),
                        rec.getRecommendedReplicas()
                );
                break;

            case DRAIN_NODE:
                prevConfig = "{\"status\": \"active\"}";
                newConfig = "{\"status\": \"drained\"}";
                clusterRepository.drainNode(rec.getResourceName());
                break;

            default:
                throw new UnsupportedOperationException("Auto-application not implemented for rule: " + rec.getType());
        }

        // Save active audit record
        auditLogRepository.save(AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .username(getCurrentUser())
                .actionType("APPLY_RECOMMENDATION")
                .resourceName(rec.getResourceName())
                .resourceType(rec.getResourceType().name())
                .namespace(rec.getNamespace())
                .previousConfiguration(prevConfig)
                .newConfiguration(newConfig)
                .estimatedMonthlySavings(rec.getEstimatedMonthlySavings())
                .build());

        // Update recommendation status
        rec.setStatus(RecommendationStatus.IMPLEMENTED);
        rec.setUpdatedAt(LocalDateTime.now());
        recommendationRepository.save(rec);
    }

    @Transactional
    public void rollbackRecommendation(Long auditLogId) {
        AuditLog audit = auditLogRepository.findAll().stream()
                .filter(a -> a.getId().equals(auditLogId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Audit log not found with ID: " + auditLogId));

        if (!"APPLY_RECOMMENDATION".equals(audit.getActionType())) {
            throw new IllegalArgumentException("Only applied recommendations can be rolled back.");
        }

        log.info("User {} requested rollback of audit log entry ID {}", getCurrentUser(), auditLogId);

        // Parse previous specs and deploy back to Kubernetes
        if ("POD".equalsIgnoreCase(audit.getResourceType()) || "DEPLOYMENT".equalsIgnoreCase(audit.getResourceType())) {
            String[] parts = audit.getResourceName().split("/");
            String name = parts[0];
            String container = parts.length > 1 ? parts[1] : name;
            
            // Re-apply previous configuration
            try {
                // If it is resource updates
                if (audit.getPreviousConfiguration().contains("cpuRequest")) {
                    // Quick parsing of configuration values from JSON
                    double prevCpu = Double.parseDouble(audit.getPreviousConfiguration().split("cpuRequest\":")[1].split(",")[0].trim());
                    double prevMem = Double.parseDouble(audit.getPreviousConfiguration().split("memoryRequestGb\":")[1].split("}")[0].trim());
                    
                    clusterRepository.updateDeploymentResources(audit.getNamespace(), name, container, prevCpu, null, prevMem, null);
                } else if (audit.getPreviousConfiguration().contains("replicas")) {
                    int prevReplicas = Integer.parseInt(audit.getPreviousConfiguration().split("replicas\":")[1].split("}")[0].trim());
                    clusterRepository.scaleDeployment(audit.getNamespace(), name, prevReplicas);
                }
            } catch (Exception e) {
                log.error("Failed to parse configurations from audit log entry for rollback", e);
                throw new RuntimeException("Rollback failed due to configuration parse error: " + e.getMessage());
            }
        } else if ("NODE".equalsIgnoreCase(audit.getResourceType())) {
            clusterRepository.cordonNode(audit.getResourceName(), false); // Uncordon node
        }

        // Log rollback transaction
        auditLogRepository.save(AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .username(getCurrentUser())
                .actionType("ROLLBACK_RECOMMENDATION")
                .resourceName(audit.getResourceName())
                .resourceType(audit.getResourceType())
                .namespace(audit.getNamespace())
                .previousConfiguration(audit.getNewConfiguration())
                .newConfiguration(audit.getPreviousConfiguration())
                .estimatedMonthlySavings(-audit.getEstimatedMonthlySavings()) // Reverse savings count
                .build());
    }

    public List<AuditLog> getAuditLogs() {
        return auditLogRepository.findAll();
    }
}
