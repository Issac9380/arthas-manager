package com.arthasmanager.model.cluster;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 集群信息（脱敏），用于前端展示.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterInfo {

    private String id;
    private String name;
    private ClusterAuthType authType;
    /** API Server 地址（TOKEN/CERT 模式下显示；KUBECONFIG 从 kubeconfig 解析） */
    private String apiServerUrl;
    /** 是否为默认集群（从 application.yml 初始化） */
    private boolean defaultCluster;
    private ClusterStatus status;
    private String statusMessage;
    private Instant createdAt;
}
