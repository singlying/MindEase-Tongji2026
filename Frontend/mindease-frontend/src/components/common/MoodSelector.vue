<!-- 前端A负责：情绪选择器组件 -->
<script setup lang="ts">
import { MOOD_OPTIONS, type MoodType } from "@/api/mood";

withDefaults(
  defineProps<{
    modelValue: MoodType;
    label?: string;
  }>(),
  {
    label: "今天的心情如何？",
  }
);

const emit = defineEmits<{
  (e: "update:modelValue", value: MoodType): void;
}>();

function selectMood(value: MoodType) {
  emit("update:modelValue", value);
}
</script>

<template>
  <div class="mood-selector">
    <div class="selector-label">{{ label }}</div>
    <div class="mood-grid">
      <button
        v-for="item in MOOD_OPTIONS"
        :key="item.value"
        type="button"
        :class="['mood-button', { active: modelValue === item.value }]"
        @click="selectMood(item.value)"
      >
        <span class="emoji">{{ item.emoji }}</span>
        <span class="name">{{ item.label }}</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.mood-selector {
  display: grid;
  gap: 12px;
}

.selector-label {
  font-weight: 600;
}

.mood-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(96px, 1fr));
  gap: 10px;
}

.mood-button {
  border: 1px solid rgba(123, 158, 137, 0.18);
  background: rgba(255, 255, 255, 0.7);
  border-radius: 16px;
  padding: 12px 10px;
  display: grid;
  place-items: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.mood-button:hover,
.mood-button.active {
  border-color: var(--ease-primary);
  background: rgba(123, 158, 137, 0.12);
}

.emoji {
  font-size: 28px;
}

.name {
  font-size: 13px;
  color: var(--ease-text);
}
</style>
