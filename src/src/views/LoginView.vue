<template>
  <div class="auth-screen">
    <form class="auth-card stack" @submit.prevent="handleLogin">
      <div class="brand-mark">ME</div>
      <div>
        <h1>欢迎回到 MindEase</h1>
        <p class="muted">今天也可以慢慢来。这里会陪你记录情绪、整理想法、找到合适的支持。</p>
      </div>
      <label class="field">
        <span>用户名</span>
        <input v-model="form.username" class="input" required autocomplete="username" />
      </label>
      <label class="field">
        <span>密码</span>
        <input v-model="form.password" class="input" type="password" required autocomplete="current-password" />
      </label>
      <button class="btn" :disabled="loading">{{ loading ? "登录中..." : "登录" }}</button>
      <button class="btn secondary" type="button" @click="handleDemoLogin">演示模式</button>
      <p class="muted">还没有账号？<RouterLink class="link" to="/register">立即注册</RouterLink></p>
      <p v-if="error" class="muted" style="color: var(--danger)">{{ error }}</p>
    </form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { authApi } from "@/api";
import { useSessionStore } from "@/stores/session";

const router = useRouter();
const session = useSessionStore();
const loading = ref(false);
const error = ref("");
const form = reactive({ username: "", password: "" });

const targetByRole = (role?: string, status?: number) => {
  if (role?.toUpperCase() === "COUNSELOR") return status === 2 ? "/counselor/audit-pending" : "/counselor/dashboard";
  if (role?.toUpperCase() === "ADMIN") return "/admin/dashboard";
  return "/home";
};

const handleLogin = async () => {
  loading.value = true;
  error.value = "";
  try {
    const login = await authApi.login(form);
    session.setToken(login.data.token);
    const profile = await session.fetchUserInfo();
    router.push(targetByRole(profile.role, profile.status));
  } catch (err: any) {
    error.value = err?.response?.data?.message || err?.message || "登录失败，请检查账号或后端服务";
  } finally {
    loading.value = false;
  }
};

const handleDemoLogin = () => {
  session.setToken(`demo_token_${Date.now()}`);
  session.setUserInfo({
    userId: 1001,
    username: "demo_user",
    nickname: "演示用户",
    avatar: "",
    role: "user",
    status: 1
  });
  router.push("/home");
};
</script>
