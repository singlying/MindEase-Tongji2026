<!-- 前端A负责：空状态组件 -->
<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string;
    description?: string;
  }>(),
  {
    description: "",
  }
);
</script>

<template>
  <div class="empty-state glass-card">
    <div class="emoji">🌿</div>
    <h3>{{ title }}</h3>
    <p v-if="description">{{ description }}</p>
    <div v-if="$slots.action" class="action-slot">
      <slot name="action" />
    </div>
  </div>
</template>

<style scoped>
.empty-state {
  padding: 32px 24px;
  display: grid;
  place-items: center;
  text-align: center;
  gap: 10px;
}

.emoji {
  font-size: 42px;
}

h3,
p {
  margin: 0;
}

p {
  max-width: 420px;
  color: var(--ease-muted);
  line-height: 1.7;
}

.action-slot {
  margin-top: 8px;
}
</style>
