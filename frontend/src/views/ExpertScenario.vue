<template>
  <main class="scenario">
    <div class="bg" aria-hidden="true"></div>
    <header class="bar">
      <button class="ghost" type="button" @click="go('/login')">← 返回登录</button>
      <strong class="logo">AICO</strong>
      <span class="badge">EXPERT</span>
    </header>

    <section class="shell">
      <div class="hero-panel">
        <p class="brand">AICO</p>
        <h1>专家协作入口</h1>
        <p class="summary">
          系统长期对齐专家的专业逻辑；Client 负责轻量输入，触发服务流程与反馈闭环。
        </p>
      </div>

      <div class="split">
        <article class="panel expert">
          <p class="kicker">对齐主体</p>
          <h2>专家工作台</h2>
          <p>审核候选节点、指导回复、沉淀可复用策略树与确认记录。</p>
          <div class="actions">
            <button type="button" class="primary" @click="enterExpert">进入专家工作台</button>
            <button type="button" class="secondary" @click="go('/decision-tree')">策略树</button>
            <button type="button" class="secondary" @click="go('/statistics')">统计</button>
          </div>
        </article>

        <article class="panel client">
          <p class="kicker">服务上下文</p>
          <h2>Client 聊天端</h2>
          <p>选择专家并自然表达诉求。Client 不是长期对齐主体，仅作为服务上下文。</p>
          <div class="actions">
            <button type="button" class="primary alt" @click="go('/parent-chat')">打开 Client 端</button>
            <button type="button" class="secondary" @click="go('/aico-alignment')">对齐状态</button>
          </div>
        </article>
      </div>
    </section>
  </main>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const go = (path) => router.push(path)

const enterExpert = () => {
  if (!userStore.userInfo || userStore.userInfo.type !== 'expert') {
    userStore.setUserInfo({ id: 1, name: 'Demo Expert', type: 'expert', role: 'expert' })
  }
  go('/expert-chat')
}
</script>

<style scoped>
.scenario {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  color: var(--aico-ink);
  background: linear-gradient(155deg, #f8f6f3 0%, #ebe7e1 45%, #e3ece8 100%);
}

.bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(720px 420px at 15% 0%, rgba(232, 93, 60, 0.14), transparent 55%),
    radial-gradient(680px 360px at 90% 20%, rgba(15, 92, 86, 0.16), transparent 60%);
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
  background: rgba(232, 93, 60, 0.12);
  color: #9b3a24;
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
  max-width: 40rem;
  margin: 0 0 28px;
  color: var(--aico-muted);
  font-size: 17px;
  line-height: 1.7;
}

.split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.panel {
  padding: 28px 26px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(20, 32, 30, 0.08);
  backdrop-filter: blur(8px);
}

.kicker {
  margin: 0 0 8px;
  color: var(--aico-teal);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.panel h2 {
  margin: 0 0 10px;
  font-family: var(--font-display);
  font-size: 24px;
}

.panel p {
  margin: 0 0 20px;
  color: var(--aico-muted);
  line-height: 1.7;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

button {
  border: 0;
  border-radius: 999px;
  padding: 11px 18px;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.primary {
  background: var(--aico-teal);
  color: #fff;
}

.primary.alt {
  background: var(--aico-coral);
}

.secondary {
  background: rgba(255, 255, 255, 0.85);
  color: var(--aico-ink);
  border: 1px solid rgba(20, 32, 30, 0.12);
}

@media (max-width: 860px) {
  .split {
    grid-template-columns: 1fr;
  }
}
</style>
