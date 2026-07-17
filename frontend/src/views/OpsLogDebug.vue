<template>
  <main class="debug">
    <header class="bar">
      <div class="left">
        <button type="button" class="ghost" @click="$router.push('/ops/monitor')">← 监控看板</button>
        <strong class="logo">AICO</strong>
        <span class="badge">LOG DEBUG</span>
      </div>
      <div class="right">
        <button type="button" class="ghost" @click="$router.push('/login')">登录页</button>
      </div>
    </header>

    <section class="shell">
      <div class="hero">
        <h1>日志查询 Debug</h1>
        <p class="sub">
          同一请求链路上的所有日志共享同一个 TraceId。输入 TraceId 可还原登录、业务处理、算法调用到返回的完整链路。
        </p>
      </div>

      <form class="query-box" @submit.prevent="queryTrace">
        <label>
          TraceId
          <input v-model="traceId" placeholder="例如从监控看板日志中复制" />
        </label>
        <label>
          关键词
          <input v-model="keyword" placeholder="可选：logger / message 关键词" />
        </label>
        <label>
          级别
          <select v-model="level">
            <option value="">全部</option>
            <option value="ERROR">ERROR</option>
            <option value="WARN">WARN</option>
            <option value="INFO">INFO</option>
            <option value="DEBUG">DEBUG</option>
          </select>
        </label>
        <div class="actions">
          <button type="submit" class="primary" :disabled="loading">
            {{ loading ? '查询中…' : '按 TraceId 查询' }}
          </button>
          <button type="button" class="secondary" :disabled="loading" @click="searchKeyword">关键词搜索</button>
          <button type="button" class="secondary" @click="useLastTrace">填入最近一次请求</button>
        </div>
      </form>

      <div class="layout">
        <aside class="side">
          <div class="panel-head">
            <h2>最近 Trace</h2>
            <button type="button" class="mini" @click="loadRecent">刷新</button>
          </div>
          <ul class="trace-list">
            <li
              v-for="item in recentTraces"
              :key="item.traceId"
              :class="{ active: item.traceId === activeTraceId, err: item.hasError }"
              @click="openTrace(item.traceId)"
            >
              <code>{{ item.traceId }}</code>
              <span>{{ item.lineCount }} 行 · {{ item.endTime }}</span>
              <p>{{ item.preview }}</p>
            </li>
            <li v-if="!recentTraces.length" class="empty">暂无 Trace 记录，先发起一次业务请求</li>
          </ul>
        </aside>

        <section class="main-panel">
          <div class="panel-head">
            <h2>
              链路详情
              <template v-if="activeTraceId"> · <code>{{ activeTraceId }}</code></template>
            </h2>
            <span v-if="detail.count != null">共 {{ detail.count }} 条</span>
          </div>

          <div v-if="detail.levels" class="level-chips">
            <span v-for="(n, lv) in detail.levels" :key="lv" class="chip" :class="lv.toLowerCase()">
              {{ lv }} {{ n }}
            </span>
          </div>

          <div v-if="!lines.length" class="empty-box">
            {{ loading ? '加载中…' : '选择左侧 Trace，或输入 TraceId 查询' }}
          </div>

          <ol v-else class="timeline">
            <li v-for="(line, idx) in lines" :key="idx" :class="line.level.toLowerCase()">
              <div class="meta">
                <span class="time">{{ line.time }}</span>
                <span class="level">{{ line.level }}</span>
                <span class="logger">{{ shortLogger(line.logger) }}</span>
                <span class="thread">{{ line.thread }}</span>
              </div>
              <pre class="msg">{{ line.message }}</pre>
              <pre v-if="line.throwable" class="throw">{{ line.throwable }}</pre>
            </li>
          </ol>
        </section>
      </div>
    </section>
  </main>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getRecentTraces, getTraceLogs, searchDebugLogs } from '@/api/ops'

const route = useRoute()
const router = useRouter()

const traceId = ref('')
const keyword = ref('')
const level = ref('')
const loading = ref(false)
const recentTraces = ref([])
const activeTraceId = ref('')
const detail = reactive({ count: null, levels: null, found: false })
const lines = ref([])

const shortLogger = (name = '') => {
  const parts = String(name).split('.')
  return parts.slice(-2).join('.')
}

const applyDetail = (data) => {
  detail.count = data.count ?? (data.lines?.length || 0)
  detail.levels = data.levels || null
  detail.found = !!data.found || !!(data.lines && data.lines.length)
  lines.value = data.lines || []
  activeTraceId.value = data.traceId || traceId.value
}

const loadRecent = async () => {
  try {
    const res = await getRecentTraces(50)
    recentTraces.value = res.data || []
  } catch (error) {
    ElMessage.error(typeof error === 'string' ? error : '最近 Trace 加载失败')
  }
}

const openTrace = async (tid) => {
  traceId.value = tid
  await queryTrace()
}

const queryTrace = async () => {
  if (!traceId.value.trim()) {
    ElMessage.warning('请输入 TraceId')
    return
  }
  try {
    loading.value = true
    const tid = traceId.value.trim()
    router.replace({ query: { ...route.query, traceId: tid } })
    const res = await getTraceLogs(tid)
    applyDetail(res.data || {})
    if (!res.data?.found) {
      ElMessage.warning('未找到该 TraceId 的日志，可能已过期或尚未产生')
    }
    await loadRecent()
  } catch (error) {
    ElMessage.error(typeof error === 'string' ? error : '查询失败')
  } finally {
    loading.value = false
  }
}

