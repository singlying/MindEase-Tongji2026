<template>
  <div class="meditation-page">
    <!-- 噪音纹理背景 -->
    <div class="noise-overlay"></div>

    <!-- 背景装饰球 -->
    <div class="bg-orbs">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
    </div>

    <!-- 头部 -->
    <transition name="fade">
      <header v-if="!isActive" class="page-header">
        <div class="header-info">
          <h1 class="page-title">Meditation</h1>
          <span class="page-subtitle">给心灵放个假</span>
        </div>
        <!-- 返回首页按钮：使用“返回”图标避免与设置齿轮含义混淆 -->
        <el-button circle class="setting-btn" @click="goBack" title="返回首页">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
      </header>
    </transition>

    <main class="main-content">
      <!-- 设置视图 -->
      <transition name="slide-up" mode="out-in">
        <div v-if="!isActive" class="setup-view" key="setup">
          <!-- 左侧：视觉引导卡片 -->
          <div class="start-card" @click="startSession">
            <div class="card-bg-blur"></div>
            <div class="card-inner glass-panel">
              <!-- 装饰圆环 -->
              <div class="decorative-rings">
                <div class="ring ring-1"></div>
                <div class="ring ring-2"></div>
              </div>

              <div class="play-btn">
                <el-icon><VideoPlay /></el-icon>
              </div>
              <h2 class="start-title">开始冥想</h2>
              <p class="start-desc">点击进入心流状态</p>
            </div>
          </div>

          <!-- 右侧：参数选择 -->
          <div class="options-card glass-panel">
            <h3 class="options-title">
              <el-icon><Clock /></el-icon>
              选择时长
            </h3>
            <div class="time-options">
              <button
                v-for="time in timeOptions"
                :key="time.value"
                :class="['time-btn', { active: selectedTime === time.value }]"
                @click="selectedTime = time.value"
              >
                <span class="time-label">{{ time.label }}</span>
                <span class="time-desc">{{ time.desc }}</span>
              </button>
            </div>
          </div>
        </div>

        <!-- 播放视图 -->
        <div v-else class="player-view" key="player">
          <!-- 呼吸动画容器 -->
          <div class="breath-container">
            <div
              class="breath-circle c1"
              :style="{ animationPlayState: isPaused ? 'paused' : 'running' }"
            ></div>
            <div
              class="breath-circle c2"
              :style="{ animationPlayState: isPaused ? 'paused' : 'running' }"
            ></div>
            <div
              class="breath-circle c3"
              :style="{ animationPlayState: isPaused ? 'paused' : 'running' }"
            ></div>

            <!-- 时间显示 -->
            <div class="timer-display">
              <h2 class="timer-text">{{ formattedTime }}</h2>
              <transition name="fade" mode="out-in">
                <p :key="breathText" class="breath-hint">
                  {{ isPaused ? "已暂停" : breathText }}
                </p>
              </transition>
            </div>
          </div>

          <!-- 控制栏 -->
          <div class="control-bar glass-panel">
            <el-button
              circle
              class="ctrl-btn stop"
              @click="stopSession"
              title="结束"
            >
              <el-icon><Close /></el-icon>
            </el-button>

            <el-button circle class="ctrl-btn play" @click="toggleTimer">
              <el-icon v-if="isPaused"><VideoPlay /></el-icon>
              <el-icon v-else><VideoPause /></el-icon>
            </el-button>

            <el-button
              circle
              class="ctrl-btn sound"
              :class="{ active: soundOn }"
              @click="toggleSound"
              title="背景白噪音"
            >
              <el-icon v-if="soundOn"><Microphone /></el-icon>
              <el-icon v-else><Mute /></el-icon>
            </el-button>
          </div>
        </div>
      </transition>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import {
  Close,
  VideoPlay,
  VideoPause,
  Clock,
  Microphone,
  Mute,
  ArrowLeft,
} from "@element-plus/icons-vue";

const router = useRouter();

