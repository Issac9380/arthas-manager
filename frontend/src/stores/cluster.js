import { defineStore } from 'pinia'
import { ref } from 'vue'
import { clusterApi } from '@/api/cluster.js'

export const useClusterStore = defineStore('cluster', () => {
  const clusters = ref([])
  const activeClusterId = ref('')
  const loading = ref(false)

  async function fetchClusters() {
    loading.value = true
    try {
      const res = await clusterApi.listClusters()
      clusters.value = res.data
      // Auto-select default cluster if no active selection
      if (!activeClusterId.value && clusters.value.length > 0) {
        const def = clusters.value.find(c => c.defaultCluster) || clusters.value[0]
        activeClusterId.value = def.id
      }
    } finally {
      loading.value = false
    }
  }

  async function addCluster(config) {
    const res = await clusterApi.addCluster(config)
    await fetchClusters()
    return res.data
  }

  async function testConnection(config) {
    const res = await clusterApi.testConnection(config)
    return res.data
  }

  async function deleteCluster(id) {
    await clusterApi.deleteCluster(id)
    if (activeClusterId.value === id) activeClusterId.value = ''
    await fetchClusters()
  }

  function setActive(id) {
    activeClusterId.value = id
  }

  return {
    clusters, activeClusterId, loading,
    fetchClusters, addCluster, testConnection, deleteCluster, setActive
  }
})
