package com.arthasmanager.arthas.session;

import io.fabric8.kubernetes.client.LocalPortForward;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Represents an active Arthas diagnostic session attached to a JVM inside a Pod.
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>Arthas is started inside the container and binds to its HTTP port.</li>
 *   <li>A Fabric8 {@link LocalPortForward} tunnels the container's Arthas port to a local port.</li>
 *   <li>All subsequent Arthas HTTP API calls are made to {@code localhost:localPort}.</li>
 *   <li>When the user disconnects, {@link #close()} tears down the port-forward.</li>
 * </ol>
 */
@Data
@Builder
public class ArthasSession {

    /** Unique session identifier returned to the frontend. */
    private String sessionId;

    private String namespace;
    private String podName;
    private String containerName;

    /** PID of the target Java process inside the container. */
    private int pid;

    /** Local port bound by the Fabric8 port-forward → container's Arthas HTTP port. */
    private int localPort;

    /** Arthas internal session ID (returned by Arthas init_session action). */
    private String arthasInternalSessionId;

    /** Fabric8 port-forward handle; close to release the tunnel. */
    private transient LocalPortForward portForward;

    private Instant createdAt;
    private Instant lastUsedAt;

    public void close() {
        if (portForward != null) {
            try {
                portForward.close();
            } catch (Exception ignored) {
            }
        }
    }

    /** Returns the base URL for Arthas HTTP API calls via the local port-forward. */
    public String arthasBaseUrl() {
        return "http://127.0.0.1:" + localPort;
    }
}
