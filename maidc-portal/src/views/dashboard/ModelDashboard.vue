<template>
  <PageContainer title="模型看板" subtitle="模型部署与推理监控">
    <!-- Top Row: Metric Cards -->
    <a-row :gutter="[16, 16]">
      <a-col :span="6">
        <MetricCard
          title="活跃部署"
          :value="metrics.activeDeployments"
          suffix="个"
          :trend="{ value: 8, type: 'up' }"
          :loading="loading"
        >
          <template #icon><RocketOutlined /></template>
        </MetricCard>
      </a-col>
      <a-col :span="6">
        <MetricCard
          title="今日推理"
          :value="metrics.todayInference"
          suffix="次"
          :trend="{ value: 15, type: 'up' }"
          :loading="loading"
        >
          <template #icon><ThunderboltOutlined /></template>
        </MetricCard>
      </a-col>
      <a-col :span="6">
        <MetricCard
          title="平均延迟"
          :value="metrics.avgLatency"
          suffix="ms"
          :trend="{ value: 12, type: 'down' }"
          :loading="loading"
        >
          <template #icon><ClockCircleOutlined /></template>
        </MetricCard>
      </a-col>
      <a-col :span="6">
        <MetricCard
          title="GPU利用率"
          :value="metrics.gpuUtilization"
          suffix="%"
          :trend="{ value: 5, type: 'up' }"
          :loading="loading"
        >
          <template #icon><DashboardOutlined /></template>
        </MetricCard>
      </a-col>
    </a-row>

    <!-- Middle Row: Inference Trend (Full Width) -->
    <a-row style="margin-top: 16px">
      <a-col :span="24">
        <a-card title="推理量趋势（近7天）" :bordered="false">
          <MetricChart :option="inferenceTrendOption" :height="360" />
        </a-card>
      </a-col>
    </a-row>

    <!-- Bottom Row: Performance Ranking + Deployment Status -->
    <a-row :gutter="[16, 16]" style="margin-top: 16px">
      <a-col :span="12">
        <a-card title="模型性能排行（推理量 Top 5）" :bordered="false">
          <MetricChart :option="performanceRankOption" :height="320" />
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card title="部署状态分布" :bordered="false">
          <MetricChart :option="deployStatusOption" :height="320" />
        </a-card>
      </a-col>
    </a-row>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import {
  RocketOutlined,
  ThunderboltOutlined,
  ClockCircleOutlined,
  DashboardOutlined,
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import MetricCard from '@/components/MetricCard/index.vue'
import MetricChart from '@/components/MetricChart/index.vue'

const loading = ref(false)

// ============ Metrics ============
const metrics = reactive({
  activeDeployments: 45,
  todayInference: 23856,
  avgLatency: 38,
  gpuUtilization: 76,
})

// ============ Inference Trend Chart (7 Days) ============
const inferenceTrendOption = {
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'cross' },
  },
  legend: {
    data: ['推理总量', '成功次数', '失败次数'],
  },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: [
      '04-05 周六',
      '04-06 周日',
      '04-07 周一',
      '04-08 周二',
      '04-09 周三',
      '04-10 周四',
      '04-11 周五',
    ],
  },
  yAxis: { type: 'value', name: '次数' },
  series: [
    {
      name: '推理总量',
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.1 },
      data: [18230, 16450, 21340, 23120, 24560, 22890, 23856],
      itemStyle: { color: '#1677ff' },
    },
    {
      name: '成功次数',
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.08 },
      data: [18120, 16380, 21260, 23050, 24490, 22810, 23790],
      itemStyle: { color: '#52c41a' },
    },
    {
      name: '失败次数',
      type: 'line',
      smooth: true,
      data: [110, 70, 80, 70, 70, 80, 66],
      itemStyle: { color: '#ff4d4f' },
    },
  ],
}

// ============ Performance Ranking Bar Chart ============
const performanceRankOption = {
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: {
    type: 'value',
    name: '推理次数',
  },
  yAxis: {
    type: 'category',
    data: [
      '病理切片分析',
      '心电图诊断',
      '骨科影像识别',
      '心血管风险评估',
      '胸部CT诊断',
    ],
  },
  series: [
    {
      type: 'bar',
      barWidth: '60%',
      data: [
        { value: 5620, itemStyle: { color: '#1677ff' } },
        { value: 7230, itemStyle: { color: '#1677ff' } },
        { value: 8450, itemStyle: { color: '#4096ff' } },
        { value: 9870, itemStyle: { color: '#4096ff' } },
        { value: 12450, itemStyle: { color: '#1677ff' } },
      ],
      itemStyle: { borderRadius: [0, 4, 4, 0] },
      label: { show: true, position: 'right', formatter: '{c}' },
    },
  ],
}

// ============ Deployment Status Pie Chart ============
const deployStatusOption = {
  tooltip: { trigger: 'item' },
  legend: { bottom: 0 },
  series: [
    {
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{d}%' },
      data: [
        { value: 32, name: '运行中', itemStyle: { color: '#52c41a' } },
        { value: 8, name: '灰度发布', itemStyle: { color: '#1677ff' } },
        { value: 5, name: '部署中', itemStyle: { color: '#faad14' } },
        { value: 3, name: '异常', itemStyle: { color: '#ff4d4f' } },
        { value: 6, name: '已停止', itemStyle: { color: '#d9d9d9' } },
      ],
    },
  ],
}
</script>

<style scoped>
</style>
