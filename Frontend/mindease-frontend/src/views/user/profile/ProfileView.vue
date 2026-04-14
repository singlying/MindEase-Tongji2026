<template>
  <div class="profile-page">
    <!-- 顶部个人资料卡片 -->
    <div class="profile-card glass-card">
      <!-- 装饰背景 -->
      <div class="decoration-bg"></div>

      <!-- 头像区域 -->
      <div class="avatar-section">
        <div class="avatar-wrapper">
          <img
            :src="userStore.userInfo?.avatar || defaultAvatar"
            :alt="userStore.userInfo?.nickname"
            class="avatar-img"
          />
        </div>
        <button
          class="avatar-edit-btn"
          @click="showEditDialog = true"
          title="更换头像"
        >
          <i class="fas fa-camera"></i>
        </button>
      </div>

      <!-- 信息区域 -->
      <div class="info-section">
        <div class="user-header">
          <h2 class="user-name">{{ userStore.userInfo?.nickname }}</h2>
          <el-tag type="success" size="large">
            <i class="fas fa-check-circle"></i>
            状态正常
          </el-tag>
          <el-tag type="info" size="large">
            <i class="fas fa-user"></i>
            {{ roleText }}
          </el-tag>
        </div>

        <p class="register-time">
          <i class="far fa-clock"></i>
          注册时间：{{ userStore.userInfo?.createTime || "未知" }}
        </p>

        <div class="action-buttons">
          <el-button type="primary" @click="showEditDialog = true">
            <i class="fas fa-pen"></i>
            编辑资料
          </el-button>
          <!-- 账号设置按钮暂无实际功能，先注释避免误导 -->
          <!--
        <el-button>
          <i class="fas fa-cog"></i>
          账号设置
        </el-button>
        --></div>
      </div>

      <!-- 关键指标（仅普通用户显示） -->
      <div class="stats-section" v-if="dashboardData && isNormalUser">
        <div class="stat-item">
          <div class="stat-label">心情均分</div>
          <div class="stat-value stat-accent">
            {{ dashboardData.moodSummary?.avgScore || 0 }}
          </div>
        </div>
        <div class="stat-item">
          <div class="stat-label">连续记录</div>
          <div class="stat-value stat-warm">
            {{ dashboardData.moodSummary?.continuousDays || 0 }}
            <span class="stat-unit">天</span>
          </div>
        </div>
        <div class="stat-item">
          <div class="stat-label">未读通知</div>
          <div class="stat-value stat-info">
            {{ dashboardData.unreadNotifications || 0 }}
          </div>
        </div>
      </div>
    </div>

    <!-- 下部内容：两列布局 -->
    <div :class="['content-grid', { 'single-column': !isNormalUser }]">
      <!-- 左侧：近期预约（仅普通用户显示） -->
      <div class="appointments-panel glass-panel" v-if="isNormalUser">
        <div class="panel-header">
          <h3 class="panel-title">
            <i class="far fa-calendar-alt"></i>
            近期预约
          </h3>
          <router-link to="/my-appointments" class="panel-link"
            >查看全部</router-link
          >
        </div>

        <div class="appointments-list">
          <div
            v-if="dashboardData?.upcomingAppointments.length"
            v-for="appointment in dashboardData.upcomingAppointments"
            :key="appointment.id"
            class="appointment-item"
          >
            <div class="appointment-header">
              <div class="counselor-info">
                <div class="counselor-avatar">
                  <img
                    :src="`https://ui-avatars.com/api/?name=${appointment.counselor}&background=random`"
                    :alt="appointment.counselor"
                  />
                </div>
                <span class="counselor-name">{{ appointment.counselor }}</span>
              </div>
              <el-tag type="primary" size="small">明日</el-tag>
            </div>
            <div class="appointment-time">
              <i class="far fa-clock"></i>
              {{ appointment.time }}
            </div>
            <!-- TODO: 进入候诊室功能暂未实现，按钮先下线避免误导
            <el-button class="appointment-btn" size="large">
              进入候诊室
            </el-button>
            -->
          </div>

          <el-empty v-else description="暂无预约" :image-size="80" />
        </div>
      </div>

      <!-- 右侧：通知中心 -->
      <div class="notifications-panel glass-panel">
        <div class="panel-header">
          <h3 class="panel-title">
            <i class="far fa-bell"></i>
            通知中心
          </h3>
          <el-button text @click="handleMarkAllRead" :loading="markAllLoading">
            <i class="fas fa-check-double"></i>
            全部已读
          </el-button>
        </div>

        <div class="notifications-list" v-loading="notificationsLoading">
          <div
            v-for="notification in notifications"
            :key="notification.id"
            :class="[
              'notification-item',
              { 'notification-unread': !notification.isRead },
            ]"
          >
            <div :class="['notification-icon', `icon-${notification.type}`]">
              <i :class="getNotificationIcon(notification.type)"></i>
            </div>
            <div class="notification-content">
              <div class="notification-header">
                <h4 class="notification-title">{{ notification.title }}</h4>
                <span class="notification-time">{{
                  formatTime(notification.createTime)
                }}</span>
              </div>
              <p class="notification-text">{{ notification.content }}</p>
            </div>
            <el-button
              v-if="!notification.isRead"
              text
              circle
              @click="handleMarkRead(notification.id)"
              title="标记为已读"
            >
              <i class="far fa-circle"></i>
            </el-button>
            <i v-else class="fas fa-check-circle notification-read-icon"></i>
          </div>

          <el-empty
            v-if="!notifications.length"
            description="暂无通知"
            :image-size="100"
          />
        </div>
      </div>
    </div>

    <!-- 编辑资料对话框 -->
    <el-dialog
      v-model="showEditDialog"
      title="编辑个人资料"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="editRules"
        label-width="80px"
      >
        <el-form-item label="昵称" prop="nickname">
          <el-input
            v-model="editForm.nickname"
            placeholder="请输入昵称"
            maxlength="20"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="头像">
          <!-- 头像预览和上传区域 -->
          <div class="avatar-upload-section">
            <div class="avatar-preview">
              <img
                :src="editForm.avatar || defaultAvatar"
                :alt="editForm.nickname"
                class="preview-img"
              />
            </div>
            <div class="upload-actions">
              <el-upload
                class="avatar-uploader"
                action=""
                :auto-upload="true"
                :show-file-list="false"
                accept="image/jpeg,image/png,image/jpg"
                :before-upload="handleBeforeUpload"
                :http-request="handleAvatarUpload"
                :on-change="handleAvatarChange"
              >
                <el-button type="primary">
                  <i class="fas fa-upload"></i>
                  选择图片
                </el-button>
              </el-upload>
              <p class="upload-tip">支持 JPG、PNG 格式，建议尺寸 256x256</p>
            </div>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button
          type="primary"
          @click="handleSaveProfile"
          :loading="saveLoading"
        >
          保存修改
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from "vue";
import { useUserStore } from "@/stores/user";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import request from "@/api/request";
import {
  getDashboard,
  getNotifications,
  markNotificationRead,
  markAllNotificationsRead,
  type DashboardData,
  type Notification,
} from "@/api/user";
import { updateUserProfile } from "@/api/auth";

