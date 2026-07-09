<!-- src/views/ExpertChat.vue -->
<template>
        <!-- 专家界面内容 -->
  <div class="expert-container">
    <!-- 顶部导航栏 -->
    <el-row class="header" type="flex" justify="space-between" align="middle">
      <h2>专家协助平台</h2>
      <el-space :size="16">
        <el-button 
          type="primary" 
          plain
          @click="handleMeeting"
          class="btn-meeting-element"
        >
          <el-icon><ChatDotRound /></el-icon>
          会话界面
        </el-button>
        <el-button 
          type="success" 
          plain
          @click="navigateToDecisionTree"
          class="btn-voice-element"
        >
          <el-icon><DataLine /></el-icon>
          决策树可视化
        </el-button>
        <el-button
          type="info"
          plain
          @click="navigateToStatistics"
          class="btn-statistics-element"
        >
          <el-icon><DataLine /></el-icon>
          专家行为统计
        </el-button>
      </el-space>
    </el-row>
    
    <div class="main-content">
      <!-- 左侧聊天列表 -->
      <div class="user-list">
        <div class="list-header">
          <h3>活跃对话</h3>
          <el-space :size="8">
            <el-button 
              type="primary" 
              plain
              @click="showCreateChatDialog"
              class="btn-new-chat-element"
            >
              <el-icon><Plus /></el-icon>
              新建聊天
            </el-button>

            <el-button 
              type="warning" 
              plain
              @click="toggleFilter"
              class="btn-filter-element"
            >
              <el-icon><Filter /></el-icon>
              筛选
            </el-button>
          </el-space>
        </div>
        <div class="filter-panel" v-if="showFilter">
          <el-form :model="filterForm" label-width="60px">
            <el-form-item label="状态">
              <el-select v-model="filterStatus" placeholder="请选择状态">
                <el-option label="全部" value="all" />
                <el-option label="需要介入" value="urgent" />
                <el-option label="AI模式" value="ai-mode" />
                <el-option label="处理中" value="in-progress" />
              </el-select>
            </el-form-item>
            <el-form-item label="时间">
              <el-select v-model="filterTime" placeholder="请选择时间">
                <el-option label="全部" value="all" />
                <el-option label="今天" value="today" />
                <el-option label="本周" value="week" />
                <el-option label="本月" value="month" />
              </el-select>
            </el-form-item>
          </el-form>
        </div>
        <div class="user-items">
          <el-empty v-if="chats.length === 0" description="暂无聊天记录">
            <el-button 
              type="primary" 
              @click="showCreateChatDialog"
              class="btn-create-first-element"
            >
              创建新聊天
            </el-button>
          </el-empty>
          <div v-else v-for="chat in filteredChats" 
               :key="chat.id" 
               :class="['user-item', { active: currentUser?.id === chat.id }]"
               @click="selectChat(chat)">
            <div class="user-info">
              <div class="user-name">{{ chat.name }}</div>
              <div class="user-message">{{ chat.lastMessage || '暂无消息' }}</div>
            </div>
            <div class="user-status" :class="getStatusClass(chat.status)">
              {{ getStatusText(chat.status) }}
            </div>
            <div class="user-time">{{ formatTime(chat.lastMessageTimestamp || chat.createdAt) }}</div>
          </div>
        </div>
      </div>

      <!-- 右侧聊天区域 -->
      <div class="chat-area">
        <el-empty v-if="!currentUser" description="请选择一个聊天会话或创建新的聊天">
          <el-button 
            type="primary" 
            @click="showCreateChatDialog"
            class="btn-create-chat-element"
          >
            创建新聊天
          </el-button>
        </el-empty>
        <template v-else>
          <div class="chat-header">
            <div class="chat-info">
              <span class="chat-name">{{ currentUser.name }}</span>
              <el-tag size="small" type="info" class="chat-id-element">
                对话ID: {{ currentUser.id }}
              </el-tag>
            </div>
            <el-space :size="16">
              <el-button 
                type="danger" 
                plain
                @click="confirmDelete(currentUser)"
                class="btn-delete-element"
              >
                <el-icon><Delete /></el-icon>
                删除对话
              </el-button>
              <el-button 
                :type="currentUser.status === 0 ? 'success' : 'primary'"
                plain
                @click="toggleChatMode"
                class="btn-mode-switch-element"
              >
                <el-icon><Switch /></el-icon>
                {{ currentUser.status === 0 ? '切换为专家模式' : '切换为AI模式' }}
              </el-button>
            </el-space>
            <div class="chat-mode">
              <span class="mode-label">模式:</span>
              <span class="mode-value" :class="getStatusClass(currentUser.status)">
                {{ getStatusText(currentUser.status) }}
              </span>
            </div>
          </div>

          <!-- 修改布局为左右结构 -->
          <div class="content-container">
            <!-- 家长画像和回复策略面板 -->
            <div v-if="parentModel" class="parent-model-panel">
              <el-card class="model-card">
                <template #header>
                  <div class="card-header">
                    <h3>家长画像与回复策略</h3>
                    <br />
                    <el-tag v-if="parentModel.tag" size="small" type="success">{{ parentModel.tag }}</el-tag>
                  </div>
                </template>
                <div class="model-content">
                  <div class="model-item">
                    <div class="model-label">家长特点:</div>
                    <div class="model-value">{{ parentModel.profile || '暂无数据' }}</div>
                  </div>
                  <div class="model-item">
                    <div class="model-label">事件总结:</div>
                    <div class="model-value">{{ parentModel.event_summary || parentModel.eventSummary || '暂无数据' }}</div>
                  </div>
                  <div class="model-item">
                    <div class="model-label">回复策略:</div>
                    <div class="model-value">{{ parentModel.reply_strategy || parentModel.replyStrategy || '暂无数据' }}</div>
                  </div>
                  <div class="model-item">
                    <div class="model-label">当前需求:</div>
                    <div class="model-value">{{ parentModel.current_need || parentModel.currentNeed || '暂无数据' }}</div>
                  </div>
                  <div class="model-item">
                    <div class="model-label">主要问题:</div>
                    <div class="model-value">{{ parentModel.presenting_problem || parentModel.presentingProblem || '暂无数据' }}</div>
                  </div>
                  <div class="model-item">
                    <div class="model-label">情绪状态:</div>
                    <div class="model-value">{{ parentModel.emotion_state || parentModel.emotionState || '暂无数据' }}</div>
                  </div>
                  <div class="model-item">
                    <div class="model-label">风险信号:</div>
                    <div class="model-value">{{ formatModelValue(parentModel.risk_signals || parentModel.riskSignals) }}</div>
                  </div>
                  <div class="model-item">
                    <div class="model-label">客观背景:</div>
                    <div class="model-value">{{ formatModelValue(parentModel.objective_background || parentModel.objectiveBackground) }}</div>
                  </div>
                  <div class="model-item">
                    <div class="model-label">主观感知:</div>
                    <div class="model-value">{{ formatModelValue(parentModel.subjective_perception || parentModel.subjectivePerception) }}</div>
                  </div>
                  <div class="model-item">
                    <div class="model-label">关系背景:</div>
                    <div class="model-value">{{ parentModel.relationship_context || parentModel.relationshipContext || '暂无数据' }}</div>
                  </div>
                  <div class="model-item">
                    <div class="model-label">沟通风格:</div>
                    <div class="model-value">{{ parentModel.communication_style || parentModel.communicationStyle || '暂无数据' }}</div>
                  </div>
                  <div class="model-item">
                    <div class="model-label">敏感点:</div>
                    <div class="model-value">{{ formatModelValue(parentModel.sensitivity_points || parentModel.sensitivityPoints) }}</div>
                  </div>
                  <div class="model-item">
                    <div class="model-label">下一步提问:</div>
                    <div class="model-value">{{ parentModel.next_best_question || parentModel.nextBestQuestion || '暂无数据' }}</div>
                  </div>
                </div>
              </el-card>
            </div>
            <!-- 占位区域，确保布局一致 -->
            <div v-else class="parent-model-panel empty-model">
              <div class="empty-model-content">
                <p>暂无家长画像</p>
              </div>
            </div>

            <div class="messages-container">
              <div class="chat-messages" ref="messageContainer">
                <div v-for="message in messages" 
                    :key="message.id" 
                    :class="['message', message.senderIdentity, { 'ai-interaction': message.isAIMessage }]">
                  <div class="message-info">
                    <span class="sender">
                      {{ message.senderIdentity === 'parent' ? '家长' : 
                         message.senderIdentity === 'expert' ? '专家' : 
                         message.senderIdentity === 'bot' ? 'AI' : '系统' }}
                    </span>
                    <span class="time">{{ formatTime(message.timestamp) }}</span>
                  </div>
                  <div :class="['message-content', 
                              message.isPlaceholder ? 'placeholder' : '',
                              message.senderIdentity !== 'parent' && message.senderIdentity !== 'expert' ? 'ai-message' : '']">
                    {{ message.content }}
                  </div>
                </div>
              </div>

              <div class="chat-input" style="display: flex; flex-direction: column; gap: 12px;">
                <!-- AI对话输入框 -->
                <div style="display: flex; gap: 12px; align-items: flex-start;">
                  <el-input
                    v-model="aiExpertInput"
                    type="textarea"
                    :rows="2"
                    placeholder="输入与AI对话内容..."
                  />
                  <el-button type="success" @click="sendAIInstruction">AI回复</el-button>
                </div>
                <!-- 普通聊天输入框 -->
                <div style="display: flex; gap: 12px; align-items: flex-start;">
                  <el-input
                    v-model="messageInput"
                    type="textarea"
                    :rows="2"
                    placeholder="输入回复内容..."
                    @keydown.enter.exact.prevent="sendMessage"
                    @keydown.enter.shift.exact="newline"
                    class="message-input-element"
                  />
                  <el-button type="primary" @click="sendMessage" class="btn-send-element">发送</el-button>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- 确认删除对话框 -->
    <el-dialog
      v-model="showDeleteDialog"
      title="确认删除"
      width="400px"
      class="delete-dialog-element"
    >
      <p>您确定要删除这个对话吗？此操作不可恢复。</p>
      <template #footer>
        <el-space :size="16">
          <el-button @click="showDeleteDialog = false">取消</el-button>
          <el-button type="danger" @click="handleDelete">确认删除</el-button>
        </el-space>
      </template>
    </el-dialog>

    <!-- 新建聊天对话框 -->
    <el-dialog
      v-model="showNewChatDialog"
      title="新建聊天"
      width="400px"
      class="new-chat-dialog-element"
    >
      <div v-if="loadingParents" class="dialog-loading">
        <el-skeleton :rows="3" animated />
      </div>
      <div v-else>
        <div v-if="parents.length === 0" class="empty-parents">
          <el-empty description="暂无可选家长" />
        </div>
        <div v-else class="parent-list">
          <p>请选择要联系的家长：</p>
          <div class="parent-grid">
            <el-card 
              v-for="parent in parents" 
              :key="parent.parentId" 
              shadow="hover" 
              @click="createNewChat(parent)"
              class="parent-card"
            >
              <div class="parent-name">{{ parent.parentName }}</div>
              <div class="parent-id">ID: {{ parent.parentId }}</div>
            </el-card>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="closeNewChatDialog">取消</el-button>
      </template>
    </el-dialog>

    <!-- AI推荐回复对话框 -->
    <el-dialog
      v-model="showAIRecommendation"
      title="AI推荐回复"
      width="500px"
    >
      <div class="recommendation-content">
        <p>{{ aiRecommendation }}</p>
      </div>
      <template #footer>
        <el-space>
          <el-button @click="getRecommendation">换一个</el-button>
          <el-button type="primary" @click="adoptRecommendation">采纳</el-button>
          <el-button @click="showAIRecommendation = false">取消</el-button>
        </el-space>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { Client } from '@stomp/stompjs'
