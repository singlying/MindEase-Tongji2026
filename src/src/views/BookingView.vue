<template>
  <div class="grid">
    <section class="form-card span-5 stack">
      <h2>预约咨询</h2>
      <input v-model="date" class="input" type="date" />
      <button class="btn secondary" @click="loadSlots">查询可用时段</button>
      <textarea v-model="note" class="textarea" placeholder="给咨询师的备注"></textarea>
      <button class="btn" :disabled="!selected" @click="book">确认预约</button>
      <p class="muted">{{ message }}</p>
    </section>
    <section class="panel span-7">
      <h2>可用时段</h2>
      <div class="list">
        <button v-for="slot in slots" :key="slot.startTime" :class="['list-row', selected === slot ? 'chip' : '']" :disabled="!slot.available" @click="selected = slot">
          {{ slot.startTime }} - {{ slot.endTime }} · {{ slot.available ? "可预约" : "已占用" }}
        </button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRoute } from "vue-router";
import { appointmentApi } from "@/api";

const route = useRoute();
const counselorId = Number(route.params.counselorId);
const date = ref(new Date().toISOString().slice(0, 10));
const slots = ref<any[]>([]);
const selected = ref<any>(null);
const note = ref("");
const message = ref("");

const loadSlots = async () => {
  const response = await appointmentApi.slots(counselorId, date.value);
  slots.value = response.data.slots || [];
};

const book = async () => {
  if (!selected.value) return;
  const response = await appointmentApi.create({
    counselorId,
    startTime: selected.value.startTime,
    endTime: selected.value.endTime,
    userNote: note.value
  });
  message.value = response.message || "预约已提交";
};
</script>
