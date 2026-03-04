import { defineStore } from 'pinia'
import { ref } from 'vue'
import { arthasApi } from '@/api/arthas.js'
import { ElMessage } from 'element-plus'

export const useArthasStore = defineStore('arthas', () => {
  const sessionId = ref(null)
  const commandMeta = ref([])
  const selectedCommand = ref(null)
  const results = ref([])
  const loading = ref(false)
  const deploying = ref(false)
  const attaching = ref(false)
  // { "8": { recommended: "3.7.2", supported: [...] }, "11": ..., "17": ..., "21": ... }
  const versionMatrix = ref({})

  async function loadCommandMeta() {
    const res = await arthasApi.listCommands()
    commandMeta.value = res.data
  }

  async function loadVersionMatrix() {
    try {
      const res = await arthasApi.getVersionMatrix()
      versionMatrix.value = res.data
    } catch (e) {
      console.warn('Failed to load version matrix', e)
    }
  }

  /** Returns the recommended Arthas version for a given JDK major version string. */
  function getRecommendedArthasVersion(jdkVersion) {
    return versionMatrix.value[jdkVersion]?.recommended ?? '3.7.2'
  }

  /** Returns supported Arthas versions for a given JDK major version string. */
  function getSupportedArthasVersions(jdkVersion) {
    return versionMatrix.value[jdkVersion]?.supported ?? ['3.7.2']
  }

  async function deploy(request) {
    deploying.value = true
    try {
      await arthasApi.deploy(request)
      ElMessage.success('Arthas 部署成功')
    } finally {
      deploying.value = false
    }
  }

  async function attach(request) {
    attaching.value = true
    try {
      const res = await arthasApi.attach(request)
      sessionId.value = res.data
      ElMessage.success('已连接 Arthas，sessionId: ' + res.data)
    } finally {
      attaching.value = false
    }
  }

  async function execute(commandType, params) {
    if (!sessionId.value) {
      ElMessage.warning('请先连接到一个 Java 进程')
      return
    }
    loading.value = true
    try {
      const res = await arthasApi.execute({ sessionId: sessionId.value, commandType, params })
      results.value.unshift({
        id: Date.now(),
        commandType,
        params,
        result: res.data,
        timestamp: new Date().toLocaleTimeString()
      })
    } finally {
      loading.value = false
    }
  }

  async function closeSession() {
    if (!sessionId.value) return
    await arthasApi.closeSession(sessionId.value)
    sessionId.value = null
    results.value = []
    ElMessage.info('会话已关闭')
  }

  function selectCommand(type) {
    selectedCommand.value = commandMeta.value.find(c => c.type === type) ?? null
  }

  function clearResults() {
    results.value = []
  }

  return {
    sessionId, commandMeta, selectedCommand, results, loading, deploying, attaching,
    versionMatrix,
    loadCommandMeta, loadVersionMatrix,
    getRecommendedArthasVersion, getSupportedArthasVersions,
    deploy, attach, execute, closeSession, selectCommand, clearResults
  }
})
