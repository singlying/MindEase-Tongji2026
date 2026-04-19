<!-- 前端B负责：注册页，后续承接咨询师注册与资料上传 -->
<script setup lang="ts">
import { useRouter } from "vue-router";

const router = useRouter();
</script>

<template>
  <div class="auth-page">
    <section class="auth-card glass-card">
      <div class="tag">前端B负责</div>
      <h1>注册页占位</h1>
      <p>
        当前只保留页面入口与后续职责说明。正式的多角色注册、咨询师资质上传与字段校验会按计划分批迁移。
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

.tag {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(196, 124, 107, 0.14);
  color: #9a5e52;
  font-size: 13px;
  font-weight: 700;
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
