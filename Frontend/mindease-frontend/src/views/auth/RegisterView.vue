<template>
  <div class="register-container">
    <!-- 背景装饰层 -->
    <div class="noise-overlay"></div>
    <div class="fixed inset-0 overflow-hidden pointer-events-none z-0">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="orb orb-3"></div>
    </div>

    <!-- 注册卡片 -->
    <div class="register-card glass-card animate-fade-in">
      <div class="register-header">
        <h1>注册账号</h1>
        <p>加入 MindEase，开启心理健康之旅</p>
      </div>

      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        class="register-form"
      >
        <!-- 角色选择 -->
        <el-form-item prop="role" label="注册身份">
          <el-radio-group v-model="registerForm.role" size="large">
            <el-radio value="user">普通用户</el-radio>
            <el-radio value="counselor">心理咨询师</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item prop="username">
          <el-input
            v-model="registerForm.username"
            placeholder="请输入用户名"
            size="large"
            prefix-icon="User"
          />
        </el-form-item>

        <el-form-item prop="nickname">
          <el-input
            v-model="registerForm.nickname"
            placeholder="请输入昵称"
            size="large"
            prefix-icon="Avatar"
          />
        </el-form-item>

        <el-form-item prop="phone">
          <el-input
            v-model="registerForm.phone"
            placeholder="请输入手机号"
            size="large"
            prefix-icon="Phone"
            maxlength="11"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="请输入密码（至少6位）"
            size="large"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="请确认密码"
            size="large"
            prefix-icon="Lock"
            show-password
            @keyup.enter="handleRegister"
          />
        </el-form-item>

        <!-- 咨询师注册提示 -->
        <el-alert
          v-if="registerForm.role === 'counselor'"
          title="咨询师注册说明"
          type="info"
          :closable="false"
          class="role-tip"
        >
          注册后需等待管理员审核，审核通过后方可登录使用
        </el-alert>

        <el-form-item>
          <button
            type="button"
            :disabled="loading"
            class="btn-accent w-full"
            @click="handleRegister"
          >
            <i class="fas fa-user-plus mr-2" v-if="!loading"></i>
            <span v-if="loading">注册中...</span>
            <span v-else>注册</span>
          </button>
        </el-form-item>
      </el-form>

      <div class="register-footer">
        <span>已有账号？</span>
        <router-link to="/login">立即登录</router-link>
      </div>
    </div>

    <!-- 咨询师资质提交对话框 -->
    <el-dialog
      v-model="showAuditDialog"
      title="提交资质证明"
      width="600px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <el-alert
        title="重要提示"
        type="warning"
        :closable="false"
        class="audit-alert"
      >
        为避免Token过期无法提交，建议您立即上传资质证明。您也可以选择稍后在个人中心提交。
      </el-alert>

      <el-form
        ref="auditFormRef"
        :model="auditForm"
        :rules="auditRules"
        label-width="100px"
        class="audit-form"
      >
        <el-form-item label="真实姓名" prop="realName">
          <el-input
            v-model="auditForm.realName"
            placeholder="请输入您的真实姓名"
            maxlength="20"
          />
        </el-form-item>

        <el-form-item label="职称" prop="title">
          <el-input
            v-model="auditForm.title"
            placeholder="如：国家二级心理咨询师/督导"
            maxlength="30"
          />
        </el-form-item>

        <el-form-item label="从业年限" prop="experienceYears">
          <el-input-number
            v-model="auditForm.experienceYears"
            :min="0"
            :max="60"
            controls-position="right"
            style="width: 200px"
          />
          <span class="hint-text">年</span>
        </el-form-item>

        <el-form-item label="专长领域" prop="specialty">
          <el-select
            v-model="auditForm.specialty"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="请选择或输入专长"
            style="width: 100%"
          >
            <el-option
              v-for="item in specialtyOptions"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="个人简介" prop="bio">
          <el-input
            v-model="auditForm.bio"
            type="textarea"
            :rows="3"
            maxlength="300"
            show-word-limit
            placeholder="补充专业背景、擅长人群/疗法等，便于审核与匹配"
          />
        </el-form-item>

        <el-form-item label="所在城市" prop="location">
          <el-input
            v-model="auditForm.location"
            placeholder="如：上海浦东 / 线上"
            maxlength="50"
          />
        </el-form-item>

        <el-form-item label="每小时价格" prop="pricePerHour">
          <el-input-number
            v-model="auditForm.pricePerHour"
            :min="0"
            :step="50"
            :max="2000"
            controls-position="right"
            style="width: 220px"
            placeholder="单位：元"
          />
          <span class="hint-text">元/小时</span>
        </el-form-item>

        <el-form-item label="资格证书" prop="qualificationUrl" required>
          <div class="upload-section">
            <el-upload
              class="qualification-uploader"
              action=""
              :auto-upload="true"
              :show-file-list="false"
              accept="image/jpeg,image/png,image/jpg"
              :before-upload="beforeUploadImage"
              :on-change="handleQualificationChange"
              :http-request="handleQualificationUpload"
            >
              <img
                v-if="qualificationPreview"
                :src="qualificationPreview"
                class="upload-preview"
              />
              <div v-else class="upload-placeholder">
                <i class="fas fa-upload"></i>
                <div class="upload-text">点击上传资格证书</div>
              </div>
            </el-upload>
            <div class="upload-tip">
              <i class="fas fa-info-circle"></i>
              支持 JPG、PNG 格式，建议大小不超过 10MB
            </div>

            <!-- ⚠️ 【临时测试】直接输入图片URL（后期删除） -->
            <el-input
              v-model="auditForm.qualificationUrl"
              placeholder="临时测试：直接输入图片URL"
              class="temp-url-input"
            >
              <template #prepend>测试URL</template>
            </el-input>
          </div>
        </el-form-item>

        <el-form-item label="身份证" prop="idCardUrl">
          <div class="upload-section">
            <el-upload
              class="idcard-uploader"
              action=""
              :auto-upload="true"
              :show-file-list="false"
              accept="image/jpeg,image/png,image/jpg"
              :before-upload="beforeUploadImage"
              :on-change="handleIdCardChange"
              :http-request="handleIdCardUpload"
            >
              <img
                v-if="idCardPreview"
                :src="idCardPreview"
                class="upload-preview"
              />
              <div v-else class="upload-placeholder">
                <i class="fas fa-upload"></i>
                <div class="upload-text">点击上传身份证（可选）</div>
              </div>
            </el-upload>
            <div class="upload-tip">
              <i class="fas fa-info-circle"></i>
              可选项，有助于加快审核速度
            </div>

            <!-- ⚠️ 【临时测试】直接输入图片URL（后期删除） -->
            <el-input
              v-model="auditForm.idCardUrl"
              placeholder="临时测试：直接输入图片URL（可选）"
              class="temp-url-input"
            >
              <template #prepend>测试URL</template>
            </el-input>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="handleSkipAudit" :disabled="auditLoading">
          稍后提交
        </el-button>
        <el-button
          type="primary"
          @click="handleSubmitAudit"
          :loading="auditLoading"
        >
          立即提交审核
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import { register } from "@/api/auth";
import { submitAudit } from "@/api/counselor";
import request from "@/api/request";
import { useUserStore } from "@/stores/user";

