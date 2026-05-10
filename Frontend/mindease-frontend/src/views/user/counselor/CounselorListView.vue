<script setup lang="ts">
import { ref, onMounted, computed } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import {
  getRecommendStatus,
  getRecommendCounselors,
  getCounselorDetail,
  getCounselorReviews,
  type RecommendStatus,
  type CounselorRecommend,
  type CounselorDetail,
  type CounselorReview,
} from "@/api/counselorRecommend";

const router = useRouter();

// 状态
const loading = ref(false);
const recommendStatus = ref<RecommendStatus | null>(null);
const counselors = ref<CounselorRecommend[]>([]);
const recommendContext = ref<{
  strategy: string;
  basedOn: string;
  userTags: string[];
} | null>(null);

// 筛选条件
const filterSpecialty = ref("");
const sortBy = ref<"smart" | "price_asc" | "rating_desc">("smart");
const searchKeyword = ref("");

// 后端现仅保留 keyword 参数做统一模糊查询，这里将搜索词与专长筛选合并传递
const buildKeyword = () => {
  const parts = [searchKeyword.value.trim(), filterSpecialty.value.trim()].filter(
    (v) => v
  );
  return parts.join(" ");
};

// 专长选项
const specialtyOptions = [
  { label: "全部专长", value: "" },
  { label: "焦虑症", value: "焦虑症" },
  { label: "抑郁症", value: "抑郁症" },
  { label: "压力管理", value: "压力管理" },
  { label: "情感问题", value: "情感问题" },
  { label: "学业压力", value: "学业压力" },
  { label: "职场压力", value: "职场压力" },
  { label: "家庭关系", value: "家庭关系" },
];

// 排序选项
const sortOptions = [
  { label: "智能推荐", value: "smart" },
  { label: "评分最高", value: "rating_desc" },
  { label: "价格最低", value: "price_asc" },
];

// 弹窗相关
const showDetailModal = ref(false);
const detailLoading = ref(false);
const selectedCounselor = ref<CounselorDetail | null>(null);
const counselorReviews = ref<CounselorReview[]>([]);

// 加载推荐状态
const loadRecommendStatus = async () => {
  try {
    const res = (await getRecommendStatus()) as any;
    recommendStatus.value = res.data;
  } catch (error) {
    console.error("加载推荐状态失败:", error);
  }
};

// 加载咨询师列表
const loadCounselors = async () => {
  loading.value = true;
  try {
    const keyword = buildKeyword();
    const params: any = { sort: sortBy.value };
    if (keyword) params.keyword = keyword;

    const res = (await getRecommendCounselors(params)) as any;
    counselors.value = res.data.counselors || [];
    recommendContext.value = res.data.recommendContext;
  } catch (error) {
    console.error("加载咨询师列表失败:", error);
    counselors.value = [];
  } finally {
    loading.value = false;
  }
};

// 搜索
const handleSearch = () => {
  loadCounselors();
};

// 筛选变化
const handleFilterChange = () => {
  loadCounselors();
};

// 重置筛选
const resetFilters = () => {
  searchKeyword.value = "";
  filterSpecialty.value = "";
  sortBy.value = "smart";
  loadCounselors();
};

// 打开咨询师详情弹窗
const openCounselorDetail = async (counselor: CounselorRecommend) => {
  showDetailModal.value = true;
  detailLoading.value = true;
  try {
    const [detailRes, reviewRes] = await Promise.all([
      getCounselorDetail(counselor.id),
      getCounselorReviews(counselor.id, 5, 0),
    ]);
    selectedCounselor.value = (detailRes as any).data;
    counselorReviews.value = (reviewRes as any).data.reviews || [];
  } catch (error) {
    console.error("加载咨询师详情失败:", error);
    ElMessage.error("加载详情失败");
  } finally {
    detailLoading.value = false;
  }
};

// 关闭弹窗
const closeDetailModal = () => {
  showDetailModal.value = false;
  selectedCounselor.value = null;
  counselorReviews.value = [];
};

// 前往预约页面
const goToBooking = (counselorId: number) => {
  closeDetailModal();
  router.push(`/booking/${counselorId}`);
};

