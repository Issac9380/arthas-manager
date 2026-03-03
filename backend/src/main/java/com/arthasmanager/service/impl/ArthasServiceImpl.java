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
import com.arthasmanager.service.ArthasService;
import com.arthasmanager.service.FileTransferService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates the full Arthas lifecycle:
 * deploy → attach → execute commands → close.
 *
 * <p>Design pattern: <b>Facade</b> — hides the complexity of K8s exec,
 * file upload, port-forwarding, and Arthas HTTP API behind a clean interface.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArthasServiceImpl implements ArthasService {

    @Value("${arthas.default-arthas-version:3.7.2}")
    private String defaultArthasVersion;

    private final FileTransferService fileTransferService;
    private final ArthasCommandFactory commandFactory;
    private final ArthasCommandExecutor commandExecutor;
    private final ArthasSessionManager sessionManager;
    private final ArthasVersionRegistry versionRegistry;

    @Override
    public void deploy(DeployRequest request) {
        if (request.isUploadJdk()) {
            log.info("Uploading JDK {} to {}/{}/{}", request.getJdkVersion(),
                    request.getNamespace(), request.getPodName(), request.getContainerName());
            fileTransferService.uploadJdk(
                    request.getClusterId(),
                    request.getNamespace(), request.getPodName(),
                    request.getContainerName(), request.getJdkVersion());
        }
        String arthasVersion = resolveArthasVersion(request.getArthasVersion());
        log.info("Deploying Arthas {} to {}/{}/{}", arthasVersion,
                request.getNamespace(), request.getPodName(), request.getContainerName());
        fileTransferService.deployArthas(
                request.getClusterId(),
                request.getNamespace(), request.getPodName(),
                request.getContainerName(), arthasVersion);
    }

    @Override
    public String attach(AttachRequest request) {
        String sessionId = UUID.randomUUID().toString();

        int localPort = fileTransferService.startArthasAndPortForward(
                request.getClusterId(),
                request.getNamespace(), request.getPodName(),
                request.getContainerName(), request.getPid(), sessionId);

        ArthasSession session = ArthasSession.builder()
                .sessionId(sessionId)
                .clusterId(request.getClusterId())
                .namespace(request.getNamespace())
                .podName(request.getPodName())
                .containerName(request.getContainerName())
                .pid(request.getPid())
                .localPort(localPort)
                .createdAt(Instant.now())
                .lastUsedAt(Instant.now())
                .build();

        String internalSessionId = commandExecutor.initSession(session);
        session.setArthasInternalSessionId(internalSessionId);

        sessionManager.put(session);
        log.info("Arthas attached: sessionId={}, cluster={}, pod={}, pid={}",
                sessionId, request.getClusterId(), request.getPodName(), request.getPid());
        return sessionId;
    }

    @Override
    public JsonNode execute(ArthasCommandRequest request) {
        ArthasSession session = sessionManager.get(request.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Session not found: " + request.getSessionId()));

        ArthasCommand command = commandFactory.getCommand(request.getCommandType());
        String commandString = command.buildCommandString(request.getParams());
        log.debug("Built command string: {}", commandString);

        return commandExecutor.execute(session, commandString);
    }

    @Override
    public void close(String sessionId) {
        sessionManager.get(sessionId).ifPresent(session -> {
            commandExecutor.closeSession(session);
            session.close();
        });
        sessionManager.remove(sessionId);
    }

    @Override
    public List<Map<String, Object>> listCommandMeta() {
        return commandFactory.listCommandMeta();
    }

    @Override
    public Map<String, Object> getVersionMatrix() {
        return versionRegistry.getVersionMatrix();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String resolveArthasVersion(String requested) {
        return (requested == null || requested.isBlank()) ? defaultArthasVersion : requested;
    }
}
