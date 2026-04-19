<!-- 前端A负责：我的预约页 -->
<script setup lang="ts">
import PlaceholderPage from "@/components/common/PlaceholderPage.vue";

const actions = [
  { label: "继续预约", to: "/counselor-list" },
  { label: "返回首页", to: "/home" },
];
</script>

<template>
  <PlaceholderPage title="我的预约" owner="A" :actions="actions" />
</template>