// 获取可用时间显示文本
const getAvailabilityText = (nextAvailableTime: string | null) => {
  if (!nextAvailableTime) return "暂无排班";
  // 简单判断
  const date = new Date(nextAvailableTime);
  const today = new Date();
  const tomorrow = new Date(today);
  tomorrow.setDate(tomorrow.getDate() + 1);

  if (date.toDateString() === today.toDateString()) return "今日可约";
  if (date.toDateString() === tomorrow.toDateString()) return "明日可约";
  return "本周可约";
};

// 获取可用状态类型
const getAvailabilityType = (nextAvailableTime: string | null) => {
  if (!nextAvailableTime) return "info";
  const date = new Date(nextAvailableTime);
  const today = new Date();
  if (date.toDateString() === today.toDateString()) return "success";
  return "warning";
};

// 获取默认头像
const getAvatarUrl = (avatar: string | null) => {
  return (
    avatar ||
    "https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png"
  );
};

// 初始化
onMounted(() => {
  loadRecommendStatus();
  loadCounselors();
});
</script>

<template>
  <div class="counselor-list-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 class="page-title">专家推荐</h2>
      <div class="header-actions">
        <el-button @click="router.push('/my-appointments')">
          <el-icon><Calendar /></el-icon> 我的预约
        </el-button>
        <div class="quick-filters">
          <el-button
            v-for="opt in specialtyOptions.slice(1, 4)"
            :key="opt.value"
            :type="filterSpecialty === opt.value ? 'primary' : 'default'"
            round
            size="small"
            @click="
              filterSpecialty = filterSpecialty === opt.value ? '' : opt.value;
              handleFilterChange();
            "
          >
            {{ opt.label }}
          </el-button>
          <el-button
            :type="filterSpecialty === '' ? 'primary' : 'default'"
            round
            size="small"
            @click="
              filterSpecialty = '';
              handleFilterChange();
            "
          >
            全部
          </el-button>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar glass-card">
      <div class="filter-row">
        <span class="filter-label">
          <el-icon><Filter /></el-icon>
          筛选：
        </span>
        <el-select
          v-model="filterSpecialty"
          placeholder="专长"
          clearable
          style="width: 120px"
          @change="handleFilterChange"
        >
          <el-option
            v-for="opt in specialtyOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <el-select
          v-model="sortBy"
          placeholder="排序"
          style="width: 110px"
          @change="handleFilterChange"
        >
          <el-option
            v-for="opt in sortOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <el-input
          v-model="searchKeyword"
          placeholder="搜索姓名/职称/地区/专长..."
          clearable
          style="width: 200px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
      </div>
      <el-button text type="primary" @click="resetFilters">
        <el-icon><Refresh /></el-icon>
        重置
      </el-button>
    </div>

    <!-- AI推荐提示 -->
    <div
      v-if="recommendContext && recommendStatus?.recommendationReady"
      class="ai-tip glass-card"
    >
      <div class="ai-tip-icon">
        <el-icon><MagicStick /></el-icon>
      </div>
      <div class="ai-tip-content">
        <h3>智能匹配结果</h3>
        <p>
          根据你近期的情绪日记分析和心理测评结果，我们为你推荐以下专业咨询师。他们在
          <span
            v-for="(tag, index) in recommendContext.userTags?.slice(0, 2)"
            :key="tag"
            class="highlight"
          >
            {{ tag }}<span v-if="index < 1">、</span>
          </span>
          方面有丰富经验，非常适合你当前的需求。
        </p>
      </div>
      <el-button text type="primary" @click="router.push('/emotion-report')">
        查看分析详情
        <el-icon><ArrowRight /></el-icon>
      </el-button>
    </div>

    <!-- 未完成推荐条件提示 -->
    <div
      v-else-if="recommendStatus && !recommendStatus.recommendationReady"
      class="status-tip glass-card"
    >
      <el-icon class="status-icon"><InfoFilled /></el-icon>
      <div class="status-content">
        <h4>完善信息获得更精准推荐</h4>
        <p>
          完成
          <span
            v-if="!recommendStatus.hasAssessment"
            class="action-link"
            @click="router.push('/assessment')"
            >心理测评</span
          >
          <span
            v-if="!recommendStatus.hasAssessment && !recommendStatus.hasMoodLog"
            >和</span
          >
          <span
            v-if="!recommendStatus.hasMoodLog"
            class="action-link"
            @click="router.push('/mood-diary')"
            >情绪日记</span
          >
          后，我们将为您提供更精准的咨询师推荐。
        </p>
      </div>
    </div>

    <!-- 咨询师列表 -->
    <div v-loading="loading" class="counselor-grid">
      <template v-if="counselors.length > 0">
        <div
          v-for="counselor in counselors"
          :key="counselor.id"
          class="counselor-card glass-panel"
          @click="openCounselorDetail(counselor)"
        >
          <div class="counselor-avatar">
            <el-avatar :size="112" :src="getAvatarUrl(counselor.avatar)" />
            <div class="rating-badge">
              <el-icon><Star /></el-icon>
              <span>{{ counselor.rating?.toFixed(1) || "5.0" }}</span>
            </div>
          </div>
          <div class="counselor-info">
            <div class="info-header">
              <h3 class="counselor-name">{{ counselor.realName }}</h3>
              <p class="counselor-title">
                {{ counselor.title }} • {{ counselor.experienceYears }}年经验
              </p>
              <div class="counselor-tags">
                <span
                  v-for="tag in counselor.specialty?.slice(0, 3)"
                  :key="tag"
                  class="tag tag-accent"
                >
                  {{ tag }}
                </span>
              </div>
            </div>
            <div class="info-footer">
              <el-tag
                :type="getAvailabilityType(counselor.nextAvailableTime)"
                size="small"
                effect="light"
              >
                <span
                  v-if="
                    getAvailabilityType(counselor.nextAvailableTime) ===
                    'success'
                  "
                  class="pulse-dot"
                ></span>
                {{ getAvailabilityText(counselor.nextAvailableTime) }}
              </el-tag>
              <el-button
                type="primary"
                size="small"
                @click.stop="goToBooking(counselor.id)"
              >
                预约详情
              </el-button>
            </div>
          </div>
        </div>
      </template>

      <!-- 空状态 -->
      <div v-else-if="!loading" class="empty-state">
        <div class="empty-icon">
          <el-icon><User /></el-icon>
        </div>
        <p class="empty-text">暂无匹配的咨询师</p>
        <el-button type="primary" @click="resetFilters">重置筛选</el-button>
      </div>
    </div>

    <!-- 咨询师详情弹窗 -->
    <el-dialog
      v-model="showDetailModal"
      :show-close="false"
      width="900px"
      class="counselor-detail-dialog"
      destroy-on-close
    >
      <template #header>
        <div class="dialog-header">
          <span></span>
          <el-button text circle @click="closeDetailModal">
            <el-icon :size="20"><Close /></el-icon>
          </el-button>
        </div>
      </template>

      <div v-loading="detailLoading" class="detail-content">
        <template v-if="selectedCounselor">
          <div class="detail-layout">
            <!-- 左侧信息 -->
            <div class="detail-left">
              <div class="profile-section">
                <el-avatar
                  :size="128"
                  :src="getAvatarUrl(selectedCounselor.avatar)"
                />
                <div class="rating-badge large">
                  <el-icon><Star /></el-icon>
                  <span>{{
                    selectedCounselor.rating?.toFixed(1) || "5.0"
                  }}</span>
                </div>
              </div>
              <h2 class="detail-name">{{ selectedCounselor.realName }}</h2>
              <p class="detail-title">{{ selectedCounselor.title }}</p>

              <div class="detail-info-list">
                <div class="info-item">
                  <el-icon><Briefcase /></el-icon>
                  <div>
                    <span class="label">从业经验</span>
                    <span class="value"
                      >{{ selectedCounselor.experienceYears }}年</span
                    >
                  </div>
                </div>
                <div class="info-item">
                  <el-icon><Money /></el-icon>
                  <div>
                    <span class="label">咨询费用</span>
                    <span class="value"
                      >¥{{ selectedCounselor.pricePerHour }} / 小时</span
                    >
                  </div>
                </div>
                <div class="info-item">
                  <el-icon><Location /></el-icon>
                  <div>
                    <span class="label">咨询地点</span>
                    <span class="value"
                      >{{ selectedCounselor.location || "线上咨询" }}</span
                    >
                  </div>
                </div>
                <div class="info-item specialty-item">
                  <p class="label">擅长领域</p>
                  <div class="specialty-tags">
                    <span
                      v-for="tag in selectedCounselor.specialty"
                      :key="tag"
                      class="tag tag-accent"
                    >
                      {{ tag }}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 右侧评价和操作 -->
            <div class="detail-right">
              <div class="bio-section" v-if="selectedCounselor.bio">
                <h4>咨询师简介</h4>
                <p>{{ selectedCounselor.bio }}</p>
              </div>

              <div class="reviews-section">
                <h4>用户评价 ({{ selectedCounselor.reviewCount || 0 }})</h4>
                <div v-if="counselorReviews.length > 0" class="reviews-list">
                  <div
                    v-for="review in counselorReviews"
                    :key="review.id"
                    class="review-item"
                  >
                    <div class="review-header">
                      <el-avatar
                        :size="32"
                        :src="getAvatarUrl(review.avatar)"
                      />
                      <span class="reviewer-name">{{ review.nickname }}</span>
                      <div class="review-rating">
                        <el-icon
                          v-for="n in review.rating"
                          :key="n"
                          color="#f59e0b"
                          ><Star
                        /></el-icon>
                      </div>
                    </div>
                    <p class="review-content">{{ review.content }}</p>
                    <span class="review-time">{{ review.createTime }}</span>
                  </div>
                </div>
                <div v-else class="no-reviews">暂无评价</div>
              </div>

              <div class="action-section">
                <el-button
                  type="primary"
                  size="large"
                  class="booking-btn"
                  @click="goToBooking(selectedCounselor.id)"
                >
                  <el-icon><Calendar /></el-icon>
                  立即预约
                </el-button>
              </div>
            </div>
          </div>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.counselor-list-page {
  max-width: 1400px;
  margin: 0 auto;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--ease-dark);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.quick-filters {
  display: flex;
  gap: 8px;
}

