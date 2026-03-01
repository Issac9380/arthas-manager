import http from './index.js'

export const arthasApi = {
  /** Returns command metadata list for dynamic form rendering */
  listCommands: () => http.get('/arthas/commands'),

  /** Deploy Arthas (and optionally JDK) to a container */
  deploy: (data) => http.post('/arthas/deploy', data),

  /** Attach Arthas to a Java process — returns sessionId */
  attach: (data) => http.post('/arthas/attach', data),

  /** Execute a wrapped command via an active session */
  execute: (data) => http.post('/arthas/execute', data),

  /** Close a session */
  closeSession: (sessionId) => http.delete(`/arthas/sessions/${sessionId}`)
}
