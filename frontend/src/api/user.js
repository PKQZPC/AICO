import axios from '@/utils/axios'

// 获取所有家长用户列表
export const getParentUsers = () => {
  return axios.get('/parents/list')
}

// 搜索家长用户
export const searchParentUsers = (keyword) => {
  return axios.get(`/parents/search?keyword=${encodeURIComponent(keyword)}`)
} 