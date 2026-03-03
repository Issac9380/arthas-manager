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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Deploys JDK and Arthas into Pod containers using Fabric8 APIs.
 *
 * <h3>Arthas deployment strategy</h3>
 * <p>Downloads the <em>full</em> Arthas binary distribution zip for the requested version
 * from Maven Central, caches it locally under {@code tools-dir/arthas/{version}/}, and
 * uploads the essential jars to {@code /tmp/arthas/} inside the container.
 * Because the full distribution is pre-staged, the container needs <strong>no internet
 * access</strong> at attach time — arthas-boot.jar finds its peers via
 * {@code --arthas-home /tmp/arthas}.
 *
 * <h3>Essential files uploaded</h3>
 * <ul>
 *   <li>arthas-boot.jar  — launcher</li>
 *   <li>arthas-agent.jar — Java agent injected into the target JVM</li>
 *   <li>arthas-core.jar  — core diagnostics logic</li>
 *   <li>arthas-spy.jar   — bytecode transformer</li>
 *   <li>arthas.properties — runtime configuration</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileTransferServiceImpl implements FileTransferService {

    private static final List<String> ESSENTIAL_FILES = List.of(
            "arthas-boot.jar", "arthas-agent.jar", "arthas-core.jar",
            "arthas-spy.jar", "arthas.properties");

    @Value("${arthas.tools-dir}")
    private String toolsDir;

    @Value("${arthas.distribution-url}")
    private String distributionUrl;

    @Value("${arthas.default-api-port:39394}")
    private int arthasApiPort;

    private final ClusterService clusterService;
    private final KubernetesService kubernetesService;

    /** sessionId → active port-forward */
    private final ConcurrentHashMap<String, LocalPortForward> portForwards = new ConcurrentHashMap<>();

    // ── FileTransferService ───────────────────────────────────────────────────

    @Override
    public void deployArthas(String clusterId, String namespace, String podName,
                             String containerName, String arthasVersion) {
        Path cacheDir = ensureArthasDistribution(arthasVersion);
        KubernetesClient client = clusterService.getClient(clusterId);

        // Clean slate inside the container
        kubernetesService.execCommand(clusterId, namespace, podName, containerName,
                "sh", "-c", "rm -rf /tmp/arthas && mkdir -p /tmp/arthas");

        // Upload each essential file
        for (String filename : ESSENTIAL_FILES) {
            Path localFile = cacheDir.resolve(filename);
            if (!Files.exists(localFile)) {
                log.warn("Arthas {} essential file not in cache, skipping: {}", arthasVersion, filename);
                continue;
            }
            client.pods()
                    .inNamespace(namespace)
                    .withName(podName)
                    .inContainer(containerName)
                    .file("/tmp/arthas/" + filename)
                    .upload(localFile);
            log.debug("  uploaded: /tmp/arthas/{}", filename);
        }

        log.info("Arthas {} deployed to {}/{}/{}", arthasVersion, namespace, podName, containerName);
    }

    @Override
    public void uploadJdk(String clusterId, String namespace, String podName,
                          String containerName, String jdkVersion) {
        Path jdkArchive = Paths.get(toolsDir, "jdk", "jdk-" + jdkVersion + ".tar.gz");
        if (!Files.exists(jdkArchive)) {
            throw new IllegalStateException(
                    "JDK archive not found: " + jdkArchive +
                    ". Please place jdk-" + jdkVersion + ".tar.gz in the tools/jdk/ directory.");
        }

        KubernetesClient client = clusterService.getClient(clusterId);

        kubernetesService.execCommand(clusterId, namespace, podName, containerName,
                "sh", "-c", "rm -rf /tmp/jdk && mkdir -p /tmp/jdk");

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

        // --arthas-home tells arthas-boot.jar to use the pre-uploaded local jars
        // instead of downloading them from the internet at runtime
        String startCmd = String.format(
                "%s -jar /tmp/arthas/arthas-boot.jar" +
                " --arthas-home /tmp/arthas" +
                " --attach-only --http-port %d --pid %d &",
                javaCmd, arthasApiPort, pid);

        kubernetesService.execCommand(clusterId, namespace, podName, containerName, "sh", "-c", startCmd);

        try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        KubernetesClient client = clusterService.getClient(clusterId);
        LocalPortForward pf = client.pods()
                .inNamespace(namespace)
                .withName(podName)
                .portForward(arthasApiPort);

        portForwards.put(sessionId, pf);
        int localPort = pf.getLocalPort();
        log.info("Port-forward established: localhost:{} → {}/container:{}", localPort, podName, arthasApiPort);
        return localPort;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Ensures the Arthas distribution for the given version is extracted to the local cache.
     * Returns the cache directory containing the essential jars.
     */
    Path ensureArthasDistribution(String version) {
        Path cacheDir = Paths.get(toolsDir, "arthas", version);

        if (isCacheComplete(cacheDir)) {
            log.debug("Arthas {} found in local cache: {}", version, cacheDir);
            return cacheDir;
        }

        String url = distributionUrl.replace("{version}", version);
        Path zipPath = Paths.get(toolsDir, "arthas", "arthas-" + version + "-bin.zip");

        try {
            Files.createDirectories(cacheDir);
            log.info("Downloading Arthas {} from {}", version, url);

            try (InputStream in = new URL(url).openStream()) {
                Files.copy(in, zipPath, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("Arthas {} downloaded ({} bytes), extracting essential files…",
                    version, Files.size(zipPath));

            extractEssentialFiles(zipPath, cacheDir);
            Files.deleteIfExists(zipPath);

            log.info("Arthas {} cached at {}", version, cacheDir);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to download/extract Arthas " + version + " from " + url + ": " + e.getMessage(), e);
        }

        return cacheDir;
    }

    /** Returns {@code true} only when every jar in {@link #ESSENTIAL_FILES} is present. */
    private boolean isCacheComplete(Path dir) {
        if (!Files.isDirectory(dir)) return false;
        return ESSENTIAL_FILES.stream()
                .filter(f -> f.endsWith(".jar"))
                .allMatch(f -> Files.exists(dir.resolve(f)));
    }

    /**
     * Extracts only {@link #ESSENTIAL_FILES} from a zip archive.
     * Handles both flat and single-top-directory zip layouts by stripping the leading path.
     */
    private void extractEssentialFiles(Path zipPath, Path destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String filename = Paths.get(entry.getName()).getFileName().toString();
                    if (ESSENTIAL_FILES.contains(filename)) {
                        Path dest = destDir.resolve(filename);
                        Files.copy(zis, dest, StandardCopyOption.REPLACE_EXISTING);
                        log.debug("  extracted: {} ({} bytes)", filename, Files.size(dest));
                    }
                }
                zis.closeEntry();
            }
        }
    }

    /** Prefers system {@code java} on PATH; falls back to the uploaded JDK at {@code /tmp/jdk}. */
    private String resolveJavaCommand(String clusterId, String namespace,
                                      String podName, String containerName) {
        String systemJava = kubernetesService
                .execCommand(clusterId, namespace, podName, containerName, "which", "java")
                .trim();
        return systemJava.isEmpty() ? "/tmp/jdk/bin/java" : systemJava;
    }
}
