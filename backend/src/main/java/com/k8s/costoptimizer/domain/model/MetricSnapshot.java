package com.k8s.costoptimizer.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Immutable point-in-time telemetry snapshot for a specific container within a pod.
 * Collected at each scan interval and retained in a sliding window for temporal analysis.
 *
 * This is the foundational data structure that powers the Temporal Anomaly Detection engine —
 * by preserving a time series of snapshots, the system can compute percentile-based utilization
 * profiles (P50/P95/P99) rather than relying on instantaneous point-in-time readings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricSnapshot {

    /** Composite key: namespace/pod/container */
    private String resourceKey;

    /** Wall-clock timestamp when this snapshot was captured */
    private LocalDateTime timestamp;

    /** CPU usage in cores at snapshot time */
    private double cpuUsage;

    /** Memory usage in GB at snapshot time */
    private double memoryUsageGb;

    /** CPU request configured for this container */
    private double cpuRequest;

    /** Memory request (GB) configured for this container */
    private double memoryRequestGb;

    /**
     * Derived hour-of-day (0-23) for temporal bucketing.
     * Used to construct hourly usage heatmaps and detect diurnal patterns.
     */
    public int getHourOfDay() {
        return timestamp != null ? timestamp.getHour() : 0;
    }

    /**
     * Derived day-of-week (1=Monday, 7=Sunday) for weekly pattern detection.
     */
    public int getDayOfWeek() {
        return timestamp != null ? timestamp.getDayOfWeek().getValue() : 1;
    }
}
