package com.arthasmanager.model.dto;

import lombok.Data;

@Data
public class AttachRequest {
    private String namespace;
    private String podName;
    private String containerName;
    /** Target Java process PID inside the container */
    private int pid;
}
