<!-- 前端B负责：注册页，后续承接咨询师注册与资料上传 -->
<script setup lang="ts">
import { useRouter } from "vue-router";

const router = useRouter();
</script>

<template>
  <div class="auth-page">
    <section class="auth-card glass-card">
      <h1>注册</h1>
      <p>
        当前已开放页面入口，多角色注册与咨询师资质资料完善正在持续开发中。
      </p>

      <div class="action-row">
        <el-button type="primary" @click="router.push('/login')">返回登录页</el-button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
}

.auth-card {
  width: min(680px, 100%);
  padding: 32px;
  display: grid;
  gap: 18px;
}

h1,
p {
  margin: 0;
}

p {
  color: var(--ease-muted);
  line-height: 1.7;
}
</style>
