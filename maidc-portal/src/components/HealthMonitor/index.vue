<template>
  <a-spin :spinning="loading">
    <!-- 状态卡片 -->
    <a-row :gutter="16" style="margin-bottom: 16px">
      <a-col :span="6">
        <a-statistic title="连接状态" :value="stats.availabilityRate >= 0.9 ? '正常' : '异常'"
          :value-style="{ color: stats.availabilityRate >= 0.9 ? '#52c41a' : '#ff4d4f' }" />
      </a-col>
      <a-col :span="6">
        <a-statistic title="平均延迟" :value="Math.round(stats.avgLatencyMs || 0)" suffix="ms" />
      </a-col>
      <a-col :span="6">
        <a-statistic title="30天可用率" :value="(stats.availabilityRate * 100).toFixed(1)" suffix="%" />
      </a-col>
      <a-col :span="6">
        <a-statistic title="总检查次数" :value="stats.totalChecks || 0" />
      </a-col>
    </a-row>

    <!-- 延迟趋势图 -->
    <a-card title="延迟趋势" size="small" style="margin-bottom: 16px">
      <MetricChart v-if="!chartError" :option="latencyChartOption" height="250px" />
      <div v-else style="height: 250px; display: flex; align-items: center; justify-content: center; color: #999">
        延迟趋势图（需安装 MetricChart 组件）
      </div>
    </a-card>

    <!-- 最近检查记录 -->
    <a-table :columns="healthColumns" :data-source="healthData" row-key="id" size="small"
      :pagination="{ pageSize: 10, showTotal: (t: number) => `共 ${t} 条` }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-badge :color="record.status === 'SUCCESS' ? 'green' : 'red'"
            :text="record.status === 'SUCCESS' ? '成功' : record.status === 'TIMEOUT' ? '超时' : '失败'" />
        </template>
        <template v-if="column.key === 'latency'">
          {{ record.latency_ms != null ? `${record.latency_ms}ms` : '-' }}
        </template>
        <template v-if="column.key === 'time'">
          {{ formatDateTime(record.checked_at) }}
        </template>
      </template>
    </a-table>
  </a-spin>
</template>

<script setup lang="ts">
import { ref, watch, computed, defineAsyncComponent } from 'vue'
import { getDataSourceHealth, getDataSourceHealthStats } from '@/api/data'

const props = defineProps<{ sourceId: number }>()

const loading = ref(false)
const healthData = ref<any[]>([])
const stats = ref<Record<string, any>>({})
const chartError = ref(false)

const MetricChart = defineAsyncComponent(() =>
  import('@/components/MetricChart/index.vue').catch(() => {
    chartError.value = true
    return { default: { template: '<div>Chart unavailable</div>' } }
  })
)

const healthColumns = [
  { title: '检查时间', key: 'time', width: 170 },
  { title: '状态', key: 'status', width: 100 },
  { title: '延迟', key: 'latency', width: 100 },
  { title: '错误信息', dataIndex: 'error_message', ellipsis: true },
]

const latencyChartOption = computed(() => {
  const data = [...healthData.value].reverse()
  return {
    tooltip: { trigger: 'axis', formatter: '{b}: {c}ms' },
    xAxis: { type: 'category', data: data.map(h => formatDateTime(h.checked_at).slice(11, 16)) },
    yAxis: { type: 'value', name: '延迟(ms)' },
    series: [{
      type: 'line', data: data.map(h => h.latency_ms),
      smooth: true, areaStyle: { opacity: 0.15 },
      itemStyle: { color: '#1677ff' }
    }],
    grid: { left: 50, right: 20, top: 20, bottom: 30 }
  }
})

function formatDateTime(dateStr: string) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

async function loadData() {
  loading.value = true
  try {
    const [healthRes, statsRes] = await Promise.all([
      getDataSourceHealth(props.sourceId, 100),
      getDataSourceHealthStats(props.sourceId, 30)
    ])
    healthData.value = healthRes.data.data
    stats.value = statsRes.data.data
  } finally {
    loading.value = false
  }
}

watch(() => props.sourceId, () => loadData(), { immediate: true })
</script>
