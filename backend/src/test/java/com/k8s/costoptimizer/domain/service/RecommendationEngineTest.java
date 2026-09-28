package com.k8s.costoptimizer.domain.service;

import com.k8s.costoptimizer.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RecommendationEngineTest {

    private CostOptimizationEngine engine;
    private double cpuRate;
    private double memoryRate;

    @BeforeEach
    public void setUp() {
        engine = new CostOptimizationEngine();
        cpuRate = 0.05;      // $0.05 per core hour
        memoryRate = 0.005;  // $0.005 per GB hour
    }

    @Test
    public void testMissingRequestsAndLimitsRule() {
        // Container lacking CPU requests and limits
        ContainerMetric container = ContainerMetric.builder()
                .name("web-container")
                .cpuRequest(0.0)
                .cpuLimit(0.0)
                .cpuUsage(0.01)
                .memoryRequestGb(0.256)
                .memoryLimitGb(0.512)
                .memoryUsageGb(0.050)
                .build();

        PodResourceInfo pod = PodResourceInfo.builder()
                .name("web-pod-xyz")
                .namespace("production")
                .status("Running")
                .controllerType("Deployment")
                .controllerName("web-deployment")
                .containers(List.of(container))
                .build();

        List<Recommendation> recommendations = engine.generateRecommendations(
                List.of(pod),
                Collections.emptyList(),
                cpuRate,
                memoryRate
        );

        assertNotNull(recommendations);
        // Should flag ADD_CPU_REQUEST and ADD_CPU_LIMIT
        boolean hasCpuRequestRec = recommendations.stream()
                .anyMatch(r -> r.getType() == RecommendationType.ADD_CPU_REQUEST);
        boolean hasCpuLimitRec = recommendations.stream()
                .anyMatch(r -> r.getType() == RecommendationType.ADD_CPU_LIMIT);

        assertTrue(hasCpuRequestRec, "Should recommend adding CPU request");
        assertTrue(hasCpuLimitRec, "Should recommend adding CPU limit");
    }

    @Test
    public void testUnderutilizedPodRule() {
        // Pod with 1.0 CPU requested but only using 0.05 CPU (5% utilization)
        ContainerMetric container = ContainerMetric.builder()
                .name("database-container")
                .cpuRequest(1.0)
                .cpuLimit(2.0)
                .cpuUsage(0.05)
                .memoryRequestGb(2.0)
                .memoryLimitGb(4.0)
                .memoryUsageGb(1.8) // memory is fine
                .build();

        PodResourceInfo pod = PodResourceInfo.builder()
                .name("db-pod-123")
                .namespace("staging")
                .status("Running")
                .controllerType("StatefulSet")
                .controllerName("db-statefulset")
                .containers(List.of(container))
                .build();

        List<Recommendation> recommendations = engine.generateRecommendations(
                List.of(pod),
                Collections.emptyList(),
                cpuRate,
                memoryRate
        );

        assertNotNull(recommendations);
        // Should flag REDUCE_CPU_REQUEST
        boolean hasReduceCpu = recommendations.stream()
                .anyMatch(r -> r.getType() == RecommendationType.REDUCE_CPU_REQUEST);

        assertTrue(hasReduceCpu, "Should recommend reducing CPU request");
        
        Recommendation cpuRec = recommendations.stream()
                .filter(r -> r.getType() == RecommendationType.REDUCE_CPU_REQUEST)
                .findFirst()
                .orElseThrow();
        
        assertTrue(cpuRec.getEstimatedMonthlySavings() > 0, "Savings should be calculated");
        assertEquals(1.0, cpuRec.getCurrentCpuRequest());
        assertEquals(0.075, cpuRec.getRecommendedCpuRequest(), 0.001); // 0.05 * 1.5 = 0.075 cores
    }

    @Test
    public void testIdleNodeRule() {
        // Two nodes to bypass single-node suppression check
        NodeResourceInfo node1 = NodeResourceInfo.builder()
                .name("node-control-plane")
                .role("control-plane")
                .status("Ready")
                .cpuAllocatable(2.0)
                .cpuUsage(1.5)
                .memoryAllocatableGb(8.0)
                .memoryUsageGb(6.0)
                .podCount(10)
                .build();

        // Second node is idle (10% CPU and memory utilization, 2 pods running)
        NodeResourceInfo node2 = NodeResourceInfo.builder()
                .name("node-worker-idle")
                .role("worker")
                .status("Ready")
                .cpuAllocatable(4.0)
                .cpuUsage(0.2) // 5%
                .memoryAllocatableGb(16.0)
                .memoryUsageGb(1.0) // ~6%
                .podCount(2)
                .build();

        List<Recommendation> recommendations = engine.generateRecommendations(
                Collections.emptyList(),
                List.of(node1, node2),
                cpuRate,
                memoryRate
        );

        assertNotNull(recommendations);
        boolean hasDrainNode = recommendations.stream()
                .anyMatch(r -> r.getType() == RecommendationType.DRAIN_NODE);

        assertTrue(hasDrainNode, "Should recommend draining node2");
        Recommendation drainRec = recommendations.stream()
                .filter(r -> r.getType() == RecommendationType.DRAIN_NODE)
                .findFirst()
                .orElseThrow();
        
        assertEquals("node-worker-idle", drainRec.getResourceName());
        // Estimated monthly savings: (4 * 0.05 + 16 * 0.005) * 730 = (0.2 + 0.08) * 730 = 0.28 * 730 = $204.4
        assertEquals(204.4, drainRec.getEstimatedMonthlySavings(), 0.01);
    }
}
