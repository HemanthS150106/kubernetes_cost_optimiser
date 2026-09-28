# Kubernetes Cost Optimizer — Novelty: Temporal Anomaly Detection for Spike-Aware Rightsizing

> **One-liner:** *Developing a Kubernetes Cost Optimizer that uses **percentile-based temporal anomaly detection** to eliminate the critical false-positive problem in static-threshold resource rightsizing — preventing OOMKills and CPU throttling caused by recommendations that ignore workload spike patterns.*

---

## 1. Problem Statement

Cloud-native Kubernetes clusters routinely **overprovision resources by 50-65%** (Datadog State of Kubernetes 2024), directly inflating infrastructure costs. Existing cost optimization tools (Kubecost, CAST AI, GKE Recommender) analyze resource utilization at a **single point-in-time snapshot** and compare it against static thresholds (e.g., "if CPU usage < 25% of request, recommend downsizing").

### The Critical Flaw: Snapshot-Based Analysis

| Scenario | What Happens with Static Analysis | Real-World Consequence |
|---|---|---|
| Cron job scanned during its idle window | "Reduce CPU from 2 to 0.1 cores" | Job fails on next hourly burst — service outage |
| Web server scanned at 3 AM | "Reduce memory from 4GB to 0.5GB" | OOMKill during 9 AM traffic surge |
| Batch processor between queue bursts | "Pod is idle, remove resources" | Queue pile-up, cascading latency spikes |
| Weekend scan of weekday-heavy service | "Overprovisioned, downsize aggressively" | Monday morning crashes |

**The fundamental flaw is that a single metric reading tells you nothing about a workload's behavioral pattern.** A pod at 5% CPU right now could be at 95% in an hour. Static-threshold optimizers cannot distinguish between a truly idle resource and one that's between spikes.

---

## 2. Our Novel Contribution: Temporal Anomaly Detection Engine

We introduce a **Time-Series Aware Optimization Layer** that sits alongside the existing static rules and addresses their limitations by:

### 2.1 Sliding Window Metric Collection
- Each 5-minute scan records per-container CPU/Memory snapshots into an **in-memory 24-hour sliding window** (`MetricHistoryStore`)
- Snapshots are timestamped and indexed by hour-of-day for temporal bucketing
- Automatic eviction of stale data beyond the retention window

### 2.2 Percentile-Based Statistical Profiling
Instead of comparing instantaneous usage against a threshold, we compute:

| Metric | Purpose |
|---|---|
| **P50 (Median)** | Typical sustained usage |
| **P95 (95th Percentile)** | The "safe" value — covers 95% of observed behavior |
| **P99 (99th Percentile)** | Near-peak usage, for critical workloads |
| **Burstiness Coefficient** | Peak-to-Mean ratio — distinguishes flat workloads from spiky ones |
| **Hourly Heatmap** | 24-bucket per-hour average usage for visualizing diurnal patterns |

**Key insight:** We use P95 utilization (not mean, not instantaneous) as the basis for rightsizing recommendations. This means the recommended resource value survives 95% of the workload's observed behavior.

### 2.3 Workload Pattern Classification
The engine classifies each container into one of five behavioral patterns:

```
┌─────────────┬──────────────────────────────────────────────────────────┐
│ STABLE      │ Flat usage. Safe to rightsize aggressively (30% headroom)│
│ DIURNAL     │ Day/night cycle. Rightsize with 50% headroom             │
│ BURSTY      │ Unpredictable spikes. 80% headroom or skip entirely     │
│ GROWING     │ Upward trend. Do NOT reduce resources                   │
│ IDLE        │ Near-zero sustained usage. Candidate for deletion       │
└─────────────┴──────────────────────────────────────────────────────────┘
```

### 2.4 Pattern-Aware Safety Headroom
Each pattern gets a different safety multiplier applied on top of P95 usage:

```
Recommended CPU = P95_Usage × Headroom_Multiplier

Where Headroom_Multiplier =
    1.3 (30%)  for STABLE workloads
    1.5 (50%)  for DIURNAL workloads
    1.8 (80%)  for BURSTY workloads
    N/A        for GROWING (skip recommendation)
```

This eliminates the one-size-fits-all threshold that causes false positives.

---

## 3. Architecture Integration

```
                        ┌─────────────────────────────┐
                        │   ClusterAnalysisUseCase     │
                        │  (5-min scheduled scan)      │
                        └──────┬──────────┬────────────┘
                               │          │
                    ┌──────────▼──┐  ┌────▼─────────────┐
                    │ Static Rules │  │ recordSnapshots() │
                    │ (existing)   │  │ ──▶ MetricHistory │
                    └──────────┬──┘  │     Store (24h)   │
                               │     └────┬──────────────┘
                               │          │
                               │  ┌───────▼───────────────┐
                               │  │ TemporalAnomalyRule    │
                               │  │ ├─ computeProfile()    │
                               │  │ ├─ P95 threshold check │
                               │  │ ├─ Pattern classify    │
                               │  │ └─ Headroom-aware rec  │
                               │  └───────┬───────────────┘
                               │          │
                         ┌─────▼──────────▼──────┐
                         │ Merged Recommendations │
                         │  (Static + Temporal)   │
                         └────────────────────────┘
```

### New Files Added

