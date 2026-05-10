<template>
  <div class="login-container">
    <!-- 背景装饰层 -->
    <div class="noise-overlay"></div>
    <div class="fixed inset-0 overflow-hidden pointer-events-none z-0">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="orb orb-3"></div>
    </div>

    <!-- 登录卡片 -->
    <div class="login-card glass-card animate-fade-in">
      <div class="login-header">
        <h1>MindEase</h1>
        <p>智能心理健康支持平台</p>
      </div>

      <!-- 开发提示 -->
      <el-alert title="开发提示" type="info" :closable="false" class="dev-tip">
        后端未启动时，可使用"演示模式"查看前端界面
      </el-alert>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            size="large"
            prefix-icon="User"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <button
            type="button"
            :disabled="loading"
            class="btn-accent w-full"
            @click="handleLogin"
          >
            <i class="fas fa-sign-in-alt mr-2" v-if="!loading"></i>
            <span v-if="loading">登录中...</span>
            <span v-else>登录</span>
          </button>
        </el-form-item>

        <el-form-item>
          <button
            type="button"
            class="demo-btn w-full"
            @click="handleDemoLogin"
          >
            <i class="fas fa-eye mr-2"></i>
            演示模式（跳过登录）
          </button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <span>还没有账号？</span>
        <router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import { login } from "@/api/auth";
import { useUserStore } from "@/stores/user";

const router = useRouter();
const userStore = useUserStore();

const loginFormRef = ref<FormInstance>();
const loading = ref(false);

const loginForm = reactive({
  username: "",
  password: "",
});

const loginRules: FormRules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, message: "密码长度不能少于6位", trigger: "blur" },
  ],
};

// 正常登录（需要后端API）
const handleLogin = async () => {
  if (!loginFormRef.value) return;

  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true;
      try {
        const res = await login(loginForm);
        const userData = res.data as any;

        // 保存token
        userStore.setToken(userData.token);

        // 获取完整用户信息
        console.log("[登录] token已保存，开始获取完整用户信息...");
        const fullUserInfo = await userStore.fetchUserInfo();
        console.log("[登录] 获取到完整用户信息：", {
          role: fullUserInfo.role,
          status: fullUserInfo.status,
          userId: fullUserInfo.userId,
        });

        ElMessage.success("登录成功");

        // 根据角色和状态跳转到不同页面
        if (fullUserInfo.role?.toUpperCase() === "COUNSELOR") {
          // 咨询师根据审核状态跳转
          if (fullUserInfo.status === 2) {
            // 待审核：跳转到待审核专用页面
            console.log("[登录] 待审核咨询师，跳转到 /counselor/audit-pending");
            router.push("/counselor/audit-pending");
          } else {
            // 已审核：跳转到工作台
            console.log("[登录] 已审核咨询师，跳转到 /counselor/dashboard");
            router.push("/counselor/dashboard");
          }
        } else if (fullUserInfo.role?.toUpperCase() === "ADMIN") {
          console.log("[登录] 管理员，跳转到 /admin/dashboard");
          router.push("/admin/dashboard");
        } else {
          console.log("[登录] 普通用户，跳转到 /home");
          router.push("/home");
        }
      } catch (error: any) {
        console.error("登录失败:", error);
        // 处理403错误（账号审核中或已禁用）
        if (error.response?.status === 403) {
          ElMessage.error(error.response?.data?.message || "登录失败");
        } else {
          ElMessage.error("登录失败，请检查用户名和密码");
        }
      } finally {
        loading.value = false;
      }
    }
  });
};

// 演示模式登录（Mock数据，用于前端开发测试）
const handleDemoLogin = () => {
  // 模拟Token
  const mockToken = "mock_token_for_development_" + Date.now();

  // 模拟用户信息（普通用户）
  const mockUserInfo = {
    userId: 1001,
    username: "demo_user",
    nickname: "演示用户",
    avatar:
      "https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png",
    role: "user" as const,
    status: 1, // 启用状态
  };

  // 保存到状态管理
  userStore.setToken(mockToken);
  userStore.setUserInfo(mockUserInfo);

  ElMessage.success("已进入演示模式");
  router.push("/home");
};
</script>

<style scoped>
/* 登录容器 - 使用CSS变量 */
.login-container {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--ease-bg);
  padding: 20px;
}

/* 登录卡片 - glass-card样式已在shared-styles中定义 */
.login-card {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 420px;
  padding: 48px 40px;
  border-radius: 2rem;
}

/* 标题区域 */
.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.login-header h1 {
  font-size: 32px;
  font-weight: bold;
  color: var(--ease-dark);
  margin-bottom: 8px;
}

.login-header p {
  font-size: 14px;
  color: var(--gray-500);
}

/* 表单区域 */
.login-form {
  margin-bottom: 24px;
}

/* 页脚 */
.login-footer {
  text-align: center;
  font-size: 14px;
  color: var(--gray-500);
}

.login-footer a {
  color: var(--ease-accent);
  text-decoration: none;
  margin-left: 8px;
  transition: color 0.3s ease;
}

.login-footer a:hover {
  color: var(--ease-accent-dark);
}

/* 开发提示 */
.dev-tip {
  margin-bottom: 20px;
}

/* 演示按钮 */
.demo-btn {
  padding: 0.75rem 2rem;
  border-radius: 0.75rem;
  font-weight: 600;
  transition: all 0.3s ease;
  background: white;
  border: 2px solid var(--ease-accent);
  color: var(--ease-accent);
  cursor: pointer;
}

.demo-btn:hover {
  background: var(--ease-bg);
  border-color: var(--ease-accent-dark);
  color: var(--ease-accent-dark);
}

/* 工具类 */
.w-full {
  width: 100%;
}

.mr-2 {
  margin-right: 0.5rem;
}

/* 固定定位辅助类 */
.fixed {
  position: fixed;
}

.inset-0 {
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
}

.overflow-hidden {
  overflow: hidden;
}

.pointer-events-none {
  pointer-events: none;
}

.z-0 {
  z-index: 0;
}
</style>
