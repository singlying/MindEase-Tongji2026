<!-- 前端A负责：情绪趋势图表 -->
<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from "vue";
import * as echarts from "echarts";

import type { MoodTrendItem } from "@/api/mood";

const props = withDefaults(
  defineProps<{
    data: MoodTrendItem[];
    height?: string;
  }>(),
  {
    height: "280px",
  }
);

const chartRef = ref<HTMLElement | null>(null);
let chartInstance: echarts.ECharts | null = null;

function renderChart() {
  if (!chartRef.value) {
    return;
  }

  if (chartInstance) {
    chartInstance.dispose();
  }

  chartInstance = echarts.init(chartRef.value);

  chartInstance.setOption({
    grid: {
      top: 24,
      left: 20,
      right: 20,
      bottom: 20,
      containLabel: true,
    },
    tooltip: {
      trigger: "axis",
    },
    xAxis: {
      type: "category",
      data: props.data.map((item) => {
        const date = new Date(item.date);
        return `${date.getMonth() + 1}/${date.getDate()}`;
      }),
      boundaryGap: false,
    },
    yAxis: {
      type: "value",
      min: 0,
      max: 10,
      interval: 2,
    },
    series: [
      {
        type: "line",
        smooth: true,
        data: props.data.map((item) => item.score),
        lineStyle: {
          width: 3,
          color: "#7b9e89",
        },
        itemStyle: {
          color: "#7b9e89",
        },
        areaStyle: {
          color: "rgba(123, 158, 137, 0.18)",
        },
      },
    ],
  });
}

function handleResize() {
  chartInstance?.resize();
}

watch(
  () => props.data,
  () => {
    renderChart();
  },
  { deep: true }
);

onMounted(() => {
  renderChart();
  window.addEventListener("resize", handleResize);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize);
  chartInstance?.dispose();
  chartInstance = null;
});
</script>

<template>
  <div ref="chartRef" class="mood-trend-chart" :style="{ height }"></div>
</template>

<style scoped>
.mood-trend-chart {
  width: 100%;
}
</style>
