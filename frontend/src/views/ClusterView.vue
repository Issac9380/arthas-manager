<template>
  <div class="cluster-view">

    <!-- ── 集群选择栏 ─────────────────────────────────────────────── -->
    <el-card shadow="never" class="cluster-bar" style="margin-bottom:16px">
      <div class="bar-inner">
        <span class="bar-label">当前集群：</span>
        <el-select
          v-model="clusterStore.activeClusterId"
          placeholder="请添加集群"
          style="width:260px"
          @change="onClusterChange"
        >
          <el-option
            v-for="c in clusterStore.clusters"
            :key="c.id"
            :label="c.name"
            :value="c.id"
          >
            <div style="display:flex;align-items:center;gap:8px">
              <el-badge is-dot :type="statusType(c.status)" />
              <span>{{ c.name }}</span>
              <el-tag size="small" style="margin-left:auto">{{ authLabel(c.authType) }}</el-tag>
            </div>
          </el-option>
        </el-select>

        <template v-if="activeCluster">
          <el-tag :type="statusType(activeCluster.status)" style="margin-left:8px">
            {{ activeCluster.status }}
          </el-tag>
          <el-text type="info" size="small" style="margin-left:8px">
            {{ activeCluster.apiServerUrl }}
          </el-text>
        </template>

        <div style="margin-left:auto;display:flex;gap:8px">
          <el-button type="primary" :icon="Plus" @click="openAddDialog">添加集群</el-button>
          <el-button :icon="Delete" @click="confirmDelete" :disabled="!activeCluster || activeCluster.defaultCluster">
            删除集群
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- ── K8s 资源浏览 ───────────────────────────────────────────── -->
    <el-row :gutter="24">
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">集群资源</span>
              <el-button size="small" :icon="Refresh" @click="fetchNamespaces" :loading="k8sStore.loading">
                刷新
              </el-button>
            </div>
          </template>

          <el-select
            v-model="selectedNs"
            placeholder="选择 Namespace"
            style="width:100%;margin-bottom:16px"
            @change="onNamespaceChange"
            filterable
          >
            <el-option v-for="ns in k8sStore.namespaces" :key="ns.name" :label="ns.name" :value="ns.name">
              <span>{{ ns.name }}</span>
              <el-tag size="small" :type="ns.status === 'Active' ? 'success' : 'warning'" style="float:right">
                {{ ns.status }}
              </el-tag>
            </el-option>
          </el-select>

          <el-table :data="k8sStore.pods" v-loading="k8sStore.loading" @row-click="onPodClick"
            highlight-current-row size="small" style="cursor:pointer">
            <el-table-column prop="name" label="Pod 名称" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'Running' ? 'success' : 'warning'">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="nodeName" label="节点" show-overflow-tooltip width="120" />
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="14">
        <el-card shadow="never" v-if="k8sStore.selectedPod">
          <template #header>
            <div class="card-header">
              <span class="card-title">{{ k8sStore.selectedPod.name }}</span>
              <el-tag :type="k8sStore.selectedPod.status === 'Running' ? 'success' : 'warning'">
                {{ k8sStore.selectedPod.status }}
              </el-tag>
            </div>
          </template>
          <el-tabs v-model="activeContainer">
            <el-tab-pane v-for="container in k8sStore.selectedPod.containers"
              :key="container.name" :label="container.name" :name="container.name">
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
                <el-table v-if="activeContainer === container.name && k8sStore.javaProcesses.length > 0"
                  :data="k8sStore.javaProcesses" size="small" style="margin-top:12px">
                  <el-table-column prop="pid" label="PID" width="80" />
                  <el-table-column prop="mainClass" label="主类" show-overflow-tooltip />
                  <el-table-column label="操作" width="140">
                    <template #default="{ row }">
                      <el-button type="success" size="small" @click="goToDiagnosis(row, container.name)">
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

    <!-- ── 添加集群对话框 ──────────────────────────────────────────── -->
    <el-dialog v-model="addDialogVisible" title="添加集群" width="600px" :close-on-click-modal="false">
      <el-form :model="form" label-width="110px" label-position="right">
        <el-form-item label="集群名称" required>
          <el-input v-model="form.name" placeholder="例如：生产环境 / dev-cluster" />
        </el-form-item>

        <el-form-item label="认证方式" required>
          <el-radio-group v-model="form.authType">
            <el-radio-button value="KUBECONFIG">Kubeconfig</el-radio-button>
            <el-radio-button value="TOKEN">Token</el-radio-button>
            <el-radio-button value="CERT">证书</el-radio-button>
            <el-radio-button value="IN_CLUSTER">In-Cluster</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <!-- KUBECONFIG -->
        <template v-if="form.authType === 'KUBECONFIG'">
          <el-form-item label="Kubeconfig" required>
            <el-input v-model="form.kubeconfigContent" type="textarea" :rows="10"
              placeholder="粘贴 kubeconfig YAML 内容（~/.kube/config 文件内容）" />
          </el-form-item>
        </template>

        <!-- TOKEN -->
        <template v-if="form.authType === 'TOKEN'">
          <el-form-item label="API Server" required>
            <el-input v-model="form.apiServerUrl" placeholder="https://192.168.1.100:6443" />
          </el-form-item>
          <el-form-item label="Bearer Token" required>
            <el-input v-model="form.token" type="password" show-password
              placeholder="eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..." />
          </el-form-item>
          <el-form-item label="CA 证书 (PEM)">
            <el-input v-model="form.caCertData" type="textarea" :rows="4"
              placeholder="-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----\n（可选）" />
          </el-form-item>
          <el-form-item label="跳过 TLS 验证">
            <el-switch v-model="form.skipTlsVerify" />
            <el-text type="warning" size="small" style="margin-left:8px">仅用于测试环境</el-text>
          </el-form-item>
        </template>

        <!-- CERT -->
        <template v-if="form.authType === 'CERT'">
          <el-form-item label="API Server" required>
            <el-input v-model="form.apiServerUrl" placeholder="https://192.168.1.100:6443" />
          </el-form-item>
          <el-form-item label="CA 证书 (PEM)" required>
            <el-input v-model="form.caCertData" type="textarea" :rows="4"
              placeholder="-----BEGIN CERTIFICATE-----\n..." />
          </el-form-item>
          <el-form-item label="客户端证书 (PEM)" required>
            <el-input v-model="form.clientCertData" type="textarea" :rows="4"
              placeholder="-----BEGIN CERTIFICATE-----\n..." />
          </el-form-item>
          <el-form-item label="客户端私钥 (PEM)" required>
            <el-input v-model="form.clientKeyData" type="textarea" :rows="4"
              placeholder="-----BEGIN RSA PRIVATE KEY-----\n..." />
          </el-form-item>
          <el-form-item label="跳过 TLS 验证">
            <el-switch v-model="form.skipTlsVerify" />
          </el-form-item>
        </template>

        <!-- IN_CLUSTER -->
        <template v-if="form.authType === 'IN_CLUSTER'">
          <el-alert title="将使用 Pod 内置 ServiceAccount Token 自动连接当前 Kubernetes 集群（仅在 Pod 内部运行时有效）。"
            type="info" :closable="false" style="margin-bottom:12px" />
        </template>

        <!-- 连接测试结果 -->
        <el-form-item v-if="testResult" label="测试结果">
          <el-alert
            :title="testResult.statusMessage"
            :type="testResult.status === 'CONNECTED' ? 'success' : 'error'"
            :closable="false"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button :loading="testing" @click="doTest">测试连接</el-button>
        <el-button type="primary" :loading="adding" @click="doAdd">确认添加</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search, Plus, Delete } from '@element-plus/icons-vue'
