<template>
  <div class="audit-pending page-container">
    <div class="pending-header">
      <div class="status-icon">
        <i class="fas fa-clock"></i>
      </div>
      <h1 class="page-title">资质审核中</h1>
      <p class="page-subtitle">
        您好，{{ userStore.userInfo?.nickname }}，欢迎加入MindEase！
      </p>
    </div>

    <!-- 审核状态卡片 -->
    <div class="status-card glass-panel">
      <div v-if="auditStatus" class="audit-info">
        <div class="info-row">
          <span class="label">
            <i class="fas fa-file-alt"></i>
            审核状态
          </span>
          <span :class="['value', 'status-' + auditStatus.latestStatus]">
            {{ getStatusText(auditStatus.latestStatus) }}
          </span>
        </div>
        <div v-if="auditStatus.submitTime" class="info-row">
          <span class="label">
            <i class="fas fa-calendar"></i>
            提交时间
          </span>
          <span class="value">
            {{ formatTime(auditStatus.submitTime) }}
          </span>
        </div>
        <div v-if="auditStatus.auditRemark" class="info-row">
          <span class="label">
            <i class="fas fa-comment"></i>
            审核备注
          </span>
          <span class="value remark">{{ auditStatus.auditRemark }}</span>
        </div>
      </div>

      <div v-else class="no-audit">
        <i class="fas fa-exclamation-circle"></i>
        <p>您还未提交资质审核申请</p>
        <p class="hint">请点击下方按钮提交您的职业资质证明</p>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="action-buttons">
      <button
        v-if="!auditStatus || auditStatus.latestStatus === 'REJECTED'"
        class="btn-primary"
        @click="showSubmitDialog = true"
      >
        <i class="fas fa-upload"></i>
        {{ auditStatus ? "重新提交审核" : "提交审核申请" }}
      </button>
      <button class="btn-secondary" @click="handleRefresh">
        <i class="fas fa-sync-alt"></i>
        刷新状态
      </button>
      <button class="btn-secondary" @click="handleLogout">
        <i class="fas fa-sign-out-alt"></i>
        退出登录
      </button>
    </div>

    <!-- 温馨提示 -->
    <div class="tips glass-panel">
      <h3>
        <i class="fas fa-lightbulb"></i>
        温馨提示
      </h3>
      <ul>
        <li>
          <i class="fas fa-check"></i>
          请确保上传的资格证书清晰可辨，格式为JPG或PNG
        </li>
        <li>
          <i class="fas fa-check"></i>
          审核通常在1-3个工作日内完成，请耐心等待
        </li>
        <li>
          <i class="fas fa-check"></i>
          审核结果将通过系统通知告知，请留意消息
        </li>
        <li>
          <i class="fas fa-check"></i>
          如有疑问，请联系管理员或查看帮助文档
        </li>
      </ul>
    </div>

    <!-- 提交审核对话框 -->
    <el-dialog
      v-model="showSubmitDialog"
      title="提交资质证明"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="auditFormRef"
        :model="auditForm"
        :rules="auditRules"
        label-width="100px"
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
        <el-button @click="showSubmitDialog = false" :disabled="submitLoading">
          取消
        </el-button>
        <el-button
          type="primary"
          @click="handleSubmitAudit"
          :loading="submitLoading"
        >
          提交审核
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
} from "element-plus";
import { submitAudit, getAuditStatus } from "@/api/counselor";
import request from "@/api/request";
import type { AuditStatus } from "@/api/counselor";

const router = useRouter();
const userStore = useUserStore();

const showSubmitDialog = ref(false);
const submitLoading = ref(false);
const auditStatus = ref<AuditStatus | null>(null);
const auditFormRef = ref<FormInstance>();

// 专长选项（按后端示例）
const specialtyOptions = [
  "焦虑",
  "抑郁",
  "失眠",
  "职场压力",
  "家庭关系",
  "情绪管理",
  "学业压力",
  "婚恋关系",
];

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

// 获取审核状态
const fetchAuditStatus = async () => {
  try {
    const res = await getAuditStatus();
    // 后端返回 ApiResponse，直接取 data；若返回层级有调整，做兼容
    auditStatus.value = (res as any)?.data ?? (res as any);
  } catch (error: any) {
    // 如果是“暂无审核记录”或404，不显示错误，只设置为null
    const errorMsg = error.message || error.response?.data?.message || "";
    if (errorMsg.includes("暂无审核记录") || error.response?.status === 404) {
      auditStatus.value = null;
      console.log("[审核状态] 暂无审核记录，请提交资质审核");
    } else {
      console.error("获取审核状态失败:", error);
    }
  }
};

// 预览用，避免将 base64 写入提交字段
const qualificationPreview = ref("");
const idCardPreview = ref("");

// 处理资格证书上传
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

// 处理身份证上传
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

