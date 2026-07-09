<!-- src/views/ParentChat.vue -->
<template>
  <!-- 家长界面内容 -->
  <div class="parent-container">
    <!-- 顶部导航栏 -->
    <div class="header">
      <h2>家长咨询平台</h2>
    </div>
    
    <div class="main-content">
      <!-- 左侧用户列表 -->
      <div class="user-list">
        <div class="list-header">
          <h3>活跃对话</h3>
          <div class="list-actions">
            <!-- 新建聊天按钮 -->
            <button class="btn-new-chat" @click="showCreateChatDialog">
              <i class="icon-add"></i>
              新建对话
            </button>
          </div>
        </div>
        <div class="user-items">
          <div v-if="isLoading" class="loading-state">
            <p>加载中...</p>
          </div>
          <div v-else-if="chats.length === 0" class="empty-chats">
            <div class="empty-state">
              <i class="icon-empty"></i>
              <p>暂无对话，请点击"新建对话"开始咨询</p>
            </div>
          </div>
          <div v-else v-for="chat in chats" 
               :key="chat.id" 
               :class="['user-item', { active: currentChat?.id === chat.id }]"
               @click="selectChat(chat)">
            <div class="user-info">
              <div class="user-name">{{ chat.expertName || '专家' }}</div>
              <div class="user-message">{{ chat.lastMessage || '暂无消息' }}</div>
            </div>
            <div class="user-time">{{ formatTime(chat.lastMessageTimestamp || chat.createdAt) }}</div>
          </div>
        </div>
      </div>

      <!-- 右侧聊天区域 -->
      <div class="chat-area">
        <div v-if="!currentChat" class="empty-chat-area">
          <div class="welcome-message">
            <h3>欢迎使用家长咨询平台</h3>
            <p>请选择一个聊天会话开始交流，或者点击"新建对话"开始咨询</p>
            <button class="btn-create" @click="showCreateChatDialog">新建对话</button>
          </div>
        </div>
        <div class="chat-header" v-if="currentChat">
          <div class="chat-info">
            <span class="chat-name">{{ currentChat.expertName || '专家' }}</span>
            <span class="chat-id">对话ID: {{ currentChat.id }}</span>
          </div>
          <div class="chat-actions">
            <button class="btn-delete" @click="confirmDelete(currentChat)">
              <i class="icon-delete"></i>
              删除对话
            </button>
          </div>
        </div>

        <div class="chat-messages" ref="messageContainer">
          <template v-for="(group, date) in messagesGroupedByDate" :key="date">
            <div class="date-separator">
              <span class="date-text">{{ date }}</span>
            </div>
            <div v-for="message in group" 
                 :key="message.id" 
                 :class="['message', message.senderIdentity]">
              <div class="message-info">
                <span class="sender">{{ message.senderIdentity === 'parent' ? '我' : '专家' }}</span>
                <span class="time">{{ formatMessageTime(message.timestamp) }}</span>
              </div>
              <div :class="['message-content', 
                          message.senderIdentity !== 'parent' ? 'other-message' : '']">
                {{ message.content }}
              </div>
            </div>
          </template>
        </div>

        <div class="chat-input">
          <div class="input-area">
            <textarea 
              v-model="messageInput" 
              placeholder="输入消息内容..."
              @keydown.enter.exact.prevent="sendMessage"
              @keydown.enter.shift.exact="newline"
            ></textarea>
            <div class="input-actions">
              <button class="btn-send" @click="sendMessage">发送</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 确认删除对话框 -->
    <div class="delete-dialog" v-if="showDeleteDialog">
      <div class="delete-dialog-content">
        <h3>确认删除</h3>
        <p>您确定要删除这个对话吗？此操作不可恢复。</p>
        <div class="delete-dialog-actions">
          <button class="btn-cancel" @click="showDeleteDialog = false">取消</button>
          <button class="btn-confirm" @click="handleDelete">确认删除</button>
        </div>
      </div>
    </div>

    <!-- 新建对话框 -->
    <div class="create-chat-dialog" v-if="showCreateDialog">
      <div class="create-dialog-content">
        <h3>新建对话</h3>
        <p>请选择要咨询的专家：</p>
        <div class="expert-list">
          <div v-if="loadingExperts" class="loading-state">
            <p>加载专家列表中...</p>
          </div>
          <div v-else-if="experts.length === 0" class="empty-state">
            <p>暂无可选专家</p>
          </div>
          <div v-else class="expert-items">
            <div
              v-for="expert in experts"
              :key="expert.expertId"
              class="expert-item"
              @click="selectExpert(expert)"
            >
              <div class="expert-name">{{ expert.expertName }}</div>
            </div>
          </div>
        </div>
        <div class="create-dialog-actions">
          <button class="btn-cancel" @click="showCreateDialog = false">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { Client } from '@stomp/stompjs'
