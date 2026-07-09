<template>
  <main class="workbench-page">
    <section class="workspace">
      <header class="hero">
        <div>
          <p class="eyebrow">PERSONAL WORKBENCH</p>
          <h1>个人 AICO 工作台</h1>
          <p class="summary">
            PERSONAL 模式把个人用户本人作为长期对齐主体。系统围绕“我是谁、我和谁说话、我想完成什么样的对话”持续更新画像、关系网络、偏好记忆和策略树。
          </p>
        </div>
        <button class="secondary" @click="go('/personal')">返回入口</button>
      </header>

      <section class="layout">
        <article class="panel conversation-panel">
          <div class="panel-head">
            <div>
              <p class="section-label">Conversation Space</p>
              <h2>我的对话空间</h2>
            </div>
            <span class="status">PERSONAL</span>
          </div>
          <div class="field-grid">
            <label>
              <span>对话对象</span>
              <select v-model="draft.partnerType">
                <option value="self">自己</option>
                <option value="friend">朋友</option>
                <option value="family">家人</option>
                <option value="colleague">同事</option>
                <option value="other">其他人</option>
              </select>
            </label>
            <label>
              <span>关系状态</span>
              <select v-model="draft.relationshipState">
                <option value="new">刚接触</option>
                <option value="familiar">熟悉</option>
                <option value="close">亲密</option>
                <option value="distant">久未联系</option>
                <option value="tense">关系紧张</option>
              </select>
            </label>
          </div>
          <textarea
            v-model="draft.message"
            placeholder="写下你想组织的对话、收到的消息，或你准备发送给某个人的话。"
          />
          <div class="actions">
            <button @click="addCandidate">生成候选策略</button>
            <button class="secondary" @click="go('/aico-alignment')">查看长期对齐</button>
          </div>
        </article>

        <aside class="panel memory-panel">
          <p class="section-label">Personal Memory</p>
          <h2>画像与关系</h2>
          <ul class="memory-list">
            <li>
              <span>个人画像</span>
              <strong>表达偏好、边界、长期目标</strong>
            </li>
            <li>
              <span>关系画像</span>
              <strong>对话对象、亲密度、历史互动</strong>
            </li>
            <li>
              <span>偏好记忆</span>
              <strong>措辞习惯、节奏、风险偏好</strong>
            </li>
            <li>
              <span>个人 RAG</span>
              <strong>历史聊天、确认策略、关系事件</strong>
            </li>
          </ul>
        </aside>
      </section>

      <section class="panel">
        <div class="panel-head">
          <div>
            <p class="section-label">Topic Graph & Strategy Tree</p>
            <h2>候选 topic 与策略树确认</h2>
          </div>
          <button class="secondary" @click="go('/decision-tree')">打开树编辑器</button>
        </div>

        <div class="candidate-grid">
          <article v-for="item in candidates" :key="item.id" class="candidate-card">
            <div class="candidate-head">
              <h3>{{ item.topic }}</h3>
              <span :class="['badge', item.status]">{{ statusText[item.status] }}</span>
            </div>
            <p>{{ item.description }}</p>
            <div class="tree-path">
              <span v-for="node in item.path" :key="node">{{ node }}</span>
            </div>
            <div class="actions">
              <button @click="markUserConfirmed(item.id)">用户确认</button>
              <button class="secondary" @click="markAiConfirmed(item.id)">AI 确认</button>
              <button class="ghost" @click="rejectCandidate(item.id)">拒绝</button>
            </div>
          </article>
        </div>
      </section>
    </section>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const draft = reactive({
  partnerType: 'friend',
  relationshipState: 'familiar',
  message: ''
})

const statusText = {
  candidate: '待确认',
  ai_confirmed: 'AI 确认',
  user_confirmed: '用户确认',
  rejected: '已拒绝'
}

const candidates = ref([
  {
    id: 1,
    topic: '久未联系后的关系恢复',
    status: 'candidate',
    description: '先判断关系温度和对方可接受度，再选择轻量寒暄、共同记忆或明确目的。',
    path: ['识别关系阶段', '选择开场策略', '观察反馈', '推进或降级']
  },
  {
    id: 2,
    topic: '带有目的的请求型对话',
    status: 'ai_confirmed',
    description: '把目的拆成铺垫、关系校准、试探、正式提出和退路维护，避免突兀推进。',
    path: ['确认目的强度', '铺垫关系', '小幅试探', '提出请求', '维护关系']
  }
])

