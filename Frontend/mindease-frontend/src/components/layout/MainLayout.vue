<!--
  组件名称: MainLayout - 主布局容器
  
  功能说明:
  - 提供应用主框架布局（侧边栏 + 内容区）
  - 包含导航菜单、用户信息、路由出口
  - 响应式设计，支持移动端适配
  - 统一管理背景装饰和玻璃态效果
  
  使用位置:
  在路由配置中作为父组件使用，无需手动引入
  
  子路由在 <router-view /> 中渲染
  
  功能模块:
  - 侧边栏导航：首页、情绪日记、AI咨询等
  - 用户信息：头像、昵称、退出登录
  - 背景装饰：渐变光球、噪点纹理
-->
<template>
  <div class="main-layout">
    <!-- 背景装饰层 -->
    <div class="noise-overlay"></div>
    <div class="fixed inset-0 overflow-hidden pointer-events-none">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="orb orb-3"></div>
    </div>

    <!-- 主布局容器 -->
    <div class="layout-container">
      <!-- 侧边栏 - 使用HTML原型风格 -->
      <aside class="sidebar glass-panel">
        <!-- Logo -->
        <div class="logo-section">
          <div class="logo-icon">
            <i class="fas fa-brain"></i>
          </div>
          <span class="logo-text">MindEase</span>
        </div>

        <!-- 导航按钮 -->
        <nav class="nav-section">
          <button
            v-for="item in menuItems"
            :key="item.path"
            :class="['nav-item', { active: activeMenu === item.path }]"
            @click="router.push(item.path)"
          >
            <i :class="['fas', item.icon]"></i>
            <span class="nav-text">{{ item.title }}</span>
          </button>
        </nav>

        <!-- 用户信息 -->
        <div class="user-section">
          <el-dropdown @command="handleCommand">
            <div class="user-card">
              <el-avatar
                :src="userStore.userInfo?.avatar || defaultAvatar"
                :size="40"
              />
              <div class="user-info">
                <span class="user-name">{{
                  userStore.userInfo?.nickname
                }}</span>
                <span class="user-role">{{ userRoleText }}</span>
              </div>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <!-- 待审核咨询师不显示个人中心 -->
                <el-dropdown-item
                  v-if="
                    !(
                      userStore.userInfo?.role?.toUpperCase() === 'COUNSELOR' &&
                      userStore.userInfo?.status === 2
                    )
                  "
                  command="profile"
                >
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item
                  command="logout"
                  :divided="
                    !(
                      userStore.userInfo?.role?.toUpperCase() === 'COUNSELOR' &&
                      userStore.userInfo?.status === 2
                    )
                  "
                >
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </aside>

      <!-- 主内容区 -->
      <main class="main-content">
        <!-- 顶部问候栏 -->
        <div class="greeting-bar">
          <div>
            <h1 class="greeting-title">
              {{ greetingText }},
              <span class="greeting-name">{{
                userStore.userInfo?.nickname
              }}</span>
            </h1>
            <p class="greeting-subtitle">接纳情绪，是疗愈的开始。</p>
          </div>
          <div class="header-actions">
            <div class="status-badge glass-panel">
              <div class="status-dot"></div>
              系统运行中
            </div>
            <button class="notification-btn glass-panel" @click="goToNotifications">
              <i class="fas fa-bell"></i>
              <span class="notification-badge"></span>
            </button>
          </div>
        </div>

        <!-- 页面内容 -->
        <div class="page-content">
          <div class="content-container">
            <router-view />
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";
import { ElMessage } from "element-plus";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

// 默认头像（使用第三方服务生成带昵称首字母的头像）
const defaultAvatar = computed(() => {
  const name = userStore.userInfo?.nickname || "User";
  return `https://ui-avatars.com/api/?name=${encodeURIComponent(
    name
  )}&background=E8E1D9&color=5F7A6A&size=128`;
});

