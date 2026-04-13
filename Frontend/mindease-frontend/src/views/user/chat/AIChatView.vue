<!-- 前端A负责：AI 咨询页面 -->
<script setup lang="ts">
import PlaceholderPage from "@/components/common/PlaceholderPage.vue";

const actions = [
  { label: "返回首页", to: "/home" },
  { label: "去咨询师推荐", to: "/counselor-list" },
];
</script>

<template>
  <PlaceholderPage title="AI 咨询" owner="A" :actions="actions" />
</template>