// 状态
const isActive = ref(false);
const isPaused = ref(false);
const soundOn = ref(true);
const selectedTime = ref(300);
const timeLeft = ref(300);
const breathText = ref("吸气 Inhale");

// 计时器和音频
let timerInterval: number | null = null;
let sessionStartTime = 0;
let pauseStartTime = 0;
let audioCtx: AudioContext | null = null;
let gainNode: GainNode | null = null;

// 时长选项
const timeOptions = [
  { label: "5 Min", value: 300, desc: "快速放松" },
  { label: "10 Min", value: 600, desc: "标准正念" },
  { label: "20 Min", value: 1200, desc: "深度疗愈" },
];

// 格式化时间
const formattedTime = computed(() => {
  const m = Math.floor(timeLeft.value / 60);
  const s = timeLeft.value % 60;
  return `${m.toString().padStart(2, "0")}:${s.toString().padStart(2, "0")}`;
});

// 返回首页
const goBack = () => {
  router.push("/home");
};

// 初始化音频 (布朗噪音)
const initAudio = () => {
  if (!audioCtx) {
    const AudioContextClass =
      window.AudioContext || (window as any).webkitAudioContext;
    audioCtx = new AudioContextClass();

    const bufferSize = audioCtx.sampleRate * 2;
    const buffer = audioCtx.createBuffer(1, bufferSize, audioCtx.sampleRate);
    const data = buffer.getChannelData(0);

    let lastOut = 0;
    for (let i = 0; i < bufferSize; i++) {
      const white = Math.random() * 2 - 1;
      lastOut = (lastOut + 0.02 * white) / 1.02;
      data[i] = lastOut * 3.5;
    }

    const noiseSource = audioCtx.createBufferSource();
    noiseSource.buffer = buffer;
    noiseSource.loop = true;

    gainNode = audioCtx.createGain();
    gainNode.gain.value = soundOn.value ? 0.05 : 0;

    noiseSource.connect(gainNode);
    gainNode.connect(audioCtx.destination);
    noiseSource.start();
  } else if (audioCtx.state === "suspended") {
    audioCtx.resume();
  }
};

const updateVolume = () => {
  if (!gainNode || !audioCtx) return;
  if (!isActive.value || isPaused.value) {
    const targetVolume = soundOn.value ? 0.03 : 0;
    gainNode.gain.setTargetAtTime(targetVolume, audioCtx.currentTime, 0.5);
  }
};

const stopAudio = () => {
  if (gainNode && audioCtx) {
    gainNode.gain.setTargetAtTime(0, audioCtx.currentTime, 0.1);
    setTimeout(() => {
      if (audioCtx) audioCtx.suspend();
    }, 200);
  }
};

// 开始冥想
const startSession = () => {
  initAudio();
  if (soundOn.value) updateVolume();

  timeLeft.value = selectedTime.value;
  isActive.value = true;
  isPaused.value = false;
  sessionStartTime = Date.now();

  runTimer();
  runBreathCycle();
};

// 暂停/继续
const toggleTimer = () => {
  isPaused.value = !isPaused.value;
  if (isPaused.value) {
    if (timerInterval) clearInterval(timerInterval);
    pauseStartTime = Date.now();
    updateVolume();
  } else {
    const pauseDuration = Date.now() - pauseStartTime;
    sessionStartTime += pauseDuration;
    runTimer();
  }
};

// 停止冥想
const stopSession = () => {
  if (timerInterval) clearInterval(timerInterval);
  isActive.value = false;
  isPaused.value = false;
  breathText.value = "准备";
  stopAudio();
};

// 切换声音
const toggleSound = () => {
  soundOn.value = !soundOn.value;
  if (isPaused.value || !isActive.value) {
    updateVolume();
  }
};

// 计时器
const runTimer = () => {
  if (timerInterval) clearInterval(timerInterval);
  timerInterval = window.setInterval(() => {
    if (timeLeft.value > 0) {
      timeLeft.value--;
    } else {
      stopSession();
    }
  }, 1000);
};

