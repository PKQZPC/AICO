<template>
  <div class="portal">
    <div class="portal-bg" aria-hidden="true">
      <div class="orb orb-a"></div>
      <div class="orb orb-b"></div>
      <div class="grid-fade"></div>
    </div>

    <header class="topbar">
      <div class="brand-mark">
        <span class="brand-dot"></span>
        <strong>AICO</strong>
      </div>
      <nav class="top-links">
        <button type="button" class="linkish" @click="goOps">运维看板</button>
        <button type="button" class="linkish" @click="goLogDebug">日志 Debug</button>
      </nav>
    </header>

    <main class="hero">
      <p class="brand-hero">AICO</p>
      <h1>选择你的协作身份</h1>
      <p class="lede">
        AI-human Alignment and Cooperation —— 让长期互动成为可对齐、可协作的持续过程。
      </p>

      <div class="role-switch" role="tablist" aria-label="角色选择">
        <button
          v-for="role in roles"
          :key="role.id"
          type="button"
          role="tab"
          class="role-tab"
          :class="{ active: activeRole === role.id }"
          :aria-selected="activeRole === role.id"
          @click="activeRole = role.id"
        >
          <span class="role-name">{{ role.label }}</span>
          <span class="role-hint">{{ role.hint }}</span>
        </button>
      </div>

      <section class="login-stage" :data-role="activeRole">
        <div class="stage-copy">
          <p class="eyebrow">{{ current.eyebrow }}</p>
          <h2>{{ current.title }}</h2>
          <p>{{ current.desc }}</p>
          <ul>
            <li v-for="item in current.bullets" :key="item">{{ item }}</li>
          </ul>
        </div>

        <form class="stage-form" @submit.prevent="submitLogin">
          <label v-if="current.needUsername" class="field">
            <span>用户名</span>
            <input
              v-model="username"
              type="text"
              autocomplete="username"
              :placeholder="current.placeholder"
              :disabled="loading"
            />
          </label>

          <p v-else class="demo-note">演示环境将使用预设专家账号直接进入工作台。</p>

          <button class="cta" type="submit" :disabled="loading">
            {{ loading ? '正在进入…' : current.cta }}
          </button>

          <p class="fineprint">
            演示账号：
            <button type="button" class="chip" @click="fillDemo('parent')">parent</button>
            <button type="button" class="chip" @click="fillDemo('user')">user</button>
            <button type="button" class="chip" @click="fillDemo('expert')">expert</button>
          </p>
        </form>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { loginParent } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
userStore.loadUserInfo()

const activeRole = ref('personal')
const username = ref('')
const loading = ref(false)

const roles = [
  { id: 'personal', label: '个人用户', hint: '长期自我对齐' },
  { id: 'expert', label: '专家', hint: '专业逻辑对齐' },
  { id: 'client', label: 'Client', hint: '轻量互动端' }
]

const roleConfig = {
  personal: {
    eyebrow: 'PERSONAL',
    title: '进入个人对齐空间',
    desc: '把你自己当作长期对齐主体，管理画像、关系网络与多轮对话策略。',
    bullets: ['个人画像与关系图谱', '话题与策略树确认', '面向自己与重要他人的协作回复'],
    needUsername: true,
    placeholder: '输入个人用户名，如 user',
    cta: '进入个人工作台',
    path: '/personal'
  },
  expert: {
    eyebrow: 'EXPERT',
    title: '进入专家工作台',
    desc: '系统对齐的是专家逻辑。你将审核候选策略、指导回复并沉淀可复用决策树。',
    bullets: ['专家策略树与确认权', 'AI 推荐与人工介入', '案例执行路径可追溯'],
    needUsername: false,
    placeholder: '',
    cta: '进入专家工作台',
    path: '/expert-chat'
  },
  client: {
    eyebrow: 'CLIENT',
    title: '进入 Client 聊天端',
    desc: '轻量输入端，用于触发专家逻辑与服务流程。你不是长期对齐主体。',
    bullets: ['选择专家并开始对话', '自然表达当前困扰', '接收专家与 AI 协作回复'],
    needUsername: true,
    placeholder: '输入 Client 用户名，如 parent',
    cta: '进入 Client 聊天',
    path: '/parent-chat'
  }
}