/* 筛选栏 */
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-radius: 16px;
  margin-bottom: 24px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: var(--ease-dark);
}

.filter-label .el-icon {
  color: var(--ease-accent);
}

/* AI推荐提示 */
.ai-tip {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 24px;
  border-radius: 24px;
  margin-bottom: 24px;
}

.ai-tip-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: var(--ease-accent);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ai-tip-icon .el-icon {
  font-size: 24px;
  color: white;
}

.ai-tip-content {
  flex: 1;
}

.ai-tip-content h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--ease-dark);
  margin: 0 0 8px 0;
}

.ai-tip-content p {
  font-size: 14px;
  color: var(--gray-600);
  margin: 0;
  line-height: 1.6;
}

.ai-tip-content .highlight {
  font-weight: 600;
  color: var(--ease-accent-dark);
}

/* 状态提示 */
.status-tip {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 20px 24px;
  border-radius: 16px;
  margin-bottom: 24px;
  background: rgba(251, 191, 36, 0.1);
  border: 1px solid rgba(251, 191, 36, 0.3);
}

.status-icon {
  font-size: 24px;
  color: #f59e0b;
  flex-shrink: 0;
}

.status-content h4 {
  font-size: 15px;
  font-weight: 600;
  color: var(--ease-dark);
  margin: 0 0 4px 0;
}

