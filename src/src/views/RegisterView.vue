<template>
  <div class="auth-screen">
    <form class="auth-card stack" @submit.prevent="submit">
      <div class="brand-mark">ME</div>
      <div>
        <h1>创建 MindEase 账号</h1>
        <p class="muted">选择你的身份，开启专属的心理健康支持旅程。</p>
      </div>
      <input v-model="form.username" class="input" placeholder="用户名" required />
      <input v-model="form.nickname" class="input" placeholder="昵称" required />
      <input v-model="form.phone" class="input" placeholder="手机号" required />
      <input v-model="form.password" class="input" placeholder="密码" type="password" required />
      <select v-model="form.role" class="select">
        <option value="user">普通用户</option>
        <option value="counselor">咨询师</option>
        <option value="admin">管理员</option>
      </select>
      <input v-if="form.role === 'admin'" v-model="form.invitationCode" class="input" placeholder="管理员邀请码" />
      <button class="btn" :disabled="loading">{{ loading ? "提交中..." : "注册" }}</button>
      <p class="muted">已有账号？<RouterLink class="link" to="/login">去登录</RouterLink></p>
      <p v-if="message" class="muted">{{ message }}</p>
    </form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { authApi } from "@/api";

const loading = ref(false);
const message = ref("");
const form = reactive({
  username: "",
  password: "",
  nickname: "",
  phone: "",
  role: "user" as "user" | "counselor" | "admin",
  invitationCode: ""
});

const submit = async () => {
  loading.value = true;
  message.value = "";
  try {
    const response = await authApi.register(form);
    message.value = response.message || "注册成功，请返回登录";
  } catch (err: any) {
    message.value = err?.response?.data?.message || err?.message || "注册失败";
  } finally {
    loading.value = false;
  }
};
</script>
