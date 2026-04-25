<!-- 前端A负责：用户端占位页面基座 -->
<!-- 前端B负责：医生端 / 管理端页面也复用此占位基座 -->
<script setup lang="ts">
import { useRouter } from "vue-router";

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
    description: "当前页面正在持续完善中，后续会逐步补充更完整的内容与交互。",
    actions: () => [],
  }
);

const router = useRouter();

function go(to: string) {
  router.push(to);
}
</script>

<template>
  <section class="placeholder glass-card">
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
