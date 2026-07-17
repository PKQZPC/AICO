<template>
  <main class="scenario">
    <div class="bg" aria-hidden="true"></div>
    <header class="bar">
      <button class="ghost" type="button" @click="go('/login')">← 返回登录</button>
      <strong class="logo">AICO</strong>
      <span class="badge">PERSONAL</span>
    </header>

    <section class="shell">
      <div class="hero-panel">
        <p class="brand">AICO</p>
        <h1>个人对齐工作台</h1>
        <p class="summary">
          你是长期对齐主体。在这里管理画像、关系网络、话题图谱与多轮协作策略。
        </p>
        <div class="hero-actions">
          <button type="button" class="primary" @click="go('/personal-workbench')">进入完整工作台</button>
          <button type="button" class="secondary" @click="go('/aico-alignment')">查看对齐状态</button>
        </div>
      </div>

      <div class="lanes">
        <article v-for="item in lanes" :key="item.title" class="lane">
          <h2>{{ item.title }}</h2>
          <p>{{ item.desc }}</p>
          <button type="button" @click="go(item.path)">{{ item.cta }}</button>
        </article>
      </div>
    </section>
  </main>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
const go = (path) => router.push(path)

const lanes = [
  {
    title: '长期对齐',
    desc: '确认个人画像、关系边、偏好记忆与确认来源。',
    cta: '打开对齐工作台',
    path: '/aico-alignment'
  },
  {
    title: '策略树',
    desc: '组织目的型聊天、关系修复与日常表达的多轮路径。',
    cta: '打开策略树',
    path: '/decision-tree'
  },
  {
    title: '对话空间',
    desc: '面向自己、朋友、家人、同事的关系化协作入口。',
    cta: '进入对话空间',
    path: '/personal-workbench'
  }
]
</script>

<style scoped>
.scenario {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  color: var(--aico-ink);
  background: linear-gradient(160deg, #f6fbf9, #e4efe9 55%, #d7e8e1);
}

.bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(700px 380px at 80% 0%, rgba(15, 92, 86, 0.18), transparent 60%),
    linear-gradient(rgba(20, 32, 30, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(20, 32, 30, 0.03) 1px, transparent 1px);
  background-size: auto, 42px 42px, 42px 42px;
  pointer-events: none;
}

.bar,
.shell {
  position: relative;
  z-index: 1;
  width: min(1100px, calc(100% - 40px));
  margin: 0 auto;
}

.bar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 24px 0 0;
}

.logo {
  font-family: var(--font-display);
  letter-spacing: 0.04em;
}

.badge {
  margin-left: auto;
  border-radius: 999px;
  padding: 4px 10px;
  background: rgba(15, 92, 86, 0.1);
  color: var(--aico-teal-deep);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.ghost {
  border: 0;
  background: transparent;
  color: var(--aico-muted);
  font: inherit;
  cursor: pointer;
}

.shell {
  padding: 36px 0 64px;
  animation: in 0.55s ease both;
}

@keyframes in {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: none; }
}

.hero-panel {
  padding: 40px 0 28px;
}

.brand {
  margin: 0 0 8px;
  font-family: var(--font-display);
  font-size: clamp(48px, 8vw, 72px);
  font-weight: 800;
  letter-spacing: -0.04em;
  color: var(--aico-teal-deep);
  line-height: 0.95;
}

h1 {
  margin: 0 0 12px;
  font-family: var(--font-display);
  font-size: clamp(28px, 4vw, 40px);
  letter-spacing: -0.03em;
}

.summary {
  max-width: 38rem;
  margin: 0 0 24px;
  color: var(--aico-muted);
  font-size: 17px;
  line-height: 1.7;
}

.hero-actions,
.lanes {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.lanes {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-top: 8px;
}

.lane {
  padding: 24px 22px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(20, 32, 30, 0.08);
  backdrop-filter: blur(8px);
}

.lane h2 {
  margin: 0 0 10px;
  font-family: var(--font-display);
  font-size: 20px;
}

.lane p {
  min-height: 3.4em;
  margin: 0 0 18px;
  color: var(--aico-muted);
  line-height: 1.65;
}

button {
  border: 0;
  border-radius: 999px;
  padding: 11px 18px;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.primary,
.lane button {
  background: var(--aico-teal);
  color: #fff;
}

.secondary {
  background: rgba(255, 255, 255, 0.8);
  color: var(--aico-teal-deep);
  border: 1px solid rgba(15, 92, 86, 0.18);
}

@media (max-width: 860px) {
  .lanes {
    grid-template-columns: 1fr;
  }
}
</style>