const userStore = useUserStore();

// 默认头像
const defaultAvatar = computed(() => {
  const name = userStore.userInfo?.nickname || "User";
  return `https://ui-avatars.com/api/?name=${encodeURIComponent(
    name
  )}&background=E8E1D9&color=5F7A6A&size=256`;
});

// 角色文本
// 判断是否为普通用户
const isNormalUser = computed(() => {
  const role = userStore.userInfo?.role?.toUpperCase();
  return role === "USER" || !role;
});

const roleText = computed(() => {
  const roleMap = {
    user: "会员用户",
    counselor: "心理咨询师",
    admin: "管理员",
  };
  return roleMap[userStore.userInfo?.role || "user"];
});

// Dashboard 数据
const dashboardData = ref<DashboardData | null>(null);

// 通知列表
const notifications = ref<Notification[]>([]);
const notificationsLoading = ref(false);

// 编辑对话框
const showEditDialog = ref(false);
const editFormRef = ref<FormInstance>();
const editForm = ref({
  nickname: "",
  avatar: "",
});
// 头像预览（避免把 base64 长串直接写入 avatar 字段）
const avatarPreview = ref("");
const saveLoading = ref(false);
const markAllLoading = ref(false);

// 表单验证规则
const editRules: FormRules = {
  nickname: [
    { required: true, message: "请输入昵称", trigger: "blur" },
    { min: 2, max: 20, message: "昵称长度为 2-20 个字符", trigger: "blur" },
  ],
};

// 获取Dashboard数据
const fetchDashboard = async () => {
  try {
    const response = await getDashboard();
    dashboardData.value = response.data;
  } catch (error) {
    console.error("获取Dashboard数据失败:", error);
  }
};

