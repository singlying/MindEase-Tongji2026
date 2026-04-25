// 前端A负责：用户端主路由骨架
// 前端B负责：第 6 周补充咨询师端 / 管理员端共享整合
import { createRouter, createWebHistory } from "vue-router";

import type { UserRole } from "@/stores/user";
import { isPendingCounselor, useUserStore } from "@/stores/user";

declare module "vue-router" {
  interface RouteMeta {
    title?: string;
    requiresAuth?: boolean;
    role?: UserRole;
  }
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: "/login",
      name: "Login",
      component: () => import("@/views/auth/LoginView.vue"),
      meta: { title: "登录" },
    },
    {
      path: "/register",
      name: "Register",
      component: () => import("@/views/auth/RegisterView.vue"),
      meta: { title: "注册" },
    },
    {
      path: "/",
      component: () => import("@/components/layout/MainLayout.vue"),
      meta: { requiresAuth: true },
      children: [
        {
          path: "home",
          name: "Home",
          component: () => import("@/views/user/HomeView.vue"),
          meta: { title: "首页", role: "USER" },
        },
        {
          path: "mood-diary",
          name: "MoodDiary",
          component: () => import("@/views/user/mood/MoodDiaryView.vue"),
          meta: { title: "情绪日记", role: "USER" },
        },
        {
          path: "mood-diary/:id",
          name: "MoodDiaryDetail",
          component: () => import("@/views/user/mood/MoodDiaryDetailView.vue"),
          meta: { title: "日记详情", role: "USER" },
        },
        {
          path: "ai-chat",
          name: "AIChat",
          component: () => import("@/views/user/chat/AIChatView.vue"),
          meta: { title: "AI 咨询", role: "USER" },
        },
        {
          path: "assessment",
          name: "Assessment",
          component: () => import("@/views/user/assessment/AssessmentView.vue"),
          meta: { title: "心理测评", role: "USER" },
        },
        {
          path: "assessment/:scaleKey",
          name: "AssessmentDetail",
          component: () =>
            import("@/views/user/assessment/AssessmentDetailView.vue"),
          meta: { title: "测评答题", role: "USER" },
        },
        {
          path: "assessment/result/:recordId",
          name: "AssessmentResult",
          component: () =>
            import("@/views/user/assessment/AssessmentResultView.vue"),
          meta: { title: "测评结果", role: "USER" },
        },
        {
          path: "assessment/history",
          name: "AssessmentHistory",
          component: () =>
            import("@/views/user/assessment/AssessmentHistoryView.vue"),
          meta: { title: "测评历史", role: "USER" },
        },
        {
          path: "counselor-list",
          name: "CounselorList",
          component: () =>
            import("@/views/user/counselor/CounselorListView.vue"),
          meta: { title: "咨询师推荐", role: "USER" },
        },
        {
          path: "booking/:counselorId",
          name: "Booking",
          component: () => import("@/views/user/appointment/BookingView.vue"),
          meta: { title: "预约咨询", role: "USER" },
        },
        {
          path: "my-appointments",
          name: "MyAppointments",
          component: () =>
            import("@/views/user/appointment/MyAppointmentsView.vue"),
          meta: { title: "我的预约", role: "USER" },
        },
        {
          path: "emotion-report",
          name: "EmotionReport",
          component: () => import("@/views/user/report/EmotionReportView.vue"),
          meta: { title: "情绪报告", role: "USER" },
        },
        {
          path: "meditation",
          name: "Meditation",
          component: () => import("@/views/user/meditation/MeditationView.vue"),
          meta: { title: "冥想时刻", role: "USER" },
        },
        {
          path: "profile",
          name: "Profile",
          component: () => import("@/views/user/profile/ProfileView.vue"),
          meta: { title: "个人中心" },
        },
        {
          path: "counselor/dashboard",
          name: "CounselorDashboard",
          component: () => import("@/views/counselor/DashboardView.vue"),
          meta: { title: "咨询师工作台", role: "COUNSELOR" },
        },
        {
          path: "counselor/audit-pending",
          name: "CounselorAuditPending",
          component: () => import("@/views/counselor/AuditPendingView.vue"),
          meta: { title: "资质审核", role: "COUNSELOR" },
        },
        {
          path: "admin/dashboard",
          name: "AdminDashboard",
          component: () => import("@/views/admin/DashboardView.vue"),
          meta: { title: "管理员工作台", role: "ADMIN" },
        },
      ],
    },
  ],
});

const PUBLIC_PATHS = ["/login", "/register"];

router.beforeEach((to, from, next) => {
  const userStore = useUserStore();
  const isPublicPage = PUBLIC_PATHS.includes(to.path);

  if (!isPublicPage && !userStore.isLoggedIn) {
    next("/login");
    return;
  }

  if (isPublicPage && userStore.isLoggedIn) {
    next(userStore.defaultRoute);
    return;
  }

  if (to.path === "/" && userStore.isLoggedIn) {
    next(userStore.defaultRoute);
    return;
  }

  if (isPendingCounselor(userStore.profile)) {
    const allowedPaths = ["/counselor/audit-pending", "/profile"];

    if (!allowedPaths.includes(to.path) && !isPublicPage) {
      next("/counselor/audit-pending");
      return;
    }
  }

  if (
    to.path === "/counselor/audit-pending" &&
    userStore.profile?.role === "COUNSELOR" &&
    !isPendingCounselor(userStore.profile)
  ) {
    next("/counselor/dashboard");
    return;
  }

  if (to.meta.role && userStore.profile?.role !== to.meta.role) {
    next(userStore.defaultRoute);
    return;
  }

  next();
});

export default router;
