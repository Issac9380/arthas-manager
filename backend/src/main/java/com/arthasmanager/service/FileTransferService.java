package com.arthasmanager.service;

/**
 * Handles uploading files (JDK archives, Arthas boot jar) to Pod containers.
 * All methods accept a {@code clusterId}; pass null to use the default cluster.
 */
public interface FileTransferService {

    void deployArthas(String clusterId, String namespace, String podName, String containerName);

    void uploadJdk(String clusterId, String namespace, String podName, String containerName, String jdkVersion);

    int startArthasAndPortForward(String clusterId, String namespace, String podName,
                                  String containerName, int pid, String sessionId);
}
