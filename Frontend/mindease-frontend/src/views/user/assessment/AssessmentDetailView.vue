<template>
  <div class="assessment-detail">
    <div v-loading="loading" class="assessment-container glass-panel">
      <!-- 头部导航 -->
      <div class="assessment-header">
        <button @click="handleBack" class="back-btn">
          <i class="fas fa-arrow-left"></i>
          返回
        </button>
        <div class="header-info">
          <h2 class="scale-title">{{ scaleDetail?.title }}</h2>
          <p class="progress-text">
            问题 <span class="current">{{ currentQuestion + 1 }}</span> /
            {{ totalQuestions }}
          </p>
        </div>
        <button class="info-btn">
          <i class="fas fa-info-circle"></i>
        </button>
      </div>

      <!-- 进度条 -->
      <div class="progress-bar-container">
        <div
          class="progress-bar-fill"
          :style="{ width: progressPercent + '%' }"
        ></div>
      </div>

      <!-- 问题区域 -->
      <div v-if="scaleDetail" class="question-area">
        <div class="question-content">
          <h3 class="question-text">
            <span class="highlight">{{
              scaleDetail.questions[currentQuestion]?.text
            }}</span>
          </h3>

          <!-- 选项列表 -->
          <div class="options-list">
            <div
              v-for="(option, index) in scaleDetail.questions[currentQuestion]
                ?.options"
              :key="index"
              :class="[
                'option-card',
                { selected: selectedAnswer === option.score },
              ]"
              @click="selectOption(option.score)"
            >
              <div class="option-letter">
                {{ String.fromCharCode(65 + index) }}
              </div>
              <span class="option-label">{{ option.label }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部导航 -->
      <div class="assessment-footer">
        <button
          class="nav-btn prev-btn"
          :disabled="currentQuestion === 0"
          @click="prevQuestion"
        >
          <i class="fas fa-chevron-left"></i>
          上一题
        </button>

        <!-- 进度点 -->
        <div class="progress-dots">
          <div
            v-for="(_, index) in scaleDetail?.questions"
            :key="index"
            :class="[
              'dot',
              {
                active: index === currentQuestion,
                answered: answers[index] !== undefined,
              },
            ]"
          ></div>
        </div>

        <button
          v-if="currentQuestion < totalQuestions - 1"
          class="nav-btn next-btn"
          :disabled="selectedAnswer === null"
          @click="nextQuestion"
        >
          下一题
          <i class="fas fa-chevron-right"></i>
        </button>
        <button
          v-else
          class="nav-btn submit-btn"
          :disabled="!allAnswered"
          @click="submitAssessment"
        >
          提交测评
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  getScaleDetail,
  submitAssessment as submitAssessmentAPI,
  type ScaleDetailVO,
} from "@/api/assessment";

const router = useRouter();
const route = useRoute();

const loading = ref(false);
const scaleDetail = ref<ScaleDetailVO | null>(null);
const currentQuestion = ref(0);
const answers = ref<Record<number, number>>({});
const selectedAnswer = ref<number | null>(null);

const scaleKey = route.params.scaleKey as string;

const totalQuestions = computed(() => scaleDetail.value?.questions.length || 0);
const progressPercent = computed(
  () => ((currentQuestion.value + 1) / totalQuestions.value) * 100
);
const allAnswered = computed(() => {
  return Object.keys(answers.value).length === totalQuestions.value;
});

const fetchScaleDetail = async () => {
  loading.value = true;
  try {
    const res = await getScaleDetail(scaleKey);
    scaleDetail.value = res.data;

    const currentAnswer = answers.value[currentQuestion.value];
    if (currentAnswer !== undefined) {
      selectedAnswer.value = currentAnswer;
    }
  } catch (error) {
    console.error("获取量表详情失败:", error);
    ElMessage.error("获取量表详情失败");
    router.back();
  } finally {
    loading.value = false;
  }
};

const selectOption = (score: number) => {
  selectedAnswer.value = score;
  answers.value[currentQuestion.value] = score;
};

const nextQuestion = () => {
  if (selectedAnswer.value === null) {
    ElMessage.warning("请选择一个选项");
    return;
  }

  if (currentQuestion.value < totalQuestions.value - 1) {
    currentQuestion.value++;
    selectedAnswer.value = answers.value[currentQuestion.value] ?? null;
  }
};

const prevQuestion = () => {
  if (currentQuestion.value > 0) {
    currentQuestion.value--;
    const prevAnswer = answers.value[currentQuestion.value];
    selectedAnswer.value = prevAnswer !== undefined ? prevAnswer : null;
  }
};

