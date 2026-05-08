<!-- 前端A负责：测评答题页 -->
<script setup lang="ts">
import PlaceholderPage from "@/components/common/PlaceholderPage.vue";

const actions = [
  { label: "查看示例结果", to: "/assessment/result/demo-result" },
  { label: "返回测评列表", to: "/assessment" },
];
</script>

<template>
  <PlaceholderPage title="测评答题" owner="A" :actions="actions" />
</template>
