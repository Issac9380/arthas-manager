import http from './index.js'

export const toolsApi = {
  listTools: () => http.get('/tools'),
  downloadTool: (url, filename) => http.post('/tools/download', { url, filename }),
  uploadTool: (formData) => http.post('/tools/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  deleteTool: (path) => http.delete('/tools', { params: { path } })
}
