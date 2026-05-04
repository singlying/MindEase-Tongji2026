<template>
  <div class="grid">
    <section class="metric span-3"><span class="muted">报告周期</span><strong>{{ report.period || "-" }}</strong></section>
    <section class="metric span-3"><span class="muted">平均情绪</span><strong>{{ Number(report.avgScore || 0).toFixed(1) }}</strong></section>
    <section class="metric span-3"><span class="muted">积极占比</span><strong>{{ Math.round((report.positiveRate || 0) * 100) }}%</strong></section>
    <section class="metric span-3"><span class="muted">连续记录</span><strong>{{ report.continuousDays || 0 }}</strong></section>
    <section class="panel span-12">
      <h2>AI 建议</h2>
      <div class="list">
        <article v-for="(item, index) in suggestions" :key="index" class="list-row">{{ item }}</article>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { reportApi } from "@/api";

const report = ref<any>({});
const suggestions = computed(() => report.value.aiSuggestions || [report.value.summary || report.value.aiSummary || "暂无报告数据"]);
onMounted(async () => {
  report.value = (await reportApi.emotion().catch(() => ({ data: {} }))).data;
});
</script>
