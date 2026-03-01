package com.arthasmanager.model.dto;

import lombok.Data;

import java.util.Map;

@Data
public class ArthasCommandRequest {
    /** Arthas session ID (returned by /api/arthas/attach) */
    private String sessionId;
    /**
     * Logical command type — e.g. "jvm", "thread", "watch".
     * Maps to a registered ArthasCommand implementation via the factory.
     */
    private String commandType;
    /** Key-value pairs for the specific command's parameters */
    private Map<String, Object> params;
}
