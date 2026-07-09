<!-- DecisionNode.vue -->
<template>
  <div class="decision-node">
    {{ data.label }}
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
  }
});
</script>

<style scoped>
.decision-node {
  width: 70px;
  height: 70px;
  background-color: #384164;
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
}
</style> 