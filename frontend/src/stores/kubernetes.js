import { defineStore } from 'pinia'
import { ref } from 'vue'
import { k8sApi } from '@/api/kubernetes.js'
import { useClusterStore } from '@/stores/cluster.js'

export const useKubernetesStore = defineStore('kubernetes', () => {
  const namespaces = ref([])
  const pods = ref([])
  const javaProcesses = ref([])
  const selectedNamespace = ref('')
  const selectedPod = ref(null)
  const selectedContainer = ref('')
  const loading = ref(false)

  function clusterId() {
    return useClusterStore().activeClusterId
  }

  async function fetchNamespaces() {
    loading.value = true
    try {
      const res = await k8sApi.listNamespaces(clusterId())
      namespaces.value = res.data
    } finally {
      loading.value = false
    }
  }

  async function fetchPods(namespace) {
    loading.value = true
    selectedNamespace.value = namespace
    pods.value = []
    javaProcesses.value = []
    try {
      const res = await k8sApi.listPods(clusterId(), namespace)
      pods.value = res.data
    } finally {
      loading.value = false
    }
  }

  async function fetchJavaProcesses(namespace, pod, container) {
    loading.value = true
    selectedContainer.value = container
    try {
      const res = await k8sApi.listJavaProcesses(clusterId(), namespace, pod, container)
      javaProcesses.value = res.data
    } finally {
      loading.value = false
    }
  }

  function selectPod(pod) {
    selectedPod.value = pod
    javaProcesses.value = []
    selectedContainer.value = ''
  }

  return {
    namespaces, pods, javaProcesses,
    selectedNamespace, selectedPod, selectedContainer,
    loading,
    fetchNamespaces, fetchPods, fetchJavaProcesses, selectPod
  }
})
