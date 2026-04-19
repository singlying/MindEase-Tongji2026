<!-- 前端A负责：测评历史页 -->
<script setup lang="ts">
import PlaceholderPage from "@/components/common/PlaceholderPage.vue";

const actions = [
  { label: "返回测评列表", to: "/assessment" },
  { label: "去情绪报告", to: "/emotion-report" },
];
</script>

<template>
  <PlaceholderPage title="测评历史" owner="A" :actions="actions" />
</template>
