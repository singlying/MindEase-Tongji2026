<template>
  <div class="staff-canvas">
    <section class="panel staff-hero admin-hero">
      <div>
        <span class="section-kicker">管理员控制台</span>
        <h2>审核、量表与题目配置</h2>
        <p class="muted">集中处理咨询师资质审核，维护心理测评量表与题目内容。</p>
      </div>
      <button class="btn" @click="load"><span v-if="loading" class="spinner"></span>刷新审核</button>
    </section>

    <section class="metric-grid">
      <article v-for="metric in metrics" :key="metric.label" class="metric-card">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
        <small>{{ metric.caption }}</small>
      </article>
    </section>

    <section class="staff-layout admin-layout">
      <section class="panel audit-board">
        <div class="row-between">
          <div><span class="section-kicker">咨询师审核</span><h2>待审核队列</h2></div>
          <span class="chip">{{ audits.length }} 条</span>
        </div>
        <div class="appointment-list">
          <article v-for="item in audits" :key="item.auditId || item.id || item.userId" class="audit-card">
            <div class="timeline-emoji">资</div>
            <div>
              <div class="row-between">
                <strong>{{ item.realName || item.nickname || item.username || "咨询师" }}</strong>
                <span class="chip">{{ item.title || "待审核" }}</span>
              </div>
              <p class="muted">{{ item.specialty || item.certification || item.reason || "资料已提交，等待管理员处理。" }}</p>
            </div>
            <div class="row-between audit-actions">
              <button class="btn" @click="process(item, true)">通过</button>
              <button class="btn danger" @click="process(item, false)">拒绝</button>
            </div>
          </article>
          <div v-if="!audits.length" class="empty-state">暂无待审核资料</div>
        </div>
      </section>

      <div class="admin-tools">
        <form class="form-card stack" @submit.prevent="createScale">
          <div><span class="section-kicker">量表管理</span><h2>新增 / 更新量表</h2></div>
          <input v-model="scale.scaleKey" class="input" placeholder="量表 Key，例如 GAD7" />
          <input v-model="scale.title" class="input" placeholder="量表标题" />
          <input v-model="scale.coverUrl" class="input" placeholder="封面 URL（可选）" />
          <textarea v-model="scale.description" class="textarea" placeholder="量表说明"></textarea>
          <button class="btn" :disabled="savingScale"><span v-if="savingScale" class="spinner"></span>保存量表</button>
        </form>

        <form class="form-card stack" @submit.prevent="createQuestion">
          <div><span class="section-kicker">题目管理</span><h2>保存题目</h2></div>
          <input v-model="question.scaleKey" class="input" placeholder="归属量表 Key" />
          <textarea v-model="question.questionText" class="textarea" placeholder="题目内容"></textarea>
          <input v-model="question.options" class="input" placeholder="选项：没有=0,几天=1,一半以上=2,几乎每天=3" />
          <button class="btn secondary" :disabled="savingQuestion"><span v-if="savingQuestion" class="spinner"></span>保存题目</button>
        </form>
        <p v-if="message" class="feedback-note" :class="{ 'danger-note': failed }">{{ message }}</p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { adminApi } from "@/api";

const audits = ref<any[]>([]);
const message = ref("");
const loading = ref(false);
const savingScale = ref(false);
const savingQuestion = ref(false);
const failed = ref(false);
const scale = reactive({ scaleKey: "", title: "", coverUrl: "", description: "", status: "ACTIVE" });
const question = reactive({ scaleKey: "", questionText: "", options: "没有=0,几天=1,一半以上=2,几乎每天=3" });
const metrics = computed(() => [
  { label: "待审核", value: audits.value.length, caption: "咨询师资料" },
  { label: "审核操作", value: "PASS", caption: "通过/拒绝" },
  { label: "量表状态", value: scale.status, caption: "默认启用" },
  { label: "题目模板", value: 4, caption: "个选项" }
]);

const load = async () => {
  loading.value = true;
  try {
    const response = await adminApi.auditList({ page: 1, pageSize: 20 });
    audits.value = response.data.list || response.data.records || response.data.audits || [];
  } finally {
    loading.value = false;
  }
};

const process = async (item: any, approved: boolean) => {
  await adminApi.processAudit({
    auditId: item.auditId || item.id,
    action: approved ? "PASS" : "REJECT",
    remark: approved ? "资料完整，审核通过" : "资料信息不完整，请补充后重新提交"
  });
  await load();
};

const createScale = async () => {
  savingScale.value = true;
  failed.value = false;
  try {
    const response = await adminApi.createScale({
      ...scale,
      scoringRules: [
        { min: 0, max: 4, level: "低风险", desc: "保持当前节奏" },
        { min: 5, max: 9, level: "中风险", desc: "建议持续观察" },
        { min: 10, max: 21, level: "高风险", desc: "建议预约咨询" }
      ]
    });
    message.value = response.message || "量表已保存";
  } catch (err) {
    failed.value = true;
    message.value = err instanceof Error ? err.message : "量表保存失败";
  } finally {
    savingScale.value = false;
  }
};

const createQuestion = async () => {
  savingQuestion.value = true;
  failed.value = false;
  try {
    const options = question.options.split(",").map((item) => {
      const [label, score] = item.split("=").map((value) => value.trim());
      return { label, score: Number(score) };
    });
    const response = await adminApi.createQuestion({
      scaleKey: question.scaleKey,
      questions: [{ questionText: question.questionText, sortOrder: 1, options, deleted: false }]
    });
    message.value = response.message || "题目已保存";
  } catch (err) {
    failed.value = true;
    message.value = err instanceof Error ? err.message : "题目保存失败";
  } finally {
    savingQuestion.value = false;
  }
};

onMounted(load);
</script>
