<!-- 前端A负责：情绪报告页 -->
<script setup lang="ts">
import PlaceholderPage from "@/components/common/PlaceholderPage.vue";

const actions = [
  { label: "返回首页", to: "/home" },
  { label: "去个人中心", to: "/profile" },
];
</script>

<template>
  <PlaceholderPage title="情绪报告" owner="A" :actions="actions" />
</template>