const router = useRouter();
const userStore = useUserStore();

const registerFormRef = ref<FormInstance>();
const auditFormRef = ref<FormInstance>();
const loading = ref(false);
const showAuditDialog = ref(false);
const auditLoading = ref(false);
const tempToken = ref(""); // 临时保存注册后获得的token

const registerForm = reactive({
  role: "user" as "user" | "counselor" | "admin",
  username: "",
  nickname: "",
  phone: "",
  password: "",
  confirmPassword: "",
  invitationCode: "",
});

const auditForm = reactive({
  realName: "",
  qualificationUrl: "",
  idCardUrl: "",
  title: "",
  experienceYears: null as number | null,
  specialty: [] as string[],
  bio: "",
  location: "",
  pricePerHour: null as number | null,
});

// 预览（避免把 base64 直接写入提交字段）
const qualificationPreview = ref("");
const idCardPreview = ref("");

// 上传前校验
const beforeUploadImage = (file: File) => {
  const validTypes = ["image/jpeg", "image/jpg", "image/png", "image/x-png"];
  if (!validTypes.includes(file.type.toLowerCase())) {
    ElMessage.error(`不支持的图片格式：${file.type}，请上传 JPG/PNG。`);
    return false;
  }
  const isLt10M = file.size / 1024 / 1024 < 10;
  if (!isLt10M) {
    ElMessage.error("上传图片大小不能超过 10MB!");
    return false;
  }
  return true;
};

// 专长选项示例
const specialtyOptions = [
  "焦虑",
  "抑郁",
  "失眠",
  "职场压力",
  "家庭关系",
  "情绪管理",
  "婚恋关系",
];

const validateConfirmPassword = (rule: any, value: any, callback: any) => {
  if (value !== registerForm.password) {
    callback(new Error("两次输入的密码不一致"));
  } else {
    callback();
  }
};

const validatePhone = (rule: any, value: any, callback: any) => {
  const phoneReg = /^1[3-9]\d{9}$/;
  if (!phoneReg.test(value)) {
    callback(new Error("请输入正确的手机号"));
  } else {
    callback();
  }
};

