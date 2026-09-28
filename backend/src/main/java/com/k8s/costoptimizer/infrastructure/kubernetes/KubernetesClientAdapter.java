package com.k8s.costoptimizer.infrastructure.kubernetes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.k8s.costoptimizer.domain.model.ContainerMetric;
import com.k8s.costoptimizer.domain.model.NodeResourceInfo;
import com.k8s.costoptimizer.domain.model.PodResourceInfo;
import com.k8s.costoptimizer.domain.repository.ClusterRepository;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.openapi.models.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Concrete infrastructure adapter implementing the domain ClusterRepository.
 * Interacts with the Kubernetes API to gather node, pod, and utilization metrics.
 */
@Component
@RequiredArgsConstructor
public class KubernetesClientAdapter implements ClusterRepository {

    private static final Logger log = LoggerFactory.getLogger(KubernetesClientAdapter.class);
    
    private final CoreV1Api coreV1Api;
    private final AppsV1Api appsV1Api;
    private final CustomObjectsApi customObjectsApi;
    private final ObjectMapper jacksonObjectMapper = new ObjectMapper();

    @Override
    public List<NodeResourceInfo> getNodes() {
        List<NodeResourceInfo> nodeInfos = new ArrayList<>();
        try {
            V1NodeList nodeList = coreV1Api.listNode(null, null, null, null, null, null, null, null, null, null, null);
            Map<String, Map<String, Double>> nodeUsage = fetchNodeLiveMetrics();

            for (V1Node node : nodeList.getItems()) {
                String name = node.getMetadata().getName();
                
                // Get roles
                String role = "worker";
                if (node.getMetadata().getLabels() != null) {
                    if (node.getMetadata().getLabels().containsKey("node-role.kubernetes.io/control-plane") ||
                        node.getMetadata().getLabels().containsKey("node-role.kubernetes.io/master")) {
                        role = "control-plane";
                    }
                }

                // Get status
                String status = "NotReady";
                if (node.getStatus() != null && node.getStatus().getConditions() != null) {
                    for (V1NodeCondition cond : node.getStatus().getConditions()) {
                        if ("Ready".equals(cond.getType()) && "True".equals(cond.getStatus())) {
                            status = "Ready";
                            break;
                        }
                    }
                }
                if (node.getSpec() != null && Boolean.TRUE.equals(node.getSpec().getUnschedulable())) {
                    status = "SchedulingDisabled";
                }

                // Node capacity and allocatable resources
                double cpuCap = 0.0;
                double cpuAlloc = 0.0;
                double memCapGb = 0.0;
                double memAllocGb = 0.0;

                if (node.getStatus() != null) {
                    Map<String, Quantity> capacity = node.getStatus().getCapacity();
                    Map<String, Quantity> allocatable = node.getStatus().getAllocatable();

                    if (capacity != null) {
                        if (capacity.containsKey("cpu")) cpuCap = capacity.get("cpu").getNumber().doubleValue();
                        if (capacity.containsKey("memory")) memCapGb = parseMemoryToGb(capacity.get("memory"));
                    }
                    if (allocatable != null) {
                        if (allocatable.containsKey("cpu")) cpuAlloc = allocatable.get("cpu").getNumber().doubleValue();
                        if (allocatable.containsKey("memory")) memAllocGb = parseMemoryToGb(allocatable.get("memory"));
                    }
                }

                // Live usage
                double cpuUse = 0.0;
                double memUseGb = 0.0;
                if (nodeUsage.containsKey(name)) {
                    cpuUse = nodeUsage.get(name).getOrDefault("cpu", 0.0);
                    memUseGb = nodeUsage.get(name).getOrDefault("memory", 0.0);
                } else {
                    // Failover mock usage (10% - 25% of allocatable)
                    cpuUse = cpuAlloc * 0.12;
                    memUseGb = memAllocGb * 0.22;
                }

                // Node monthly baseline cost (simulated based on size: $35 per CPU, $4.5 per GB memory)
                double nodeCost = (cpuAlloc * 35.0) + (memAllocGb * 4.5);

                nodeInfos.add(NodeResourceInfo.builder()
                        .name(name)
                        .status(status)
                        .role(role)
                        .labels(node.getMetadata().getLabels())
                        .cpuCapacity(cpuCap)
                        .cpuAllocatable(cpuAlloc)
                        .cpuUsage(cpuUse)
                        .memoryCapacityGb(memCapGb)
                        .memoryAllocatableGb(memAllocGb)
                        .memoryUsageGb(memUseGb)
                        .podCount(0) // Will be populated when pods are linked
                        .estimatedMonthlyCost(nodeCost)
                        .build());
            }

            // Link pod counts to nodes
            List<PodResourceInfo> pods = getPods();
            for (NodeResourceInfo n : nodeInfos) {
                long count = pods.stream().filter(p -> n.getName().equalsIgnoreCase(p.getNodeName())).count();
                n.setPodCount((int) count);
            }

        } catch (ApiException e) {
            log.error("Kubernetes API error fetching nodes: {} (Code: {})", e.getResponseBody(), e.getCode());
        } catch (Exception e) {
            log.error("Unexpected error fetching nodes", e);
        }
        return nodeInfos;
    }

