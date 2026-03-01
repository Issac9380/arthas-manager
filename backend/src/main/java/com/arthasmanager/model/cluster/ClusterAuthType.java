package com.arthasmanager.model.cluster;

/**
 * Kubernetes 集群认证方式枚举.
 */
public enum ClusterAuthType {
    /** 粘贴或上传完整的 kubeconfig YAML 文本 */
    KUBECONFIG,
    /** API Server URL + Bearer Token（+ 可选 CA 证书） */
    TOKEN,
    /** API Server URL + CA 证书 + 客户端证书 + 客户端私钥（双向 TLS） */
    CERT,
    /** 在 Kubernetes Pod 内部运行时自动使用 ServiceAccount */
    IN_CLUSTER
}
