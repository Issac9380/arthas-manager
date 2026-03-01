package com.arthasmanager.arthas.executor;

import com.arthasmanager.arthas.session.ArthasSession;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.stereotype.Component;

/**
 * Sends commands to the Arthas HTTP API running inside the container
 * via the local port-forward established during session creation.
 *
 * <p>Arthas HTTP API endpoint: {@code POST /api}
 *
 * <p>Request payload:
 * <pre>
 * { "action": "exec", "command": "jvm", "sessionId": "xxx" }
 * </pre>
 *
 * <p>Response payload:
 * <pre>
 * { "state": "SUCCEEDED", "body": { "result": { ... } } }
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArthasCommandExecutor {

    private final ObjectMapper objectMapper;

    /**
     * Executes a synchronous Arthas command and returns the full JSON response node.
     *
     * @param session        the active Arthas session (carries base URL + internal session ID)
     * @param commandString  the raw Arthas command string built by an {@link com.arthasmanager.arthas.command.ArthasCommand}
     * @return parsed JSON response from the Arthas HTTP API
     */
    public JsonNode execute(ArthasSession session, String commandString) {
        String url = session.arthasBaseUrl() + "/api";
        log.debug("Executing Arthas command [{}] via {}", commandString, url);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("action", "exec");
        body.put("command", commandString);
        if (session.getArthasInternalSessionId() != null) {
            body.put("sessionId", session.getArthasInternalSessionId());
        }

        return doPost(url, body.toString());
    }

    /**
     * Initialises an Arthas session on the server side.
     * Must be called after Arthas starts inside the container.
     */
    public String initSession(ArthasSession session) {
        String url = session.arthasBaseUrl() + "/api";
        ObjectNode body = objectMapper.createObjectNode();
        body.put("action", "init_session");

        JsonNode response = doPost(url, body.toString());
        String internalId = response.path("sessionId").asText();
        log.info("Arthas internal session initialised: {}", internalId);
        return internalId;
    }

    /**
     * Closes the Arthas session on the server side.
     */
    public void closeSession(ArthasSession session) {
        if (session.getArthasInternalSessionId() == null) return;
        try {
            String url = session.arthasBaseUrl() + "/api";
            ObjectNode body = objectMapper.createObjectNode();
            body.put("action", "close_session");
            body.put("sessionId", session.getArthasInternalSessionId());
            doPost(url, body.toString());
        } catch (Exception e) {
            log.warn("Failed to close Arthas internal session: {}", e.getMessage());
        }
    }

    // ── Internal HTTP call ────────────────────────────────────────────────────

    private JsonNode doPost(String url, String jsonBody) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);
            post.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));

            return httpClient.execute(post, response -> {
                String responseBody = EntityUtils.toString(response.getEntity());
                log.debug("Arthas API response: {}", responseBody);
                return objectMapper.readTree(responseBody);
            });
        } catch (Exception e) {
            log.error("Arthas HTTP API call failed: {}", e.getMessage(), e);
            ObjectNode errorNode = objectMapper.createObjectNode();
            errorNode.put("state", "FAILED");
            errorNode.put("message", e.getMessage());
            return errorNode;
        }
    }
}
