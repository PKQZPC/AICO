<template>
  <main class="scenario-page">
    <section class="workspace">
      <header class="hero">
        <div>
          <p class="eyebrow">EXPERT MODE</p>
          <h1>专家模式入口</h1>
          <p class="summary">
            EXPERT 场景保留两个前端：专家端复杂，client 端简单。系统长期对齐的是专家；client 负责选择专家并正常打字互动，用于触发专家逻辑树、形成案例和反馈，不作为长期对齐主体。
          </p>
        </div>
      </header>

      <section class="split">
        <article class="side-panel expert">
          <h2>专家对齐主体</h2>
          <p>系统学习专家的专业逻辑树、服务风格、知识结构、反馈习惯和节点确认规则。</p>
          <div class="actions">
            <button @click="go('/expert-chat')">专家工作台</button>
            <button class="secondary" @click="go('/decision-tree')">专家策略树</button>
            <button class="secondary" @click="go('/statistics')">专家统计</button>
          </div>
        </article>

        <article class="side-panel client">
          <h2>Client 简单输入端</h2>
          <p>client 端保留选择专家和打字聊天功能，用于触发专家树执行和 AI 推荐回复。系统不在 EXPERT 场景中对齐 client 本人。</p>
          <div class="actions">
            <button @click="go('/parent-chat')">Client 聊天端</button>
            <button class="secondary" @click="go('/aico-alignment')">专家对齐状态</button>
          </div>
        </article>
      </section>

      <section class="definition">
        <h2>专家场景的权限边界</h2>
        <div class="definition-grid">
          <div>
            <h3>专家确认</h3>
            <p>AI 只能生成候选节点或候选分支，正式固化必须经过专家确认。</p>
          </div>
          <div>
            <h3>小幅扩展</h3>
            <p>当 client 输入超出既有树覆盖范围时，AI 基于专家已有父子节点逻辑生成下一层候选。</p>
          </div>
          <div>
            <h3>可追踪</h3>
            <p>专家策略树需要记录执行路径、跳转原因、专家修改和版本变化。</p>
          </div>
        </div>
      </section>
    </section>
  </main>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const go = (path) => {
  router.push(path)
}
</script>

<style scoped>
.scenario-page {
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
.side-panel,
.definition {
  background: #ffffff;
  border: 1px solid #dbe1e7;
  border-radius: 8px;
}

.hero {
  padding: 28px;
  margin-bottom: 18px;
}

.eyebrow {
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

.summary,
.side-panel p,
.definition p {
  color: #5c6670;
  line-height: 1.7;
}

.split {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.side-panel {
  padding: 22px;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
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

.definition {
  margin-top: 16px;
  padding: 20px;
}

.definition-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

@media (max-width: 900px) {
  .split,
  .definition-grid {
    grid-template-columns: 1fr;
  }
}
</style>