// 获取通知列表
const fetchNotifications = async () => {
  notificationsLoading.value = true;
  try {
    const response = await getNotifications(20);
    notifications.value = response.data.notifications;
  } catch (error) {
    console.error("获取通知列表失败:", error);
    ElMessage.error("获取通知列表失败");
  } finally {
    notificationsLoading.value = false;
  }
};

// 标记通知已读
const handleMarkRead = async (id: number) => {
  try {
    await markNotificationRead(id);
    // 更新本地状态
    const notification = notifications.value.find((n) => n.id === id);
    if (notification) {
      notification.isRead = true;
    }
    // 更新未读数量
    if (dashboardData.value) {
      dashboardData.value.unreadNotifications = Math.max(
        0,
        dashboardData.value.unreadNotifications - 1
      );
    }
    ElMessage.success("已标记为已读");
  } catch (error) {
    console.error("标记已读失败:", error);
    ElMessage.error("操作失败");
  }
};

// 标记全部已读
const handleMarkAllRead = async () => {
  markAllLoading.value = true;
  try {
    await markAllNotificationsRead();
    // 更新本地状态
    notifications.value.forEach((n) => {
      n.isRead = true;
    });
    if (dashboardData.value) {
      dashboardData.value.unreadNotifications = 0;
    }
    ElMessage.success("全部通知已标记为已读");
  } catch (error) {
    console.error("标记全部已读失败:", error);
    ElMessage.error("操作失败");
  } finally {
    markAllLoading.value = false;
  }
};

/**
 * ========== 头像上传功能 ==========
 *
 * 后端已提供 POST /upload（multipart/form-data）接口，返回 data=文件URL
 */

// 上传前校验：类型/大小
const handleBeforeUpload = (file: File) => {
  const isImage =
    file.type === "image/jpeg" ||
    file.type === "image/png" ||
    file.type === "image/jpg";
  if (!isImage) {
    ElMessage.error("仅支持 JPG/PNG 图片");
    return false;
  }
  const isLt10M = file.size / 1024 / 1024 < 10;
  if (!isLt10M) {
    ElMessage.error("图片大小不能超过 10MB");
    return false;
  }
  return true;
};

// 处理头像文件选择
const handleAvatarChange = (file: any) => {
  // 生成本地预览URL
  const reader = new FileReader();
  reader.onload = (e) => {
    avatarPreview.value = e.target?.result as string;
  };
  reader.readAsDataURL(file.raw);
};

