// src/api/ops.js
import axios from '@/utils/axios'

export const getOpsDashboard = (params = {}) => {
  return axios.get('/ops/metrics/dashboard', { params })
}

export const getOpsSummary = () => axios.get('/ops/metrics/summary')

export const getOpsLogs = (params = {}) => axios.get('/ops/metrics/logs', { params })

export const getOpsHealth = () => axios.get('/ops/health')

export const getTraceLogs = (traceId) => axios.get(`/ops/logs/trace/${encodeURIComponent(traceId)}`)

export const searchDebugLogs = (params = {}) => axios.get('/ops/logs/search', { params })

export const getRecentTraces = (limit = 50) => axios.get('/ops/logs/recent-traces', { params: { limit } })
