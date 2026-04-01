<!-- 前端A负责：测评结果页 -->
<script setup lang="ts">
import PlaceholderPage from "@/components/common/PlaceholderPage.vue";

const actions = [
  { label: "查看测评历史", to: "/assessment/history" },
  { label: "返回测评首页", to: "/assessment" },
];
</script>

<template>
  <PlaceholderPage title="测评结果" owner="A" :actions="actions" />
</template>
