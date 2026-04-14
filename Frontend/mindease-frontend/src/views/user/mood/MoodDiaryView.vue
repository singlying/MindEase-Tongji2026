<template>
  <div class="page-container-narrow">
    <!-- 提交日记&AI分析全局加载状态 -->
    <LoadingSpinner
      v-if="submitting"
      :fullscreen="true"
      text="正在记录你的情绪，并为你生成AI分析..."
    />

    <!-- 顶部装饰条 -->
    <div class="top-decoration"></div>

    <!-- 写日记区域 -->
    <div class="card" style="margin-bottom: 2rem">
      <!-- 日期显示 -->
      <div style="margin-bottom: 2rem">
        <h2 class="section-title">{{ formattedDate }}</h2>
        <p class="page-subtitle">{{ formattedTime }}</p>
      </div>

      <!-- 情绪选择 -->
      <MoodSelector v-model="formData.moodType" />

      <!-- 情绪分数 -->
      <div style="margin-bottom: 2rem">
        <label class="section-title">情绪分数</label>
        <div
          style="
            display: flex;
            align-items: center;
            gap: 1rem;
            margin-bottom: 0.5rem;
          "
        >
          <span
            style="font-size: 2rem; font-weight: 700; color: var(--ease-accent)"
            >{{ formData.moodScore }}</span
          >
          <span style="color: var(--gray-400)">/ 10</span>
        </div>
        <el-slider
          v-model="formData.moodScore"
          :min="1"
          :max="10"
          :show-tooltip="false"
        />
        <div
          style="
            display: flex;
            justify-content: space-between;
            font-size: var(--font-xs);
            color: var(--gray-400);
            margin-top: 0.5rem;
          "
        >
          <span>较低</span>
          <span>中等</span>
          <span>强烈</span>
        </div>
      </div>

      <!-- 内容输入 -->
      <div style="margin-bottom: 2rem">
        <label class="section-title">
          <i class="fas fa-pen-fancy"></i>
          今天发生了什么？
        </label>
        <textarea
          v-model="formData.content"
          rows="8"
          class="diary-textarea"
          placeholder="此刻在想些什么？写下来，让情绪流动...

你可以记录：
• 今天发生的事情
• 内心的感受和想法
• 让你开心或烦恼的事
• 任何想要倾诉的内容"
        ></textarea>
      </div>

      <!-- 操作按钮 -->
      <div style="display: flex; justify-content: flex-end">
        <button
          @click="submitDiary"
          class="btn-gradient"
          :disabled="submitting"
        >
          <i class="fas fa-check"></i>
          提交日记
        </button>
      </div>
    </div>

    <!-- 近期日记 -->
    <div class="card">
      <div
        style="
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 1.5rem;
        "
      >
        <h3 class="section-title" style="margin: 0">
          <i class="fas fa-clock-rotate-left"></i>
          近期日记
        </h3>

        <!-- 筛选按钮 -->
        <button @click="showFilterDialog = true" class="btn-secondary btn-sm">
          <i class="fas fa-filter"></i>
          筛选
        </button>
      </div>

      <!-- 筛选条件展示 -->
      <div v-if="hasActiveFilters" class="active-filters">
        <span class="filter-tag" v-if="filterForm.moodType">
          {{ getMoodTypeName(filterForm.moodType) }}
          <i
            class="fas fa-times"
            @click="clearFilter('moodType')"
            style="margin-left: 0.5rem; cursor: pointer"
          ></i>
        </span>
        <span class="filter-tag" v-if="filterForm.dateRange[0]">
          {{ filterForm.dateRange[0] }} ~ {{ filterForm.dateRange[1] }}
          <i
            class="fas fa-times"
            @click="clearFilter('dateRange')"
            style="margin-left: 0.5rem; cursor: pointer"
          ></i>
        </span>
        <button @click="clearAllFilters" class="clear-all-btn">
          清除所有筛选
        </button>
      </div>

      <LoadingSpinner v-if="loading" />

      <EmptyState
        v-else-if="!loading && filteredDiaryList.length === 0"
        icon="fas fa-book"
        :title="hasActiveFilters ? '没有符合条件的日记' : '还没有日记'"
        :description="
          hasActiveFilters
            ? '尝试调整筛选条件'
            : '提交第一篇日记，开始你的情绪记录之旅'
        "
      />

      <div v-else class="diary-grid">
        <MoodCard
          v-for="item in filteredDiaryList"
          :key="item.id"
          :mood="item"
          @click="viewDetail"
        />
      </div>
    </div>

    <!-- 筛选对话框 -->
    <el-dialog v-model="showFilterDialog" title="筛选日记" width="500px">
      <el-form :model="filterForm" label-width="80px">
        <el-form-item label="情绪类型">
          <el-select
            v-model="filterForm.moodType"
            placeholder="全部"
            clearable
            style="width: 100%"
          >
            <el-option label="全部" value="" />
            <!-- ⭐ 使用统一定义的情绪选项数据 -->
            <el-option
              v-for="mood in MOOD_OPTIONS"
              :key="mood.value"
              :label="`${mood.emoji} ${mood.label}`"
              :value="mood.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="日期范围">
          <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="resetFilter">重置</el-button>
          <el-button type="primary" @click="applyFilter">应用筛选</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import type { MoodLogItem, CreateMoodLogParams, MoodType } from "@/api/mood";
