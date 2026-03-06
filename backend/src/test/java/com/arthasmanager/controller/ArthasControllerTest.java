package com.arthasmanager.controller;

import com.arthasmanager.model.dto.ArthasCommandRequest;
import com.arthasmanager.model.dto.AttachRequest;
import com.arthasmanager.model.dto.DeployRequest;
import com.arthasmanager.security.JwtUtil;
import com.arthasmanager.service.ArthasService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ArthasController.class)
@WithMockUser
class ArthasControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ArthasService arthasService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listCommands_returns200WithCommandList() throws Exception {
        List<Map<String, Object>> meta = List.of(
                Map.of("type", "dashboard", "displayName", "Dashboard"),
                Map.of("type", "watch", "displayName", "Watch")
        );
        given(arthasService.listCommandMeta()).willReturn(meta);

        mockMvc.perform(get("/api/arthas/commands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].type").value("dashboard"))
                .andExpect(jsonPath("$.data[1].type").value("watch"));
    }

    @Test
    void deploy_validRequest_returns200WithNullData() throws Exception {
        DeployRequest request = new DeployRequest();
        request.setClusterId("cluster-1");
        request.setNamespace("default");
        request.setPodName("my-pod");
        request.setContainerName("app");

        mockMvc.perform(post("/api/arthas/deploy").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(arthasService).deploy(any(DeployRequest.class));
    }

    @Test
    void attach_validRequest_returnsSessionId() throws Exception {
        AttachRequest request = new AttachRequest();
        request.setClusterId("cluster-1");
        request.setNamespace("default");
        request.setPodName("my-pod");
        request.setContainerName("app");
        request.setPid(1234);

        given(arthasService.attach(any(AttachRequest.class))).willReturn("session-uuid-123");

        mockMvc.perform(post("/api/arthas/attach").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("session-uuid-123"));
    }

    @Test
    void execute_validRequest_returnsJsonResult() throws Exception {
        ArthasCommandRequest request = new ArthasCommandRequest();
        request.setSessionId("session-uuid-123");
        request.setCommandType("dashboard");

        JsonNode fakeResult = objectMapper.readTree("{\"status\":\"OK\",\"body\":{\"cpu\":12.5}}");
        given(arthasService.execute(any(ArthasCommandRequest.class))).willReturn(fakeResult);

        mockMvc.perform(post("/api/arthas/execute").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("OK"))
                .andExpect(jsonPath("$.data.body.cpu").value(12.5));
    }

    @Test
    void close_existingSession_returns200WithNullData() throws Exception {
        mockMvc.perform(delete("/api/arthas/sessions/session-uuid-123").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(arthasService).close("session-uuid-123");
    }
}
