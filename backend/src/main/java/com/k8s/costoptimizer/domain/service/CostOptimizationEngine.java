package com.k8s.costoptimizer.domain.service;

import com.k8s.costoptimizer.domain.model.NodeResourceInfo;
import com.k8s.costoptimizer.domain.model.PodResourceInfo;
import com.k8s.costoptimizer.domain.model.Recommendation;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain engine class that evaluates a set of strategies to analyze and recommend cost optimizations.
 *
 * <h2>Engine Design</h2>
 * The engine applies the Strategy Pattern to separate recommendation logic into pluggable rules.
 * Rules are executed sequentially, and their results are aggregated into a single recommendation list.
 *
 * <h2>Temporal Analysis Integration</h2>
 * In addition to the original static-threshold rules, the engine now includes the
 * {@link TemporalAnomalyRule} which uses historical metric snapshots from the
 * {@link MetricHistoryStore} to make percentile-based, spike-aware recommendations.
 * The MetricHistoryStore is exposed as a public dependency so that the application
 * layer can feed it snapshots from each scan interval.
 */
public class CostOptimizationEngine {

    private final List<RecommendationRule> rules;
    private final MetricHistoryStore metricHistoryStore;

    public CostOptimizationEngine() {
        this.metricHistoryStore = new MetricHistoryStore();
        this.rules = List.of(
                new MissingRequestsOrLimitsRule(),
                new UnderutilizedPodRule(),
                new IdleNodeRule(),
                new OverprovisionedDeploymentRule(),
                new TemporalAnomalyRule(metricHistoryStore)
        );
    }

    public CostOptimizationEngine(List<RecommendationRule> customRules) {
        this.metricHistoryStore = new MetricHistoryStore();
        this.rules = customRules;
    }

    public CostOptimizationEngine(List<RecommendationRule> customRules, MetricHistoryStore historyStore) {
        this.metricHistoryStore = historyStore;
        this.rules = customRules;
    }

    /**
     * Executes all rules against current cluster data.
     */
    public List<Recommendation> generateRecommendations(
            List<PodResourceInfo> pods,
            List<NodeResourceInfo> nodes,
            double cpuRate,
            double memoryRate) {
        List<Recommendation> recommendations = new ArrayList<>();
        for (RecommendationRule rule : rules) {
            recommendations.addAll(rule.evaluate(pods, nodes, cpuRate, memoryRate));
        }
        return recommendations;
    }

    /**
     * Exposes the MetricHistoryStore so the application layer can record snapshots
     * and the presentation layer can serve temporal profiles to the frontend.
     */
    public MetricHistoryStore getMetricHistoryStore() {
        return metricHistoryStore;
    }
}