import { useRouter } from 'vue-router'
import { 
  getExpertChats, 
  sendExpertMessage, 
  getChatMessages, 
  updateChatStatus, 
  updateReadTime,
  deleteChat,
  createChat,
  getAllParents,
  getParentModel,
  getExpertAIMessages,
  sendExpertAIInstruction,
  getAIRecommendation,
  adoptAIRecommendation
} from '@/api/chat'
import {
  ChatDotRound,
  DataLine,
  Plus,
  Refresh,
  Filter,
  Delete,
  Switch
} from '@element-plus/icons-vue'

const router = useRouter()
const messageInput = ref('')
const currentUser = ref(null)
const messageContainer = ref(null)
const showFilter = ref(false)
const filterStatus = ref('all')
const filterTime = ref('all')
let stompClient = null

// 聊天列表
const chats = ref([])
// 当前聊天消息
const messages = ref([])
// WebSocket 连接状态
const wsConnected = ref(false)
// AI回复占位消息ID
const aiPlaceholderId = ref(null)

// 固定的专家ID
const EXPERT_ID = 1

// 添加删除对话框相关变量
const showDeleteDialog = ref(false)
const chatToDelete = ref(null)

// 添加一个跟踪临时消息ID的映射
const tempMessageIds = ref(new Map());

