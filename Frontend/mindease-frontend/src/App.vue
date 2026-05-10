<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <!-- 直接渲染路由视图，不添加任何额外内容 -->
  <RouterView />
</template>

<style>
/* 全局样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html,
body,
#app {
  width: 100%;
  height: 100%;
  overflow: hidden;
}

body {
  font-family: 'Outfit', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  background: #f2f5f3;
}

/* Element Plus 主题色覆盖 */
:root {
  --el-color-primary: #7b9e89;
  --el-color-primary-light-3: #9ab5a4;
  --el-color-primary-light-5: #b3c7bb;
  --el-color-primary-light-7: #ccd9d2;
  --el-color-primary-light-8: #e0e9e4;
  --el-color-primary-light-9: #eff4f1;
  --el-color-primary-dark-2: #5f7a6a;
}
</style>
