package com.arthasmanager.service.impl;

import com.arthasmanager.service.FileTransferService;
import com.arthasmanager.service.KubernetesService;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.LocalPortForward;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deploys JDK and Arthas into Pod containers using kubectl-style Fabric8 APIs.
 *
 * <p>Deployment flow:
 * <ol>
 *   <li>Download arthas-boot.jar to local tools cache (once).</li>
 *   <li>Upload the jar to {@code /tmp/arthas/} inside the container via {@code kubectl cp}.</li>
 *   <li>Start Arthas: {@code java -jar /tmp/arthas/arthas-boot.jar --attach-only <pid>}.</li>
 *   <li>Set up a Fabric8 port-forward so the backend can reach Arthas HTTP API.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileTransferServiceImpl implements FileTransferService {

    @Value("${arthas.tools-dir}")
    private String toolsDir;

    @Value("${arthas.boot-jar-url}")
    private String bootJarUrl;

    @Value("${arthas.default-api-port:39394}")
    private int arthasApiPort;

    private final KubernetesClient kubernetesClient;
    private final KubernetesService kubernetesService;

    /** sessionId → LocalPortForward — kept so callers can close them later via ArthasSession. */
    private final ConcurrentHashMap<String, LocalPortForward> portForwards = new ConcurrentHashMap<>();

    @Override
    public void deployArthas(String namespace, String podName, String containerName) {
        Path localJar = ensureArthasBootJar();

        // Create target directory inside container
        kubernetesService.execCommand(namespace, podName, containerName,
                "mkdir", "-p", "/tmp/arthas");

        // Upload via Fabric8 file upload
        kubernetesClient.pods()
                .inNamespace(namespace)
                .withName(podName)
                .inContainer(containerName)
                .file("/tmp/arthas/arthas-boot.jar")
                .upload(localJar);

        log.info("Arthas boot jar uploaded to {}/{}/{}", namespace, podName, containerName);
    }

    @Override
    public void uploadJdk(String namespace, String podName, String containerName, String jdkVersion) {
        // Locate cached JDK tarball
        Path jdkArchive = Paths.get(toolsDir, "jdk-" + jdkVersion + ".tar.gz");
        if (!Files.exists(jdkArchive)) {
            throw new IllegalStateException(
                    "JDK archive not found: " + jdkArchive +
                    ". Please download it via the Tools page first.");
        }

        // Create extract directory and upload
        kubernetesService.execCommand(namespace, podName, containerName,
                "mkdir", "-p", "/tmp/jdk");
        kubernetesClient.pods()
                .inNamespace(namespace)
                .withName(podName)
                .inContainer(containerName)
                .file("/tmp/jdk/jdk.tar.gz")
                .upload(jdkArchive);

        // Extract in-place
        kubernetesService.execCommand(namespace, podName, containerName,
                "tar", "-xzf", "/tmp/jdk/jdk.tar.gz", "-C", "/tmp/jdk", "--strip-components=1");

        log.info("JDK {} uploaded and extracted to {}/{}/{}", jdkVersion, namespace, podName, containerName);
    }

    @Override
    public int startArthasAndPortForward(String namespace, String podName,
                                         String containerName, int pid, String sessionId) {
        // Determine java executable path
        String javaCmd = resolveJavaCommand(namespace, podName, containerName);

        // Start Arthas in background, binding HTTP API to the default port
        String startCmd = String.format(
                "%s -jar /tmp/arthas/arthas-boot.jar --attach-only --http-port %d --pid %d &",
                javaCmd, arthasApiPort, pid);

        kubernetesService.execCommand(namespace, podName, containerName,
                "sh", "-c", startCmd);

        // Give Arthas a moment to bind the port
        try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Set up port-forward: random local port → container's arthasApiPort
        LocalPortForward pf = kubernetesClient.pods()
                .inNamespace(namespace)
                .withName(podName)
                .portForward(arthasApiPort);

        portForwards.put(sessionId, pf);
        int localPort = pf.getLocalPort();
        log.info("Port-forward established: localhost:{} → {}:{}/arthas", localPort, podName, arthasApiPort);
        return localPort;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String resolveJavaCommand(String namespace, String podName, String containerName) {
        String systemJava = kubernetesService.execCommand(namespace, podName, containerName, "which", "java").trim();
        if (!systemJava.isEmpty()) return systemJava;
        // Fall back to uploaded JDK
        return "/tmp/jdk/bin/java";
    }

    private Path ensureArthasBootJar() {
        Path dir = Paths.get(toolsDir, "arthas");
        Path jar = dir.resolve("arthas-boot.jar");
        if (Files.exists(jar)) return jar;

        try {
            Files.createDirectories(dir);
            log.info("Downloading arthas-boot.jar from {}", bootJarUrl);
            try (InputStream in = new URL(bootJarUrl).openStream()) {
                Files.copy(in, jar);
            }
            log.info("arthas-boot.jar cached at {}", jar);
        } catch (Exception e) {
            throw new RuntimeException("Failed to download arthas-boot.jar: " + e.getMessage(), e);
        }
        return jar;
    }
}