| Layer | File | Purpose |
|---|---|---|
| **Domain Model** | `MetricSnapshot.java` | Immutable point-in-time telemetry record |
| **Domain Model** | `TemporalProfile.java` | Statistical profile with percentiles, burstiness, hourly heatmap |
| **Domain Service** | `MetricHistoryStore.java` | In-memory sliding window store with eviction |
| **Domain Service** | `TemporalAnomalyRule.java` | The novel `RecommendationRule` strategy implementation |
| **Presentation** | `TemporalProfileController.java` | REST endpoint for profiles & summary |
| **Frontend** | `TemporalProfiles.tsx` | Dashboard page with heatmaps and pattern visualizations |

### Modified Files

| File | Change |
|---|---|
| `RecommendationType.java` | Added `TEMPORAL_CPU_RIGHTSIZE`, `TEMPORAL_MEM_RIGHTSIZE` |
| `CostOptimizationEngine.java` | Registered `TemporalAnomalyRule` as 5th strategy |
| `ClusterAnalysisUseCase.java` | Added snapshot recording on each scan, exposed profile queries |
| `api.ts` | Added `temporalService` with profile/summary endpoints |
| `App.tsx` | Added `/temporal-profiles` route |
| `Layout.tsx` | Added sidebar navigation for Temporal Profiles |

---

## 4. Technical Depth & Academic Rigor

### Research Foundation
- **Google Autopilot (EuroSys 2020):** Uses exponential histograms of resource usage for vertical autoscaling decisions — our percentile approach is a simplified variant
- **Kubernetes VPA Algorithm:** The official Vertical Pod Autoscaler uses decaying histograms with configurable percentile targets (default P95) — we adopt this principle for advisory recommendations
- **Datadog State of K8s 2024:** Reports 65% of containers have limits set >2x actual usage — validates the market need

### Design Patterns Used
1. **Strategy Pattern** — `TemporalAnomalyRule` implements the same `RecommendationRule` interface as all other rules, making it hot-pluggable
2. **Sliding Window** — 24-hour time-bounded retention with automatic eviction
3. **Statistical Profiling** — Percentile computation using sorted-array interpolation (O(n log n))

### What Makes This Novel vs Existing Tools

| Feature | Kubecost / CAST AI | Our Implementation |
|---|---|---|
| Metric Source | Single snapshot or 1h average | 24h sliding window with P95 |
| Spike Awareness | ❌ None | ✅ Burstiness coefficient |
| Pattern Classification | ❌ None | ✅ STABLE/DIURNAL/BURSTY/GROWING/IDLE |
| Adaptive Headroom | ❌ Fixed % | ✅ Pattern-dependent (30-80%) |
| Frontend Heatmap | ❌ Basic charts | ✅ Per-container 24h CPU/Mem heatmap |
| False Positive Prevention | ❌ Manual review required | ✅ Automatic via temporal gating |

---

## 5. Viability Argument

### Why This Project Is Production-Viable

1. **Real Problem, Massive Market:** The FinOps Foundation estimates $32B in annual Kubernetes cloud waste. Even a 10% reduction justifies the tool.

2. **Immediate Demonstrability:** The project runs against any local Minikube cluster. Deploy a sample cron job + a stable web server, run 6+ scans, and the temporal profiles page immediately shows different classifications and recommendations.

3. **Non-Destructive:** Unlike auto-scalers, this tool is **advisory-only** with manual apply/rollback — zero risk of breaking production.

4. **Scalable Architecture:** The in-memory MetricHistoryStore is designed with a clear interface that could be swapped for TimescaleDB/InfluxDB in production without changing the domain logic.

5. **Clean Architecture Compliance:** The temporal analysis follows the same Clean Architecture layers (Domain → Application → Infrastructure → Presentation) as the existing codebase, maintaining architectural integrity.

---

## 6. Demo Flow for Presentation

1. **Show Dashboard** → Point out that current recommendations use static thresholds
2. **Deploy a bursty workload** → `kubectl run stress --image=busybox -- sh -c 'while true; do dd if=/dev/urandom of=/dev/null bs=1M count=100; sleep 300; done'`
3. **Run 6-8 manual scans** from the Dashboard (or wait 30 min for scheduled scans)
4. **Navigate to Temporal Profiles** → Show the heatmap, burstiness coefficient, and pattern classification
5. **Compare Recommendations** → Show how the static rule says "reduce CPU" but the temporal rule either skips the recommendation entirely (BURSTY pattern) or recommends a much more conservative value with 80% headroom
6. **Conclusion:** "Without temporal analysis, following the static recommendation would have caused service degradation. Our engine prevented a false positive."

---

## 7. Resume Bullet Point

> **Temporal Anomaly Detection Engine**: *Designed and implemented a percentile-based temporal anomaly detection layer for Kubernetes resource rightsizing, using 24-hour sliding window metric collection, P95 utilization thresholds, workload pattern classification (STABLE/DIURNAL/BURSTY/GROWING/IDLE), and pattern-aware safety headroom — eliminating false-positive rightsizing recommendations that cause OOMKills and CPU throttling in production clusters.*

---

*Project Status: Core temporal analysis engine is implemented and integrated. The project is intentionally not fully completed — the temporal store uses in-memory storage (production would use a time-series database), and the frontend heatmap can be extended with click-to-drill-down. These are clear, documented extension points for future work.*
