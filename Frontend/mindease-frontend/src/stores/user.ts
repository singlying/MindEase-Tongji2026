// 前端A负责：登录态、角色状态、默认跳转规则
import { computed, ref } from "vue";
import { defineStore } from "pinia";

export type UserRole = "USER" | "COUNSELOR" | "ADMIN";
export type CounselorStatus = "PENDING" | "APPROVED";

export interface DemoUserProfile {
  nickname: string;
  role: UserRole;
  counselorStatus?: CounselorStatus;
}

export interface NavigationItem {
  label: string;
  path: string;
}

const TOKEN_KEY = "mindease-demo-token";
const PROFILE_KEY = "mindease-demo-profile";

export function isPendingCounselor(profile: DemoUserProfile | null) {
  return (
    profile?.role === "COUNSELOR" && profile.counselorStatus === "PENDING"
  );
}

export function getDefaultRoute(profile: DemoUserProfile | null) {
  if (!profile) {
    return "/login";
  }

  if (profile.role === "ADMIN") {
    return "/admin/dashboard";
  }

  if (profile.role === "COUNSELOR") {
    return isPendingCounselor(profile)
      ? "/counselor/audit-pending"
      : "/counselor/dashboard";
  }

  return "/home";
}

export function getRoleLabel(profile: DemoUserProfile | null) {
  if (!profile) {
    return "未登录";
  }

  if (profile.role === "COUNSELOR") {
    return isPendingCounselor(profile) ? "咨询师待审核" : "咨询师端";
  }

  return profile.role === "ADMIN" ? "管理员端" : "用户端";
}

export function getNavigationItems(
  profile: DemoUserProfile | null,
): NavigationItem[] {
  if (!profile) {
    return [];
  }

  if (profile.role === "ADMIN") {
    return [{ label: "管理员工作台", path: "/admin/dashboard" }];
  }

  if (profile.role === "COUNSELOR") {
    return isPendingCounselor(profile)
      ? [{ label: "资质审核", path: "/counselor/audit-pending" }]
      : [{ label: "咨询师工作台", path: "/counselor/dashboard" }];
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
}

function readProfile(): DemoUserProfile | null {
  const raw = localStorage.getItem(PROFILE_KEY);

  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as DemoUserProfile;
  } catch {
    localStorage.removeItem(PROFILE_KEY);
    return null;
  }
}

export const useUserStore = defineStore("user", () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || "");
  const profile = ref<DemoUserProfile | null>(readProfile());

  const isLoggedIn = computed(() => Boolean(token.value && profile.value));
  const defaultRoute = computed(() => getDefaultRoute(profile.value));
  const roleLabel = computed(() => getRoleLabel(profile.value));
  const navigationItems = computed(() => getNavigationItems(profile.value));
  const pendingCounselor = computed(() => isPendingCounselor(profile.value));

  function login(nextProfile: DemoUserProfile) {
    token.value = `demo-token-${Date.now()}`;
    profile.value = nextProfile;
    localStorage.setItem(TOKEN_KEY, token.value);
    localStorage.setItem(PROFILE_KEY, JSON.stringify(nextProfile));
  }

  function logout() {
    token.value = "";
    profile.value = null;
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(PROFILE_KEY);
  }

  return {
    token,
    profile,
    isLoggedIn,
    defaultRoute,
    roleLabel,
    navigationItems,
    pendingCounselor,
    login,
    logout,
  };
});
