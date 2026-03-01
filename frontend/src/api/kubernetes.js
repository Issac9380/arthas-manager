import http from './index.js'

export const k8sApi = {
  listNamespaces: (clusterId) =>
    http.get('/k8s/namespaces', { params: clusterId ? { clusterId } : {} }),

  listPods: (clusterId, namespace) =>
    http.get(`/k8s/namespaces/${namespace}/pods`, { params: clusterId ? { clusterId } : {} }),

  listJavaProcesses: (clusterId, namespace, pod, container) =>
    http.get(`/k8s/namespaces/${namespace}/pods/${pod}/containers/${container}/processes`,
      { params: clusterId ? { clusterId } : {} }),

  hasJava: (clusterId, namespace, pod, container) =>
    http.get(`/k8s/namespaces/${namespace}/pods/${pod}/containers/${container}/java`,
      { params: clusterId ? { clusterId } : {} })
}