import { useUserStore } from '@/stores/user'
import { 
  getParentChats, 
  sendParentMessage, 
  getChatMessages,
  deleteChat,
  createChat,
  getAllExperts
} from '@/api/chat'

const userStore = useUserStore()
const messageInput = ref('')
const currentChat = ref(null)
const messageContainer = ref(null)
let stompClient = null

// 聊天列表
const chats = ref([])
// 当前聊天消息
const messages = ref([])
// WebSocket 连接状态
const wsConnected = ref(false)
// 固定的家长ID
const PARENT_ID = userStore.userInfo?.id || 1

// 添加一个跟踪临时消息ID的映射
const tempMessageIds = ref(new Map());

// 在组件中添加删除对话框相关变量
const showDeleteDialog = ref(false)
const chatToDelete = ref(null)

// 添加loading状态变量
const isLoading = ref(true)

// 新建对话框相关变量
const showCreateDialog = ref(false)
const experts = ref([])
const loadingExperts = ref(false)

// 将消息按日期分组
const messagesGroupedByDate = computed(() => {
  const groups = {};
  
  messages.value.forEach(message => {
    if (!message.timestamp) return;
    
    const date = new Date(message.timestamp);
    const now = new Date();
    
    // 获取日期标签
    let dateLabel;
    
    // 如果是今天
    if (date.toDateString() === now.toDateString()) {
      dateLabel = '今天';
    } 
    // 如果是昨天
    else {
      const yesterday = new Date(now);
      yesterday.setDate(yesterday.getDate() - 1);
      if (date.toDateString() === yesterday.toDateString()) {
        dateLabel = '昨天';
      } 
      // 其他日期
      else {
        const isCurrentYear = date.getFullYear() === now.getFullYear();
        if (isCurrentYear) {
          dateLabel = `${date.getMonth() + 1}月${date.getDate()}日`;
        } else {
          dateLabel = `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`;
        }
      }
    }
    
    // 确保组存在
    if (!groups[dateLabel]) {
      groups[dateLabel] = [];
    }
    
    // 添加消息到对应日期组
    groups[dateLabel].push(message);
  });
  
  // 为每个日期组内的消息按时间排序
  for (const dateLabel in groups) {
    groups[dateLabel].sort((a, b) => {
      return new Date(a.timestamp) - new Date(b.timestamp);
    });
  }
  
  // 创建一个排序后的对象，按日期从早到晚排序
  const sortedGroups = {};
  const today = new Date();
  const yesterday = new Date(today);
  yesterday.setDate(yesterday.getDate() - 1);
  
  // 定义日期标签的优先级
  const dateOrder = {};
  
  // 首先添加非"今天"和"昨天"的日期（从早到晚）
  const dateLabelsByDate = Object.keys(groups)
    .filter(label => label !== '今天' && label !== '昨天')
    .sort((a, b) => {
      // 解析日期字符串并比较
      const dateA = parseCustomDateLabel(a);
      const dateB = parseCustomDateLabel(b);
      return dateA - dateB;
    });
  
  // 添加排序后的日期
  dateLabelsByDate.forEach(label => {
    sortedGroups[label] = groups[label];
  });
  
  // 然后添加"昨天"（如果有）
  if (groups['昨天']) {
    sortedGroups['昨天'] = groups['昨天'];
  }
  
  // 最后添加"今天"（如果有）
  if (groups['今天']) {
    sortedGroups['今天'] = groups['今天'];
  }
  
  return sortedGroups;
});

