<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";
import { getMoodTrend } from "@/api/mood";
import type { MoodTrendItem } from "@/api/mood";
import MoodTrendChart from "@/components/charts/MoodTrendChart.vue";

const router = useRouter();
const userStore = useUserStore();

// 情绪滑块相关
const moodValue = ref(4);
const moodEmoji = ref("😌");
const moodText = ref("平静 & 放松");

// 情绪映射
const moodMap: Record<number, { emoji: string; text: string }> = {
  1: { emoji: "😫", text: "糟糕透了" },
  2: { emoji: "😔", text: "有些低落" },
  3: { emoji: "😐", text: "还行吧" },
  4: { emoji: "😌", text: "平静 & 放松" },
  5: { emoji: "🤩", text: "超级棒！" },
};

// 情绪滑块变化
const handleMoodChange = (value: number) => {
  const mood = moodMap[value];
  if (mood) {
    moodEmoji.value = mood.emoji;
    moodText.value = mood.text;
  }
};

// 7天情绪趋势数据
const trendData = ref<MoodTrendItem[]>([]);
const avgScore = ref(0);
const positiveRate = ref(0);
const continuousDays = ref(0);
const trendLoading = ref(false);

// 加载情绪趋势数据
const loadMoodTrend = async () => {
  trendLoading.value = true;
  try {
    const res = (await getMoodTrend(7)) as any;
    const data = res.data;

    // 转换后端数据格式：将dates和scores数组转换为MoodTrendItem[]
    trendData.value = data.dates.map((date: string, index: number) => ({
      date,
      score: data.scores[index],
    }));

    avgScore.value = data.avgScore;
    positiveRate.value = Math.round(data.positiveRate * 100);
    continuousDays.value = data.continuousDays;
  } catch (error) {
    console.error("加载情绪趋势失败:", error);
  } finally {
    trendLoading.value = false;
  }
};

onMounted(() => {
  loadMoodTrend();
});
</script>

<template>
  <div class="home-page">
    <!-- 网格布局 -->
    <div class="grid-container">
      <!-- 情绪滑块卡片 - 2列2行 -->
      <div class="mood-card glass-card">
        <div class="card-content">
          <div class="card-header">
            <h2 class="card-title">此刻心情</h2>
          </div>

          <div class="mood-display">
            <div class="mood-emoji">{{ moodEmoji }}</div>
            <div class="mood-text">{{ moodText }}</div>

            <el-slider
              v-model="moodValue"
              :min="1"
              :max="5"
              :show-tooltip="false"
              class="mood-slider"
              @change="handleMoodChange"
            />

            <div class="mood-labels">
              <span>低落</span>
              <span>平静</span>
              <span>愉悦</span>
            </div>
          </div>

          <el-button
            type="primary"
            class="record-btn"
            @click="router.push('/mood-diary')"
          >
            <el-icon><EditPen /></el-icon>
            写日记
          </el-button>
        </div>
      </div>

      <!-- AI咨询快捷入口 - 2列2行 -->
      <div class="ai-card" @click="router.push('/ai-chat')">
        <div class="ai-header">
          <div class="ai-info">
            <div class="ai-icon">
              <el-icon><ChatDotRound /></el-icon>
            </div>
            <div>
              <h2 class="ai-title">MindEase AI</h2>
              <p class="ai-subtitle">24h 深度陪伴</p>
            </div>
          </div>
          <el-icon class="arrow-icon"><ArrowRight /></el-icon>
        </div>

        <div class="ai-content">
          <p class="ai-quote">"需要聊聊吗？"</p>
          <p class="ai-desc">点击此处，即刻开启对话</p>
        </div>
      </div>

      <!-- 心理测评入口 -->
      <div class="feature-card glass-card" @click="router.push('/assessment')">
        <div class="feature-icon warm">
          <el-icon><DocumentChecked /></el-icon>
        </div>
        <div class="feature-info">
          <h3 class="feature-title">心理测评</h3>
          <p class="feature-subtitle">PHQ-9 / GAD-7</p>
        </div>
      </div>

      <!-- 咨询师推荐入口 - 2列 -->
      <div
        class="counselor-card glass-card"
        @click="router.push('/counselor-list')"
      >
        <div class="counselor-content">
          <div class="counselor-info">
            <el-tag type="info" size="small">找到专属最佳匹配</el-tag>
            <h3 class="counselor-name">咨询师推荐</h3>
            <p class="counselor-desc">各类疏导专家</p>
          </div>
          <!-- <el-avatar
            
          /> -->
        </div>
      </div>

      <!-- 冥想时刻 -->
      <div class="meditation-card" @click="router.push('/meditation')">
        <div class="meditation-icon">
          <el-icon><VideoPlay /></el-icon>
        </div>
        <h3 class="meditation-title">冥想时刻</h3>
        <p class="meditation-subtitle">放松身心</p>
      </div>

      <!-- 7天情绪趋势图 - 4列 -->
      <div class="chart-card glass-card">
        <div class="chart-header">
          <h3 class="chart-title">
            <el-icon><TrendCharts /></el-icon>
            7天情绪流动
          </h3>
          <div class="chart-legend">
            <span class="legend-dot"></span>
            <span class="legend-text">情绪得分 (0-10)</span>
          </div>
        </div>

        <div v-if="trendLoading" class="chart-loading">
          <el-icon class="is-loading"><Loading /></el-icon>
          <p>加载中...</p>
        </div>
        <div v-else-if="trendData.length > 0" class="chart-container">
          <MoodTrendChart :data="trendData" height="280px" />
        </div>
        <div v-else class="chart-empty">
          <p>暂无数据</p>
          <el-button size="small" @click="router.push('/mood-diary/new')">
            开始记录
          </el-button>
        </div>

        <div class="chart-stats">
          <div class="stat-item">
            <div class="stat-value">{{ avgScore.toFixed(1) }}</div>
            <div class="stat-label">平均情绪值</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ positiveRate }}%</div>
            <div class="stat-label">积极占比</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ continuousDays }}/7</div>
            <div class="stat-label">连续记录</div>
          </div>
        </div>
      </div>

      <!-- 每日建议 - 2列 -->
      <div class="tips-card glass-card">
        <h3 class="tips-title">
          <el-icon color="#f59e0b"><Sunny /></el-icon>
          每日建议
        </h3>
        <ul class="tips-list">
          <li>
            <el-icon color="#7b9e89"><Check /></el-icon>
            <span>尝试10分钟正念冥想，帮助缓解焦虑</span>
          </li>
          <li>
            <el-icon color="#7b9e89"><Check /></el-icon>
            <span>保持规律的睡眠时间，改善整体情绪</span>
          </li>
          <li>
            <el-icon color="#7b9e89"><Check /></el-icon>
            <span>与朋友或家人保持联系，社交支持很重要</span>
          </li>
        </ul>
      </div>

      <!-- 推荐音乐 - 2列 -->
      <!-- TODO: 推荐音乐模块暂时下线，后续接入真实音频资源与播放逻辑后再启用
      <div class="music-card glass-card">
        <div class="music-header">
          <h3 class="music-title">
            <el-icon><Headset /></el-icon>
            推荐音乐
          </h3>
          <el-link type="primary" :underline="false">查看更多</el-link>
        </div>

        <div class="music-list">
          <div class="music-item">
            <div class="music-cover"></div>
            <div class="music-info">
              <h4>平静放松 - 自然之声</h4>
              <p>舒缓音乐 • 30分钟</p>
            </div>
            <el-icon class="play-icon" color="#7b9e89"><VideoPlay /></el-icon>
          </div>

          <div class="music-item">
            <div class="music-cover"></div>
            <div class="music-info">
              <h4>冥想音乐合集</h4>
              <p>疗愈系列 • 12首</p>
            </div>
            <el-icon class="play-icon" color="#7b9e89"><VideoPlay /></el-icon>
          </div>
        </div>
      </div>
      -->
    </div>
  </div>
