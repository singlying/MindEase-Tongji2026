<!-- 前端A负责：加载态组件 -->
<script setup lang="ts">
withDefaults(
  defineProps<{
    text?: string;
    fullscreen?: boolean;
  }>(),
  {
    text: "加载中...",
    fullscreen: false,
  }
);
</script>

<template>
  <div :class="['loading-spinner', { fullscreen }]">
    <div class="spinner-card glass-card">
      <el-skeleton animated :rows="3" />
      <div class="loading-text">{{ text }}</div>
    </div>
  </div>
</template>

<style scoped>
.loading-spinner {
  width: 100%;
}

.loading-spinner.fullscreen {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: grid;
  place-items: center;
  background: rgba(243, 246, 244, 0.72);
  backdrop-filter: blur(6px);
}

.spinner-card {
  padding: 20px;
  width: min(100%, 360px);
}

.loading-text {
  margin-top: 12px;
  color: var(--ease-muted);
  font-size: 14px;
}
</style>
