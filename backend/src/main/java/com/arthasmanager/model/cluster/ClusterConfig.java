package com.arthasmanager.model.cluster;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 集群连接配置（含敏感信息，仅在服务端内部使用，不对外暴露）.
 *
 * <p>根据 authType 使用不同字段：
 * <ul>
 *   <li>KUBECONFIG — 使用 {@code kubeconfigContent}</li>
 *   <li>TOKEN     — 使用 {@code apiServerUrl}, {@code token}, 可选 {@code caCertData}, {@code skipTlsVerify}</li>
 *   <li>CERT      — 使用 {@code apiServerUrl}, {@code caCertData}, {@code clientCertData}, {@code clientKeyData}</li>
 *   <li>IN_CLUSTER — 无需额外配置</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterConfig {

    /** 集群显示名称 */
    private String name;

    private ClusterAuthType authType;

    // ── TOKEN / CERT 共用 ─────────────────────────────────────────────────────

    /** Kubernetes API Server 地址，例如 https://192.168.1.100:6443 */
    private String apiServerUrl;

    /** 是否跳过 TLS 证书校验（仅用于测试环境） */
    private boolean skipTlsVerify;

    // ── TOKEN 认证 ────────────────────────────────────────────────────────────

    /** Bearer Token，例如 ServiceAccount 的 token */
    private String token;

    /** CA 证书内容（PEM 格式），TOKEN 和 CERT 模式均可用 */
    private String caCertData;

    // ── CERT 认证 ─────────────────────────────────────────────────────────────

    /** 客户端证书（PEM 格式） */
    private String clientCertData;

    /** 客户端私钥（PEM 格式） */
    private String clientKeyData;

    // ── KUBECONFIG 认证 ────────────────────────────────────────────────────────

    /** 完整的 kubeconfig YAML 文本 */
    private String kubeconfigContent;
}