// 添加新建聊天相关变量
const showNewChatDialog = ref(false)
const parents = ref([])
const loadingParents = ref(false)

// 添加家长画像和回复策略数据
const parentModel = ref(null)
const aiExpertInput = ref("")
const aiRecommendation = ref('')
const showAIRecommendation = ref(false)

// 过滤后的聊天列表
const filteredChats = computed(() => {
  let result = [...chats.value]
  
  if (filterStatus.value !== 'all') {
    result = result.filter(chat => {
      if (filterStatus.value === 'urgent') return chat.status === 1
      if (filterStatus.value === 'ai-mode') return chat.status === 0
      if (filterStatus.value === 'in-progress') return chat.status === 2
      return true
    })
  }
  
  // 时间过滤逻辑
  if (filterTime.value !== 'all') {
    const now = new Date()
    result = result.filter(chat => {
      const chatTime = new Date(chat.lastMessageTimestamp)
      if (filterTime.value === 'today') {
        return chatTime.toDateString() === now.toDateString()
      }
      if (filterTime.value === 'week') {
        const weekStart = new Date(now.setDate(now.getDate() - now.getDay()))
        return chatTime >= weekStart
      }
      if (filterTime.value === 'month') {
        return chatTime.getMonth() === now.getMonth() && chatTime.getFullYear() === now.getFullYear()
      }
      return true
    })
  }
  
  return result
})

// 获取聊天列表
const fetchChats = async () => {
  console.log('fetchChats function called');
  try {
    const response = await getExpertChats(EXPERT_ID)
    console.log('API response received:', response);

    // Log the value and type right before the check
    console.log('Checking response.code value:', response?.code);
    console.log('Checking response.code type:', typeof response?.code);

    if (response.code === 200) {
      console.log('Inside if block, code is 200.');
      console.log('Attempting to log original data array...');
      console.log('Original data array:', response.data);
      console.log('Original data array length:', response.data?.length);
      console.log('First element of original data:', response.data?.[0]);

      const mappedChats = response.data.map(chat => ({
        ...chat,
        lastMessageTimestamp: chat.lastMessageTimestamp || chat.createdAt
      }));
      console.log('Mapped data array:', mappedChats);
      chats.value = mappedChats;
      console.log('Assigned to chats.value, current chats.value:', chats.value);
      
      // 如果 WebSocket 已连接，订阅新聊天的消息
      if (wsConnected.value) {
        chats.value.forEach(chat => {
          subscribeToChat(chat.id)
          subscribeToParentModel(chat.id)
        })
      }
    }
  } catch (error) {
    console.error('获取聊天列表失败:', error)
  }
}

// 获取聊天消息
const fetchMessages = async (chatId) => {
  console.log(`fetchMessages called for chatId: ${chatId}`);
  try {
    console.log(`Calling getExpertAIMessages API for chatId: ${chatId}`);
    const response = await getExpertAIMessages(chatId)
    console.log(`getExpertAIMessages API response for chatId ${chatId}:`, response);

    if (response && response.code === 200) {
      console.log(`Successfully fetched messages for chatId ${chatId}, data:`, response.data);
      
      // 处理普通消息
      const commonMessages = (response.data.commonMessages || []).map(msg => ({
        ...msg,
        timestamp: msg.timestamp || msg.createTimestamp || new Date().toISOString()
      }));
      
      // 处理专家与AI交互消息
      const aiMessages = (response.data.expertsAIMessages || []).map(msg => ({
        ...msg,
        timestamp: msg.timestamp || msg.createTimestamp || new Date().toISOString(),
        isAIMessage: true // 标记为AI交互消息
      }));
      
      // 合并消息并按时间排序
      messages.value = [...commonMessages, ...aiMessages].sort((a, b) => 
        new Date(a.timestamp) - new Date(b.timestamp)
      );
      
      console.log(`messages.value updated for chatId ${chatId}:`, messages.value);
      scrollToBottom()
    } else {
      console.error(`获取消息失败 (API non-200), chatID: ${chatId}, code: ${response?.code}, msg: ${response?.msg}`);
      messages.value = [];
    }
  } catch (error) {
    console.error(`获取消息失败 (catch block), chatID: ${chatId}:`, error)
    messages.value = [];
  }
}

// 发送消息
const sendMessage = async () => {
  console.log('sendMessage called'); // Log function entry
  console.log('messageInput:', messageInput.value);
  console.log('currentUser:', currentUser.value);

  if (!messageInput.value.trim() || !currentUser.value) {
    console.log('sendMessage stopped: message input is empty or no user selected.');
    return;
  }

  console.log('Preparing messageData...');
  try {
    const messageData = {
      chatId: currentUser.value.id,
      senderId: EXPERT_ID,
      receiverId: currentUser.value.parentId,
      content: messageInput.value.trim() // Use trimmed value
    }
    console.log('messageData prepared:', messageData);

    // 生成临时ID用于乐观更新
    const tempId = Date.now();
    
    // --- Optimistic Update --- 
    // Construct the message object locally to display immediately
    const sentMessage = {
      id: tempId, // Use timestamp as temporary ID, might be updated by WebSocket later
      chatId: currentUser.value.id,
      senderIdentity: 'expert', // Assuming the sender is always the expert in this component
      senderId: EXPERT_ID,
      content: messageInput.value.trim(),
      timestamp: new Date().toISOString() // Use current time
    };
    
    // 记录原始消息内容，用于后续WebSocket消息匹配
    tempMessageIds.value.set(messageInput.value.trim(), tempId);
    
    // 10秒后自动清理，避免内存泄漏
    setTimeout(() => {
      tempMessageIds.value.delete(messageInput.value.trim());
    }, 10000);
    
    messages.value.push(sentMessage);
    console.log('Optimistically added message to list:', sentMessage);
    scrollToBottom();
    // --- End Optimistic Update ---

    // 清空输入框
    messageInput.value = '';
    
    console.log('Calling sendExpertMessage API...');
    const response = await sendExpertMessage(messageData)
    console.log('sendExpertMessage API response:', response);

    if (response.code !== 200) { 
      console.error('Send message API returned non-200 code:', response.code, response.msg);
    }
  } catch (error) {
    console.error('发送消息失败 (catch block):', error)
  }
}