// 组件挂载时获取用户信息（防止刷新后userInfo丢失）
onMounted(async () => {
  // 如果有token但没有用户信息，则重新获取
  if (userStore.token && !userStore.userInfo) {
    try {
      await userStore.fetchUserInfo();
    } catch (error) {
      // 如果获取失败（token无效），会被响应拦截器处理并跳转登录页
      console.error("获取用户信息失败:", error);
    }
  }
});

// 菜单项配置 - 根据用户角色动态显示
const menuItems = computed(() => {
  const userRole = userStore.userInfo?.role?.toUpperCase();
  const userStatus = userStore.userInfo?.status;

  // 咨询师菜单
  if (userRole === "COUNSELOR") {
    // 待审核咨询师：只显示审核状态（不显示其他菜单）
    if (userStatus === 2) {
      return [
        {
          path: "/counselor/audit-pending",
          icon: "fa-clock",
          title: "资质审核",
        },
      ];
    }
    // 已审核咨询师：显示工作台
    return [
      { path: "/counselor/dashboard", icon: "fa-home", title: "工作台" },
      { path: "/profile", icon: "fa-user-cog", title: "个人设置" },
    ];
  }

  // 管理员菜单
  if (userRole === "ADMIN") {
    return [
      { path: "/admin/dashboard", icon: "fa-home", title: "管理员工作台" },
      { path: "/profile", icon: "fa-user-cog", title: "个人设置" },
    ];
  }

  // 普通用户菜单
  return [
    { path: "/home", icon: "fa-home", title: "用户首页" },
    { path: "/mood-diary", icon: "fa-book-open", title: "情绪日记" },
    { path: "/ai-chat", icon: "fa-comments", title: "AI 智能咨询" },
    { path: "/assessment", icon: "fa-clipboard-list", title: "心理健康测评" },
    { path: "/counselor-list", icon: "fa-user-doctor", title: "咨询师推荐" },
    { path: "/emotion-report", icon: "fa-chart-line", title: "情绪报告" },
  ];
});

// 当前激活的菜单
const activeMenu = computed(() => route.path);

// 用户角色文本
const userRoleText = computed(() => {
  const userRole = userStore.userInfo?.role?.toUpperCase();
  const roleMap: Record<string, string> = {
    USER: "会员用户",
    COUNSELOR: "咨询师",
    ADMIN: "管理员",
  };
  return roleMap[userRole || ""] || "普通用户";
});

// 动态问候语（根据时间显示）
const greetingText = computed(() => {
  const hour = new Date().getHours();

  if (hour >= 5 && hour < 12) {
    return "Good Morning";
  } else if (hour >= 12 && hour < 18) {
    return "Good Afternoon";
  } else {
    return "Good Evening";
  }
});

// 通知按钮：统一跳转到个人中心（包含通知中心面板）
const goToNotifications = () => {
  router.push("/profile");
};

// 用户操作处理
const handleCommand = (command: string) => {
  if (command === "profile") {
    router.push("/profile");
  } else if (command === "logout") {
    userStore.logout();
    ElMessage.success("退出登录成功");
    router.push("/login");
  }
};
</script>

<style scoped>
/* 主布局 */
.main-layout {
  position: relative;
  height: 100vh;
  overflow: hidden;
  background: var(--ease-bg);
}

/* 布局容器 */
.layout-container {
  position: relative;
  z-index: 10;
  display: flex;
  height: 100%;
  padding: 1.5rem;
  gap: 1.5rem;
  max-width: 1920px;
  margin: 0 auto;
}

/* 侧边栏 - 模仿HTML原型的hover展开效果 */
.sidebar {
  display: flex;
  flex-direction: column;
  width: 96px; /* 默认宽度 w-24 */
  padding: 2rem 0;
  border-radius: 2.5rem; /* rounded-[2.5rem] */
  transition: all 0.5s cubic-bezier(0.23, 1, 0.32, 1); /* 弹性动画 */
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(31, 38, 135, 0.1);
  align-items: center;
  z-index: 50;
}

.sidebar:hover {
  width: 256px; /* hover时展开 w-64 */
}

/* Logo区域 */
.logo-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  margin-bottom: 2.5rem;
  flex-shrink: 0;
}

