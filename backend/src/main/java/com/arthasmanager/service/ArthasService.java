package com.arthasmanager.service;

import com.arthasmanager.model.dto.ArthasCommandRequest;
import com.arthasmanager.model.dto.AttachRequest;
import com.arthasmanager.model.dto.DeployRequest;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * High-level Arthas management facade.
 * Orchestrates deploy → attach → execute → close lifecycle.
 */
public interface ArthasService {

    /**
     * Deploys Arthas (and optionally a JDK) into the container.
     */
    void deploy(DeployRequest request);

    /**
     * Attaches Arthas to the target Java process, sets up port-forwarding,
     * and returns a session ID for subsequent command calls.
     *
     * @return unique session ID
     */
    String attach(AttachRequest request);

    /**
     * Executes a command against an active session and returns the Arthas JSON result.
     */
    JsonNode execute(ArthasCommandRequest request);

    /**
     * Closes the session and tears down the port-forward.
     */
    void close(String sessionId);

    /**
     * Returns metadata for all available commands (used to build the UI dynamically).
     */
    List<Map<String, Object>> listCommandMeta();

    /**
     * Returns the JDK ↔ Arthas version compatibility matrix.
     * Shape: {@code { "17": { "recommended": "3.7.2", "supported": ["3.7.2", "3.6.9"] }, … }}
     */
    Map<String, Object> getVersionMatrix();
}
