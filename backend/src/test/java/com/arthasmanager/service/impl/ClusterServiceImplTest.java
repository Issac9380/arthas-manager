package com.arthasmanager.service.impl;

import com.arthasmanager.model.cluster.ClusterAuthType;
import com.arthasmanager.model.cluster.ClusterConfig;
import com.arthasmanager.model.cluster.ClusterInfo;
import com.arthasmanager.model.cluster.ClusterStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.VersionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ClusterServiceImplTest {

    @InjectMocks
    private ClusterServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "defaultKubeconfigPath", "");
    }

    // ── listClusters / addCluster / deleteCluster ─────────────────────────────

    @Test
    void listClusters_initially_returnsEmptyList() {
        // Before initDefaultCluster, no clusters registered
        assertThat(service.listClusters()).isEmpty();
    }

    @Test
    void deleteCluster_defaultCluster_throwsIllegalArgument() {
        // Inject a default entry so the guard fires
        ClusterInfo info = ClusterInfo.builder()
                .id("default").name("default")
                .authType(ClusterAuthType.IN_CLUSTER)
                .status(ClusterStatus.UNKNOWN).build();

        @SuppressWarnings("unchecked")
        java.util.concurrent.ConcurrentHashMap<String, ClusterInfo> infoReg =
                (java.util.concurrent.ConcurrentHashMap<String, ClusterInfo>)
                        ReflectionTestUtils.getField(service, "infoRegistry");
        infoReg.put("default", info);

        assertThatThrownBy(() -> service.deleteCluster("default"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Default cluster");
    }

    @Test
    void getClient_unknownCluster_throwsIllegalStateException() {
        assertThatThrownBy(() -> service.getClient("no-such-id"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no-such-id");
    }

    @Test
    void getClient_nullOrBlankId_fallsBackToDefault() {
        // null/blank → resolves to DEFAULT_CLUSTER_ID; since no default is registered, throws
        assertThatThrownBy(() -> service.getClient(null))
                .isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> service.getClient("  "))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── testConnection ────────────────────────────────────────────────────────

    @Test
    void testConnection_withMockedConnectedClient_returnsConnectedStatus() {
        // Use a real ClusterServiceImpl spy so we can stub buildClientInternal
        ClusterServiceImpl spy = org.mockito.Mockito.spy(service);

        KubernetesClient mockClient = mock(KubernetesClient.class);
        given(mockClient.getKubernetesVersion()).willReturn(new VersionInfo.Builder().build());
        doReturn(mockClient).when(spy).buildClientInternal(any());

        ClusterConfig config = ClusterConfig.builder()
                .name("test")
                .authType(ClusterAuthType.TOKEN)
                .apiServerUrl("https://1.2.3.4:6443")
                .token("tok")
                .build();

        ClusterInfo result = spy.testConnection(config);

        assertThat(result.getStatus()).isEqualTo(ClusterStatus.CONNECTED);
        assertThat(result.getStatusMessage()).contains("Connection successful");
    }

    @Test
    void testConnection_whenClientThrows_returnsErrorStatus() {
        ClusterServiceImpl spy = org.mockito.Mockito.spy(service);

        KubernetesClient mockClient = mock(KubernetesClient.class);
        given(mockClient.getKubernetesVersion())
                .willThrow(new RuntimeException("connection refused"));
        doReturn(mockClient).when(spy).buildClientInternal(any());

        ClusterConfig config = ClusterConfig.builder()
                .name("test")
                .authType(ClusterAuthType.IN_CLUSTER)
                .build();

        ClusterInfo result = spy.testConnection(config);

        assertThat(result.getStatus()).isEqualTo(ClusterStatus.DISCONNECTED);
    }

    @Test
    void testConnection_whenBuildClientThrows_returnsErrorStatus() {
        ClusterServiceImpl spy = org.mockito.Mockito.spy(service);
        doThrow(new RuntimeException("bad config")).when(spy).buildClientInternal(any());

        ClusterConfig config = ClusterConfig.builder()
                .name("test")
                .authType(ClusterAuthType.TOKEN)
                .apiServerUrl("https://bad:6443")
                .token("tok")
                .build();

        ClusterInfo result = spy.testConnection(config);

        assertThat(result.getStatus()).isEqualTo(ClusterStatus.ERROR);
        assertThat(result.getStatusMessage()).contains("bad config");
    }

    // ── addCluster ────────────────────────────────────────────────────────────

    @Test
    void addCluster_registersClusterAndReturnsInfo() {
        ClusterServiceImpl spy = org.mockito.Mockito.spy(service);

        KubernetesClient mockClient = mock(KubernetesClient.class);
        given(mockClient.getKubernetesVersion())
                .willThrow(new RuntimeException("no cluster"));
        doReturn(mockClient).when(spy).buildClientInternal(any());

        ClusterConfig config = ClusterConfig.builder()
                .name("new-cluster")
                .authType(ClusterAuthType.TOKEN)
                .apiServerUrl("https://1.2.3.4:6443")
                .token("tok")
                .build();

        ClusterInfo info = spy.addCluster(config);

        assertThat(info.getId()).isNotBlank();
        assertThat(info.getName()).isEqualTo("new-cluster");
        assertThat(info.getAuthType()).isEqualTo(ClusterAuthType.TOKEN);
        assertThat(spy.listClusters()).hasSize(1);
        assertThat(spy.getClusterInfo(info.getId())).isNotNull();
    }

    @Test
    void addCluster_thenDelete_removesCluster() {
        ClusterServiceImpl spy = org.mockito.Mockito.spy(service);

        KubernetesClient mockClient = mock(KubernetesClient.class);
        given(mockClient.getKubernetesVersion())
                .willThrow(new RuntimeException("no cluster"));
        doReturn(mockClient).when(spy).buildClientInternal(any());

        ClusterConfig config = ClusterConfig.builder()
                .name("temp-cluster")
                .authType(ClusterAuthType.IN_CLUSTER)
                .build();

        ClusterInfo info = spy.addCluster(config);
        assertThat(spy.listClusters()).hasSize(1);

        spy.deleteCluster(info.getId());
        assertThat(spy.listClusters()).isEmpty();
    }

    // ── getClusterInfo ────────────────────────────────────────────────────────

    @Test
    void getClusterInfo_unknownId_returnsNull() {
        assertThat(service.getClusterInfo("not-registered")).isNull();
    }
}
