// src/utils/axios.js
import axios from 'axios'

function createTraceId() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID().replace(/-/g, '').slice(0, 16)
  }
  return `${Date.now().toString(16)}${Math.random().toString(16).slice(2, 10)}`.slice(0, 16)
}

const service = axios.create({
  baseURL: '/api',
  timeout: 8000
})

service.interceptors.request.use((config) => {
  const traceId = createTraceId()
  config.headers = config.headers || {}
  config.headers['X-Trace-Id'] = traceId
  config.__traceId = traceId
  try {
    sessionStorage.setItem('aico_last_trace_id', traceId)
  } catch (_) {
    /* ignore */
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    const serverTrace = response.headers?.['x-trace-id'] || response.config?.__traceId
    if (serverTrace) {
      try {
        sessionStorage.setItem('aico_last_trace_id', serverTrace)
      } catch (_) {
        /* ignore */
      }
    }
    if (response.data.code === 200) {
      return response.data
    }
    return Promise.reject(response.data.msg)
  },
  (error) => {
    const serverTrace = error.response?.headers?.['x-trace-id'] || error.config?.__traceId
    if (serverTrace) {
      try {
        sessionStorage.setItem('aico_last_trace_id', serverTrace)
      } catch (_) {
        /* ignore */
      }
    }
    return Promise.reject(error)
  }
)

export default service
