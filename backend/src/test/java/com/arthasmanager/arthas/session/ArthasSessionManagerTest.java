package com.arthasmanager.arthas.session;

import io.fabric8.kubernetes.client.LocalPortForward;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ArthasSessionManagerTest {

    private ArthasSessionManager manager;

    @BeforeEach
    void setUp() {
        manager = new ArthasSessionManager();
        ReflectionTestUtils.setField(manager, "sessionTimeoutMinutes", 30);
    }

    private ArthasSession buildSession(String id) {
        return ArthasSession.builder()
                .sessionId(id)
                .clusterId("default")
                .namespace("default")
                .podName("pod-1")
                .containerName("app")
                .localPort(39400)
                .createdAt(Instant.now())
                .build();
    }

    // ── put / get / remove ────────────────────────────────────────────────────

    @Test
    void put_thenGet_returnsSession() {
        ArthasSession session = buildSession("s1");
        manager.put(session);

        Optional<ArthasSession> found = manager.get("s1");

        assertThat(found).isPresent().contains(session);
    }

    @Test
    void get_unknownId_returnsEmpty() {
        assertThat(manager.get("no-such-id")).isEmpty();
    }

    @Test
    void get_updatesLastUsedAt() throws InterruptedException {
        ArthasSession session = buildSession("s2");
        manager.put(session);

        Thread.sleep(5);  // small delay so timestamps differ
        manager.get("s2");

        assertThat(session.getLastUsedAt()).isNotNull();
        assertThat(session.getLastUsedAt()).isAfterOrEqualTo(session.getCreatedAt());
    }

    @Test
    void remove_deletesSession() {
        ArthasSession session = buildSession("s3");
        manager.put(session);
        manager.remove("s3");

        assertThat(manager.get("s3")).isEmpty();
    }

    @Test
    void remove_closesPortForward() throws Exception {
        LocalPortForward pf = mock(LocalPortForward.class);
        ArthasSession session = ArthasSession.builder()
                .sessionId("s4")
                .portForward(pf)
                .createdAt(Instant.now())
                .build();

        manager.put(session);
        manager.remove("s4");

        verify(pf).close();
    }

    @Test
    void all_returnsAllSessions() {
        manager.put(buildSession("s5"));
        manager.put(buildSession("s6"));
        manager.put(buildSession("s7"));

        assertThat(manager.all()).hasSize(3);
    }

    // ── eviction ──────────────────────────────────────────────────────────────

    @Test
    void evictIdleSessions_removesExpiredSessions() throws Exception {
        LocalPortForward pf = mock(LocalPortForward.class);

        // Session used 35 minutes ago (beyond 30-min timeout)
        ArthasSession expiredSession = ArthasSession.builder()
                .sessionId("expired")
                .portForward(pf)
                .createdAt(Instant.now().minusSeconds(35 * 60))
                .lastUsedAt(Instant.now().minusSeconds(35 * 60))
                .build();

        // Session used 5 minutes ago (within timeout)
        ArthasSession activeSession = buildSession("active");
        activeSession.setLastUsedAt(Instant.now().minusSeconds(5 * 60));

        manager.put(expiredSession);
        manager.put(activeSession);

        manager.evictIdleSessions();

        assertThat(manager.get("expired")).isEmpty();
        assertThat(manager.get("active")).isPresent();
        verify(pf).close();
    }

    @Test
    void evictIdleSessions_usesCreatedAtWhenLastUsedAtIsNull() throws Exception {
        LocalPortForward pf = mock(LocalPortForward.class);

        // Session created 35 minutes ago, never accessed (lastUsedAt is null)
        ArthasSession session = ArthasSession.builder()
                .sessionId("old-session")
                .portForward(pf)
                .createdAt(Instant.now().minusSeconds(35 * 60))
                .lastUsedAt(null)
                .build();

        manager.put(session);
        manager.evictIdleSessions();

        assertThat(manager.get("old-session")).isEmpty();
        verify(pf).close();
    }

    @Test
    void evictIdleSessions_keepsActiveSessions() {
        ArthasSession recent = buildSession("recent");
        recent.setLastUsedAt(Instant.now().minusSeconds(60)); // 1 minute ago
        manager.put(recent);

        manager.evictIdleSessions();

        assertThat(manager.get("recent")).isPresent();
    }

    @Test
    void evictIdleSessions_handlesEmptyRegistry() {
        // Should not throw on empty map
        manager.evictIdleSessions();
    }
}
