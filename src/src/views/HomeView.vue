<template>
  <div class="home-canvas">
    <section class="daily-ritual panel">
      <div>
        <span class="section-kicker">今日照护计划</span>
        <h2>把今天拆成可以完成的四个小动作</h2>
        <p class="muted">记录此刻、完成测评、预约咨询、进入一次短呼吸练习。</p>
      </div>
      <div class="ritual-actions">
        <button class="btn" @click="$router.push('/mood-diary')">写下此刻</button>
        <button class="btn secondary" @click="$router.push('/ai-chat')">开始陪伴</button>
      </div>
    </section>

    <section class="metric-grid">
      <article class="metric-card" v-for="metric in metrics" :key="metric.label">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
        <small>{{ metric.caption }}</small>
      </article>
    </section>

    <section class="focus-layout">
      <article class="panel mood-board">
        <div class="row-between">
          <div>
            <span class="section-kicker">7 天情绪流动</span>
            <h2>近期能量曲线</h2>
          </div>
          <span class="chip">平均 {{ avgScore }}</span>
        </div>
        <div class="chart-bars elevated-bars">
          <div v-for="(score, index) in scores" :key="index" class="bar-column">
            <div class="bar" :style="{ height: `${Math.max(16, score * 16)}px` }"></div>
            <span>{{ dayLabel(index) }}</span>
          </div>
        </div>
      </article>

      <article class="panel action-studio">
        <span class="section-kicker">快速行动</span>
        <h2>下一步入口</h2>
        <div class="action-list">
          <button class="action-tile active" @click="$router.push('/mood-diary')">
            <span>记录</span>
            <strong>记录此刻心情</strong>
          </button>
          <button class="action-tile" @click="$router.push('/ai-chat')">
            <span>陪伴</span>
            <strong>进入 AI 咨询</strong>
          </button>
          <button class="action-tile" @click="$router.push('/assessment')">
            <span>测评</span>
            <strong>开始心理测评</strong>
          </button>
        </div>
      </article>
    </section>

    <section class="home-lower">
      <article class="panel">
        <div class="row-between">
          <div>
            <span class="section-kicker">近期提醒</span>
            <h2>照护队列</h2>
          </div>
          <span class="chip">{{ notifications.length }} 条</span>
        </div>
        <div class="timeline-list compact">
          <article v-for="item in notificationItems" :key="item.id" class="timeline-item">
            <div class="timeline-dot"></div>
            <div>
              <strong>{{ item.title }}</strong>
              <p class="muted">{{ item.content }}</p>
            </div>
          </article>
        </div>
      </article>

      <article class="panel rhythm-panel">
        <span class="section-kicker">建议节奏</span>
        <h2>今天的恢复资源</h2>
        <div class="rhythm-list">
          <div v-for="item in rhythm" :key="item.time" class="rhythm-row">
            <span>{{ item.time }}</span>
            <p>{{ item.text }}</p>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { moodApi, userApi } from "@/api";

const trend = ref<any>({});
const notifications = ref<any[]>([]);
const scores = computed<number[]>(() => Array.isArray(trend.value?.scores) ? trend.value.scores : [3, 5, 4, 7, 6, 8, 7]);
const dates = computed<string[]>(() => Array.isArray(trend.value?.dates) ? trend.value.dates.map(String) : ["一", "二", "三", "四", "五", "六", "日"]);
const avgScore = computed(() => Number(trend.value?.avgScore || 0).toFixed(1));
const positiveRate = computed(() => Math.round((trend.value?.positiveRate || 0) * 100));
const continuousDays = computed(() => trend.value?.continuousDays || 0);

const metrics = computed(() => [
  { label: "平均情绪", value: avgScore.value, caption: "近 7 天" },
  { label: "积极占比", value: `${positiveRate.value}%`, caption: "稳定向上" },
  { label: "连续记录", value: continuousDays.value, caption: "天" },
  { label: "待读通知", value: notifications.value.length, caption: "条提醒" }
]);

const fallbackNotifications = [
  { id: "n1", title: "晚间呼吸练习", content: "今晚 21:30 有一段 5 分钟训练" },
  { id: "n2", title: "咨询提醒", content: "明天下午有一场预约咨询" },
  { id: "n3", title: "测评建议", content: "可以复测 GAD-7 观察近期变化" }
];
const notificationItems = computed(() => notifications.value.length ? notifications.value : fallbackNotifications);

const rhythm = [
  { time: "上午", text: "完成一次 2 分钟身体扫描，标记当前压力水平。" },
  { time: "下午", text: "在学习或工作切换间隙进行短呼吸训练。" },
  { time: "晚上", text: "写一条情绪日记，观察触发事件和恢复资源。" }
];

const dayLabel = (index: number) => String(dates.value[index] || `D${index + 1}`);

onMounted(async () => {
  trend.value = (await moodApi.trend(7).catch(() => ({ data: {} }))).data;
  notifications.value = ((await userApi.notifications(8).catch(() => ({ data: { notifications: [] } }))).data.notifications || []);
});
</script>
