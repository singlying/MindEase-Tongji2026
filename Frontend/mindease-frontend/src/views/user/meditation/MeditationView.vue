<!-- 前端A负责：冥想时刻页 -->
<script setup lang="ts">
import { computed, onUnmounted, ref } from "vue";
import { ElMessage } from "element-plus";
import {
  Close,
  Clock,
  Headset,
  Refresh,
  VideoPause,
  VideoPlay,
} from "@element-plus/icons-vue";

interface DurationOption {
  label: string;
  value: number;
  desc: string;
}

interface BreathStep {
  title: string;
  hint: string;
}

const durationOptions: DurationOption[] = [
  { label: "3 分钟", value: 180, desc: "快速回到当下" },
  { label: "5 分钟", value: 300, desc: "适合课间放松" },
  { label: "10 分钟", value: 600, desc: "完整呼吸练习" },
];

const defaultBreathStep: BreathStep = {
  title: "吸气",
  hint: "慢慢吸气，感受胸腔展开",
};

const breathSteps: BreathStep[] = [
  defaultBreathStep,
  { title: "停留", hint: "轻轻停住，不需要用力" },
  { title: "呼气", hint: "缓慢呼出，把紧绷交给地面" },
  { title: "放松", hint: "保持自然呼吸，观察身体变化" },
];

const selectedDuration = ref(300);
const timeLeft = ref(300);
const isRunning = ref(false);
const isPaused = ref(false);
const breathIndex = ref(0);

let timer: number | undefined;
let breathTimer: number | undefined;

const currentStep = computed<BreathStep>(
  () => breathSteps[breathIndex.value] ?? defaultBreathStep,
);
const progress = computed(() => {
  const used = selectedDuration.value - timeLeft.value;
  return Math.min(100, Math.round((used / selectedDuration.value) * 100));
});

const formattedTime = computed(() => {
  const minutes = Math.floor(timeLeft.value / 60);
  const seconds = timeLeft.value % 60;

  return `${minutes.toString().padStart(2, "0")}:${seconds
    .toString()
    .padStart(2, "0")}`;
});

function clearTimers() {
  if (timer) {
    window.clearInterval(timer);
    timer = undefined;
  }

  if (breathTimer) {
    window.clearInterval(breathTimer);
    breathTimer = undefined;
  }
}

function runTimers() {
  clearTimers();

  timer = window.setInterval(() => {
    if (timeLeft.value <= 1) {
      timeLeft.value = 0;
      clearTimers();
      isRunning.value = false;
      isPaused.value = false;
      ElMessage.success("练习完成，愿这份平静多停留一会儿");
      return;
    }

    timeLeft.value -= 1;
  }, 1000);

  breathTimer = window.setInterval(() => {
    breathIndex.value = (breathIndex.value + 1) % breathSteps.length;
  }, 4000);
}

function selectDuration(value: number) {
  if (isRunning.value) {
    return;
  }

  selectedDuration.value = value;
  timeLeft.value = value;
}

function startSession() {
  timeLeft.value = selectedDuration.value;
  breathIndex.value = 0;
  isRunning.value = true;
  isPaused.value = false;
  runTimers();
}

function togglePause() {
  if (!isRunning.value) {
    startSession();
    return;
  }

  isPaused.value = !isPaused.value;

  if (isPaused.value) {
    clearTimers();
  } else {
    runTimers();
  }
}

function stopSession() {
  clearTimers();
  isRunning.value = false;
  isPaused.value = false;
  breathIndex.value = 0;
  timeLeft.value = selectedDuration.value;
}

function restartSession() {
  stopSession();
  startSession();
}

onUnmounted(() => {
  clearTimers();
});
</script>

<template>
  <div class="meditation-page">
    <section class="page-head">
      <div>
        <div class="eyebrow">冥想时刻</div>
        <h2>用几分钟，把注意力带回身体</h2>
        <p>选择一个合适的时长，跟随呼吸节奏完成一段温和的放松练习。</p>
      </div>
      <div class="head-badge">
        <el-icon><Headset /></el-icon>
        安静环境体验更佳
      </div>
    </section>

    <section class="meditation-shell glass-card">
      <aside class="settings-panel">
        <h3>
          <el-icon><Clock /></el-icon>
          选择练习时长
        </h3>
        <button
          v-for="option in durationOptions"
          :key="option.value"
          type="button"
          class="duration-card"
          :class="{ active: selectedDuration === option.value }"
          :disabled="isRunning"
          @click="selectDuration(option.value)"
        >
          <span>{{ option.label }}</span>
          <small>{{ option.desc }}</small>
        </button>

        <div class="practice-tips">
          <strong>练习提示</strong>
          <p>肩膀自然放松，双脚踩稳地面。念头出现时，不需要压制，只要轻轻回到呼吸。</p>
        </div>
      </aside>

      <main class="breath-stage">
        <div class="breath-orbit" :class="{ active: isRunning && !isPaused }">
          <div class="orbit-ring ring-one"></div>
          <div class="orbit-ring ring-two"></div>
          <div class="breath-core">
            <span>{{ currentStep.title }}</span>
            <strong>{{ formattedTime }}</strong>
            <small>{{ isPaused ? "已暂停" : currentStep.hint }}</small>
          </div>
        </div>

        <el-progress
          :percentage="progress"
          :stroke-width="10"
          :show-text="false"
          class="session-progress"
        />

        <div class="controls">
          <el-button circle class="control-btn" :disabled="!isRunning" @click="stopSession">
            <el-icon><Close /></el-icon>
          </el-button>
          <el-button type="primary" circle class="main-control" @click="togglePause">
            <el-icon v-if="!isRunning || isPaused"><VideoPlay /></el-icon>
            <el-icon v-else><VideoPause /></el-icon>
          </el-button>
          <el-button circle class="control-btn" :disabled="!isRunning" @click="restartSession">
            <el-icon><Refresh /></el-icon>
          </el-button>
        </div>
      </main>
    </section>
  </div>
