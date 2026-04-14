<!--
  组件名称: EmptyState - 空状态占位
  
  功能说明:
  - 显示数据为空时的占位界面
  - 展示图标、标题、描述信息
  - 支持通过插槽添加自定义操作按钮
  
  使用示例:
  <EmptyState 
    icon="fas fa-book" 
    title="还没有日记" 
    description="开始记录你的第一篇日记吧"
  >
    <template #action>
      <button @click="create">创建日记</button>
    </template>
  </EmptyState>
  
  Props:
  - icon: FontAwesome图标类名 (string, 默认: "fas fa-inbox")
  - title: 标题文字 (string, 默认: "暂无数据")
  - description: 描述文字 (string, 默认: "这里还没有任何内容")
  
  Slots:
  - action: 自定义操作区域，如按钮等
-->
<template>
  <div class="empty-state">
    <div class="empty-icon">
      <i :class="icon"></i>
    </div>
    <h3 class="empty-title">{{ title }}</h3>
    <p class="empty-description">{{ description }}</p>
    <slot name="action"></slot>
  </div>
</template>

<script setup lang="ts">
interface Props {
  icon?: string;
  title?: string;
  description?: string;
}

withDefaults(defineProps<Props>(), {
  icon: "fas fa-inbox",
  title: "暂无数据",
  description: "这里还没有任何内容",
});
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  text-align: center;
}

.empty-icon {
  width: 5rem;
  height: 5rem;
  border-radius: 50%;
  background: linear-gradient(
    135deg,
    rgba(123, 158, 137, 0.1),
    rgba(123, 158, 137, 0.05)
  );
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1.5rem;
}

.empty-icon i {
  font-size: 2.5rem;
  color: var(--ease-accent);
  opacity: 0.6;
}

.empty-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--ease-dark);
  margin-bottom: 0.5rem;
}

.empty-description {
  font-size: 0.875rem;
  color: #6b7280;
  max-width: 20rem;
  line-height: 1.6;
}
</style>
