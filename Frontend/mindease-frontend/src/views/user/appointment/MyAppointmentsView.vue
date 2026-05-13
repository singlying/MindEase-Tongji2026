<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  getMyAppointments,
  cancelAppointment,
  type AppointmentListItem,
} from "@/api/appointment";
import { submitReview } from "@/api/counselorRecommend";

const router = useRouter();

// 状态
const loading = ref(false);
const appointments = ref<AppointmentListItem[]>([]);
const total = ref(0);
const currentPage = ref(1);
const statusFilter = ref("");

// 评价弹窗状态
const showReviewDialog = ref(false);
const reviewLoading = ref(false);
const currentAppointment = ref<AppointmentListItem | null>(null);
const reviewForm = ref({
  rating: 5,
  content: "",
});

// 状态选项
const statusOptions = [
  { label: "全部", value: "" },
  { label: "待确认", value: "PENDING" },
  { label: "已确认", value: "CONFIRMED" },
  { label: "已完成", value: "COMPLETED" },
  { label: "已取消", value: "CANCELLED" },
];

// 状态映射
const statusMap: Record<string, { label: string; type: string }> = {
  PENDING: { label: "待确认", type: "warning" },
  CONFIRMED: { label: "已确认", type: "success" },
  COMPLETED: { label: "已完成", type: "info" },
  CANCELLED: { label: "已取消", type: "danger" },
};

// 加载预约列表
const loadAppointments = async () => {
  loading.value = true;
  try {
    const res = (await getMyAppointments(
      statusFilter.value || undefined,
      currentPage.value
    )) as any;
    appointments.value = res.data.list || [];
    total.value = res.data.total || 0;
  } catch (e) {
    appointments.value = [];
  } finally {
    loading.value = false;
  }
};

// 筛选变化
const handleFilterChange = () => {
  currentPage.value = 1;
  loadAppointments();
};

// 取消预约
const handleCancel = async (id: number) => {
  try {
    const { value } = await ElMessageBox.prompt("请输入取消原因", "取消预约", {
      confirmButtonText: "确认取消",
      cancelButtonText: "返回",
      inputPlaceholder: "例如：时间冲突",
    });
    await cancelAppointment(id, value || "用户取消");
    ElMessage.success("预约已取消");
    loadAppointments();
  } catch {
    // 用户取消操作
  }
};

// 打开评价弹窗
const openReviewDialog = (item: AppointmentListItem) => {
  currentAppointment.value = item;
  reviewForm.value = { rating: 5, content: "" };
  showReviewDialog.value = true;
};

// 提交评价
const handleSubmitReview = async () => {
  if (!currentAppointment.value) return;
  if (!reviewForm.value.content.trim()) {
    ElMessage.warning("请填写评价内容");
    return;
  }
  reviewLoading.value = true;
  try {
    // 注意：后端需要counselorId，但我们列表里没有，这里用appointmentId让后端从预约中获取
    await submitReview({
      counselorId: 0, // 后端会从appointmentId获取
      appointmentId: currentAppointment.value.id,
      rating: reviewForm.value.rating,
      content: reviewForm.value.content,
    });
    ElMessage.success("评价提交成功");
    showReviewDialog.value = false;
    loadAppointments(); // 刷新列表
  } catch (e: any) {
    ElMessage.error(e.message || "评价提交失败");
  } finally {
    reviewLoading.value = false;
  }
};

// 格式化时间
const formatTime = (timeStr: string) => {
  const date = new Date(timeStr);
  const month = date.getMonth() + 1;
  const day = date.getDate();
  const hours = String(date.getHours()).padStart(2, "0");
  const minutes = String(date.getMinutes()).padStart(2, "0");
  return `${month}月${day}日 ${hours}:${minutes}`;
};

// 获取默认头像
const getAvatar = (url: string | null) =>
  url || "https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png";

onMounted(loadAppointments);
</script>

