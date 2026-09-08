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
import { ref, reactive, onMounted } from 'vue'
import {
  RocketOutlined,
  ThunderboltOutlined,
  ClockCircleOutlined,
  DashboardOutlined,
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import MetricCard from '@/components/MetricCard/index.vue'
import MetricChart from '@/components/MetricChart/index.vue'
import { getMetricsOverview, getDeployments } from '@/api/model'

const loading = ref(false)

const metrics = reactive({
  activeDeployments: 0,
  todayInference: 0,
  avgLatency: 0,
  gpuUtilization: 0,
})

const inferenceTrendOption = ref<Record<string, any>>({})
const performanceRankOption = ref<Record<string, any>>({})
const deployStatusOption = ref<Record<string, any>>({})

function buildTrendOption(trend: any) {
  if (!trend) return {}
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: { data: ['推理总量', '成功次数', '失败次数'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: trend.dates || [] },
    yAxis: { type: 'value', name: '次数' },
    series: [
      { name: '推理总量', type: 'line', smooth: true, areaStyle: { opacity: 0.1 }, data: trend.total || [], itemStyle: { color: '#1677ff' } },
      { name: '成功次数', type: 'line', smooth: true, areaStyle: { opacity: 0.08 }, data: trend.success || [], itemStyle: { color: '#52c41a' } },
      { name: '失败次数', type: 'line', smooth: true, data: trend.failed || [], itemStyle: { color: '#ff4d4f' } },
    ],
  }
}

function buildRankOption(ranking: any[]) {
  if (!ranking?.length) return {}
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'value', name: '推理次数' },
    yAxis: { type: 'category', data: ranking.map(r => r.name) },
    series: [{
      type: 'bar', barWidth: '60%',
      data: ranking.map(r => ({ value: r.count, itemStyle: { color: '#1677ff' } })),
      itemStyle: { borderRadius: [0, 4, 4, 0] },
      label: { show: true, position: 'right', formatter: '{c}' },
    }],
  }
}

function buildDeployOption(distribution: any[]) {
  if (!distribution?.length) return {}
  const colors: Record<string, string> = { '运行中': '#52c41a', '灰度发布': '#1677ff', '部署中': '#faad14', '异常': '#ff4d4f', '已停止': '#d9d9d9' }
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['40%', '70%'], avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{d}%' },
      data: distribution.map(d => ({ value: d.count, name: d.status, itemStyle: { color: colors[d.status] || '#8c8c8c' } })),
    }],
  }
}

async function fetchDashboard() {
  loading.value = true
  try {
    const [metricsRes, deploymentsRes] = await Promise.all([
      getMetricsOverview().catch(() => null),
      getDeployments({ page: 1, page_size: 1 }).catch(() => null),
    ])
    if (deploymentsRes) {
      metrics.activeDeployments = deploymentsRes.data.data.total || 0
    }
    if (metricsRes) {
      const data = metricsRes.data.data
      if (data) {
        metrics.todayInference = data.todayInference || 0
        metrics.avgLatency = data.avgLatency || 0
        metrics.gpuUtilization = data.gpuUtilization || 0
        inferenceTrendOption.value = buildTrendOption(data.inferenceTrend)
        performanceRankOption.value = buildRankOption(data.performanceRanking)
        deployStatusOption.value = buildDeployOption(data.deploymentStatusDistribution)
      }
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchDashboard()
})
</script>

<style scoped>
</style>
