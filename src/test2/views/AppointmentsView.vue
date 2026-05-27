<template>
  <section class="panel">
    <div class="row-between">
      <h2>我的预约</h2>
      <select v-model="status" class="select" style="max-width: 180px" @change="load">
        <option value="">全部</option>
        <option value="pending">待确认</option>
        <option value="confirmed">已确认</option>
        <option value="cancelled">已取消</option>
      </select>
    </div>
    <div class="list">
      <article v-for="item in list" :key="item.id" class="list-row">
        <div class="row-between">
          <div>
            <strong>{{ item.targetName || item.counselorName }}</strong>
            <p class="muted">{{ item.startTime }} - {{ item.endTime }} · {{ item.status }}</p>
          </div>
          <button class="btn danger" @click="cancel(item.id)">取消</button>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { appointmentApi } from "@/api";

const status = ref("");
const list = ref<any[]>([]);

const load = async () => {
  const response = await appointmentApi.mine({ status: status.value || undefined, page: 1, pageSize: 20 });
  list.value = response.data.list || response.data.appointments || [];
};

const cancel = async (id: number) => {
  await appointmentApi.cancel(id, "用户主动取消");
  await load();
};

onMounted(load);
</script>
