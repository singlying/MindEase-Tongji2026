<!--
  组件名称: LoadingSpinner - 加载动画
  
  功能说明:
  - 显示旋转的加载动画
  - 支持全屏或局部显示
  - 可自定义加载文字
  
  使用示例:
  <LoadingSpinner text="数据加载中..." />
  <LoadingSpinner fullscreen />
  
  Props:
  - text: 加载提示文字 (string, 默认: "加载中...")
  - fullscreen: 是否全屏显示 (boolean, 默认: false)
-->
<template>
  <div class="loading-spinner" :class="{ 'is-fullscreen': fullscreen }">
    <div class="spinner-container">
      <div class="spinner"></div>
      <p v-if="text" class="loading-text">{{ text }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  text?: string;
  fullscreen?: boolean;
}

withDefaults(defineProps<Props>(), {
  text: "加载中...",
  fullscreen: false,
});
</script>

<style scoped>
.loading-spinner {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 3rem;
}

.loading-spinner.is-fullscreen {
  position: fixed;
  inset: 0;
  background: rgba(242, 245, 243, 0.8);
  backdrop-filter: blur(4px);
  z-index: 9999;
}

.spinner-container {
  text-align: center;
}

.spinner {
  width: 3rem;
  height: 3rem;
  border: 3px solid rgba(123, 158, 137, 0.2);
  border-top-color: var(--ease-accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 1rem;
}

.loading-text {
  font-size: 0.875rem;
  color: #6b7280;
  font-weight: 500;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
