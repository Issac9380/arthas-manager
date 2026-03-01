package com.arthasmanager.config;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Slf4j
@Configuration
public class KubernetesConfig {

    @Value("${kubernetes.kubeconfig:}")
    private String kubeconfigPath;

    /**
     * Builds a Fabric8 KubernetesClient.
     * Falls back to in-cluster config when running inside a Pod.
     * Returns null if no Kubernetes environment is available (dev mode).
     */
    @Bean
    public KubernetesClient kubernetesClient() {
        try {
            Config config;
            if (kubeconfigPath != null && !kubeconfigPath.isBlank() && new File(kubeconfigPath).exists()) {
                config = Config.fromKubeconfig(kubeconfigPath);
            } else {
                config = new ConfigBuilder().build();
            }
            KubernetesClient client = new KubernetesClientBuilder().withConfig(config).build();
            log.info("Kubernetes client initialized: {}", config.getMasterUrl());
            return client;
        } catch (Exception e) {
            log.warn("Kubernetes client initialization failed (no cluster available): {}. K8s features disabled.", e.getMessage());
            return null;
        }
    }
}