</template>

<style scoped>
.home-page {
  max-width: 1600px;
  margin: 0 auto;
}

/* 网格布局 */
.grid-container {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

/* 玻璃卡片效果 */
.glass-card {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(24px);
  border-radius: 32px;
  padding: 32px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
}

.glass-card:hover {
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

/* 情绪卡片 - 2列2行 */
.mood-card {
  grid-column: span 2;
  grid-row: span 2;
  cursor: default;
}

.card-content {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.card-title {
  font-size: 24px;
  font-weight: bold;
  color: #2c3e50;
}

.mood-display {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px 0;
}

.mood-emoji {
  font-size: 72px;
  margin-bottom: 24px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

.mood-text {
  font-size: 24px;
  font-weight: 500;
  color: #7b9e89;
  margin-bottom: 32px;
}

.mood-slider {
  width: 75%;
  margin-bottom: 12px;
}

.mood-labels {
  width: 75%;
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #9ca3af;
}

.record-btn {
  width: 100%;
  background: #7b9e89;
  border-color: #7b9e89;
  border-radius: 12px;
  height: 48px;
  font-size: 16px;
}

.record-btn:hover {
  background: #5f7a6a;
  border-color: #5f7a6a;
}

/* AI咨询卡片 - 2列2行 */
.ai-card {
  grid-column: span 2;
  grid-row: span 2;
  background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
  border-radius: 32px;
  padding: 32px;
  color: white;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  transition: all 0.3s;
  position: relative;
  overflow: hidden;
}

.ai-card::before {
  content: "";
  position: absolute;
  inset: 0;
  background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100"><rect width="100" height="100" fill="%23ffffff" opacity="0.02"/></svg>');
  opacity: 0.2;
}

.ai-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 24px 72px rgba(0, 0, 0, 0.4);
}

.ai-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  position: relative;
  z-index: 1;
}

.ai-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ai-icon {
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.ai-title {
  font-size: 18px;
  font-weight: bold;
  margin: 0;
}

.ai-subtitle {
  font-size: 12px;
  color: #94a3b8;
  margin: 4px 0 0 0;
}

.arrow-icon {
  font-size: 20px;
  transition: transform 0.3s;
}

.ai-card:hover .arrow-icon {
  transform: rotate(-45deg);
}

.ai-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  position: relative;
  z-index: 1;
}

.ai-quote {
  font-size: 28px;
  font-style: italic;
  margin-bottom: 12px;
}

.ai-desc {
  font-size: 14px;
  color: #94a3b8;
}

/* 心理测评卡片 */
.feature-card {
  grid-column: span 1;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.feature-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  transition: transform 0.3s;
}

.feature-icon.warm {
  background: rgba(196, 124, 107, 0.1);
  color: #c47c6b;
}

.feature-card:hover .feature-icon {
  transform: scale(1.1);
}

.feature-title {
  font-size: 18px;
  font-weight: bold;
  color: #2c3e50;
  margin: 0;
}

.feature-subtitle {
  font-size: 12px;
  color: #9ca3af;
  margin: 4px 0 0 0;
}

.progress-bar {
  width: 100%;
  height: 6px;
  background: #e5e7eb;
  border-radius: 3px;
  overflow: hidden;
  margin-top: 8px;
}

.progress-fill {
  height: 100%;
  background: #7b9e89;
  border-radius: 3px;
  transition: width 0.3s;
}

.progress-text {
  font-size: 12px;
  color: #9ca3af;
  margin: 8px 0 0 0;
}

/* 咨询师卡片 - 2列 */
.counselor-card {
  grid-column: span 2;
  cursor: pointer;
}

.counselor-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.counselor-info {
  flex: 1;
}

.counselor-name {
  font-size: 20px;
  font-weight: bold;
  color: #2c3e50;
  margin: 8px 0 4px 0;
}

.counselor-desc {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.counselor-avatar {
  border: 3px solid white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* 冥想卡片 */
.meditation-card {
  grid-column: span 1;
  background: #7b9e89;
  color: white;
  border-radius: 32px;
  padding: 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}

.meditation-card:hover {
  background: #5f7a6a;
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(123, 158, 137, 0.3);
}

.meditation-icon {
  width: 48px;
  height: 48px;
  border: 2px solid white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  margin-bottom: 12px;
}

.meditation-title {
  font-size: 20px;
  font-weight: 500;
  margin: 0 0 4px 0;
}

.meditation-subtitle {
  font-size: 12px;
  opacity: 0.8;
  margin: 0;
}

/* 图表卡片 - 4列 */
.chart-card {
  grid-column: span 4;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.chart-title {
  font-size: 20px;
  font-weight: bold;
  color: #2c3e50;
  display: flex;
  align-items: center;
  gap: 8px;
}

.chart-legend {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #6b7280;
}

.legend-dot {
  width: 12px;
  height: 12px;
  background: #7b9e89;
  border-radius: 50%;
}

.chart-container {
  margin-bottom: 24px;
}

.chart-loading,
.chart-empty {
  height: 280px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #9ca3af;
  margin-bottom: 24px;
}

.chart-loading .el-icon {
  font-size: 32px;
  color: var(--ease-accent);
}

.chart-empty p {
  margin: 0 0 8px 0;
}

.chart-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
  padding-top: 24px;
  border-top: 1px solid #e5e7eb;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #7b9e89;
}

.stat-label {
  font-size: 12px;
  color: #6b7280;
  margin-top: 4px;
}

/* 每日建议 - 2列 */
.tips-card {
  grid-column: span 2;
}

.tips-title {
  font-size: 18px;
  font-weight: bold;
  color: #2c3e50;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.tips-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tips-list li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 14px;
  color: #4b5563;
  line-height: 1.6;
}

/* 推荐音乐 - 2列 */
.music-card {
  grid-column: span 2;
}

.music-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.music-title {
  font-size: 18px;
  font-weight: bold;
  color: #2c3e50;
  display: flex;
  align-items: center;
  gap: 8px;
}

.music-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.music-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: background 0.3s;
}

.music-item:hover {
  background: rgba(255, 255, 255, 0.8);
}

.music-cover {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #7b9e89 0%, #5f7a6a 100%);
  border-radius: 8px;
  flex-shrink: 0;
}

.music-info {
  flex: 1;
}

.music-info h4 {
  font-size: 14px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0 0 4px 0;
}

.music-info p {
  font-size: 12px;
  color: #9ca3af;
  margin: 0;
}

.play-icon {
  font-size: 24px;
}

/* 响应式布局 */
@media (max-width: 1024px) {
  .grid-container {
    grid-template-columns: repeat(2, 1fr);
  }

  .mood-card,
  .ai-card,
  .chart-card {
    grid-column: span 2;
  }

  .counselor-card,
  .tips-card,
  .music-card {
    grid-column: span 2;
  }
}

@media (max-width: 768px) {
  .grid-container {
    grid-template-columns: 1fr;
  }

  .mood-card,
  .ai-card,
  .feature-card,
  .counselor-card,
  .meditation-card,
  .chart-card,
  .tips-card,
  .music-card {
    grid-column: span 1;
  }
}
</style>
