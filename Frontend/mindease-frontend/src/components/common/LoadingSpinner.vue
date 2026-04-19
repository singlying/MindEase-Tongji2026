<!-- 前端A负责：加载态组件占位 -->
<template>
  <div class="loading-spinner">
    <el-skeleton :rows="2" animated />
  </div>
</template>

<style scoped>
.loading-spinner {
  width: 100%;
}
</style>
