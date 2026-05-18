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
      // 不设置固定的redirect，由路由守卫根据角色动态重定向
      meta: { requiresAuth: true },
      children: [
        // ============ 用户端路由 ============
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
          meta: { title: "AI咨询", role: "user" },
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

        // ============ 咨询师端路由 ============
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

        // ============ 管理员端路由 ============
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

// 路由守卫
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore();

  console.log("[Router Guard] 进入路由守卫", {
    from: from.path,
    to: to.path,
    isLoggedIn: userStore.isLoggedIn,
    userInfo: userStore.userInfo
      ? {
          role: userStore.userInfo.role,
          status: userStore.userInfo.status,
        }
      : null,
  });

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    console.log("[Router Guard] 需要登录，跳转到/login");
    next("/login");
    return;
  }

  // 最高优先级：待审核咨询师权限控制（受限Token机制）
  if (userStore.isLoggedIn && userStore.userInfo) {
    const { role, status } = userStore.userInfo;
    console.log("[Router Guard] 检查用户信息", {
      role,
      status,
      targetPath: to.path,
    });

    // 待审核咨询师只能访问审核相关页面
    if (role?.toUpperCase() === "COUNSELOR" && status === 2) {
      const allowedPaths = [
        "/counselor/audit-pending",
        "/login",
        "/register",
        "/profile",
      ];

      if (!allowedPaths.includes(to.path)) {
        console.log(
          "[路由守卫] 待审核咨询师尝试访问受限页面，强制跳转到审核页面",
          to.path
        );
        next("/counselor/audit-pending");
        return;
      }
    }

    // 已审核咨询师不应访问待审核页面
    if (role?.toUpperCase() === "COUNSELOR" && status === 1) {
      if (to.path === "/counselor/audit-pending") {
        console.log("[路由守卫] 已审核咨询师尝试访问待审核页面，跳转到工作台");
        next("/counselor/dashboard");
        return;
      }
    }
  }

  // 已登录用户访问登录/注册页或根路径，根据角色和状态重定向
  if (
    (to.path === "/login" || to.path === "/register" || to.path === "/") &&
    userStore.isLoggedIn
  ) {
    const userInfo = userStore.userInfo;

    // 咨询师根据状态跳转
    if (userInfo?.role?.toUpperCase() === "COUNSELOR") {
      if (userInfo.status === 2) {
        next("/counselor/audit-pending");
      } else {
        next("/counselor/dashboard");
      }
    } else if (userInfo?.role?.toUpperCase() === "ADMIN") {
      next("/admin/dashboard");
    } else {
      next("/home");
    }
    return;
  }

  // 防止不同角色访问其他角色的页面
  if (userStore.isLoggedIn && userStore.userInfo) {
    const userRole = userStore.userInfo.role?.toUpperCase();
    const targetRole = to.meta.role as string;

    // 如果路由指定了role，检查是否匹配
    if (targetRole && targetRole.toUpperCase() !== userRole) {
      // 角色不匹配，重定向到对应的首页
      console.log("[路由守卫] 角色不匹配，重定向", {
        userRole,
        targetRole,
        targetPath: to.path,
      });
      if (userRole === "COUNSELOR") {
        if (userStore.userInfo.status === 2) {
          next("/counselor/audit-pending");
        } else {
          next("/counselor/dashboard");
        }
      } else if (userRole === "ADMIN") {
        next("/admin/dashboard");
      } else {
        next("/home");
      }
      return;
    }
  }

  next();
});

export default router;
