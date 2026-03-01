package com.arthasmanager.config;

import org.springframework.context.annotation.Configuration;

/**
 * Kubernetes 配置占位类.
 * KubernetesClient 实例由 {@link com.arthasmanager.service.impl.ClusterServiceImpl} 统一管理，
 * 支持多集群、多认证方式（Kubeconfig / Token / Cert / In-Cluster）。
 */
@Configuration
public class KubernetesConfig {
    // KubernetesClient bean is managed by ClusterServiceImpl
}
