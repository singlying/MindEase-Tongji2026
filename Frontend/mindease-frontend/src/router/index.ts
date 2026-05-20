import { createRouter, createWebHistory } from "vue-router";
import { useUserStore } from "@/stores/user";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: "/login",
      name: "Login",
      component: () => import("@/views/auth/LoginView.vue"),
      meta: { requiresAuth: false },
    },
    {
      path: "/register",
      name: "Register",
      component: () => import("@/views/auth/RegisterView.vue"),
      meta: { requiresAuth: false },
    },
    {
      path: "/",
      name: "Layout",
      component: () => import("@/components/layout/MainLayout.vue"),
      meta: { requiresAuth: true },
      children: [
        {
          path: "/home",
          name: "Home",
          component: () => import("@/views/user/HomeView.vue"),
          meta: { title: "首页", role: "user" },
        },
        {
          path: "/mood-diary",
          name: "MoodDiary",
          component: () => import("@/views/user/mood/MoodDiaryView.vue"),
          meta: { title: "情绪日记", role: "user" },
        },
        {
          path: "/mood-diary/:id",
          name: "MoodDiaryDetail",
          component: () => import("@/views/user/mood/MoodDiaryDetailView.vue"),
          meta: { title: "日记详情", role: "user" },
        },
        {
          path: "/ai-chat",
          name: "AIChat",
          component: () => import("@/views/user/chat/AIChatView.vue"),
          meta: { title: "AI 咨询", role: "user" },
        },
        {
          path: "/assessment",
          name: "Assessment",
          component: () => import("@/views/user/assessment/AssessmentView.vue"),
          meta: { title: "心理测评", role: "user" },
        },
        {
          path: "/assessment/:scaleKey",
          name: "AssessmentDetail",
          component: () =>
            import("@/views/user/assessment/AssessmentDetailView.vue"),
          meta: { title: "测评答题", role: "user" },
        },
        {
          path: "/assessment/result/:recordId",
          name: "AssessmentResult",
          component: () =>
            import("@/views/user/assessment/AssessmentResultView.vue"),
          meta: { title: "测评结果", role: "user" },
        },
        {
          path: "/assessment/history",
          name: "AssessmentHistory",
          component: () =>
            import("@/views/user/assessment/AssessmentHistoryView.vue"),
          meta: { title: "测评历史", role: "user" },
        },
        {
          path: "/counselor-list",
          name: "CounselorList",
          component: () =>
            import("@/views/user/counselor/CounselorListView.vue"),
          meta: { title: "咨询师推荐", role: "user" },
        },
        {
          path: "/booking/:counselorId",
          name: "Booking",
          component: () => import("@/views/user/appointment/BookingView.vue"),
          meta: { title: "预约咨询", role: "user" },
        },
        {
          path: "/my-appointments",
          name: "MyAppointments",
          component: () =>
            import("@/views/user/appointment/MyAppointmentsView.vue"),
          meta: { title: "我的预约", role: "user" },
        },
        {
          path: "/profile",
          name: "Profile",
          component: () => import("@/views/user/profile/ProfileView.vue"),
          meta: { title: "个人中心" },
        },
        {
          path: "/emotion-report",
          name: "EmotionReport",
          component: () => import("@/views/user/report/EmotionReportView.vue"),
          meta: { title: "情绪报告", role: "user" },
        },
        {
          path: "/meditation",
          name: "Meditation",
          component: () => import("@/views/user/meditation/MeditationView.vue"),
          meta: { title: "冥想时刻", role: "user" },
        },
        {
          path: "/counselor/dashboard",
          name: "CounselorDashboard",
          component: () => import("@/views/counselor/DashboardView.vue"),
          meta: { title: "咨询师工作台", role: "counselor" },
        },
        {
          path: "/counselor/audit-pending",
          name: "CounselorAuditPending",
          component: () => import("@/views/counselor/AuditPendingView.vue"),
          meta: { title: "待审核", role: "counselor" },
        },
        {
          path: "/admin/dashboard",
          name: "AdminDashboard",
          component: () => import("@/views/admin/DashboardView.vue"),
          meta: { title: "管理员工作台", role: "admin" },
        },
      ],
    },
  ],
});

const getDefaultPath = (role?: string, status?: number) => {
  const normalizedRole = role?.toUpperCase();
  if (normalizedRole === "COUNSELOR") {
    return status === 2 ? "/counselor/audit-pending" : "/counselor/dashboard";
  }
  if (normalizedRole === "ADMIN") {
    return "/admin/dashboard";
  }
  return "/home";
};

router.beforeEach(async (to) => {
  const userStore = useUserStore();

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return "/login";
  }

  if (userStore.isLoggedIn && !userStore.userInfo) {
    try {
      await userStore.fetchUserInfo();
    } catch (_error) {
      userStore.logout();
      return "/login";
    }
  }

  const userInfo = userStore.userInfo;
  if (!userInfo) {
    return true;
  }

  const userRole = userInfo.role?.toUpperCase();
  const targetRole = (to.meta.role as string | undefined)?.toUpperCase();

  if (userRole === "COUNSELOR" && userInfo.status === 2) {
    const allowedPaths = [
      "/counselor/audit-pending",
      "/login",
      "/register",
      "/profile",
    ];

    if (!allowedPaths.includes(to.path)) {
      return "/counselor/audit-pending";
    }
  }

  if (
    (to.path === "/" || to.path === "/login" || to.path === "/register") &&
    userStore.isLoggedIn
  ) {
    return getDefaultPath(userInfo.role, userInfo.status);
  }

  if (targetRole && targetRole !== userRole) {
    return getDefaultPath(userInfo.role, userInfo.status);
  }

  return true;
});

export default router;