.logo-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(to bottom right, var(--ease-accent), #10b981);
  border-radius: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 1.25rem;
  box-shadow: 0 10px 20px rgba(123, 158, 137, 0.3);
  margin-bottom: 0.5rem;
  transition: transform 0.3s;
}

.sidebar:hover .logo-icon {
  transform: scale(1.1); /* hover时放大 */
}

.logo-text {
  font-size: 1.125rem;
  font-family: serif;
  font-weight: bold;
  color: var(--ease-dark);
  opacity: 0;
  white-space: nowrap;
  transition: opacity 0.3s 75ms; /* 延迟75ms */
}

.sidebar:hover .logo-text {
  opacity: 1;
}

/* 导航区域 */
.nav-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  width: 100%;
  padding: 0 0.75rem;
  flex: 1;
}

/* 导航按钮 */
.nav-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.875rem;
  border-radius: 1rem;
  background: transparent;
  border: 1px solid transparent;
  color: var(--gray-500);
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.25, 1, 0.5, 1);
  font-weight: 500;
}

.nav-item i {
  font-size: 1.25rem;
  min-width: 1.5rem;
  text-align: center;
}

.nav-text {
  opacity: 0;
  white-space: nowrap;
  transition: opacity 0.3s 75ms;
}

.sidebar:hover .nav-text {
  opacity: 1;
}

.nav-item:hover:not(.active) {
  background: white;
  color: var(--ease-accent);
  box-shadow: 0 4px 12px rgba(123, 158, 137, 0.15);
}

.nav-item.active {
  background: rgba(123, 158, 137, 0.1);
  color: var(--ease-accent);
  border-color: rgba(123, 158, 137, 0.3);
  font-weight: 600;
}

/* 用户区域 */
.user-section {
  width: 100%;
  padding: 0 1rem;
  margin-top: auto;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem;
  border-radius: 0.75rem;
  cursor: pointer;
  transition: background 0.3s;
  justify-content: center;
}

.sidebar:hover .user-card {
  justify-content: flex-start;
}

.user-card:hover {
  background: rgba(255, 255, 255, 0.4);
}

.user-info {
  display: flex;
  flex-direction: column;
  opacity: 0;
  overflow: hidden;
  transition: opacity 0.3s 75ms;
}

.sidebar:hover .user-info {
  opacity: 1;
}

.user-name {
  font-size: 0.875rem;
  font-weight: bold;
  color: var(--ease-dark);
  white-space: nowrap;
}

.user-role {
  font-size: 0.75rem;
  color: var(--gray-500);
  white-space: nowrap;
}

/* 主内容区 */
.main-content {
  flex: 1;
  position: relative;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

/* 问候栏 */
.greeting-bar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: 40;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  padding: 0.5rem;
  margin-bottom: 1rem;
  pointer-events: none;
}

.greeting-bar > * {
  pointer-events: auto;
}

.greeting-title {
  font-family: serif;
  font-size: 2.25rem;
  color: var(--ease-dark);
}

.greeting-name {
  font-style: italic;
  color: var(--ease-accent-dark);
}

.greeting-subtitle {
  color: var(--gray-500);
  margin-top: 0.25rem;
  font-weight: 300;
}

.header-actions {
  display: flex;
  gap: 1rem;
}

.status-badge {
  padding: 0.5rem 1rem;
  border-radius: 9999px;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--gray-600);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.status-dot {
  width: 0.5rem;
  height: 0.5rem;
  background: #4ade80;
  border-radius: 50%;
  animation: pulse 2s ease-in-out infinite;
}

.notification-btn {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--gray-500);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: background 0.3s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  position: relative;
}

.notification-btn:hover {
  background: white;
}

.notification-badge {
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
  width: 0.5rem;
  height: 0.5rem;
  background: #f87171;
  border-radius: 50%;
}

/* 页面内容 */
.page-content {
  padding-top: 6rem; /* 为问候栏留出空间 */
  height: 100%;
}

.content-container {
  height: 100%;
  width: 100%;
  border-radius: 2rem;
  overflow: hidden;
  overflow-y: auto;
  background: var(--ease-bg);
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
</style>