</template>

<style scoped>
.meditation-page {
  display: grid;
  gap: 20px;
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.eyebrow {
  color: var(--ease-primary-dark);
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.08em;
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

.page-head p {
  margin-top: 8px;
  color: var(--ease-muted);
  line-height: 1.7;
}

.head-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(123, 158, 137, 0.1);
  color: var(--ease-primary-dark);
  font-weight: 800;
}

.meditation-shell {
  min-height: 620px;
  display: grid;
  grid-template-columns: 310px 1fr;
  overflow: hidden;
}

.settings-panel {
  padding: 26px;
  border-right: 1px solid var(--ease-border);
  background:
    radial-gradient(circle at top, rgba(123, 158, 137, 0.14), transparent 36%),
    rgba(255, 255, 255, 0.54);
}

.settings-panel h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 18px;
}

.duration-card {
  width: 100%;
  display: grid;
  gap: 6px;
  margin-bottom: 12px;
  padding: 16px;
  border: 1px solid rgba(123, 158, 137, 0.18);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--ease-text);
  cursor: pointer;
  text-align: left;
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.duration-card:not(:disabled):hover,
.duration-card.active {
  transform: translateY(-2px);
  border-color: rgba(95, 122, 106, 0.42);
  background: rgba(123, 158, 137, 0.12);
}

.duration-card span {
  font-size: 18px;
  font-weight: 800;
}

.duration-card small,
.practice-tips p {
  color: var(--ease-muted);
  line-height: 1.7;
}

.practice-tips {
  margin-top: 26px;
  padding: 18px;
  border-radius: 18px;
  background: rgba(255, 247, 237, 0.72);
}

.practice-tips strong {
  color: #8b604f;
}

.practice-tips p {
  margin-top: 8px;
}

.breath-stage {
  min-width: 0;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 28px;
  padding: 34px;
  background:
    radial-gradient(circle at 54% 42%, rgba(123, 158, 137, 0.2), transparent 30%),
    radial-gradient(circle at 70% 66%, rgba(196, 124, 107, 0.12), transparent 24%);
}

.breath-orbit {
  position: relative;
  width: min(370px, 72vw);
  aspect-ratio: 1;
  display: grid;
  place-items: center;
}

.orbit-ring {
  position: absolute;
  inset: 10%;
  border-radius: 50%;
  border: 1px solid rgba(123, 158, 137, 0.2);
  background: rgba(255, 255, 255, 0.36);
  box-shadow: 0 28px 80px rgba(68, 91, 78, 0.16);
}

.ring-two {
  inset: 20%;
  background: rgba(255, 255, 255, 0.52);
}

.breath-orbit.active .ring-one {
  animation: breathe 8s ease-in-out infinite;
}

.breath-orbit.active .ring-two {
  animation: breathe 8s ease-in-out infinite reverse;
}

.breath-core {
  position: relative;
  z-index: 1;
  width: 58%;
  aspect-ratio: 1;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 12px;
  border-radius: 50%;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.96), rgba(246, 250, 247, 0.82));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9), 0 24px 60px rgba(47, 65, 57, 0.12);
  text-align: center;
}

.breath-core span {
  color: var(--ease-primary-dark);
  font-weight: 900;
  letter-spacing: 0.12em;
}

.breath-core strong {
  font-size: 42px;
}

.breath-core small {
  max-width: 180px;
  color: var(--ease-muted);
  line-height: 1.6;
}

.session-progress {
  width: min(460px, 80%);
}

.controls {
  display: flex;
  align-items: center;
  gap: 16px;
}

.control-btn {
  width: 48px;
  height: 48px;
}

.main-control {
  width: 66px;
  height: 66px;
  font-size: 24px;
  box-shadow: 0 18px 34px rgba(95, 122, 106, 0.28);
}

@keyframes breathe {
  0%,
  100% {
    transform: scale(0.86);
    opacity: 0.72;
  }

  50% {
    transform: scale(1.08);
    opacity: 1;
  }
}

@media (max-width: 960px) {
  .meditation-shell {
    grid-template-columns: 1fr;
  }

  .settings-panel {
    border-right: 0;
    border-bottom: 1px solid var(--ease-border);
  }
}

@media (max-width: 720px) {
  .page-head {
    display: grid;
  }

  .head-badge {
    justify-content: center;
  }
}
</style>
