package com.arthasmanager.service;

import com.arthasmanager.model.cluster.ClusterConfig;
import com.arthasmanager.model.cluster.ClusterInfo;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.List;

/**
 * 多集群注册与 KubernetesClient 工厂.
 *
 * <p>支持四种认证方式：KUBECONFIG、TOKEN、CERT、IN_CLUSTER。
 * 集群信息保存在内存中；重启后需重新添加（后续可持久化到文件/数据库）。
 */
public interface ClusterService {

    /**
     * 添加并注册一个新集群，返回包含 id 的 ClusterInfo.
     */
    ClusterInfo addCluster(ClusterConfig config);

    /**
     * 列出所有已注册集群（脱敏）.
     */
    List<ClusterInfo> listClusters();

    /**
     * 获取指定集群信息.
     */
    ClusterInfo getClusterInfo(String clusterId);

    /**
     * 删除集群（默认集群不可删除）.
     */
    void deleteCluster(String clusterId);

    /**
     * 测试连接（不保存），返回连接状态.
     */
    ClusterInfo testConnection(ClusterConfig config);

    /**
     * 获取指定集群的 KubernetesClient.
     * clusterId 为 null 或空字符串时返回默认集群的 client.
     *
     * @throws IllegalStateException 集群不存在或未连接时
     */
    KubernetesClient getClient(String clusterId);
}
