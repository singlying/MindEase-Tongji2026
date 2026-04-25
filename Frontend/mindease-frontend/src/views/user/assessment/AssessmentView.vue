<!-- 前端A负责：心理测评列表 -->
<script setup lang="ts">
import PlaceholderPage from "@/components/common/PlaceholderPage.vue";

const actions = [
  { label: "进入示例量表", to: "/assessment/gad-7" },
  { label: "查看测评历史", to: "/assessment/history" },
];
</script>

<template>
  <PlaceholderPage title="心理测评" owner="A" :actions="actions" />
</template>
