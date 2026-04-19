<!-- 前端A负责：用户首页 -->
<script setup lang="ts">
import PlaceholderPage from "@/components/common/PlaceholderPage.vue";

const actions = [
  { label: "去情绪日记", to: "/mood-diary" },
  { label: "去 AI 咨询", to: "/ai-chat" },
  { label: "去心理测评", to: "/assessment" },
];
</script>

<template>
  <PlaceholderPage title="用户首页" owner="A" :actions="actions" />
</template>