.status-content p {
  font-size: 14px;
  color: var(--gray-600);
  margin: 0;
}

.action-link {
  color: var(--ease-accent);
  cursor: pointer;
  text-decoration: underline;
}

.action-link:hover {
  color: var(--ease-accent-dark);
}

/* 咨询师网格 */
.counselor-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
  min-height: 300px;
}

/* 咨询师卡片 */
.counselor-card {
  display: flex;
  gap: 24px;
  padding: 24px;
  border-radius: 24px;
  cursor: pointer;
  transition: all 0.3s ease;
  border-left: 4px solid transparent;
}

.counselor-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  border-left-color: var(--ease-accent);
}

.counselor-avatar {
  position: relative;
  flex-shrink: 0;
}

.counselor-avatar .el-avatar {
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.rating-badge {
  position: absolute;
  bottom: -8px;
  right: -8px;
  background: white;
  padding: 4px 8px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 700;
  color: var(--gray-700);
}

.rating-badge .el-icon {
  color: #f59e0b;
  font-size: 12px;
}

.rating-badge.large {
  padding: 6px 12px;
  font-size: 14px;
  bottom: -12px;
  right: -12px;
}

.rating-badge.large .el-icon {
  font-size: 14px;
}

.counselor-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.counselor-name {
  font-size: 20px;
  font-weight: 700;
  color: var(--ease-dark);
  margin: 0 0 4px 0;
  transition: color 0.3s;
}

.counselor-card:hover .counselor-name {
  color: var(--ease-accent);
}

.counselor-title {
  font-size: 14px;
  color: var(--gray-500);
  margin: 0 0 12px 0;
}

.counselor-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.tag-accent {
  background: rgba(123, 158, 137, 0.15);
  color: var(--ease-accent-dark);
}

.info-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
}

