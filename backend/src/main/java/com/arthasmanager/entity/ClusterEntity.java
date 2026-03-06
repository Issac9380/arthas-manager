package com.arthasmanager.entity;

import com.arthasmanager.model.cluster.ClusterAuthType;
import com.arthasmanager.model.cluster.ClusterStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterEntity {
    private String id;
    private Long userId;
    private String name;
    private ClusterAuthType authType;
    private String apiServerUrl;
    private boolean skipTlsVerify;
    private String token;
    private String caCertData;
    private String clientCertData;
    private String clientKeyData;
    private String kubeconfigContent;
    private boolean defaultCluster;
    private ClusterStatus status;
    private String statusMessage;
    private Instant createdAt;
}