const go = (path) => {
  router.push(path)
}

const updateStatus = (id, status) => {
  const item = candidates.value.find((candidate) => candidate.id === id)
  if (item) {
    item.status = status
  }
}

const markUserConfirmed = (id) => updateStatus(id, 'user_confirmed')
const markAiConfirmed = (id) => updateStatus(id, 'ai_confirmed')
const rejectCandidate = (id) => updateStatus(id, 'rejected')

const addCandidate = () => {
  const topic = draft.message.trim() ? '基于当前输入的动态 topic' : '新的日常对话 topic'
  candidates.value.unshift({
    id: Date.now(),
    topic,
    status: 'candidate',
    description: `对象类型：${draft.partnerType}；关系状态：${draft.relationshipState}。等待后端 LLM topic extractor 与 embedding 合并逻辑接入。`,
    path: ['抽取 topic', '匹配关系状态', '检索历史树', '生成候选节点']
  })
}
</script>

<style scoped>
.workbench-page {
  min-height: 100vh;
  background: #f4f6f8;
  color: #17202a;
}

.workspace {
  width: min(1180px, calc(100vw - 40px));
  margin: 0 auto;
  padding: 32px 0;
}

.hero,
.panel,
.candidate-card {
  background: #ffffff;
  border: 1px solid #dbe1e7;
  border-radius: 8px;
}

.hero {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding: 28px;
  margin-bottom: 18px;
}

.eyebrow,
.section-label {
  margin: 0 0 8px;
  color: #206bc4;
  font-size: 13px;
  font-weight: 700;
}

h1,
h2,
h3,
p {
  margin-top: 0;
}

h1 {
  margin-bottom: 10px;
  font-size: 30px;
}

h2 {
  margin-bottom: 12px;
}

.summary {
  max-width: 820px;
  margin-bottom: 0;
  color: #5c6670;
  line-height: 1.7;
}

.layout {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(280px, 0.8fr);
  gap: 16px;
  margin-bottom: 16px;
}

.panel {
  padding: 20px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 14px;
}

.status,
.badge {
  border-radius: 999px;
  padding: 5px 10px;
  font-size: 12px;
  font-weight: 700;
  background: #eef3f8;
  color: #1f3349;
  white-space: nowrap;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

label span {
  display: block;
  margin-bottom: 6px;
  color: #5c6670;
  font-size: 13px;
}

select,
textarea {
  width: 100%;
  border: 1px solid #cfd7df;
  border-radius: 6px;
  background: #ffffff;
  color: #17202a;
  font: inherit;
}

select {
  height: 40px;
  padding: 0 10px;
}

textarea {
  min-height: 150px;
  resize: vertical;
  padding: 12px;
  box-sizing: border-box;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

button {
  border: 0;
  border-radius: 6px;
  padding: 10px 16px;
  background: #206bc4;
  color: #ffffff;
  font-weight: 600;
  cursor: pointer;
}

button.secondary {
  background: #eef3f8;
  color: #1f3349;
}

button.ghost {
  background: transparent;
  color: #8a2432;
  border: 1px solid #efccd2;
}

.memory-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 12px;
}

.memory-list li {
  border-bottom: 1px solid #edf1f5;
  padding-bottom: 12px;
}

.memory-list span {
  display: block;
  color: #5c6670;
  font-size: 13px;
}

.memory-list strong {
  display: block;
  margin-top: 4px;
  font-size: 14px;
}

.candidate-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.candidate-card {
  padding: 16px;
}

.candidate-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.candidate-card p {
  color: #5c6670;
  line-height: 1.7;
}

.badge.candidate {
  background: #fff5d6;
  color: #7a5100;
}

.badge.ai_confirmed {
  background: #e7f0ff;
  color: #174d93;
}

.badge.user_confirmed {
  background: #e6f7ed;
  color: #1f6b3a;
}

.badge.rejected {
  background: #f9e3e7;
  color: #8a2432;
}

.tree-path {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tree-path span {
  border-radius: 999px;
  padding: 6px 10px;
  background: #f3f6f9;
  color: #3d4a57;
  font-size: 12px;
}

@media (max-width: 900px) {
  .hero,
  .panel-head {
    flex-direction: column;
  }

  .layout,
  .field-grid,
  .candidate-grid {
    grid-template-columns: 1fr;
  }
}
</style>
