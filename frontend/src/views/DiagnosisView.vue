<template>
  <div class="diagnosis-view">
    <!-- Session Bar -->
    <el-card shadow="never" class="session-bar">
      <el-row :gutter="16" align="middle">
        <el-col :span="18">
          <el-descriptions :column="5" size="small">
            <el-descriptions-item label="Namespace">{{ target.namespace || '-' }}</el-descriptions-item>
            <el-descriptions-item label="Pod">{{ target.pod || '-' }}</el-descriptions-item>
            <el-descriptions-item label="容器">{{ target.container || '-' }}</el-descriptions-item>
            <el-descriptions-item label="PID">{{ target.pid || '-' }}</el-descriptions-item>
            <el-descriptions-item label="主类">
              <el-text type="info" size="small" truncated>{{ target.mainClass || '-' }}</el-text>
            </el-descriptions-item>
          </el-descriptions>
        </el-col>
        <el-col :span="6" style="text-align: right">
          <el-space>
            <el-tag v-if="store.sessionId" type="success">已连接</el-tag>
            <el-tag v-else type="info">未连接</el-tag>

            <el-button
              v-if="!store.sessionId"
              type="primary"
              size="small"
              @click="showDeployDialog = true"
              :disabled="!target.pod"
            >
              部署 Arthas
            </el-button>
            <el-button
              v-if="!store.sessionId"
              type="success"
              size="small"
              @click="doAttach"
              :loading="store.attaching"
              :disabled="!target.pid"
            >
              连接进程
            </el-button>
            <el-button
              v-if="store.sessionId"
              type="danger"
              size="small"
              @click="store.closeSession"
            >
              断开连接
            </el-button>
          </el-space>
        </el-col>
      </el-row>
    </el-card>

    <el-row :gutter="20" style="margin-top: 16px">
      <!-- Left panel: command menu -->
      <el-col :span="6">
        <el-card shadow="never" class="command-menu">
          <template #header><span class="card-title">诊断命令</span></template>
          <el-menu
            :default-active="selectedCommandType"
            @select="onCommandSelect"
          >
            <el-menu-item
              v-for="cmd in store.commandMeta"
              :key="cmd.type"
              :index="cmd.type"
            >
              <span>{{ cmd.displayName }}</span>
            </el-menu-item>
          </el-menu>
        </el-card>
      </el-col>

      <!-- Center: dynamic command form -->
      <el-col :span="10">
        <el-card shadow="never" v-if="store.selectedCommand">
          <template #header>
            <div class="card-header">
              <div>
                <span class="card-title">{{ store.selectedCommand.displayName }}</span>
                <el-text type="info" size="small" style="margin-left: 8px">
                  {{ store.selectedCommand.description }}
                </el-text>
              </div>
            </div>
          </template>

          <el-form :model="commandParams" label-position="top" size="default">
            <el-form-item
              v-for="param in store.selectedCommand.params"
              :key="param.name"
              :label="param.label"
              :required="param.required"
            >
              <el-tooltip :content="param.description" placement="top" :disabled="!param.description">
                <template v-if="param.type === 'BOOLEAN'">
                  <el-switch v-model="commandParams[param.name]" />
                </template>
                <template v-else-if="param.type === 'INTEGER'">
                  <el-input-number
                    v-model="commandParams[param.name]"
                    :min="1"
                    style="width: 100%"
                    controls-position="right"
                  />
                </template>
                <template v-else>
                  <el-input
                    v-model="commandParams[param.name]"
                    :placeholder="param.defaultValue || param.description"
                    clearable
                  />
                </template>
              </el-tooltip>
            </el-form-item>
          </el-form>

          <el-divider />

          <div class="command-preview">
            <el-text type="info" size="small">预览命令：</el-text>
            <el-tag type="info" effect="plain" class="preview-tag">{{ previewCommand }}</el-tag>
          </div>

          <el-button
            type="primary"
            style="margin-top: 16px; width: 100%"
            @click="executeCommand"
            :loading="store.loading"
            :disabled="!store.sessionId"
          >
            <el-icon><VideoPlay /></el-icon>
            执行命令
          </el-button>
        </el-card>

        <el-empty v-else description="← 请从左侧选择一个诊断命令" />
      </el-col>

      <!-- Right: results -->
      <el-col :span="8">
        <el-card shadow="never" class="result-panel">
          <template #header>
            <div class="card-header">
              <span class="card-title">执行结果</span>
              <el-button text size="small" @click="store.clearResults">清空</el-button>
            </div>
          </template>

          <div class="result-list" v-if="store.results.length > 0">
            <el-collapse accordion>
              <el-collapse-item
                v-for="item in store.results"
                :key="item.id"
                :name="item.id"
              >
                <template #title>
                  <el-tag size="small" type="primary" style="margin-right: 8px">{{ item.commandType }}</el-tag>
                  <el-text size="small" type="info">{{ item.timestamp }}</el-text>
                </template>
                <pre class="result-json">{{ JSON.stringify(item.result, null, 2) }}</pre>
              </el-collapse-item>
            </el-collapse>
          </div>

          <el-empty v-else description="暂无执行结果" />
        </el-card>
      </el-col>
    </el-row>

    <!-- Deploy Dialog -->
    <el-dialog v-model="showDeployDialog" title="部署 Arthas 到容器" width="480px">
      <el-form :model="deployForm" label-width="120px">
        <el-form-item label="上传 JDK">
          <el-switch v-model="deployForm.uploadJdk" />
        </el-form-item>
        <el-form-item v-if="deployForm.uploadJdk" label="JDK 版本">
          <el-select v-model="deployForm.jdkVersion">
            <el-option label="Java 17" value="17" />
            <el-option label="Java 11" value="11" />
            <el-option label="Java 8"  value="8"  />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDeployDialog = false">取消</el-button>
        <el-button type="primary" @click="doDeploy" :loading="store.deploying">开始部署</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useArthasStore } from '@/stores/arthas.js'

