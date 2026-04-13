<template>
  <div class="meditation-scene">
    <section class="panel meditation-hero">
      <div>
        <span class="section-kicker">冥想声场</span>
        <h2>{{ active }}</h2>
        <p class="muted">{{ current.description }}</p>
      </div>
      <div class="timer-dial">
        <div class="breath-orb" :class="{ running }">{{ breathLabel }}</div>
        <strong>{{ timeText }}</strong>
      </div>
    </section>

    <section class="practice-grid">
      <button
        v-for="item in programs"
        :key="item.name"
        :class="['practice-card', { active: active === item.name }]"
        @click="active = item.name"
      >
        <span>{{ item.icon }}</span>
        <strong>{{ item.name }}</strong>
        <small>{{ item.description }}</small>
      </button>
    </section>

    <section class="panel breath-coach">
      <div class="row-between">
        <div>
          <span class="section-kicker">呼吸引导</span>
          <h2>{{ phaseText }}</h2>
          <p class="muted">{{ current.guidance }}</p>
        </div>
        <button class="btn" @click="toggle">{{ running ? "暂停" : "开始" }}</button>
      </div>
      <div class="breath-steps">
        <div v-for="step in steps" :key="step.name" :class="{ active: step.name === breathLabel }">
          <strong>{{ step.name }}</strong>
          <span>{{ step.text }}</span>
        </div>
      </div>
      <div class="session-progress"><i :style="{ width: `${progress}%` }"></i></div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from "vue";

const programs = [
  { name: "晨间唤醒", icon: "☀️", description: "用轻柔节奏打开身体", guidance: "坐直，感受脚底和椅面的支撑。" },
  { name: "焦虑舒缓", icon: "🌿", description: "把注意力从警觉拉回呼吸", guidance: "允许紧张存在，然后把注意力放回呼气。" },
  { name: "睡前放松", icon: "🌙", description: "降低刺激，进入更慢的节奏", guidance: "放松下颌、肩颈和手指，准备结束一天。" }
];
const steps = [
  { name: "吸气", text: "4 秒，慢慢扩张胸腔" },
  { name: "停留", text: "4 秒，感受空气停在身体里" },
  { name: "呼气", text: "4 秒，释放肩颈力量" }
];
const active = ref(programs[0].name);
const running = ref(false);
const seconds = ref(300);
let timer: number | undefined;

const current = computed(() => programs.find((item) => item.name === active.value) || programs[0]);
const timeText = computed(() => {
  const min = Math.floor(seconds.value / 60).toString().padStart(2, "0");
  const sec = (seconds.value % 60).toString().padStart(2, "0");
  return `${min}:${sec}`;
});
const progress = computed(() => Math.round(((300 - seconds.value) / 300) * 100));
const phase = computed(() => seconds.value % 12);
const breathLabel = computed(() => (phase.value < 4 ? "吸气" : phase.value < 8 ? "停留" : "呼气"));
const phaseText = computed(() => `${breathLabel.value}，把注意力放回身体`);

const stop = () => {
  running.value = false;
  if (timer) window.clearInterval(timer);
};

const toggle = () => {
  if (running.value) {
    stop();
    return;
  }
  running.value = true;
  timer = window.setInterval(() => {
    seconds.value = Math.max(0, seconds.value - 1);
    if (seconds.value === 0) stop();
  }, 1000);
};

watch(active, () => {
  stop();
  seconds.value = 300;
});

onBeforeUnmount(stop);
</script>
pt>
