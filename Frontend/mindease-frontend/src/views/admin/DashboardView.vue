<template>
  <div class="admin-dashboard">
    <div class="dashboard-header">
      <h1 class="page-title">管理员工作台</h1>
      <p class="page-subtitle">平台运营管理中心</p>
    </div>

    <div class="main-card glass-card">
      <el-tabs v-model="activeTab" class="custom-tabs">
        <el-tab-pane label="咨询师资质审核" name="audit">
          <div class="tab-content">
            <div class="action-bar">
              <h3 class="section-title">
                <el-icon class="icon"><UserFilled /></el-icon> 待审核列表
              </h3>
              <el-button
                @click="loadAuditList"
                :loading="auditLoading"
                circle
                plain
                type="primary"
              >
                <el-icon><Refresh /></el-icon>
              </el-button>
            </div>

            <el-table
              :data="auditList"
              v-loading="auditLoading"
              style="width: 100%"
              :header-cell-style="{ background: '#f9fafb', color: '#4b5563' }"
            >
              <el-table-column prop="realName" label="真实姓名" width="140">
                <template #default="scope">
                  <span class="font-medium text-gray-900">{{
                    scope.row.realName
                  }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="username" label="用户名" width="140" />
              <el-table-column prop="title" label="申请职称" width="200" />
              <el-table-column prop="submitTime" label="提交时间">
                <template #default="scope">
                  <span class="text-gray-500">{{
                    formatDateTime(scope.row.submitTime)
                  }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" fixed="right" width="240">
                <template #default="scope">
                  <el-button
                    type="primary"
                    size="small"
                    plain
                    @click="handleView(scope.row)"
                    >详情</el-button
                  >
                  <el-button
                    type="success"
                    size="small"
                    plain
                    @click="handleProcess(scope.row, 'PASS')"
                    >通过</el-button
                  >
                  <el-button
                    type="danger"
                    size="small"
                    plain
                    @click="openRejectDialog(scope.row)"
                    >拒绝</el-button
                  >
                </template>
              </el-table-column>
            </el-table>

            <div class="pagination-wrapper">
              <el-pagination
                layout="prev, pager, next"
                :total="auditTotal"
                @current-change="handleAuditPageChange"
                background
              />
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="心理量表管理" name="scale">
          <div class="tab-content">
            <div class="scale-toolbar">
              <div class="select-wrapper">
                <span class="label">当前编辑：</span>
                <el-select
                  v-model="currentScaleKey"
                  placeholder="请选择量表"
                  @change="handleScaleChange"
                  size="large"
                  style="width: 260px"
                >
                  <el-option
                    v-for="item in scaleOptions"
                    :key="item.scaleKey"
                    :label="item.title"
                    :value="item.scaleKey"
                  />
                  <el-option label="+ 新建量表" value="__new" />
                </el-select>
              </div>
              <div class="btn-group">
                <el-button
                  type="primary"
                  @click="handleSaveScaleConfig"
                  color="#7b9e89"
                  >保存配置</el-button
                >
                <el-button type="success" @click="handleSaveQuestions" plain
                  >保存题目</el-button
                >
              </div>
            </div>

            <div v-if="currentScaleKey" class="scale-editor-grid">
              <div class="config-panel">
                <h4 class="panel-title">基础信息</h4>
                <el-form :model="scaleConfig" label-position="top">
                  <el-form-item label="量表标识 (Key)">
                    <el-input
                      v-model="scaleConfig.scaleKey"
                      disabled
                      class="custom-input"
                    />
                  </el-form-item>
                  <el-form-item label="量表标题">
                    <el-input
                      v-model="scaleConfig.title"
                      placeholder="例如：GAD-7 焦虑症筛查"
                      class="custom-input"
                    />
                  </el-form-item>
                  <el-form-item label="封面图片">
                    <div class="cover-upload">
                      <div
                        class="cover-preview"
                        v-if="coverPreview || scaleConfig.coverUrl"
                      >
                        <img
                          :src="coverPreview || scaleConfig.coverUrl"
                          alt="封面"
                        />
                      </div>
                      <el-upload
                        class="cover-uploader"
                        action=""
                        :auto-upload="true"
                        :show-file-list="false"
                        accept="image/jpeg,image/png,image/jpg"
                        :before-upload="beforeUploadImage"
                        :http-request="handleCoverUpload"
                        :on-change="handleCoverChange"
                      >
                        <el-button type="primary" plain>选择图片</el-button>
                      </el-upload>
                      <p class="upload-tip">
                        支持 JPG/PNG，建议 16:9，&lt; 5MB
                      </p>
                    </div>
                  </el-form-item>
                  <el-form-item label="描述说明">
                    <el-input
                      v-model="scaleConfig.description"
                      type="textarea"
                      :rows="6"
                      placeholder="请输入量表的引导语或说明"
                      class="custom-textarea"
                    />
                  </el-form-item>

                  <el-divider content-position="left"
                    >评分规则设置（根据总分给出结果）</el-divider
                  >
                  <div class="scoring-panel">
                    <div class="scoring-header">
                      <span class="scoring-col">分数下限</span>
                      <span class="scoring-col">分数上限</span>
                      <span class="scoring-col">结果等级</span>
                      <span class="scoring-col flex-1">解释说明</span>
                      <el-button
                        type="primary"
                        text
                        size="small"
                        @click="addScoringRule"
                      >
                        + 新增规则
                      </el-button>
                    </div>
                    <div
                      v-if="!scaleConfig.scoringRules.length"
                      class="scoring-empty"
                    >
                      当前暂无评分规则，请点击「新增规则」按接口文档配置
                      <span class="hint"
                        >例如：0-4分「没有焦虑」、5-9分「轻度焦虑」等。</span
                      >
                    </div>
                    <div
                      v-for="(rule, idx) in scaleConfig.scoringRules"
                      :key="idx"
                      class="scoring-row"
                    >
                      <el-input-number
                        v-model="rule.min"
                        :min="0"
                        :max="999"
                        controls-position="right"
                        size="small"
                      />
                      <span class="dash">-</span>
                      <el-input-number
                        v-model="rule.max"
                        :min="0"
                        :max="999"
                        controls-position="right"
                        size="small"
                      />
                      <el-input
                        v-model="rule.level"
                        placeholder="如：轻度焦虑"
                        size="small"
                        class="scoring-level-input"
                      />
                      <el-input
                        v-model="rule.desc"
                        placeholder="用于展示给用户的文字说明"
                        size="small"
                        type="textarea"
                        :rows="2"
                      />
                      <el-button
                        type="danger"
                        text
                        size="small"
                        @click="removeScoringRule(idx)"
                        >删除</el-button
                      >
                    </div>
                  </div>
                </el-form>
              </div>

              <div class="questions-panel">
                <div class="panel-header">
                  <h4 class="panel-title">题目列表 ({{ questions.length }})</h4>
                  <el-button
                    size="small"
                    @click="addQuestion"
                    icon="Plus"
                    circle
                  ></el-button>
                </div>

                <el-collapse class="custom-collapse">
                  <el-collapse-item
                    v-for="(q, index) in questions"
                    :key="index"
                    :name="index"
                  >
                    <template #title>
                      <div class="question-header">
                        <span class="q-index">Q{{ index + 1 }}</span>
                        <span class="q-text-preview">{{
                          q.questionText || "请输入题干..."
                        }}</span>
                      </div>
                    </template>
                    <div class="question-body">
                      <el-form label-width="60px">
                        <el-form-item label="题干">
                          <el-input
                            v-model="q.questionText"
                            placeholder="请输入题目内容"
                          />
                        </el-form-item>
                        <div class="options-list">
                          <p class="sub-label">选项配置：</p>
                          <div
                            v-for="(opt, optIdx) in q.options"
                            :key="optIdx"
                            class="option-row"
                          >
                            <el-input
                              v-model="opt.label"
                              placeholder="选项文案"
                              size="small"
                            />
                            <el-input-number
                              v-model="opt.score"
                              :min="0"
                              size="small"
                              controls-position="right"
                              style="width: 80px"
                            />
                          </div>
                        </div>
                        <div class="action-footer">
                          <el-button
                            type="danger"
                            link
                            size="small"
                            @click="removeQuestion(index)"
                            >删除此题</el-button
                          >
                        </div>
                      </el-form>
                    </div>
                  </el-collapse-item>
                </el-collapse>
              </div>
            </div>

            <div v-else class="empty-state">
              <el-icon class="empty-icon"><Document /></el-icon>
              <p>请先从上方选择一个量表开始编辑</p>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog
      v-model="detailVisible"
      title="资质审核详情"
      width="700px"
      align-center
      class="custom-dialog"
    >
      <div v-if="currentItem" class="detail-content">
        <el-descriptions :column="2" border class="custom-desc">
          <el-descriptions-item label="真实姓名">{{
            currentItem.realName
          }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{
            currentItem.username
          }}</el-descriptions-item>
          <el-descriptions-item label="职称">{{
            currentItem.title
          }}</el-descriptions-item>
          <el-descriptions-item label="从业年限"
            >{{ currentItem.experienceYears }} 年</el-descriptions-item
          >
          <el-descriptions-item label="所在地区">{{
            currentItem.location
          }}</el-descriptions-item>
          <el-descriptions-item label="期望价格"
            >{{ currentItem.pricePerHour }} 元/小时</el-descriptions-item
          >
        </el-descriptions>

        <div class="info-block">
          <h4 class="block-title">个人简介</h4>
          <div class="text-box">{{ currentItem.bio }}</div>
        </div>

        <div class="info-block">
          <h4 class="block-title">专长领域</h4>
          <div class="tags-wrapper">
            <el-tag
              v-for="tag in formatSpecialty(currentItem.specialty)"
              :key="tag"
              type="info"
              effect="plain"
              >{{ tag }}</el-tag
            >
          </div>
        </div>

        <div class="images-grid">
          <div class="img-box">
            <h4 class="block-title">资格证书</h4>
            <el-image
              :src="currentItem.qualificationUrl"
              :preview-src-list="[currentItem.qualificationUrl]"
              class="cert-img"
              fit="cover"
            />
          </div>
          <div v-if="currentItem.idCardUrl" class="img-box">
            <h4 class="block-title">身份证</h4>
            <el-image
              :src="currentItem.idCardUrl"
              :preview-src-list="[currentItem.idCardUrl]"
              class="cert-img"
              fit="cover"
            />
          </div>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
          <el-button type="danger" @click="openRejectDialog(currentItem)"
            >拒绝</el-button
          >
          <el-button type="success" @click="handleProcess(currentItem, 'PASS')"
            >通过</el-button
          >
        </span>
      </template>
    </el-dialog>

    <el-dialog
      v-model="rejectVisible"
      title="拒绝审核"
      width="400px"
      align-center
    >
      <el-form>
        <el-form-item>
          <el-input
            v-model="rejectRemark"
            type="textarea"
            placeholder="请输入拒绝原因（例如：证书模糊、年限不符等）"
            :rows="4"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import {
  getAuditList,
  processAudit,
  saveScaleConfig,
  saveScaleQuestions,
  getScaleDetail,
  type AuditItem,
} from "@/api/admin";
import { getScaleList, type Scale } from "@/api/assessment";
import request from "@/api/request";
import { ElMessage, ElMessageBox } from "element-plus";
import { UserFilled, Refresh, Plus, Document } from "@element-plus/icons-vue";

const activeTab = ref("audit");

// --- 审核模块逻辑 ---
const auditLoading = ref(false);
const auditList = ref<AuditItem[]>([]);
const auditTotal = ref(0);
const auditPage = ref(1);
const detailVisible = ref(false);
const currentItem = ref<AuditItem | null>(null);
const rejectVisible = ref(false);
const rejectRemark = ref("");

onMounted(() => {
  loadAuditList();
  fetchScaleOptions();
});

const loadAuditList = async () => {
  auditLoading.value = true;
  try {
    const res = await getAuditList({ page: auditPage.value, pageSize: 20 });
    const data = res.data as any;
    auditList.value = data.list || [];
    auditTotal.value = data.total || 0;
  } catch (error) {
    ElMessage.error("获取列表失败");
  } finally {
    auditLoading.value = false;
  }
};

const handleAuditPageChange = (page: number) => {
  auditPage.value = page;
  loadAuditList();
};

const handleView = (row: AuditItem) => {
  currentItem.value = row;
  detailVisible.value = true;
};

const formatSpecialty = (val: string | string[] | undefined): string[] => {
  if (!val) return [];
  if (Array.isArray(val)) return val;
  try {
    return JSON.parse(val);
  } catch {
    return [val];
  }
};

const handleProcess = async (row: AuditItem | null, action: "PASS") => {
  if (!row) return;
  try {
    await ElMessageBox.confirm(
      `确定要通过 ${row.realName} 的审核吗？`,
      "提示",
      { type: "warning" }
    );
    await processAudit({ auditId: row.auditId, action, remark: "审核通过" });
    ElMessage.success("操作成功");
    detailVisible.value = false;
    loadAuditList();
  } catch {}
};

const openRejectDialog = (row: AuditItem | null) => {
  if (!row) return;
  currentItem.value = row;
  rejectRemark.value = "";
  rejectVisible.value = true;
};

const confirmReject = async () => {
  if (!rejectRemark.value) return ElMessage.warning("请填写原因");
  try {
    await processAudit({
      auditId: currentItem.value!.auditId,
      action: "REJECT",
      remark: rejectRemark.value,
    });
    ElMessage.success("已拒绝");
    rejectVisible.value = false;
    detailVisible.value = false;
    loadAuditList();
  } catch (e) {
    ElMessage.error("操作失败");
  }
};

const formatDateTime = (str: string) => {
  if (!str) return "";
  return new Date(str).toLocaleString("zh-CN");
};

// --- 量表管理逻辑 ---
const currentScaleKey = ref("");
const scaleOptions = ref<Scale[]>([]);

interface ScoringRule {
  min: number;
  max: number;
  level: string;
  desc: string;
}

const DEFAULT_SCORING_RULES: Record<string, ScoringRule[]> = {
  "gad-7": [
    {
      min: 0,
      max: 4,
      level: "没有焦虑",
      desc: "您的情绪状态良好，请继续保持。",
    },
    { min: 5, max: 9, level: "轻度焦虑", desc: "建议适当进行放松训练。" },
    { min: 10, max: 14, level: "中度焦虑", desc: "建议寻求心理咨询师帮助。" },
    { min: 15, max: 21, level: "重度焦虑", desc: "强烈建议前往医院就诊。" },
  ],
};

const scaleConfig = reactive<{
  id: number | null;
  scaleKey: string;
  title: string;
  coverUrl: string;
  description: string;
  status: string;
  scoringRules: ScoringRule[];
}>({
  id: null,
  scaleKey: "",
  title: "",
  coverUrl: "",
  description: "",
  status: "active",
  scoringRules: [],
});
const coverPreview = ref("");
const questions = ref<any[]>([]);

const fetchScaleOptions = async () => {
  try {
    const res = await getScaleList();
    const data = (res as any).data;
    scaleOptions.value = data.scales || [];
  } catch (e) {
    console.error("获取量表列表失败", e);
  }
};

const loadScaleData = async (key: string) => {
  // 1) 先清空数据，防止显示上一次的残留
  scaleConfig.id = null;
  scaleConfig.scaleKey = key;
  scaleConfig.title = "";
  scaleConfig.coverUrl = "";
  scaleConfig.description = "";
  scaleConfig.scoringRules = [];
  questions.value = [];

  try {
    // 2) 调用详情接口 (复用已有的 /assessment/scale/{key} 接口)
    const res = (await getScaleDetail(key)) as any;
    const data = res.data || {};

    // 3) 回显基础信息
    scaleConfig.scaleKey = data.scaleKey || key;
    scaleConfig.title = data.title || "";
    scaleConfig.coverUrl = data.coverUrl || "";
    scaleConfig.description = data.description || "";
    // 保存ID，这样后端保存时知道是更新而不是新建
    scaleConfig.id = data.id ?? null;

    // 4) 回显题目列表（兼容字段名，带上题目/选项信息，供“题目显示/编辑”使用）
    if (Array.isArray(data.questions)) {
      questions.value = data.questions.map((q: any) => ({
        id: q.id,
        questionText: q.questionText || q.text,
        sortOrder: q.sortOrder,
        options: q.options || [],
      }));
    } else {
      questions.value = [];
    }

    // 5) 回显评分规则（供“量表规则设置”使用）
    // 后端若返回 scoringRules 则优先使用；否则按量表Key套用默认规则（如 GAD-7）
    if (Array.isArray(data.scoringRules) && data.scoringRules.length) {
      scaleConfig.scoringRules = data.scoringRules;
    } else if (DEFAULT_SCORING_RULES[key]) {
      // 深拷贝，避免直接引用默认对象导致编辑污染
      scaleConfig.scoringRules = JSON.parse(
        JSON.stringify(DEFAULT_SCORING_RULES[key])
      );
    } else {
      scaleConfig.scoringRules = [];
    }
  } catch (error) {
    console.error("加载量表详情失败或量表不存在:", error);
    // 如果接口报错（可能还没创建该量表），则保留 key，让管理员可继续新建/编辑
    scaleConfig.id = null;
    scaleConfig.scaleKey = key;
    scaleConfig.title = "";
    scaleConfig.coverUrl = "";
    scaleConfig.description = "";
    scaleConfig.scoringRules = [];
    questions.value = [];
  }
};

const handleScaleChange = async (val: string) => {
  if (val === "__new") {
    await handleCreateNewScale();
    return;
  }
  currentScaleKey.value = val;
  await loadScaleData(val);
};

const handleSaveScaleConfig = async () => {
  try {
    await saveScaleConfig(scaleConfig);
    ElMessage.success("基础配置保存成功");
    // 保存成功后刷新量表下拉选项，确保新建或修改后的量表可以再次被选择编辑
    fetchScaleOptions();
  } catch (e) {
    ElMessage.error("保存失败");
  }
};

const handleSaveQuestions = async () => {
  try {
    await saveScaleQuestions({
      scaleKey: scaleConfig.scaleKey,
      questions: questions.value,
    });
    ElMessage.success("题目保存成功");
  } catch (e) {
    ElMessage.error("保存失败");
  }
};

const addQuestion = () => {
  questions.value.push({
    questionText: "",
    sortOrder: questions.value.length + 1,
    options: [
      { label: "完全没有", score: 0 },
      { label: "有几天", score: 1 },
      { label: "一半以上天数", score: 2 },
      { label: "几乎每天", score: 3 },
    ],
  });
};

const removeQuestion = (index: number) => {
  questions.value.splice(index, 1);
};

// ============ 量表封面上传 ============
const handleCoverChange = (file: any) => {
  // 仅用于本地预览，不写入实际 coverUrl，避免把 blob: 地址保存到数据库
  coverPreview.value = URL.createObjectURL(file.raw);
};

const beforeUploadImage = (file: File) => {
  const isImage = file.type.startsWith("image/");
  const isLt5M = file.size / 1024 / 1024 < 5;
  if (!isImage) ElMessage.error("请上传图片文件");
  if (!isLt5M) ElMessage.error("图片大小需小于 5MB");
  return isImage && isLt5M;
};

const handleCoverUpload = async (options: any) => {
  const formData = new FormData();
  formData.append("file", options.file);
  try {
    const res = await request.post("/upload", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    const url = (res as any)?.data?.data ?? (res as any)?.data;
    if (url) {
      scaleConfig.coverUrl = url;
      coverPreview.value = url;
      ElMessage.success("封面上传成功");
      options.onSuccess?.(res, options.file);
    } else {
      throw new Error("上传返回为空");
    }
  } catch (error) {
    console.error("封面上传失败", error);
    ElMessage.error("封面上传失败");
    options.onError?.(error);
  }
};

// ============ 新建量表 ============
const handleCreateNewScale = async () => {
  const newKey = `custom-${Date.now()}`;
  currentScaleKey.value = newKey;
  scaleConfig.id = null;
  scaleConfig.scaleKey = newKey;
  scaleConfig.title = "";
  scaleConfig.coverUrl = "";
  scaleConfig.description = "";
  scaleConfig.status = "active";
  scaleConfig.scoringRules = [];
  questions.value = [];

  try {
    await saveScaleConfig(scaleConfig);
    ElMessage.success("新量表已创建，可继续编辑后保存");
    // 新建成功后刷新量表列表
    fetchScaleOptions();
  } catch (e) {
    ElMessage.error("新建量表失败");
  }
};

// ============ 评分规则管理 ============
const addScoringRule = () => {
  scaleConfig.scoringRules.push({
    min: 0,
    max: 0,
    level: "",
    desc: "",
  });
};

const removeScoringRule = (index: number) => {
  scaleConfig.scoringRules.splice(index, 1);
};
</script>

<style scoped>
.admin-dashboard {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

.dashboard-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 28px;
  font-weight: bold;
  color: #2c3e50;
  margin-bottom: 8px;
}

.page-subtitle {
  color: #6b7280;
}

/* 玻璃卡片 */
.glass-card {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(24px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  padding: 24px;
  min-height: 80vh;
}

.tab-content {
  padding: 16px;
}

/* 审核模块 */
.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.section-title {
  font-size: 18px;
  font-weight: bold;
  color: #2c3e50;
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination-wrapper {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}

/* 量表管理模块 */
.scale-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.select-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}

.select-wrapper .label {
  font-weight: 500;
  color: #374151;
}

.scale-editor-grid {
  display: grid;
  grid-template-columns: 1fr 1.5fr;
  gap: 32px;
}

/* 配置面板 */
.config-panel,
.questions-panel {
  background: rgba(255, 255, 255, 0.5);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid rgba(255, 255, 255, 0.6);
}

.cover-upload {
  display: flex;
  align-items: center;
  gap: 16px;
}

.cover-preview {
  width: 180px;
  height: 100px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-tip {
  margin-top: 8px;
  color: #6b7280;
  font-size: 12px;
}

.scoring-panel {
  margin-top: 8px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px dashed rgba(148, 163, 184, 0.6);
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.scoring-header {
  display: grid;
  grid-template-columns: 110px 110px 150px 1fr auto;
  align-items: center;
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
}

.scoring-col {
  padding: 0 4px;
  white-space: nowrap; /* 防止表头文字被挤成竖排，保持与其他标签一致 */
}

.scoring-empty {
  font-size: 12px;
  color: #9ca3af;
  padding: 6px 4px 2px;
}

.scoring-empty .hint {
  margin-left: 4px;
}

.scoring-row {
  display: grid;
  grid-template-columns: 110px 16px 110px 150px 1fr auto;
  align-items: flex-start;
  gap: 4px;
}

.scoring-row .el-input-number {
  width: 100%;
}

.scoring-level-input {
  width: 100%;
}

.scoring-row .el-textarea {
  width: 100%;
}

.scoring-row .dash {
  text-align: center;
  color: #9ca3af;
}

.panel-title {
  font-size: 16px;
  font-weight: bold;
  color: #1f2937;
  margin-bottom: 20px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

/* 问题列表美化 */
.custom-collapse {
  border: none;
  --el-collapse-header-bg-color: transparent;
  --el-collapse-content-bg-color: transparent;
}

.question-header {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.q-index {
  background: #7b9e89;
  color: white;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: bold;
}

.q-text-preview {
  color: #4b5563;
  font-size: 14px;
}

.question-body {
  background: rgba(255, 255, 255, 0.4);
  padding: 16px;
  border-radius: 12px;
}

.options-list {
  background: rgba(255, 255, 255, 0.5);
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 12px;
}

.sub-label {
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 8px;
}

.option-row {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
}

.action-footer {
  text-align: right;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400px;
  color: #9ca3af;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

/* 详情弹窗美化 */
.info-block {
  margin-top: 24px;
}

.block-title {
  font-size: 14px;
  font-weight: bold;
  color: #374151;
  margin-bottom: 8px;
}

.text-box {
  background: #f9fafb;
  padding: 16px;
  border-radius: 8px;
  color: #4b5563;
  line-height: 1.6;
}

.tags-wrapper {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.images-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 24px;
}

.cert-img {
  width: 100%;
  height: 200px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
}
</style>
