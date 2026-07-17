<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <RouterView />
</template>

<style>
:root {
  --aico-ink: #14201e;
  --aico-teal: #0f5c56;
  --aico-teal-deep: #0a3d39;
  --aico-mist: #e8f2ef;
  --aico-sand: #f3f6f4;
  --aico-coral: #e85d3c;
  --aico-line: rgba(20, 32, 30, 0.12);
  --aico-muted: #5a6b67;
  /* 正常黑体 */
  --font-display: "Microsoft YaHei", "PingFang SC", "Hiragino Sans GB", "Heiti SC", "Noto Sans SC", SimHei, sans-serif;
  --font-body: "Microsoft YaHei", "PingFang SC", "Hiragino Sans GB", "Heiti SC", "Noto Sans SC", SimHei, sans-serif;
}

html,
body,
#app {
  width: 100%;
  min-height: 100vh;
  margin: 0 !important;
  padding: 0 !important;
  box-sizing: border-box;
  background: var(--aico-sand);
  color: var(--aico-ink);
  font-family: var(--font-body);
}

*,
*::before,
*::after {
  box-sizing: border-box;
}
</style>
