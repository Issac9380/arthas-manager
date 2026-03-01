package com.arthasmanager.service;

/**
 * Handles uploading files (JDK archives, Arthas boot jar) to Pod containers.
 */
public interface FileTransferService {

    /**
     * Uploads the Arthas boot jar to {@code /tmp/arthas/arthas-boot.jar}
     * inside the specified container. Downloads it first if not cached locally.
     */
    void deployArthas(String namespace, String podName, String containerName);

    /**
     * Uploads a JDK tar.gz from the local tools cache to the container,
     * extracts it under {@code /tmp/jdk/}, and sets {@code JAVA_HOME}.
     *
     * @param jdkVersion e.g. "17"
     */
    void uploadJdk(String namespace, String podName, String containerName, String jdkVersion);

    /**
     * Starts the Arthas agent inside the container and attaches it to the given PID.
     * Returns the local port to which Arthas HTTP API is port-forwarded.
     *
     * @return local TCP port serving the Arthas HTTP API
     */
    int startArthasAndPortForward(String namespace, String podName, String containerName,
                                  int pid, String sessionId);
}
