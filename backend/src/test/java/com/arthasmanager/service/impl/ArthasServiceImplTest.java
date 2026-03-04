package com.arthasmanager.service.impl;

import com.arthasmanager.arthas.command.ArthasCommand;
import com.arthasmanager.arthas.executor.ArthasCommandExecutor;
import com.arthasmanager.arthas.factory.ArthasCommandFactory;
import com.arthasmanager.arthas.session.ArthasSession;
import com.arthasmanager.arthas.session.ArthasSessionManager;
import com.arthasmanager.arthas.version.ArthasVersionRegistry;
import com.arthasmanager.model.dto.ArthasCommandRequest;
import com.arthasmanager.model.dto.AttachRequest;
import com.arthasmanager.model.dto.DeployRequest;
import com.arthasmanager.service.FileTransferService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ArthasServiceImplTest {

    @Mock
    private FileTransferService fileTransferService;
    @Mock
    private ArthasCommandFactory commandFactory;
    @Mock
    private ArthasCommandExecutor commandExecutor;
    @Mock
    private ArthasSessionManager sessionManager;
    @Mock
    private ArthasVersionRegistry versionRegistry;

    @InjectMocks
    private ArthasServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "defaultArthasVersion", "3.7.2");
    }

    // ── deploy ────────────────────────────────────────────────────────────────

    @Test
    void deploy_withUploadJdk_callsUploadJdkAndDeployArthas() {
        DeployRequest request = new DeployRequest();
        request.setClusterId("c1");
        request.setNamespace("default");
        request.setPodName("pod-1");
        request.setContainerName("app");
        request.setUploadJdk(true);
        request.setJdkVersion("17");
        request.setArthasVersion("3.7.2");

        service.deploy(request);

        verify(fileTransferService).uploadJdk("c1", "default", "pod-1", "app", "17");
        verify(fileTransferService).deployArthas("c1", "default", "pod-1", "app", "3.7.2");
    }

    @Test
    void deploy_withoutUploadJdk_skipsJdkUpload() {
        DeployRequest request = new DeployRequest();
        request.setClusterId("c1");
        request.setNamespace("default");
        request.setPodName("pod-1");
        request.setContainerName("app");
        request.setUploadJdk(false);
        request.setArthasVersion("3.7.2");

        service.deploy(request);

        verify(fileTransferService, never()).uploadJdk(any(), any(), any(), any(), any());
        verify(fileTransferService).deployArthas("c1", "default", "pod-1", "app", "3.7.2");
    }

    @Test
    void deploy_withBlankArthasVersion_usesDefaultVersion() {
        DeployRequest request = new DeployRequest();
        request.setNamespace("default");
        request.setPodName("pod-1");
        request.setContainerName("app");
        request.setUploadJdk(false);
        request.setArthasVersion("  ");

        service.deploy(request);

        verify(fileTransferService).deployArthas(any(), any(), any(), any(), eq("3.7.2"));
    }

    @Test
    void deploy_withNullArthasVersion_usesDefaultVersion() {
        DeployRequest request = new DeployRequest();
        request.setNamespace("default");
        request.setPodName("pod-1");
        request.setContainerName("app");
        request.setUploadJdk(false);
        request.setArthasVersion(null);

        service.deploy(request);

        verify(fileTransferService).deployArthas(any(), any(), any(), any(), eq("3.7.2"));
    }

    // ── attach ────────────────────────────────────────────────────────────────

    @Test
    void attach_returnsSessionId_andStoresSessionInManager() {
        AttachRequest request = new AttachRequest();
        request.setClusterId("c1");
        request.setNamespace("default");
        request.setPodName("pod-1");
        request.setContainerName("app");
        request.setPid(1234);

        given(fileTransferService.startArthasAndPortForward(eq("c1"), eq("default"), eq("pod-1"), eq("app"), eq(1234), anyString()))
                .willAnswer(inv -> 39395);
        given(commandExecutor.initSession(any())).willReturn("arthas-internal-session-id");

        String sessionId = service.attach(request);

        assertThat(sessionId).isNotBlank();
        verify(sessionManager).put(any(ArthasSession.class));
    }

    @Test
    void attach_setsArthasInternalSessionId() {
        AttachRequest request = new AttachRequest();
        request.setClusterId("c1");
        request.setNamespace("default");
        request.setPodName("pod-1");
        request.setContainerName("app");
        request.setPid(42);

        given(fileTransferService.startArthasAndPortForward(any(), any(), any(), any(), anyInt(), anyString()))
                .willReturn(12345);
        given(commandExecutor.initSession(any())).willReturn("sess-abc");

        service.attach(request);

        ArgumentCaptor<ArthasSession> captor = ArgumentCaptor.forClass(ArthasSession.class);
        verify(sessionManager).put(captor.capture());
        assertThat(captor.getValue().getArthasInternalSessionId()).isEqualTo("sess-abc");
    }

    // ── execute ───────────────────────────────────────────────────────────────

    @Test
    void execute_withValidSession_delegatesToExecutor() throws Exception {
        ArthasSession session = ArthasSession.builder()
                .sessionId("sess-1")
                .localPort(39395)
                .build();
        given(sessionManager.get("sess-1")).willReturn(Optional.of(session));

        ArthasCommand command = mock(ArthasCommand.class);
        given(commandFactory.getCommand("jvm")).willReturn(command);
        given(command.buildCommandString(any())).willReturn("jvm");

        JsonNode resultNode = new ObjectMapper().readTree("{\"state\":\"SUCCEEDED\"}");
        given(commandExecutor.execute(session, "jvm")).willReturn(resultNode);

        ArthasCommandRequest request = new ArthasCommandRequest();
        request.setSessionId("sess-1");
        request.setCommandType("jvm");
        request.setParams(Map.of());

        JsonNode result = service.execute(request);

        assertThat(result.path("state").asText()).isEqualTo("SUCCEEDED");
    }

    @Test
    void execute_withUnknownSessionId_throwsIllegalArgumentException() {
        given(sessionManager.get("no-such-session")).willReturn(Optional.empty());

        ArthasCommandRequest request = new ArthasCommandRequest();
        request.setSessionId("no-such-session");
        request.setCommandType("jvm");

        assertThatThrownBy(() -> service.execute(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no-such-session");
    }

    // ── close ─────────────────────────────────────────────────────────────────

    @Test
    void close_withActiveSession_closesSessionAndRemovesFromManager() {
        ArthasSession session = mock(ArthasSession.class);
        given(sessionManager.get("sess-1")).willReturn(Optional.of(session));

        service.close("sess-1");

        verify(commandExecutor).closeSession(session);
        verify(session).close();
        verify(sessionManager).remove("sess-1");
    }

    @Test
    void close_withUnknownSessionId_onlyCallsRemove() {
        given(sessionManager.get("no-such")).willReturn(Optional.empty());

        service.close("no-such");

        verify(commandExecutor, never()).closeSession(any());
        verify(sessionManager).remove("no-such");
    }

    // ── listCommandMeta ───────────────────────────────────────────────────────

    @Test
    void listCommandMeta_delegatesToFactory() {
        List<Map<String, Object>> meta = List.of(Map.of("type", "jvm"));
        given(commandFactory.listCommandMeta()).willReturn(meta);

        assertThat(service.listCommandMeta()).isSameAs(meta);
    }

    // ── getVersionMatrix ──────────────────────────────────────────────────────

    @Test
    void getVersionMatrix_delegatesToRegistry() {
        Map<String, Object> matrix = Map.of("8", List.of("3.7.2"));
        given(versionRegistry.getVersionMatrix()).willReturn(matrix);

        assertThat(service.getVersionMatrix()).isSameAs(matrix);
    }
}
