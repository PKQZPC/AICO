<!-- src/views/Login.vue -->
<template>
  <div class="login-container">
    <div class="login-card">
      <h2>专家协助平台</h2>
      <div class="login-form">
        <!-- 用户登录 -->
        <div class="user-login">
          <el-input
            v-model="username"
            placeholder="请输入用户名"
            class="login-input"
            :disabled="isLoading"
          />
          <el-button 
            type="primary" 
            @click="handleUserLogin"
            class="login-button"
            :loading="isLoading"
          >
            {{ isLoading ? '登录中...' : '用户登录' }}
          </el-button>
        </div>
        
        <!-- 专家登录 -->
        <div class="expert-login">
          <el-button 
            type="success" 
            @click="handleExpertLogin"
            class="login-button"
            :disabled="isLoading"
          >
            专家登录
          </el-button>
        </div>

        <!-- 测试按钮 -->
        <div class="test-button">
          <el-button 
            type="info" 
            @click="showTestDialog = true"
            class="test-button"
            :disabled="isLoading"
          >
            联通测试
          </el-button>
        </div>
      </div>
    </div>

    <!-- 测试功能弹窗 -->
    <el-dialog
      v-model="showTestDialog"
      title="联通测试"
      width="800px"
      class="test-dialog"
    >
      <div class="test-content">
        <!-- WebSocket 测试区域 -->
        <div class="websocket-test-area">
          <h3>WebSocket 测试</h3>
          <div class="ws-controls">
            <el-button 
              @click="connectWebSocket" 
              :disabled="isConnected"
              type="primary"
            >
              连接 WebSocket
            </el-button>
            <el-button 
              @click="disconnectWebSocket" 
              :disabled="!isConnected"
              type="warning"
            >
              断开连接
            </el-button>
            <el-button 
              @click="subscribeChat" 
              :disabled="!isConnected"
              type="success"
            >
              订阅聊天
            </el-button>
          </div>
          <div class="ws-input">
            <el-input v-model="chatId" placeholder="输入聊天ID" />
            <el-input v-model="messageContent" placeholder="输入消息内容" />
            <el-button 
              @click="sendMessage" 
              :disabled="!isConnected"
              type="primary"
            >
              发送消息
            </el-button>
          </div>
          <div class="ws-status">
            <p>连接状态: {{ isConnected ? '已连接' : '未连接' }}</p>
            <p>订阅状态: {{ isSubscribed ? '已订阅' : '未订阅' }}</p>
          </div>
          <div class="ws-messages">
            <h4>消息记录：</h4>
            <div class="message-list">
              <div v-for="(msg, index) in messages" :key="index" class="message-item">
                <pre>{{ JSON.stringify(msg, null, 2) }}</pre>
              </div>
            </div>
          </div>
        </div>

        <!-- API 测试区域 -->
        <div class="api-test-area">
          <h3>API 测试</h3>
          <div class="test-buttons">
            <el-button @click="testCreateChat" type="primary">测试创建聊天</el-button>
            <el-button @click="testGetParentChats" type="success">测试获取家长聊天列表</el-button>
            <el-button @click="testGetExpertChats" type="warning">测试获取专家聊天列表</el-button>
            <el-button @click="testSendMessage" type="info">测试发送消息</el-button>
          </div>
          <div class="test-result">
            <h4>测试结果：</h4>
            <pre>{{ testResult }}</pre>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { loginParent } from '@/api/auth'
import { 
  createChat, 
  getParentChats, 
  getExpertChats, 
  sendParentMessage 
} from '@/api/chat'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const username = ref('')
const testResult = ref('')
const showTestDialog = ref(false)
const isLoading = ref(false)

// WebSocket 相关状态
const ws = ref(null)
const isConnected = ref(false)
const isSubscribed = ref(false)
const messages = ref([])
const chatId = ref('')
const messageContent = ref('')

// 用户登录
const handleUserLogin = async () => {
  if (!username.value) {
    ElMessage.warning('请输入用户名')
    return
  }
  
  try {
    isLoading.value = true
    const res = await loginParent(username.value)
    userStore.setUserInfo({
      id: res.data.userId,
      name: res.data.name,
      type: 'parent'
    })
    ElMessage.success('登录成功')
    router.push('/parent-chat')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    isLoading.value = false
  }
}

// 专家登录
const handleExpertLogin = () => {
  userStore.setUserInfo({
    id: 1,
    name: '专家用户',
    type: 'expert'
  })
  ElMessage.success('专家登录成功')
  router.push('/expert-chat')
}

// WebSocket 连接
const connectWebSocket = () => {
  const wsTarget = import.meta.env.VITE_AICO_WS_TARGET || 'ws://localhost:8081/chat'
  ws.value = new WebSocket(wsTarget)
  
  ws.value.onopen = () => {
    isConnected.value = true
    messages.value.push({ type: 'system', content: 'WebSocket 连接成功' })
  }
  
  ws.value.onclose = () => {
    isConnected.value = false
    isSubscribed.value = false
    messages.value.push({ type: 'system', content: 'WebSocket 连接已关闭' })
  }
  
  ws.value.onerror = (error) => {
    messages.value.push({ type: 'error', content: `WebSocket 错误: ${error.message}` })
  }
  
  ws.value.onmessage = (event) => {
    try {
      const message = JSON.parse(event.data)
      messages.value.push(message)
      
      // 检查是否需要专家介入
      if (message.data?.needExpert) {
        messages.value.push({ 
          type: 'system', 
          content: '系统提示：需要专家介入' 
        })
      }
    } catch (error) {
      messages.value.push({ 
        type: 'error', 
        content: `消息解析错误: ${error.message}` 
      })
    }
  }
}

