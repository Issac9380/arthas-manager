package com.arthasmanager.controller;

import com.arthasmanager.model.dto.JavaProcessInfo;
import com.arthasmanager.model.dto.NamespaceInfo;
import com.arthasmanager.model.dto.PodInfo;
import com.arthasmanager.model.vo.Result;
import com.arthasmanager.service.KubernetesService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/k8s")
@RequiredArgsConstructor
public class KubernetesController {

    private final KubernetesService kubernetesService;

    @GetMapping("/namespaces")
    public Result<List<NamespaceInfo>> listNamespaces(
            @RequestParam(required = false) String clusterId) {
        return Result.success(kubernetesService.listNamespaces(clusterId));
    }

    @GetMapping("/namespaces/{namespace}/pods")
    public Result<List<PodInfo>> listPods(
            @PathVariable String namespace,
            @RequestParam(required = false) String clusterId) {
        return Result.success(kubernetesService.listPods(clusterId, namespace));
    }

    @GetMapping("/namespaces/{namespace}/pods/{pod}/containers/{container}/processes")
    public Result<List<JavaProcessInfo>> listJavaProcesses(
            @PathVariable String namespace,
            @PathVariable String pod,
            @PathVariable String container,
            @RequestParam(required = false) String clusterId) {
        return Result.success(kubernetesService.listJavaProcesses(clusterId, namespace, pod, container));
    }

    @GetMapping("/namespaces/{namespace}/pods/{pod}/containers/{container}/java")
    public Result<Boolean> hasJava(
            @PathVariable String namespace,
            @PathVariable String pod,
            @PathVariable String container,
            @RequestParam(required = false) String clusterId) {
        return Result.success(kubernetesService.hasJava(clusterId, namespace, pod, container));
    }
}
