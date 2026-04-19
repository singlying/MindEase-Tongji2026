<!-- 前端B负责：咨询师待审核页 -->
<script setup lang="ts">
import PlaceholderPage from "@/components/common/PlaceholderPage.vue";

const actions = [{ label: "去个人中心", to: "/profile" }];
</script>

<template>
  <PlaceholderPage title="资质审核中" owner="B" :actions="actions" />
</template>
