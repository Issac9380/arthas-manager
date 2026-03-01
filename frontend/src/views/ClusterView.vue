<template>
  <div class="cluster-view">
    <el-row :gutter="24">
      <!-- Left: Namespace + Pod tree -->
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">集群资源</span>
              <el-button size="small" :icon="Refresh" @click="fetchNamespaces" :loading="store.loading">
                刷新
              </el-button>
            </div>
          </template>

          <el-select
            v-model="selectedNs"
            placeholder="选择 Namespace"
            style="width: 100%; margin-bottom: 16px"
            @change="onNamespaceChange"
            filterable
          >
            <el-option
              v-for="ns in store.namespaces"
              :key="ns.name"
              :label="ns.name"
              :value="ns.name"
            >
              <span>{{ ns.name }}</span>
              <el-tag size="small" :type="ns.status === 'Active' ? 'success' : 'warning'" style="float:right">
                {{ ns.status }}
              </el-tag>
            </el-option>
          </el-select>

          <el-table
            :data="store.pods"
            v-loading="store.loading"
            @row-click="onPodClick"
            highlight-current-row
            size="small"
            style="cursor: pointer"
          >
            <el-table-column prop="name" label="Pod 名称" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'Running' ? 'success' : 'warning'">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="nodeName" label="节点" show-overflow-tooltip width="120" />
          </el-table>
        </el-card>
      </el-col>

      <!-- Right: Container + Process detail -->
      <el-col :span="14">
        <el-card shadow="never" v-if="store.selectedPod">
          <template #header>
            <div class="card-header">
              <span class="card-title">
                <el-icon><Box /></el-icon>
                {{ store.selectedPod.name }}
              </span>
              <el-tag :type="store.selectedPod.status === 'Running' ? 'success' : 'warning'">
                {{ store.selectedPod.status }}
              </el-tag>
            </div>
          </template>

          <el-tabs v-model="activeContainer">
            <el-tab-pane
              v-for="container in store.selectedPod.containers"
              :key="container.name"
              :label="container.name"
              :name="container.name"
            >
              <div class="container-detail">
                <el-descriptions :column="2" size="small" border>
                  <el-descriptions-item label="镜像">
                    <el-text type="info" size="small">{{ container.image }}</el-text>
                  </el-descriptions-item>
                  <el-descriptions-item label="Arthas">
                    <el-tag :type="container.arthasDeployed ? 'success' : 'info'" size="small">
                      {{ container.arthasDeployed ? '已部署' : '未部署' }}
                    </el-tag>
                  </el-descriptions-item>
                </el-descriptions>

                <div class="action-bar">
                  <el-button type="primary" size="small" @click="loadProcesses(container.name)" :icon="Search">
                    查看 Java 进程
                  </el-button>
                </div>

                <!-- Java process table -->
                <el-table
                  v-if="activeContainer === container.name && store.javaProcesses.length > 0"
                  :data="store.javaProcesses"
                  size="small"
                  style="margin-top: 12px"
                >
                  <el-table-column prop="pid" label="PID" width="80" />
                  <el-table-column prop="mainClass" label="主类" show-overflow-tooltip />
                  <el-table-column label="操作" width="140">
                    <template #default="{ row }">
                      <el-button
                        type="success"
                        size="small"
                        @click="goToDiagnosis(row, container.name)"
                      >
                        诊断
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>

        <el-empty v-else description="← 请从左侧选择一个 Pod" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, Search } from '@element-plus/icons-vue'
import { useKubernetesStore } from '@/stores/kubernetes.js'

const store = useKubernetesStore()
const router = useRouter()

const selectedNs = ref('')
const activeContainer = ref('')

onMounted(() => store.fetchNamespaces())

async function fetchNamespaces() {
  await store.fetchNamespaces()
}

async function onNamespaceChange(ns) {
  await store.fetchPods(ns)
}

function onPodClick(pod) {
  store.selectPod(pod)
  activeContainer.value = pod.containers[0]?.name ?? ''
}

async function loadProcesses(containerName) {
  if (!store.selectedPod) return
  await store.fetchJavaProcesses(
    store.selectedNamespace,
    store.selectedPod.name,
    containerName
  )
}

function goToDiagnosis(process, containerName) {
  router.push({
    path: '/diagnosis',
    query: {
      namespace: store.selectedNamespace,
      pod: store.selectedPod.name,
      container: containerName,
      pid: process.pid,
      mainClass: process.mainClass
    }
  })
}
</script>

<style scoped>
.cluster-view {}
.card-header { display: flex; align-items: center; justify-content: space-between; }
.card-title { font-weight: 600; font-size: 15px; display: flex; align-items: center; gap: 6px; }
.container-detail { display: flex; flex-direction: column; gap: 12px; }
.action-bar { display: flex; gap: 8px; margin-top: 4px; }
</style>
