<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import {
  ArrowLeft,
  Download,
  User,
  TrendCharts,
  Notebook,
  PieChart,
  MagicStick,
  Check,
} from "@element-plus/icons-vue";
import {
  getEmotionReport,
  exportEmotionReport,
  type EmotionReportData,
} from "@/api/report";
import MoodTrendChart from "@/components/charts/MoodTrendChart.vue";

const router = useRouter();

const loading = ref(false);
const report = ref<EmotionReportData | null>(null);
const dateRange = ref<[string, string] | null>(null);

const trendList = computed(() => {
  if (!report.value) return [];
  const { dates, scores } = report.value.trendData;
  return dates.map((d, i) => ({
    date: d,
    score: scores[i] ?? 0,
  }));
});

const distributionList = computed(() => {
  if (!report.value) return [];
  const dist = report.value.distribution as Record<
    string,
    number | string | undefined
  >;
  const total = Object.values(dist).reduce<number>((sum, v) => {
    const num =
      typeof v === "number" ? v : Number(String(v ?? "0").replace("%", ""));
    return sum + (Number.isFinite(num) ? num : 0);
  }, 0);
  return Object.entries(dist).map(([mood, v]) => {
    const value: number =
      typeof v === "number" ? v : Number(String(v ?? "0").replace("%", ""));
    const safeValue = Number.isFinite(value) ? value : 0;
    const percent = total > 0 ? Math.round((safeValue / total) * 100) : 0;
    const normMood =
      mood.length > 0
        ? mood.charAt(0).toUpperCase() + mood.slice(1).toLowerCase()
        : mood;
    return { mood: normMood, value: safeValue, percent };
  });
});

const ensureDateRange = () => {
  if (dateRange.value && dateRange.value.length === 2) return dateRange.value;
  const end = new Date();
  const start = new Date();
  start.setDate(end.getDate() - 6); // 默认近7天
  const fmt = (d: Date) => {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, "0");
    const day = String(d.getDate()).padStart(2, "0");
    return `${y}-${m}-${day}`;
  };
  dateRange.value = [fmt(start), fmt(end)];
  return dateRange.value;
};

const loadReport = async () => {
  loading.value = true;
  try {
    const [start, end] = ensureDateRange();
    const res = (await getEmotionReport({
      startDate: start,
      endDate: end,
    })) as any;
    report.value = res.data;
  } catch (e) {
    ElMessage.error("情绪报告获取失败");
  } finally {
    loading.value = false;
  }
};

const handleExport = async () => {
  try {
    const res = await exportEmotionReport();
    if (res && res.data) {
      const blob = res.data as Blob;
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "emotion_report.pdf";
      a.click();
      window.URL.revokeObjectURL(url);
      ElMessage.success("报告导出成功");
    }
  } catch (err: any) {
    ElMessage.error(err?.message || "报告导出失败");
  }
};

const moodEmojiMap: Record<string, string> = {
  Happy: "😊",
  Calm: "😌",
  Anxious: "😰",
  Sad: "😢",
  Angry: "😠",
  Tired: "😴",
  Excited: "🎉",
};

const moodLabelMap: Record<string, string> = {
  Happy: "开心",
  Calm: "平静",
  Anxious: "焦虑",
  Sad: "难过",
  Angry: "愤怒",
  Tired: "疲惫",
  Excited: "兴奋",
};

const formatDate = (dateStr: string) => {
  const d = new Date(dateStr);
  const m = d.getMonth() + 1;
  const day = d.getDate();
  return `${m}月${day}日`;
};

onMounted(loadReport);
</script>

