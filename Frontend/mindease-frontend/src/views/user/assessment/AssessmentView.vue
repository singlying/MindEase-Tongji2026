<!-- 前端A负责：心理测评列表 -->
<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";

import type { Scale } from "@/api/assessment";
import { getScaleList } from "@/api/assessment";
import EmptyState from "@/components/common/EmptyState.vue";
import LoadingSpinner from "@/components/common/LoadingSpinner.vue";

const router = useRouter();

const loading = ref(false);
const scales = ref<Scale[]>([]);

async function loadScales() {
  loading.value = true;

  try {
    const response = await getScaleList();
    scales.value = response.data.scales.filter((item: Scale) => item.status === "active");
  } catch {
    ElMessage.error("测评列表加载失败，请稍后再试");
  } finally {
    loading.value = false;
  }
}

function startAssessment(scaleKey: string) {
  router.push(`/assessment/${scaleKey}`);
}

onMounted(() => {
  loadScales();
});
</script>

<template>
  <div class="assessment-page">
    <section class="page-head">
      <div>
        <div class="eyebrow">心理测评</div>
        <h2>选择适合当前状态的量表</h2>
        <p>通过标准化问卷了解近期心理状态，结果仅作为自我观察参考。</p>
      </div>
      <el-button plain @click="router.push('/assessment/history')">
        测评历史
      </el-button>
    </section>

    <LoadingSpinner v-if="loading" text="正在加载测评量表" />

    <EmptyState
      v-else-if="scales.length === 0"
      title="暂无可用量表"
      description="稍后再回来看看，新的测评内容会持续补充。"
    />

    <section v-else class="scale-grid">
      <article
        v-for="scale in scales"
        :key="scale.scaleKey"
        class="scale-card glass-card"
        @click="startAssessment(scale.scaleKey)"
      >
        <div class="scale-mark" :style="{ backgroundColor: scale.coverColor }">
          {{ scale.title.slice(0, 3) }}
        </div>
        <div class="scale-body">
          <h3>{{ scale.title }}</h3>
          <p>{{ scale.description }}</p>
          <div class="scale-meta">
            <span>{{ scale.questionCount }} 题</span>
            <span>约 {{ scale.estimatedMinutes }} 分钟</span>
          </div>
        </div>
        <el-button type="primary" plain>开始</el-button>
      </article>
    </section>

    <section class="notice-card glass-card">
      <h3>作答提示</h3>
      <div class="notice-list">
        <span>选择安静、不受打扰的环境</span>
        <span>根据最近一段时间的真实感受作答</span>
        <span>结果不能替代专业诊断</span>
      </div>
    </section>
  </div>
</template>

<style scoped>
.assessment-page {
  display: grid;
  gap: 20px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.eyebrow {
  color: var(--ease-primary-dark);
  font-size: 13px;
  font-weight: 700;
}

h2,
h3,
p {
  margin: 0;
}

h2 {
  margin-top: 6px;
  font-size: 30px;
}

p {
  margin-top: 8px;
  color: var(--ease-muted);
  line-height: 1.7;
}

.scale-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.scale-card {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 16px;
  padding: 22px;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease;
}

.scale-card:hover {
  transform: translateY(-2px);
  border-color: rgba(95, 122, 106, 0.35);
}

.scale-mark {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  color: #fff;
  font-weight: 700;
}

.scale-body {
  min-width: 0;
}

.scale-body h3 {
  font-size: 17px;
}

.scale-meta {
  margin-top: 14px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  color: var(--ease-primary-dark);
  font-size: 13px;
  font-weight: 700;
}

.notice-card {
  padding: 22px;
}

.notice-list {
  margin-top: 14px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  color: var(--ease-muted);
}

.notice-list span {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(123, 158, 137, 0.08);
}

@media (max-width: 1100px) {
  .scale-grid {
    grid-template-columns: 1fr;
  }

  .scale-card {
    grid-template-columns: auto 1fr;
  }

  .scale-card .el-button {
    grid-column: 1 / -1;
  }
}

@media (max-width: 720px) {
  .page-head {
    display: grid;
  }
}
</style>
