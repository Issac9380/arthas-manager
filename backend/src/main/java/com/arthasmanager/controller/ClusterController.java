package com.arthasmanager.controller;

import com.arthasmanager.model.cluster.ClusterConfig;
import com.arthasmanager.model.cluster.ClusterInfo;
import com.arthasmanager.model.vo.Result;
import com.arthasmanager.service.ClusterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 集群管理 REST API.
 *
 * <ul>
 *   <li>GET  /api/clusters              — 列出所有集群</li>
 *   <li>POST /api/clusters              — 添加集群</li>
 *   <li>POST /api/clusters/test         — 测试连接（不保存）</li>
 *   <li>DELETE /api/clusters/{id}       — 删除集群</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/clusters")
@RequiredArgsConstructor
public class ClusterController {

    private final ClusterService clusterService;

    @GetMapping
    public Result<List<ClusterInfo>> listClusters() {
        return Result.success(clusterService.listClusters());
    }

    @PostMapping
    public Result<ClusterInfo> addCluster(@RequestBody ClusterConfig config) {
        return Result.success(clusterService.addCluster(config));
    }

    @PostMapping("/test")
    public Result<ClusterInfo> testConnection(@RequestBody ClusterConfig config) {
        return Result.success(clusterService.testConnection(config));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteCluster(@PathVariable String id) {
        clusterService.deleteCluster(id);
        return Result.success(null);
    }
}
