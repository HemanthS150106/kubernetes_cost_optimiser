package com.k8s.costoptimizer.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Computed statistical profile of a workload's resource usage over a sliding time window.
 * 
 * Unlike a single point-in-time metric read, this captures behavioral characteristics:
 * - Percentile-based usage (P50, P95, P99) to distinguish sustained waste from bursty workloads
 * - Peak-to-mean ratio (burstiness coefficient) to classify workload patterns
 * - Per-hour average usage array for constructing temporal heatmaps
 *
 * This profile is what eliminates false-positive rightsizing recommendations — a pod
 * that averages 5% CPU but spikes to 80% hourly is NOT the same as one that's flat at 5%.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemporalProfile {

    private String resourceKey;
    private int snapshotCount;

    // --- CPU Statistics ---
    private double cpuP50;
    private double cpuP95;
    private double cpuP99;
    private double cpuMean;
    private double cpuMax;
    private double cpuRequest;

    // --- Memory Statistics ---
    private double memP50;
    private double memP95;
    private double memP99;
    private double memMean;
    private double memMax;
    private double memRequestGb;

    /**
     * Burstiness coefficient: ratio of peak usage to mean usage.
     * - Coefficient near 1.0 → flat/stable workload (safe to rightsize aggressively)
     * - Coefficient > 3.0 → bursty workload (dangerous to rightsize based on mean)
     * - Coefficient > 5.0 → highly spiky workload (do NOT rightsize)
     */
    private double cpuBurstiness;
    private double memBurstiness;

    /**
     * Per-hour average CPU usage (index 0 = midnight, index 23 = 11 PM).
     * Used to render the temporal heatmap on the frontend.
     */
    private double[] hourlyAvgCpu;
    private double[] hourlyAvgMem;

    /**
     * Classification of the workload's temporal behavior.
     */
    public enum WorkloadPattern {
        STABLE,       // Flat usage, safe to rightsize
        DIURNAL,      // Predictable day/night pattern
        BURSTY,       // Unpredictable spikes
        GROWING,      // Sustained upward trend
        IDLE          // Consistently near-zero usage
    }

    private WorkloadPattern pattern;
}
