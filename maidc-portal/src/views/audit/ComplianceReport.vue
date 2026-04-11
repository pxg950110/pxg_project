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
            98.5<span class="card-unit">%</span>
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
            92<span class="card-unit">分</span>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card orange">
          <div class="card-icon orange">
            <ExclamationCircleOutlined />
          </div>
          <div class="card-title">待整改项</div>
          <div class="card-value">3</div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card blue">
          <div class="card-icon blue">
            <CalendarOutlined />
          </div>
          <div class="card-title">审计周期</div>
          <div class="card-value" style="font-size: 24px">2026-Q1</div>
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
        :data-source="mockReports"
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
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import {
  FileProtectOutlined,
  SafetyCertificateOutlined,
  ExclamationCircleOutlined,
  CalendarOutlined,
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'

// ============ Chart Refs ============
const pieChartRef = ref<HTMLElement>()
const lineChartRef = ref<HTMLElement>()
let pieChart: echarts.ECharts | null = null
let lineChart: echarts.ECharts | null = null

onMounted(() => {
  if (pieChartRef.value) {
    pieChart = echarts.init(pieChartRef.value)
    pieChart.setOption({
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c} ({d}%)',
      },
      legend: {
        bottom: 0,
        data: ['查询', '创建', '更新', '删除', '其他'],
      },
      series: [
        {
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 6,
            borderColor: '#fff',
            borderWidth: 2,
          },
          label: {
            show: true,
            formatter: '{b}\n{d}%',
          },
          data: [
            { value: 45, name: '查询', itemStyle: { color: '#1677ff' } },
            { value: 20, name: '创建', itemStyle: { color: '#52c41a' } },
            { value: 18, name: '更新', itemStyle: { color: '#faad14' } },
            { value: 8, name: '删除', itemStyle: { color: '#ff4d4f' } },
            { value: 9, name: '其他', itemStyle: { color: '#8c8c8c' } },
          ],
        },
      ],
    })
  }

  if (lineChartRef.value) {
    lineChart = echarts.init(lineChartRef.value)
    lineChart.setOption({
      tooltip: {
        trigger: 'axis',
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true,
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: ['2026-01', '2026-02', '2026-03', '2026-04'],
      },
      yAxis: {
        type: 'value',
        min: 0,
        max: 100,
        name: '得分',
      },
      series: [
        {
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 8,
          data: [88, 90, 91, 92],
          itemStyle: { color: '#1677ff' },
          lineStyle: { color: '#1677ff' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(22, 119, 255, 0.25)' },
              { offset: 1, color: 'rgba(22, 119, 255, 0.02)' },
            ]),
          },
        },
      ],
    })
  }
})

onUnmounted(() => {
  pieChart?.dispose()
  lineChart?.dispose()
})

// ============ Report Table ============
const columns = [
  { title: '检查项', dataIndex: 'check_item', width: 250 },
  { title: '类别', dataIndex: 'category', width: 120 },
  { title: '状态', dataIndex: 'status', width: 100 },
  { title: '得分', dataIndex: 'score', width: 100 },
  { title: '最后检查时间', dataIndex: 'last_check', width: 170 },
]

const mockReports = [
  { id: 1, check_item: '数据访问权限控制', category: '访问控制', status: '合格', status_color: 'green', score: 95, last_check: '2026-04-12 08:00' },
  { id: 2, check_item: '审计日志完整性', category: '审计追踪', status: '合格', status_color: 'green', score: 100, last_check: '2026-04-12 08:00' },
  { id: 3, check_item: '数据加密传输', category: '数据安全', status: '合格', status_color: 'green', score: 98, last_check: '2026-04-11 20:00' },
  { id: 4, check_item: '定期访问审查', category: '访问控制', status: '待整改', status_color: 'orange', score: 72, last_check: '2026-04-10 14:00' },
  { id: 5, check_item: '数据保留策略执行', category: '数据管理', status: '合格', status_color: 'green', score: 90, last_check: '2026-04-09 10:00' },
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
