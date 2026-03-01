package com.arthasmanager.service.impl;

import com.arthasmanager.model.dto.ContainerInfo;
import com.arthasmanager.model.dto.JavaProcessInfo;
import com.arthasmanager.model.dto.NamespaceInfo;
import com.arthasmanager.model.dto.PodInfo;
import com.arthasmanager.service.ClusterService;
import com.arthasmanager.service.KubernetesService;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KubernetesServiceImpl implements KubernetesService {

    private final ClusterService clusterService;

    @Override
    public List<NamespaceInfo> listNamespaces(String clusterId) {
        return client(clusterId).namespaces().list().getItems().stream()
                .map(ns -> NamespaceInfo.builder()
                        .name(ns.getMetadata().getName())
                        .status(ns.getStatus() != null ? ns.getStatus().getPhase() : "Unknown")
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<PodInfo> listPods(String clusterId, String namespace) {
        return client(clusterId).pods().inNamespace(namespace).list().getItems().stream()
                .map(this::toPodInfo)
                .collect(Collectors.toList());
    }

    @Override
    public List<JavaProcessInfo> listJavaProcesses(String clusterId, String namespace, String podName, String containerName) {
        String output = execCommand(clusterId, namespace, podName, containerName, "jps", "-l");
        List<JavaProcessInfo> processes = new ArrayList<>();
        for (String line : output.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\s+", 2);
            if (parts.length < 1) continue;
            try {
                int pid = Integer.parseInt(parts[0]);
                String mainClass = parts.length > 1 ? parts[1] : "Unknown";
                if (mainClass.contains("arthas") || mainClass.equals("sun.tools.jps.Jps")) continue;
                processes.add(JavaProcessInfo.builder().pid(pid).mainClass(mainClass).build());
            } catch (NumberFormatException e) {
                log.debug("Skipping jps line: {}", line);
            }
        }
        return processes;
    }

    @Override
    public String execCommand(String clusterId, String namespace, String podName, String containerName, String... command) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        CountDownLatch latch = new CountDownLatch(1);

        try (ExecWatch watch = client(clusterId).pods()
                .inNamespace(namespace)
                .withName(podName)
                .inContainer(containerName)
                .writingOutput(out)
                .writingError(err)
                .usingListener(new io.fabric8.kubernetes.client.dsl.ExecListener() {
                    @Override public void onClose(int code, String reason) { latch.countDown(); }
                    @Override public void onFailure(Throwable t, Response failureResponse) {
                        log.warn("Exec failed: {}", t.getMessage());
                        latch.countDown();
                    }
                })
                .exec(command)) {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                log.warn("Exec timed out for pod {}/{}", namespace, podName);
            }
        } catch (Exception e) {
            log.error("Exec command failed: {}", e.getMessage(), e);
            return "";
        }

        String stderr = err.toString();
        if (!stderr.isBlank()) log.debug("Exec stderr: {}", stderr);
        return out.toString();
    }

    @Override
    public boolean hasJava(String clusterId, String namespace, String podName, String containerName) {
        String output = execCommand(clusterId, namespace, podName, containerName, "which", "java");
        return output != null && !output.trim().isEmpty();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private KubernetesClient client(String clusterId) {
        return clusterService.getClient(clusterId);
    }

    private PodInfo toPodInfo(Pod pod) {
        List<ContainerInfo> containers = pod.getSpec().getContainers().stream()
                .map(c -> ContainerInfo.builder()
                        .name(c.getName())
                        .image(c.getImage())
                        .arthasDeployed(false)
                        .build())
                .collect(Collectors.toList());

        String status = pod.getStatus() != null && pod.getStatus().getPhase() != null
                ? pod.getStatus().getPhase() : "Unknown";

        return PodInfo.builder()
                .name(pod.getMetadata().getName())
                .namespace(pod.getMetadata().getNamespace())
                .status(status)
                .podIP(pod.getStatus() != null ? pod.getStatus().getPodIP() : null)
                .nodeName(pod.getSpec().getNodeName())
                .containers(containers)
                .build();
    }
}