const searchKeyword = async () => {
  try {
    loading.value = true
    const res = await searchDebugLogs({
      q: keyword.value.trim() || undefined,
      level: level.value || undefined,
      limit: 200
    })
    applyDetail({
      count: res.data?.count,
      levels: null,
      found: true,
      lines: res.data?.lines || [],
      traceId: ''
    })
    activeTraceId.value = ''
    if (res.data?.recentTraces) {
      recentTraces.value = res.data.recentTraces
    }
  } catch (error) {
    ElMessage.error(typeof error === 'string' ? error : '搜索失败')
  } finally {
    loading.value = false
  }
}

const useLastTrace = () => {
  try {
    const last = sessionStorage.getItem('aico_last_trace_id')
    if (!last) {
      ElMessage.info('暂无最近请求 TraceId，请先在前端触发一次接口调用')
      return
    }
    traceId.value = last
  } catch (_) {
    ElMessage.warning('无法读取本地 TraceId')
  }
}

onMounted(async () => {
  await loadRecent()
  if (route.query.traceId) {
    traceId.value = String(route.query.traceId)
    await queryTrace()
  } else {
    useLastTrace()
  }
})
</script>

<style scoped>
.debug {
  min-height: 100vh;
  background: linear-gradient(180deg, #f4f8f6, #e8efec);
  color: var(--aico-ink);
  font-family: var(--font-body);
}

.bar,
.shell {
  width: min(1200px, calc(100% - 32px));
  margin: 0 auto;
}

.bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 22px 0 0;
}

.left,
.right,
.actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.logo {
  letter-spacing: 0.04em;
}

.badge {
  border-radius: 999px;
  padding: 4px 10px;
  background: rgba(15, 92, 86, 0.1);
  color: var(--aico-teal-deep);
  font-size: 12px;
  font-weight: 700;
}

.ghost,
.primary,
.secondary,
.mini {
  border: 0;
  border-radius: 999px;
  font: inherit;
  cursor: pointer;
}

.ghost,
.mini {
  background: transparent;
  color: var(--aico-muted);
}

.primary {
  padding: 10px 16px;
  background: var(--aico-teal);
  color: #fff;
  font-weight: 700;
}

.secondary {
  padding: 10px 16px;
  background: #fff;
  border: 1px solid rgba(20, 32, 30, 0.12);
}

.shell {
  padding: 24px 0 56px;
}

h1 {
  margin: 0 0 8px;
  font-size: 30px;
}

.sub {
  margin: 0 0 20px;
  color: var(--aico-muted);
  line-height: 1.7;
}

.query-box {
  display: grid;
  grid-template-columns: 1.4fr 1fr 0.6fr;
  gap: 12px;
  padding: 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(20, 32, 30, 0.08);
  margin-bottom: 14px;
}

.query-box label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  font-weight: 700;
}

.query-box input,
.query-box select {
  height: 40px;
  border: 1px solid rgba(20, 32, 30, 0.14);
  border-radius: 10px;
  padding: 0 12px;
  font: inherit;
  font-weight: 400;
}

.query-box .actions {
  grid-column: 1 / -1;
}

.layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 12px;
}

.side,
.main-panel {
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(20, 32, 30, 0.08);
  padding: 14px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.panel-head h2 {
  margin: 0;
  font-size: 16px;
}

.trace-list {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 68vh;
  overflow: auto;
}

.trace-list li {
  padding: 10px;
  border-radius: 12px;
  border: 1px solid transparent;
  cursor: pointer;
  margin-bottom: 8px;
  background: #f7faf8;
}

.trace-list li:hover,
.trace-list li.active {
  border-color: rgba(15, 92, 86, 0.28);
  background: #eef6f3;
}

.trace-list li.err {
  border-color: rgba(232, 93, 60, 0.35);
}

.trace-list code {
  display: block;
  font-size: 12px;
}

.trace-list span,
.trace-list p,
.empty,
.empty-box {
  color: var(--aico-muted);
  font-size: 12px;
}

.trace-list p {
  margin: 4px 0 0;
  line-height: 1.4;
}

.level-chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.chip {
  border-radius: 999px;
  padding: 2px 10px;
  font-size: 12px;
  font-weight: 700;
  background: #eef3f1;
}

.chip.error { background: rgba(160, 20, 20, 0.12); color: #8a1212; }
.chip.warn { background: rgba(232, 93, 60, 0.14); color: #9b3a24; }
.chip.info { background: rgba(15, 92, 86, 0.12); color: var(--aico-teal-deep); }

.timeline {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 68vh;
  overflow: auto;
}

.timeline li {
  padding: 12px 0;
  border-bottom: 1px solid rgba(20, 32, 30, 0.06);
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 12px;
  color: var(--aico-muted);
}

.level {
  font-weight: 700;
}

.timeline li.error .level { color: #8a1212; }
.timeline li.warn .level { color: #9b3a24; }
.timeline li.info .level { color: var(--aico-teal-deep); }

.msg,
.throw {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
  line-height: 1.5;
}

.throw {
  margin-top: 8px;
  padding: 8px;
  border-radius: 8px;
  background: rgba(160, 20, 20, 0.06);
  color: #8a1212;
}

@media (max-width: 900px) {
  .query-box,
  .layout {
    grid-template-columns: 1fr;
  }
}
</style>
