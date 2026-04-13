<!-- 前端A负责：登录页、演示登录入口 -->
<script setup lang="ts">
import { reactive } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";

import type { CounselorStatus, UserRole } from "@/stores/user";
import { useUserStore } from "@/stores/user";

const router = useRouter();
const userStore = useUserStore();

const form = reactive<{
  nickname: string;
  role: UserRole;
  counselorStatus: CounselorStatus;
}>({
  nickname: "MindEase 体验用户",
  role: "USER",
  counselorStatus: "APPROVED",
});

function submitLogin() {
  userStore.login({
    nickname: form.nickname || "MindEase 体验用户",
    role: form.role,
    counselorStatus: form.role === "COUNSELOR" ? form.counselorStatus : undefined,
  });

  ElMessage.success("登录成功，正在跳转。");
  router.push(userStore.defaultRoute);
}

function quickLogin(role: UserRole, counselorStatus: CounselorStatus = "APPROVED") {
  form.role = role;
  form.counselorStatus = counselorStatus;
  form.nickname =
    role === "USER"
      ? "用户端体验账号"
      : role === "COUNSELOR"
        ? counselorStatus === "PENDING"
          ? "待审核咨询师"
          : "咨询师体验账号"
        : "管理员体验账号";

  submitLogin();
}
</script>

<template>
  <div class="auth-page">
    <section class="auth-card glass-card">
      <div class="auth-intro">
        <h1>欢迎来到 MindEase</h1>
        <p>
          当前版本支持快速进入不同角色页面，便于体验系统的主要功能入口。
        </p>
      </div>

      <el-form label-position="top" class="auth-form">
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="请输入显示昵称" />
        </el-form-item>

        <el-form-item label="角色">
          <el-radio-group v-model="form.role">
            <el-radio-button value="USER">普通用户</el-radio-button>
            <el-radio-button value="COUNSELOR">咨询师</el-radio-button>
            <el-radio-button value="ADMIN">管理员</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="form.role === 'COUNSELOR'" label="咨询师状态">
          <el-radio-group v-model="form.counselorStatus">
            <el-radio-button value="APPROVED">已审核</el-radio-button>
            <el-radio-button value="PENDING">待审核</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <div class="form-actions">
          <el-button type="primary" @click="submitLogin">进入系统</el-button>
          <el-button @click="router.push('/register')">去注册页</el-button>
        </div>
      </el-form>

      <div class="quick-actions">
        <span>快速进入：</span>
        <el-button plain @click="quickLogin('USER')">用户端</el-button>
        <el-button plain @click="quickLogin('COUNSELOR', 'APPROVED')">咨询师端</el-button>
        <el-button plain @click="quickLogin('COUNSELOR', 'PENDING')">待审核咨询师</el-button>
        <el-button plain @click="quickLogin('ADMIN')">管理员端</el-button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
}

.auth-card {
  width: min(760px, 100%);
  padding: 32px;
  display: grid;
  gap: 24px;
}

h1 {
  margin: 0 0 8px;
}

p {
  margin: 0;
  color: var(--ease-muted);
  line-height: 1.7;
}

.auth-form {
  display: grid;
}

.form-actions,
.quick-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.quick-actions {
  align-items: center;
}
</style>
