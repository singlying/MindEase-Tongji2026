<template>
  <div class="grid">
    <section class="panel span-7">
      <div class="row-between">
        <h2>测评量表</h2>
        <button class="btn secondary" @click="load">刷新</button>
      </div>
      <div class="list">
        <article v-for="scale in scales" :key="scale.scaleKey || scale.key || scale.id" class="list-row">
          <div class="row-between">
            <div class="row-between" style="justify-content: flex-start">
              <img class="cover-img" :src="scale.coverUrl || fallbackCover" :alt="scale.title || scale.name || scale.scaleName" @error="replaceCover" />
              <div>
              <strong>{{ scale.title || scale.name || scale.scaleName }}</strong>
              <p class="muted">{{ scale.description || "按后端返回题目开始作答" }}</p>
              </div>
            </div>
            <button class="btn" @click="start(scale.scaleKey || scale.key)">进入</button>
          </div>
        </article>
      </div>
    </section>
    <section class="panel span-5">
      <h2>历史记录</h2>
      <div class="list">
        <article v-for="record in records" :key="record.id || record.recordId" class="list-row">
          <strong>{{ record.scaleName || record.scaleKey || "测评记录" }}</strong>
          <p class="muted">{{ record.resultLevel || record.level || record.createTime }}</p>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { assessmentApi } from "@/api";

const scales = ref<any[]>([]);
const records = ref<any[]>([]);
const fallbackCover = "https://images.unsplash.com/photo-1493836512294-502baa1986e2?auto=format&fit=crop&w=320&q=70";
const replaceCover = (event: Event) => {
  (event.target as HTMLImageElement).src = fallbackCover;
};

const load = async () => {
  const scaleResponse = await assessmentApi.scales();
  scales.value = scaleResponse.data.scales || scaleResponse.data.list || scaleResponse.data || [];
  const recordResponse = await assessmentApi.records({ limit: 8, offset: 0 }).catch(() => ({ data: { records: [] } }));
  records.value = recordResponse.data.records || recordResponse.data.list || [];
};

const start = async (scaleKey: string) => {
  if (!scaleKey) return;
  await assessmentApi.scale(scaleKey);
  window.location.hash = `#${scaleKey}`;
};

onMounted(load);
</script>
