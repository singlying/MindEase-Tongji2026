<template>
  <div class="profile-layout">
    <section class="panel profile-card">
      <div class="profile-avatar">
        <img v-if="avatar && avatarOk" :src="avatar" alt="用户头像" @error="avatarOk = false" />
        <span v-else>{{ initials }}</span>
      </div>
      <h2>{{ nickname || session.userInfo?.username || "MindEase 用户" }}</h2>
      <p class="muted">{{ roleText }} · {{ session.userInfo?.username }}</p>
      <div class="profile-stats">
        <div><strong>{{ session.userInfo?.status ?? "-" }}</strong><span>账号状态</span></div>
        <div><strong>{{ createDate }}</strong><span>加入时间</span></div>
      </div>
    </section>

    <form class="form-card stack profile-form" @submit.prevent="save">
      <div>
        <span class="section-kicker">个人资料</span>
        <h2>基础信息</h2>
        <p class="muted">昵称和头像会显示在工作台、咨询记录和个人中心。</p>
      </div>
      <label class="field">
        <span>昵称</span>
        <input v-model="nickname" class="input" placeholder="请输入昵称" />
      </label>
      <label class="field">
        <span>头像 URL</span>
        <input v-model="avatar" class="input" placeholder="https://..." @input="avatarOk = true" />
      </label>
      <button class="btn" :disabled="saving">
        <span v-if="saving" class="spinner"></span>
        {{ saving ? "保存中" : "保存资料" }}
      </button>
      <p v-if="message" class="feedback-note" :class="{ 'danger-note': failed }">{{ message }}</p>
    </form>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { authApi } from "@/api";
import { useSessionStore } from "@/stores/session";

const session = useSessionStore();
const nickname = ref("");
const avatar = ref("");
const message = ref("");
const saving = ref(false);
const failed = ref(false);
const avatarOk = ref(true);

const initials = computed(() => (nickname.value || session.userInfo?.username || "ME").slice(0, 2).toUpperCase());
const createDate = computed(() => String(session.userInfo?.createTime || "").replace("T", " ").slice(0, 10) || "-");
const roleText = computed(() => {
  const role = session.userInfo?.role?.toUpperCase();
  if (role === "ADMIN") return "管理员";
  if (role === "COUNSELOR") return "咨询师";
  return "普通用户";
});

onMounted(async () => {
  if (!session.userInfo) await session.fetchUserInfo().catch(() => undefined);
  nickname.value = session.userInfo?.nickname || "";
  avatar.value = session.userInfo?.avatar || "";
});

const save = async () => {
  saving.value = true;
  failed.value = false;
  try {
    const response = await authApi.updateProfile({ nickname: nickname.value, avatar: avatar.value });
    message.value = response.message || "资料已保存";
    session.fetchUserInfo().catch(() => undefined);
  } catch (err) {
    failed.value = true;
    message.value = err instanceof Error ? err.message : "保存失败，请稍后再试";
  } finally {
    saving.value = false;
  }
};
</script>
