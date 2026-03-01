package com.arthasmanager.config;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class KubernetesConfig {

    @Value("${kubernetes.kubeconfig:}")
    private String kubeconfigPath;

    /**
     * Builds a Fabric8 KubernetesClient.
     * Falls back to in-cluster config when running inside a Pod.
     */
    @Bean
    public KubernetesClient kubernetesClient() {
        Config config;
        if (kubeconfigPath != null && !kubeconfigPath.isBlank() && new File(kubeconfigPath).exists()) {
            config = Config.fromKubeconfig(kubeconfigPath);
        } else {
            config = new ConfigBuilder().build();
        }
        return new KubernetesClientBuilder().withConfig(config).build();
    }
}