// 呼吸循环
const runBreathCycle = () => {
  const cycle = () => {
    if (!isActive.value) return;

    if (!isPaused.value) {
      const now = Date.now();
      const elapsed = now - sessionStartTime;
      const cycleTime = 8000;
      const offset = (elapsed % cycleTime) / cycleTime;

      if (offset < 0.5) {
        breathText.value = "吸气 Inhale";
      } else {
        breathText.value = "呼气 Exhale";
      }

      // 动态音量
      if (gainNode && audioCtx && soundOn.value) {
        const wave = Math.sin(offset * Math.PI);
        const dynamicVol = 0.03 + wave * 0.05;
        gainNode.gain.setTargetAtTime(dynamicVol, audioCtx.currentTime, 0.05);
      } else if (gainNode && audioCtx && !soundOn.value) {
        gainNode.gain.setTargetAtTime(0, audioCtx.currentTime, 0.1);
      }

      requestAnimationFrame(cycle);
    } else {
      setTimeout(() => requestAnimationFrame(cycle), 500);
    }
  };
  requestAnimationFrame(cycle);
};

onUnmounted(() => {
  if (timerInterval) clearInterval(timerInterval);
  if (audioCtx) audioCtx.close();
});
</script>

<style scoped>
.meditation-page {
  position: fixed;
  inset: 0;
  background: #f2f5f3;
  overflow: hidden;
  z-index: 100;
}

/* 背景装饰球 */
.bg-orbs {
  position: fixed;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  animation: float 10s infinite ease-in-out;
}

.orb-1 {
  width: 500px;
  height: 500px;
  background: rgba(123, 158, 137, 0.4);
  top: -10%;
  left: -10%;
}

.orb-2 {
  width: 400px;
  height: 400px;
  background: rgba(196, 124, 107, 0.2);
  bottom: -10%;
  right: -10%;
  animation-delay: 2s;
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-20px);
  }
}

/* 头部 */
.page-header {
  position: relative;
  z-index: 20;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 32px;
}

.page-title {
  font-size: 32px;
  font-weight: bold;
  color: #2c3e50;
  margin: 0;
  font-family: "Georgia", serif;
}

.page-subtitle {
  display: block;
  font-size: 14px;
  color: #6b7280;
  margin-top: 4px;
}

.setting-btn {
  width: 48px;
  height: 48px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.8);
}

/* 主内容 */
.main-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  height: calc(100vh - 100px);
}

/* 玻璃面板 */
.glass-panel {
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 8px 32px rgba(31, 38, 135, 0.05);
  border-radius: 32px;
}

/* 设置视图 */
.setup-view {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32px;
  max-width: 900px;
  width: 100%;
  align-items: center;
}

/* 开始卡片 */
.start-card {
  position: relative;
  cursor: pointer;
}

.card-bg-blur {
  position: absolute;
  inset: 0;
  background: rgba(123, 158, 137, 0.2);
  filter: blur(40px);
  border-radius: 50%;
  transition: transform 0.7s;
}

.start-card:hover .card-bg-blur {
  transform: scale(1.05);
}

.card-inner {
  aspect-ratio: 4/3;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  transition: all 0.5s;
  border: 2px solid rgba(255, 255, 255, 0.5);
}

.start-card:hover .card-inner {
  transform: translateY(-8px);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
}

/* 装饰圆环 */
.decorative-rings {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.3;
}

.ring {
  position: absolute;
  border: 1px solid #7b9e89;
  border-radius: 50%;
}

.ring-1 {
  width: 200px;
  height: 200px;
  animation: spin 10s linear infinite;
}

