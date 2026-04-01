<!-- 前端A负责：week2 情绪日记页首轮迁移 -->
<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";

import EmptyState from "@/components/common/EmptyState.vue";
import LoadingSpinner from "@/components/common/LoadingSpinner.vue";
import MoodCard from "@/components/common/MoodCard.vue";
import MoodSelector from "@/components/common/MoodSelector.vue";
import { createMoodLog, getMoodLogs, type MoodLogItem, type MoodType } from "@/api/mood";

const router = useRouter();

const logs = ref<MoodLogItem[]>([]);
const loading = ref(false);
const submitting = ref(false);

const formData = ref<{
  moodType: MoodType;
  moodScore: number;
  content: string;
}>({
  moodType: "Calm",
  moodScore: 6,
  content: "",
});

const todayLabel = computed(() => {
  const now = new Date();
  return `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日`;
});

async function loadLogs() {
  loading.value = true;
  try {
    const response = await getMoodLogs(8, 0);
    logs.value = response.data.logs;
  } finally {
    loading.value = false;
  }
}

async function submitDiary() {
  if (!formData.value.content.trim()) {
    ElMessage.warning("先写一点今天的感受再提交吧。");
    return;
  }

  submitting.value = true;

  try {
    await createMoodLog({
      moodType: formData.value.moodType,
      moodScore: formData.value.moodScore,
      content: formData.value.content,
      tags: [],
      logDate: new Date().toISOString(),
    });

    ElMessage.success("日记已记录。");
    formData.value.content = "";
    formData.value.moodType = "Calm";
    formData.value.moodScore = 6;
    await loadLogs();
  } finally {
    submitting.value = false;
  }
}

function viewDetail(id: number) {
  router.push(`/mood-diary/${id}`);
}

onMounted(() => {
  loadLogs();
});
</script>

<template>
  <div class="mood-page">
    <LoadingSpinner v-if="submitting" fullscreen text="正在保存这次情绪记录..." />

    <section class="editor glass-card">
      <div class="section-head">
        <div>
          <div class="eyebrow">情绪记录</div>
          <h2>记录今天的感受</h2>
          <p>{{ todayLabel }}，给今天的情绪留下一点空间。</p>
        </div>
        <el-button plain @click="router.push('/home')">返回首页</el-button>
      </div>

      <MoodSelector v-model="formData.moodType" />

      <div class="score-block">
        <div class="score-label">情绪强度：{{ formData.moodScore }}/10</div>
        <el-slider
          v-model="formData.moodScore"
          :min="1"
          :max="10"
          :show-tooltip="false"
        />
      </div>

      <el-input
        v-model="formData.content"
        type="textarea"
        :rows="7"
        resize="none"
        placeholder="写下今天发生了什么，你最在意的感受是什么。"
      />

      <div class="editor-actions">
        <el-button type="primary" @click="submitDiary">提交日记</el-button>
      </div>
    </section>

    <section class="history glass-card">
      <div class="section-head">
        <div>
          <h3>最近记录</h3>
          <p>回顾最近写下的内容，看看这几天的状态变化。</p>
        </div>
      </div>

      <LoadingSpinner v-if="loading" text="正在加载最近记录..." />

      <EmptyState
        v-else-if="!logs.length"
        title="还没有情绪记录"
        description="先提交第一篇日记，后面我们再逐步补齐筛选、编辑与更完整的分析展示。"
      />

      <div v-else class="history-grid">
        <MoodCard
          v-for="item in logs"
          :key="item.id"
          :mood="item"
          @click="viewDetail"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
.mood-page {
  display: grid;
  gap: 20px;
}

.editor,
.history {
  padding: 24px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
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

p {
  margin-top: 8px;
  color: var(--ease-muted);
  line-height: 1.7;
}

.score-block {
  margin: 6px 0 14px;
}

.score-label {
  margin-bottom: 10px;
  font-weight: 600;
}

.editor-actions {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}

.history-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 14px;
}

@media (max-width: 800px) {
  .section-head {
    flex-direction: column;
  }
}
</style>