// 更新聊天状态
const updateStatus = async (chatId) => {
  try {
    // 调用专家模式，不需要传递特定参数
    const response = await updateChatStatus(chatId)
    if (response.code === 200) {
      // 更新本地状态
      const chat = chats.value.find(c => c.id === chatId)
      if (chat) {
        chat.status = 2 // 专家已介入
      }
      console.log(`聊天 ${chatId} 状态已更新为专家已介入`)
    }
  } catch (error) {
    console.error('更新状态失败:', error)
  }
}

// 设置聊天为AI托管模式
const setToAIMode = async (chatId) => {
  try {
    // 调用AI模式，传递mode参数
    const response = await updateChatStatus(chatId, { mode: 'ai' })
    if (response.code === 200) {
      // 更新本地状态
      const chat = chats.value.find(c => c.id === chatId)
      if (chat) {
        chat.status = 0 // AI托管
      }
      console.log(`聊天 ${chatId} 状态已更新为AI托管`)
    }
  } catch (error) {
    console.error('更新状态至AI托管失败:', error)
  }
}

// 更新已读时间
const updateLastRead = async (chatId) => {
  try {
    const data = {
      lastReadTimestampExpert: Date.now(),
      lastReadTimestampParent: null
    }
    await updateReadTime(chatId, data)
  } catch (error) {
    console.error('更新已读时间失败:', error)
  }
}

// 选择聊天
const selectChat = async (chat) => {
  console.log('selectChat called with chat:', chat);
  
  // 切换到新的聊天
  currentUser.value = chat
  console.log(`currentUser set to id: ${chat.id}, name: ${chat.name}`);
  
  try {
    await fetchMessages(chat.id)
    await updateLastRead(chat.id)
    // 获取家长画像和回复策略
    await fetchParentModel(chat.id)
    
    console.log(`Finished processing selectChat for id: ${chat.id}`);
  } catch (error) {
    console.error(`Error during selectChat processing for id: ${chat.id}:`, error);
  }
}

// 滚动到底部
const scrollToBottom = () => {
  setTimeout(() => {
    if (messageContainer.value) {
      messageContainer.value.scrollTop = messageContainer.value.scrollHeight
    }
  }, 0)
}

// 切换筛选面板
const toggleFilter = () => {
  showFilter.value = !showFilter.value
}

// 刷新聊天列表
const refreshChats = () => {
  fetchChats()
}

// 生命周期钩子
onMounted(() => {
  fetchChats()
  initWebSocket()
})

// WebSocket相关
const initWebSocket = () => {
  // 初始化STOMP客户端
  const wsTarget = import.meta.env.VITE_AICO_WS_TARGET || 'ws://localhost:8081/chat'
  const socket = new WebSocket(wsTarget)
  stompClient = new Client({
    webSocketFactory: () => socket,
    debug: (str) => console.log(str),
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000
  })

  stompClient.onConnect = (frame) => {
    console.log('成功连接WebSocket:', frame)
    wsConnected.value = true
    
    // 如果已经获取了聊天列表，订阅所有聊天
    if (chats.value.length > 0) {
      chats.value.forEach(chat => {
        subscribeToChat(chat.id)
        subscribeToParentModel(chat.id)
      })
    }
    
    // 如果当前有选中的用户，订阅该用户的聊天
    if (currentUser.value) {
      subscribeToChat(currentUser.value.id)
      subscribeToParentModel(currentUser.value.id)
    }
  }

  stompClient.onStompError = (frame) => {
    console.error('STOMP协议错误:', frame.headers.message)
    wsConnected.value = false
  }

  stompClient.activate()
}

// 订阅聊天频道
const subscribeToChat = (chatId) => {
  console.log(`订阅聊天: ${chatId}`)
  if (!stompClient || !wsConnected.value) {
    console.error('WebSocket未连接，无法订阅')
    return
  }
  
  // 订阅普通聊天消息
  try {
    const subscription = stompClient.subscribe(
      `/topic/chat/${chatId}`,
      (message) => handleIncomingMessage(message, chatId),
      { id: `expert-sub-${chatId}` }
    )
    console.log(`成功订阅聊天 ${chatId}, subscription ID: ${subscription.id}`)
  } catch (error) {
    console.error(`订阅聊天失败 ${chatId}:`, error)
  }

  // 订阅专家与AI交互消息
  try {
    const aiSubscription = stompClient.subscribe(
      `/topic/chat/${chatId}/expert_ai`,
      (message) => handleAIInteractionMessage(message, chatId),
      { id: `expert-ai-sub-${chatId}` }
    )
    console.log(`成功订阅专家与AI交互 ${chatId}, subscription ID: ${aiSubscription.id}`)
  } catch (error) {
    console.error(`订阅专家与AI交互失败 ${chatId}:`, error)
  }
}