.ring-2 {
  width: 150px;
  height: 150px;
  border-color: rgba(196, 124, 107, 0.5);
  animation: spin 15s linear infinite reverse;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.play-btn {
  width: 80px;
  height: 80px;
  background: #7b9e89;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 32px;
  margin-bottom: 24px;
  box-shadow: 0 8px 24px rgba(123, 158, 137, 0.3);
  transition: transform 0.3s;
  z-index: 10;
}

.start-card:hover .play-btn {
  transform: scale(1.1);
}

.start-title {
  font-size: 28px;
  font-weight: bold;
  color: #2c3e50;
  margin: 0 0 8px 0;
  z-index: 10;
  font-family: "Georgia", serif;
}

.start-desc {
  color: #6b7280;
  margin: 0;
  z-index: 10;
}

/* 选项卡片 */
.options-card {
  padding: 32px;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.options-title {
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 24px 0;
}

.options-title .el-icon {
  color: #7b9e89;
}

.time-options {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.time-btn {
  padding: 16px 20px;
  border-radius: 16px;
  border: 1px solid transparent;
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.3s;
  text-align: left;
}

.time-btn:hover {
  background: white;
}

.time-btn.active {
  background: #7b9e89;
  color: white;
  box-shadow: 0 8px 24px rgba(123, 158, 137, 0.3);
}

.time-label {
  font-size: 18px;
  font-weight: 500;
}

.time-desc {
  font-size: 14px;
  opacity: 0.7;
}

/* 播放视图 */
.player-view {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

/* 呼吸动画容器 */
.breath-container {
  position: relative;
  width: 400px;
  height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 48px;
}

.breath-circle {
  position: absolute;
  border-radius: 50%;
  animation: breathe 8s ease-in-out infinite;
}

.breath-circle.c1 {
  width: 250px;
  height: 250px;
  background: #7b9e89;
  opacity: 0.2;
  filter: blur(8px);
}

.breath-circle.c2 {
  width: 250px;
  height: 250px;
  background: #7b9e89;
  opacity: 0.1;
  filter: blur(20px);
  animation-delay: 1s;
}

.breath-circle.c3 {
  width: 200px;
  height: 200px;
  background: #c47c6b;
  opacity: 0.1;
  filter: blur(30px);
  animation-delay: 2s;
}

@keyframes breathe {
  0%,
  100% {
    transform: scale(1);
    opacity: 0.6;
  }
  50% {
    transform: scale(1.5);
    opacity: 0.3;
  }
}

/* 时间显示 */
.timer-display {
  position: relative;
  z-index: 10;
  text-align: center;
}

.timer-text {
  font-size: 72px;
  font-weight: 500;
  color: #2c3e50;
  margin: 0;
  font-family: "Georgia", serif;
  letter-spacing: 4px;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.breath-hint {
  font-size: 16px;
  font-weight: 500;
  color: #5f7a6a;
  margin: 16px 0 0 0;
  letter-spacing: 4px;
  text-transform: uppercase;
  height: 24px;
}

/* 控制栏 */
.control-bar {
  display: flex;
  align-items: center;
  gap: 32px;
  padding: 16px 32px;
  border-radius: 100px;
}

.ctrl-btn {
  border: none;
  transition: all 0.3s;
}

.ctrl-btn.stop {
  width: 44px;
  height: 44px;
  background: transparent;
  color: #9ca3af;
}

.ctrl-btn.stop:hover {
  color: #c47c6b;
}

.ctrl-btn.play {
  width: 64px;
  height: 64px;
  background: #2c3e50;
  color: white;
  font-size: 24px;
}

.ctrl-btn.play:hover {
  background: #1a252f;
  transform: scale(1.05);
}

.ctrl-btn.sound {
  width: 44px;
  height: 44px;
  background: transparent;
  color: #9ca3af;
}

.ctrl-btn.sound.active {
  color: #7b9e89;
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.5s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-up-enter-active {
  transition: all 0.6s cubic-bezier(0.23, 1, 0.32, 1);
}

.slide-up-leave-active {
  transition: all 0.4s ease-in;
}

.slide-up-enter-from {
  opacity: 0;
  transform: translateY(40px);
}

.slide-up-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}

/* 响应式 */
@media (max-width: 768px) {
  .setup-view {
    grid-template-columns: 1fr;
    gap: 24px;
  }

  .timer-text {
    font-size: 56px;
  }

  .breath-container {
    width: 300px;
    height: 300px;
  }
}
</style>
