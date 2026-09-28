package com.k8s.costoptimizer.domain.repository;

import com.k8s.costoptimizer.domain.model.NodeResourceInfo;
import com.k8s.costoptimizer.domain.model.PodResourceInfo;

import java.util.List;

/**
 * Domain boundary port for querying resources and metrics from the Kubernetes cluster.
 */
public interface ClusterRepository {
    List<NodeResourceInfo> getNodes();
    List<PodResourceInfo> getPods();
    List<String> getNamespaces();
    int getDeploymentCount();
    int getDaemonSetCount();
    int getStatefulSetCount();
    int getReplicaSetCount();
    int getServiceCount();

    void scaleDeployment(String namespace, String name, int replicas);
    void restartDeployment(String namespace, String name);
    void deletePod(String namespace, String name);
    void cordonNode(String nodeName, boolean cordon);
    void drainNode(String nodeName);
    void updateDeploymentResources(String namespace, String name, String containerName, Double cpuRequest, Double cpuLimit, Double memRequestGb, Double memLimitGb);
}