.pulse-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  background: #22c55e;
  border-radius: 50%;
  margin-right: 6px;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

/* 空状态 */
.empty-state {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  text-align: center;
}

.empty-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: rgba(123, 158, 137, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.empty-icon .el-icon {
  font-size: 36px;
  color: var(--ease-accent);
}

.empty-text {
  color: var(--gray-500);
  margin-bottom: 16px;
}

/* 详情弹窗 */
.counselor-detail-dialog :deep(.el-dialog) {
  border-radius: 24px;
  overflow: hidden;
}

.counselor-detail-dialog :deep(.el-dialog__header) {
  padding: 16px 20px;
  margin: 0;
}

.counselor-detail-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-content {
  min-height: 400px;
}

.detail-layout {
  display: flex;
  min-height: 500px;
}

.detail-left {
  width: 280px;
  padding: 32px;
  background: linear-gradient(
    to bottom,
    rgba(123, 158, 137, 0.08),
    transparent
  );
  border-right: 1px solid rgba(255, 255, 255, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.profile-section {
  position: relative;
  margin-bottom: 16px;
}

.profile-section .el-avatar {
  border-radius: 16px;
  border: 4px solid white;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.detail-name {
  font-size: 24px;
  font-weight: 700;
  color: var(--ease-dark);
  margin: 0 0 4px 0;
  text-align: center;
}

.detail-title {
  font-size: 14px;
  color: var(--gray-500);
  margin: 0 0 24px 0;
  text-align: center;
}

.detail-info-list {
  width: 100%;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 16px;
  padding: 16px;
}

.info-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.info-item:last-child {
  border-bottom: none;
}

.info-item .el-icon {
  color: var(--ease-accent);
  font-size: 18px;
  margin-top: 2px;
}

.info-item .label {
  display: block;
  font-size: 12px;
  color: var(--gray-400);
}

.info-item .value {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--ease-dark);
}

.specialty-item {
  flex-direction: column;
  gap: 8px;
}

.specialty-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.detail-right {
  flex: 1;
  padding: 32px;
  display: flex;
  flex-direction: column;
}

.bio-section {
  margin-bottom: 24px;
}

.bio-section h4,
.reviews-section h4 {
  font-size: 16px;
  font-weight: 600;
  color: var(--ease-dark);
  margin: 0 0 12px 0;
}

.bio-section p {
  font-size: 14px;
  color: var(--gray-600);
  line-height: 1.8;
  margin: 0;
}

.reviews-section {
  flex: 1;
  overflow: hidden;
}

.reviews-list {
  max-height: 250px;
  overflow-y: auto;
}

.review-item {
  padding: 16px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 12px;
  margin-bottom: 12px;
}

.review-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.reviewer-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--ease-dark);
}

.review-rating {
  margin-left: auto;
  display: flex;
  gap: 2px;
}

.review-rating .el-icon {
  font-size: 14px;
}

.review-content {
  font-size: 14px;
  color: var(--gray-600);
  margin: 0 0 8px 0;
  line-height: 1.6;
}

.review-time {
  font-size: 12px;
  color: var(--gray-400);
}

.no-reviews {
  text-align: center;
  padding: 40px;
  color: var(--gray-400);
}

.action-section {
  margin-top: auto;
  padding-top: 24px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.booking-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  border-radius: 12px;
  background: var(--ease-accent);
  border-color: var(--ease-accent);
}

.booking-btn:hover {
  background: var(--ease-accent-dark);
  border-color: var(--ease-accent-dark);
}

/* 响应式 */
@media (max-width: 1024px) {
  .counselor-grid {
    grid-template-columns: 1fr;
  }

  .detail-layout {
    flex-direction: column;
  }

  .detail-left {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid rgba(255, 255, 255, 0.5);
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .filter-bar {
    flex-direction: column;
    gap: 16px;
  }

  .filter-row {
    width: 100%;
  }

  .counselor-card {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .info-footer {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
