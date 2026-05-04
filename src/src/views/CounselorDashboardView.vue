<template>
  <div class="staff-canvas">
    <section class="panel staff-hero">
      <div>
        <span class="section-kicker">咨询师工作台</span>
        <h2>今日预约与排班管理</h2>
        <p class="muted">集中查看来访者预约、确认待处理事项，并维护可预约时段。</p>
      </div>
      <button class="btn" @click="load">
        <span v-if="loading" class="spinner"></span>
        刷新预约
      </button>
    </section>

    <section class="metric-grid">
      <article v-for="metric in metrics" :key="metric.label" class="metric-card">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
        <small>{{ metric.caption }}</small>
      </article>
    </section>

    <section class="staff-layout">
      <form class="form-card stack schedule-card" @submit.prevent="createSchedule">
        <div>
          <span class="section-kicker">排班设置</span>
          <h2>每周可预约时间</h2>
        </div>
        <div class="weekday-grid">
          <button
            v-for="day in weekdays"
            :key="day.value"
            type="button"
            :class="['weekday-chip', { active: workDays.includes(day.value) }]"
            @click="toggleDay(day.value)"
          >
            {{ day.label }}
          </button>
        </div>
        <label class="field"><span>上午时段</span><input v-model="morning" class="input" placeholder="09:00-12:00" /></label>
        <label class="field"><span>下午时段</span><input v-model="afternoon" class="input" placeholder="14:00-18:00" /></label>
        <button class="btn" :disabled="saving">
          <span v-if="saving" class="spinner"></span>
          {{ saving ? "提交中" : "提交排班" }}
        </button>
        <p v-if="message" class="feedback-note" :class="{ 'danger-note': failed }">{{ message }}</p>
      </form>

      <section class="panel appointment-board">
        <div class="row-between">
          <div>
            <span class="section-kicker">来访预约</span>
            <h2>待处理队列</h2>
          </div>
          <span class="chip">{{ appointments.length }} 条</span>
        </div>
        <div v-if="loading" class="timeline-list">
          <div v-for="i in 3" :key="i" class="skeleton-card"></div>
        </div>
        <div v-else class="appointment-list">
          <article v-for="item in appointments" :key="item.id || item.appointmentId" class="appointment-card">
            <div class="timeline-emoji">{{ statusIcon(item.status) }}</div>
            <div>
              <div class="row-between">
                <strong>{{ item.targetName || item.userName || "来访者" }}</strong>
                <span class="chip">{{ statusText(item.status) }}</span>
              </div>
              <p class="muted">{{ formatTime(item.startTime) }} - {{ formatTime(item.endTime) }}</p>
              <p class="muted">{{ item.userNote || item.note || "暂无备注" }}</p>
            </div>
            <button class="btn secondary" :disabled="String(item.status).toUpperCase() !== 'PENDING'" @click="confirm(item.id || item.appointmentId)">确认</button>
          </article>
          <div v-if="!appointments.length" class="empty-state">暂无待处理预约</div>
        </div>
      </section>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { appointmentApi } from "@/api";

const appointments = ref<any[]>([]);
const message = ref("");
const morning = ref("09:00-12:00");
const afternoon = ref("14:00-18:00");
const workDays = ref([1, 2, 3, 4, 5]);
const loading = ref(false);
const saving = ref(false);
const failed = ref(false);
const weekdays = [
  { label: "周一", value: 1 }, { label: "周二", value: 2 }, { label: "周三", value: 3 },
  { label: "周四", value: 4 }, { label: "周五", value: 5 }, { label: "周六", value: 6 }, { label: "周日", value: 7 }
];

const pendingCount = computed(() => appointments.value.filter((item) => String(item.status).toUpperCase() === "PENDING").length);
const confirmedCount = computed(() => appointments.value.filter((item) => String(item.status).toUpperCase() === "CONFIRMED").length);
const metrics = computed(() => [
  { label: "全部预约", value: appointments.value.length, caption: "当前列表" },
  { label: "待确认", value: pendingCount.value, caption: "需要处理" },
  { label: "已确认", value: confirmedCount.value, caption: "后续跟进" },
  { label: "开放工作日", value: workDays.value.length, caption: "天/周" }
]);

const formatTime = (value?: string) => String(value || "").replace("T", " ").slice(0, 16);
const statusText = (status?: string) => ({ PENDING: "待确认", CONFIRMED: "已确认", COMPLETED: "已完成", CANCELLED: "已取消" }[String(status || "").toUpperCase()] || status || "未知");
const statusIcon = (status?: string) => String(status || "").toUpperCase() === "PENDING" ? "⏳" : "✓";
const toggleDay = (day: number) => {
  workDays.value = workDays.value.includes(day) ? workDays.value.filter((item) => item !== day) : [...workDays.value, day].sort();
};

const load = async () => {
  loading.value = true;
  try {
    const data = (await appointmentApi.mine({ page: 1, pageSize: 20 })).data;
    appointments.value = data.list || data.appointments || [];
  } finally {
    loading.value = false;
  }
};

const confirm = async (id: number) => {
  await appointmentApi.confirm(id);
  await load();
};

const createSchedule = async () => {
  saving.value = true;
  failed.value = false;
  const toHour = (value: string) => {
    const [start, end] = value.split("-").map((item) => item.trim());
    return { start, end };
  };
  try {
    const response = await appointmentApi.schedule({ workDays: workDays.value, workHours: [toHour(morning.value), toHour(afternoon.value)] });
    message.value = response.message || "排班已提交";
  } catch (err) {
    failed.value = true;
    message.value = err instanceof Error ? err.message : "排班提交失败";
  } finally {
    saving.value = false;
  }
};

onMounted(load);
</script>
