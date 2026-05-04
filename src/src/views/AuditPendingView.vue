<template>
  <div class="grid">
    <form class="form-card span-6 stack" @submit.prevent="submit">
      <h2>提交咨询师资质</h2>
      <input v-model="form.realName" class="input" placeholder="真实姓名" />
      <input v-model="form.title" class="input" placeholder="职称" />
      <input v-model.number="form.experienceYears" class="input" type="number" placeholder="从业年限" />
      <input v-model="form.qualificationUrl" class="input" placeholder="资质证明 URL" />
      <textarea v-model="form.bio" class="textarea" placeholder="个人简介"></textarea>
      <button class="btn">提交审核</button>
      <p class="muted">{{ message }}</p>
    </form>
    <section class="panel span-6">
      <h2>审核状态</h2>
      <pre class="muted">{{ status }}</pre>
      <button class="btn secondary" @click="load">刷新状态</button>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { counselorApi } from "@/api";

const status = ref<any>({});
const message = ref("");
const form = reactive({ realName: "", title: "", experienceYears: 0, qualificationUrl: "", bio: "" });

const load = async () => {
  status.value = (await counselorApi.auditStatus().catch(() => ({ data: {} }))).data;
};

const submit = async () => {
  const response = await counselorApi.submitAudit(form);
  message.value = response.message || "已提交";
  await load();
};

onMounted(load);
</script>
