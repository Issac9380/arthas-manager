package com.arthasmanager.model.dto;

import lombok.Data;

@Data
public class AttachRequest {
    /** Cluster id; null or empty means the default cluster */
    private String clusterId;
    private String namespace;
    private String podName;
    private String containerName;
    /** Target Java process PID inside the container */
    private int pid;
}
