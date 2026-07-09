// src/api/chat.js
import axios from '@/utils/axios'

// 创建聊天
export const createChat = (data) => {
  return axios.post('/chats/create_chat', data)
}

// 获取家长聊天列表
export const getParentChats = (parentId) => {
    return axios.get(`/parents/${parentId}/get_chats`)
  }
  
  // 获取专家聊天列表
  export const getExpertChats = (expertId) => {
    return axios.get(`/experts/${expertId}/get_chats`)
  }
  
  // 获取所有专家列表（供家长选择）
  export const getAllExperts = () => {
    return axios.get('/experts')
  }
  
  // 获取所有家长列表（供专家选择）
  export const getAllParents = () => {
    return axios.get('/parents')
  }
  
  // 获取家长画像和回复策略
  export const getParentModel = (chatId) => {
    return axios.get(`/chats/${chatId}/parent-model`)
  }
  
  // 家长发送消息
  export const sendParentMessage = (data) => {
    return axios.post('/parents/send_message', data)
  }
  
  // 专家发送消息
  export const sendExpertMessage = (data) => {
    return axios.post('/experts/send_message', data)
  }
  
  // 获取消息列表
  export const getChatMessages = (chatId) => {
    return axios.get(`/chats/${chatId}`)
  }
  
  // 设置聊天状态
  export const updateChatStatus = (chatId, data) => {
    return axios.put(`/chats/${chatId}/status`, data)
  }
  
  // 设置已读时间
  export const updateReadTime = (chatId, data) => {
    return axios.put(`/chats/${chatId}/read_time`, data)
  }

  // 设置聊天状态为AI托管
  export const setAIModeStatus = (chatId) => {
    return axios.put(`/chats/${chatId}/ai_mode`)
  }

  // 删除聊天
  export const deleteChat = (chatId) => {
    return axios.delete(`/chats/${chatId}`)
  }

  // 获取专家行为统计报表（综合）
  export const getExpertExportSimple = (params) => {
    return axios.get('/exports/simple', { params });
  }

  // 获取专家行为统计报表（明细）
  export const getExpertExportDetail = (params) => {
    return axios.get('/exports/detail', { params });
  }

  // 获取专家与AI的聊天消息
  export const getExpertAIMessages = (chatId) => {
    return axios.get(`/chats/experts/${chatId}`)
  }

  // 专家发送AI指示消息
  export const sendExpertAIInstruction = (chatId, data) => {
    return axios.post(`/expert_ai/chats/${chatId}/instructions`, data)
  }

  // 获取AI推荐回复
  export const getAIRecommendation = (chatId) => {
    return axios.get(`/expert_ai/chats/${chatId}/recommend`)
  }

  // 采纳AI推荐回复
  export const adoptAIRecommendation = (chatId, data) => {
    return axios.post(`/expert_ai/chats/${chatId}/adopt`, data)
  }