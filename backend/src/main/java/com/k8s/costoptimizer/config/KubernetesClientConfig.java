package com.k8s.costoptimizer.config;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.io.FileReader;
import java.io.IOException;

/**
 * Spring configuration class for initializing the official Kubernetes Java Client beans.
 */
@org.springframework.context.annotation.Configuration
public class KubernetesClientConfig {

    private static final Logger log = LoggerFactory.getLogger(KubernetesClientConfig.class);

    @Value("${app.k8s.use-kubeconfig:true}")
    private boolean useKubeconfig;

    @Value("${app.k8s.kubeconfig-path:}")
    private String kubeconfigPath;

    @Bean
    public ApiClient apiClient() {
        ApiClient client = null;
        try {
            if (useKubeconfig) {
                if (kubeconfigPath != null && !kubeconfigPath.trim().isEmpty()) {
                    log.info("Loading Kubernetes client using specified Kubeconfig path: {}", kubeconfigPath);
                    client = ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new FileReader(kubeconfigPath))).build();
                } else {
                    log.info("Attempting default search path for local Kubeconfig...");
                    try {
                        client = ClientBuilder.defaultClient();
                        log.info("Successfully loaded local Kubeconfig.");
                    } catch (IOException ioException) {
                        log.warn("Default Kubeconfig not found. Falling back to cluster service account config...");
                        client = ClientBuilder.cluster().build();
                        log.info("Successfully loaded In-Cluster Kubernetes configuration.");
                    }
                }
            } else {
                log.info("Loading In-Cluster Kubernetes configuration...");
                client = ClientBuilder.cluster().build();
            }
        } catch (Exception e) {
            log.error("Failed to initialize Kubernetes API client. Cost Optimizer will run in fallback mock/stub mode: {}", e.getMessage());
            // Create a default client object to avoid dependency injection failures,
            // though actual requests will crash or fallback gracefully.
            client = new ApiClient();
        }
        
        if (client != null) {
            Configuration.setDefaultApiClient(client);
        }
        return client;
    }

    @Bean
    public CoreV1Api coreV1Api(ApiClient apiClient) {
        return new CoreV1Api(apiClient);
    }

    @Bean
    public AppsV1Api appsV1Api(ApiClient apiClient) {
        return new AppsV1Api(apiClient);
    }

    @Bean
    public CustomObjectsApi customObjectsApi(ApiClient apiClient) {
        return new CustomObjectsApi(apiClient);
    }
}
