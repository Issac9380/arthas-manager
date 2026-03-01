package com.arthasmanager.service;

import com.arthasmanager.model.dto.JavaProcessInfo;
import com.arthasmanager.model.dto.NamespaceInfo;
import com.arthasmanager.model.dto.PodInfo;

import java.util.List;

/**
 * Facade over the Fabric8 Kubernetes client.
 * All methods accept a {@code clusterId} to support multi-cluster operations.
 * Pass {@code null} or empty string to use the default cluster.
 */
public interface KubernetesService {

    List<NamespaceInfo> listNamespaces(String clusterId);

    List<PodInfo> listPods(String clusterId, String namespace);

    List<JavaProcessInfo> listJavaProcesses(String clusterId, String namespace, String podName, String containerName);

    String execCommand(String clusterId, String namespace, String podName, String containerName, String... command);

    boolean hasJava(String clusterId, String namespace, String podName, String containerName);
}