    @Override
    public List<PodResourceInfo> getPods() {
        List<PodResourceInfo> podInfos = new ArrayList<>();
        try {
            V1PodList podList = coreV1Api.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null, null, null);
            Map<String, Map<String, Map<String, Double>>> podUsage = fetchPodLiveMetrics();

            for (V1Pod pod : podList.getItems()) {
                String name = pod.getMetadata().getName();
                String ns = pod.getMetadata().getNamespace();
                String nodeName = pod.getSpec() != null ? pod.getSpec().getNodeName() : null;
                String status = pod.getStatus() != null ? pod.getStatus().getPhase() : "Unknown";

                // Controller info
                String controllerType = "None";
                String controllerName = "None";
                if (pod.getMetadata().getOwnerReferences() != null && !pod.getMetadata().getOwnerReferences().isEmpty()) {
                    V1OwnerReference owner = pod.getMetadata().getOwnerReferences().get(0);
                    controllerType = owner.getKind();
                    controllerName = owner.getName();
                    
                    // Trace back from ReplicaSet to Deployment
                    if ("ReplicaSet".equalsIgnoreCase(controllerType)) {
                        controllerType = "Deployment";
                        if (controllerName.contains("-")) {
                            controllerName = controllerName.substring(0, controllerName.lastIndexOf("-"));
                        }
                    }
                }

                // Compute age
                long age = 0;
                if (pod.getMetadata().getCreationTimestamp() != null) {
                    age = (System.currentTimeMillis() - pod.getMetadata().getCreationTimestamp().toInstant().toEpochMilli()) / 1000;
                }

                List<ContainerMetric> containers = new ArrayList<>();
                if (pod.getSpec() != null && pod.getSpec().getContainers() != null) {
                    for (V1Container container : pod.getSpec().getContainers()) {
                        String cName = container.getName();
                        
                        double cpuReq = 0.0;
                        double cpuLim = 0.0;
                        double memReqGb = 0.0;
                        double memLimGb = 0.0;

                        if (container.getResources() != null) {
                            Map<String, Quantity> requests = container.getResources().getRequests();
                            Map<String, Quantity> limits = container.getResources().getLimits();

                            if (requests != null) {
                                if (requests.containsKey("cpu")) cpuReq = requests.get("cpu").getNumber().doubleValue();
                                if (requests.containsKey("memory")) memReqGb = parseMemoryToGb(requests.get("memory"));
                            }
                            if (limits != null) {
                                if (limits.containsKey("cpu")) cpuLim = limits.get("cpu").getNumber().doubleValue();
                                if (limits.containsKey("memory")) memLimGb = parseMemoryToGb(limits.get("memory"));
                            }
                        }

                        // Usage from metrics server
                        double cpuUse = 0.0;
                        double memUseGb = 0.0;
                        String metricKey = ns + "/" + name;
                        
                        if (podUsage.containsKey(metricKey) && podUsage.get(metricKey).containsKey(cName)) {
                            cpuUse = podUsage.get(metricKey).get(cName).getOrDefault("cpu", 0.0);
                            memUseGb = podUsage.get(metricKey).get(cName).getOrDefault("memory", 0.0);
                        } else {
                            // Fallback simulation: CPU is 10% of request, Mem is 20% of request
                            // If requests are not configured, simulate baseline workloads
                            cpuUse = cpuReq > 0 ? cpuReq * 0.08 : 0.01;
                            memUseGb = memReqGb > 0 ? memReqGb * 0.18 : 0.05;
                        }

                        containers.add(ContainerMetric.builder()
                                .name(cName)
                                .cpuRequest(cpuReq)
                                .cpuLimit(cpuLim)
                                .cpuUsage(cpuUse)
                                .memoryRequestGb(memReqGb)
                                .memoryLimitGb(memLimGb)
                                .memoryUsageGb(memUseGb)
                                .build());
                    }
                }

                podInfos.add(PodResourceInfo.builder()
                        .name(name)
                        .namespace(ns)
                        .nodeName(nodeName)
                        .status(status)
                        .controllerType(controllerType)
                        .controllerName(controllerName)
                        .labels(pod.getMetadata().getLabels())
                        .containers(containers)
                        .ageInSeconds(age)
                        .build());
            }
        } catch (ApiException e) {
            log.error("Kubernetes API error fetching pods: {} (Code: {})", e.getResponseBody(), e.getCode());
        } catch (Exception e) {
            log.error("Unexpected error fetching pods", e);
        }
        return podInfos;
    }

    @Override
    public List<String> getNamespaces() {
        List<String> namespaces = new ArrayList<>();
        try {
            V1NamespaceList nsList = coreV1Api.listNamespace(null, null, null, null, null, null, null, null, null, null, null);
            for (V1Namespace ns : nsList.getItems()) {
                namespaces.add(ns.getMetadata().getName());
            }
        } catch (Exception e) {
            log.error("Failed to fetch namespaces, returning default List", e);
            namespaces.addAll(List.of("default", "kube-system", "kube-public"));
        }
        return namespaces;
    }

    @Override
    public int getDeploymentCount() {
        try {
            return appsV1Api.listDeploymentForAllNamespaces(null, null, null, null, null, null, null, null, null, null, null).getItems().size();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public int getDaemonSetCount() {
        try {
            return appsV1Api.listDaemonSetForAllNamespaces(null, null, null, null, null, null, null, null, null, null, null).getItems().size();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public int getStatefulSetCount() {
        try {
            return appsV1Api.listStatefulSetForAllNamespaces(null, null, null, null, null, null, null, null, null, null, null).getItems().size();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public int getReplicaSetCount() {
        try {
            return appsV1Api.listReplicaSetForAllNamespaces(null, null, null, null, null, null, null, null, null, null, null).getItems().size();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public int getServiceCount() {
        try {
            return coreV1Api.listServiceForAllNamespaces(null, null, null, null, null, null, null, null, null, null, null).getItems().size();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Queries Node Metrics from Metrics Server.
     * Returns: Map<NodeName, Map<"cpu"|"memory", Value>>
     */
    private Map<String, Map<String, Double>> fetchNodeLiveMetrics() {
        Map<String, Map<String, Double>> usageMap = new HashMap<>();
        try {
            Object rawObj = customObjectsApi.getClusterCustomObject("metrics.k8s.io", "v1beta1", "nodes", null);
            String jsonStr = jacksonObjectMapper.writeValueAsString(rawObj);
            JsonNode root = jacksonObjectMapper.readTree(jsonStr);
            JsonNode items = root.get("items");

            if (items != null && items.isArray()) {
                for (JsonNode item : items) {
                    String nodeName = item.get("metadata").get("name").asText();
                    double cpuCores = parseCpuMetric(item.get("usage").get("cpu").asText());
                    double memGb = parseMemoryMetricToGb(item.get("usage").get("memory").asText());

                    Map<String, Double> metrics = new HashMap<>();
                    metrics.put("cpu", cpuCores);
                    metrics.put("memory", memGb);
                    usageMap.put(nodeName, metrics);
                }
            }
        } catch (Exception e) {
            log.warn("Metrics Server (node metrics) unreachable. Reason: {}", e.getMessage());
        }
        return usageMap;
    }

    /**
     * Queries Pod Metrics from Metrics Server.
     * Returns: Map<Namespace/PodName, Map<ContainerName, Map<"cpu"|"memory", Value>>>
     */
    private Map<String, Map<String, Map<String, Double>>> fetchPodLiveMetrics() {
        Map<String, Map<String, Map<String, Double>>> usageMap = new HashMap<>();
        try {
            Object rawObj = customObjectsApi.getClusterCustomObject("metrics.k8s.io", "v1beta1", "pods", null);
            String jsonStr = jacksonObjectMapper.writeValueAsString(rawObj);
            JsonNode root = jacksonObjectMapper.readTree(jsonStr);
            JsonNode items = root.get("items");

            if (items != null && items.isArray()) {
                for (JsonNode item : items) {
                    String name = item.get("metadata").get("name").asText();
                    String ns = item.get("metadata").get("namespace").asText();
                    String key = ns + "/" + name;

                    Map<String, Map<String, Double>> containerUsage = new HashMap<>();
                    JsonNode containers = item.get("containers");
                    
                    if (containers != null && containers.isArray()) {
                        for (JsonNode container : containers) {
                            String cName = container.get("name").asText();
                            double cpu = parseCpuMetric(container.get("usage").get("cpu").asText());
                            double mem = parseMemoryMetricToGb(container.get("usage").get("memory").asText());

                            Map<String, Double> metrics = new HashMap<>();
                            metrics.put("cpu", cpu);
                            metrics.put("memory", mem);
                            containerUsage.put(cName, metrics);
                        }
                    }
                    usageMap.put(key, containerUsage);
                }
            }
        } catch (Exception e) {
            log.warn("Metrics Server (pod metrics) unreachable. Reason: {}", e.getMessage());
        }
        return usageMap;
    }

    private double parseMemoryToGb(Quantity quantity) {
        if (quantity == null) return 0.0;
        return quantity.getNumber().doubleValue() / (1024.0 * 1024.0 * 1024.0);
    }

    private double parseCpuMetric(String cpuStr) {
        if (cpuStr == null || cpuStr.isEmpty()) return 0.0;
        try {
            if (cpuStr.endsWith("n")) {
                // Nanocores
                return Double.parseDouble(cpuStr.substring(0, cpuStr.length() - 1)) / 1000000000.0;
            } else if (cpuStr.endsWith("u")) {
                // Microcores
                return Double.parseDouble(cpuStr.substring(0, cpuStr.length() - 1)) / 1000000.0;
            } else if (cpuStr.endsWith("m")) {
                // Millicores
                return Double.parseDouble(cpuStr.substring(0, cpuStr.length() - 1)) / 1000.0;
            } else {
                return Double.parseDouble(cpuStr);
            }
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private double parseMemoryMetricToGb(String memStr) {
        if (memStr == null || memStr.isEmpty()) return 0.0;
        try {
            double multiplier = 1.0; // Assume bytes
            String clean = memStr.replaceAll("[^a-zA-Z]", "");
            String num = memStr.replaceAll("[a-zA-Z]", "");

            if ("Ki".equalsIgnoreCase(clean)) {
                multiplier = 1024.0;
            } else if ("Mi".equalsIgnoreCase(clean)) {
                multiplier = 1024.0 * 1024.0;
            } else if ("Gi".equalsIgnoreCase(clean)) {
                multiplier = 1024.0 * 1024.0 * 1024.0;
            } else if ("Ti".equalsIgnoreCase(clean)) {
                multiplier = 1024.0 * 1024.0 * 1024.0 * 1024.0;
            }

            double bytes = Double.parseDouble(num) * multiplier;
            return bytes / (1024.0 * 1024.0 * 1024.0);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    @Override
    public void scaleDeployment(String namespace, String name, int replicas) {
        try {
            String jsonPatch = "[{\"op\":\"replace\",\"path\":\"/spec/replicas\",\"value\":" + replicas + "}]";
            appsV1Api.patchNamespacedDeployment(name, namespace, new V1Patch(jsonPatch), null, null, null, null, null);
        } catch (ApiException e) {
            log.error("Kubernetes API error scaling deployment {}: {} (Code: {})", name, e.getResponseBody(), e.getCode());
            throw new RuntimeException("K8s API error scaling deployment: " + e.getResponseBody(), e);
        }
    }

    @Override
    public void restartDeployment(String namespace, String name) {
        try {
            String now = java.time.Instant.now().toString();
            String jsonPatch = "[{\"op\":\"add\",\"path\":\"/spec/template/metadata/annotations/kubectl.kubernetes.io~1restartedAt\",\"value\":\"" + now + "\"}]";
            appsV1Api.patchNamespacedDeployment(name, namespace, new V1Patch(jsonPatch), null, null, null, null, null);
        } catch (ApiException e) {
            log.error("Kubernetes API error restarting deployment {}: {} (Code: {})", name, e.getResponseBody(), e.getCode());
            throw new RuntimeException("K8s API error restarting deployment: " + e.getResponseBody(), e);
        }
    }

    @Override
    public void deletePod(String namespace, String name) {
        try {
            coreV1Api.deleteNamespacedPod(name, namespace, null, null, null, null, null, null);
        } catch (ApiException e) {
            log.error("Kubernetes API error deleting pod {}: {} (Code: {})", name, e.getResponseBody(), e.getCode());
            throw new RuntimeException("K8s API error deleting pod: " + e.getResponseBody(), e);
        }
    }

    @Override
    public void cordonNode(String nodeName, boolean cordon) {
        try {
            String jsonPatch = "[{\"op\":\"replace\",\"path\":\"/spec/unschedulable\",\"value\":" + cordon + "}]";
            coreV1Api.patchNode(nodeName, new V1Patch(jsonPatch), null, null, null, null, null);
        } catch (ApiException e) {
            log.error("Kubernetes API error cordoning node {}: {} (Code: {})", nodeName, e.getResponseBody(), e.getCode());
            throw new RuntimeException("K8s API error cordoning node: " + e.getResponseBody(), e);
        }
    }

    @Override
    public void drainNode(String nodeName) {
        try {
            // Cordon node first to stop new scheduling
            cordonNode(nodeName, true);
            
            // List all pods on the specific node using fieldSelector
            V1PodList podList = coreV1Api.listPodForAllNamespaces(null, null, "spec.nodeName=" + nodeName, null, null, null, null, null, null, null, null);
            
            for (V1Pod pod : podList.getItems()) {
                String podName = pod.getMetadata().getName();
                String podNs = pod.getMetadata().getNamespace();
                
                // Skip DaemonSets (we don't evict daemonset pods) and system namespace pods if needed
                boolean isDaemonSet = false;
                if (pod.getMetadata().getOwnerReferences() != null) {
                    isDaemonSet = pod.getMetadata().getOwnerReferences().stream()
                            .anyMatch(ref -> "DaemonSet".equalsIgnoreCase(ref.getKind()));
                }
                
                if (isDaemonSet || "kube-system".equals(podNs)) {
                    log.info("Skipping eviction of DaemonSet/System pod: {}/{}", podNs, podName);
                    continue;
                }
                
                // Evict pod using the Eviction API
                V1Eviction eviction = new V1Eviction();
                V1ObjectMeta meta = new V1ObjectMeta();
                meta.setName(podName);
                meta.setNamespace(podNs);
                eviction.setMetadata(meta);
                
                log.info("Evicting pod {} from namespace {} as part of node drain", podName, podNs);
                coreV1Api.createNamespacedPodEviction(podName, podNs, eviction, null, null, null, null);
            }
        } catch (ApiException e) {
            log.error("Kubernetes API error draining node {}: {} (Code: {})", nodeName, e.getResponseBody(), e.getCode());
            throw new RuntimeException("K8s API error draining node: " + e.getResponseBody(), e);
        }
    }

    @Override
    public void updateDeploymentResources(String namespace, String name, String containerName, Double cpuRequest, Double cpuLimit, Double memRequestGb, Double memLimitGb) {
        try {
            V1Deployment dep = appsV1Api.readNamespacedDeployment(name, namespace, null);
            if (dep == null || dep.getSpec() == null || dep.getSpec().getTemplate() == null || dep.getSpec().getTemplate().getSpec() == null) {
                throw new RuntimeException("Deployment spec not found for " + name);
            }
            
            List<V1Container> containers = dep.getSpec().getTemplate().getSpec().getContainers();
            boolean found = false;
            
            for (V1Container container : containers) {
                if (container.getName().equals(containerName)) {
                    found = true;
                    V1ResourceRequirements resources = container.getResources();
                    if (resources == null) {
                        resources = new V1ResourceRequirements();
                        container.setResources(resources);
                    }
                    Map<String, Quantity> requests = resources.getRequests();
                    if (requests == null) {
                        requests = new HashMap<>();
                        resources.setRequests(requests);
                    }
                    Map<String, Quantity> limits = resources.getLimits();
                    if (limits == null) {
                        limits = new HashMap<>();
                        resources.setLimits(limits);
                    }

                    if (cpuRequest != null) {
                        if (cpuRequest == 0.0) {
                            requests.remove("cpu");
                        } else {
                            requests.put("cpu", new Quantity(String.format("%.3f", cpuRequest)));
                        }
                    }
                    if (memRequestGb != null) {
                        if (memRequestGb == 0.0) {
                            requests.remove("memory");
                        } else {
                            requests.put("memory", new Quantity(String.format("%dMi", (int)(memRequestGb * 1024))));
                        }
                    }
                    if (cpuLimit != null) {
                        if (cpuLimit == 0.0) {
                            limits.remove("cpu");
                        } else {
                            limits.put("cpu", new Quantity(String.format("%.3f", cpuLimit)));
                        }
                    }
                    if (memLimitGb != null) {
                        if (memLimitGb == 0.0) {
                            limits.remove("memory");
                        } else {
                            limits.put("memory", new Quantity(String.format("%dMi", (int)(memLimitGb * 1024))));
                        }
                    }
                    break;
                }
            }
            
            if (!found) {
                throw new IllegalArgumentException("Container " + containerName + " not found in deployment " + name);
            }
            
            appsV1Api.replaceNamespacedDeployment(name, namespace, dep, null, null, null, null);
        } catch (ApiException e) {
            log.error("Kubernetes API error updating deployment resources {}: {} (Code: {})", name, e.getResponseBody(), e.getCode());
            throw new RuntimeException("K8s API error updating resources: " + e.getResponseBody(), e);
        }
    }
}