// 自定义上传逻辑：调用后端 /upload 返回 URL
const handleAvatarUpload = async (options: any) => {
  const formData = new FormData();
  formData.append("file", options.file);
  try {
    const res = await request.post("/upload", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    // 兼容后端 Result 结构 {code,message,data:url}
    const url = (res as any)?.data?.data ?? (res as any)?.data;
    if (url) {
      editForm.value.avatar = url;
      avatarPreview.value = url;
      ElMessage.success("头像上传成功");
      options.onSuccess?.(res, options.file);
    } else {
      throw new Error("未返回文件地址");
    }
  } catch (error: any) {
    console.error("图片上传失败:", error);
    ElMessage.error(error?.message || "图片上传失败，请重试");
    // 恢复为原头像
    editForm.value.avatar = userStore.userInfo?.avatar || "";
    options.onError?.(error);
  }
};

// 保存个人资料
const handleSaveProfile = async () => {
  if (!editFormRef.value) return;

  await editFormRef.value.validate(async (valid) => {
    if (!valid) return;

    saveLoading.value = true;
    try {
      await updateUserProfile({
        nickname: editForm.value.nickname,
        avatar: editForm.value.avatar,
      });

      // 更新本地用户信息
      if (userStore.userInfo) {
        userStore.userInfo.nickname = editForm.value.nickname;
        userStore.userInfo.avatar = editForm.value.avatar;
      }

      ElMessage.success("保存成功");
      showEditDialog.value = false;
    } catch (error) {
      console.error("保存失败:", error);
      ElMessage.error("保存失败");
    } finally {
      saveLoading.value = false;
    }
  });
};

// 获取通知图标
const getNotificationIcon = (type: string) => {
  const iconMap: Record<string, string> = {
    appointment: "fas fa-exclamation",
    system: "fas fa-info",
    report: "fas fa-check",
  };
  return iconMap[type] || "fas fa-bell";
};

// 格式化时间
const formatTime = (timeStr: string) => {
  const now = new Date();
  const time = new Date(timeStr);
  const diff = now.getTime() - time.getTime();
  const dayDiff = Math.floor(diff / (1000 * 60 * 60 * 24));

  if (dayDiff === 0) {
    const hours = time.getHours().toString().padStart(2, "0");
    const minutes = time.getMinutes().toString().padStart(2, "0");
    return `${hours}:${minutes}`;
  } else if (dayDiff === 1) {
    return "昨天";
  } else if (dayDiff < 7) {
    return `${dayDiff}天前`;
  } else {
    return timeStr.split(" ")[0];
  }
};

// 页面加载时获取数据
onMounted(() => {
  fetchDashboard();
  fetchNotifications();

  // 初始化编辑表单
  if (userStore.userInfo) {
    editForm.value.nickname = userStore.userInfo.nickname;
    editForm.value.avatar = userStore.userInfo.avatar;
    avatarPreview.value = userStore.userInfo.avatar;
  }
});
</script>

<style scoped>
.profile-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: var(--spacing-lg);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

/* 顶部个人资料卡片 */
.profile-card {
  border-radius: var(--radius-xl);
  padding: var(--spacing-xl);
  display: flex;
  align-items: center;
  gap: var(--spacing-xl);
  position: relative;
  overflow: hidden;
  flex-wrap: wrap;
}

.decoration-bg {
  position: absolute;
  top: 0;
  right: 0;
  width: 16rem;
  height: 16rem;
  background: linear-gradient(
    to bottom left,
    rgba(123, 158, 137, 0.1),
    transparent
  );
  border-bottom-left-radius: 100%;
  pointer-events: none;
}

/* 头像区域 */
.avatar-section {
  position: relative;
  z-index: 1;
}

.avatar-wrapper {
  width: 8rem;
  height: 8rem;
  border-radius: var(--radius-full);
  padding: var(--spacing-xs);
  border: 2px solid white;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  background: rgba(255, 255, 255, 0.5);
}

.avatar-img {
  width: 100%;
  height: 100%;
  border-radius: var(--radius-full);
  object-fit: cover;
}

.avatar-edit-btn {
  position: absolute;
  bottom: 0;
  right: 0;
  background: white;
  padding: var(--spacing-sm);
  border-radius: var(--radius-full);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  border: none;
  color: var(--ease-accent-dark);
  cursor: pointer;
  transition: all 0.3s;
}

.avatar-edit-btn:hover {
  background: var(--gray-50);
  transform: scale(1.1);
}

/* 信息区域 */
.info-section {
  flex: 1;
  min-width: 300px;
  z-index: 1;
}

.user-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-sm);
  flex-wrap: wrap;
}

.user-name {
  font-size: var(--font-3xl);
  font-family: serif;
  font-weight: bold;
  color: var(--ease-dark);
  margin: 0;
}

.register-time {
  color: var(--gray-500);
  margin: var(--spacing-sm) 0 var(--spacing-md) 0;
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.action-buttons {
  display: flex;
  gap: var(--spacing-md);
  flex-wrap: wrap;
}

/* 关键指标 */
.stats-section {
  display: flex;
  gap: var(--spacing-lg);
  z-index: 1;
  background: rgba(255, 255, 255, 0.4);
  padding: var(--spacing-md);
  border-radius: var(--radius-lg);
  backdrop-filter: blur(10px);
}

.stat-item {
  text-align: center;
  padding: 0 var(--spacing-md);
  border-right: 1px solid rgba(0, 0, 0, 0.1);
}

.stat-item:last-child {
  border-right: none;
}

.stat-label {
  font-size: var(--font-xs);
  color: var(--gray-500);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: var(--spacing-xs);
}

.stat-value {
  font-size: var(--font-2xl);
  font-weight: bold;
}

.stat-accent {
  color: var(--ease-accent-dark);
}

.stat-warm {
  color: var(--ease-warm);
}

.stat-info {
  color: #2563eb;
}

.stat-unit {
  font-size: var(--font-xs);
  font-weight: normal;
  color: var(--gray-500);
}

/* 内容网格 */
.content-grid {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: var(--spacing-lg);
}

/* 单列布局（咨询师/管理员） */
.content-grid.single-column {
  grid-template-columns: 1fr;
}

.content-grid.single-column .notifications-panel {
  max-width: 800px;
  margin: 0 auto;
}

@media (max-width: 1024px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}

/* 面板扩展样式（glass-panel基础样式在main.css中） */
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
}

