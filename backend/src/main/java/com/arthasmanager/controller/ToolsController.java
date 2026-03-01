package com.arthasmanager.controller;

import com.arthasmanager.model.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Manages local tool cache (JDK archives, Arthas jars).
 * The frontend's "Tools" page lets operators download or upload binaries
 * that are later pushed to containers during the deploy step.
 */
@Slf4j
@RestController
@RequestMapping("/api/tools")
public class ToolsController {

    @Value("${arthas.tools-dir}")
    private String toolsDir;

    /** Lists all files currently in the local tools cache. */
    @GetMapping
    public Result<List<Map<String, Object>>> listTools() throws IOException {
        Path dir = Paths.get(toolsDir);
        if (!Files.exists(dir)) return Result.success(List.of());

        List<Map<String, Object>> files = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(dir, 2)) {
            stream.filter(Files::isRegularFile).forEach(p -> files.add(Map.of(
                    "name", p.getFileName().toString(),
                    "path", dir.relativize(p).toString(),
                    "size", safeSize(p)
            )));
        }
        return Result.success(files);
    }

    /**
     * Downloads a remote file (e.g. JDK tar.gz from Adoptium) into the tools cache.
     * The caller supplies the URL and the desired local filename.
     */
    @PostMapping("/download")
    public Result<String> downloadTool(@RequestBody Map<String, String> body) {
        String url      = body.get("url");
        String filename = body.get("filename");
        if (url == null || filename == null) {
            return Result.error("url and filename are required");
        }

        Path target = Paths.get(toolsDir, filename);
        try {
            Files.createDirectories(target.getParent());
            log.info("Downloading tool from {} → {}", url, target);
            try (InputStream in = new URL(url).openStream()) {
                Files.copy(in, target);
            }
            log.info("Tool downloaded: {}", target);
            return Result.success(target.toString());
        } catch (Exception e) {
            log.error("Download failed: {}", e.getMessage(), e);
            return Result.error("Download failed: " + e.getMessage());
        }
    }

    /** Accepts a file upload (e.g. JDK tar.gz) and stores it in the tools cache. */
    @PostMapping("/upload")
    public Result<String> uploadTool(@RequestParam("file") MultipartFile file,
                                     @RequestParam("subdir") String subdir) throws IOException {
        Path dir = Paths.get(toolsDir, subdir);
        Files.createDirectories(dir);
        Path target = dir.resolve(file.getOriginalFilename());
        file.transferTo(target.toFile());
        log.info("Tool uploaded: {}", target);
        return Result.success(target.toString());
    }

    /** Deletes a file from the tools cache. */
    @DeleteMapping
    public Result<Void> deleteTool(@RequestParam String path) throws IOException {
        Path target = Paths.get(toolsDir, path);
        Files.deleteIfExists(target);
        return Result.success();
    }

    private long safeSize(Path p) {
        try { return Files.size(p); } catch (IOException e) { return -1; }
    }
}
