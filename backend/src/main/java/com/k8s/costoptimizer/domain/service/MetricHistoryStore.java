package com.k8s.costoptimizer.domain.service;

import com.k8s.costoptimizer.domain.model.MetricSnapshot;
import com.k8s.costoptimizer.domain.model.TemporalProfile;
import com.k8s.costoptimizer.domain.model.TemporalProfile.WorkloadPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * In-memory sliding window store for per-container metric snapshots.
 *
 * <h2>Design Rationale</h2>
 * Traditional Kubernetes cost optimizers (including the existing rules in this project)
 * evaluate a SINGLE point-in-time metric snapshot to decide rightsizing. This produces
 * dangerously wrong recommendations for:
 * <ul>
 *   <li>Cron-job-like workloads that spike hourly/daily</li>
 *   <li>Web servers with diurnal traffic patterns (busy during business hours, idle at night)</li>
 *   <li>Batch processors that queue up work and burst periodically</li>
 * </ul>
 *
 * This store collects metrics across scan intervals and maintains a configurable sliding
 * window (default: 24h) of snapshots per resource. From this history, {@link #computeProfile}
 * produces percentile statistics (P50/P95/P99), burstiness coefficients, and hourly
 * heatmap data that enables the {@link TemporalAnomalyRule} to make spike-aware decisions.
 *
 * <h2>Storage</h2>
 * The current implementation is in-memory (ConcurrentHashMap) for simplicity. In a
 * production deployment this would be backed by a time-series database (InfluxDB, Prometheus
 * long-term storage, or TimescaleDB).
 */
public class MetricHistoryStore {

    private static final Logger log = LoggerFactory.getLogger(MetricHistoryStore.class);

    /** Maximum window to retain snapshots (hours) */
    private static final int WINDOW_HOURS = 24;

    /** Minimum number of snapshots required before a temporal profile is considered reliable */
    public static final int MIN_SNAPSHOTS_FOR_PROFILE = 6;

    /**
     * Map from resourceKey (namespace/pod/container) to ordered list of snapshots.
     * Thread-safe via ConcurrentHashMap + CopyOnWriteArrayList for safe concurrent reads during analysis.
     */
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<MetricSnapshot>> history = new ConcurrentHashMap<>();

    /**
     * Records a batch of snapshots from the latest scan interval.
     * Automatically evicts snapshots older than the retention window.
     */
    public void recordSnapshots(List<MetricSnapshot> snapshots) {
        for (MetricSnapshot snapshot : snapshots) {
            history.computeIfAbsent(snapshot.getResourceKey(), k -> new CopyOnWriteArrayList<>())
                    .add(snapshot);
        }
        evictStaleSnapshots();
        log.debug("Recorded {} snapshots. Total tracked resources: {}", snapshots.size(), history.size());
    }

    /**
     * Computes a statistical temporal profile for a given resource key.
     * Returns Optional.empty() if insufficient history exists.
     */
    public Optional<TemporalProfile> computeProfile(String resourceKey) {
        List<MetricSnapshot> snapshots = history.getOrDefault(resourceKey, new CopyOnWriteArrayList<>());

        if (snapshots.size() < MIN_SNAPSHOTS_FOR_PROFILE) {
            return Optional.empty();
        }

        double[] cpuValues = snapshots.stream().mapToDouble(MetricSnapshot::getCpuUsage).sorted().toArray();
        double[] memValues = snapshots.stream().mapToDouble(MetricSnapshot::getMemoryUsageGb).sorted().toArray();

        double cpuP50 = percentile(cpuValues, 50);
        double cpuP95 = percentile(cpuValues, 95);
        double cpuP99 = percentile(cpuValues, 99);
        double cpuMean = Arrays.stream(cpuValues).average().orElse(0.0);
        double cpuMax = Arrays.stream(cpuValues).max().orElse(0.0);

        double memP50 = percentile(memValues, 50);
        double memP95 = percentile(memValues, 95);
        double memP99 = percentile(memValues, 99);
        double memMean = Arrays.stream(memValues).average().orElse(0.0);
        double memMax = Arrays.stream(memValues).max().orElse(0.0);

        double cpuBurstiness = cpuMean > 0 ? cpuMax / cpuMean : 1.0;
        double memBurstiness = memMean > 0 ? memMax / memMean : 1.0;

        // Build hourly average arrays (24 buckets)
        double[] hourlyAvgCpu = new double[24];
        double[] hourlyAvgMem = new double[24];
        int[] hourlyCounts = new int[24];

        for (MetricSnapshot s : snapshots) {
            int hour = s.getHourOfDay();
            hourlyAvgCpu[hour] += s.getCpuUsage();
            hourlyAvgMem[hour] += s.getMemoryUsageGb();
            hourlyCounts[hour]++;
        }
        for (int h = 0; h < 24; h++) {
            if (hourlyCounts[h] > 0) {
                hourlyAvgCpu[h] /= hourlyCounts[h];
                hourlyAvgMem[h] /= hourlyCounts[h];
            }
        }

        // Classify workload pattern
        WorkloadPattern pattern = classifyPattern(cpuValues, cpuBurstiness, cpuMean, snapshots);

        MetricSnapshot latest = snapshots.get(snapshots.size() - 1);

        return Optional.of(TemporalProfile.builder()
                .resourceKey(resourceKey)
                .snapshotCount(snapshots.size())
                .cpuP50(cpuP50).cpuP95(cpuP95).cpuP99(cpuP99)
                .cpuMean(cpuMean).cpuMax(cpuMax)
                .cpuRequest(latest.getCpuRequest())
                .memP50(memP50).memP95(memP95).memP99(memP99)
                .memMean(memMean).memMax(memMax)
                .memRequestGb(latest.getMemoryRequestGb())
                .cpuBurstiness(cpuBurstiness)
                .memBurstiness(memBurstiness)
                .hourlyAvgCpu(hourlyAvgCpu)
                .hourlyAvgMem(hourlyAvgMem)
                .pattern(pattern)
                .build());
    }

    /**
     * Returns all computed temporal profiles for resources that have sufficient history.
     */
    public List<TemporalProfile> getAllProfiles() {
        return history.keySet().stream()
                .map(this::computeProfile)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    /**
     * Returns the total count of tracked resources.
     */
    public int getTrackedResourceCount() {
        return history.size();
    }

    // --- Internal Methods ---

    private void evictStaleSnapshots() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(WINDOW_HOURS);
        for (Map.Entry<String, CopyOnWriteArrayList<MetricSnapshot>> entry : history.entrySet()) {
            entry.getValue().removeIf(s -> s.getTimestamp().isBefore(cutoff));
            if (entry.getValue().isEmpty()) {
                history.remove(entry.getKey());
            }
        }
    }

    /**
     * Computes the Nth percentile from a SORTED array of doubles using linear interpolation.
     */
    private double percentile(double[] sorted, int pct) {
        if (sorted.length == 0) return 0.0;
        if (sorted.length == 1) return sorted[0];
        double index = (pct / 100.0) * (sorted.length - 1);
        int lower = (int) Math.floor(index);
        int upper = Math.min(lower + 1, sorted.length - 1);
        double weight = index - lower;
        return sorted[lower] * (1 - weight) + sorted[upper] * weight;
    }

    /**
     * Classifies workload behavior based on statistical characteristics of the time series.
     */
    private WorkloadPattern classifyPattern(double[] cpuValues, double burstiness, double mean, List<MetricSnapshot> snapshots) {
        // IDLE: mean CPU usage under 1% of a core
        if (mean < 0.01) {
            return WorkloadPattern.IDLE;
        }

        // BURSTY: peak-to-mean ratio exceeds 3x
        if (burstiness > 3.0) {
            return WorkloadPattern.BURSTY;
        }

        // DIURNAL: check if there's a significant variance between hourly buckets
        double[] hourlyTotals = new double[24];
        int[] hourlyCounts = new int[24];
        for (MetricSnapshot s : snapshots) {
            int h = s.getHourOfDay();
            hourlyTotals[h] += s.getCpuUsage();
            hourlyCounts[h]++;
        }
        double[] hourlyAvg = new double[24];
        int populated = 0;
        for (int i = 0; i < 24; i++) {
            if (hourlyCounts[i] > 0) {
                hourlyAvg[i] = hourlyTotals[i] / hourlyCounts[i];
                populated++;
            }
        }
        if (populated >= 4) {
            double hourlyMean = Arrays.stream(hourlyAvg).filter(v -> v > 0).average().orElse(0);
            double hourlyMax = Arrays.stream(hourlyAvg).max().orElse(0);
            if (hourlyMean > 0 && (hourlyMax / hourlyMean) > 2.0) {
                return WorkloadPattern.DIURNAL;
            }
        }

        // GROWING: check if the latter half of snapshots has noticeably higher mean
        if (cpuValues.length >= 12) {
            int half = cpuValues.length / 2;
            double firstHalfMean = Arrays.stream(cpuValues, 0, half).average().orElse(0);
            double secondHalfMean = Arrays.stream(cpuValues, half, cpuValues.length).average().orElse(0);
            if (firstHalfMean > 0 && (secondHalfMean / firstHalfMean) > 1.5) {
                return WorkloadPattern.GROWING;
            }
        }

        return WorkloadPattern.STABLE;
    }
}
