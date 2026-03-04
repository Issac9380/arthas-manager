package com.arthasmanager.service.impl;

import com.arthasmanager.model.cluster.ClusterAuthType;
import com.arthasmanager.model.cluster.ClusterConfig;
import com.arthasmanager.model.cluster.ClusterInfo;
import com.arthasmanager.model.cluster.ClusterStatus;
import com.arthasmanager.service.ClusterService;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 集群注册表与 KubernetesClient 工厂实现.
 *
 * <p>启动时自动尝试从 {@code application.yml} 中读取 kubeconfig，
 * 注册名为 "default" 的默认集群。
 */
@Slf4j
@Service
public class ClusterServiceImpl implements ClusterService {

    private static final String DEFAULT_CLUSTER_ID = "default";

    @Value("${kubernetes.kubeconfig:}")
    private String defaultKubeconfigPath;

    /** 集群配置注册表（含敏感信息，仅内存持有） */
    private final ConcurrentHashMap<String, ClusterConfig> configRegistry = new ConcurrentHashMap<>();
    /** 集群信息注册表（脱敏，用于对外展示） */
    private final ConcurrentHashMap<String, ClusterInfo> infoRegistry = new ConcurrentHashMap<>();
    /** KubernetesClient 注册表 */
    private final ConcurrentHashMap<String, KubernetesClient> clientRegistry = new ConcurrentHashMap<>();

    // ── 启动时初始化默认集群 ───────────────────────────────────────────────────

    @PostConstruct
    public void initDefaultCluster() {
        String kubeconfigFile = (defaultKubeconfigPath == null || defaultKubeconfigPath.isBlank())
                ? System.getProperty("user.home") + "/.kube/config"
                : defaultKubeconfigPath;

        ClusterConfig config = null;
        if (new File(kubeconfigFile).exists()) {
            try {
                String content = Files.readString(Path.of(kubeconfigFile));
                config = ClusterConfig.builder()
                        .name("default")
                        .authType(ClusterAuthType.KUBECONFIG)
                        .kubeconfigContent(content)
                        .build();
                log.info("Loaded kubeconfig from: {}", kubeconfigFile);
            } catch (Exception e) {
                log.warn("Could not read kubeconfig at {}: {}", kubeconfigFile, e.getMessage());
            }
        }

        if (config == null) {
            config = ClusterConfig.builder()
                    .name("in-cluster")
                    .authType(ClusterAuthType.IN_CLUSTER)
                    .build();
            log.info("No kubeconfig found, trying in-cluster ServiceAccount.");
        }

        try {
            KubernetesClient client = buildClientInternal(config);
            ClusterStatus status = probeStatus(client);
            String apiUrl = client.getMasterUrl() != null ? client.getMasterUrl().toString() : "unknown";

            configRegistry.put(DEFAULT_CLUSTER_ID, config);
            clientRegistry.put(DEFAULT_CLUSTER_ID, client);
            infoRegistry.put(DEFAULT_CLUSTER_ID, ClusterInfo.builder()
                    .id(DEFAULT_CLUSTER_ID)
                    .name(config.getName())
                    .authType(config.getAuthType())
                    .apiServerUrl(apiUrl)
                    .defaultCluster(true)
                    .status(status)
                    .statusMessage(status == ClusterStatus.CONNECTED ? "Connected" : "Cluster unreachable")
                    .createdAt(Instant.now())
                    .build());
            log.info("Default cluster registered: {} → {} ({})", config.getName(), apiUrl, status);
        } catch (Exception e) {
            log.warn("Default cluster initialization failed: {}. K8s features will be unavailable until a cluster is added.", e.getMessage());
        }
    }

    // ── ClusterService 接口实现 ───────────────────────────────────────────────

    @Override
    public ClusterInfo addCluster(ClusterConfig config) {
        String id = UUID.randomUUID().toString();
        KubernetesClient client = buildClientInternal(config);
        ClusterStatus status = probeStatus(client);
        String apiUrl = resolveApiServerUrl(config, client);

        configRegistry.put(id, config);
        clientRegistry.put(id, client);

        ClusterInfo info = ClusterInfo.builder()
                .id(id)
                .name(config.getName())
                .authType(config.getAuthType())
                .apiServerUrl(apiUrl)
                .defaultCluster(false)
                .status(status)
                .statusMessage(status == ClusterStatus.CONNECTED ? "Connected" : "Cluster unreachable")
                .createdAt(Instant.now())
                .build();
        infoRegistry.put(id, info);
        log.info("Cluster added: {} ({}) → {} [{}]", config.getName(), config.getAuthType(), apiUrl, status);
        return info;
    }

    @Override
    public List<ClusterInfo> listClusters() {
        return new ArrayList<>(infoRegistry.values());
    }

    @Override
    public ClusterInfo getClusterInfo(String clusterId) {
        return infoRegistry.get(effectiveId(clusterId));
    }

