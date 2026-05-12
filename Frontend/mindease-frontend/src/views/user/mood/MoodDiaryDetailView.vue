<template>
  <div class="mood-diary-detail-page">
    <LoadingSpinner v-if="loading" fullscreen />

    <template v-else-if="diary">
      <!-- 顶部装饰条 -->
      <div class="top-decoration"></div>

      <!-- 主卡片 -->
      <div class="main-card glass-panel">
        <!-- 操作按钮 -->
        <div class="actions">
          <button @click="goBack" class="btn-gradient">
            <i class="fas fa-arrow-left"></i>
            返回列表
          </button>
          <button @click="confirmDelete" class="btn-secondary">
            <i class="fas fa-trash"></i>
            删除
          </button>
        </div>

        <!-- 日期信息 -->
        <div class="diary-header">
          <div class="date-info">
            <h1 class="diary-date">{{ formattedDate }}</h1>
            <p class="diary-time">{{ formattedTime }}</p>
          </div>
          <div class="mood-badge">
            <span class="mood-emoji">{{ moodEmoji }}</span>
            <div class="mood-info">
              <span class="mood-type">{{ moodTypeName }}</span>
              <div class="mood-score-bar">
                <div
                  class="mood-score-fill"
                  :style="{ width: `${diary.moodScore * 10}%` }"
                ></div>
              </div>
              <span class="mood-score-text">{{ diary.moodScore }}/10</span>
            </div>
          </div>
        </div>

        <!-- 标签 -->
        <div v-if="diary.tags && diary.tags.length > 0" class="tags-section">
          <i class="fas fa-tags"></i>
          <span v-for="tag in diary.tags" :key="tag" class="tag">
            {{ tag }}
          </span>
        </div>

        <!-- 日记内容 -->
        <div class="diary-content">
          <p>{{ diary.content }}</p>
        </div>

        <!-- AI分析 -->
        <div v-if="diary.aiAnalysis" class="ai-analysis glass-card">
          <div class="ai-header">
            <div class="ai-icon-wrapper">
              <i class="fas fa-robot"></i>
            </div>
            <h3 class="ai-title">AI 情绪分析</h3>
          </div>
          <p class="ai-content">{{ diary.aiAnalysis }}</p>
        </div>

        <!-- 元数据 -->
        <div class="metadata">
          <div class="metadata-item">
            <i class="fas fa-calendar"></i>
            <span>记录于 {{ diary.logDate }}</span>
          </div>
          <div v-if="diary.createTime" class="metadata-item">
            <i class="fas fa-clock"></i>
            <span>创建于 {{ diary.createTime }}</span>
          </div>
        </div>
      </div>

      <!-- 导航到上一篇/下一篇 -->
      <div class="navigation-section">
        <button v-if="hasPrev" @click="gotoPrev" class="nav-btn glass-panel">
          <i class="fas fa-chevron-left"></i>
          <span>上一篇</span>
        </button>
        <button v-if="hasNext" @click="gotoNext" class="nav-btn glass-panel">
          <span>下一篇</span>
          <i class="fas fa-chevron-right"></i>
        </button>
      </div>
    </template>

    <EmptyState
      v-else
      icon="fas fa-exclamation-circle"
      title="日记不存在"
      description="该日记可能已被删除或不存在"
    >
      <template #action>
        <button @click="goBack" class="btn-primary">返回列表</button>
      </template>
    </EmptyState>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import type { MoodLogItem, MoodType } from "@/api/mood";
import { getMoodLogDetail, deleteMoodLog, MOOD_TYPE_MAP } from "@/api/mood";
import LoadingSpinner from "@/components/common/LoadingSpinner.vue";
import EmptyState from "@/components/common/EmptyState.vue";

const router = useRouter();
const route = useRoute();

const diary = ref<MoodLogItem | null>(null);
const loading = ref(false);

// 上一篇/下一篇（TODO: 需要后端支持）
const hasPrev = ref(false);
const hasNext = ref(false);

// 格式化日期
const formattedDate = computed(() => {
  if (!diary.value) return "";
  const date = new Date(diary.value.logDate);
  return `${date.getFullYear()}年 ${date.getMonth() + 1}月 ${date.getDate()}日`;
});

const formattedTime = computed(() => {
  if (!diary.value) return "";
  const date = new Date(diary.value.logDate);
  const days = [
    "星期日",
    "星期一",
    "星期二",
    "星期三",
    "星期四",
    "星期五",
    "星期六",
  ];
  return `${days[date.getDay()]} ${date.getHours()}:${String(
    date.getMinutes()
  ).padStart(2, "0")}`;
});

// 情绪类型名称
const moodTypeName = computed(() => {
  if (!diary.value) return "";
  return MOOD_TYPE_MAP[diary.value.moodType]?.label || diary.value.moodType;
});

// 情绪emoji
const moodEmoji = computed(() => {
  if (!diary.value) return "";
  return (
    diary.value.emoji || MOOD_TYPE_MAP[diary.value.moodType]?.emoji || "😊"
  );
});

// 加载日记详情
const loadDetail = async () => {
  const id = route.params.id as string;
  if (!id) {
    router.push("/mood-diary");
    return;
  }

  loading.value = true;

  try {
    const res = (await getMoodLogDetail(Number(id))) as any;
    diary.value = res.data;

    // TODO: 从后端获取上一篇/下一篇信息
    hasPrev.value = false;
    hasNext.value = false;
  } catch (error) {
    console.error("加载失败:", error);
    ElMessage.error("加载日记失败");
    diary.value = null;
  } finally {
    loading.value = false;
  }
};

