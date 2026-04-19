<!-- 前端A负责：情绪日记列表/编辑入口 -->
<script setup lang="ts">
import PlaceholderPage from "@/components/common/PlaceholderPage.vue";

const actions = [
  { label: "查看示例详情", to: "/mood-diary/demo-entry" },
  { label: "返回首页", to: "/home" },
];
</script>

<template>
  <PlaceholderPage title="情绪日记" owner="A" :actions="actions" />
</template>
