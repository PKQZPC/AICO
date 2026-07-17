<template>
  <main class="ops">
    <header class="bar">
      <div class="left">
        <button type="button" class="ghost" @click="$router.push('/login')">← 登录页</button>
        <strong class="logo">AICO</strong>
        <span class="badge">OPS MONITOR</span>
      </div>
      <div class="right">
        <button type="button" class="ghost" @click="$router.push('/ops/log-debug')">日志 Debug</button>
        <label class="auto">
          <input v-model="autoRefresh" type="checkbox" />
          自动刷新
        </label>
        <button type="button" class="refresh" :disabled="loading" @click="load">
          {{ loading ? '刷新中…' : '立即刷新' }}
        </button>
      </div>
    </header>

    <section class="shell" v-if="summary">
      <div class="hero">
        <p class="brand">AICO</p>
        <h1>运行监控与请求日志</h1>
        <p class="sub">
          覆盖登录、会话、消息、对齐、策略树与专家 AI 关键链路。数据为进程内聚合，适合联调与单节点上线初期。
        </p>
      </div>

      <div class="kpi-grid">
        <article class="kpi">
          <span>总请求</span>
          <strong>{{ summary.totalRequests }}</strong>
        </article>
        <article class="kpi">
          <span>错误数</span>
          <strong class="warn">{{ summary.totalErrors }}</strong>
        </article>
        <article class="kpi">
          <span>错误率</span>
          <strong>{{ formatRate(summary.errorRate) }}</strong>
        </article>
        <article class="kpi">
          <span>平均延迟</span>
          <strong>{{ summary.avgLatencyMs }} ms</strong>
        </article>
        <article class="kpi">
          <span>近似 QPS</span>
          <strong>{{ summary.qpsApprox }}</strong>
        </article>
        <article class="kpi">
          <span>运行时长</span>
          <strong>{{ formatUptime(summary.uptimeSeconds) }}</strong>
        </article>
      </div>

      <div class="panels">
        <section class="panel chart-panel">
          <div class="panel-head">
            <h2>近 {{ timeseries.length }} 分钟请求量</h2>
          </div>
          <div class="chart">
            <svg viewBox="0 0 640 180" preserveAspectRatio="none">
              <polyline
                fill="none"
                stroke="#0f5c56"
                stroke-width="3"
                :points="chartPoints"
              />
              <polygon
                :points="chartArea"
                fill="rgba(15,92,86,0.12)"
              />
            </svg>
            <div class="chart-meta">
              <span>峰值 {{ chartMax }}</span>
              <span>最新 {{ latestRequests }}</span>
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="panel-head"><h2>业务打点</h2></div>
          <ul class="event-list">
            <li v-for="(count, name) in summary.businessEvents" :key="name">
              <code>{{ name }}</code>
              <strong>{{ count }}</strong>
            </li>
            <li v-if="!Object.keys(summary.businessEvents || {}).length" class="empty">暂无业务事件</li>
          </ul>
        </section>
      </div>

      <div class="panels">
        <section class="panel wide">
          <div class="panel-head"><h2>Top 接口</h2></div>
          <table>
            <thead>
              <tr>
                <th>接口</th>
                <th>次数</th>
                <th>错误</th>
                <th>平均延迟</th>
                <th>最近状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in summary.topEndpoints || []" :key="item.endpoint">
                <td><code>{{ item.endpoint }}</code></td>
                <td>{{ item.count }}</td>
                <td>{{ item.errors }}</td>
                <td>{{ item.avgLatencyMs }} ms</td>
                <td>
                  <span class="status" :class="statusClass(item.lastStatus)">{{ item.lastStatus }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </section>
      </div>

      <section class="panel wide">
        <div class="panel-head">
          <h2>请求日志</h2>
          <input v-model="query" class="search" placeholder="过滤 path / method" @keyup.enter="load" />
        </div>
        <div class="log-table-wrap">
          <table>
            <thead>
              <tr>
                <th>时间</th>
                <th>方法</th>
                <th>路径</th>
                <th>状态</th>
                <th>耗时</th>
                <th>Trace</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, idx) in logs" :key="idx">
                <td>{{ row.time }}</td>
                <td>{{ row.method }}</td>
                <td><code>{{ row.rawPath || row.path }}</code></td>
                <td><span class="status" :class="statusClass(row.status)">{{ row.status }}</span></td>
                <td>{{ row.latencyMs }} ms</td>
                <td><code class="trace-link" @click="$router.push({ path: '/ops/log-debug', query: { traceId: row.traceId } })">{{ row.traceId }}</code></td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </section>

    <section v-else class="shell">
      <p class="loading">正在加载监控数据…</p>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getOpsDashboard } from '@/api/ops'

const summary = ref(null)
const timeseries = ref([])
const logs = ref([])
const loading = ref(false)
const autoRefresh = ref(true)
const query = ref('')
let timer = null

const chartMax = computed(() => {
  const values = timeseries.value.map((p) => p.requests || 0)
  return Math.max(1, ...values)
})

const latestRequests = computed(() => {
  if (!timeseries.value.length) return 0
  return timeseries.value[timeseries.value.length - 1].requests || 0
})

const chartPoints = computed(() => {
  const data = timeseries.value
  if (!data.length) return ''
  const max = chartMax.value
  const w = 640
  const h = 180
  return data
    .map((p, i) => {
      const x = data.length === 1 ? 0 : (i / (data.length - 1)) * w
      const y = h - ((p.requests || 0) / max) * (h - 16) - 8
      return `${x},${y}`
    })
    .join(' ')
})

