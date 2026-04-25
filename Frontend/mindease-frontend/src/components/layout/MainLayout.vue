<!-- 前端A统一负责：共享主布局文件 -->
<!-- 前端B如需调整医生端 / 管理员端入口，通过A统一提交，避免共享文件冲突 -->
<script setup lang="ts">
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";

import { useUserStore } from "@/stores/user";

interface MenuItem {
  label: string;
  path: string;
}

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const pageTitle = computed(() => route.meta.title || "MindEase");

const menuItems = computed<MenuItem[]>(() => {
  if (!userStore.profile) {
    return [];
  }

  if (userStore.profile.role === "ADMIN") {
    return [{ label: "管理员工作台", path: "/admin/dashboard" }];
  }

  if (userStore.profile.role === "COUNSELOR") {
    if (userStore.profile.counselorStatus === "PENDING") {
      return [{ label: "资质审核", path: "/counselor/audit-pending" }];
    }

    return [{ label: "咨询师工作台", path: "/counselor/dashboard" }];
  }

  return [
    { label: "首页", path: "/home" },
    { label: "情绪日记", path: "/mood-diary" },
    { label: "AI 咨询", path: "/ai-chat" },
    { label: "心理测评", path: "/assessment" },
    { label: "咨询师推荐", path: "/counselor-list" },
    { label: "我的预约", path: "/my-appointments" },
    { label: "情绪报告", path: "/emotion-report" },
    { label: "冥想时刻", path: "/meditation" },
  ];
});

function goProfile() {
  router.push("/profile");
}

function handleLogout() {
  userStore.logout();
  router.push("/login");
}
</script>

<template>
  <div class="shell">
    <aside class="sidebar glass-card">
      <div>
        <div class="brand">MindEase</div>
        <div class="owner-tag">智能心理支持与咨询平台</div>
      </div>

      <el-menu
        :default-active="route.path"
        router
        class="nav-menu"
      >
        <el-menu-item
          v-for="item in menuItems"
          :key="item.path"
          :index="item.path"
        >
          {{ item.label }}
        </el-menu-item>
      </el-menu>

      <div class="sidebar-footer">
        <div class="user-panel">
          <strong>{{ userStore.profile?.nickname }}</strong>
          <span>
            {{
              userStore.profile?.role === "USER"
                ? "用户端"
                : userStore.profile?.role === "COUNSELOR"
                  ? userStore.profile?.counselorStatus === "PENDING"
                    ? "咨询师待审核"
                    : "咨询师端"
                  : "管理员端"
            }}
          </span>
        </div>
        <div class="footer-actions">
          <el-button plain @click="goProfile">个人中心</el-button>
          <el-button type="primary" @click="handleLogout">退出登录</el-button>
        </div>
      </div>
    </aside>

    <section class="content-area">
      <header class="topbar glass-card">
        <div>
          <div class="topbar-title">{{ pageTitle }}</div>
          <div class="topbar-subtitle">
            关注情绪变化，逐步建立更稳定的自我支持节奏。
          </div>
        </div>
      </header>

      <main class="content-main">
        <router-view />
      </main>
    </section>
  </div>
</template>

<style scoped>
.shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 20px;
  padding: 20px;
}

.sidebar {
  padding: 24px 18px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: calc(100vh - 40px);
}

.brand {
  font-size: 28px;
  font-weight: 800;
  color: var(--ease-primary-dark);
  margin-bottom: 8px;
}

.owner-tag {
  color: var(--ease-muted);
  font-size: 13px;
}

.nav-menu {
  border-right: none;
  background: transparent;
  margin: 24px 0;
}

.sidebar-footer {
  display: grid;
  gap: 16px;
}

.user-panel {
  display: grid;
  gap: 6px;
  padding: 16px;
  border-radius: 16px;
  background: rgba(123, 158, 137, 0.08);
}

.user-panel span {
  color: var(--ease-muted);
  font-size: 13px;
}

.footer-actions {
  display: grid;
  gap: 10px;
}

.content-area {
  display: grid;
  grid-template-rows: auto 1fr;
  gap: 20px;
}

.topbar {
  padding: 20px 24px;
}

.topbar-title {
  font-size: 28px;
  font-weight: 700;
}

.topbar-subtitle {
  margin-top: 6px;
  color: var(--ease-muted);
}

.content-main {
  min-width: 0;
}

@media (max-width: 960px) {
  .shell {
    grid-template-columns: 1fr;
  }

  .sidebar {
    min-height: auto;
  }
}
</style>
