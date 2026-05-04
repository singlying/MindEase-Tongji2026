<template>
  <div class="grid">
    <form class="form-card span-5 stack mood-composer" @submit.prevent="createMood">
      <div class="row-between">
        <div>
          <h2>写一条情绪日记</h2>
          <p class="muted">先选一个情绪，再把它放进文字里。</p>
        </div>
        <div class="mood-hero">{{ selectedMood.emoji }}</div>
      </div>

      <div class="mood-picker">
        <button
          v-for="mood in moodOptions"
          :key="mood.type"
          type="button"
          :class="['mood-option', { active: form.moodType === mood.type }]"
          @click="selectMood(mood)"
        >
          <span>{{ mood.emoji }}</span>
          <strong>{{ mood.label }}</strong>
        </button>
      </div>

      <label class="field">
        <span>情绪强度 {{ form.moodScore }}/10</span>
        <input v-model.number="form.moodScore" class="range" type="range" min="1" max="10" />
      </label>

      <textarea v-model="form.content" class="textarea journal-paper" placeholder="今天哪一刻最影响你的心情？"></textarea>
      <input v-model="tagText" class="input" placeholder="标签，例如：睡眠, 汇报, 运动" />
      <button class="btn submit-glow" :disabled="saving">
        <span v-if="saving" class="spinner"></span>
        {{ saving ? "正在保存" : "保存日记" }}
      </button>
      <p v-if="message" class="feedback-note">{{ message }}</p>
    </form>

    <section class="panel span-7">
      <div class="row-between">
        <div>
          <h2>情绪时间线</h2>
          <p class="muted">按最近记录回看情绪、触发事件和恢复资源。</p>
        </div>
        <button class="btn secondary" @click="load"><span v-if="loading" class="spinner"></span>刷新</button>
      </div>

      <div v-if="loading" class="timeline-list">
        <div v-for="i in 3" :key="i" class="skeleton-card"></div>
      </div>
      <div v-else class="timeline-list">
        <article v-for="item in logs" :key="item.id || item.logId" class="timeline-item">
          <div class="timeline-emoji">{{ moodMeta(item.moodType).emoji }}</div>
          <div>
            <div class="row-between">
              <strong>{{ moodMeta(item.moodType).label }} · {{ item.moodScore || item.score }}/10</strong>
              <span class="chip">{{ formatDate(item.logDate || item.createTime) }}</span>
            </div>
            <p class="muted">{{ item.content }}</p>
            <span v-for="tag in item.tags || []" :key="tag" class="chip tag-chip">{{ tag }}</span>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { moodApi } from "@/api";

const moodOptions = [
  { type: "Happy", label: "愉悦", emoji: "😊", score: 8 },
  { type: "Calm", label: "平静", emoji: "😌", score: 6 },
  { type: "Sad", label: "低落", emoji: "😔", score: 3 },
  { type: "Anxious", label: "焦虑", emoji: "😰", score: 4 },
  { type: "Angry", label: "烦躁", emoji: "😤", score: 3 }
];

const logs = ref<any[]>([]);
const tagText = ref("");
const message = ref("");
const loading = ref(false);
const saving = ref(false);
const form = reactive({ moodType: "Calm", moodScore: 6, content: "" });

const selectedMood = computed(() => moodMeta(form.moodType));
const moodMeta = (type: string) => moodOptions.find((item) => item.type.toLowerCase() === String(type).toLowerCase()) || moodOptions[1];
const selectMood = (mood: (typeof moodOptions)[number]) => {
  form.moodType = mood.type;
  form.moodScore = mood.score;
};

const formatDate = (value: string) => String(value || "").replace("T", " ").slice(0, 16);
const localDateTime = () => {
  const date = new Date();
  const pad = (value: number) => String(value).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
};

const load = async () => {
  loading.value = true;
  try {
    const response = await moodApi.list({ limit: 10, offset: 0 });
    logs.value = response.data.logs || [];
  } catch (err) {
    message.value = err instanceof Error ? err.message : "情绪记录加载失败";
  } finally {
    loading.value = false;
  }
};

const createMood = async () => {
  saving.value = true;
  try {
    const response = await moodApi.create({
      ...form,
      tags: tagText.value.split(",").map((tag) => tag.trim()).filter(Boolean),
      logDate: localDateTime()
    });
    message.value = response.data.aiAnalysis || response.message || "已保存";
    form.content = "";
    await load();
  } catch (err) {
    message.value = err instanceof Error ? err.message : "保存失败，请稍后再试";
  } finally {
    saving.value = false;
  }
};

onMounted(load);
</script>
