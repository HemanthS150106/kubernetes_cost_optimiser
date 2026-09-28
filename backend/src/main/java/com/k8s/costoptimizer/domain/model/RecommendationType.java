package com.k8s.costoptimizer.domain.model;

/**
 * Types of cost optimization recommendations.
 */
public enum RecommendationType {
    REDUCE_CPU_REQUEST("Reduce CPU Request"),
    REDUCE_MEM_REQUEST("Reduce Memory Request"),
    REDUCE_REPLICAS("Reduce Replicas"),
    DELETE_UNUSED_SERVICE("Delete Unused Service"),
    DRAIN_NODE("Drain Node / Downscale Node Group"),
    CONSOLIDATE_WORKLOADS("Consolidate Workloads"),
    ADD_CPU_LIMIT("Add CPU Limit"),
    ADD_MEM_LIMIT("Add Memory Limit"),
    ADD_CPU_REQUEST("Add CPU Request"),
    ADD_MEM_REQUEST("Add Memory Request"),
    TEMPORAL_CPU_RIGHTSIZE("Temporal CPU Rightsize"),
    TEMPORAL_MEM_RIGHTSIZE("Temporal Memory Rightsize");

    private final String displayName;

    RecommendationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
