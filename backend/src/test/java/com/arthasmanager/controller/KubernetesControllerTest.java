package com.arthasmanager.controller;

import com.arthasmanager.model.dto.JavaProcessInfo;
import com.arthasmanager.model.dto.NamespaceInfo;
import com.arthasmanager.model.dto.PodInfo;
import com.arthasmanager.security.JwtUtil;
import com.arthasmanager.service.KubernetesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(KubernetesController.class)
@WithMockUser
class KubernetesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KubernetesService kubernetesService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    // ── /api/k8s/namespaces ───────────────────────────────────────────────────

    @Test
    void listNamespaces_withoutClusterId_callsServiceWithNull() throws Exception {
        given(kubernetesService.listNamespaces(null))
                .willReturn(List.of(
                        NamespaceInfo.builder().name("default").build(),
                        NamespaceInfo.builder().name("kube-system").build()
                ));

        mockMvc.perform(get("/api/k8s/namespaces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("default"));

        verify(kubernetesService).listNamespaces(null);
    }

    @Test
    void listNamespaces_withClusterId_passesThroughToService() throws Exception {
        given(kubernetesService.listNamespaces("cluster-42"))
                .willReturn(List.of(NamespaceInfo.builder().name("java-apps").build()));

        mockMvc.perform(get("/api/k8s/namespaces").param("clusterId", "cluster-42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("java-apps"));

        verify(kubernetesService).listNamespaces("cluster-42");
    }

    // ── /api/k8s/namespaces/{namespace}/pods ─────────────────────────────────

    @Test
    void listPods_returnsPodsForNamespace() throws Exception {
        PodInfo pod = PodInfo.builder()
                .name("spring-app-1")
                .namespace("java-apps")
                .status("Running")
                .build();

        given(kubernetesService.listPods(null, "java-apps")).willReturn(List.of(pod));

        mockMvc.perform(get("/api/k8s/namespaces/java-apps/pods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("spring-app-1"))
                .andExpect(jsonPath("$.data[0].status").value("Running"));
    }

    @Test
    void listPods_withClusterId_passesClusterIdToService() throws Exception {
        given(kubernetesService.listPods("c1", "ns1")).willReturn(List.of());

        mockMvc.perform(get("/api/k8s/namespaces/ns1/pods").param("clusterId", "c1"))
                .andExpect(status().isOk());

        verify(kubernetesService).listPods("c1", "ns1");
    }

    // ── /api/k8s/namespaces/{ns}/pods/{pod}/containers/{container}/processes ─

    @Test
    void listJavaProcesses_returnsProcessList() throws Exception {
        JavaProcessInfo proc = JavaProcessInfo.builder()
                .pid(1234)
                .mainClass("com.example.App")
                .build();

        given(kubernetesService.listJavaProcesses(null, "ns", "pod", "app"))
                .willReturn(List.of(proc));

        mockMvc.perform(get("/api/k8s/namespaces/ns/pods/pod/containers/app/processes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].pid").value(1234))
                .andExpect(jsonPath("$.data[0].mainClass").value("com.example.App"));
    }

    // ── /api/k8s/namespaces/{ns}/pods/{pod}/containers/{container}/java ──────

    @Test
    void hasJava_whenJavaPresent_returnsTrue() throws Exception {
        given(kubernetesService.hasJava(null, "ns", "pod", "app")).willReturn(true);

        mockMvc.perform(get("/api/k8s/namespaces/ns/pods/pod/containers/app/java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void hasJava_whenJavaAbsent_returnsFalse() throws Exception {
        given(kubernetesService.hasJava(null, "ns", "pod", "app")).willReturn(false);

        mockMvc.perform(get("/api/k8s/namespaces/ns/pods/pod/containers/app/java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));
    }
}
