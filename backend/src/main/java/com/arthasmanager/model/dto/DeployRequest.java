package com.arthasmanager.model.dto;

import lombok.Data;

@Data
public class DeployRequest {
    private String namespace;
    private String podName;
    private String containerName;
    /** Whether to upload a bundled JDK before deploying Arthas */
    private boolean uploadJdk;
    /** JDK version to upload, e.g. "17" — only used when uploadJdk=true */
    private String jdkVersion;
}
