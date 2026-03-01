<template>
  <div class="tools-view">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">工具缓存管理</span>
          <div class="actions">
            <el-button type="primary" size="small" :icon="Download" @click="showDownloadDialog = true">
              下载工具
            </el-button>
            <el-upload action="" :before-upload="handleUpload" :show-file-list="false">
              <el-button size="small" :icon="Upload">上传文件</el-button>
            </el-upload>
            <el-button size="small" :icon="Refresh" @click="loadTools">刷新</el-button>
          </div>
        </div>
      </template>

      <el-alert type="info" :closable="false" style="margin-bottom: 16px">
        本地工具缓存目录。下载 JDK 和 Arthas 后，可在「诊断中心」中一键部署到容器。
      </el-alert>

      <el-table :data="tools" v-loading="loading" size="small">
        <el-table-column prop="name" label="文件名" />
        <el-table-column prop="path" label="相对路径" />
        <el-table-column prop="size" label="大小">
          <template #default="{ row }">{{ formatSize(row.size) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="danger" size="small" text @click="deleteTool(row.path)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Download dialog -->
    <el-dialog v-model="showDownloadDialog" title="下载工具" width="500px">
      <el-form :model="downloadForm" label-width="80px">
        <el-form-item label="下载 URL">
          <el-input v-model="downloadForm.url" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="保存文件名">
          <el-input v-model="downloadForm.filename" placeholder="e.g. jdk-17.tar.gz" />
        </el-form-item>
        <el-form-item label="">
          <el-space wrap>
            <el-button size="small" @click="fillJdkPreset('17')">OpenJDK 17</el-button>
            <el-button size="small" @click="fillJdkPreset('11')">OpenJDK 11</el-button>
            <el-button size="small" @click="fillArthasPreset">Arthas Boot Jar</el-button>
          </el-space>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDownloadDialog = false">取消</el-button>
        <el-button type="primary" @click="startDownload" :loading="downloading">开始下载</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Refresh, Download, Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { toolsApi } from '@/api/tools.js'

const tools = ref([])
const loading = ref(false)
const downloading = ref(false)
const showDownloadDialog = ref(false)
const downloadForm = ref({ url: '', filename: '' })

onMounted(loadTools)

async function loadTools() {
  loading.value = true
  try {
    const res = await toolsApi.listTools()
    tools.value = res.data
  } finally {
    loading.value = false
  }
}

async function startDownload() {
  if (!downloadForm.value.url || !downloadForm.value.filename) {
    ElMessage.warning('请填写 URL 和文件名')
    return
  }
  downloading.value = true
  try {
    await toolsApi.downloadTool(downloadForm.value.url, downloadForm.value.filename)
    ElMessage.success('下载成功')
    showDownloadDialog.value = false
    await loadTools()
  } finally {
    downloading.value = false
  }
}

async function handleUpload(file) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('subdir', 'uploads')
  try {
    await toolsApi.uploadTool(formData)
    ElMessage.success('上传成功')
    await loadTools()
  } catch {}
  return false // prevent default upload
}

async function deleteTool(path) {
  await ElMessageBox.confirm('确认删除该文件？', '提示', { type: 'warning' })
  await toolsApi.deleteTool(path)
  ElMessage.success('已删除')
  await loadTools()
}

function fillJdkPreset(version) {
  downloadForm.value.url = `https://github.com/adoptium/temurin${version}-binaries/releases/download/jdk-${version}.0.0%2B0/OpenJDK${version}U-jdk_x64_linux_hotspot_${version}.0.0_0.tar.gz`
  downloadForm.value.filename = `jdk-${version}.tar.gz`
}

function fillArthasPreset() {
  downloadForm.value.url = 'https://arthas.aliyun.com/arthas-boot.jar'
  downloadForm.value.filename = 'arthas-boot.jar'
}

function formatSize(bytes) {
  if (bytes < 0) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}
</script>

<style scoped>
.tools-view {}
.card-header { display: flex; align-items: center; justify-content: space-between; }
.card-title { font-weight: 600; font-size: 15px; }
.actions { display: flex; gap: 8px; align-items: center; }
</style>