// 返回列表
const goBack = () => {
  router.push("/mood-diary");
};

// 确认删除
const confirmDelete = () => {
  ElMessageBox.confirm("确定要删除这篇日记吗？删除后无法恢复。", "删除确认", {
    confirmButtonText: "确定删除",
    cancelButtonText: "取消",
    type: "warning",
    confirmButtonClass: "el-button--danger",
  })
    .then(() => {
      handleDelete();
    })
    .catch(() => {
      // 取消删除
    });
};

// 删除日记
const handleDelete = async () => {
  if (!diary.value) return;

  try {
    await deleteMoodLog(diary.value.id);
    ElMessage.success("删除成功");
    router.push("/mood-diary");
  } catch (error) {
    console.error("删除失败:", error);
    ElMessage.error("删除失败");
  }
};

// 上一篇
const gotoPrev = () => {
  // TODO: 实现跳转到上一篇
  ElMessage.info("功能开发中");
};

// 下一篇
const gotoNext = () => {
  // TODO: 实现跳转到下一篇
  ElMessage.info("功能开发中");
};

onMounted(() => {
  loadDetail();
});
</script>

<style scoped>
.mood-diary-detail-page {
  max-width: 56rem;
  margin: 0 auto;
  padding: var(--spacing-xl) var(--spacing-lg);
  min-height: calc(100vh - 4rem);
}

.main-card {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: var(--radius-2xl);
  padding: var(--spacing-2xl);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  margin-bottom: var(--spacing-lg);
}

.actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-xl);
  padding-bottom: var(--spacing-lg);
  border-bottom: 1px solid var(--gray-200);
}

.diary-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--spacing-xl);
  gap: var(--spacing-xl);
}

.date-info {
  flex: 1;
}

.diary-date {
  font-size: var(--font-4xl);
  font-weight: 700;
  color: var(--ease-dark);
  margin: 0 0 var(--spacing-sm) 0;
  line-height: 1.2;
}

.diary-time {
  font-size: var(--font-base);
  color: var(--gray-500);
  margin: 0;
}

.mood-badge {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem 1.5rem;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 1rem;
}

.mood-emoji {
  font-size: 3rem;
  line-height: 1;
}

.mood-info {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.mood-type {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--ease-dark);
}

.mood-score-bar {
  width: 8rem;
  height: 0.5rem;
  background: rgba(229, 231, 235, 0.6);
  border-radius: 9999px;
  overflow: hidden;
}

.mood-score-fill {
  height: 100%;
  background: linear-gradient(to right, var(--ease-accent), #10b981);
  border-radius: 9999px;
  transition: width 0.3s ease;
}

.mood-score-text {
  font-size: 0.875rem;
  color: #6b7280;
}

.tags-section {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
  margin-bottom: 2rem;
  padding: 1rem;
  background: rgba(123, 158, 137, 0.05);
  border-radius: 0.75rem;
}

.tags-section i {
  color: var(--ease-accent);
  font-size: 1rem;
}

.tag {
  padding: 0.375rem 0.875rem;
  background: rgba(123, 158, 137, 0.15);
  color: var(--ease-accent-dark);
  border-radius: 9999px;
  font-size: 0.875rem;
  font-weight: 500;
}

.diary-content {
  margin-bottom: 2rem;
  line-height: 2;
  font-size: 1.125rem;
  color: #374151;
  white-space: pre-wrap;
  word-break: break-word;
}

.ai-analysis {
  padding: 1.5rem;
  border-radius: 1rem;
  margin-bottom: 2rem;
}

.ai-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.ai-icon-wrapper {
  width: 2rem;
  height: 2rem;
  border-radius: 0.5rem;
  background: linear-gradient(135deg, var(--ease-accent), #10b981);
  display: flex;
  align-items: center;
  justify-content: center;
}

.ai-icon-wrapper i {
  color: white;
  font-size: 1rem;
}

.ai-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--ease-dark);
}

.ai-content {
  font-size: 0.9375rem;
  line-height: 1.75;
  color: #4b5563;
}

.metadata {
  display: flex;
  flex-wrap: wrap;
  gap: 1.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid rgba(229, 231, 235, 0.5);
}

.metadata-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: #6b7280;
}

.metadata-item i {
  color: var(--ease-accent);
}

.navigation-section {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}

.nav-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 1rem 1.5rem;
  border-radius: 1rem;
  border: none;
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--ease-dark);
  transition: all 0.2s ease;
}

.nav-btn:hover {
  background: rgba(255, 255, 255, 0.8);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.btn-primary {
  padding: 0.75rem 1.5rem;
  background: linear-gradient(135deg, var(--ease-accent), #10b981);
  color: white;
  border: none;
  border-radius: 0.75rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 12px rgba(123, 158, 137, 0.3);
  margin-top: 1rem;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(123, 158, 137, 0.4);
}

@media (max-width: 768px) {
  .mood-diary-detail-page {
    padding: 1rem;
  }

  .main-card {
    padding: 1.5rem;
  }

  .diary-header {
    flex-direction: column;
  }

  .header-actions {
    flex-wrap: wrap;
  }

  .navigation-section {
    flex-direction: column;
  }
}
</style>
