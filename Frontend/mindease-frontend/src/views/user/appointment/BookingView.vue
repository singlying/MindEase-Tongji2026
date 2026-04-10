<script setup lang="ts">
import { ref, computed, onMounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import {
  getCounselorDetail,
  type CounselorDetail,
} from "@/api/counselorRecommend";
import {
  getAvailableSlots,
  createAppointment,
  type TimeSlot,
} from "@/api/appointment";

const route = useRoute();
const router = useRouter();
const counselorId = computed(() => Number(route.params.counselorId));

// 状态
const loading = ref(false);
const submitting = ref(false);
const counselor = ref<CounselorDetail | null>(null);
const slots = ref<TimeSlot[]>([]);

// 选择状态
const selectedDate = ref("");
const selectedSlot = ref<TimeSlot | null>(null);
const userNote = ref("");

// 日期相关
const currentMonth = ref(new Date());
const weekDays = ["日", "一", "二", "三", "四", "五", "六"];

// 生成日历日期
const calendarDays = computed(() => {
  const year = currentMonth.value.getFullYear();
  const month = currentMonth.value.getMonth();
  const firstDay = new Date(year, month, 1).getDay();
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const days: { day: number; date: string; disabled: boolean }[] = [];
  // 填充空白
  for (let i = 0; i < firstDay; i++) {
    days.push({ day: 0, date: "", disabled: true });
  }
  // 填充日期
  for (let d = 1; d <= daysInMonth; d++) {
    const date = new Date(year, month, d);
    const dateStr = `${year}-${String(month + 1).padStart(2, "0")}-${String(
      d
    ).padStart(2, "0")}`;
    days.push({ day: d, date: dateStr, disabled: date < today });
  }
  return days;
});

const monthLabel = computed(() => {
  const y = currentMonth.value.getFullYear();
  const m = currentMonth.value.getMonth() + 1;
  return `${y}年${m}月`;
});

// 分组时段
const morningSlots = computed(() =>
  slots.value.filter((s) => parseInt(s.startTime) < 12)
);
const afternoonSlots = computed(() =>
  slots.value.filter(
    (s) => parseInt(s.startTime) >= 12 && parseInt(s.startTime) < 18
  )
);
const eveningSlots = computed(() =>
  slots.value.filter((s) => parseInt(s.startTime) >= 18)
);

// 预约信息
const bookingInfo = computed(() => {
  if (!selectedDate.value || !selectedSlot.value || !counselor.value)
    return null;
  const [y, m, d] = selectedDate.value.split("-");
  return {
    counselor: counselor.value.realName,
    date: `${y}年${m}月${d}日`,
    time: `${selectedSlot.value.startTime} - ${selectedSlot.value.endTime}`,
    price: counselor.value.pricePerHour,
  };
});

// 加载咨询师信息
const loadCounselor = async () => {
  try {
    const res = (await getCounselorDetail(counselorId.value)) as any;
    counselor.value = res.data;
  } catch (e) {
    ElMessage.error("加载咨询师信息失败");
  }
};

// 加载可用时段
const loadSlots = async () => {
  if (!selectedDate.value) return;
  loading.value = true;
  try {
    const res = (await getAvailableSlots(
      counselorId.value,
      selectedDate.value
    )) as any;
    slots.value = res.data.slots || [];
    selectedSlot.value = null;
  } catch (e) {
    slots.value = [];
  } finally {
    loading.value = false;
  }
};

// 选择日期
const selectDate = (date: string) => {
  if (!date) return;
  selectedDate.value = date;
};

// 选择时段
const selectSlot = (slot: TimeSlot) => {
  if (!slot.available) return;
  selectedSlot.value = slot;
};

// 切换月份
const changeMonth = (delta: number) => {
  const d = new Date(currentMonth.value);
  d.setMonth(d.getMonth() + delta);
  currentMonth.value = d;
};

// 提交预约
const submitBooking = async () => {
  if (!selectedDate.value || !selectedSlot.value) {
    ElMessage.warning("请选择预约时间");
    return;
  }
  submitting.value = true;
  try {
    await createAppointment({
      counselorId: counselorId.value,
      startTime: `${selectedDate.value} ${selectedSlot.value.startTime}:00`,
      endTime: `${selectedDate.value} ${selectedSlot.value.endTime}:00`,
      userNote: userNote.value || undefined,
    });
    ElMessage.success("预约成功！");
    router.push("/my-appointments");
  } catch (e) {
    ElMessage.error("预约失败，请重试");
  } finally {
    submitting.value = false;
  }
};

// 监听日期变化
watch(selectedDate, loadSlots);

onMounted(() => {
  loadCounselor();
  // 默认选中今天
  const today = new Date();
  selectedDate.value = `${today.getFullYear()}-${String(
    today.getMonth() + 1
  ).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;
});
</script>

<template>
  <div class="booking-page">
    <!-- 返回按钮 -->
    <button class="back-btn" @click="router.push('/counselor-list')">
      <el-icon><ArrowLeft /></el-icon>
      <span>返回咨询师列表</span>
    </button>

    <div class="booking-layout">
      <!-- 左侧：咨询师信息 -->
      <aside class="counselor-sidebar glass-panel" v-if="counselor">
        <div class="counselor-profile">
          <el-avatar
            :size="128"
            :src="counselor.avatar || undefined"
            class="avatar"
          />
          <h2>{{ counselor.realName }}</h2>
          <p class="title">{{ counselor.title }}</p>
          <div class="rating">
            <el-rate :model-value="counselor.rating" disabled />
            <span
              >{{ counselor.rating }} ({{ counselor.reviewCount }}条评价)</span
            >
          </div>
        </div>
        <div class="counselor-info">
          <div class="info-item">
            <el-icon class="icon"><Briefcase /></el-icon>
            <div>
              <p class="label">执业经验</p>
              <p class="value">{{ counselor.experienceYears }}年</p>
            </div>
          </div>
          <div class="info-item">
            <el-icon class="icon"><Location /></el-icon>
            <div>
              <p class="label">咨询地点</p>
              <p class="value">{{ counselor.location || "线上咨询" }}</p>
            </div>
          </div>
          <div class="info-item">
            <el-icon class="icon"><Money /></el-icon>
            <div>
              <p class="label">咨询费用</p>
              <p class="value">¥{{ counselor.pricePerHour }}/小时</p>
            </div>
          </div>
          <div class="info-item">
            <el-icon class="icon"><Star /></el-icon>
            <div>
              <p class="label">擅长领域</p>
              <div class="tags">
                <span
                  v-for="tag in counselor.specialty"
                  :key="tag"
                  class="tag"
                  >{{ tag }}</span
                >
              </div>
            </div>
          </div>
        </div>
      </aside>

      <!-- 右侧：预约表单 -->
      <main class="booking-main glass-panel">
        <h3 class="section-title">
          <el-icon><Calendar /></el-icon> 选择预约时间
        </h3>

        <!-- 日期选择 -->
        <div class="calendar-section">
          <div class="calendar-header">
            <span class="month-label">{{ monthLabel }}</span>
            <div class="month-nav">
              <el-button text circle @click="changeMonth(-1)"
                ><el-icon><ArrowLeft /></el-icon
              ></el-button>
              <el-button text circle @click="changeMonth(1)"
                ><el-icon><ArrowRight /></el-icon
              ></el-button>
            </div>
          </div>
          <div class="calendar-weekdays">
            <span v-for="w in weekDays" :key="w">{{ w }}</span>
          </div>
          <div class="calendar-grid">
            <button
              v-for="(d, i) in calendarDays"
              :key="i"
              class="calendar-day"
              :class="{ active: d.date === selectedDate }"
              :disabled="d.disabled || !d.day"
              @click="selectDate(d.date)"
            >
              {{ d.day || "" }}
            </button>
          </div>
        </div>

        <!-- 时间段选择 -->
        <div class="slots-section" v-loading="loading">
          <template v-if="selectedDate && slots.length">
            <div v-if="morningSlots.length" class="slot-group">
              <p class="slot-label">
                <el-icon color="#eab308"><Sunny /></el-icon> 上午
              </p>
              <div class="slot-grid">
                <button
                  v-for="s in morningSlots"
                  :key="s.startTime"
                  class="time-slot"
                  :class="{ selected: selectedSlot?.startTime === s.startTime }"
                  :disabled="!s.available"
                  @click="selectSlot(s)"
                >
                  {{ s.startTime }}
                </button>
              </div>
            </div>
            <div v-if="afternoonSlots.length" class="slot-group">
              <p class="slot-label">
                <el-icon color="#f97316"><PartlyCloudy /></el-icon> 下午
              </p>
              <div class="slot-grid">
                <button
                  v-for="s in afternoonSlots"
                  :key="s.startTime"
                  class="time-slot"
                  :class="{ selected: selectedSlot?.startTime === s.startTime }"
                  :disabled="!s.available"
                  @click="selectSlot(s)"
                >
                  {{ s.startTime }}
                </button>
              </div>
            </div>
            <div v-if="eveningSlots.length" class="slot-group">
              <p class="slot-label">
                <el-icon color="#6366f1"><Moon /></el-icon> 晚上
              </p>
              <div class="slot-grid">
                <button
                  v-for="s in eveningSlots"
                  :key="s.startTime"
                  class="time-slot"
                  :class="{ selected: selectedSlot?.startTime === s.startTime }"
                  :disabled="!s.available"
                  @click="selectSlot(s)"
                >
                  {{ s.startTime }}
                </button>
              </div>
            </div>
          </template>
          <div v-else-if="selectedDate" class="empty-slots">
            <p>该日期暂无可用时段</p>
          </div>
        </div>

        <!-- 预约确认 -->
        <div v-if="bookingInfo" class="confirm-box">
          <h4>预约信息确认</h4>
          <div class="confirm-row">
            <span class="label">咨询师</span
            ><span class="value">{{ bookingInfo.counselor }}</span>
          </div>
          <div class="confirm-row">
            <span class="label">日期</span
            ><span class="value">{{ bookingInfo.date }}</span>
          </div>
          <div class="confirm-row">
            <span class="label">时间</span
            ><span class="value">{{ bookingInfo.time }}</span>
          </div>
          <div class="confirm-row">
            <span class="label">费用</span
            ><span class="price">¥{{ bookingInfo.price }}</span>
          </div>
        </div>

        <!-- 备注 -->
        <div class="note-section">
          <label>备注信息（可选）</label>
          <el-input
            v-model="userNote"
            type="textarea"
            :rows="3"
            placeholder="请简要描述您希望咨询的问题..."
          />
        </div>

        <!-- 提交 -->
        <el-button
          type="primary"
          size="large"
          class="submit-btn"
          :loading="submitting"
          @click="submitBooking"
        >
          <el-icon><Check /></el-icon> 确认预约
        </el-button>
        <p class="tip">
          <el-icon><InfoFilled /></el-icon> 预约后我们会通过短信和APP通知您
        </p>
      </main>
    </div>
  </div>
</template>

<style scoped>
.booking-page {
  max-width: 1200px;
  margin: 0 auto;
}

/* 返回按钮 */
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  margin-bottom: 20px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  color: var(--gray-700);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  backdrop-filter: blur(10px);
}

.back-btn:hover {
  background: white;
  color: var(--ease-accent);
  transform: translateX(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.back-btn .el-icon {
  font-size: 16px;
}

.booking-layout {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 24px;
}

/* 咨询师侧边栏 */
.counselor-sidebar {
  position: sticky;
  top: 24px;
  height: fit-content;
}
.counselor-profile {
  text-align: center;
  margin-bottom: 24px;
}
.counselor-profile .avatar {
  border: 4px solid white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  margin-bottom: 12px;
}
.counselor-profile h2 {
  font-size: 24px;
  margin: 0 0 4px;
}
.counselor-profile .title {
  color: var(--gray-500);
  font-size: 14px;
  margin: 0 0 12px;
}
.counselor-profile .rating {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
  color: var(--gray-600);
}
.counselor-info {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.counselor-info .tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

/* 预约主区域 */
.booking-main {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 日历 */
.calendar-section {
  margin-bottom: 8px;
}
.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.month-label {
  font-weight: 600;
  color: var(--ease-dark);
}
.calendar-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  font-size: 12px;
  color: var(--gray-500);
  margin-bottom: 8px;
}
.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}

/* 时段 */
.slots-section {
  min-height: 120px;
}
.slot-group {
  margin-bottom: 16px;
}
.slot-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--gray-600);
  margin-bottom: 12px;
}
.slot-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.empty-slots {
  text-align: center;
  padding: 40px;
  color: var(--gray-400);
}

/* 备注 */
.note-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.note-section label {
  font-weight: 600;
  font-size: 14px;
}

/* 提交 */
.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
}
.tip {
  text-align: center;
  font-size: 12px;
  color: var(--gray-500);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

/* 响应式 */
@media (max-width: 768px) {
  .booking-layout {
    grid-template-columns: 1fr;
  }
  .counselor-sidebar {
    position: static;
  }
  .slot-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>
