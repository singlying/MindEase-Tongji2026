<!-- 前端A负责：咨询师推荐页 -->
<script setup lang="ts">
import PlaceholderPage from "@/components/common/PlaceholderPage.vue";

const actions = [
  { label: "去预约页", to: "/booking/demo-counselor" },
  { label: "查看我的预约", to: "/my-appointments" },
];
</script>

<template>
  <PlaceholderPage title="咨询师推荐" owner="A" :actions="actions" />
</template>