const registerRules: FormRules = {
  role: [{ required: true, message: "请选择注册身份", trigger: "change" }],
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    {
      min: 3,
      max: 20,
      message: "用户名长度在 3 到 20 个字符",
      trigger: "blur",
    },
  ],
  nickname: [{ required: true, message: "请输入昵称", trigger: "blur" }],
  phone: [
    { required: true, message: "请输入手机号", trigger: "blur" },
    { validator: validatePhone, trigger: "blur" },
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, message: "密码长度不能少于6位", trigger: "blur" },
  ],
  confirmPassword: [
    { required: true, message: "请确认密码", trigger: "blur" },
    { validator: validateConfirmPassword, trigger: "blur" },
  ],
};

const auditRules: FormRules = {
  realName: [{ required: true, message: "请输入真实姓名", trigger: "blur" }],
  qualificationUrl: [
    { required: true, message: "请上传资格证书", trigger: "change" },
  ],
  title: [{ required: true, message: "请输入职称", trigger: "blur" }],
  experienceYears: [
    { required: true, message: "请输入从业年限", trigger: "change" },
    {
      type: "number",
      min: 0,
      max: 60,
      message: "年限需在0-60之间",
      trigger: "change",
    },
  ],
  specialty: [
    {
      type: "array",
      required: true,
      message: "请选择至少一个专长领域",
      trigger: "change",
    },
  ],
  pricePerHour: [
    { required: true, message: "请输入每小时价格", trigger: "change" },
  ],
  location: [
    { required: true, message: "请输入所在城市/地区", trigger: "blur" },
  ],
};

const handleRegister = async () => {
  if (!registerFormRef.value) return;

  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true;
      try {
        const res = await register({
          username: registerForm.username,
          nickname: registerForm.nickname,
          phone: registerForm.phone,
          password: registerForm.password,
          role: registerForm.role,
          invitationCode: registerForm.invitationCode || undefined,
        });

        const registerData = res.data as any;

        // 根据角色和状态给出不同提示
        if (registerData.role === "counselor" && registerData.status === 2) {
          // 咨询师注册成功，保存token并弹出审核对话框
          tempToken.value = registerData.token;
          userStore.setToken(registerData.token);

          // 初始化预览（若后端已有占位URL则展示）
          qualificationPreview.value = auditForm.qualificationUrl || "";
          idCardPreview.value = auditForm.idCardUrl || "";

          ElMessage.success("注册成功！");
          showAuditDialog.value = true;
        } else {
          ElMessage.success("注册成功，请登录");
          router.push("/login");
        }
      } catch (error: any) {
        console.error("注册失败:", error);
        ElMessage.error(
          error.response?.data?.message || "注册失败，请稍后重试"
        );
      } finally {
        loading.value = false;
      }
    }
  });
};

/**
 * ========== 审核资质提交功能 ==========
 */

// 处理资格证书上传（仅预览，实际 URL 在上传成功后写入）
const handleQualificationChange = (file: any) => {
  qualificationPreview.value = URL.createObjectURL(file.raw);
};

