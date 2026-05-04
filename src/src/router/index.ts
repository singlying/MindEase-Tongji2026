import { createRouter, createWebHistory } from "vue-router";
import { useSessionStore } from "@/stores/session";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: "/login", name: "Login", component: () => import("@/views/LoginView.vue"), meta: { public: true } },
    { path: "/register", name: "Register", component: () => import("@/views/RegisterView.vue"), meta: { public: true } },
    {
      path: "/",
      component: () => import("@/components/AppShell.vue"),
      meta: { requiresAuth: true },
      children: [
        { path: "", redirect: "/home" },
        { path: "home", component: () => import("@/views/HomeView.vue"), meta: { role: "user" } },
        { path: "mood-diary", component: () => import("@/views/MoodView.vue"), meta: { role: "user" } },
        { path: "mood-diary/:id", component: () => import("@/views/MoodView.vue"), meta: { role: "user" } },
        { path: "ai-chat", component: () => import("@/views/ChatView.vue"), meta: { role: "user" } },
        { path: "assessment", component: () => import("@/views/AssessmentView.vue"), meta: { role: "user" } },
        { path: "assessment/:scaleKey", component: () => import("@/views/AssessmentView.vue"), meta: { role: "user" } },
        { path: "assessment/result/:recordId", component: () => import("@/views/AssessmentView.vue"), meta: { role: "user" } },
        { path: "assessment/history", component: () => import("@/views/AssessmentView.vue"), meta: { role: "user" } },
        { path: "counselor-list", component: () => import("@/views/CounselorView.vue"), meta: { role: "user" } },
        { path: "booking/:counselorId", component: () => import("@/views/BookingView.vue"), meta: { role: "user" } },
        { path: "my-appointments", component: () => import("@/views/AppointmentsView.vue"), meta: { role: "user" } },
        { path: "emotion-report", component: () => import("@/views/ReportView.vue"), meta: { role: "user" } },
        { path: "meditation", component: () => import("@/views/MeditationView.vue"), meta: { role: "user" } },
        { path: "profile", component: () => import("@/views/ProfileView.vue") },
        { path: "counselor/dashboard", component: () => import("@/views/CounselorDashboardView.vue"), meta: { role: "counselor" } },
        { path: "counselor/audit-pending", component: () => import("@/views/AuditPendingView.vue"), meta: { role: "counselor" } },
        { path: "admin/dashboard", component: () => import("@/views/AdminDashboardView.vue"), meta: { role: "admin" } }
      ]
    }
  ]
});

const homeByUser = (role?: string, status?: number) => {
  const normalized = role?.toUpperCase();
  if (normalized === "COUNSELOR") {
    return status === 2 ? "/counselor/audit-pending" : "/counselor/dashboard";
  }
  if (normalized === "ADMIN") return "/admin/dashboard";
  return "/home";
};

router.beforeEach(async (to) => {
  const session = useSessionStore();
  if (to.meta.requiresAuth && !session.isLoggedIn) return "/login";

  if (session.isLoggedIn && !session.userInfo) {
    try {
      await session.fetchUserInfo();
    } catch {
      session.logout();
      return "/login";
    }
  }

  const user = session.userInfo;
  if ((to.path === "/" || to.path === "/login" || to.path === "/register") && session.isLoggedIn) {
    return homeByUser(user?.role, user?.status);
  }

  if (user?.role?.toUpperCase() === "COUNSELOR" && user.status === 2) {
    const allowed = ["/counselor/audit-pending", "/profile"];
    if (!allowed.includes(to.path)) return "/counselor/audit-pending";
  }

  const targetRole = String(to.meta.role || "").toUpperCase();
  if (targetRole && user && targetRole !== user.role.toUpperCase()) {
    return homeByUser(user.role, user.status);
  }
});

export default router;
