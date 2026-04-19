<!-- 前端A负责：预约页 -->
<script setup lang="ts">
import PlaceholderPage from "@/components/common/PlaceholderPage.vue";

const actions = [
  { label: "返回咨询师列表", to: "/counselor-list" },
  { label: "查看我的预约", to: "/my-appointments" },
];
</script>

<template>
  <PlaceholderPage title="预约咨询" owner="A" :actions="actions" />
</template>
