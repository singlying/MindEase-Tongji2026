<!--
  组件名称: MoodTrendChart - 情绪趋势图表
  
  功能说明:
  - 使用ECharts展示情绪趋势折线图
  - 支持显示7天或30天的情绪变化
  - 自动响应数据变化重新渲染
  - 组件销毁时自动清理图表实例
  
  使用示例:
  <MoodTrendChart :data="trendData" height="400px" />
  
  Props:
  - data: 趋势数据数组 (MoodTrendItem[], 必填)
  - height: 图表高度 (string, 默认: "300px")
  
  数据格式:
  MoodTrendItem { date: string; score: number; moodType: string }
-->
<template>
  <div ref="chartRef" class="mood-trend-chart"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onBeforeUnmount } from "vue";
import * as echarts from "echarts";
import type { MoodTrendItem } from "@/api/mood";

interface Props {
  data: MoodTrendItem[];
  height?: string;
}

const props = withDefaults(defineProps<Props>(), {
  height: "300px",
});

const chartRef = ref<HTMLElement>();
let chartInstance: echarts.ECharts | null = null;

const initChart = () => {
  if (!chartRef.value) return;

  // 销毁旧实例
  if (chartInstance) {
    chartInstance.dispose();
  }

  // 创建新实例
  chartInstance = echarts.init(chartRef.value);

  // 提取数据
  const dates = props.data.map((item) => {
    const date = new Date(item.date);
    return `${date.getMonth() + 1}/${date.getDate()}`;
  });
  const scores = props.data.map((item) => item.score);

  // 配置项
  const option: echarts.EChartsOption = {
    grid: {
      left: "3%",
      right: "4%",
      bottom: "3%",
      top: "10%",
      containLabel: true,
    },
    xAxis: {
      type: "category",
      data: dates,
      boundaryGap: false,
      axisLine: {
        lineStyle: {
          color: "#e5e7eb",
        },
      },
      axisLabel: {
        color: "#9ca3af",
        fontSize: 12,
      },
    },
    yAxis: {
      type: "value",
      min: 0,
      max: 10,
      interval: 2,
      axisLine: {
        show: false,
      },
      axisTick: {
        show: false,
      },
      axisLabel: {
        color: "#9ca3af",
        fontSize: 12,
      },
      splitLine: {
        lineStyle: {
          color: "#f3f4f6",
          type: "dashed",
        },
      },
    },
    tooltip: {
      trigger: "axis",
      backgroundColor: "rgba(255, 255, 255, 0.95)",
      borderColor: "#7b9e89",
      borderWidth: 1,
      textStyle: {
        color: "#374151",
      },
      formatter: (params: any) => {
        const item = params[0];
        return `
          <div style="padding: 4px 8px;">
            <div style="font-weight: 600; margin-bottom: 4px;">${item.axisValue}</div>
            <div style="color: #7b9e89;">
              情绪得分: <strong>${item.value}</strong>/10
            </div>
          </div>
        `;
      },
    },
    series: [
      {
        type: "line",
        data: scores,
        smooth: true,
        symbol: "circle",
        symbolSize: 8,
        lineStyle: {
          color: {
            type: "linear",
            x: 0,
            y: 0,
            x2: 1,
            y2: 0,
            colorStops: [
              { offset: 0, color: "#7b9e89" },
              { offset: 1, color: "#10b981" },
            ],
          },
          width: 3,
        },
        itemStyle: {
          color: "#7b9e89",
          borderColor: "#fff",
          borderWidth: 2,
        },
        areaStyle: {
          color: {
            type: "linear",
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: "rgba(123, 158, 137, 0.3)" },
              { offset: 1, color: "rgba(123, 158, 137, 0.05)" },
            ],
          },
        },
        emphasis: {
          itemStyle: {
            color: "#7b9e89",
            borderColor: "#fff",
            borderWidth: 3,
            shadowBlur: 10,
            shadowColor: "rgba(123, 158, 137, 0.5)",
          },
        },
      },
    ],
  };

  chartInstance.setOption(option);
};

// 响应式调整
const handleResize = () => {
  chartInstance?.resize();
};

// 监听数据变化
watch(
  () => props.data,
  () => {
    initChart();
  },
  { deep: true }
);

onMounted(() => {
  initChart();
  window.addEventListener("resize", handleResize);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize);
  if (chartInstance) {
    chartInstance.dispose();
    chartInstance = null;
  }
});
</script>

<style scoped>
.mood-trend-chart {
  width: 100%;
  height: v-bind(height);
}
</style>