const route  = useRoute()
const store  = useArthasStore()

const target = reactive({
  namespace: route.query.namespace || '',
  pod: route.query.pod || '',
  container: route.query.container || '',
  pid: route.query.pid ? Number(route.query.pid) : null,
  mainClass: route.query.mainClass || ''
})

const selectedCommandType = ref('')
const commandParams = ref({})
const showDeployDialog = ref(false)
const deployForm = reactive({ uploadJdk: false, jdkVersion: '17' })

onMounted(async () => {
  await store.loadCommandMeta()
  if (store.commandMeta.length > 0) {
    store.selectCommand(store.commandMeta[0].type)
    selectedCommandType.value = store.commandMeta[0].type
    initParams()
  }
})

// Re-initialise form params when command changes
watch(() => store.selectedCommand, () => initParams())

function initParams() {
  if (!store.selectedCommand) return
  const defaults = {}
  store.selectedCommand.params.forEach(p => {
    if (p.type === 'BOOLEAN') {
      defaults[p.name] = p.defaultValue === 'true'
    } else if (p.type === 'INTEGER') {
      defaults[p.name] = p.defaultValue ? Number(p.defaultValue) : null
    } else {
      defaults[p.name] = p.defaultValue || ''
    }
  })
  commandParams.value = defaults
}

function onCommandSelect(type) {
  selectedCommandType.value = type
  store.selectCommand(type)
}

const previewCommand = computed(() => {
  if (!store.selectedCommand) return ''
  // Best-effort local preview using the display name
  return `${store.selectedCommand.type} (参数已填写)`
})

async function doDeploy() {
  await store.deploy({
    namespace: target.namespace,
    podName: target.pod,
    containerName: target.container,
    uploadJdk: deployForm.uploadJdk,
    jdkVersion: deployForm.jdkVersion
  })
  showDeployDialog.value = false
}

async function doAttach() {
  await store.attach({
    namespace: target.namespace,
    podName: target.pod,
    containerName: target.container,
    pid: target.pid
  })
}

async function executeCommand() {
  await store.execute(selectedCommandType.value, { ...commandParams.value })
}
</script>

<style scoped>
.diagnosis-view { display: flex; flex-direction: column; }
.session-bar {}
.card-header { display: flex; align-items: center; justify-content: space-between; width: 100%; }
.card-title { font-weight: 600; font-size: 15px; }
.command-menu .el-menu { border-right: none; }
.result-panel { height: calc(100vh - 280px); overflow-y: auto; }
.result-list { max-height: calc(100vh - 340px); overflow-y: auto; }
.result-json {
  font-size: 12px;
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 12px;
  border-radius: 4px;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
.command-preview { background: #f5f7fa; padding: 10px 12px; border-radius: 4px; }
.preview-tag { font-family: monospace; max-width: 100%; }
</style>
