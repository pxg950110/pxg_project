<template>
  <PageContainer title="合规报表">
    <!-- Summary Cards -->
    <a-row :gutter="[16, 16]">
      <a-col :span="6">
        <div class="summary-card green">
          <div class="card-icon green">
            <FileProtectOutlined />
          </div>
          <div class="card-title">审计覆盖率</div>
          <div class="card-value">
            {{ summary.coverageRate }}<span class="card-unit">%</span>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card green">
          <div class="card-icon green">
            <SafetyCertificateOutlined />
          </div>
          <div class="card-title">合规得分</div>
          <div class="card-value">
            {{ summary.score }}<span class="card-unit">分</span>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card orange">
          <div class="card-icon orange">
            <ExclamationCircleOutlined />
          </div>
          <div class="card-title">待整改项</div>
          <div class="card-value">{{ summary.pendingItems }}</div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card blue">
          <div class="card-icon blue">
            <CalendarOutlined />
          </div>
          <div class="card-title">审计周期</div>
          <div class="card-value" style="font-size: 24px">{{ summary.auditPeriod }}</div>
        </div>
      </a-col>
    </a-row>

    <!-- Charts Row -->
    <a-row :gutter="[16, 16]" style="margin-top: 16px">
      <a-col :span="12">
        <a-card title="操作类型分布" :bordered="false">
          <div ref="pieChartRef" style="height: 300px"></div>
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card title="合规趋势" :bordered="false">
          <div ref="lineChartRef" style="height: 300px"></div>
        </a-card>
      </a-col>
    </a-row>

    <!-- Report Table -->
    <a-card title="合规检查报告" :bordered="false" style="margin-top: 16px">
      <a-table
        :columns="columns"
        :data-source="reports"
        :pagination="false"
        row-key="id"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'check_item'">
            <span style="font-weight: 600">{{ record.check_item }}</span>
          </template>
          <template v-if="column.dataIndex === 'status'">
            <a-tag :color="record.status_color">{{ record.status }}</a-tag>
          </template>
        </template>
      </a-table>
    </a-card>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import {
  FileProtectOutlined,
  SafetyCertificateOutlined,
  ExclamationCircleOutlined,
  CalendarOutlined,
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import { getComplianceReport } from '@/api/audit'

const loading = ref(false)
const summary = reactive({
  coverageRate: 0,
  score: 0,
  pendingItems: 0,
  auditPeriod: '-',
})
const reports = ref<any[]>([])

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
  const colors = ['#1677ff', '#52c41a', '#faad14', '#ff4d4f', '#8c8c8c']
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
      itemStyle: { color: '#1677ff' },
      lineStyle: { color: '#1677ff' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(22, 119, 255, 0.25)' },
          { offset: 1, color: 'rgba(22, 119, 255, 0.02)' },
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

const columns = [
  { title: '检查项', dataIndex: 'check_item', width: 250 },
  { title: '类别', dataIndex: 'category', width: 120 },
  { title: '状态', dataIndex: 'status', width: 100 },
  { title: '得分', dataIndex: 'score', width: 100 },
  { title: '最后检查时间', dataIndex: 'last_check', width: 170 },
]
</script>

<style scoped>
.summary-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.summary-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
}

.summary-card.green::before {
  background: #52c41a;
}

.summary-card.orange::before {
  background: #faad14;
}

.summary-card.blue::before {
  background: #1677ff;
}

.card-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  margin-bottom: 12px;
}

.card-icon.green {
  background: #f6ffed;
  color: #52c41a;
}

.card-icon.orange {
  background: #fffbe6;
  color: #faad14;
}

.card-icon.blue {
  background: #e6f4ff;
  color: #1677ff;
}

.card-title {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.45);
  margin-bottom: 4px;
}

.card-value {
  font-size: 28px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
}

.card-unit {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.45);
  font-weight: 400;
}
</style>
