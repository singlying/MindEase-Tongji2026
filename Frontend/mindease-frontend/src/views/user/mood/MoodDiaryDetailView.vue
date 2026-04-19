<!-- 前端A负责：情绪日记详情 -->
<script setup lang="ts">
import PlaceholderPage from "@/components/common/PlaceholderPage.vue";

const actions = [
  { label: "返回日记列表", to: "/mood-diary" },
  { label: "去情绪报告", to: "/emotion-report" },
];
</script>

<template>
  <PlaceholderPage title="日记详情" owner="A" :actions="actions" />
</template>
