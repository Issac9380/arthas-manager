package com.arthasmanager.service;

import com.arthasmanager.model.dto.JavaProcessInfo;
import com.arthasmanager.model.dto.NamespaceInfo;
import com.arthasmanager.model.dto.PodInfo;

import java.util.List;

/**
 * Facade over the Fabric8 Kubernetes client.
 * Provides coarse-grained operations needed by the management UI.
 */
public interface KubernetesService {

    List<NamespaceInfo> listNamespaces();

    List<PodInfo> listPods(String namespace);

    /**
     * Executes {@code jps -l} inside the container and returns the running Java processes.
     */
    List<JavaProcessInfo> listJavaProcesses(String namespace, String podName, String containerName);

    /**
     * Executes an arbitrary shell command inside a container and returns stdout.
     */
    String execCommand(String namespace, String podName, String containerName, String... command);

    /**
     * Checks whether {@code java} is available inside the container.
     */
    boolean hasJava(String namespace, String podName, String containerName);
}