// 提交审核
const handleSubmitAudit = async () => {
  if (!auditFormRef.value) return;

  await auditFormRef.value.validate(async (valid) => {
    if (valid) {
      // 防止把 base64 数据提交到后端，导致 DB 字段溢出
      if (auditForm.qualificationUrl.startsWith("data:")) {
        ElMessage.error("资格证书正在上传或尚未上传成功，请先完成上传。");
        return;
      }
      if (auditForm.idCardUrl && auditForm.idCardUrl.startsWith("data:")) {
        ElMessage.error("身份证图片正在上传或尚未上传成功，请先完成上传。");
        return;
      }

      submitLoading.value = true;
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
          message: "资质提交成功！请耐心等待管理员审核。",
          duration: 5000,
        });

        showSubmitDialog.value = false;
        // 重新获取审核状态；若后端异步处理，先本地置为审核中以避免“未提交”提示
        auditStatus.value = {
          latestStatus: "PENDING",
          auditRemark: "",
          submitTime: new Date().toISOString(),
        } as AuditStatus;
        await fetchAuditStatus();
      } catch (error: any) {
        console.error("提交审核失败:", error);
        ElMessage.error(
          error.response?.data?.message || "提交审核失败，请稍后重试"
        );
      } finally {
        submitLoading.value = false;
      }
    }
  });
};

// 刷新状态
const handleRefresh = async () => {
  await fetchAuditStatus();
  ElMessage.success("已刷新审核状态");
};

// 退出登录
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm("确定要退出登录吗？", "提示", {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning",
    });

    userStore.logout();
    ElMessage.success("已退出登录");
    router.push("/login");
  } catch {
    // 用户取消
  }
};

// 获取状态文本
const getStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    PENDING: "审核中",
    APPROVED: "已通过",
    REJECTED: "已拒绝",
  };
  return statusMap[status] || "未知状态";
};

// 格式化时间
const formatTime = (time: string) => {
  const date = new Date(time);
  return date.toLocaleString("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
};

onMounted(() => {
  fetchAuditStatus();
});
</script>

<style scoped>
.audit-pending {
  min-height: 100vh;
  padding: var(--spacing-2xl);
  max-width: 900px;
  margin: 0 auto;
}

.pending-header {
  text-align: center;
  margin-bottom: var(--spacing-2xl);
}

.status-icon {
  width: 100px;
  height: 100px;
  margin: 0 auto var(--spacing-lg);
  background: linear-gradient(135deg, var(--ease-accent), var(--ease-primary));
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: pulse 2s ease-in-out infinite;
}

.status-icon i {
  font-size: 3rem;
  color: white;
}

@keyframes pulse {
  0%,
  100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.05);
    opacity: 0.8;
  }
}

.page-title {
  font-size: var(--font-3xl);
  color: var(--ease-dark);
  margin-bottom: var(--spacing-sm);
}

.page-subtitle {
  font-size: var(--font-lg);
  color: var(--gray-600);
}

/* 状态卡片 */
.status-card {
  padding: var(--spacing-2xl);
  margin-bottom: var(--spacing-xl);
}

.audit-info .info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-md) 0;
  border-bottom: 1px solid var(--gray-200);
}

.audit-info .info-row:last-child {
  border-bottom: none;
}

.info-row .label {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-weight: 500;
  color: var(--gray-700);
}

.info-row .label i {
  color: var(--ease-accent);
  width: 20px;
}

.info-row .value {
  font-weight: 600;
  color: var(--ease-dark);
}

.info-row .value.remark {
  font-weight: normal;
  max-width: 60%;
  text-align: right;
}

.value.status-PENDING {
  color: #ff9800;
}

.value.status-APPROVED {
  color: #4caf50;
}

.value.status-REJECTED {
  color: #f44336;
}

.no-audit {
  text-align: center;
  padding: var(--spacing-2xl);
  color: var(--gray-600);
}

.no-audit i {
  font-size: 4rem;
  color: var(--ease-accent);
  margin-bottom: var(--spacing-md);
  opacity: 0.5;
}

.no-audit p {
  font-size: var(--font-lg);
  margin-bottom: var(--spacing-sm);
}

.no-audit .hint {
  font-size: var(--font-base);
  color: var(--gray-500);
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  justify-content: center;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-xl);
  flex-wrap: wrap;
}

.action-buttons button {
  min-width: 150px;
}

.action-buttons i {
  margin-right: var(--spacing-sm);
}

/* 温馨提示 */
.tips {
  padding: var(--spacing-xl);
  background: rgba(94, 147, 117, 0.05);
}

.tips h3 {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: var(--font-xl);
  color: var(--ease-dark);
  margin-bottom: var(--spacing-md);
}

.tips h3 i {
  color: var(--ease-accent);
}

.tips ul {
  list-style: none;
  padding: 0;
}

.tips li {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) 0;
  color: var(--gray-700);
  font-size: var(--font-base);
}

.tips li i {
  color: var(--ease-accent);
  margin-top: 4px;
  font-size: var(--font-sm);
}

/* 上传相关样式 */
.upload-section {
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
