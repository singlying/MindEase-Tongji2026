<!-- 前端A负责：冥想时刻页 -->
<script setup lang="ts">
import PlaceholderPage from "@/components/common/PlaceholderPage.vue";

const actions = [
  { label: "返回首页", to: "/home" },
  { label: "去 AI 咨询", to: "/ai-chat" },
];
</script>

<template>
  <PlaceholderPage title="冥想时刻" owner="A" :actions="actions" />
</template>
