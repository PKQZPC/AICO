<template>
  <main class="scenario-page">
    <section class="workspace">
      <header class="hero">
        <div>
          <p class="eyebrow">PERSONAL MODE</p>
          <h1>个人 AICO 工作台</h1>
          <p class="summary">
            每个用户都是自己长期对齐系统的 owner，也拥有完整的“专家式”工作台能力。
          </p>
        </div>
      </header>

      <section class="mode-grid">
        <article class="mode-panel">
          <h2>我的长期对齐</h2>
          <p>查看和确认个人画像、关系画像、偏好记忆、topic graph 与策略树候选。</p>
          <button @click="go('/aico-alignment')">打开对齐工作台</button>
        </article>

        <article class="mode-panel">
          <h2>我的策略树</h2>
          <p>个人模式也需要 Dialogue Strategy Tree，用于组织目的型聊天、关系修复、日常表达等多轮对话。</p>
          <button @click="go('/decision-tree')">打开策略树编辑器</button>
        </article>

        <article class="mode-panel">
          <h2>我的对话空间</h2>
          <p>进入个人 self/peer 多关系对话入口，用于组织自己、朋友、家人、同事等不同对象的长期对话策略。</p>
          <button @click="go('/personal-workbench')">进入完整工作台</button>
        </article>
      </section>

      <section class="definition">
        <h2>PERSONAL 与专家场景的区别</h2>
        <div class="definition-grid">
          <div>
            <h3>确认权</h3>
            <p>候选 topic、关系描述和策略树由 AI 主动生成，可形成 AI 确认；正式固化时需要标清用户确认、AI 确认或待确认状态。</p>
          </div>
          <div>
            <h3>扩展权</h3>
            <p>AI 可以更主动地生成候选树和候选分支，但需要保留用户编辑、删除和拒绝入口。</p>
          </div>
          <div>
            <h3>对齐目标</h3>
            <p>长期理解用户本人、不同关系对象、表达偏好和日常多轮对话策略。</p>
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

.hero {
  background: #ffffff;
  border: 1px solid #dbe1e7;
  border-radius: 8px;
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

.summary {
  max-width: 760px;
  margin-bottom: 0;
  color: #5c6670;
  line-height: 1.7;
}

.mode-grid,
.definition-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.mode-panel,
.definition {
  background: #ffffff;
  border: 1px solid #dbe1e7;
  border-radius: 8px;
  padding: 20px;
}

.mode-panel p,
.definition p {
  color: #5c6670;
  line-height: 1.7;
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

.definition {
  margin-top: 16px;
}

@media (max-width: 900px) {
  .mode-grid,
  .definition-grid {
    grid-template-columns: 1fr;
  }
}
</style>
