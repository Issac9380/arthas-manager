package com.arthasmanager.websocket;

import com.arthasmanager.arthas.executor.ArthasCommandExecutor;
import com.arthasmanager.arthas.session.ArthasSession;
import com.arthasmanager.arthas.session.ArthasSessionManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler that streams Arthas command output in real time.
 *
 * <p>Message protocol (JSON):
 * <pre>
 * Client → Server:
 *   { "action": "exec", "sessionId": "xxx", "command": "dashboard -n 1" }
 *   { "action": "close", "sessionId": "xxx" }
 *
 * Server → Client:
 *   { "type": "result",  "data": { ... } }
 *   { "type": "error",   "message": "..." }
 *   { "type": "closed" }
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArthasWebSocketHandler extends TextWebSocketHandler {

    private final ArthasSessionManager sessionManager;
    private final ArthasCommandExecutor commandExecutor;
    private final ObjectMapper objectMapper;

    /** Maps WebSocket session ID → Arthas session ID */
    private final ConcurrentHashMap<String, String> wsToArthasSession = new ConcurrentHashMap<>();

    @Override
    protected void handleTextMessage(WebSocketSession wsSession, TextMessage message) throws Exception {
        JsonNode msg = objectMapper.readTree(message.getPayload());
        String action    = msg.path("action").asText();
        String sessionId = msg.path("sessionId").asText();
        String command   = msg.path("command").asText();

        switch (action) {
            case "exec" -> {
                ArthasSession arthasSession = sessionManager.get(sessionId).orElse(null);
                if (arthasSession == null) {
                    send(wsSession, Map.of("type", "error", "message", "Session not found: " + sessionId));
                    return;
                }
                wsToArthasSession.put(wsSession.getId(), sessionId);
                JsonNode result = commandExecutor.execute(arthasSession, command);
                send(wsSession, Map.of("type", "result", "data", result));
            }
            case "close" -> {
                send(wsSession, Map.of("type", "closed"));
                wsSession.close();
            }
            default -> send(wsSession, Map.of("type", "error", "message", "Unknown action: " + action));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        wsToArthasSession.remove(session.getId());
        log.debug("WebSocket closed: {} status={}", session.getId(), status);
    }

    private void send(WebSocketSession wsSession, Object payload) throws Exception {
        String json = objectMapper.writeValueAsString(payload);
        if (wsSession.isOpen()) {
            wsSession.sendMessage(new TextMessage(json));
        }
    }
}
