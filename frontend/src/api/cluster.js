import http from './index.js'

export const clusterApi = {
  listClusters: () => http.get('/clusters'),

  addCluster: (config) => http.post('/clusters', config),

  testConnection: (config) => http.post('/clusters/test', config),

  deleteCluster: (id) => http.delete(`/clusters/${id}`)
}
