<!-- 前端A负责：用户端占位页面基座 -->
<!-- 前端B负责：医生端 / 管理端页面也复用此占位基座 -->
<script setup lang="ts">
import { useRoute, useRouter } from "vue-router";

interface PageAction {
  label: string;
  to: string;
}

const props = withDefaults(
  defineProps<{
    title: string;
    owner: "A" | "B";
    description?: string;
    actions?: PageAction[];
  }>(),
  {
    description: "当前页面内容暂时留空，后续按 commit 计划逐步迁移旧项目具体实现。",
    actions: () => [],
  }
);

const route = useRoute();
const router = useRouter();

function go(to: string) {
  router.push(to);
}
</script>

<template>
  <section class="placeholder glass-card">
    <div class="label-row">
      <span class="owner-badge">前端{{ props.owner }}负责</span>
      <span class="path-text">当前路由：{{ route.path }}</span>
    </div>

    <h2>{{ props.title }}</h2>
    <p>{{ props.description }}</p>

    <div v-if="props.actions.length" class="actions">
      <el-button
        v-for="action in props.actions"
        :key="action.to"
        type="primary"
        plain
        @click="go(action.to)"
      >
        {{ action.label }}
      </el-button>
    </div>
  </section>
</template>

<style scoped>
.placeholder {
  padding: 32px;
  min-height: 360px;
  display: grid;
  align-content: start;
  gap: 16px;
}

.label-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.owner-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(123, 158, 137, 0.12);
  color: var(--ease-primary-dark);
  font-size: 13px;
  font-weight: 700;
}

.path-text {
  color: var(--ease-muted);
  font-size: 13px;
}

h2 {
  margin: 0;
  font-size: 28px;
}

p {
  margin: 0;
  max-width: 720px;
  color: var(--ease-muted);
  line-height: 1.7;
}

.actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 8px;
}
</style>