.panel-title {
  font-size: var(--font-xl);
  font-weight: bold;
  color: var(--ease-dark);
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin: 0;
}

.panel-title i {
  color: var(--ease-accent);
}

.panel-link {
  font-size: var(--font-sm);
  color: var(--ease-accent-dark);
  text-decoration: none;
}

.panel-link:hover {
  text-decoration: underline;
}

/* 预约列表 */
.appointments-list {
  flex: 1;
  overflow-y: auto;
  padding-right: var(--spacing-xs);
}

.appointment-item {
  background: rgba(255, 255, 255, 0.6);
  padding: var(--spacing-md);
  border-radius: var(--radius-lg);
  border: 1px solid white;
  margin-bottom: var(--spacing-md);
  transition: all 0.3s;
}

.appointment-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.appointment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-sm);
}

.counselor-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.counselor-avatar {
  width: 2rem;
  height: 2rem;
  border-radius: var(--radius-full);
  overflow: hidden;
}

.counselor-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.counselor-name {
  font-weight: bold;
  color: var(--gray-800);
}

.appointment-time {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  color: var(--gray-600);
  font-size: var(--font-sm);
  margin-bottom: var(--spacing-md);
}

.appointment-btn {
  width: 100%;
  background: var(--ease-bg);
  color: var(--ease-dark);
  border: none;
  transition: all 0.3s;
}

.appointment-btn:hover {
  background: var(--ease-accent);
  color: white;
}

/* 通知列表 */
.notifications-list {
  flex: 1;
  overflow-y: auto;
  padding-right: var(--spacing-xs);
}

.notification-item {
  background: rgba(255, 255, 255, 0.6);
  padding: var(--spacing-md);
  border-radius: var(--radius-lg);
  border: 1px solid white;
  display: flex;
  gap: var(--spacing-md);
  align-items: flex-start;
  margin-bottom: var(--spacing-md);
  transition: all 0.3s;
}

.notification-item:hover {
  background: white;
}

.notification-unread {
  background: rgba(255, 255, 255, 0.8);
  border-left: 4px solid var(--ease-warm);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.notification-icon {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: var(--spacing-xs);
}

.icon-appointment {
  background: rgba(196, 124, 107, 0.2);
  color: var(--ease-warm);
}

.icon-system {
  background: rgba(37, 99, 235, 0.2);
  color: #2563eb;
}

.icon-report {
  background: rgba(34, 197, 94, 0.2);
  color: #22c55e;
}

.notification-content {
  flex: 1;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--spacing-xs);
}

.notification-title {
  font-weight: bold;
  color: var(--gray-800);
  margin: 0;
  font-size: var(--font-base);
}

.notification-time {
  font-size: var(--font-xs);
  color: var(--gray-400);
  flex-shrink: 0;
  margin-left: var(--spacing-sm);
}

.notification-text {
  color: var(--gray-600);
  font-size: var(--font-sm);
  margin: 0;
  line-height: 1.5;
}

.notification-read-icon {
  color: var(--ease-accent);
  margin-top: var(--spacing-sm);
}

/* 头像上传组件样式 */
.avatar-upload-section {
  display: flex;
  align-items: center;
  gap: var(--spacing-xl);
}

.avatar-preview {
  width: 100px;
  height: 100px;
  border-radius: var(--radius-full);
  overflow: hidden;
  border: 2px solid var(--gray-200);
  flex-shrink: 0;
}

.avatar-preview .preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-actions {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.upload-tip {
  font-size: var(--font-sm);
  color: var(--gray-400);
  margin: 0;
}

.avatar-uploader :deep(.el-upload) {
  cursor: pointer;
}

/* 响应式 */
@media (max-width: 768px) {
  .profile-page {
    padding: var(--spacing-md);
  }

  .profile-card {
    flex-direction: column;
    text-align: center;
  }

  .user-header {
    justify-content: center;
  }

  .action-buttons {
    justify-content: center;
  }

  .stats-section {
    flex-direction: column;
    gap: var(--spacing-md);
  }

  .stat-item {
    border-right: none;
    border-bottom: 1px solid rgba(0, 0, 0, 0.1);
    padding: var(--spacing-md) 0;
  }

  .stat-item:last-child {
    border-bottom: none;
  }
}
</style>
