// src/utils/axios.js
import axios from 'axios'

const service = axios.create({
  baseURL: '/api',  // 使用代理地址
  timeout: 5000
})

service.interceptors.response.use(
  response => {
    if (response.data.code === 200) {
      return response.data
    } else {
      return Promise.reject(response.data.msg)
    }
  },
  error => {
    return Promise.reject(error)
  }
)

export default service