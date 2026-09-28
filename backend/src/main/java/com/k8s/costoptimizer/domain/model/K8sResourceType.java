package com.k8s.costoptimizer.domain.model;

/**
 * Types of resources inside Kubernetes.
 */
public enum K8sResourceType {
    POD,
    DEPLOYMENT,
    REPLICASET,
    STATEFULSET,
    DAEMONSET,
    SERVICE,
    NODE,
    NAMESPACE
}
