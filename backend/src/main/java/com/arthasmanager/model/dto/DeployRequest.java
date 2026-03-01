package com.arthasmanager.model.dto;

import lombok.Data;

@Data
public class DeployRequest {
    /** Cluster id; null or empty means the default cluster */
    private String clusterId;
    private String namespace;
    private String podName;
    private String containerName;
    /** Whether to upload a bundled JDK before deploying Arthas */
    private boolean uploadJdk;
    /** JDK version to upload, e.g. "17" — only used when uploadJdk=true */
    private String jdkVersion;
}
