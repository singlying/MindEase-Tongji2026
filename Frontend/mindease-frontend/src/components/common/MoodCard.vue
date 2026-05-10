<!--
  组件名称: MoodCard - 情绪日记卡片
  
  功能说明:
  - 展示单条情绪日记的摘要信息
  - 显示日期、emoji、内容摘要、标签、评分
  - 支持点击事件跳转到详情页
  
  使用示例:
  <MoodCard :mood="diaryItem" @click="viewDetail" />
  
  Props:
  - mood: 日记数据对象 (MoodLogItem, 必填)
  - showActions: 是否显示操作按钮 (boolean, 默认: false)
  
  Emits:
  - click: 点击卡片时触发，参数为日记ID
  - edit: 点击编辑按钮时触发 (需showActions=true)
  - delete: 点击删除按钮时触发 (需showActions=true)
-->
<template>
  <div class="mood-card" @click="handleClick">
    <div class="mood-card-header">
      <div class="mood-card-date">
        <span class="date-text">{{ formattedDate }}</span>
        <span class="day-text">{{ dayOfWeek }}</span>
      </div>
      <div class="mood-emoji">{{ getEmojiByType(mood.moodType) }}</div>
    </div>

    <div class="mood-card-content">
      <p class="mood-content-text">{{ truncatedContent }}</p>
    </div>

    <div v-if="mood.tags && mood.tags.length > 0" class="mood-card-tags">
      <span v-for="tag in mood.tags.slice(0, 3)" :key="tag" class="mood-tag">
        {{ tag }}
      </span>
    </div>

    <div class="mood-card-footer">
      <div class="mood-score">
        <i class="fas fa-heart"></i>
        <span>{{ mood.moodScore }}/10</span>
      </div>
      <div class="mood-actions" @click.stop>
        <button
          v-if="showActions"
          class="action-btn"
          @click="$emit('edit', mood.id)"
          title="编辑"
        >
          <i class="fas fa-pen"></i>
        </button>
        <button
          v-if="showActions"
          class="action-btn danger"
          @click="$emit('delete', mood.id)"
          title="删除"
        >
          <i class="fas fa-trash"></i>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import type { MoodLogItem, MoodType } from "@/api/mood";

interface Props {
  mood: MoodLogItem;
  showActions?: boolean;
  maxLength?: number;
}

interface Emits {
  (e: "click", id: number): void;
  (e: "edit", id: number): void;
  (e: "delete", id: number): void;
}

const props = withDefaults(defineProps<Props>(), {
  showActions: false,
  maxLength: 100,
});

const emit = defineEmits<Emits>();

// 情绪类型到emoji的映射
const emojiMap: Record<MoodType, string> = {
  Happy: "😊",
  Sad: "😢",
  Anxious: "😰",
  Calm: "😌",
  Angry: "😠",
  Tired: "😴",
  Excited: "🎉",
};

const getEmojiByType = (type: MoodType): string => {
  return props.mood.emoji || emojiMap[type] || "😊";
};

// 格式化日期
const formattedDate = computed(() => {
  const date = new Date(props.mood.logDate);
  return `${date.getMonth() + 1}月${date.getDate()}日`;
});

const dayOfWeek = computed(() => {
  const date = new Date(props.mood.logDate);
  const days = [
    "星期日",
    "星期一",
    "星期二",
    "星期三",
    "星期四",
    "星期五",
    "星期六",
  ];
  return days[date.getDay()];
});

// 截断内容
const truncatedContent = computed(() => {
  if (props.mood.content.length <= props.maxLength) {
    return props.mood.content;
  }
  return props.mood.content.substring(0, props.maxLength) + "...";
});

const handleClick = () => {
  emit("click", props.mood.id);
};
</script>

<style scoped>
.mood-card {
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(10px);
  border-radius: 1rem;
  padding: 1.25rem;
  border: 1px solid rgba(229, 231, 235, 0.8);
  cursor: pointer;
  transition: all 0.3s ease;
}

.mood-card:hover {
  background: rgba(255, 255, 255, 0.7);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
  border-color: var(--ease-accent);
}

.mood-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
}

.mood-card-date {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.date-text {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--ease-dark);
}

.day-text {
  font-size: 0.75rem;
  color: #6b7280;
}

.mood-emoji {
  font-size: 2rem;
  line-height: 1;
}

.mood-card-content {
  margin-bottom: 1rem;
}

.mood-content-text {
  font-size: 0.875rem;
  color: #4b5563;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.mood-card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.mood-tag {
  font-size: 0.75rem;
  padding: 0.25rem 0.75rem;
  background: rgba(123, 158, 137, 0.1);
  color: var(--ease-accent-dark);
  border-radius: 9999px;
  font-weight: 500;
}

.mood-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 1rem;
  border-top: 1px solid rgba(229, 231, 235, 0.5);
}

.mood-score {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: var(--ease-accent);
  font-weight: 600;
}

.mood-score i {
  color: var(--ease-warm);
}

.mood-actions {
  display: flex;
  gap: 0.5rem;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.mood-card:hover .mood-actions {
  opacity: 1;
}

.action-btn {
  width: 2rem;
  height: 2rem;
  border-radius: 0.5rem;
  border: none;
  background: rgba(123, 158, 137, 0.1);
  color: var(--ease-accent);
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-btn:hover {
  background: var(--ease-accent);
  color: white;
}

.action-btn.danger {
  background: rgba(239, 68, 68, 0.1);
  color: #dc2626;
}

.action-btn.danger:hover {
  background: #dc2626;
  color: white;
}
</style>