import {
  createMoodLog,
  getMoodLogs,
  MOOD_TYPE_MAP,
  MOOD_OPTIONS,
} from "@/api/mood";
import MoodSelector from "@/components/common/MoodSelector.vue";
import MoodCard from "@/components/common/MoodCard.vue";
import LoadingSpinner from "@/components/common/LoadingSpinner.vue";
import EmptyState from "@/components/common/EmptyState.vue";

const router = useRouter();

// 将本地时间格式化为后端可解析的 ISO 本地时间 "YYYY-MM-DDTHH:mm:ss"
const formatLocalDateTime = () => {
  const d = new Date();
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(
    d.getHours()
  )}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
};

// 表单数据
const formData = ref<CreateMoodLogParams>({
  moodType: "" as MoodType,
  moodScore: 5,
  content: "",
  tags: [],
  logDate: formatLocalDateTime(), // 使用本地时间避免 toISOString() 带来的时区偏差
});
const submitting = ref(false);

// 日期时间
const now = new Date();
const formattedDate = computed(
  () => `${now.getFullYear()}年 ${now.getMonth() + 1}月 ${now.getDate()}日`
);
const formattedTime = computed(() => {
  const days = [
    "星期日",
    "星期一",
    "星期二",
    "星期三",
    "星期四",
    "星期五",
    "星期六",
  ];
  return `${days[now.getDay()]} • ${now.getHours()}:${String(
    now.getMinutes()
  ).padStart(2, "0")}`;
});

// 近期日记列表
const diaryList = ref<MoodLogItem[]>([]);
const loading = ref(false);

// 筛选相关状态
const showFilterDialog = ref(false);
const filterForm = ref({
  moodType: "" as MoodType | "",
  dateRange: [] as string[],
});

// 筛选后的日记列表（前端本地筛选）
const filteredDiaryList = computed(() => {
  let result = diaryList.value;

  // 按情绪类型筛选
  if (filterForm.value.moodType) {
    result = result.filter(
      (item) => item.moodType === filterForm.value.moodType
    );
  }

  // 按日期范围筛选
  if (filterForm.value.dateRange && filterForm.value.dateRange.length === 2) {
    const startDate = filterForm.value.dateRange[0] || "";
    const endDate = filterForm.value.dateRange[1] || "";
    if (startDate && endDate) {
      result = result.filter((item) => {
        const itemDate = item.logDate.split("T")[0] || ""; // ISO格式提取日期部分 "2024-11-27"
        return itemDate >= startDate && itemDate <= endDate;
      });
    }
  }

  return result;
});

// 是否有激活的筛选条件
const hasActiveFilters = computed(() => {
  return (
    !!filterForm.value.moodType ||
    (filterForm.value.dateRange && filterForm.value.dateRange.length === 2)
  );
});

// ⭐ 情绪类型中文名获取（使用统一定义的数据）
const getMoodTypeName = (type: string) => {
  return MOOD_TYPE_MAP[type as MoodType]?.label || type;
};

