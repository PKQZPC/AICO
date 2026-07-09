<template>
  <main class="alignment-page">
    <section class="workspace">
      <header class="header">
        <div>
          <h1>AICO 对齐工作台</h1>
          <p>长期个人画像、关系画像与专家反馈闭环</p>
        </div>
        <button class="ghost" @click="loadStates">刷新状态</button>
      </header>

      <section class="panel">
        <div class="grid">
          <label>
            用户 ID
            <input v-model="form.userId" placeholder="user_001" />
          </label>
          <label>
            对话对象 ID
            <input v-model="form.counterpartId" placeholder="expert_001 / client_002" />
          </label>
          <label>
            当前角色
            <select v-model="form.role">
              <option value="client">client</option>
              <option value="expert">expert</option>
              <option value="peer">peer</option>
              <option value="self">self</option>
            </select>
          </label>
          <label>
            AICO 模式
            <select v-model="form.mode">
              <option value="personal">PERSONAL：对齐自己</option>
              <option value="expert">EXPERT：对齐专家</option>
            </select>
          </label>
          <label>
            对齐主体 ID
            <input v-model="form.alignedSubjectId" placeholder="personal 用 userId；expert 用 expert_001" />
          </label>
        </div>

        <label class="message">
          本轮交互内容
          <textarea v-model="form.message" rows="5" placeholder="输入用户与专家/他人交互中的关键内容"></textarea>
        </label>

        <div class="actions">
          <button @click="submitTurn">写入本轮对齐</button>
          <button class="ghost" @click="submitFeedback">写入反馈</button>
        </div>
      </section>

      <section class="columns">
        <article class="state">
          <h2>{{ form.mode === 'expert' ? '专家对齐状态' : '个人对齐状态' }}</h2>
          <pre>{{ personalText }}</pre>
        </article>
        <article class="state">
          <h2>{{ form.mode === 'expert' ? 'Client 服务画像' : '关系对齐状态' }}</h2>
          <pre>{{ relationshipText }}</pre>
        </article>
      </section>

      <section class="columns">
        <article class="state">
          <h2>Topic Graph</h2>
          <pre>{{ topicText }}</pre>
        </article>
        <article class="state">
          <h2>Strategy Tree Execution</h2>
          <pre>{{ treeText }}</pre>
        </article>
      </section>

      <section class="columns">
        <article class="state">
          <h2>关系网络图谱</h2>
          <div class="graph-box">
            <div class="graph-node">{{ form.userId || 'user' }}</div>
            <div class="graph-edge">{{ graphEdgeText }}</div>
            <div class="graph-node secondary">{{ form.counterpartId || 'counterpart' }}</div>
          </div>
          <pre>{{ relationshipGraphText }}</pre>
        </article>
        <article class="state">
          <h2>专家画像 / Client 服务画像</h2>
          <pre>{{ expertClientText }}</pre>
        </article>
      </section>

      <section class="panel">
        <h2>确认与反馈</h2>
        <div class="grid">
          <label>
            用户评分
            <input v-model.number="feedback.userScore" type="number" min="0" max="1" step="0.05" />
          </label>
          <label>
            对方反馈
            <input v-model.number="feedback.partnerSignal" type="number" min="0" max="1" step="0.05" />
          </label>
          <label>
            系统观察
            <input v-model.number="feedback.systemScore" type="number" min="0" max="1" step="0.05" />
          </label>
          <label>
            LLM 评估
            <input v-model.number="feedback.llmScore" type="number" min="0" max="1" step="0.05" />
          </label>
        </div>
        <p class="hint">
          PERSONAL 使用用户、对方、系统观察和 LLM 的多源反馈；EXPERT 只允许专家反馈修改和确认专家树节点。
        </p>
      </section>

      <section class="state">
        <h2>生成时可注入的 Alignment Context</h2>
        <pre>{{ contextText }}</pre>
      </section>
    </section>
  </main>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import {
  createAlignmentTurn,
  getPersonalAlignment,
  getRelationshipAlignment,
  submitAlignmentFeedback
} from '@/api/alignment'

const form = reactive({
  userId: 'user_001',
  counterpartId: 'expert_001',
  role: 'client',
  mode: 'personal',
  alignedSubjectId: '',
  message: ''
})

const feedback = reactive({
  userScore: 0.8,
  partnerSignal: 0.7,
  systemScore: 0.75,
  llmScore: 0.75
})

const personal = ref(null)
const relationship = ref(null)
const alignmentContext = ref('')
const topicGraph = ref(null)
const strategyTree = ref(null)
const strategyTreeExecution = ref(null)
const expertProfile = ref(null)
const clientServiceProfile = ref(null)
const relationshipGraph = ref(null)

