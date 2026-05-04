<template>
  <div class="app-shell">
    <div class="top-progress" :class="{ active: globalLoading }"></div>
    <div class="loading-toast" :class="{ active: globalLoading }">
      <span class="spinner"></span>
      正在同步数据
    </div>
    <aside class="sidebar">
      <div class="sidebar-brand">
        <div class="brand-mark">ME</div>
        <div>
          <strong>MindEase</strong>
          <span class="muted">心理健康支持平台</span>
        </div>
      </div>

      <nav class="nav">
        <RouterLink v-for="item in menuItems" :key="item.path" :to="item.path">
          {{ item.icon }} {{ item.title }}
        </RouterLink>
      </nav>

      <div class="sidebar-footer">
        <div class="panel">
          <div class="mini-profile">
            <img v-if="session.userInfo?.avatar && avatarOk" :src="session.userInfo.avatar" alt="头像" @error="avatarOk = false" />
            <span v-else>{{ initials }}</span>
            <div>
              <strong>{{ session.userInfo?.nickname || "MindEase 用户" }}</strong>
              <p class="muted">{{ roleText }}</p>
            </div>
          </div>
          <button class="btn secondary" style="width: 100%" @click="logout">退出登录</button>
        </div>
      </div>
    </aside>

    <main class="content">
      <header class="topbar">
        <div class="page-title">
          <h1>{{ title }}</h1>
          <p class="muted">记录情绪、完成测评、预约咨询，在一个宁静清晰的空间里照看自己。</p>
        </div>
        <button class="btn secondary" @click="$router.push('/profile')">个人中心</button>
      </header>
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useSessionStore } from "@/stores/session";

const route = useRoute();
const router = useRouter();
const session = useSessionStore();
const globalLoading = ref(false);
const avatarOk = ref(true);

const handleLoading = (event: Event) => {
  globalLoading.value = Boolean((event as CustomEvent).detail?.loading);
};

onMounted(async () => {
  window.addEventListener("mindease:loading", handleLoading);
  if (session.token && !session.userInfo) {
    await session.fetchUserInfo().catch(() => session.logout());
  }
});

onBeforeUnmount(() => window.removeEventListener("mindease:loading", handleLoading));

const title = computed(() => {
  const map: Record<string, string> = {
    "/home": "心理能量总览",
    "/mood-diary": "情绪星图日记",
    "/ai-chat": "AI 陪伴舱",
    "/assessment": "心理测评中心",
    "/counselor-list": "咨询师星系",
    "/my-appointments": "预约轨道",
    "/emotion-report": "情绪洞察报告",
    "/meditation": "冥想声场",
    "/profile": "个人中心",
    "/counselor/dashboard": "咨询师工作台",
    "/counselor/audit-pending": "资质审核",
    "/admin/dashboard": "管理员控制台"
  };
  return map[route.path] || "MindEase";
});

const roleText = computed(() => {
  const role = session.userInfo?.role?.toUpperCase();
  if (role === "ADMIN") return "管理员";
  if (role === "COUNSELOR") return session.userInfo?.status === 2 ? "待审核咨询师" : "咨询师";
  return "普通用户";
});

const initials = computed(() => (session.userInfo?.nickname || session.userInfo?.username || "ME").slice(0, 2).toUpperCase());

const menuItems = computed(() => {
  const role = session.userInfo?.role?.toUpperCase();
  const status = session.userInfo?.status;
  if (role === "ADMIN") {
    return [
      { path: "/admin/dashboard", title: "管理员工作台", icon: "✦" },
      { path: "/profile", title: "个人中心", icon: "◌" }
    ];
  }
  if (role === "COUNSELOR") {
    if (status === 2) return [{ path: "/counselor/audit-pending", title: "资质审核", icon: "◷" }];
    return [
      { path: "/counselor/dashboard", title: "咨询师工作台", icon: "✦" },
      { path: "/profile", title: "个人中心", icon: "◌" }
    ];
  }
  return [
    { path: "/home", title: "首页", icon: "✦" },
    { path: "/mood-diary", title: "情绪日记", icon: "◍" },
    { path: "/ai-chat", title: "AI 咨询", icon: "✧" },
    { path: "/assessment", title: "心理测评", icon: "◇" },
    { path: "/counselor-list", title: "咨询师推荐", icon: "◎" },
    { path: "/my-appointments", title: "我的预约", icon: "◷" },
    { path: "/emotion-report", title: "情绪报告", icon: "▰" },
    { path: "/meditation", title: "冥想", icon: "☾" }
  ];
});

const logout = () => {
  session.logout();
  router.push("/login");
};
</script>
