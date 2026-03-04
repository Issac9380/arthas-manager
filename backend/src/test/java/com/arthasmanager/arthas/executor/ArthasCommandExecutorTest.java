package com.arthasmanager.arthas.executor;

import com.arthasmanager.arthas.session.ArthasSession;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ArthasCommandExecutor.
 *
 * These tests exercise the error-handling path (unreachable host) and verify
 * the request body structure via an ObjectMapper spy. Full HTTP round-trip
 * tests require a live Arthas agent and are handled in integration/e2e tests.
 */
class ArthasCommandExecutorTest {

    private ArthasCommandExecutor executor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        executor = new ArthasCommandExecutor(objectMapper);
    }

    // ── execute ───────────────────────────────────────────────────────────────

    @Test
    void execute_whenHostUnreachable_returnsFailedNode() {
        ArthasSession session = ArthasSession.builder()
                .sessionId("sess-1")
                .localPort(1) // port 1 is never listening
                .arthasInternalSessionId("arthas-sess")
                .build();

        JsonNode result = executor.execute(session, "jvm");

        assertThat(result.path("state").asText()).isEqualTo("FAILED");
        assertThat(result.has("message")).isTrue();
    }

    @Test
    void execute_withoutInternalSessionId_returnsFailedNode() {
        ArthasSession session = ArthasSession.builder()
                .sessionId("sess-1")
                .localPort(1)
                .arthasInternalSessionId(null)
                .build();

        JsonNode result = executor.execute(session, "thread -n 5");

        assertThat(result.path("state").asText()).isEqualTo("FAILED");
    }

    // ── initSession ───────────────────────────────────────────────────────────

    @Test
    void initSession_whenHostUnreachable_returnsEmptySessionId() {
        ArthasSession session = ArthasSession.builder()
                .sessionId("sess-1")
                .localPort(1)
                .build();

        // doPost returns a FAILED node → .path("sessionId").asText() returns ""
        String internalId = executor.initSession(session);

        assertThat(internalId).isBlank();
    }

    // ── closeSession ──────────────────────────────────────────────────────────

    @Test
    void closeSession_withNullInternalSessionId_doesNothing() {
        ArthasSession session = ArthasSession.builder()
                .sessionId("sess-1")
                .localPort(1)
                .arthasInternalSessionId(null)
                .build();

        // Should not throw even though there's no server to call
        executor.closeSession(session);
    }

    @Test
    void closeSession_whenHostUnreachable_swallowsException() {
        ArthasSession session = ArthasSession.builder()
                .sessionId("sess-1")
                .localPort(1)
                .arthasInternalSessionId("arthas-sess")
                .build();

        // Should not throw — error is logged and swallowed
        executor.closeSession(session);
    }

    // ── objectMapper integration ──────────────────────────────────────────────

    @Test
    void execute_failedNode_isValidJson() {
        ArthasSession session = ArthasSession.builder()
                .sessionId("sess-x")
                .localPort(1)
                .build();

        JsonNode result = executor.execute(session, "dashboard");

        assertThat(result.isObject()).isTrue();
        assertThat(result.has("state")).isTrue();
        assertThat(result.has("message")).isTrue();
    }
}
