// 用户状态管理
import { defineStore } from "pinia";
import { ref } from "vue";
import type { UserInfo } from "@/api/auth";
import { getUserProfile } from "@/api/auth";

export const useUserStore = defineStore("user", () => {
  // 用户信息
  const userInfo = ref<UserInfo | null>(null);

  // Token
  const token = ref<string>(localStorage.getItem("token") || "");

  // 是否已登录
  const isLoggedIn = ref<boolean>(!!token.value);

  // 设置 Token
  const setToken = (newToken: string) => {
    token.value = newToken;
    localStorage.setItem("token", newToken);
    isLoggedIn.value = true;
  };

  // 设置用户信息
  const setUserInfo = (info: UserInfo) => {
    userInfo.value = info;
  };

  // 获取用户信息
  const fetchUserInfo = async () => {
    try {
      const res = await getUserProfile();
      const data = (res.data as any).data || res.data;
      userInfo.value = data;
      return data;
    } catch (error) {
      console.error("获取用户信息失败:", error);
      throw error;
    }
  };

  // 登出
  const logout = () => {
    token.value = "";
    userInfo.value = null;
    isLoggedIn.value = false;
    localStorage.removeItem("token");
  };

  return {
    userInfo,
    token,
    isLoggedIn,
    setToken,
    setUserInfo,
    fetchUserInfo,
    logout,
  };
});