// 处理接收到的消息
const handleIncomingMessage = (message, chatId) => {
  console.log(`收到聊天 ${chatId} 的消息:`, message.body)
  try {
    const data = JSON.parse(message.body)
    
    // 检查消息是否有效
    if (!data || !data.data) {
      console.error('收到无效消息格式:', message.body)
      return
    }
    
    const messageData = data.data
    
    // 检查消息是否属于当前聊天
    if (currentUser.value && messageData.chatId === currentUser.value.id) {
      // 检查是否是自己刚刚发送的消息（通过内容匹配）
      // 如果是自己刚刚通过API发送的消息，则跳过添加（因为已经在UI上乐观添加过了）
      const isSentByMe = messageData.senderIdentity === 'expert' && 
                          messageData.senderId === EXPERT_ID && 
                          tempMessageIds.value.has(messageData.content);
      
      if (isSentByMe) {
        console.log('跳过添加自己已发送的消息:', messageData.content);
        // 找到临时消息并更新ID
        const tempId = tempMessageIds.value.get(messageData.content);
        const tempMessage = messages.value.find(m => m.id === tempId);
        if (tempMessage) {
          tempMessage.id = messageData.messageId;
        }
        return;
      }
      
      // 如果存在占位消息，并且收到了新消息（AI自动回复或系统消息），替换占位消息
      if (aiPlaceholderId.value) {
        console.log('检测到AI占位消息，尝试替换:', aiPlaceholderId.value);
        // 查找占位消息
        const placeholderIndex = messages.value.findIndex(m => m.id === aiPlaceholderId.value);
        
        if (placeholderIndex !== -1) {
          console.log('找到占位消息，替换为:', messageData.content);
          // 替换占位消息
          messages.value[placeholderIndex] = {
            id: messageData.messageId,
            chatId: messageData.chatId,
            senderIdentity: messageData.senderIdentity,
            senderId: messageData.senderId,
            content: messageData.content,
            timestamp: messageData.createTimestamp
          };
          
          // 重置占位消息ID
          aiPlaceholderId.value = null;
          
          scrollToBottom();
          return;
        } else {
          console.log('未找到占位消息，重置占位ID');
          // 如果找不到占位消息，也重置ID，避免后续消息出现问题
          aiPlaceholderId.value = null;
        }
      }
      
      // 检查消息是否已存在（避免重复）
      if (!messages.value.some(m => 
          m.id === messageData.messageId || 
          (m.content === messageData.content && 
           m.senderIdentity === messageData.senderIdentity &&
           m.senderId === messageData.senderId))) {
           
        // 添加消息
        messages.value.push({
          id: messageData.messageId,
          chatId: messageData.chatId,
          senderIdentity: messageData.senderIdentity,
          senderId: messageData.senderId,
          content: messageData.content,
          timestamp: messageData.createTimestamp
        });
        
        // 如果是家长发送的消息，并且当前对话处于AI模式，添加一个AI回复的占位消息
        if (messageData.senderIdentity === 'parent' && currentUser.value.status === 0) {
          console.log('家长消息，添加AI回复占位消息');
          const placeholderId = 'ai-placeholder-' + Date.now();
          aiPlaceholderId.value = placeholderId;
          
          // 添加占位消息
          messages.value.push({
            id: placeholderId,
            chatId: messageData.chatId,
            senderIdentity: 'expert',
            senderId: EXPERT_ID,
            content: 'AI回复生成中...',
            timestamp: new Date().toISOString(),
            isPlaceholder: true
          });
        }
        
        scrollToBottom();
      } else {
        console.log('消息已存在，避免重复:', messageData);
      }
    }
    
    // 更新聊天列表中的最后一条消息
    const chat = chats.value.find(c => c.id === messageData.chatId)
    if (chat) {
      chat.lastMessage = messageData.content
      chat.lastMessageTimestamp = messageData.createTimestamp
    }
  } catch (error) {
    console.error('处理消息失败:', error)
  }
}

// 处理专家与AI交互消息
const handleAIInteractionMessage = (message, chatId) => {
  console.log(`收到专家与AI交互消息:`, message.body)
  try {
    const data = JSON.parse(message.body)
    
    if (!data || !data.data) {
      console.error('收到无效消息格式:', message.body)
      return
    }
    
    const messageData = data.data
    
    // 如果是AI推荐回复
    if (typeof messageData === 'string') {
      aiRecommendation.value = messageData
      return
    }
    
    // 如果是其他类型的消息，添加到消息列表
    if (currentUser.value && messageData.chatId === currentUser.value.id) {
      messages.value.push({
        id: messageData.messageId,
        chatId: messageData.chatId,
        senderIdentity: messageData.senderIdentity,
        senderId: messageData.senderId,
        content: messageData.content,
        timestamp: messageData.createTimestamp || new Date().toISOString()
      })
      scrollToBottom()
    }
  } catch (error) {
    console.error('处理专家与AI交互消息失败:', error)
  }
}

// 组件卸载前断开连接
onBeforeUnmount(() => {
  if (stompClient) {
    stompClient.deactivate().then(() => {
      console.log('WebSocket连接已关闭')
      wsConnected.value = false
    }).catch(error => {
      console.error('关闭WebSocket连接失败:', error)
    })
  }
})

// 获取状态对应的 CSS 类名
const getStatusClass = (status) => {
  switch (status) {
    case 0: return 'ai-mode'
    case 1: return 'urgent'
    case 2: return 'in-progress'
    default: return ''
  }
}

// 获取状态对应的文本
const getStatusText = (status) => {
  switch (status) {
    case 0: return 'AI模式'
    case 1: return '需要介入'
    case 2: return '处理中'
    default: return ''
  }
}

