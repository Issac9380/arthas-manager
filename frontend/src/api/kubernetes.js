import http from './index.js'

export const k8sApi = {
  listNamespaces: () => http.get('/k8s/namespaces'),

  listPods: (namespace) => http.get(`/k8s/namespaces/${namespace}/pods`),

  listJavaProcesses: (namespace, pod, container) =>
    http.get(`/k8s/namespaces/${namespace}/pods/${pod}/containers/${container}/processes`),

  hasJava: (namespace, pod, container) =>
    http.get(`/k8s/namespaces/${namespace}/pods/${pod}/containers/${container}/java`)
}