const personalText = computed(() => JSON.stringify(personal.value, null, 2))
const relationshipText = computed(() => {
  if (form.mode === 'expert') {
    return JSON.stringify(clientServiceProfile.value, null, 2)
  }
  return JSON.stringify(relationship.value, null, 2)
})
const contextText = computed(() => alignmentContext.value || '暂无上下文')
const topicText = computed(() => JSON.stringify(topicGraph.value, null, 2))
const treeText = computed(() => JSON.stringify({
  strategyTree: strategyTree.value,
  execution: strategyTreeExecution.value
}, null, 2))
const expertClientText = computed(() => JSON.stringify({
  expertProfile: expertProfile.value,
  clientServiceProfile: clientServiceProfile.value
}, null, 2))
const relationshipGraphText = computed(() => JSON.stringify(relationshipGraph.value, null, 2))
const graphEdgeText = computed(() => {
  const graph = relationshipGraph.value || {}
  const dimensions = graph.dimensions || {}
  if (form.mode === 'expert') return 'case context'
  return `trust ${dimensions.trust ?? '-'} / tension ${dimensions.tension ?? '-'}`
})

const submitTurn = async () => {
  const response = await createAlignmentTurn({
    userId: form.userId,
    counterpartId: form.counterpartId,
    role: form.role,
    mode: form.mode,
    alignedSubjectId: form.alignedSubjectId || undefined,
    alignedSubjectType: form.mode === 'expert' ? 'expert' : 'self',
    interactionPartnerId: form.mode === 'expert' ? form.userId : form.counterpartId,
    interactionPartnerType: form.mode === 'expert' ? 'client' : 'peer',
    message: form.message,
    profile: {
      entry: 'aico-alignment-workbench'
    }
  })
  personal.value = response.data.personalAlignment
  relationship.value = response.data.relationshipAlignment
  alignmentContext.value = response.data.alignmentContext
  topicGraph.value = response.data.topicGraph
  strategyTree.value = response.data.strategyTree
  strategyTreeExecution.value = response.data.strategyTreeExecution
  expertProfile.value = response.data.expertProfile
  clientServiceProfile.value = response.data.clientServiceProfile || response.data.clientContextProfile
  relationshipGraph.value = response.data.relationshipGraph
}

const loadStates = async () => {
  const subjectId = form.mode === 'expert' ? (form.alignedSubjectId || 'expert_001') : form.userId
  const [personalResponse, relationshipResponse] = await Promise.all([
    getPersonalAlignment(subjectId),
    getRelationshipAlignment(form.userId, form.counterpartId)
  ])
  personal.value = personalResponse.data
  relationship.value = relationshipResponse.data
  relationshipGraph.value = relationshipResponse.data
}

const submitFeedback = async () => {
  await submitAlignmentFeedback({
    userId: form.userId,
    counterpartId: form.counterpartId,
    mode: form.mode,
    source: form.mode === 'expert' ? 'expert' : form.role,
    sourceId: form.mode === 'expert' ? (form.alignedSubjectId || 'expert_001') : form.userId,
    feedbackType: form.mode === 'expert' ? 'expert_tree_confirmation' : 'preference',
    content: form.message || '用户希望 AICO 后续回复更贴合长期偏好与关系背景',
    userScore: form.mode === 'personal' ? feedback.userScore : undefined,
    partnerSignal: form.mode === 'personal' ? feedback.partnerSignal : undefined,
    systemScore: form.mode === 'personal' ? feedback.systemScore : undefined,
    llmScore: form.mode === 'personal' ? feedback.llmScore : undefined
  })
  await loadStates()
}
</script>

<style scoped>
.alignment-page {
  min-height: 100vh;
  background: #f4f6f8;
  color: #17202a;
}

.workspace {
  width: min(1180px, calc(100vw - 40px));
  margin: 0 auto;
  padding: 32px 0;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 20px;
}

.header h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
}

.header p {
  margin: 6px 0 0;
  color: #5c6670;
}

.panel,
.state {
  background: #ffffff;
  border: 1px solid #dbe1e7;
  border-radius: 8px;
  padding: 18px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 14px;
  color: #3d4852;
}

input,
select,
textarea {
  border: 1px solid #ccd5df;
  border-radius: 6px;
  padding: 10px 12px;
  font: inherit;
  background: #ffffff;
  color: #17202a;
}

textarea {
  resize: vertical;
}

.message {
  margin-top: 14px;
}

.actions {
  display: flex;
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

button.ghost {
  background: #eef3f8;
  color: #1f3349;
}

.columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin: 16px 0;
}

.state h2 {
  margin: 0 0 12px;
  font-size: 18px;
}

.panel h2 {
  margin: 0 0 14px;
  font-size: 18px;
}

.hint {
  margin: 12px 0 0;
  color: #5c6670;
  line-height: 1.6;
}

.graph-box {
  display: grid;
  grid-template-columns: minmax(100px, 1fr) minmax(120px, 1.2fr) minmax(100px, 1fr);
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.graph-node,
.graph-edge {
  border-radius: 8px;
  padding: 12px;
  text-align: center;
  background: #e8f1ff;
  color: #174d93;
  font-weight: 700;
}

.graph-node.secondary {
  background: #e8f7ee;
  color: #21643b;
}

.graph-edge {
  background: #f5f7fa;
  color: #3d4852;
}

pre {
  min-height: 140px;
  margin: 0;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  color: #24313f;
}

@media (max-width: 820px) {
  .header,
  .columns {
    grid-template-columns: 1fr;
    display: grid;
  }

  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