// 格式化时间
const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const now = new Date()
  
  // 如果是今天
  if (date.toDateString() === now.toDateString()) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  
  // 判断是否是今年
  const isCurrentYear = date.getFullYear() === now.getFullYear()
  
  if (isCurrentYear) {
    // 今年但不是今天，使用"X月X日 HH:MM"格式
    return `${date.getMonth() + 1}月${date.getDate()}日 ${date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`
  } else {
    // 不是今年，使用"YYYY年X月X日 HH:MM"格式
    return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日 ${date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`
  }
}

// 处理换行
const newline = (e) => {
  e.preventDefault()
  messageInput.value += '\n'
}

// 处理转接
const handleTransfer = () => {
  // TODO: 实现转接功能
  console.log('转接对话')
}

// 切换聊天模式
const toggleChatMode = async () => {
  if (!currentUser.value) return;
  
  try {
    if (currentUser.value.status === 0) {
      // 如果当前是AI模式，切换为专家模式
      const response = await updateChatStatus(currentUser.value.id);
      if (response.code === 200) {
        currentUser.value.status = 2; // 专家已介入
        console.log(`聊天 ${currentUser.value.id} 已切换为专家模式`);
      }
    } else {
      // 如果当前是专家模式或需要介入，切换为AI模式
      const response = await updateChatStatus(currentUser.value.id, { mode: 'ai' });
      if (response.code === 200) {
        currentUser.value.status = 0; // AI托管
        console.log(`聊天 ${currentUser.value.id} 已切换为AI模式`);
      }
    }
  } catch (error) {
    console.error('切换聊天模式失败:', error);
  }
}

// 跳转到决策树页面
const navigateToDecisionTree = () => {
  router.push('/decision-tree')
}

// 处理会话界面按钮点击
const handleMeeting = () => {
  // 如果已经在会话界面则不需要跳转
  console.log('会话界面按钮点击')
}

// 打开确认删除对话框
const confirmDelete = (chat) => {
  chatToDelete.value = chat
  showDeleteDialog.value = true
}

// 执行删除聊天
const handleDelete = async () => {
  if (!chatToDelete.value) return
  
  try {
    console.log(`正在删除聊天: ${chatToDelete.value.id}`);
    const response = await deleteChat(chatToDelete.value.id)
    console.log('删除聊天响应:', response);
    
    if (response.code === 200) {
      // 从聊天列表中移除
      chats.value = chats.value.filter(c => c.id !== chatToDelete.value.id)
      
      // 如果当前选中的聊天被删除，则清空当前聊天
      if (currentUser.value && currentUser.value.id === chatToDelete.value.id) {
        currentUser.value = null
        messages.value = []
      }
      
      // 关闭对话框
      showDeleteDialog.value = false
      chatToDelete.value = null
      
      console.log('聊天删除成功');
    } else {
      console.error('删除聊天返回非200状态码:', response);
    }
  } catch (error) {
    console.error('删除聊天失败:', error);
  }
}

// 打开创建聊天对话框
const showCreateChatDialog = async () => {
  showNewChatDialog.value = true
  await fetchParents()
}

// 关闭新建聊天对话框
const closeNewChatDialog = () => {
  showNewChatDialog.value = false
}

// 获取家长列表
const fetchParents = async () => {
  loadingParents.value = true
  try {
    const response = await getAllParents()
    console.log('获取家长列表响应:', response)
    if (response.code === 200 && response.data) {
      // 确保数据格式正确，检查并适配字段名称
      parents.value = response.data.map(parent => {
        // 如果API返回的是不同的字段名，进行适配
        return {
          parentId: parent.parentId || parent.id,
          parentName: parent.parentName || parent.name
        }
      })
      console.log('处理后的家长列表:', parents.value)
    } else {
      parents.value = []
    }
  } catch (error) {
    console.error('获取家长列表失败:', error)
    parents.value = []
  } finally {
    loadingParents.value = false
  }
}

// 创建新聊天
const createNewChat = async (parent) => {
  try {
    const chatData = {
      parentId: parent.parentId,
      expertId: EXPERT_ID
    }
    
    const response = await createChat(chatData)
    if (response.code === 200 && response.data) {
      showNewChatDialog.value = false
      // 刷新聊天列表
      await fetchChats()
      // 选中新创建的聊天
      const newChat = chats.value.find(c => c.id === response.data.id)
      if (newChat) {
        selectChat(newChat)
      }
    } else {
      ElMessage.error('创建聊天失败')
    }
  } catch (error) {
    console.error('创建聊天失败:', error)
    ElMessage.error('创建聊天失败')
  }
}

// 添加新的辅助函数
const getStatusTagType = (status) => {
  switch (status) {
    case 0: return 'success'
    case 1: return 'danger'
    case 2: return 'warning'
    default: return 'info'
  }
}

const getSenderTagType = (senderIdentity) => {
  switch (senderIdentity) {
    case 'parent': return 'primary'
    case 'expert': return 'warning'
    case 'ai': return 'success'
    default: return 'info'
  }
}

// 获取家长画像和回复策略
const fetchParentModel = async (chatId) => {
  try {
    console.log(`获取聊天 ${chatId} 的家长画像和回复策略`);
    const response = await getParentModel(chatId)
    
    if (response.code === 200 && response.data) {
      console.log('家长画像和回复策略:', response.data);
      parentModel.value = response.data;
    } else {
      console.error('获取家长画像和回复策略失败:', response);
      parentModel.value = null;
    }
  } catch (error) {
    console.error('获取家长画像和回复策略出错:', error);
    parentModel.value = null;
  }
}

// 处理来自WebSocket的家长画像更新
const handleParentModelUpdate = (message) => {
  try {
    const data = JSON.parse(message.body);
    console.log('收到家长画像更新:', data);
    
    if (data.code === 200 && data.data) {
      // 更新家长画像数据
      parentModel.value = data.data;
    }
  } catch (error) {
    console.error('处理家长画像更新出错:', error);
  }
}

// 订阅家长画像更新
const subscribeToParentModel = (chatId) => {
  console.log(`订阅聊天 ${chatId} 的家长画像更新`);
  if (!stompClient || !wsConnected.value) {
    console.error('WebSocket未连接，无法订阅家长画像更新');
    return;
  }
  
  try {
    const subscription = stompClient.subscribe(
      `/topic/chat/${chatId}/parent-model`,
      (message) => handleParentModelUpdate(message),
      { id: `parent-model-sub-${chatId}` }
    );
    console.log(`成功订阅聊天 ${chatId} 的家长画像更新, subscription ID: ${subscription.id}`);
  } catch (error) {
    console.error(`订阅聊天 ${chatId} 的家长画像更新失败:`, error);
  }
}

const formatModelValue = (value) => {
  if (!value) return '暂无数据'
  if (Array.isArray(value)) return value.length ? value.join('；') : '暂无数据'
  if (typeof value === 'object') {
    const entries = Object.entries(value).filter(([, item]) => item !== null && item !== undefined && item !== '')
    return entries.length ? entries.map(([key, item]) => `${key}: ${item}`).join('；') : '暂无数据'
  }
  return value
}

// 跳转到专家行为统计页面
const navigateToStatistics = () => {
  router.push('/statistics')
}

// 发送AI指示消息
const sendAIInstruction = async () => {
  if (!aiExpertInput.value.trim() || !currentUser.value) {
    return
  }

  try {
    const data = {
      expertId: EXPERT_ID,
      type: 'knowledge', // 默认类型，可以根据需要修改
      content: aiExpertInput.value.trim()
    }

    const response = await sendExpertAIInstruction(currentUser.value.id, data)
    if (response.code === 200) {
      // 清空输入框
      aiExpertInput.value = ''
      // 可以添加成功提示
      ElMessage.success('AI指示已发送')
    }
  } catch (error) {
    console.error('发送AI指示失败:', error)
    ElMessage.error('发送AI指示失败')
  }
}

// 获取AI推荐回复
const getRecommendation = async () => {
  if (!currentUser.value) return

  try {
    const response = await getAIRecommendation(currentUser.value.id)
    if (response.code === 200) {
      showAIRecommendation.value = true
      // 等待WebSocket消息更新推荐内容
    }
  } catch (error) {
    console.error('获取AI推荐失败:', error)
    ElMessage.error('获取AI推荐失败')
  }
}

// 采纳AI推荐回复
const adoptRecommendation = async () => {
  if (!currentUser.value || !aiRecommendation.value) return

  try {
    const data = {
      chatId: currentUser.value.id,
      senderId: EXPERT_ID,
      receiverId: currentUser.value.parentId,
      content: aiRecommendation.value
    }

    const response = await adoptAIRecommendation(currentUser.value.id, data)
    if (response.code === 200) {
      showAIRecommendation.value = false
      aiRecommendation.value = ''
      ElMessage.success('已采纳AI推荐回复')
    }
  } catch (error) {
    console.error('采纳AI推荐失败:', error)
    ElMessage.error('采纳AI推荐失败')
  }
}
</script>

<style scoped>
.expert-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
  width: 100vw;
  min-width: 0;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.header h2 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.header-actions {
  display: flex;
  gap: 16px;
}

.header-actions button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  background-color: #f2f6fc;
  color: #409eff;
  cursor: pointer;
}

.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
  width: 100%;
  min-width: 0;
}

.user-list {
  width: 280px;
  background-color: #fff;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}

.list-header {
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.list-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.list-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.btn-new-chat, .btn-refresh, .btn-filter {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  background-color: #f2f6fc;
  color: #409eff;
  cursor: pointer;
}

.filter-panel {
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.filter-item {
  margin-bottom: 16px;
}

.filter-item label {
  display: block;
  margin-bottom: 8px;
  font-weight: bold;
}

.filter-item select {
  width: 100%;
  padding: 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.user-items {
  flex: 1;
  overflow-y: auto;
  padding: 0;
}

.user-item {
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;
  cursor: pointer;
  position: relative;
  background-color: #fff;
  transition: background-color 0.3s;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.user-item:hover {
  background-color: #f5f7fa;
}

.user-item.active {
  background-color: #ecf5ff;
}

.user-info {
  margin-right: 80px;

}

.user-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
  margin-bottom: 4px;
}

.user-message {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 170px;
  margin-bottom: 4px;
}

.user-status {
  position: absolute;
  right: 16px;
  top: 16px;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  margin-left: 8px;
}

.user-status.urgent {
  background-color: #fff0f0;
  color: #f56c6c;
}

.user-status.ai-mode {
  background-color: #f0f9eb;
  color: #67c23a;
}

.user-status.in-progress {
  background-color: #f4f4f5;
  color: #909399;
}

.user-time {
  position: absolute;
  right: 16px;
  bottom: 16px;
  font-size: 12px;
  color: #999;
  margin-left: 8px;
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #fff;
  min-width: 0;
  width: 100%;
  overflow: hidden;
}

.chat-header {
  padding: 16px 24px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  gap: 16px;
}

.chat-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16px;
}

.chat-name {
  font-size: 16px;
  font-weight: 500;
  margin-right: 8px;
}

.chat-id {
  color: #909399;
  font-size: 14px;
  margin-left: 8px;
}

.chat-actions {
  display: flex;
  gap: 16px;
}

.btn-delete {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  background-color: #f2f6fc;
  color: #409eff;
  cursor: pointer;
}

.btn-mode-switch {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-mode-switch.ai-mode {
  background-color: #f0f9eb;
  color: #67c23a;
}

.btn-mode-switch.in-progress {
  background-color: #f2f6fc;
  color: #409eff;
}

.btn-mode-switch.urgent {
  background-color: #fff0f0;
  color: #f56c6c;
}

.chat-mode {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
}

.mode-label {
  color: #909399;
  margin-right: 4px;
}

.mode-value {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  margin-left: 4px;
}

.mode-value.urgent {
  background-color: #fff0f0;
  color: #f56c6c;
}

.mode-value.ai-mode {
  background-color: #f0f9eb;
  color: #67c23a;
}

.mode-value.in-progress {
  background-color: #f4f4f5;
  color: #909399;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.message {
  margin-bottom: 24px;
  max-width: 80%;
}

.message.parent {
  margin-right: auto;
}

.message.ai, .message.expert {
  margin-left: auto;
}

.message-info {
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.sender {
  font-size: 12px;
  font-weight: 500;
  margin-right: 8px;
}

.time {
  font-size: 12px;
  color: #999;
}

.message.parent .message-info {
  color: #409eff;
}

.message.ai .message-info {
  color: #67c23a;
}

.message.expert .message-info {
  color: #e6a23c;
}

.message-content {
  padding: 12px 16px;
  border-radius: 4px;
  font-size: 14px;
  line-height: 1.5;
}

.message.parent .message-content {
  background-color: #ecf5ff;
  color: #333;
}

.message.ai .message-content {
  background-color: #f5e8ff;
  color: #333;
}

.message.expert .message-content {
  background-color: #fdf6ec;
  color: #333;
}

.message-status {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: #fff;
}

.message.parent .message-status {
  background-color: #409eff;
}

.message.ai .message-status {
  background-color: #67c23a;
}

.message.expert .message-status {
  background-color: #e6a23c;
}

.chat-input {
  padding: 16px 24px;
  border-top: 1px solid #e4e7ed;
}

.input-area {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.input-area textarea {
  flex: 1;
  padding: 8px 16px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
}

.input-actions {
  display: flex;
  gap: 16px;
}

.btn-send, .btn-ai-send {
  padding: 8px 20px;
  border: none;
  border-radius: 4px;
  background-color: #409eff;
  color: #fff;
  cursor: pointer;
  transition: background-color 0.3s;
}

.btn-send:hover, .btn-ai-send:hover {
  background-color: #66b1ff;
}

.delete-dialog {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.delete-dialog-content {
  width: 400px;
  background-color: #fff;
  border-radius: 4px;
  padding: 24px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.delete-dialog-content h3 {
  margin-top: 0;
  color: #f56c6c;
}

.delete-dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 24px;
}

.btn-cancel {
  padding: 8px 20px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background-color: #fff;
  color: #606266;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-confirm {
  padding: 8px 20px;
  border: none;
  border-radius: 4px;
  background-color: #f56c6c;
  color: #fff;
  cursor: pointer;
  transition: background-color 0.3s;
}

.btn-cancel:hover {
  color: #409eff;
  border-color: #c6e2ff;
  background-color: #ecf5ff;
}

.btn-confirm:hover {
  background-color: #f78989;
}

/* 占位消息样式 */
.message-content.placeholder {
  background-color: #f5f5f5;
  color: #999;
  font-style: italic;
}

/* AI消息样式 */
.message-content.ai-message {
  background-color: #f5e8ff !important; /* 紫色背景，使用!important确保优先级 */
  color: #333;
}

/* 新建聊天对话框样式 */
.new-chat-dialog {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.new-chat-dialog-content {
  width: 400px;
  background-color: #fff;
  border-radius: 4px;
  padding: 24px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.new-chat-dialog-content h3 {
  margin-top: 0;
  color: #409eff;
  margin-bottom: 16px;
}

.input-area {
  margin-bottom: 16px;
}

.input-item {
  margin-bottom: 16px;
}

.input-item label {
  display: block;
  margin-bottom: 8px;
  font-weight: bold;
}

.input-item input {
  width: 100%;
  padding: 8px 16px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
}

.new-chat-dialog-actions {
  display: flex;
  justify-content: flex-end;
}

.btn-cancel {
  padding: 8px 20px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background-color: #fff;
  color: #606266;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-confirm {
  padding: 8px 20px;
  border: none;
  border-radius: 4px;
  background-color: #409eff;
  color: #fff;
  cursor: pointer;
  transition: background-color 0.3s;
}

.btn-cancel:hover {
  color: #409eff;
  border-color: #c6e2ff;
  background-color: #ecf5ff;
}

.btn-confirm:hover {
  background-color: #66b1ff;
}

/* 添加按钮图标样式 */
.icon-add:before {
  content: '+';
  font-weight: bold;
  font-size: 16px;
}

/* 空聊天状态样式 */
.empty-chats {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
  padding: 20px;
}

.empty-state {
  text-align: center;
  padding: 30px 20px;
}

.icon-empty {
  display: block;
  font-size: 48px;
  color: #dcdfe6;
  margin-bottom: 16px;
}

.icon-empty:before {
  content: '💬';
}

.empty-state p {
  color: #909399;
  margin-bottom: 16px;
}

.btn-create-first {
  background-color: #409eff;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
}

.empty-chat-area {
  display: flex;
  flex: 1;
  justify-content: center;
  align-items: center;
}

.welcome-message {
  text-align: center;
  padding: 40px;
  max-width: 500px;
}

.welcome-message h3 {
  margin-bottom: 16px;
  color: #303133;
}

.welcome-message p {
  color: #606266;
  margin-bottom: 24px;
}

.btn-create-chat {
  background-color: #409eff;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
}

/* Element Plus 组件样式覆盖 */
.el-button {
  font-size: inherit;
  padding: 8px 15px;
}

.el-tag {
  font-size: 12px;
}

.el-input,
.el-select {
  width: 100%;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .el-button {
    width: 100%;
  }
  
  .el-space {
    width: 100%;
  }
}

.message-input-element {
  margin-bottom: 16px;
}

.message-input-element :deep(.el-textarea__inner) {
  border-radius: 8px;
  padding: 12px;
  font-size: 14px;
  line-height: 1.5;
  resize: none;
}

.btn-send-element,
.btn-ai-send-element {
  border-radius: 8px;
  padding: 8px 20px;
}

.el-button {
  border-radius: 8px;
}

.el-tag {
  border-radius: 8px;
}

/* 家长列表样式 */
.parent-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 16px;
  margin-top: 16px;
  max-height: 400px;
  overflow-y: auto;
}

.parent-card {
  cursor: pointer;
  transition: all 0.3s;
}

.parent-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.parent-name {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 8px;
}

.parent-id {
  font-size: 12px;
  color: #999;
}

.dialog-loading {
  padding: 20px;
}

/* 确保对话框有足够的高度 */
.new-chat-dialog-element {
  min-height: 300px;
}

.parent-list p {
  margin-bottom: 16px;
  font-size: 14px;
  color: #606266;
}

/* 确保滚动条美观 */
.parent-grid::-webkit-scrollbar {
  width: 8px;
}

.parent-grid::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.parent-grid::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 4px;
}

.parent-grid::-webkit-scrollbar-thumb:hover {
  background: #555;
}

/* 家长画像和回复策略样式 */
.parent-model-panel {
  width: 260px;
  border-right: 1px solid #e4e7ed;
  overflow-y: auto;
  height: 100%;
}

.model-card {
  margin-bottom: 0;
  border: none;
  box-shadow: none;
}

.model-card :deep(.el-card__header) {
  padding: 10px;
  font-size: 12px;
}

.model-card :deep(.el-card__body) {
  padding: 10px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-direction: column;
  gap: 4px; /* 控制行间距 */
}

.card-header h3 {
  white-space: nowrap; /* 强制标题不换行 */
  overflow: hidden;
  text-overflow: ellipsis; /* 过长时显示省略号 */
  flex-shrink: 0; /* 防止标题被压缩 */
  min-width: 25px;
  margin: 0;
  font-size: 14px;
  color: #303133;
}

.model-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.model-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.model-label {
  font-weight: 500;
  font-size: 12px;
  color: #606266;
}

.model-value {
  color: #303133;
  line-height: 1.4;
  font-size: 12px;
  word-break: break-all;
}

/* 修改布局为左右结构 */
.content-container {
  display: flex;
  flex: 1;
  height: calc(100vh - 140px);
  overflow: hidden;
}

.messages-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.chat-input {
  padding: 16px;
  border-top: 1px solid #e4e7ed;
  background-color: #fff;
}

.empty-model {
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f5f7fa;
}

.empty-model-content {
  text-align: center;
  color: #909399;
  font-size: 12px;
}

/* 按钮间距优化 */
.list-header el-space,
.list-header .el-space {
  gap: 16px !important;
}

.panel-actions,
.input-actions {
  gap: 16px !important;
}

.btn-new-chat-element {
  margin-right: 8px;
}

.btn-send-element {
  margin-right: 8px;
}

/* 新增专家与AI对话回复框样式 */
.ai-expert-reply-box {
  margin-top: 16px;
  padding: 16px;
  border-top: 1px solid #e4e7ed;
  background: #f9fafc;
  display: flex;
  gap: 12px;
  align-items: flex-start;
}
.ai-expert-reply-box .el-input {
  flex: 1;
}
.ai-expert-reply-box .el-button {
  min-width: 80px;
}

/* AI推荐回复对话框样式 */
.recommendation-content {
  padding: 16px;
  background-color: #f5f7fa;
  border-radius: 4px;
  margin-bottom: 16px;
}

.recommendation-content p {
  margin: 0;
  line-height: 1.6;
  color: #303133;
}

/* AI交互消息样式 */
.message.ai-interaction {
  background-color: #f0f9eb;
  border-left: 3px solid #67c23a;
  margin: 8px 0;
  padding: 8px;
  border-radius: 4px;
}

.message.ai-interaction .message-info {
  color: #67c23a;
}

.message.ai-interaction .message-content {
  background-color: #f0f9eb;
  color: #333;
}
</style>
