<template>
  <div class="grid">
    <section class="form-card span-4 stack">
      <h2>智能匹配</h2>
      <input v-model="keyword" class="input" placeholder="搜索咨询师、方向或城市" />
      <select v-model="sort" class="select">
        <option value="smart">智能排序</option>
        <option value="price_asc">价格优先</option>
        <option value="rating_desc">评分优先</option>
      </select>
      <button class="btn" @click="load">获取推荐</button>
      <p class="muted">推荐条件：{{ status?.recommendationReady ? "已满足" : "等待更多测评或日记数据" }}</p>
    </section>
    <section class="panel span-8">
      <h2>咨询师列表</h2>
      <div class="list">
        <article v-for="item in counselors" :key="item.id" class="list-row">
          <div class="row-between">
            <div class="row-between" style="justify-content: flex-start">
              <img class="avatar-img" :src="item.avatar || fallbackAvatar(item.realName)" :alt="item.realName" @error="replaceImage" />
              <div>
              <strong>{{ item.realName }}</strong>
              <p class="muted">{{ item.title }} · {{ item.experienceYears }} 年经验 · ￥{{ item.pricePerHour }}/小时</p>
              <span v-for="tag in item.tags || item.specialty || []" :key="tag" class="chip" style="margin-right: 6px">{{ tag }}</span>
              </div>
            </div>
            <button class="btn" @click="$router.push(`/booking/${item.id}`)">预约</button>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { counselorApi } from "@/api";

const keyword = ref("");
const sort = ref<"smart" | "price_asc" | "rating_desc">("smart");
const status = ref<any>(null);
const counselors = ref<any[]>([]);

const load = async () => {
  status.value = (await counselorApi.recommendStatus().catch(() => ({ data: null }))).data;
  const response = await counselorApi.recommend({ keyword: keyword.value, sort: sort.value });
  counselors.value = response.data.counselors || [];
};

const fallbackAvatar = (name = "ME") => `https://ui-avatars.com/api/?name=${encodeURIComponent(name)}&background=DFE8D9&color=20211F&size=128`;
const replaceImage = (event: Event) => {
  const image = event.target as HTMLImageElement;
  image.src = fallbackAvatar(image.alt);
};

onMounted(load);
</script>
