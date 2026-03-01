package com.arthasmanager.service.impl;

import com.arthasmanager.service.ClusterService;
import com.arthasmanager.service.FileTransferService;
import com.arthasmanager.service.KubernetesService;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.LocalPortForward;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
 *   <li>Upload the jar to {@code /tmp/arthas/} inside the container via Fabric8 file upload.</li>
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

    private final ClusterService clusterService;
    private final KubernetesService kubernetesService;

    /** sessionId → LocalPortForward */
    private final ConcurrentHashMap<String, LocalPortForward> portForwards = new ConcurrentHashMap<>();

    @Override
    public void deployArthas(String clusterId, String namespace, String podName, String containerName) {
        Path localJar = ensureArthasBootJar();
        KubernetesClient client = clusterService.getClient(clusterId);

        kubernetesService.execCommand(clusterId, namespace, podName, containerName,
                "mkdir", "-p", "/tmp/arthas");

        client.pods()
                .inNamespace(namespace)
                .withName(podName)
                .inContainer(containerName)
                .file("/tmp/arthas/arthas-boot.jar")
                .upload(localJar);

        log.info("Arthas boot jar uploaded to {}/{}/{}", namespace, podName, containerName);
    }

    @Override
    public void uploadJdk(String clusterId, String namespace, String podName, String containerName, String jdkVersion) {
        Path jdkArchive = Paths.get(toolsDir, "jdk-" + jdkVersion + ".tar.gz");
        if (!Files.exists(jdkArchive)) {
            throw new IllegalStateException(
                    "JDK archive not found: " + jdkArchive + ". Please download it via the Tools page first.");
        }

        KubernetesClient client = clusterService.getClient(clusterId);

        kubernetesService.execCommand(clusterId, namespace, podName, containerName,
                "mkdir", "-p", "/tmp/jdk");
        client.pods()
                .inNamespace(namespace)
                .withName(podName)
                .inContainer(containerName)
                .file("/tmp/jdk/jdk.tar.gz")
                .upload(jdkArchive);

        kubernetesService.execCommand(clusterId, namespace, podName, containerName,
                "tar", "-xzf", "/tmp/jdk/jdk.tar.gz", "-C", "/tmp/jdk", "--strip-components=1");

        log.info("JDK {} uploaded and extracted to {}/{}/{}", jdkVersion, namespace, podName, containerName);
    }

    @Override
    public int startArthasAndPortForward(String clusterId, String namespace, String podName,
                                         String containerName, int pid, String sessionId) {
        String javaCmd = resolveJavaCommand(clusterId, namespace, podName, containerName);

        String startCmd = String.format(
                "%s -jar /tmp/arthas/arthas-boot.jar --attach-only --http-port %d --pid %d &",
                javaCmd, arthasApiPort, pid);

        kubernetesService.execCommand(clusterId, namespace, podName, containerName,
                "sh", "-c", startCmd);

        try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        KubernetesClient client = clusterService.getClient(clusterId);
        LocalPortForward pf = client.pods()
                .inNamespace(namespace)
                .withName(podName)
                .portForward(arthasApiPort);

        portForwards.put(sessionId, pf);
        int localPort = pf.getLocalPort();
        log.info("Port-forward established: localhost:{} → {}:{}/arthas", localPort, podName, arthasApiPort);
        return localPort;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String resolveJavaCommand(String clusterId, String namespace, String podName, String containerName) {
        String systemJava = kubernetesService.execCommand(clusterId, namespace, podName, containerName, "which", "java").trim();
        return systemJava.isEmpty() ? "/tmp/jdk/bin/java" : systemJava;
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