import { useClusterStore } from '@/stores/cluster.js'
import { useKubernetesStore } from '@/stores/kubernetes.js'

const router = useRouter()
const clusterStore = useClusterStore()
const k8sStore = useKubernetesStore()

const selectedNs = ref('')
const activeContainer = ref('')

// ── 集群管理 ────────────────────────────────────────────────────────────────
const addDialogVisible = ref(false)
const testing = ref(false)
const adding = ref(false)
const testResult = ref(null)

const emptyForm = () => ({
  name: '',
  authType: 'KUBECONFIG',
  apiServerUrl: '',
  skipTlsVerify: false,
  token: '',
  caCertData: '',
  clientCertData: '',
  clientKeyData: '',
  kubeconfigContent: ''
})
const form = ref(emptyForm())

const activeCluster = computed(() =>
  clusterStore.clusters.find(c => c.id === clusterStore.activeClusterId) || null
)

function statusType(status) {
  return { CONNECTED: 'success', DISCONNECTED: 'warning', ERROR: 'danger', UNKNOWN: 'info' }[status] || 'info'
}

function authLabel(authType) {
  return { KUBECONFIG: 'Kubeconfig', TOKEN: 'Token', CERT: 'Cert', IN_CLUSTER: 'In-Cluster' }[authType] || authType
}