<template>
  <div class="report-page" v-loading="loading">
    <!-- 顶部操作 -->
    <div class="page-header">
      <button class="back-btn" @click="router.push('/home')">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回首页</span>
      </button>
      <div class="header-actions">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="loadReport"
        />
        <el-button type="primary" @click="handleExport" plain>
          <el-icon><Download /></el-icon>
          导出PDF
        </el-button>
      </div>
    </div>

    <!-- 概览卡片 -->
    <div class="glass-card summary-card" v-if="report">
      <div class="summary-left">
        <div class="avatar-box">
          <el-icon><User /></el-icon>
        </div>
        <div>
          <h2>你的情绪健康报告</h2>
          <p>报告周期：{{ report.period }}</p>
        </div>
      </div>
      <div class="summary-stats">
        <div class="stat">
          <p class="label">综合评分</p>
          <p class="value accent">{{ report.avgScore?.toFixed(1) ?? "-" }}</p>
          <span class="tag">良好</span>
        </div>
        <div class="stat">
          <p class="label">积极占比</p>
          <p class="value">
            {{ Math.round((report.positiveRate || 0) * 100) }}%
          </p>
          <span class="tag soft">情绪平衡</span>
        </div>
        <div class="stat">
          <p class="label">连续记录</p>
          <p class="value">{{ report.continuousDays || 0 }} 天</p>
          <span class="tag soft">坚持打卡</span>
        </div>
      </div>
    </div>

    <!-- 主体布局 -->
    <div class="grid">
      <div class="left-col">
        <!-- 趋势图 -->
        <div class="glass-card panel">
          <div class="panel-header">
            <h3>
              <el-icon><TrendCharts /></el-icon> 近7天情绪趋势
            </h3>
          </div>
          <MoodTrendChart :data="trendList" height="320px" />
        </div>

        <!-- 日记摘录 -->
        <div class="glass-card panel">
          <div class="panel-header">
            <h3>
              <el-icon><Notebook /></el-icon> 近期日记摘录
            </h3>
          </div>
          <div v-if="report?.recentLogs?.length" class="logs-list">
            <div
              v-for="log in report?.recentLogs"
              :key="log.date + log.content"
              class="log-item"
            >
              <div class="log-head">
                <div class="log-meta">
                  <span class="emoji">{{
                    log.emoji || moodEmojiMap[log.moodType] || "🙂"
                  }}</span>
                  <div>
                    <p class="date">{{ formatDate(log.date) }}</p>
                    <p class="score">情绪得分：{{ log.score }}</p>
                  </div>
                </div>
                <span class="tag small">{{
                  moodLabelMap[log.moodType] || log.moodType
                }}</span>
              </div>
              <p class="log-content">{{ log.content }}</p>
            </div>
          </div>
          <div v-else class="empty-box">暂无日记记录</div>
        </div>
      </div>

      <div class="right-col">
        <!-- 情绪分布 -->
        <div class="glass-card panel">
          <div class="panel-header">
            <h3>
              <el-icon><PieChart /></el-icon> 情绪分布
            </h3>
          </div>
          <div v-if="distributionList.length" class="dist-list">
            <div
              v-for="item in distributionList"
              :key="item.mood"
              class="dist-row"
            >
              <div class="dist-label">
                <span class="emoji">{{ moodEmojiMap[item.mood] || "🙂" }}</span>
                <span>{{ moodLabelMap[item.mood] || item.mood }}</span>
              </div>
              <div class="progress">
                <div class="bar" :style="{ width: item.percent + '%' }"></div>
              </div>
              <span class="percent">{{ item.percent }}%</span>
            </div>
          </div>
          <div v-else class="empty-box">暂无统计数据</div>
        </div>

        <!-- AI 建议 -->
        <div class="glass-card panel">
          <div class="panel-header">
            <h3>
              <el-icon><MagicStick /></el-icon> AI 建议
            </h3>
          </div>
          <ul v-if="report?.aiSuggestions?.length" class="suggest-list">
            <li v-for="(s, idx) in report?.aiSuggestions" :key="idx">
              <el-icon><Check /></el-icon>
              <span>{{ s }}</span>
            </li>
          </ul>
          <div v-else class="empty-box">暂无建议</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.report-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  color: var(--gray-700);
  cursor: pointer;
  transition: all 0.2s ease;
  backdrop-filter: blur(10px);
}

.back-btn:hover {
  color: var(--ease-accent);
  transform: translateX(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.glass-card {
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 18px;
  padding: 18px 20px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.06);
  backdrop-filter: blur(12px);
}

.summary-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.summary-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-box {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background: var(--ease-accent);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.summary-left h2 {
  margin: 0 0 6px;
  font-size: 22px;
  color: var(--ease-dark);
}

.summary-left p {
  margin: 0;
  color: var(--gray-500);
}

.summary-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  width: 50%;
}

.stat {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 14px;
  padding: 12px 14px;
  border: 1px solid rgba(0, 0, 0, 0.03);
}

.stat .label {
  margin: 0;
  color: var(--gray-500);
  font-size: 13px;
}

.stat .value {
  margin: 4px 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--ease-dark);
}

.stat .value.accent {
  color: var(--ease-accent);
}

.tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 10px;
  background: rgba(123, 158, 137, 0.15);
  color: var(--ease-accent);
  font-size: 12px;
  font-weight: 600;
}

.tag.soft {
  background: rgba(123, 158, 137, 0.12);
}

.tag.small {
  font-size: 12px;
  padding: 2px 8px;
}

.grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
}

.panel {
  margin-bottom: 16px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.panel-header h3 {
  margin: 0;
  font-size: 16px;
  color: var(--ease-dark);
  display: flex;
  align-items: center;
  gap: 6px;
}

.logs-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.log-item {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(0, 0, 0, 0.03);
  border-radius: 14px;
  padding: 12px;
}

.log-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.log-meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.log-meta .emoji {
  font-size: 26px;
}

.log-meta .date {
  margin: 0;
  font-weight: 600;
  color: var(--ease-dark);
}

.log-meta .score {
  margin: 0;
  font-size: 12px;
  color: var(--gray-500);
}

.log-content {
  margin: 0;
  color: var(--gray-700);
  line-height: 1.6;
}

.dist-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.dist-row {
  display: grid;
  grid-template-columns: 1fr 2fr auto;
  gap: 10px;
  align-items: center;
}

.dist-label {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--ease-dark);
  font-weight: 600;
}

.progress {
  width: 100%;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 999px;
  height: 8px;
  overflow: hidden;
}

.progress .bar {
  height: 100%;
  background: linear-gradient(90deg, #7b9e89, #10b981);
}

.percent {
  font-weight: 600;
  color: var(--ease-accent);
}

.suggest-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-left: 4px;
}

.suggest-list li {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--gray-700);
}

.empty-box {
  padding: 20px;
  text-align: center;
  color: var(--gray-500);
  background: rgba(0, 0, 0, 0.02);
  border-radius: 12px;
}

@media (max-width: 1024px) {
  .grid {
    grid-template-columns: 1fr;
  }
  .summary-stats {
    width: 100%;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .summary-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  .summary-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