const submitAssessment = async () => {
  if (!allAnswered.value) {
    ElMessage.warning("请完成所有题目");
    return;
  }

  try {
    await ElMessageBox.confirm("确认提交测评？提交后将无法修改答案。", "提示", {
      confirmButtonText: "确认提交",
      cancelButtonText: "再检查一下",
      type: "info",
    });

    loading.value = true;

    const answersArray = scaleDetail.value!.questions.map((q, index) => ({
      questionId: q.id,
      score: answers.value[index]!,
    }));

    const res = await submitAssessmentAPI({
      scaleKey,
      answers: answersArray,
    });

    ElMessage.success("测评提交成功");

    router.push({
      name: "AssessmentResult",
      params: { recordId: res.data.recordId },
    });
  } catch (error: any) {
    if (error !== "cancel") {
      console.error("提交测评失败:", error);
      ElMessage.error("提交测评失败");
    }
  } finally {
    loading.value = false;
  }
};

const handleBack = async () => {
  if (Object.keys(answers.value).length > 0) {
    try {
      await ElMessageBox.confirm("测评尚未完成，确认退出吗？", "提示", {
        confirmButtonText: "确认退出",
        cancelButtonText: "继续测评",
        type: "warning",
      });
      router.back();
    } catch {
      // 用户取消
    }
  } else {
    router.back();
  }
};

onMounted(() => {
  fetchScaleDetail();
});
</script>

<style scoped>
.assessment-detail {
  min-height: 100vh;
  padding: var(--spacing-xl);
  display: flex;
  align-items: center;
  justify-content: center;
}

.assessment-container {
  max-width: 900px;
  width: 100%;
  padding: var(--spacing-xl);
  border-radius: 2rem;
}

/* 头部 */
.assessment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
}

.back-btn,
.info-btn {
  background: none;
  border: none;
  color: var(--gray-500);
  cursor: pointer;
  padding: var(--spacing-sm);
  transition: color 0.3s ease;
}

.back-btn:hover,
.info-btn:hover {
  color: var(--ease-dark);
}

.back-btn {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

.header-info {
  text-align: center;
  flex: 1;
}

.scale-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--ease-dark);
  margin-bottom: var(--spacing-xs);
}

.progress-text {
  font-size: 0.875rem;
  color: var(--gray-500);
}

.progress-text .current {
  color: var(--ease-accent);
  font-weight: 600;
}

/* 进度条 */
.progress-bar-container {
  height: 6px;
  background: rgba(123, 158, 137, 0.1);
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: var(--spacing-xl);
}

.progress-bar-fill {
  height: 100%;
  background: linear-gradient(
    90deg,
    var(--ease-accent),
    var(--ease-accent-dark)
  );
  transition: width 0.3s ease;
  border-radius: 3px;
}

/* 问题区域 */
.question-area {
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--spacing-xl);
}

.question-content {
  width: 100%;
  max-width: 700px;
}

.question-text {
  font-size: 1.75rem;
  font-family: var(--font-serif);
  font-weight: 500;
  color: var(--ease-dark);
  line-height: 1.6;
  text-align: center;
  margin-bottom: var(--spacing-xl);
}

.question-text .highlight {
  color: var(--ease-warm);
}

/* 选项列表 */
.options-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.option-card {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-md) var(--spacing-lg);
  background: rgba(255, 255, 255, 0.4);
  border: 2px solid transparent;
  border-radius: 1rem;
  cursor: pointer;
  transition: all 0.3s ease;
}

.option-card:hover {
  background: rgba(255, 255, 255, 0.6);
  border-color: var(--ease-accent);
}

.option-card.selected {
  background: rgba(123, 158, 137, 0.1);
  border-color: var(--ease-accent);
}

.option-card.selected .option-letter {
  background: var(--ease-accent);
  color: white;
  border-color: var(--ease-accent);
}

.option-letter {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid var(--gray-300);
  border-radius: 0.5rem;
  font-weight: 700;
  color: var(--gray-500);
  transition: all 0.3s ease;
  flex-shrink: 0;
}

.option-label {
  font-size: 1.125rem;
  color: var(--gray-700);
}

/* 底部导航 */
.assessment-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: var(--spacing-lg);
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.nav-btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

.prev-btn {
  background: var(--gray-100);
  color: var(--gray-700);
}

.prev-btn:hover:not(:disabled) {
  background: var(--gray-200);
}

.next-btn,
.submit-btn {
  background: var(--ease-accent);
  color: white;
}

.next-btn:hover:not(:disabled),
.submit-btn:hover:not(:disabled) {
  background: var(--ease-accent-dark);
}

.nav-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 进度点 */
.progress-dots {
  display: flex;
  gap: var(--spacing-xs);
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--gray-300);
  transition: all 0.3s ease;
}

.dot.active {
  background: var(--ease-accent);
  transform: scale(1.2);
}

.dot.answered {
  background: var(--ease-accent);
  opacity: 0.5;
}

@media (max-width: 768px) {
  .assessment-detail {
    padding: var(--spacing-md);
  }

  .assessment-container {
    padding: var(--spacing-md);
  }

  .question-text {
    font-size: 1.25rem;
  }

  .option-label {
    font-size: 1rem;
  }

  .progress-dots {
    display: none;
  }
}
</style>