function openAddDialog() {
  form.value = emptyForm()
  testResult.value = null
  addDialogVisible.value = true
}

async function doTest() {
  testing.value = true
  testResult.value = null
  try {
    testResult.value = await clusterStore.testConnection(form.value)
  } catch (e) {
    testResult.value = { status: 'ERROR', statusMessage: e.message }
  } finally {
    testing.value = false
  }
}

async function doAdd() {
  if (!form.value.name) {
    ElMessage.warning('请填写集群名称')
    return
  }
  adding.value = true
  try {
    await clusterStore.addCluster(form.value)
    addDialogVisible.value = false
    ElMessage.success('集群添加成功')
  } catch (e) {
    ElMessage.error('添加失败：' + e.message)
  } finally {
    adding.value = false
  }
}

async function confirmDelete() {
  if (!activeCluster.value) return
  await ElMessageBox.confirm(
    `确定删除集群 "${activeCluster.value.name}" 吗？`,
    '删除集群', { type: 'warning' }
  )
  await clusterStore.deleteCluster(clusterStore.activeClusterId)
  ElMessage.success('已删除')
}

function onClusterChange() {
  // Reset k8s state when switching cluster
  selectedNs.value = ''
  k8sStore.namespaces = []
  k8sStore.pods = []
  k8sStore.javaProcesses = []
  k8sStore.selectedPod = null
}

// ── K8s 操作 ──────────────────────────────────────────────────────────────
onMounted(async () => {
  await clusterStore.fetchClusters()
})

async function fetchNamespaces() {
  if (!clusterStore.activeClusterId) {
    ElMessage.warning('请先选择或添加集群')
    return
  }
  await k8sStore.fetchNamespaces()
}

async function onNamespaceChange(ns) {
  await k8sStore.fetchPods(ns)
}

function onPodClick(pod) {
  k8sStore.selectPod(pod)
  activeContainer.value = pod.containers[0]?.name ?? ''
}

async function loadProcesses(containerName) {
  if (!k8sStore.selectedPod) return
  await k8sStore.fetchJavaProcesses(
    k8sStore.selectedNamespace,
    k8sStore.selectedPod.name,
    containerName
  )
}

function goToDiagnosis(process, containerName) {
  router.push({
    path: '/diagnosis',
    query: {
      clusterId: clusterStore.activeClusterId,
      namespace: k8sStore.selectedNamespace,
      pod: k8sStore.selectedPod.name,
      container: containerName,
      pid: process.pid,
      mainClass: process.mainClass
    }
  })
}
</script>

<style scoped>
.cluster-bar .bar-inner { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.bar-label { font-weight: 600; white-space: nowrap; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.card-title { font-weight: 600; font-size: 15px; display: flex; align-items: center; gap: 6px; }
.container-detail { display: flex; flex-direction: column; gap: 12px; }
.action-bar { display: flex; gap: 8px; margin-top: 4px; }
</style>