const chartArea = computed(() => {
  if (!chartPoints.value) return ''
  return `0,180 ${chartPoints.value} 640,180`
})

const formatRate = (rate) => `${((rate || 0) * 100).toFixed(2)}%`

const formatUptime = (seconds = 0) => {
  const s = Number(seconds) || 0
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  return `${h}h ${m}m ${sec}s`
}

const statusClass = (status) => {
  if (status >= 500) return 's5'
  if (status >= 400) return 's4'
  if (status >= 200) return 's2'
  return ''
}

const load = async () => {
  try {
    loading.value = true
    const res = await getOpsDashboard({ points: 30, logLimit: 120 })
    summary.value = res.data.summary
    timeseries.value = res.data.timeseries || []
    let rows = res.data.logs || []
    if (query.value.trim()) {
      const q = query.value.trim().toLowerCase()
      rows = rows.filter((r) =>
        String(r.path || '').toLowerCase().includes(q)
        || String(r.rawPath || '').toLowerCase().includes(q)
        || String(r.method || '').toLowerCase().includes(q)
      )
    }
    logs.value = rows
  } catch (error) {
    ElMessage.error(typeof error === 'string' ? error : '监控数据加载失败')
  } finally {
    loading.value = false
  }
}

const setupTimer = () => {
  if (timer) clearInterval(timer)
  if (autoRefresh.value) {
    timer = setInterval(load, 5000)
  }
}

watch(autoRefresh, setupTimer)

onMounted(() => {
  load()
  setupTimer()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.ops {
  min-height: 100vh;
  background:
    radial-gradient(900px 420px at 10% -10%, rgba(15, 92, 86, 0.12), transparent 55%),
    linear-gradient(180deg, #f4f8f6, #e8efec);
  color: var(--aico-ink);
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
  gap: 16px;
  padding: 22px 0 0;
}

.left,
.right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  font-family: var(--font-display);
  letter-spacing: 0.04em;
}

.badge {
  border-radius: 999px;
  padding: 4px 10px;
  background: rgba(15, 92, 86, 0.1);
  color: var(--aico-teal-deep);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.ghost,
.refresh {
  border: 0;
  border-radius: 999px;
  font: inherit;
  cursor: pointer;
}

.ghost {
  background: transparent;
  color: var(--aico-muted);
}

.trace-link {
  color: var(--aico-teal);
  cursor: pointer;
  text-decoration: underline;
}

.refresh {
  padding: 8px 14px;
  background: var(--aico-teal);
  color: #fff;
  font-weight: 700;
}

.auto {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--aico-muted);
  font-size: 14px;
}

.shell {
  padding: 28px 0 56px;
}

.brand {
  margin: 0;
  font-family: var(--font-display);
  font-size: 56px;
  font-weight: 800;
  letter-spacing: -0.04em;
  color: var(--aico-teal-deep);
  line-height: 0.95;
}

h1 {
  margin: 8px 0 10px;
  font-family: var(--font-display);
  font-size: 32px;
  letter-spacing: -0.03em;
}

.sub,
.loading {
  color: var(--aico-muted);
  line-height: 1.7;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
  margin: 28px 0 16px;
}

.kpi {
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(20, 32, 30, 0.08);
}

.kpi span {
  display: block;
  color: var(--aico-muted);
  font-size: 13px;
}

.kpi strong {
  display: block;
  margin-top: 8px;
  font-family: var(--font-display);
  font-size: 24px;
}

.kpi .warn {
  color: var(--aico-coral);
}

.panels {
  display: grid;
  grid-template-columns: 1.4fr 0.8fr;
  gap: 12px;
  margin-bottom: 12px;
}

.panel {
  padding: 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid rgba(20, 32, 30, 0.08);
}

.panel.wide {
  grid-column: 1 / -1;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.panel-head h2 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 18px;
}

.chart {
  height: 200px;
}

.chart svg {
  width: 100%;
  height: 170px;
}

.chart-meta {
  display: flex;
  justify-content: space-between;
  color: var(--aico-muted);
  font-size: 13px;
}

.event-list {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 220px;
  overflow: auto;
}

.event-list li {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid rgba(20, 32, 30, 0.06);
}

.event-list .empty {
  color: var(--aico-muted);
}

.search {
  height: 36px;
  border: 1px solid rgba(20, 32, 30, 0.14);
  border-radius: 10px;
  padding: 0 12px;
  min-width: 220px;
  font: inherit;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

th,
td {
  text-align: left;
  padding: 10px 8px;
  border-bottom: 1px solid rgba(20, 32, 30, 0.06);
  vertical-align: top;
}

code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
}

.log-table-wrap {
  max-height: 420px;
  overflow: auto;
}

.status {
  display: inline-block;
  min-width: 36px;
  border-radius: 999px;
  padding: 2px 8px;
  text-align: center;
  font-weight: 700;
}

.status.s2 { background: rgba(15, 92, 86, 0.12); color: var(--aico-teal-deep); }
.status.s4 { background: rgba(232, 93, 60, 0.14); color: #9b3a24; }
.status.s5 { background: rgba(160, 20, 20, 0.12); color: #8a1212; }

@media (max-width: 980px) {
  .kpi-grid,
  .panels {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 640px) {
  .kpi-grid,
  .panels,
  .bar {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
