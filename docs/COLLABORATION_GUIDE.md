# 前端协作文档

> 2025 年 12 月 1 日  

### 新增通用样式系统

**位置**：`src/assets/main.css`（第 6-56 行）

**新增 60+个 CSS 变量**，建议在组件中使用变量而非硬编码：提高整体设计的一致性
```css
.my-title {
  font-size: 24px;
  color: #2c3e50;
  margin: 16px;
}

/* 推荐换为： */
.my-title {
  font-size: var(--font-2xl);
  color: var(--ease-dark);
  margin: var(--spacing-md);
}
```
不过当然硬编码也没问题，怎么方便怎么来

#### 可用变量列表

**字体大小**：

```css
--font-xs    /* 12px - 小字 */
--font-sm    /* 14px - 次要文字 */
--font-base  /* 16px - 正文 */
--font-lg    /* 18px - 强调 */
--font-xl    /* 20px - 副标题 */
--font-2xl   /* 24px - 标题 */
--font-3xl   /* 30px - 大标题 */
--font-4xl   /* 36px - 超大标题 */
```

**间距**：

```css
--spacing-xs   /* 0.25rem = 4px */
--spacing-sm   /* 0.5rem = 8px */
--spacing-md   /* 1rem = 16px */
--spacing-lg   /* 1.5rem = 24px */
--spacing-xl   /* 2rem = 32px */
--spacing-2xl  /* 3rem = 48px */
```

**圆角**：

```css
--radius-sm   /* 0.5rem = 8px */
--radius-md   /* 0.75rem = 12px */
--radius-lg   /* 1rem = 16px */
--radius-xl   /* 1.5rem = 24px */
--radius-full /* 9999px - 胶囊形 */
```

**颜色**：

```css
--ease-accent       /* 主题色 - 鼠尾草绿 */
--ease-dark         /* 深色文字 */
--gray-100 ~ --gray-900  /* 灰度 */
```

#### 全局样式类（可直接使用）

**容器**：

```html
<div class="page-container">
  <!-- 常规页面容器 -->
  <div class="page-container-narrow">
    <!-- 窄页面容器（适合详情页）-->
    <div class="card"><!-- 卡片容器 --></div>
  </div>
</div>
```

**标题**：

```html
<h1 class="page-title">主标题</h1>
<h2 class="section-title">小节标题</h2>
<p class="page-subtitle">副标题</p>
```

**按钮**：

```html
<button class="btn-gradient">主按钮</button>
<button class="btn-secondary">次要按钮</button>
<button class="btn-accent">强调按钮</button>
```

**其他**：

```html
<span class="tag">标签</span>
<div class="empty-state">空状态</div>
<div class="loading-state">加载状态</div>
```

---

### 3. 新增组件

**位置**：`src/components/charts/MoodTrendChart.vue`

**用途**：ECharts 情绪趋势图组件

**使用示例**：

```vue
<script setup>
import MoodTrendChart from "@/components/charts/MoodTrendChart.vue";

const trendData = ref([
  { date: "2025-11-25", score: 7.5 },
  { date: "2025-11-26", score: 8.0 },
]);
</script>

<template>
  <MoodTrendChart :data="trendData" height="300px" />
</template>
```

## 💡 开发建议

### 每日工作流程

```bash
# 1. 每天开发前，先同步主仓库
git fetch upstream
git merge upstream/main
# 2. 如有冲突，解决冲突后提交
git add .
git commit -m "merge: 同步主仓库"
# 3. 安装可能的新依赖
npm install
# 4. 启动开发服务器
npm run dev
# 5. 开发完成后推送到自己的fork
git push origin your-branch
```