// 辅助函数：解析自定义日期标签
const parseCustomDateLabel = (label) => {
  // 处理 "X月X日" 格式
  const monthDayMatch = label.match(/(\d+)月(\d+)日/);
  if (monthDayMatch) {
    const currentYear = new Date().getFullYear();
    const month = parseInt(monthDayMatch[1]) - 1; // 月份从0开始
    const day = parseInt(monthDayMatch[2]);
    return new Date(currentYear, month, day);
  }
  
  // 处理 "YYYY年X月X日" 格式
  const yearMonthDayMatch = label.match(/(\d+)年(\d+)月(\d+)日/);
  if (yearMonthDayMatch) {
    const year = parseInt(yearMonthDayMatch[1]);
    const month = parseInt(yearMonthDayMatch[2]) - 1;
    const day = parseInt(yearMonthDayMatch[3]);
    return new Date(year, month, day);
  }
  
  // 默认返回遥远的过去日期（确保无法识别的日期排在最前面）
  return new Date(0);
};

// 专门用于消息气泡中的时间显示（只显示小时和分钟）
const formatMessageTime = (timestamp) => {
  if (!timestamp) return '';
  const date = new Date(timestamp);
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
};

const scrollToBottom = () => {
  setTimeout(() => {
    if (messageContainer.value) {
      messageContainer.value.scrollTop = messageContainer.value.scrollHeight
    }
  }, 0)
}

// 获取聊天列表
const fetchChats = async () => {
  console.log('fetchChats function called');
  isLoading.value = true;
  
  try {
    const response = await getParentChats(PARENT_ID)
    console.log('API response received:', response);

    if (response.code === 200) {
      console.log('Inside if block, code is 200.');
      console.log('Original data array:', response.data);
      
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
        })
      }
      
      // 如果没有聊天记录，自动创建一个与专家的聊天
      if (chats.value.length === 0) {
        console.log('No chats found, creating a new chat');
        createNewChat();
      } else if (currentChat.value === null && chats.value.length > 0) {
        // 如果有聊天记录但没有选中的聊天，自动选中第一个
        selectChat(chats.value[0]);
      }
    }
  } catch (error) {
    console.error('获取聊天列表失败:', error)
  } finally {
    isLoading.value = false;
  }
}

