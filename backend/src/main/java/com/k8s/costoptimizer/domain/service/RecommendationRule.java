package com.k8s.costoptimizer.domain.service;

import com.k8s.costoptimizer.domain.model.NodeResourceInfo;
import com.k8s.costoptimizer.domain.model.PodResourceInfo;
import com.k8s.costoptimizer.domain.model.Recommendation;

import java.util.List;

/**
 * Strategy interface for evaluation rules that analyze cluster state and emit recommendations.
 */
public interface RecommendationRule {
    List<Recommendation> evaluate(List<PodResourceInfo> pods, List<NodeResourceInfo> nodes, double cpuRate, double memoryRate);
}
