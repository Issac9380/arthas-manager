package com.arthasmanager.controller;

import com.arthasmanager.model.dto.ArthasCommandRequest;
import com.arthasmanager.model.dto.AttachRequest;
import com.arthasmanager.model.dto.DeployRequest;
import com.arthasmanager.model.vo.Result;
import com.arthasmanager.service.ArthasService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/arthas")
@RequiredArgsConstructor
public class ArthasController {

    private final ArthasService arthasService;

    /** Returns command metadata so the frontend can render dynamic forms. */
    @GetMapping("/commands")
    public Result<List<Map<String, Object>>> listCommands() {
        return Result.success(arthasService.listCommandMeta());
    }

    /** Deploys Arthas (and optionally a JDK) into a container. */
    @PostMapping("/deploy")
    public Result<Void> deploy(@RequestBody DeployRequest request) {
        arthasService.deploy(request);
        return Result.success();
    }

    /**
     * Attaches Arthas to a JVM process, sets up port-forwarding,
     * and returns a session ID for subsequent command executions.
     */
    @PostMapping("/attach")
    public Result<String> attach(@RequestBody AttachRequest request) {
        String sessionId = arthasService.attach(request);
        return Result.success(sessionId);
    }

    /** Executes an Arthas command against an active session. */
    @PostMapping("/execute")
    public Result<JsonNode> execute(@RequestBody ArthasCommandRequest request) {
        JsonNode result = arthasService.execute(request);
        return Result.success(result);
    }

    /** Closes the session and tears down the port-forward. */
    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> close(@PathVariable String sessionId) {
        arthasService.close(sessionId);
        return Result.success();
    }
}
