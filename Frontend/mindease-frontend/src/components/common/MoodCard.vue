<!-- 前端A负责：情绪日记卡片组件 -->
<script setup lang="ts">
import { computed } from "vue";

import type { MoodLogItem } from "@/api/mood";

const props = withDefaults(
  defineProps<{
    mood: MoodLogItem;
  }>(),
  {}
);

const emit = defineEmits<{
  (e: "click", id: number): void;
}>();

const formattedDate = computed(() => {
  const date = new Date(props.mood.logDate);
  return `${date.getMonth() + 1}月${date.getDate()}日`;
});

const snippet = computed(() => {
  return props.mood.content.length > 48
    ? `${props.mood.content.slice(0, 48)}...`
    : props.mood.content;
});

function handleClick() {
  emit("click", props.mood.id);
}
</script>

<template>
  <article class="mood-card glass-card" @click="handleClick">
    <div class="card-head">
      <div>
        <div class="date">{{ formattedDate }}</div>
        <div class="type">{{ props.mood.emoji }} {{ props.mood.moodType }}</div>
      </div>
      <div class="score">{{ props.mood.moodScore }}/10</div>
    </div>

    <p class="content">{{ snippet }}</p>

    <div v-if="props.mood.tags.length" class="tags">
      <span v-for="tag in props.mood.tags.slice(0, 3)" :key="tag" class="tag">
        {{ tag }}
      </span>
    </div>
  </article>
</template>

<style scoped>
.mood-card {
  padding: 18px;
  display: grid;
  gap: 12px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.mood-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 36px rgba(47, 65, 57, 0.1);
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.date {
  font-size: 13px;
  color: var(--ease-muted);
}

.type {
  margin-top: 4px;
  font-weight: 600;
}

.score {
  color: var(--ease-primary-dark);
  font-weight: 700;
}

.content {
  margin: 0;
  color: var(--ease-text);
  line-height: 1.7;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(123, 158, 137, 0.12);
  color: var(--ease-primary-dark);
  font-size: 12px;
}
</style>