// 提交日记
const submitDiary = async () => {
  // 验证
  if (!formData.value.moodType) {
    ElMessage.warning("请选择情绪类型");
    return;
  }
  if (!formData.value.content.trim()) {
    ElMessage.warning("请填写日记内容");
    return;
  }

  submitting.value = true;

  try {
    const res = (await createMoodLog(formData.value)) as any;
    ElMessage.success("日记提交成功");

    // 显示AI分析
    if (res.data.aiAnalysis) {
      ElMessageBox.alert(res.data.aiAnalysis, "AI 情绪分析", {
        confirmButtonText: "好的",
        type: "success",
      });
    }

    // 重置表单
    formData.value = {
      moodType: "" as MoodType,
      moodScore: 5,
      content: "",
      tags: [],
      logDate: formatLocalDateTime(),
    };

    // 刷新列表
    loadDiaries();
  } catch (error) {
    console.error("提交失败:", error);
  } finally {
    submitting.value = false;
  }
};

// 加载近期日记
const loadDiaries = async () => {
  loading.value = true;

  try {
    const res = (await getMoodLogs(10, 0)) as any;
    diaryList.value = res.data.logs;
  } catch (error) {
    console.error("加载失败:", error);
  } finally {
    loading.value = false;
  }
};

// 查看详情
const viewDetail = (id: number) => {
  router.push(`/mood-diary/${id}`);
};

// 应用筛选
const applyFilter = () => {
  showFilterDialog.value = false;
  ElMessage.success("筛选已应用");
};

// 重置筛选
const resetFilter = () => {
  filterForm.value = {
    moodType: "" as MoodType | "",
    dateRange: [],
  };
  ElMessage.info("筛选已重置");
};

// 清除单个筛选条件
const clearFilter = (type: "moodType" | "dateRange") => {
  if (type === "moodType") {
    filterForm.value.moodType = "" as MoodType | "";
  } else if (type === "dateRange") {
    filterForm.value.dateRange = [];
  }
};

// 清除所有筛选
const clearAllFilters = () => {
  filterForm.value = {
    moodType: "" as MoodType | "",
    dateRange: [],
  };
  ElMessage.info("已清除所有筛选");
};

onMounted(() => {
  loadDiaries();
});
</script>

<style scoped>
.diary-textarea {
  width: 100%;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(10px);
  border: 2px solid var(--gray-200);
  border-radius: var(--radius-lg);
  padding: 1.5rem;
  font-size: var(--font-base);
  color: var(--gray-700);
  line-height: 1.75;
  resize: none;
  transition: all 0.3s ease;
  outline: none;
  font-family: inherit;
}

.diary-textarea::placeholder {
  color: var(--gray-400);
}

.diary-textarea:hover {
  background: rgba(255, 255, 255, 0.7);
}

.diary-textarea:focus {
  border-color: var(--ease-accent);
  box-shadow: 0 0 0 3px rgba(123, 158, 137, 0.1);
}

.diary-grid {
  display: grid;
  gap: var(--spacing-lg);
  margin-top: var(--spacing-lg);
}

/* 筛选相关样式 */
.btn-sm {
  font-size: var(--font-sm);
  padding: 0.5rem 1rem;
}

.active-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
  margin-bottom: 1.5rem;
  padding: 1rem;
  background: rgba(123, 158, 137, 0.05);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(123, 158, 137, 0.1);
}

.filter-tag {
  display: inline-flex;
  align-items: center;
  padding: 0.5rem 1rem;
  background: var(--ease-accent);
  color: white;
  border-radius: var(--radius-md);
  font-size: var(--font-sm);
  font-weight: 500;
}

.filter-tag i {
  opacity: 0.8;
  transition: opacity 0.2s;
}

.filter-tag i:hover {
  opacity: 1;
}

.clear-all-btn {
  padding: 0.5rem 1rem;
  background: transparent;
  border: 1px solid var(--gray-300);
  color: var(--gray-600);
  border-radius: var(--radius-md);
  font-size: var(--font-sm);
  cursor: pointer;
  transition: all 0.3s ease;
}

.clear-all-btn:hover {
  background: var(--gray-100);
  border-color: var(--gray-400);
  color: var(--gray-700);
}

@media (max-width: 768px) {
  .diary-grid {
    gap: var(--spacing-md);
  }
}
</style>