// 获取聊天消息
const fetchMessages = async (chatId) => {
  console.log(`fetchMessages called for chatId: ${chatId}`);
  try {
    console.log(`Calling getChatMessages API for chatId: ${chatId}`);
    const response = await getChatMessages(chatId)
    console.log(`getChatMessages API response for chatId ${chatId}:`, response);

    if (response && response.code === 200) {
      console.log(`Successfully fetched messages for chatId ${chatId}, data:`, response.data);
      
      // 确保每条消息都有时间戳
      const processedMessages = (response.data || []).map(msg => {
        // 确保消息有时间戳，如果没有使用当前时间
        return {
          ...msg,
          timestamp: msg.timestamp || msg.createTimestamp || new Date().toISOString()
        };
      });
      
      messages.value = processedMessages;
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
  console.log('sendMessage called');
  console.log('messageInput:', messageInput.value);
  console.log('currentChat:', currentChat.value);

  if (!messageInput.value.trim() || !currentChat.value) {
    console.log('sendMessage stopped: message input is empty or no chat selected.');
    return;
  }

  console.log('Preparing messageData...');
  try {
    const messageData = {
      chatId: currentChat.value.id,
      senderId: PARENT_ID,
      receiverId: currentChat.value.expertId,
      content: messageInput.value.trim()
    }
    console.log('messageData prepared:', messageData);

    // 生成临时ID用于乐观更新
    const tempId = Date.now();
    
    // 乐观更新：立即在界面上显示发送的消息
    const sentMessage = {
      id: tempId, // 使用临时ID
      chatId: currentChat.value.id,
      senderIdentity: 'parent',
      senderId: PARENT_ID,
      content: messageInput.value.trim(),
      timestamp: new Date().toISOString()
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

    // 清空输入框
    messageInput.value = '';
    
    console.log('Calling sendParentMessage API...');
    const response = await sendParentMessage(messageData)
    console.log('sendParentMessage API response:', response);

    if (response.code !== 200) {
      console.error('Send message API returned non-200 code:', response.code, response.msg);
      // 可以在这里处理发送失败的情况，例如在UI上显示错误提示
    }
  } catch (error) {
    console.error('发送消息失败 (catch block):', error)
    // 可以在这里添加发送失败的处理，比如重新显示消息输入框的内容
  }
}

// 选择聊天
const selectChat = async (chat) => {
  console.log('selectChat called with chat:', chat);
  currentChat.value = chat
  console.log(`currentChat set to id: ${chat.id}, expertName: ${chat.expertName || '专家'}`);
  try {
    await fetchMessages(chat.id)
    console.log(`Finished processing selectChat for id: ${chat.id}`);
  } catch (error) {
    console.error(`Error during selectChat processing for id: ${chat.id}:`, error);
  }
}

// 换行处理
const newline = (e) => {
  e.preventDefault()
  messageInput.value += '\n'
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
      })
    }
    
    // 如果当前有选中的用户，订阅该用户的聊天
    if (currentChat.value) {
      subscribeToChat(currentChat.value.id)
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
  
  // 避免重复订阅
  try {
    const subscription = stompClient.subscribe(
      `/topic/chat/${chatId}`,
      (message) => handleIncomingMessage(message, chatId),
      { id: `parent-sub-${chatId}` }
    )
    console.log(`成功订阅聊天 ${chatId}, subscription ID: ${subscription.id}`)
  } catch (error) {
    console.error(`订阅聊天失败 ${chatId}:`, error)
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
    if (currentChat.value && messageData.chatId === currentChat.value.id) {
      // 检查是否是自己刚刚发送的消息（通过内容匹配）
      // 如果是自己刚刚通过API发送的消息，则跳过添加（因为已经在UI上乐观添加过了）
      const isSentByMe = messageData.senderIdentity === 'parent' && 
                          messageData.senderId === PARENT_ID && 
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
      
      // 检查消息是否已存在（避免重复）
      if (!messages.value.some(m => 
          m.id === messageData.messageId || 
          (m.content === messageData.content && 
           m.senderIdentity === messageData.senderIdentity &&
           m.senderId === messageData.senderId))) {
        messages.value.push({
          id: messageData.messageId,
          chatId: messageData.chatId,
          senderIdentity: messageData.senderIdentity,
          senderId: messageData.senderId,
          content: messageData.content,
          timestamp: messageData.createTimestamp || new Date().toISOString() // 确保有时间戳
        })
        scrollToBottom()
      } else {
        console.log('消息已存在，避免重复:', messageData);
      }
    }
    
    // 更新聊天列表中的最后一条消息
    const chat = chats.value.find(c => c.id === messageData.chatId)
    if (chat) {
      chat.lastMessage = messageData.content
      chat.lastMessageTimestamp = messageData.createTimestamp || new Date().toISOString() // 确保有时间戳
    }
  } catch (error) {
    console.error('处理消息失败:', error)
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
      if (currentChat.value && currentChat.value.id === chatToDelete.value.id) {
        currentChat.value = null
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

// 添加创建新聊天函数
const createNewChat = async () => {
  try {
    const data = {
      parentId: PARENT_ID,
      expertId: 1  // 默认专家ID，可以根据实际情况调整
    }
    
    console.log('Creating new chat with data:', data);
    const response = await createChat(data)
    
    if (response.code === 200) {
      console.log('New chat created successfully:', response.data);
      // 刷新聊天列表
      await fetchChats();
    } else {
      console.error('Failed to create new chat:', response);
    }
  } catch (error) {
    console.error('Create chat error:', error);
  }
}

// 显示创建对话框
const showCreateChatDialog = async () => {
  showCreateDialog.value = true
  await fetchExperts()
}

// 获取专家列表
const fetchExperts = async () => {
  loadingExperts.value = true
  try {
    const response = await getAllExperts()
    if (response.code === 200 && response.data) {
      experts.value = response.data
    } else {
      experts.value = []
    }
  } catch (error) {
    console.error('获取专家列表失败:', error)
    experts.value = []
  } finally {
    loadingExperts.value = false
  }
}

// 选择专家并创建聊天
const selectExpert = async (expert) => {
  try {
    const chatData = {
      parentId: PARENT_ID,
      expertId: expert.expertId
    }
    const response = await createChat(chatData)
    if (response.code === 200 && response.data) {
      showCreateDialog.value = false
      // 刷新聊天列表
      await fetchChats()
      // 选中新创建的聊天
      const newChat = chats.value.find(c => c.id === response.data.id)
      if (newChat) {
        selectChat(newChat)
      }
    } else {
      console.error('创建聊天失败:', response)
    }
  } catch (error) {
    console.error('创建聊天失败:', error)
  }
}
</script>

<style scoped>
.parent-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
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

.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
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
  margin-bottom: 8px;
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
  min-width: 1000px;
}

/* 添加日期分隔符样式 */
.date-separator {
  display: flex;
  justify-content: center;
  margin: 20px 0;
  position: relative;
}

.date-separator:after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  top: 50%;
  height: 1px;
  background-color: #e4e7ed;
  z-index: 1;
}

.date-text {
  background-color: #fff;
  padding: 0 16px;
  font-size: 12px;
  color: #909399;
  position: relative;
  z-index: 2;
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
  border-radius: 8px;
  background-color: #f56c6c;
  color: #fff;
  cursor: pointer;
  transition: background-color 0.3s;
}

.btn-delete:hover {
  background-color: #f78989;
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
  margin-left: auto;
}

.message.expert, .message.ai {
  margin-right: auto;
}

.message-info {
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.message.parent .message-info {
  color: #409eff;
}

.message.expert .message-info, 
.message.ai .message-info, 
.message:not(.parent) .message-info .time, 
.message:not(.parent) .message-info .sender {
  color: #e6a23c !important;
}

.message-content {
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.5;
}

.message.parent .message-content {
  background-color: #ecf5ff;
  color: #333;
}

.message.expert .message-content, .message.ai .message-content, .message-content.other-message {
  background-color: #fdf6ec;
  color: #333;
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
  padding: 12px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.5;
  resize: none;
}

.input-actions {
  display: flex;
  gap: 16px;
}

.btn-send {
  padding: 8px 20px;
  border: none;
  border-radius: 8px;
  background-color: #409eff;
  color: #fff;
  cursor: pointer;
  transition: background-color 0.3s;
}

.btn-send:hover {
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
  border-radius: 8px;
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
  border-radius: 8px;
  background-color: #fff;
  color: #606266;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-confirm {
  padding: 8px 20px;
  border: none;
  border-radius: 8px;
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

/* 空聊天状态样式 */
.empty-chats, .loading-state {
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

.empty-state p, .loading-state p {
  color: #909399;
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
}

.btn-new-chat {
  padding: 8px 20px;
  border: none;
  border-radius: 8px;
  background-color: #409eff;
  color: #fff;
  cursor: pointer;
  transition: background-color 0.3s;
}

.btn-new-chat:hover {
  background-color: #66b1ff;
}

.create-chat-dialog {
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

.create-dialog-content {
  width: 400px;
  background-color: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.create-dialog-content h3 {
  margin-top: 0;
  color: #333;
}

.create-dialog-content p {
  margin-bottom: 24px;
  color: #606266;
}

.expert-list {
  margin-bottom: 24px;
}

.expert-items {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.expert-item {
  padding: 8px 16px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.expert-item:hover {
  background-color: #f5f7fa;
}

.expert-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.create-dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
}

.btn-cancel {
  padding: 8px 20px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  background-color: #fff;
  color: #606266;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-cancel:hover {
  color: #409eff;
  border-color: #c6e2ff;
  background-color: #ecf5ff;
}

.btn-create {
  padding: 8px 20px;
  border: none;
  border-radius: 8px;
  background-color: #409eff;
  color: #fff;
  cursor: pointer;
  transition: background-color 0.3s;
}

.btn-create:hover {
  background-color: #66b1ff;
}
</style>
