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

const TOKEN_KEY = "mindease-demo-token";
const PROFILE_KEY = "mindease-demo-profile";

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

  const defaultRoute = computed(() => {
    if (!profile.value) {
      return "/login";
    }

    if (profile.value.role === "ADMIN") {
      return "/admin/dashboard";
    }

    if (profile.value.role === "COUNSELOR") {
      return profile.value.counselorStatus === "PENDING"
        ? "/counselor/audit-pending"
        : "/counselor/dashboard";
    }

    return "/home";
  });

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
    login,
    logout,
  };
});
