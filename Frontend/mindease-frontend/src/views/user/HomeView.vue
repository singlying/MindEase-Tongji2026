<!-- 前端A负责：week2 用户首页首轮迁移 -->
<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";

import type { MoodLogItem, MoodTrendItem } from "@/api/mood";
import { getMoodLogs, getMoodStats, getMoodTrend, MOOD_TYPE_MAP } from "@/api/mood";
import MoodTrendChart from "@/components/charts/MoodTrendChart.vue";

const router = useRouter();

const moodValue = ref(4);
const recentLogs = ref<MoodLogItem[]>([]);
const trendData = ref<MoodTrendItem[]>([]);
const averageScore = ref(0);
const positiveRate = ref(0);

const moodPreview = computed<{ emoji: string; text: string }>(() => {
  switch (moodValue.value) {
    case 1:
      return { emoji: "😣", text: "今天状态比较吃力" };
    case 2:
      return { emoji: "😕", text: "有一点低落" };
    case 3:
      return { emoji: "🙂", text: "情绪比较平稳" };
    case 4:
      return { emoji: "😌", text: "整体放松而平静" };
    case 5:
      return { emoji: "🤩", text: "今天状态很好" };
    default:
      return { emoji: "🙂", text: "情绪比较平稳" };
  }
});

async function loadHomeData() {
  const [logsRes, trendRes, statsRes] = await Promise.all([
    getMoodLogs(3, 0),
    getMoodTrend(7),
    getMoodStats(),
  ]);

  recentLogs.value = logsRes.data.logs;
  trendData.value = trendRes.data.dates.map((date: string, index: number) => ({
    date,
    score: trendRes.data.scores[index],
  }));
  averageScore.value = statsRes.data.averageScore;
  positiveRate.value = statsRes.data.positiveRate;
}

onMounted(() => {
  loadHomeData();
});
</script>

<template>
  <div class="home-page">
    <section class="hero-grid">
      <article class="hero-card glass-card">
        <div class="hero-head">
          <div>
            <div class="eyebrow">今日概览</div>
            <h2>此刻心情</h2>
            <p>{{ moodPreview.text }}</p>
          </div>
          <div class="hero-emoji">{{ moodPreview.emoji }}</div>
        </div>

        <el-slider
          v-model="moodValue"
          :min="1"
          :max="5"
          :show-tooltip="false"
        />

        <div class="hero-actions">
          <el-button type="primary" @click="router.push('/mood-diary')">写一篇日记</el-button>
          <el-button plain @click="router.push('/emotion-report')">查看报告</el-button>
        </div>
      </article>

      <article class="quick-card glass-card" @click="router.push('/ai-chat')">
        <div class="quick-title">AI 咨询</div>
        <div class="quick-desc">随时开始一段对话，获得及时的情绪支持。</div>
      </article>

      <article class="quick-card glass-card" @click="router.push('/assessment')">
        <div class="quick-title">心理测评</div>
        <div class="quick-desc">通过量表了解近期状态，形成更清晰的自我认知。</div>
      </article>

      <article class="quick-card glass-card" @click="router.push('/counselor-list')">
        <div class="quick-title">咨询师推荐</div>
        <div class="quick-desc">查看适合你的专业支持方向与预约入口。</div>
      </article>
    </section>

    <section class="content-grid">
      <article class="trend-card glass-card">
        <div class="section-head">
          <div>
            <h3>7天情绪趋势</h3>
            <p>用一张图快速回顾最近一周的情绪波动。</p>
          </div>
          <div class="stats">
            <span>均值 {{ averageScore.toFixed(1) }}</span>
            <span>积极占比 {{ Math.round(positiveRate * 100) }}%</span>
          </div>
        </div>
        <MoodTrendChart :data="trendData" />
      </article>

      <article class="recent-card glass-card">
        <div class="section-head">
          <div>
            <h3>最近记录</h3>
            <p>快速回顾最近几次记录下来的情绪与想法。</p>
          </div>
        </div>

        <div class="recent-list">
          <button
            v-for="item in recentLogs"
            :key="item.id"
            class="recent-item"
            @click="router.push(`/mood-diary/${item.id}`)"
          >
            <span>{{ item.emoji }} {{ MOOD_TYPE_MAP[item.moodType].label }}</span>
            <span>{{ item.content.slice(0, 22) }}...</span>
          </button>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
.home-page {
  display: grid;
  gap: 20px;
}

.hero-grid {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1fr;
  gap: 16px;
}

.hero-card,
.quick-card,
.trend-card,
.recent-card {
  padding: 24px;
}

.hero-head,
.section-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
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

h2,
h3 {
  margin-top: 6px;
}

p {
  margin-top: 8px;
  color: var(--ease-muted);
  line-height: 1.7;
}

.hero-emoji {
  font-size: 52px;
}

.hero-actions {
  margin-top: 18px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.quick-card {
  cursor: pointer;
  transition: transform 0.2s ease;
}

.quick-card:hover {
  transform: translateY(-2px);
}

.quick-title {
  font-weight: 700;
}

.quick-desc {
  margin-top: 10px;
  color: var(--ease-muted);
  line-height: 1.7;
}

.content-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
}

.stats {
  display: grid;
  gap: 6px;
  text-align: right;
  color: var(--ease-primary-dark);
  font-weight: 600;
}

.recent-list {
  margin-top: 18px;
  display: grid;
  gap: 12px;
}

.recent-item {
  border: 1px solid rgba(123, 158, 137, 0.12);
  background: rgba(123, 158, 137, 0.06);
  border-radius: 16px;
  padding: 14px 16px;
  display: grid;
  gap: 6px;
  text-align: left;
  cursor: pointer;
}

@media (max-width: 1100px) {
  .hero-grid,
  .content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
