package com.arthasmanager.service.impl;

import com.arthasmanager.entity.ClusterEntity;
import com.arthasmanager.mapper.ClusterMapper;
import com.arthasmanager.model.cluster.ClusterAuthType;
import com.arthasmanager.model.cluster.ClusterConfig;
import com.arthasmanager.model.cluster.ClusterInfo;
import com.arthasmanager.model.cluster.ClusterStatus;
import com.arthasmanager.security.UserPrincipal;
import com.arthasmanager.service.ClusterService;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Cluster registry backed by SQLite via MyBatis.
 *
 * <p>Each user owns their own cluster records. The current user ID is resolved
 * from the Spring Security context on every operation.
 *
 * <p>Live {@link KubernetesClient} instances are cached in-memory (keyed by
 * cluster ID) to avoid rebuilding the connection on every call. They are
 * rebuilt on-demand from the DB when not cached.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClusterServiceImpl implements ClusterService {

    private final ClusterMapper clusterMapper;

    /** Live KubernetesClient connections — keyed by clusterId. */
    private final ConcurrentHashMap<String, KubernetesClient> clientRegistry = new ConcurrentHashMap<>();

    // ── ClusterService interface ───────────────────────────────────────────────

    @Override
    public ClusterInfo addCluster(ClusterConfig config) {
        Long userId = getCurrentUserId();
        String id = UUID.randomUUID().toString();

        KubernetesClient client = buildClientInternal(config);
        ClusterStatus status = probeStatus(client);
        String apiUrl = resolveApiServerUrl(config, client);

        ClusterEntity entity = ClusterEntity.builder()
                .id(id)
                .userId(userId)
                .name(config.getName())
                .authType(config.getAuthType())
                .apiServerUrl(apiUrl)
                .skipTlsVerify(config.isSkipTlsVerify())
                .token(config.getToken())
                .caCertData(config.getCaCertData())
                .clientCertData(config.getClientCertData())
                .clientKeyData(config.getClientKeyData())
                .kubeconfigContent(config.getKubeconfigContent())
                .defaultCluster(false)
                .status(status)
                .statusMessage(status == ClusterStatus.CONNECTED ? "Connected" : "Cluster unreachable")
                .createdAt(Instant.now())
                .build();

        clusterMapper.insert(entity);
        clientRegistry.put(id, client);

        log.info("Cluster added: {} ({}) → {} [{}]", config.getName(), config.getAuthType(), apiUrl, status);
        return toInfo(entity);
    }

    @Override
    public List<ClusterInfo> listClusters() {
        Long userId = getCurrentUserId();
        return clusterMapper.findByUserId(userId).stream()
                .map(this::toInfo)
                .collect(Collectors.toList());
    }

    @Override
    public ClusterInfo getClusterInfo(String clusterId) {
        ClusterEntity entity = clusterMapper.findById(clusterId);
        return entity != null ? toInfo(entity) : null;
    }

    @Override
    public void deleteCluster(String clusterId) {
        KubernetesClient client = clientRegistry.remove(clusterId);
        if (client != null) {
            try { client.close(); } catch (Exception ignored) {}
        }
        clusterMapper.deleteById(clusterId);
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
                    .statusMessage(status == ClusterStatus.CONNECTED
                            ? "Connection successful"
                            : "Cluster unreachable (check credentials/network)")
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
        ClusterEntity entity;
        if (clusterId == null || clusterId.isBlank()) {
            Long userId = getCurrentUserId();
            entity = clusterMapper.findDefaultByUserId(userId);
        } else {
            entity = clusterMapper.findById(clusterId);
        }

        if (entity == null) {
            throw new IllegalStateException(
                    "Cluster not found: '" + clusterId + "'. Please add a cluster in the Cluster Management page.");
        }

        return clientRegistry.computeIfAbsent(entity.getId(), id -> buildClientFromEntity(entity));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    protected Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }
        throw new IllegalStateException("No authenticated user found in security context.");
    }

    private ClusterInfo toInfo(ClusterEntity entity) {
        return ClusterInfo.builder()
                .id(entity.getId())
                .name(entity.getName())
                .authType(entity.getAuthType())
                .apiServerUrl(entity.getApiServerUrl())
                .defaultCluster(entity.isDefaultCluster())
                .status(entity.getStatus())
                .statusMessage(entity.getStatusMessage())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private KubernetesClient buildClientFromEntity(ClusterEntity entity) {
        ClusterConfig config = ClusterConfig.builder()
                .name(entity.getName())
                .authType(entity.getAuthType())
                .apiServerUrl(entity.getApiServerUrl())
                .skipTlsVerify(entity.isSkipTlsVerify())
                .token(entity.getToken())
                .caCertData(entity.getCaCertData())
                .clientCertData(entity.getClientCertData())
                .clientKeyData(entity.getClientKeyData())
                .kubeconfigContent(entity.getKubeconfigContent())
                .build();
        return buildClientInternal(config);
    }

    /**
     * Build a KubernetesClient from the given config.
     * Proxy is explicitly disabled to prevent system proxy settings from interfering.
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
                Config noProxy = new ConfigBuilder(k8sConfig)
                        .withHttpProxy(null)
                        .withHttpsProxy(null)
                        .build();
                yield new KubernetesClientBuilder().withConfig(noProxy).build();
            }
            default -> {
                Config k8sConfig = new ConfigBuilder()
                        .withHttpProxy(null)
                        .withHttpsProxy(null)
                        .build();
                yield new KubernetesClientBuilder().withConfig(k8sConfig).build();
            }
        };
    }

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

    private String b64(String pem) {
        if (pem == null) return null;
        return Base64.getEncoder().encodeToString(pem.getBytes(StandardCharsets.UTF_8));
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
