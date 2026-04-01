<!-- 前端B负责：咨询师工作台 -->
<script setup lang="ts">
import PlaceholderPage from "@/components/common/PlaceholderPage.vue";

const actions = [{ label: "去个人中心", to: "/profile" }];
</script>

<template>
  <PlaceholderPage title="咨询师工作台" owner="B" :actions="actions" />
</template>
