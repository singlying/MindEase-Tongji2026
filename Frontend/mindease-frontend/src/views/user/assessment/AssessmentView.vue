<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div>
        <h1 class="page-title">心理健康测评</h1>
        <p class="page-subtitle">科学评估，了解自己的心理状态</p>
      </div>
      <button class="btn-secondary" @click="router.push('/assessment/history')">
        <i class="fas fa-history"></i>
        测评历史
      </button>
    </div>

    <!-- 量表列表 -->
    <div v-loading="loading" class="scales-grid">
      <div
        v-for="scale in scales"
        :key="scale.scaleKey"
        class="scale-card glass-card"
        @click="startAssessment(scale.scaleKey)"
      >
        <div class="scale-cover">
          <img :src="scale.coverUrl" :alt="scale.title" />
        </div>
        <div class="scale-content">
          <h3 class="scale-title">{{ scale.title }}</h3>
          <p class="scale-description">{{ scale.description }}</p>
        </div>
        <div class="scale-action">
          <i class="fas fa-arrow-right"></i>
        </div>
      </div>

      <!-- 空状态 -->
      <el-empty
        v-if="!loading && scales.length === 0"
        description="暂无可用量表"
      />
    </div>

    <!-- 温馨提示 -->
    <div class="tips-card glass-card">
      <div class="tips-header">
        <i class="fas fa-lightbulb"></i>
        <span>温馨提示</span>
      </div>
      <ul class="tips-list">
        <li>请在安静、不受打扰的环境中完成测评</li>
        <li>根据最近两周的实际感受如实作答</li>
        <li>测评结果仅供参考，不能替代专业诊断</li>
        <li>如有严重心理问题，请及时寻求专业帮助</li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { getScaleList, type Scale } from "@/api/assessment";

const router = useRouter();

const loading = ref(false);
const scales = ref<Scale[]>([]);

const fetchScales = async () => {
  loading.value = true;
  try {
    const res = await getScaleList();
    scales.value = res.data.scales;
  } catch (error) {
    console.error("获取量表列表失败:", error);
    ElMessage.error("获取量表列表失败");
  } finally {
    loading.value = false;
  }
};

const startAssessment = (scaleKey: string) => {
  router.push(`/assessment/${scaleKey}`);
};

onMounted(() => {
  fetchScales();
});
</script>

<style scoped>
.page-container {
  max-width: 1200px;
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

/* 量表网格 */
.scales-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-xl);
}

/* 量表卡片 */
.scale-card {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
  padding: var(--spacing-lg);
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
  border-radius: 1rem;
}

.scale-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(123, 158, 137, 0.15);
  border-color: var(--ease-accent);
}

.scale-cover {
  width: 80px;
  height: 80px;
  border-radius: 0.75rem;
  overflow: hidden;
  flex-shrink: 0;
}

.scale-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.scale-content {
  flex: 1;
  min-width: 0;
}

.scale-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--ease-dark);
  margin-bottom: var(--spacing-xs);
}

.scale-description {
  font-size: 0.875rem;
  color: var(--gray-500);
  margin-bottom: var(--spacing-sm);
  line-height: 1.5;
}

.scale-action {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(123, 158, 137, 0.1);
  color: var(--ease-accent);
  transition: all 0.3s ease;
}

.scale-card:hover .scale-action {
  background: var(--ease-accent);
  color: white;
}

/* 提示卡片 */
.tips-card {
  padding: var(--spacing-lg);
  border-radius: 1rem;
}

.tips-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: 1rem;
  font-weight: 600;
  color: var(--ease-dark);
  margin-bottom: var(--spacing-md);
}

.tips-header i {
  color: var(--ease-warm);
}

.tips-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.tips-list li {
  position: relative;
  padding-left: var(--spacing-lg);
  margin-bottom: var(--spacing-sm);
  font-size: 0.875rem;
  color: var(--gray-600);
  line-height: 1.6;
}

.tips-list li::before {
  content: "•";
  position: absolute;
  left: 0;
  color: var(--ease-accent);
  font-weight: bold;
}

.tips-list li:last-child {
  margin-bottom: 0;
}

@media (max-width: 768px) {
  .page-container {
    padding: var(--spacing-md);
  }

  .page-header {
    flex-direction: column;
    gap: var(--spacing-md);
  }

  .scales-grid {
    grid-template-columns: 1fr;
  }
}
</style>
