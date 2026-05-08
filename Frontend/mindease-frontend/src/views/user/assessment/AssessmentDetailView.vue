<!-- 前端A负责：测评答题页 -->
<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";

import type { ScaleDetail } from "@/api/assessment";
import { getScaleDetail, submitAssessment } from "@/api/assessment";
import LoadingSpinner from "@/components/common/LoadingSpinner.vue";

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const submitting = ref(false);
const detail = ref<ScaleDetail | null>(null);
const currentIndex = ref(0);
const answers = ref<Record<number, number>>({});

const scaleKey = computed(() => String(route.params.scaleKey || ""));
const currentQuestion = computed(() => detail.value?.questions[currentIndex.value]);
const totalQuestions = computed(() => detail.value?.questions.length ?? 0);
const answeredCount = computed(() => Object.keys(answers.value).length);
const progress = computed(() => {
  if (!totalQuestions.value) {
    return 0;
  }

  return Math.round(((currentIndex.value + 1) / totalQuestions.value) * 100);
});
const selectedScore = computed(() => {
  const question = currentQuestion.value;
  return question ? answers.value[question.id] : undefined;
});
const canSubmit = computed(() => answeredCount.value === totalQuestions.value);

async function loadDetail() {
  loading.value = true;

  try {
    const response = await getScaleDetail(scaleKey.value);
    detail.value = response.data;
  } catch {
    ElMessage.error("测评内容加载失败，请稍后再试");
    router.push("/assessment");
  } finally {
    loading.value = false;
  }
}

function selectOption(score: number) {
  const question = currentQuestion.value;

  if (!question) {
    return;
  }

  answers.value[question.id] = score;
}

function prevQuestion() {
  if (currentIndex.value > 0) {
    currentIndex.value -= 1;
  }
}

function nextQuestion() {
  if (selectedScore.value === undefined) {
    ElMessage.warning("请先选择一个答案");
    return;
  }

  if (currentIndex.value < totalQuestions.value - 1) {
    currentIndex.value += 1;
  }
}

async function handleBack() {
  if (answeredCount.value === 0) {
    router.push("/assessment");
    return;
  }

  try {
    await ElMessageBox.confirm("当前测评尚未提交，确认返回列表吗？", "提示", {
      confirmButtonText: "确认返回",
      cancelButtonText: "继续作答",
      type: "warning",
    });
    router.push("/assessment");
  } catch {
    // 用户继续作答
  }
}

async function handleSubmit() {
  if (!detail.value || !canSubmit.value) {
    ElMessage.warning("请完成所有题目后再提交");
    return;
  }

  submitting.value = true;

  try {
    const response = await submitAssessment({
      scaleKey: detail.value.scaleKey,
      answers: detail.value.questions.map((question) => ({
        questionId: question.id,
        score: answers.value[question.id]!,
      })),
    });

    ElMessage.success("测评已提交");
    router.push(`/assessment/result/${response.data.recordId}`);
  } catch {
    ElMessage.error("提交失败，请稍后再试");
  } finally {
    submitting.value = false;
  }
}

onMounted(() => {
  loadDetail();
});
</script>

<template>
  <div class="assessment-detail-page">
    <LoadingSpinner v-if="loading" text="正在加载测评内容" />

    <section v-else-if="detail && currentQuestion" class="answer-card glass-card">
      <header class="answer-head">
        <el-button plain @click="handleBack">返回</el-button>
        <div class="head-center">
          <div class="eyebrow">{{ detail.title }}</div>
          <h2>{{ currentQuestion.text }}</h2>
          <p>{{ detail.instruction }}</p>
        </div>
        <div class="question-count">
          {{ currentIndex + 1 }} / {{ totalQuestions }}
        </div>
      </header>

      <el-progress :percentage="progress" :show-text="false" />

      <div class="options-list">
        <button
          v-for="(option, index) in currentQuestion.options"
          :key="option.label"
          :class="['option-item', { selected: selectedScore === option.score }]"
          @click="selectOption(option.score)"
        >
          <span class="option-code">{{ String.fromCharCode(65 + index) }}</span>
          <span>{{ option.label }}</span>
        </button>
      </div>

      <footer class="answer-footer">
        <el-button :disabled="currentIndex === 0" @click="prevQuestion">
          上一题
        </el-button>

        <div class="answered-state">
          已完成 {{ answeredCount }} / {{ totalQuestions }}
        </div>

        <el-button
          v-if="currentIndex < totalQuestions - 1"
          type="primary"
          :disabled="selectedScore === undefined"
          @click="nextQuestion"
        >
          下一题
        </el-button>
        <el-button
          v-else
          type="primary"
          :loading="submitting"
          :disabled="!canSubmit"
          @click="handleSubmit"
        >
          提交测评
        </el-button>
      </footer>
    </section>
  </div>
</template>

<style scoped>
.assessment-detail-page {
  min-height: calc(100vh - 120px);
  display: grid;
  place-items: center;
}

.answer-card {
  width: min(920px, 100%);
  padding: 28px;
  display: grid;
  gap: 26px;
}

.answer-head {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 18px;
  align-items: start;
}

.head-center {
  text-align: center;
}

.eyebrow {
  color: var(--ease-primary-dark);
  font-size: 13px;
  font-weight: 700;
}

h2,
p {
  margin: 0;
}

h2 {
  margin-top: 10px;
  font-size: 26px;
  line-height: 1.5;
}

p {
  margin-top: 10px;
  color: var(--ease-muted);
}

.question-count {
  color: var(--ease-primary-dark);
  font-weight: 800;
}

.options-list {
  display: grid;
  gap: 14px;
}

.option-item {
  width: 100%;
  border: 1px solid rgba(123, 158, 137, 0.16);
  background: rgba(255, 255, 255, 0.72);
  border-radius: 16px;
  padding: 16px 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  color: var(--ease-text);
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease;
}

.option-item:hover,
.option-item.selected {
  border-color: var(--ease-primary);
  background: rgba(123, 158, 137, 0.12);
}

.option-code {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: rgba(123, 158, 137, 0.12);
  color: var(--ease-primary-dark);
  font-weight: 800;
  flex: 0 0 auto;
}

.option-item.selected .option-code {
  background: var(--ease-primary);
  color: #fff;
}

.answer-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.answered-state {
  color: var(--ease-muted);
  font-weight: 700;
}

@media (max-width: 720px) {
  .answer-card {
    padding: 20px;
  }

  .answer-head {
    grid-template-columns: 1fr;
  }

  .head-center {
    text-align: left;
  }
}
</style>
