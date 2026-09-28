package com.k8s.costoptimizer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the Kubernetes Cost Optimizer backend.
 * Enables scheduling to pull cluster metrics on a periodic basis.
 */
@SpringBootApplication
@EnableScheduling
public class CostOptimizerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CostOptimizerApplication.class, args);
    }
}
