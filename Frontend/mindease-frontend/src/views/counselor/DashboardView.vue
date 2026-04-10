<template>
  <div class="counselor-dashboard">
    <div class="dashboard-header">
      <h1 class="page-title">咨询师工作台</h1>
      <p class="page-subtitle">
        欢迎回来，{{ userStore.userInfo?.nickname }} 医生
      </p>
    </div>

    <div class="stats-grid">
      <div class="stat-card glass-card">
        <div class="stat-icon blue">
          <el-icon><Calendar /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.todayAppointments }}</div>
          <div class="stat-label">今日预约</div>
        </div>
      </div>
      <div class="stat-card glass-card">
        <div class="stat-icon orange">
          <el-icon><Bell /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.pendingCount }}</div>
          <div class="stat-label">待确认预约</div>
        </div>
      </div>
      <div class="stat-card glass-card">
        <div class="stat-icon green">
          <el-icon><Check /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.monthlyTotal }}</div>
          <div class="stat-label">本月完成</div>
        </div>
      </div>
      <div class="stat-card glass-card">
        <div class="stat-icon purple">
          <el-icon><Star /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.currentRating }}</div>
          <div class="stat-label">当前评分</div>
        </div>
      </div>
    </div>

    <div class="main-content-grid">
      <div class="appointments-section glass-card">
        <div class="section-header">
          <h3 class="section-title">
            <el-icon><List /></el-icon> 预约管理
          </h3>
          <el-tabs v-model="activeTab" @tab-change="handleTabChange" class="custom-tabs">
            <el-tab-pane label="待确认" name="PENDING"></el-tab-pane>
            <el-tab-pane label="已确认" name="CONFIRMED"></el-tab-pane>
            <el-tab-pane label="已完成" name="COMPLETED"></el-tab-pane>
          </el-tabs>
        </div>

        <div class="table-container">
          <el-table
            :data="appointmentList"
            v-loading="loading"
            style="width: 100%"
            empty-text="暂无该状态的预约"
            :header-cell-style="{ background: 'transparent', color: '#6b7280' }"
            :cell-style="{ background: 'transparent' }"
          >
            <el-table-column prop="startTime" label="预约时间" width="180">
              <template #default="scope">
                <span class="time-text">{{ formatDateTime(scope.row.startTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="咨询者" width="160">
              <template #default="scope">
                <div class="user-info">
                  <el-avatar :size="32" :src="scope.row.targetAvatar || ''" class="user-avatar">
                    {{ scope.row.targetName?.charAt(0) }}
                  </el-avatar>
                  <span class="user-name">{{ scope.row.targetName || '匿名用户' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="scope">
                <el-tag :type="getStatusType(scope.row.status)" effect="light" round>
                  {{ getStatusText(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作">
              <template #default="scope">
                <el-button link type="primary" size="small" @click="viewDetail(scope.row)">查看详情</el-button>
                <el-popconfirm
                  v-if="scope.row.status === 'PENDING'"
                  title="确定要接受这个预约吗？"
                  @confirm="handleConfirm(scope.row.id)"
                >
                  <template #reference>
                    <el-button link type="success" size="small">确认接单</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="pagination-wrapper">
          <el-pagination
            layout="prev, pager, next"
            :total="total"
            :page-size="pageSize"
            @current-change="handlePageChange"
            background
          />
        </div>
      </div>

      <div class="schedule-section glass-card">
        <div class="section-header">
          <h3 class="section-title">
            <el-icon><Clock /></el-icon> 排班设置
          </h3>
        </div>
        <p class="schedule-desc">设置您的工作时间，用户将在这些时段看到您的空闲状态。</p>

        <el-form :model="scheduleForm" label-position="top" class="schedule-form">
          <el-form-item label="工作日选择">
            <el-checkbox-group v-model="scheduleForm.workDays" class="days-group">
              <el-checkbox-button v-for="day in 7" :key="day" :label="day">
                {{ ['周一','周二','周三','周四','周五','周六','周日'][day-1] }}
              </el-checkbox-button>
            </el-checkbox-group>
          </el-form-item>

          <el-form-item label="每日工作时段">
            <div v-for="(range, index) in scheduleForm.workHours" :key="index" class="time-range-row">
              <el-time-select
                v-model="range.start"
                start="08:00"
                step="01:00"
                end="22:00"
                placeholder="开始"
                class="time-select"
                :clearable="false"
              />
              <span class="separator">至</span>
              <el-time-select
                v-model="range.end"
                start="09:00"
                step="01:00"
                end="23:00"
                :min-time="range.start"
                placeholder="结束"
                class="time-select"
                :clearable="false"
              />
            </div>
          </el-form-item>

          <el-button type="primary" class="save-btn" @click="saveSchedule" :loading="scheduleLoading">
            保存排班设置
          </el-button>
        </el-form>
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="预约详情" width="520px" class="custom-dialog" align-center>
      <div v-if="currentAppointment" class="detail-content">
        <div class="detail-item">
          <span class="label">咨询者</span>
          <span class="value">{{ currentAppointment.targetName || '未知用户' }}</span>
        </div>
        <div class="detail-item">
          <span class="label">预约时间</span>
          <span class="value">{{ formatDateTime(currentAppointment.startTime) }}</span>
        </div>
        <div class="detail-item">
          <span class="label">当前状态</span>
          <el-tag :type="getStatusType(currentAppointment.status)" size="small">
            {{ getStatusText(currentAppointment.status) }}
          </el-tag>
        </div>
        <div class="detail-note">
          <span class="label">用户备注</span>
          <div class="note-box">
            {{ currentAppointment.userNote || '无备注' }}
          </div>
        </div>
        <div class="detail-actions">
          <el-button
            type="primary"
            plain
            size="small"
            :disabled="!currentAppointment.userId"
            @click="handleExportUserReport"
          >
            导出近期情绪报告
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getMyAppointments, confirmAppointment, setSchedule, getAppointmentDetail } from '@/api/counselorAppointment'
import { exportUserEmotionReport } from '@/api/report'
import { ElMessage } from 'element-plus'
import { Calendar, Bell, Check, Star, List, Clock } from '@element-plus/icons-vue'

const userStore = useUserStore()
const loading = ref(false)
const scheduleLoading = ref(false)

// 统计数据
const stats = reactive({
  todayAppointments: 0,
  pendingCount: 0,
  monthlyTotal: 0,
  currentRating: 5.0
})

const activeTab = ref('PENDING')
const appointmentList = ref<any[]>([])
const total = ref(0)
const pageSize = 10
const currentPage = ref(1)

const detailVisible = ref(false)
const currentAppointment = ref<any>(null)

const scheduleForm = reactive({
  workDays: [] as number[],
  workHours: [{ start: '09:00', end: '18:00' }]
})

onMounted(async () => {
  await loadStats()
  await loadAppointments()
})

const loadStats = async () => {
  // 模拟数据或调用接口
}

const loadAppointments = async () => {
  loading.value = true
  try {
    const res = await getMyAppointments({
      status: activeTab.value,
      page: currentPage.value,
      pageSize: pageSize
    })
    const data = res.data as any
    appointmentList.value = data.list
    total.value = data.total
    if (activeTab.value === 'PENDING') {
      stats.pendingCount = data.total
    }
  } catch (error) {
    console.error('加载预约失败', error)
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  currentPage.value = 1
  loadAppointments()
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  loadAppointments()
}

const handleConfirm = async (id: number) => {
  try {
    await confirmAppointment(id)
    ElMessage.success('预约已确认')
    loadAppointments()
    if (stats.pendingCount > 0) stats.pendingCount--
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const viewDetail = async (row: any) => {
  try {
    // 调用详情接口获取完整信息（包括用户备注），并保留列表中的基础信息
    const res = await getAppointmentDetail(row.id)
    const detail = (res as any).data || {}
    currentAppointment.value = { ...row, ...detail }
    detailVisible.value = true
  } catch (error) {
    console.error('加载预约详情失败', error)
    ElMessage.error('加载详情失败')
  }
}

const saveSchedule = async () => {
  if (scheduleForm.workDays.length === 0) {
    ElMessage.warning('请至少选择一个工作日')
    return
  }
  scheduleLoading.value = true
  try {
    await setSchedule({
      workDays: scheduleForm.workDays,
      workHours: scheduleForm.workHours
    })
    ElMessage.success('排班设置成功！')
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    scheduleLoading.value = false
  }
}

const handleExportUserReport = async () => {
  if (!currentAppointment.value || !currentAppointment.value.userId) {
    ElMessage.warning('暂时无法获取该用户ID，无法导出情绪报告')
    return
  }

  try {
    const res = await exportUserEmotionReport(currentAppointment.value.userId, 'pdf')
    if (res && res.data) {
      const blob = res.data as Blob
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'user_emotion_report.pdf'
      a.click()
      window.URL.revokeObjectURL(url)
      ElMessage.success('情绪报告导出成功')
    }
  } catch (error: any) {
    console.error('导出情绪报告失败', error)
    ElMessage.error(error?.message || '情绪报告导出失败')
  }
}

const formatDateTime = (str: string) => {
  if(!str) return ''
  return new Date(str).toLocaleString('zh-CN', {month: '2-digit', day: '2-digit', hour:'2-digit', minute:'2-digit'})
}

const getStatusType = (status: string) => {
  const map: any = { PENDING: 'warning', CONFIRMED: 'primary', COMPLETED: 'success', CANCELLED: 'info' }
  return map[status] || 'info'
}

const getStatusText = (status: string) => {
  const map: any = { PENDING: '待确认', CONFIRMED: '已确认', COMPLETED: '已完成', CANCELLED: '已取消' }
  return map[status] || status
}
</script>

<style scoped>
/* 页面基础 */
.counselor-dashboard {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

.dashboard-header {
  margin-bottom: 32px;
}

.page-title {
  font-size: 28px;
  font-weight: bold;
  color: #2c3e50;
  margin-bottom: 8px;
}

.page-subtitle {
  font-size: 14px;
  color: #6b7280;
}

/* 玻璃拟态卡片通用样式 */
.glass-card {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(24px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.glass-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.08);
}

/* 统计卡片网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  margin-bottom: 32px;
}

.stat-card {
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 20px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.stat-icon.blue { background: #e0f2fe; color: #0284c7; }
.stat-icon.orange { background: #ffedd5; color: #ea580c; }
.stat-icon.green { background: #dcfce7; color: #16a34a; }
.stat-icon.purple { background: #f3e8ff; color: #9333ea; }

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #2c3e50;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #6b7280;
}

/* 主内容布局 */
.main-content-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

.appointments-section,
.schedule-section {
  padding: 32px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  border-bottom: 1px solid rgba(0,0,0,0.05);
  padding-bottom: 16px;
}

.section-title {
  font-size: 18px;
  font-weight: bold;
  color: #2c3e50;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 表格样式优化 */
.table-container :deep(.el-table) {
  background-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-row-hover-bg-color: rgba(123, 158, 137, 0.1);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-name {
  font-weight: 500;
  color: #374151;
}

.time-text {
  font-family: monospace;
  color: #4b5563;
  font-weight: 500;
}

.pagination-wrapper {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}

/* 排班设置样式 */
.schedule-desc {
  color: #6b7280;
  font-size: 14px;
  margin-bottom: 24px;
}

.days-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.days-group :deep(.el-checkbox-button__inner) {
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  padding: 8px 12px;
  box-shadow: none;
}

.days-group :deep(.el-checkbox-button.is-checked .el-checkbox-button__inner) {
  background-color: #7b9e89;
  border-color: #7b9e89;
  box-shadow: none;
}

.time-range-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.separator {
  color: #9ca3af;
}

.time-select {
  flex: 1;
}

.save-btn {
  width: 100%;
  margin-top: 24px;
  background-color: #7b9e89;
  border-color: #7b9e89;
  height: 44px;
  font-size: 16px;
  border-radius: 12px;
}

.save-btn:hover {
  background-color: #5f7a6a;
  border-color: #5f7a6a;
}

/* 详情弹窗美化 */
.detail-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px dashed #e5e7eb;
}

.detail-item .label {
  color: #6b7280;
}

.detail-item .value {
  font-weight: 500;
  color: #111827;
}

.note-box {
  margin-top: 8px;
  background: #f9fafb;
  padding: 12px;
  border-radius: 8px;
  color: #4b5563;
  font-size: 14px;
  line-height: 1.5;
}

/* 响应式 */
@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .main-content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>