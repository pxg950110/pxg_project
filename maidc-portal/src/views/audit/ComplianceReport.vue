<template>
  <PageContainer title="合规报表">
    <!-- Summary Cards -->
    <el-row :gutter="16">
      <el-col :span="6">
        <MetricCard title="审计覆盖率" :value="summary.coverageRate" suffix="%" :icon="Document" />
      </el-col>
      <el-col :span="6">
        <MetricCard title="合规得分" :value="summary.score" suffix="分" :icon="CircleCheck" />
      </el-col>
      <el-col :span="6">
        <MetricCard title="待整改项" :value="summary.pendingItems" :icon="Warning" />
      </el-col>
      <el-col :span="6">
        <MetricCard title="审计周期" :value="summary.auditPeriod" :icon="Calendar" />
      </el-col>
    </el-row>

    <!-- Charts Row -->
    <el-row :gutter="16" class="mt-4">
      <el-col :span="12">
        <el-card shadow="never" class="!rounded-xl !border-slate-200/80 shadow-clinical-sm">
          <template #header>
            <span class="font-semibold text-slate-900">操作类型分布</span>
          </template>
          <div ref="pieChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="!rounded-xl !border-slate-200/80 shadow-clinical-sm">
          <template #header>
            <span class="font-semibold text-slate-900">合规趋势</span>
          </template>
          <div ref="lineChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Report Table -->
    <el-card shadow="never" class="mt-4 !rounded-xl !border-slate-200/80 shadow-clinical-sm">
      <template #header>
        <span class="font-semibold text-slate-900">合规检查报告</span>
      </template>
      <el-table :data="reports" row-key="id" size="small">
        <el-table-column label="检查项" min-width="250">
          <template #default="{ row }">
            <span class="font-semibold">{{ row.check_item }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类别" prop="category" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="toTagType(row.status_color)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="得分" prop="score" width="100" />
        <el-table-column label="最后检查时间" prop="last_check" width="170" />
      </el-table>
    </el-card>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { Document, CircleCheck, Warning, Calendar } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import MetricCard from '@/components/MetricCard/index.vue'
import { getComplianceReport } from '@/api/audit'

const loading = ref(false)
const summary = reactive({
  coverageRate: 0,
  score: 0,
  pendingItems: 0,
  auditPeriod: '-',
})
const reports = ref<any[]>([])

// --- AntD color name → el-tag type (backend sends status_color as AntD names) ---
function toTagType(color?: string): 'danger' | 'warning' | 'success' | 'primary' | 'info' {
  switch (color) {
    case 'red': return 'danger'
    case 'orange':
    case 'gold':
    case 'volcano': return 'warning'
    case 'green': return 'success'
    case 'blue':
    case 'geekblue': return 'primary'
    default: return 'info'
  }
}

// ============ Chart Refs ============
const pieChartRef = ref<HTMLElement>()
const lineChartRef = ref<HTMLElement>()
let pieChart: echarts.ECharts | null = null
let lineChart: echarts.ECharts | null = null

function initCharts() {
  if (pieChartRef.value) {
    pieChart = echarts.init(pieChartRef.value)
  }
  if (lineChartRef.value) {
    lineChart = echarts.init(lineChartRef.value)
  }
}

function updatePieChart(data: { value: number; name: string }[]) {
  if (!pieChart) return
  const colors = ['#0ea5e9', '#10b981', '#f59e0b', '#ef4444', '#94a3b8']
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, data: data.map(d => d.name) },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{d}%' },
      data: data.map((d, i) => ({ ...d, itemStyle: { color: colors[i % colors.length] } })),
    }],
  })
}

function updateLineChart(months: string[], scores: number[]) {
  if (!lineChart) return
  lineChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: months },
    yAxis: { type: 'value', min: 0, max: 100, name: '得分' },
    series: [{
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      data: scores,
      itemStyle: { color: '#0ea5e9' },
      lineStyle: { color: '#0ea5e9' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(14, 165, 233, 0.25)' },
          { offset: 1, color: 'rgba(14, 165, 233, 0.02)' },
        ]),
      },
    }],
  })
}

async function fetchReport() {
  loading.value = true
  try {
    const now = new Date()
    const year = now.getFullYear()
    const endDate = `${year}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
    const res = await getComplianceReport({
      startTime: `${year}-01-01T00:00:00`,
      endTime: `${endDate}T23:59:59`,
    })
    const data = res.data.data
    if (data) {
      const total = data.totalOperations ?? 0
      summary.coverageRate = total > 0 ? 100 : 0
      summary.score = data.successRate ? parseFloat(data.successRate) : 100
      summary.pendingItems = data.criticalEvents ?? 0
      summary.auditPeriod = `${year}-01-01 ~ ${endDate}`
      reports.value = data.checkItems ?? []
      if (data.operationTypeDistribution) {
        updatePieChart(data.operationTypeDistribution)
      }
      if (data.complianceTrend) {
        const trend = data.complianceTrend
        updateLineChart(trend.months || [], trend.scores || [])
      }
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  initCharts()
  fetchReport()
})

onUnmounted(() => {
  pieChart?.dispose()
  lineChart?.dispose()
})
</script>
