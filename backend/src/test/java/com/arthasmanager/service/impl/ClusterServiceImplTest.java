package com.arthasmanager.service.impl;

import com.arthasmanager.entity.ClusterEntity;
import com.arthasmanager.mapper.ClusterMapper;
import com.arthasmanager.model.cluster.ClusterAuthType;
import com.arthasmanager.model.cluster.ClusterConfig;
import com.arthasmanager.model.cluster.ClusterInfo;
import com.arthasmanager.model.cluster.ClusterStatus;
import com.arthasmanager.security.UserPrincipal;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.VersionInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ClusterServiceImplTest {

    @Mock
    private ClusterMapper clusterMapper;

    @InjectMocks
    private ClusterServiceImpl service;

    private static final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        UserPrincipal principal = new UserPrincipal(TEST_USER_ID, "testuser", "password");
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── listClusters ──────────────────────────────────────────────────────────

    @Test
    void listClusters_initially_returnsEmptyList() {
        given(clusterMapper.findByUserId(TEST_USER_ID)).willReturn(List.of());
        assertThat(service.listClusters()).isEmpty();
    }

    @Test
    void listClusters_returnsMappedInfoForUser() {
        ClusterEntity entity = ClusterEntity.builder()
                .id("cluster-1").userId(TEST_USER_ID).name("my-cluster")
                .authType(ClusterAuthType.TOKEN).apiServerUrl("https://1.2.3.4:6443")
                .status(ClusterStatus.CONNECTED).statusMessage("Connected")
                .build();
        given(clusterMapper.findByUserId(TEST_USER_ID)).willReturn(List.of(entity));

        List<ClusterInfo> result = service.listClusters();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("cluster-1");
        assertThat(result.get(0).getName()).isEqualTo("my-cluster");
    }

    // ── getClient ─────────────────────────────────────────────────────────────

    @Test
    void getClient_unknownCluster_throwsIllegalStateException() {
        given(clusterMapper.findById("no-such-id")).willReturn(null);

        assertThatThrownBy(() -> service.getClient("no-such-id"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getClient_nullOrBlankId_noDefaultCluster_throwsIllegalStateException() {
        given(clusterMapper.findDefaultByUserId(TEST_USER_ID)).willReturn(null);

        assertThatThrownBy(() -> service.getClient(null))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.getClient("  "))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── testConnection ────────────────────────────────────────────────────────

    @Test
    void testConnection_withMockedConnectedClient_returnsConnectedStatus() {
        ClusterServiceImpl spy = org.mockito.Mockito.spy(service);

        KubernetesClient mockClient = mock(KubernetesClient.class);
        given(mockClient.getKubernetesVersion()).willReturn(new VersionInfo.Builder().build());
        doReturn(mockClient).when(spy).buildClientInternal(any());

        ClusterConfig config = ClusterConfig.builder()
                .name("test").authType(ClusterAuthType.TOKEN)
                .apiServerUrl("https://1.2.3.4:6443").token("tok")
                .build();

        ClusterInfo result = spy.testConnection(config);

        assertThat(result.getStatus()).isEqualTo(ClusterStatus.CONNECTED);
        assertThat(result.getStatusMessage()).contains("Connection successful");
    }

    @Test
    void testConnection_whenClientThrows_returnsDisconnectedStatus() {
        ClusterServiceImpl spy = org.mockito.Mockito.spy(service);

        KubernetesClient mockClient = mock(KubernetesClient.class);
        given(mockClient.getKubernetesVersion()).willThrow(new RuntimeException("connection refused"));
        doReturn(mockClient).when(spy).buildClientInternal(any());

        ClusterConfig config = ClusterConfig.builder()
                .name("test").authType(ClusterAuthType.IN_CLUSTER).build();

        ClusterInfo result = spy.testConnection(config);

        assertThat(result.getStatus()).isEqualTo(ClusterStatus.DISCONNECTED);
    }

    @Test
    void testConnection_whenBuildClientThrows_returnsErrorStatus() {
        ClusterServiceImpl spy = org.mockito.Mockito.spy(service);
        doThrow(new RuntimeException("bad config")).when(spy).buildClientInternal(any());

        ClusterConfig config = ClusterConfig.builder()
                .name("test").authType(ClusterAuthType.TOKEN)
                .apiServerUrl("https://bad:6443").token("tok").build();

        ClusterInfo result = spy.testConnection(config);

        assertThat(result.getStatus()).isEqualTo(ClusterStatus.ERROR);
        assertThat(result.getStatusMessage()).contains("bad config");
    }

    // ── addCluster ────────────────────────────────────────────────────────────

    @Test
    void addCluster_insertsToDbAndReturnsInfo() {
        ClusterServiceImpl spy = org.mockito.Mockito.spy(service);

        KubernetesClient mockClient = mock(KubernetesClient.class);
        given(mockClient.getKubernetesVersion()).willThrow(new RuntimeException("no cluster"));
        doReturn(mockClient).when(spy).buildClientInternal(any());

        ClusterConfig config = ClusterConfig.builder()
                .name("new-cluster").authType(ClusterAuthType.TOKEN)
                .apiServerUrl("https://1.2.3.4:6443").token("tok")
                .build();

        ClusterInfo info = spy.addCluster(config);

        assertThat(info.getId()).isNotBlank();
        assertThat(info.getName()).isEqualTo("new-cluster");
        assertThat(info.getAuthType()).isEqualTo(ClusterAuthType.TOKEN);

        ArgumentCaptor<ClusterEntity> captor = ArgumentCaptor.forClass(ClusterEntity.class);
        verify(clusterMapper).insert(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(TEST_USER_ID);
    }

    @Test
    void addCluster_thenDelete_callsMapperDeleteById() {
        ClusterServiceImpl spy = org.mockito.Mockito.spy(service);

        KubernetesClient mockClient = mock(KubernetesClient.class);
        given(mockClient.getKubernetesVersion()).willThrow(new RuntimeException("no cluster"));
        doReturn(mockClient).when(spy).buildClientInternal(any());

        ClusterConfig config = ClusterConfig.builder()
                .name("temp").authType(ClusterAuthType.IN_CLUSTER).build();

        ClusterInfo info = spy.addCluster(config);
        spy.deleteCluster(info.getId());

        verify(clusterMapper).deleteById(info.getId());
    }

    // ── getClusterInfo ────────────────────────────────────────────────────────

    @Test
    void getClusterInfo_unknownId_returnsNull() {
        given(clusterMapper.findById("not-registered")).willReturn(null);
        assertThat(service.getClusterInfo("not-registered")).isNull();
    }

    @Test
    void getClusterInfo_existingId_returnsInfo() {
        ClusterEntity entity = ClusterEntity.builder()
                .id("c-1").userId(TEST_USER_ID).name("prod")
                .authType(ClusterAuthType.KUBECONFIG)
                .status(ClusterStatus.CONNECTED).statusMessage("Connected")
                .build();
        given(clusterMapper.findById("c-1")).willReturn(entity);

        ClusterInfo info = service.getClusterInfo("c-1");

        assertThat(info).isNotNull();
        assertThat(info.getName()).isEqualTo("prod");
    }
}
