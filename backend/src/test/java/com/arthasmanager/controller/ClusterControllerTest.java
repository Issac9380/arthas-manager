package com.arthasmanager.controller;

import com.arthasmanager.model.cluster.ClusterAuthType;
import com.arthasmanager.model.cluster.ClusterConfig;
import com.arthasmanager.model.cluster.ClusterInfo;
import com.arthasmanager.model.cluster.ClusterStatus;
import com.arthasmanager.service.ClusterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClusterController.class)
class ClusterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClusterService clusterService;

    @Autowired
    private ObjectMapper objectMapper;

    private ClusterInfo sampleCluster() {
        return ClusterInfo.builder()
                .id("cluster-1")
                .name("my-cluster")
                .authType(ClusterAuthType.KUBECONFIG)
                .apiServerUrl("https://1.2.3.4:6443")
                .defaultCluster(true)
                .status(ClusterStatus.CONNECTED)
                .statusMessage("Connected")
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    @Test
    void listClusters_returns200WithList() throws Exception {
        given(clusterService.listClusters()).willReturn(List.of(sampleCluster()));

        mockMvc.perform(get("/api/clusters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value("cluster-1"))
                .andExpect(jsonPath("$.data[0].status").value("CONNECTED"));
    }

    @Test
    void listClusters_emptyList_returnsEmptyArray() throws Exception {
        given(clusterService.listClusters()).willReturn(List.of());

        mockMvc.perform(get("/api/clusters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void addCluster_validRequest_returns200WithClusterInfo() throws Exception {
        ClusterConfig config = ClusterConfig.builder()
                .name("new-cluster")
                .authType(ClusterAuthType.TOKEN)
                .apiServerUrl("https://5.6.7.8:6443")
                .token("my-token")
                .build();

        given(clusterService.addCluster(any(ClusterConfig.class))).willReturn(sampleCluster());

        mockMvc.perform(post("/api/clusters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("cluster-1"));
    }

    @Test
    void addCluster_delegatesToService() throws Exception {
        ClusterConfig config = ClusterConfig.builder()
                .name("test")
                .authType(ClusterAuthType.IN_CLUSTER)
                .build();

        given(clusterService.addCluster(any())).willReturn(sampleCluster());

        mockMvc.perform(post("/api/clusters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(config)));

        verify(clusterService).addCluster(any(ClusterConfig.class));
    }

    @Test
    void testConnection_validRequest_returnsTestResult() throws Exception {
        ClusterConfig config = ClusterConfig.builder()
                .name("probe-cluster")
                .authType(ClusterAuthType.KUBECONFIG)
                .kubeconfigContent("apiVersion: v1\n")
                .build();

        ClusterInfo probeResult = ClusterInfo.builder()
                .name("probe-cluster")
                .authType(ClusterAuthType.KUBECONFIG)
                .status(ClusterStatus.CONNECTED)
                .statusMessage("Connection successful")
                .build();

        given(clusterService.testConnection(any())).willReturn(probeResult);

        mockMvc.perform(post("/api/clusters/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.statusMessage").value("Connection successful"));
    }

    @Test
    void deleteCluster_existingId_returns200() throws Exception {
        mockMvc.perform(delete("/api/clusters/cluster-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(clusterService).deleteCluster("cluster-1");
    }

    @Test
    void deleteCluster_returnsNullData() throws Exception {
        mockMvc.perform(delete("/api/clusters/cluster-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
