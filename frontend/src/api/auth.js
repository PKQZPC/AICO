// src/api/auth.js
import axios from '@/utils/axios'

export const loginParent = (username) => {
  return axios.post('/parents/login', { username })
}