<template>
  <div class="appointments-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">
        <el-icon><Calendar /></el-icon> 我的预约
      </h2>
      <el-button type="primary" @click="router.push('/counselor-list')">
        <el-icon><Plus /></el-icon> 新建预约
      </el-button>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar glass-card">
      <span class="filter-label">状态筛选：</span>
      <el-radio-group v-model="statusFilter" @change="handleFilterChange">
        <el-radio-button
          v-for="opt in statusOptions"
          :key="opt.value"
          :value="opt.value"
        >
          {{ opt.label }}
        </el-radio-button>
      </el-radio-group>
    </div>

    <!-- 预约列表 -->
    <div v-loading="loading" class="appointments-list">
      <template v-if="appointments.length">
        <div
          v-for="item in appointments"
          :key="item.id"
          class="appointment-card"
        >
          <el-avatar :size="64" :src="getAvatar(item.targetAvatar)" />
          <div class="appointment-info">
            <div class="info-top">
              <h4>{{ item.targetName }}</h4>
              <el-tag :type="statusMap[item.status]?.type as any" size="small">
                {{ statusMap[item.status]?.label || item.status }}
              </el-tag>
            </div>
            <p class="time">
              <el-icon><Clock /></el-icon>
              {{ formatTime(item.startTime) }} -
              {{ formatTime(item.endTime).split(" ")[1] }}
            </p>
          </div>
          <div class="appointment-actions">
            <el-button
              v-if="item.status === 'PENDING' || item.status === 'CONFIRMED'"
              type="danger"
              text
              @click="handleCancel(item.id)"
            >
              取消预约
            </el-button>
            <el-button
              v-if="item.status === 'COMPLETED'"
              type="primary"
              text
              @click="openReviewDialog(item)"
            >
              评价
            </el-button>
          </div>
        </div>
      </template>

      <!-- 空状态 -->
      <div v-else-if="!loading" class="empty-state">
        <div class="empty-icon">
          <el-icon><Calendar /></el-icon>
        </div>
        <p>暂无预约记录</p>
        <el-button type="primary" @click="router.push('/counselor-list')"
          >立即预约</el-button
        >
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="total > 10" class="pagination">
      <el-pagination
        v-model:current-page="currentPage"
        :total="total"
        :page-size="10"
        layout="prev, pager, next"
        @current-change="loadAppointments"
      />
    </div>

    <!-- 评价弹窗 -->
    <el-dialog
      v-model="showReviewDialog"
      title="咨询评价"
      width="480px"
      :close-on-click-modal="false"
    >
      <div v-if="currentAppointment" class="review-dialog">
        <div class="review-target">
          <el-avatar
            :size="48"
            :src="getAvatar(currentAppointment.targetAvatar)"
          />
          <div>
            <h4>{{ currentAppointment.targetName }}</h4>
            <p class="time-info">
              {{ formatTime(currentAppointment.startTime) }}
            </p>
          </div>
        </div>
        <div class="review-form">
          <div class="form-item">
            <label>评分</label>
            <el-rate
              v-model="reviewForm.rating"
              :colors="['#f59e0b', '#f59e0b', '#f59e0b']"
            />
          </div>
          <div class="form-item">
            <label>评价内容</label>
            <el-input
              v-model="reviewForm.content"
              type="textarea"
              :rows="4"
              placeholder="请分享您的咨询体验，帮助其他用户..."
              maxlength="500"
              show-word-limit
            />
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showReviewDialog = false">取消</el-button>
        <el-button
          type="primary"
          :loading="reviewLoading"
          @click="handleSubmitReview"
          >提交评价</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.appointments-page {
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 24px;
  border-radius: 16px;
  margin-bottom: 24px;
}

.filter-label {
  font-weight: 600;
  color: var(--ease-dark);
  white-space: nowrap;
}

.appointments-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 300px;
}

.appointment-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px 24px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  transition: all 0.3s ease;
}

.appointment-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08);
}

.appointment-info {
  flex: 1;
}

.info-top {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.info-top h4 {
  margin: 0;
  font-size: 18px;
  color: var(--ease-dark);
}

.time {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--gray-500);
  font-size: 14px;
  margin: 0;
}

.appointment-actions {
  display: flex;
  gap: 8px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

/* 评价弹窗 */
.review-dialog {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.review-target {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: var(--gray-50);
  border-radius: 12px;
}

.review-target h4 {
  margin: 0 0 4px;
  font-size: 16px;
  color: var(--ease-dark);
}

.review-target .time-info {
  margin: 0;
  font-size: 13px;
  color: var(--gray-500);
}

.review-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.review-form .form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.review-form label {
  font-weight: 600;
  font-size: 14px;
  color: var(--ease-dark);
}

@media (max-width: 640px) {
  .appointment-card {
    flex-direction: column;
    text-align: center;
  }
  .info-top {
    justify-content: center;
  }
  .time {
    justify-content: center;
  }
  .filter-bar {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
