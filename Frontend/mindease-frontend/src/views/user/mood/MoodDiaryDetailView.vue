<!-- 前端A负责：week2 情绪日记详情页首轮迁移 -->
<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";

import EmptyState from "@/components/common/EmptyState.vue";
import LoadingSpinner from "@/components/common/LoadingSpinner.vue";
import { deleteMoodLog, getMoodLogDetail, MOOD_TYPE_MAP, type MoodLogItem } from "@/api/mood";

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const diary = ref<MoodLogItem | null>(null);

async function loadDetail() {
  loading.value = true;
  try {
    const response = await getMoodLogDetail(Number(route.params.id));
    diary.value = response.data;
  } catch {
    diary.value = null;
  } finally {
    loading.value = false;
  }
}

async function handleDelete() {
  if (!diary.value) {
    return;
  }

  await ElMessageBox.confirm("确认删除这篇日记吗？", "删除确认", {
    type: "warning",
  });

  await deleteMoodLog(diary.value.id);
  ElMessage.success("删除成功");
  router.push("/mood-diary");
}

onMounted(() => {
  loadDetail();
});
</script>

<template>
  <LoadingSpinner v-if="loading" fullscreen text="正在加载日记详情..." />

  <div v-else class="detail-page">
    <section v-if="diary" class="detail-card glass-card">
      <div class="detail-actions">
        <el-button plain @click="router.push('/mood-diary')">返回列表</el-button>
        <el-button type="danger" plain @click="handleDelete">删除</el-button>
      </div>

      <div class="detail-head">
        <div>
          <div class="detail-date">
            {{ new Date(diary.logDate).toLocaleString("zh-CN") }}
          </div>
          <h2>{{ diary.emoji }} {{ MOOD_TYPE_MAP[diary.moodType].label }}</h2>
        </div>
        <div class="detail-score">{{ diary.moodScore }}/10</div>
      </div>

      <div class="tags" v-if="diary.tags.length">
        <span v-for="tag in diary.tags" :key="tag" class="tag">{{ tag }}</span>
      </div>

      <article class="content">
        {{ diary.content }}
      </article>

      <section v-if="diary.aiAnalysis" class="analysis">
        <h3>AI 分析</h3>
        <p>{{ diary.aiAnalysis }}</p>
      </section>
    </section>

    <EmptyState
      v-else
      title="没有找到这篇日记"
      description="可能是参数无效，或者这条记录已经被删除。"
    >
      <template #action>
        <el-button type="primary" @click="router.push('/mood-diary')">返回日记列表</el-button>
      </template>
    </EmptyState>
  </div>
</template>

<style scoped>
.detail-page {
  display: grid;
}

.detail-card {
  padding: 24px;
  display: grid;
  gap: 20px;
}

.detail-actions,
.detail-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.detail-date {
  color: var(--ease-muted);
  font-size: 14px;
}

h2,
h3,
p {
  margin: 0;
}

.detail-score {
  color: var(--ease-primary-dark);
  font-size: 28px;
  font-weight: 700;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(123, 158, 137, 0.12);
  color: var(--ease-primary-dark);
  font-size: 12px;
}

.content,
.analysis {
  line-height: 1.8;
}

.analysis {
  padding: 18px;
  border-radius: 18px;
  background: rgba(123, 158, 137, 0.08);
}

.analysis p {
  margin-top: 10px;
  color: var(--ease-muted);
}

@media (max-width: 800px) {
  .detail-actions,
  .detail-head {
    flex-direction: column;
  }
}
</style>
