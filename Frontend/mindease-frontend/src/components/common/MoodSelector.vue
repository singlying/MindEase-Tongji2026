<!--
  组件名称: MoodSelector - 情绪选择器
  
  功能说明:
  - 展示7种情绪类型供用户选择
  - 支持v-model双向绑定选中值
  - 展示情绪emoji和中文名称
  
  使用示例:
  <MoodSelector v-model="formData.moodType" label="今天的心情？" />
  
  Props:
  - modelValue: 当前选中的情绪类型 (MoodType | null)
  - label: 标签文字 (string, 默认: "今天的心情是？")
  
  Emits:
  - update:modelValue: 选中情绪变化时触发
-->
<template>
  <div class="mood-selector">
    <label v-if="label" class="mood-label">
      <i class="fas fa-face-smile"></i>
      {{ label }}
    </label>
    <div class="mood-grid">
      <button
        v-for="mood in MOOD_OPTIONS"
        :key="mood.value"
        type="button"
        class="mood-tag"
        :class="{ active: modelValue === mood.value }"
        @click="selectMood(mood.value)"
      >
        <div class="mood-icon">{{ mood.emoji }}</div>
        <div class="mood-name">{{ mood.label }}</div>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { MoodType } from "@/api/mood";
import { MOOD_OPTIONS } from "@/api/mood";

interface Props {
  modelValue?: MoodType | null;
  label?: string;
}

interface Emits {
  (e: "update:modelValue", value: MoodType): void;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  label: "今天的心情是？",
});

const emit = defineEmits<Emits>();

const selectMood = (value: MoodType) => {
  emit("update:modelValue", value);
};
</script>

<style scoped>
.mood-selector {
  margin-bottom: 2rem;
}

.mood-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--ease-dark);
  margin-bottom: 1rem;
}

.mood-label i {
  color: var(--ease-accent);
}

.mood-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 1rem;
}

.mood-tag {
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(10px);
  border-radius: 1rem;
  padding: 1.5rem 1rem;
  border: 2px solid transparent;
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: center;
}

.mood-tag:hover {
  border-color: var(--ease-accent);
  background: rgba(255, 255, 255, 0.6);
  box-shadow: 0 4px 12px rgba(123, 158, 137, 0.15);
  transform: translateY(-2px);
}

.mood-tag.active {
  border-color: var(--ease-accent);
  background: white;
  box-shadow: 0 4px 16px rgba(123, 158, 137, 0.2);
}

.mood-icon {
  font-size: 3rem;
  margin-bottom: 0.5rem;
  transition: transform 0.3s ease;
}

.mood-tag:hover .mood-icon {
  transform: scale(1.1);
}

.mood-name {
  font-size: 0.875rem;
  font-weight: 500;
  color: #4b5563;
}

@media (max-width: 768px) {
  .mood-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 0.75rem;
  }

  .mood-tag {
    padding: 1rem 0.5rem;
  }

  .mood-icon {
    font-size: 2.5rem;
  }
}
</style>