const current = computed(() => roleConfig[activeRole.value])

const fillDemo = (name) => {
  username.value = name
  if (name === 'expert') activeRole.value = 'expert'
  else if (name === 'parent') activeRole.value = 'client'
  else activeRole.value = 'personal'
}

const goOps = () => router.push('/ops/monitor')
const goLogDebug = () => router.push('/ops/log-debug')

const submitLogin = async () => {
  const role = activeRole.value
  const cfg = roleConfig[role]

  try {
    loading.value = true

    if (role === 'expert') {
      userStore.setUserInfo({
        id: 1,
        name: username.value || 'Demo Expert',
        type: 'expert',
        role: 'expert'
      })
      ElMessage.success('专家身份已就绪')
      router.push(cfg.path)
      return
    }

    if (!username.value.trim()) {
      ElMessage.warning('请输入用户名')
      return
    }

    const res = await loginParent(username.value.trim())
    userStore.setUserInfo({
      id: res.data.userId,
      name: res.data.name,
      type: role === 'personal' ? 'personal' : 'parent',
      role
    })
    ElMessage.success('登录成功')
    router.push(cfg.path)
  } catch (error) {
    ElMessage.error(typeof error === 'string' ? error : (error?.message || '登录失败'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.portal {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  color: var(--aico-ink);
  background:
    radial-gradient(1200px 600px at 12% -10%, rgba(15, 92, 86, 0.22), transparent 55%),
    radial-gradient(900px 500px at 95% 10%, rgba(232, 93, 60, 0.12), transparent 50%),
    linear-gradient(165deg, #f7fbf9 0%, #e7f1ee 48%, #dfeae6 100%);
}

.portal-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(8px);
  opacity: 0.55;
  animation: drift 14s ease-in-out infinite;
}

.orb-a {
  width: 420px;
  height: 420px;
  left: -80px;
  top: 18%;
  background: radial-gradient(circle, rgba(15, 92, 86, 0.35), transparent 70%);
}

.orb-b {
  width: 360px;
  height: 360px;
  right: -60px;
  bottom: 8%;
  background: radial-gradient(circle, rgba(232, 93, 60, 0.22), transparent 70%);
  animation-delay: -4s;
}

.grid-fade {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(20, 32, 30, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(20, 32, 30, 0.035) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(circle at 50% 30%, black, transparent 75%);
}

@keyframes drift {
  0%, 100% { transform: translate3d(0, 0, 0) scale(1); }
  50% { transform: translate3d(18px, -24px, 0) scale(1.05); }
}

.topbar {
  position: relative;
  z-index: 2;
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: min(1120px, calc(100% - 40px));
  margin: 0 auto;
  padding: 28px 0 0;
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-family: var(--font-display);
  font-size: 18px;
  letter-spacing: 0.04em;
}

.brand-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--aico-coral);
  box-shadow: 0 0 0 4px rgba(232, 93, 60, 0.18);
}

.top-links {
  display: flex;
  gap: 14px;
  align-items: center;
}

.linkish {
  border: 0;
  background: transparent;
  color: var(--aico-muted);
  font: inherit;
  cursor: pointer;
}

.linkish:hover {
  color: var(--aico-teal);
}

.hero {
  position: relative;
  z-index: 2;
  width: min(1120px, calc(100% - 40px));
  margin: 0 auto;
  padding: 48px 0 72px;
  animation: rise 0.7s ease both;
}

@keyframes rise {
  from { opacity: 0; transform: translateY(18px); }
  to { opacity: 1; transform: translateY(0); }
}

.brand-hero {
  margin: 0 0 12px;
  font-family: var(--font-display);
  font-size: clamp(56px, 10vw, 92px);
  font-weight: 800;
  letter-spacing: -0.04em;
  line-height: 0.92;
  color: var(--aico-teal-deep);
}

.hero h1 {
  margin: 0 0 14px;
  max-width: 16ch;
  font-family: var(--font-display);
  font-size: clamp(28px, 4vw, 42px);
  font-weight: 700;
  letter-spacing: -0.03em;
  line-height: 1.15;
}

.lede {
  max-width: 42rem;
  margin: 0 0 36px;
  color: var(--aico-muted);
  font-size: 18px;
  line-height: 1.7;
}

.role-switch {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 22px;
}

.role-tab {
  text-align: left;
  border: 1px solid transparent;
  border-radius: 18px;
  padding: 18px 18px 16px;
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(10px);
  cursor: pointer;
  transition: transform 0.25s ease, border-color 0.25s ease, background 0.25s ease;
}

.role-tab:hover {
  transform: translateY(-2px);
  border-color: rgba(15, 92, 86, 0.25);
}

.role-tab.active {
  background: #fff;
  border-color: rgba(15, 92, 86, 0.35);
  box-shadow: 0 18px 40px rgba(15, 92, 86, 0.08);
}

.role-name {
  display: block;
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 700;
}

.role-hint {
  display: block;
  margin-top: 4px;
  color: var(--aico-muted);
  font-size: 13px;
}

.login-stage {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 0;
  overflow: hidden;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(20, 32, 30, 0.08);
  box-shadow: 0 30px 80px rgba(15, 92, 86, 0.08);
  animation: stageIn 0.45s ease both;
}

@keyframes stageIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.stage-copy {
  padding: 36px 40px;
  background:
    linear-gradient(145deg, rgba(15, 92, 86, 0.08), transparent 55%),
    linear-gradient(180deg, #fbfdfc, #f1f7f5);
}

.stage-copy .eyebrow {
  margin: 0 0 10px;
  color: var(--aico-teal);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
}

.stage-copy h2 {
  margin: 0 0 12px;
  font-family: var(--font-display);
  font-size: 28px;
  letter-spacing: -0.02em;
}

.stage-copy p {
  margin: 0 0 18px;
  color: var(--aico-muted);
  line-height: 1.7;
}

.stage-copy ul {
  margin: 0;
  padding: 0;
  list-style: none;
}

.stage-copy li {
  position: relative;
  padding: 8px 0 8px 18px;
  color: var(--aico-ink);
}

.stage-copy li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 16px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--aico-coral);
}

.stage-form {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 16px;
  padding: 36px 32px;
  background: #fff;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
}

.field input {
  height: 48px;
  border: 1px solid rgba(20, 32, 30, 0.14);
  border-radius: 14px;
  padding: 0 14px;
  font: inherit;
  font-weight: 400;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.field input:focus {
  border-color: var(--aico-teal);
  box-shadow: 0 0 0 4px rgba(15, 92, 86, 0.12);
}

.demo-note {
  margin: 0;
  color: var(--aico-muted);
  line-height: 1.6;
}

.cta {
  height: 52px;
  border: 0;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--aico-teal) 0%, var(--aico-teal-deep) 100%);
  color: #fff;
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.cta:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 14px 28px rgba(15, 92, 86, 0.25);
}

.cta:disabled {
  opacity: 0.7;
  cursor: wait;
}

.fineprint {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin: 4px 0 0;
  color: var(--aico-muted);
  font-size: 13px;
}

.chip {
  border: 1px solid rgba(20, 32, 30, 0.12);
  border-radius: 999px;
  padding: 4px 10px;
  background: #f5faf8;
  color: var(--aico-teal-deep);
  font: inherit;
  cursor: pointer;
}

.chip:hover {
  border-color: var(--aico-teal);
}

@media (max-width: 860px) {
  .role-switch,
  .login-stage {
    grid-template-columns: 1fr;
  }

  .hero {
    padding-top: 28px;
  }

  .stage-copy,
  .stage-form {
    padding: 28px 22px;
  }
}
</style>