    @Override
    public void deleteCluster(String clusterId) {
        if (DEFAULT_CLUSTER_ID.equals(clusterId)) {
            throw new IllegalArgumentException("Default cluster cannot be deleted.");
        }
        KubernetesClient client = clientRegistry.remove(clusterId);
        if (client != null) {
            try { client.close(); } catch (Exception ignored) {}
        }
        configRegistry.remove(clusterId);
        infoRegistry.remove(clusterId);
        log.info("Cluster deleted: {}", clusterId);
    }

    @Override
    public ClusterInfo testConnection(ClusterConfig config) {
        KubernetesClient client = null;
        try {
            client = buildClientInternal(config);
            ClusterStatus status = probeStatus(client);
            String apiUrl = resolveApiServerUrl(config, client);
            return ClusterInfo.builder()
                    .name(config.getName())
                    .authType(config.getAuthType())
                    .apiServerUrl(apiUrl)
                    .status(status)
                    .statusMessage(status == ClusterStatus.CONNECTED ? "Connection successful" : "Cluster unreachable (check credentials/network)")
                    .build();
        } catch (Exception e) {
            return ClusterInfo.builder()
                    .name(config.getName())
                    .authType(config.getAuthType())
                    .status(ClusterStatus.ERROR)
                    .statusMessage("Error: " + e.getMessage())
                    .build();
        } finally {
            if (client != null) { try { client.close(); } catch (Exception ignored) {} }
        }
    }

    @Override
    public KubernetesClient getClient(String clusterId) {
        KubernetesClient client = clientRegistry.get(effectiveId(clusterId));
        if (client == null) {
            throw new IllegalStateException(
                    "Cluster not found: '" + clusterId + "'. Please add a cluster in the Cluster Management page.");
        }
        return client;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String effectiveId(String clusterId) {
        return (clusterId == null || clusterId.isBlank()) ? DEFAULT_CLUSTER_ID : clusterId;
    }

    /**
     * 根据认证方式构建 KubernetesClient.
     * 显式禁用 HTTP/HTTPS 代理，防止系统代理设置干扰 K8s API 连接。
     */
    KubernetesClient buildClientInternal(ClusterConfig config) {
        return switch (config.getAuthType()) {
            case TOKEN -> {
                ConfigBuilder b = new ConfigBuilder()
                        .withMasterUrl(config.getApiServerUrl())
                        .withOauthToken(config.getToken())
                        .withTrustCerts(config.isSkipTlsVerify())
                        .withHttpProxy(null)
                        .withHttpsProxy(null);
                if (hasText(config.getCaCertData())) {
                    b.withCaCertData(b64(config.getCaCertData()));
                }
                yield new KubernetesClientBuilder().withConfig(b.build()).build();
            }
            case CERT -> {
                ConfigBuilder b = new ConfigBuilder()
                        .withMasterUrl(config.getApiServerUrl())
                        .withTrustCerts(config.isSkipTlsVerify())
                        .withCaCertData(b64(config.getCaCertData()))
                        .withClientCertData(b64(config.getClientCertData()))
                        .withClientKeyData(b64(config.getClientKeyData()))
                        .withHttpProxy(null)
                        .withHttpsProxy(null);
                yield new KubernetesClientBuilder().withConfig(b.build()).build();
            }
            case KUBECONFIG -> {
                Config k8sConfig = Config.fromKubeconfig(config.getKubeconfigContent());
                // Disable proxy picked up from system properties
                Config noProxy = new ConfigBuilder(k8sConfig)
                        .withHttpProxy(null)
                        .withHttpsProxy(null)
                        .build();
                yield new KubernetesClientBuilder().withConfig(noProxy).build();
            }
            default -> {
                // IN_CLUSTER: auto-detect via KUBECONFIG env / in-cluster ServiceAccount
                Config k8sConfig = new ConfigBuilder()
                        .withHttpProxy(null)
                        .withHttpsProxy(null)
                        .build();
                yield new KubernetesClientBuilder().withConfig(k8sConfig).build();
            }
        };
    }

    /** 探测连通性：调用 /version 接口（无需任何 RBAC 权限）. */
    private ClusterStatus probeStatus(KubernetesClient client) {
        try {
            client.getKubernetesVersion();
            return ClusterStatus.CONNECTED;
        } catch (Exception e) {
            log.warn("Cluster probe failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            return ClusterStatus.DISCONNECTED;
        }
    }

    private String resolveApiServerUrl(ClusterConfig config, KubernetesClient client) {
        if (hasText(config.getApiServerUrl())) return config.getApiServerUrl();
        if (client != null && client.getMasterUrl() != null) return client.getMasterUrl().toString();
        return "unknown";
    }

    /** Base64-encode PEM text so Fabric8 ConfigBuilder accepts it. */
    private String b64(String pem) {
        if (pem == null) return null;
        return Base64.getEncoder().encodeToString(pem.getBytes(StandardCharsets.UTF_8));
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
