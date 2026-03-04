package com.arthasmanager.service;

/**
 * Handles uploading files (JDK archives, Arthas boot jar) to Pod containers.
 * All methods accept a {@code clusterId}; pass null to use the default cluster.
 */
public interface FileTransferService {

    /**
     * Downloads the specified Arthas distribution (if not already cached) and uploads
     * the essential jars to {@code /tmp/arthas/} inside the container.
     * The full distribution is used so the container needs no internet access at attach time.
     *
     * @param arthasVersion Arthas version to deploy, e.g. {@code "3.7.2"}
     */
    void deployArthas(String clusterId, String namespace, String podName,
                      String containerName, String arthasVersion);

    void uploadJdk(String clusterId, String namespace, String podName, String containerName, String jdkVersion);

    int startArthasAndPortForward(String clusterId, String namespace, String podName,
                                  String containerName, int pid, String sessionId);
}
