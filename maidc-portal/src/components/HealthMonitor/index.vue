<template>
  <div v-loading="loading" class="health-monitor">
    <!-- 状态卡片 -->
    <div class="health-stats">
      <div class="stat-item">
        <div class="stat-label">连接状态</div>
        <div class="stat-value" :style="{ color: stats.availabilityRate >= 0.9 ? '#52c41a' : '#ff4d4f' }">
          {{ stats.availabilityRate >= 0.9 ? '正常' : '异常' }}
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-label">平均延迟</div>
        <div class="stat-value">{{ Math.round(stats.avgLatencyMs || 0) }}<span class="stat-suffix">ms</span></div>
      </div>
      <div class="stat-item">
        <div class="stat-label">30天可用率</div>
        <div class="stat-value">{{ (stats.availabilityRate * 100).toFixed(1) }}<span class="stat-suffix">%</span></div>
      </div>
      <div class="stat-item">
        <div class="stat-label">总检查次数</div>
        <div class="stat-value">{{ stats.totalChecks || 0 }}</div>
      </div>
    </div>

    <!-- 延迟趋势图 -->
    <el-card shadow="never" size="small" class="health-card">
      <template #header>
        <span class="card-title">延迟趋势</span>
      </template>
      <MetricChart v-if="!chartError" :option="latencyChartOption" height="250px" />
      <div v-else style="height: 250px; display: flex; align-items: center; justify-content: center; color: #999">
        延迟趋势图（需安装 MetricChart 组件）
      </div>
    </el-card>

    <!-- 最近检查记录 -->
    <el-table :data="pagedHealthData" row-key="id" size="small">
      <el-table-column label="检查时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.checked_at) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <span class="status-inline">
            <span class="status-dot" :style="{ background: row.status === 'SUCCESS' ? '#52c41a' : '#ff4d4f' }" />
            {{ row.status === 'SUCCESS' ? '成功' : row.status === 'TIMEOUT' ? '超时' : '失败' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="延迟" width="100">
        <template #default="{ row }">{{ row.latency_ms != null ? `${row.latency_ms}ms` : '-' }}</template>
      </el-table-column>
      <el-table-column label="错误信息" prop="error_message" show-overflow-tooltip />
    </el-table>
    <el-pagination
      v-model:current-page="currentPage"
      :page-size="pageSize"
      :total="healthData.length"
      layout="total, prev, pager, next"
      class="health-pagination"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, defineAsyncComponent, type Component } from 'vue'
import { getDataSourceHealth, getDataSourceHealthStats } from '@/api/data'

const props = defineProps<{ sourceId: number }>()

const loading = ref(false)
const healthData = ref<any[]>([])
const stats = ref<Record<string, any>>({})
const chartError = ref(false)
const currentPage = ref(1)
const pageSize = 10

const MetricChart = defineAsyncComponent(() =>
  import('@/components/MetricChart/index.vue').catch(() => {
    chartError.value = true
    return { default: { template: '<div>Chart unavailable</div>' } as Component }
  })
)

const pagedHealthData = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return healthData.value.slice(start, start + pageSize)
})

const latencyChartOption = computed(() => {
  const data = [...healthData.value].reverse()
  return {
    tooltip: { trigger: 'axis', formatter: '{b}: {c}ms' },
    xAxis: { type: 'category', data: data.map(h => formatDateTime(h.checked_at).slice(11, 16)) },
    yAxis: { type: 'value', name: '延迟(ms)' },
    series: [{
      type: 'line', data: data.map(h => h.latency_ms),
      smooth: true, areaStyle: { opacity: 0.15 },
      itemStyle: { color: '#409eff' }
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
  currentPage.value = 1
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

<style scoped>
.health-monitor {
  width: 100%;
}
.health-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}
.stat-label {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
  margin-bottom: 4px;
}
.stat-value {
  font-size: 22px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
}
.stat-suffix {
  font-size: 13px;
  font-weight: 400;
  margin-left: 4px;
  color: rgba(0, 0, 0, 0.65);
}
.health-card {
  margin-bottom: 16px;
}
.card-title {
  font-size: 14px;
  font-weight: 500;
}
.health-pagination {
  margin-top: 12px;
  justify-content: flex-end;
}
.status-inline {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
</style>
