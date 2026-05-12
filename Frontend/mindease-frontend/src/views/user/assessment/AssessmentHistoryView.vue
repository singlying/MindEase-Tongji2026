<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div>
        <h1 class="page-title">测评历史</h1>
        <p class="page-subtitle">查看您的心理测评记录</p>
      </div>
      <button class="btn-accent" @click="router.push('/assessment')">
        <i class="fas fa-plus"></i>
        开始新测评
      </button>
    </div>

    <!-- 历史记录列表 -->
    <div v-loading="loading" class="records-list">
      <div
        v-for="record in records"
        :key="record.id"
        class="record-card glass-card"
        @click="viewDetail(record.id)"
      >
        <div class="record-header">
          <h3 class="record-title">{{ record.scaleTitle }}</h3>
          <div
            class="record-level"
            :class="`level-${getLevelClass(record.resultLevel)}`"
          >
            {{ record.resultLevel }}
          </div>
        </div>
        <div class="record-body">
          <div class="record-score">
            <span class="score-label">总分：</span>
            <span class="score-value">{{ record.totalScore }}</span>
          </div>
          <div class="record-time">
            <i class="fas fa-clock"></i>
            {{ formatTime(record.createTime) }}
          </div>
        </div>
        <div class="record-action">
          <i class="fas fa-chevron-right"></i>
        </div>
      </div>

      <!-- 空状态 -->
      <el-empty
        v-if="!loading && records.length === 0"
        description="暂无测评记录"
      >
        <button class="btn-accent" @click="router.push('/assessment')">
          开始第一次测评
        </button>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { getRecordList, type AssessmentRecord } from "@/api/assessment";

const router = useRouter();

const loading = ref(false);
const records = ref<AssessmentRecord[]>([]);

const fetchRecords = async () => {
  loading.value = true;
  try {
    const res = await getRecordList(20);
    records.value = res.data.records;
  } catch (error) {
    console.error("获取测评历史失败:", error);
    ElMessage.error("获取测评历史失败");
  } finally {
    loading.value = false;
  }
};

const getLevelClass = (resultLevel: string): string => {
  if (resultLevel.includes("正常")) return "normal";
  if (resultLevel.includes("轻度")) return "mild";
  if (resultLevel.includes("中度")) return "moderate";
  if (resultLevel.includes("重度")) return "severe";
  return "normal";
};

const formatTime = (time: string): string => {
  const date = new Date(time);
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  const days = Math.floor(diff / (1000 * 60 * 60 * 24));

  if (days === 0) {
    return (
      "今天 " +
      date.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" })
    );
  } else if (days === 1) {
    return (
      "昨天 " +
      date.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" })
    );
  } else if (days < 7) {
    return `${days}天前`;
  } else {
    return date.toLocaleDateString("zh-CN", {
      month: "2-digit",
      day: "2-digit",
    });
  }
};

const viewDetail = (recordId: number) => {
  router.push({
    name: "AssessmentResult",
    params: { recordId },
  });
};

onMounted(() => {
  fetchRecords();
});
</script>

<style scoped>
.page-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: var(--spacing-xl);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--spacing-xl);
}

.page-title {
  font-size: 2rem;
  font-weight: 700;
  color: var(--ease-dark);
  margin-bottom: var(--spacing-xs);
}

.page-subtitle {
  font-size: 1rem;
  color: var(--gray-500);
}

/* 记录列表 */
.records-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

/* 记录卡片 */
.record-card {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
  padding: var(--spacing-lg);
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
  border-radius: 1rem;
}

.record-card:hover {
  transform: translateX(4px);
  box-shadow: 0 8px 24px rgba(123, 158, 137, 0.12);
  border-color: var(--ease-accent);
}

.record-header {
  flex: 1;
  min-width: 0;
}

.record-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--ease-dark);
  margin-bottom: var(--spacing-xs);
}

.record-level {
  display: inline-block;
  padding: 0.25rem 0.75rem;
  border-radius: 1rem;
  font-size: 0.8125rem;
  font-weight: 600;
}

.level-normal {
  background: rgba(34, 197, 94, 0.1);
  color: #22c55e;
}

.level-mild {
  background: rgba(234, 179, 8, 0.1);
  color: #eab308;
}

.level-moderate {
  background: rgba(249, 115, 22, 0.1);
  color: #f97316;
}

.level-severe {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.record-body {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
}

.record-score {
  display: flex;
  align-items: baseline;
  gap: var(--spacing-xs);
}

.score-label {
  font-size: 0.875rem;
  color: var(--gray-500);
}

.score-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--ease-accent);
}

.record-time {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: 0.875rem;
  color: var(--gray-400);
}

.record-time i {
  font-size: 0.75rem;
}

.record-action {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(123, 158, 137, 0.1);
  color: var(--ease-accent);
  transition: all 0.3s ease;
}

.record-card:hover .record-action {
  background: var(--ease-accent);
  color: white;
}

@media (max-width: 768px) {
  .page-container {
    padding: var(--spacing-md);
  }

  .page-header {
    flex-direction: column;
    gap: var(--spacing-md);
  }

  .record-card {
    flex-wrap: wrap;
  }

  .record-body {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-xs);
  }
}
</style>
