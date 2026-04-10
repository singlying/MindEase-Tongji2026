<template>
  <div class="result-container">
    <div v-loading="loading" class="result-card glass-panel">
      <!-- 结果头部 -->
      <div class="result-header">
        <div class="result-icon">
          <i :class="resultIconClass"></i>
        </div>
        <h2 class="result-title">测评完成</h2>
        <p class="result-subtitle">{{ recordDetail?.scaleTitle }}</p>
      </div>

      <!-- 分数展示 -->
      <div class="score-section">
        <div class="score-display">
          <div class="score-number">{{ recordDetail?.totalScore }}</div>
          <div class="score-label">总分</div>
        </div>
        <div class="level-badge" :class="`level-${levelClass}`">
          {{ recordDetail?.resultLevel }}
        </div>
      </div>

      <!-- 结果解读 -->
      <div class="interpretation-section">
        <h3 class="section-title">
          <i class="fas fa-lightbulb"></i>
          结果解读与建议
        </h3>
        <p class="interpretation-text">{{ recordDetail?.resultDesc }}</p>
      </div>

      <!-- 操作按钮 -->
      <div class="actions-section">
        <button class="btn-secondary" @click="router.push('/assessment')">
          <i class="fas fa-redo"></i>
          再测一次
        </button>
        <button class="btn-accent" @click="router.push('/assessment/history')">
          <i class="fas fa-history"></i>
          查看历史
        </button>
        <button class="btn-secondary" @click="router.push('/counselor-list')">
          <i class="fas fa-user-doctor"></i>
          寻求帮助
        </button>
      </div>

      <!-- 温馨提示 -->
      <div class="tips-box">
        <i class="fas fa-info-circle"></i>
        <span
          >测评结果仅供参考，不能替代专业诊断。如有严重心理问题，请及时寻求专业帮助。</span
        >
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import {
  getRecordDetail,
  type AssessmentRecordDetailVO,
} from "@/api/assessment";

const router = useRouter();
const route = useRoute();

const loading = ref(false);
const recordDetail = ref<AssessmentRecordDetailVO | null>(null);

const recordId = Number(route.params.recordId);

const levelClass = computed(() => {
  const level = recordDetail.value?.resultLevel;
  if (!level) return "normal";
  if (level.includes("正常")) return "normal";
  if (level.includes("轻度")) return "mild";
  if (level.includes("中度")) return "moderate";
  if (level.includes("重度")) return "severe";
  return "normal";
});

const resultIconClass = computed(() => {
  switch (levelClass.value) {
    case "normal":
      return "fas fa-smile text-green-500";
    case "mild":
      return "fas fa-meh text-yellow-500";
    case "moderate":
      return "fas fa-frown text-orange-500";
    case "severe":
      return "fas fa-sad-tear text-red-500";
    default:
      return "fas fa-smile";
  }
});

const fetchRecordDetail = async () => {
  loading.value = true;
  try {
    const res = await getRecordDetail(recordId);
    recordDetail.value = res.data;
  } catch (error) {
    console.error("获取测评结果失败:", error);
    ElMessage.error("获取测评结果失败");
    router.push("/assessment");
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  fetchRecordDetail();
});
</script>

<style scoped>
.result-container {
  min-height: 100vh;
  padding: var(--spacing-xl);
  display: flex;
  align-items: center;
  justify-content: center;
}

.result-card {
  max-width: 700px;
  width: 100%;
  padding: var(--spacing-xl);
  border-radius: 1.5rem;
}

/* 结果头部 */
.result-header {
  text-align: center;
  margin-bottom: var(--spacing-xl);
}

.result-icon {
  font-size: 4rem;
  margin-bottom: var(--spacing-md);
}

.result-title {
  font-size: 2rem;
  font-weight: 700;
  color: var(--ease-dark);
  margin-bottom: var(--spacing-xs);
}

.result-subtitle {
  font-size: 1rem;
  color: var(--gray-500);
}

/* 分数展示 */
.score-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-lg);
  padding: var(--spacing-xl) 0;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  margin-bottom: var(--spacing-xl);
}

.score-display {
  text-align: center;
}

.score-number {
  font-size: 4rem;
  font-weight: 700;
  color: var(--ease-accent);
  line-height: 1;
  margin-bottom: var(--spacing-xs);
}

.score-label {
  font-size: 1rem;
  color: var(--gray-500);
}

.level-badge {
  padding: 0.5rem 1.5rem;
  border-radius: 2rem;
  font-size: 1.125rem;
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

/* 解读和建议 */
.interpretation-section,
.suggestions-section {
  margin-bottom: var(--spacing-xl);
}

.section-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--ease-dark);
  margin-bottom: var(--spacing-md);
}

.section-title i {
  color: var(--ease-accent);
}

.interpretation-text {
  font-size: 1rem;
  color: var(--gray-600);
  line-height: 1.8;
}

.suggestions-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.suggestions-list li {
  position: relative;
  padding-left: var(--spacing-lg);
  margin-bottom: var(--spacing-sm);
  font-size: 0.9375rem;
  color: var(--gray-600);
  line-height: 1.6;
}

.suggestions-list li::before {
  content: "✓";
  position: absolute;
  left: 0;
  color: var(--ease-accent);
  font-weight: bold;
}

/* 操作按钮 */
.actions-section {
  display: flex;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
}

.actions-section button {
  flex: 1;
}

/* 提示框 */
.tips-box {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-sm);
  padding: var(--spacing-md);
  background: rgba(123, 158, 137, 0.05);
  border-radius: 0.75rem;
  font-size: 0.875rem;
  color: var(--gray-600);
  line-height: 1.6;
}

.tips-box i {
  color: var(--ease-accent);
  margin-top: 2px;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .result-container {
    padding: var(--spacing-md);
  }

  .result-card {
    padding: var(--spacing-md);
  }

  .score-number {
    font-size: 3rem;
  }

  .actions-section {
    flex-direction: column;
  }
}
</style>
