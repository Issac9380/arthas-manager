package com.arthasmanager.arthas.session;

import io.fabric8.kubernetes.client.LocalPortForward;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ArthasSessionTest {

    @Test
    void arthasBaseUrl_returnsLocalhostWithPort() {
        ArthasSession session = ArthasSession.builder()
                .sessionId("sid-1")
                .localPort(39400)
                .build();

        assertThat(session.arthasBaseUrl()).isEqualTo("http://127.0.0.1:39400");
    }

    @Test
    void close_withPortForward_closesIt() throws Exception {
        LocalPortForward portForward = mock(LocalPortForward.class);
        ArthasSession session = ArthasSession.builder()
                .sessionId("sid-2")
                .portForward(portForward)
                .build();

        session.close();

        verify(portForward, times(1)).close();
    }

    @Test
    void close_withNullPortForward_doesNotThrow() {
        ArthasSession session = ArthasSession.builder()
                .sessionId("sid-3")
                .portForward(null)
                .build();

        // Should not throw
        session.close();
    }

    @Test
    void close_whenPortForwardThrows_swallowsException() throws Exception {
        LocalPortForward portForward = mock(LocalPortForward.class);
        doThrow(new RuntimeException("close error")).when(portForward).close();

        ArthasSession session = ArthasSession.builder()
                .sessionId("sid-4")
                .portForward(portForward)
                .build();

        // Should not propagate exception
        session.close();
    }

    @Test
    void builder_setsAllFields() {
        Instant now = Instant.now();
        ArthasSession session = ArthasSession.builder()
                .sessionId("sid-5")
                .clusterId("cluster-1")
                .namespace("default")
                .podName("my-pod")
                .containerName("app")
                .pid(1234)
                .localPort(39500)
                .arthasInternalSessionId("arthas-internal-id")
                .createdAt(now)
                .lastUsedAt(now)
                .build();

        assertThat(session.getSessionId()).isEqualTo("sid-5");
        assertThat(session.getClusterId()).isEqualTo("cluster-1");
        assertThat(session.getNamespace()).isEqualTo("default");
        assertThat(session.getPodName()).isEqualTo("my-pod");
        assertThat(session.getContainerName()).isEqualTo("app");
        assertThat(session.getPid()).isEqualTo(1234);
        assertThat(session.getLocalPort()).isEqualTo(39500);
        assertThat(session.getArthasInternalSessionId()).isEqualTo("arthas-internal-id");
        assertThat(session.getCreatedAt()).isEqualTo(now);
        assertThat(session.getLastUsedAt()).isEqualTo(now);
    }
}
