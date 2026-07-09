<!-- LeafNode.vue -->
<template>
  <div class="leaf-node">
    {{ displayLabel || data.displayLabel || data.label }}
    <div class="node-full-label">{{ fullLabel || data.label }}</div>
  </div>
</template>

<script setup>
const props = defineProps({
  id: {
    type: String,
    required: true
  },
  data: {
    type: Object,
    required: true
  },
  displayLabel: {
    type: String,
    default: ''
  },
  fullLabel: {
    type: String,
    default: ''
  }
});
</script>

<style scoped>
.leaf-node {
  width: 70px;
  height: 70px;
  background-color: #5a93c0;
  color: white;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  text-align: center;
  font-size: 12px;
  font-weight: 500;
  padding: 8px;
  cursor: pointer;
  user-select: none;
  position: relative;
  word-break: break-all;
  overflow: hidden;
}

.node-full-label {
  position: absolute;
  right: -120px;
  top: 50%;
  transform: translateY(-50%);
  background: white;
  color: #333;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  z-index: 2;
}
</style> 