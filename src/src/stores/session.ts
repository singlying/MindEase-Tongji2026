import { defineStore } from "pinia";
import { computed, ref } from "vue";
import { authApi, type UserInfo } from "@/api";

export const useSessionStore = defineStore("session", () => {
  const token = ref(localStorage.getItem("token") || "");
  const userInfo = ref<UserInfo | null>(null);
  const isLoggedIn = computed(() => Boolean(token.value));

  const setToken = (value: string) => {
    token.value = value;
    localStorage.setItem("token", value);
  };

  const setUserInfo = (value: UserInfo) => {
    userInfo.value = value;
  };

  const fetchUserInfo = async () => {
    const response = await authApi.profile();
    userInfo.value = response.data;
    return response.data;
  };

  const logout = () => {
    token.value = "";
    userInfo.value = null;
    localStorage.removeItem("token");
  };

  return { token, userInfo, isLoggedIn, setToken, setUserInfo, fetchUserInfo, logout };
});