// 断开 WebSocket 连接
const disconnectWebSocket = () => {
  if (ws.value) {
    ws.value.close()
    ws.value = null
  }
}

// 订阅聊天
const subscribeChat = () => {
  if (!chatId.value) {
    messages.value.push({ 
      type: 'error', 
      content: '请输入聊天ID' 
    })
    return
  }
  
  const subscribeMessage = {
    type: 'subscribe',
    topic: `topic/chat/${chatId.value}`
  }
  
  ws.value.send(JSON.stringify(subscribeMessage))
  isSubscribed.value = true
  messages.value.push({ 
    type: 'system', 
    content: `已订阅聊天: ${chatId.value}` 
  })
}

// 发送消息
const sendMessage = () => {
  if (!messageContent.value) {
    messages.value.push({ 
      type: 'error', 
      content: '请输入消息内容' 
    })
    return
  }
  
  const message = {
    code: 200,
    data: {
      needExpert: false,
      messageId: Date.now(),
      chatId: parseInt(chatId.value),
      senderIdentity: 'parent',
      senderId: 1,
      content: messageContent.value,
      createTimestamp: new Date().toISOString()
    },
    msg: null
  }
  
  ws.value.send(JSON.stringify(message))
  messageContent.value = ''
}

// 组件卸载时断开连接
onUnmounted(() => {
  disconnectWebSocket()
})

// API 测试函数
const testCreateChat = async () => {
  try {
    const data = {
      parentId: 1,
      expertId: 1
    }
    const res = await createChat(data)
    testResult.value = JSON.stringify(res.data, null, 2)
  } catch (error) {
    testResult.value = `错误: ${error.message}`
  }
}

const testGetParentChats = async () => {
  try {
    const res = await getParentChats(1)
    testResult.value = JSON.stringify(res.data, null, 2)
  } catch (error) {
    testResult.value = `错误: ${error.message}`
  }
}

const testGetExpertChats = async () => {
  try {
    const res = await getExpertChats(1)
    testResult.value = JSON.stringify(res.data, null, 2)
  } catch (error) {
    testResult.value = `错误: ${error.message}`
  }
}

const testSendMessage = async () => {
  try {
    const data = {
      chatId: 1,
      content: '测试消息',
      senderId: 1,
      senderType: 'parent'
    }
    const res = await sendParentMessage(data)
    testResult.value = JSON.stringify(res.data, null, 2)
  } catch (error) {
    testResult.value = `错误: ${error.message}`
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  /* background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%); */
  padding: 20px;
}

.login-card {
  background: white;
  padding: 2.5rem;
  border-radius: 16px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1);
  width: 400px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  transform: translateY(0);
  animation: slideUp 0.5s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
}

h2 {
  text-align: center;
  margin-bottom: 2rem;
  color: #333;
  font-size: 24px;
  font-weight: 600;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.login-input {
  margin-bottom: 1rem;
}

.login-input :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.login-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.login-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.login-button {
  width: 100%;
  height: 40px;
  font-size: 16px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.test-button {
  margin-top: 1rem;
}

.test-button .el-button {
  width: 100%;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.test-button .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* 测试弹窗样式 */
.test-dialog {
  :deep(.el-dialog) {
    border-radius: 16px;
    overflow: hidden;
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.15);
    animation: dialogFadeIn 0.3s ease-out;
  }
  
  @keyframes dialogFadeIn {
    from {
      opacity: 0;
      transform: scale(0.95);
    }
    to {
      opacity: 1;
      transform: scale(1);
    }
  }
  
  :deep(.el-dialog__header) {
    margin: 0;
    padding: 20px;
    background: #f8f9fa;
    border-bottom: 1px solid #eee;
  }
  
  :deep(.el-dialog__body) {
    padding: 20px;
  }
}

.test-content {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.websocket-test-area,
.api-test-area {
  background: #f8f9fa;
  padding: 1.5rem;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.websocket-test-area:hover,
.api-test-area:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.ws-controls,
.test-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.ws-controls .el-button,
.test-buttons .el-button {
  border-radius: 8px;
  transition: all 0.3s ease;
}

.ws-controls .el-button:hover,
.test-buttons .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.ws-input {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.ws-input .el-input {
  border-radius: 8px;
}

.ws-status {
  margin-bottom: 1rem;
  padding: 1rem;
  background-color: white;
  border-radius: 8px;
  border: 1px solid #eee;
  text-align: center;
  transition: all 0.3s ease;
}

.ws-status:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.ws-messages,
.test-result {
  background-color: white;
  padding: 1rem;
  border-radius: 8px;
  border: 1px solid #eee;
  max-height: 300px;
  overflow-y: auto;
  transition: all 0.3s ease;
}

.ws-messages:hover,
.test-result:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.message-item {
  padding: 0.8rem;
  background-color: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #eee;
  transition: all 0.3s ease;
}

.message-item:hover {
  transform: translateX(5px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.message-item pre {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-size: 12px;
}

.test-result pre {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-size: 12px;
}
</style>