const handleQualificationUpload = async (options: any) => {
  const formData = new FormData();
  formData.append("file", options.file);
  try {
    const res = await request.post("/upload", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    const url = (res as any)?.data?.data ?? (res as any)?.data;
    if (url) {
      auditForm.qualificationUrl = url;
      qualificationPreview.value = url;
      ElMessage.success("资格证上传成功");
      options.onSuccess?.(res, options.file);
    } else {
      throw new Error("未返回文件地址");
    }
  } catch (error: any) {
    console.error("资格证上传失败:", error);
    ElMessage.error(error?.message || "资格证上传失败，请重试");
    options.onError?.(error);
  }
};

// 处理身份证上传（仅预览，实际 URL 在上传成功后写入）
const handleIdCardChange = (file: any) => {
  idCardPreview.value = URL.createObjectURL(file.raw);
};

const handleIdCardUpload = async (options: any) => {
  const formData = new FormData();
  formData.append("file", options.file);
  try {
    const res = await request.post("/upload", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    const url = (res as any)?.data?.data ?? (res as any)?.data;
    if (url) {
      auditForm.idCardUrl = url;
      idCardPreview.value = url;
      ElMessage.success("身份证上传成功");
      options.onSuccess?.(res, options.file);
    } else {
      throw new Error("未返回文件地址");
    }
  } catch (error: any) {
    console.error("身份证上传失败:", error);
    ElMessage.error(error?.message || "身份证上传失败，请重试");
    options.onError?.(error);
  }
};

// 提交审核资料
const handleSubmitAudit = async () => {
  if (!auditFormRef.value) return;

  await auditFormRef.value.validate(async (valid) => {
    if (valid) {
      // 防止 base64 被提交导致字段溢出
      if (auditForm.qualificationUrl.startsWith("data:")) {
        ElMessage.error("资格证书正在上传或未完成上传，请先完成上传。");
        return;
      }
      if (auditForm.idCardUrl && auditForm.idCardUrl.startsWith("data:")) {
        ElMessage.error("身份证图片正在上传或未完成上传，请先完成上传。");
        return;
      }

      auditLoading.value = true;
      try {
        await submitAudit({
          realName: auditForm.realName,
          qualificationUrl: auditForm.qualificationUrl,
          idCardUrl: auditForm.idCardUrl || undefined,
          title: auditForm.title,
          experienceYears: auditForm.experienceYears || undefined,
          specialty: auditForm.specialty,
          bio: auditForm.bio,
          location: auditForm.location,
          pricePerHour: auditForm.pricePerHour || undefined,
        });

        ElMessage.success({
          message:
            "资质提交成功！请耐心等待管理员审核，审核结果将通过通知告知您。",
          duration: 5000,
        });

        showAuditDialog.value = false;
        userStore.logout(); // 清除token，引导用户登录
        router.push("/login");
      } catch (error: any) {
        console.error("提交审核失败:", error);
        ElMessage.error(
          error.response?.data?.message || "提交审核失败，请稍后重试"
        );
      } finally {
        auditLoading.value = false;
      }
    }
  });
};

// 跳过审核（稍后提交）
const handleSkipAudit = () => {
  ElMessage.warning({
    message: "请尽快登录并提交资质证明！",
    duration: 5000,
  });
  showAuditDialog.value = false;
  userStore.logout(); // 清除token
  router.push("/login");
};
</script>

<style scoped>
/* 注册容器 - 使用CSS变量 */
.register-container {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--ease-bg);
  padding: 20px;
}

/* 注册卡片 - glass-card样式已在shared-styles中定义 */
.register-card {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 420px;
  padding: 48px 40px;
  border-radius: 2rem;
}

/* 标题区域 */
.register-header {
  text-align: center;
  margin-bottom: 40px;
}

.register-header h1 {
  font-size: 32px;
  font-weight: bold;
  color: var(--ease-dark);
  margin-bottom: 8px;
}

.register-header p {
  font-size: 14px;
  color: var(--gray-500);
}

/* 表单区域 */
.register-form {
  margin-bottom: 24px;
}

/* 页脚 */
.register-footer {
  text-align: center;
  font-size: 14px;
  color: var(--gray-500);
}

/* 角色提示 */
.role-tip {
  margin-bottom: 20px;
}

/* 角色选择样式 */
:deep(.el-radio-group) {
  display: flex;
  gap: 16px;
  width: 100%;
}

:deep(.el-radio) {
  flex: 1;
  margin-right: 0;
}

:deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--ease-dark);
}

.register-footer a {
  color: var(--ease-accent);
  text-decoration: none;
  margin-left: 8px;
  transition: color 0.3s ease;
}

.register-footer a:hover {
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

/* 审核对话框样式 */
.audit-alert {
  margin-bottom: 20px;
}

.audit-form {
  margin-top: 20px;
}

.upload-section {
  width: 100%;
}

.qualification-uploader,
.idcard-uploader {
  width: 100%;
}

.upload-preview {
  width: 100%;
  max-width: 400px;
  height: auto;
  border-radius: 8px;
  border: 1px solid var(--gray-200);
  cursor: pointer;
  transition: all 0.3s ease;
}

.upload-preview:hover {
  border-color: var(--ease-accent);
  box-shadow: 0 4px 12px rgba(94, 147, 117, 0.2);
}

.upload-placeholder {
  width: 100%;
  max-width: 400px;
  height: 200px;
  border: 2px dashed var(--gray-300);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background: var(--gray-50);
}

.upload-placeholder:hover {
  border-color: var(--ease-accent);
  background: rgba(94, 147, 117, 0.05);
}

.upload-placeholder i {
  font-size: 48px;
  color: var(--gray-400);
  margin-bottom: 12px;
}

.upload-text {
  font-size: 14px;
  color: var(--gray-500);
}

.upload-tip {
  margin-top: 8px;
  font-size: 12px;
  color: var(--gray-400);
  display: flex;
  align-items: center;
  gap: 4px;
}

.upload-tip i {
  font-size: 12px;
}

/* ⚠️ 【临时测试】URL输入框样式（后期删除） */
.temp-url-input {
  margin-top: 12px;
  border: 2px dashed #ff9800;
  border-radius: 8px;
}

.temp-url-input :deep(.el-input-group__prepend) {
  background-color: #fff3e0;
  color: #f57c00;
  font-weight: 600;
  border-color: #ff9800;
}

.hint-text {
  margin-left: 8px;
  color: #888;
  font-size: 13px;
}
</style>
