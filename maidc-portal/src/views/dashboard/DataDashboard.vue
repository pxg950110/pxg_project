<template>
  <PageContainer title="数据看板" subtitle="临床数据与研究数据监控">
    <!-- Top Row: Metric Cards -->
    <a-row :gutter="[16, 16]">
      <a-col :span="6">
        <MetricCard
          title="患者总数"
          :value="metrics.totalPatients"
          suffix="人"
          :trend="{ value: 234, type: 'up' }"
          :loading="loading"
        >
          <template #icon><TeamOutlined /></template>
        </MetricCard>
      </a-col>
      <a-col :span="6">
        <MetricCard
          title="研究项目"
          :value="metrics.researchProjects"
          suffix="个"
          :trend="{ value: 3, type: 'up' }"
          :loading="loading"
        >
          <template #icon><ProjectOutlined /></template>
        </MetricCard>
      </a-col>
      <a-col :span="6">
        <MetricCard
          title="数据集"
          :value="metrics.datasets"
          suffix="个"
          :loading="loading"
        >
          <template #icon><DatabaseOutlined /></template>
        </MetricCard>
      </a-col>
      <a-col :span="6">
        <MetricCard
          title="ETL任务"
          :value="metrics.etlTasks"
          suffix="个"
          :trend="{ value: 5, type: 'up' }"
          :loading="loading"
        >
          <template #icon><SyncOutlined /></template>
        </MetricCard>
      </a-col>
    </a-row>

    <!-- Middle Row: Data Growth + Source Distribution -->
    <a-row :gutter="[16, 16]" style="margin-top: 16px">
      <a-col :span="12">
        <a-card title="数据增长趋势（月度）" :bordered="false">
          <MetricChart :option="dataGrowthOption" :height="320" />
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card title="数据来源分布" :bordered="false">
          <MetricChart :option="dataSourceOption" :height="320" />
        </a-card>
      </a-col>
    </a-row>

    <!-- Bottom Row: ETL Task Table -->
    <a-row style="margin-top: 16px">
      <a-col :span="24">
        <a-card title="最近 ETL 任务" :bordered="false">
          <a-table
            :columns="etlColumns"
            :data-source="etlTasks"
            :pagination="{ pageSize: 5, showTotal: (total: number) => `共 ${total} 条` }"
            row-key="id"
            size="middle"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'status'">
                <StatusBadge :status="record.statusLabel" :type="record.statusType" />
              </template>
              <template v-if="column.dataIndex === 'dataSize'">
                {{ record.dataSize }} GB
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import {
  TeamOutlined,
  ProjectOutlined,
  DatabaseOutlined,
  SyncOutlined,
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer'
import MetricCard from '@/components/MetricCard'
import MetricChart from '@/components/MetricChart'
import StatusBadge from '@/components/StatusBadge'

const loading = ref(false)

// ============ Metrics ============
const metrics = reactive({
  totalPatients: 152847,
  researchProjects: 28,
  datasets: 64,
  etlTasks: 156,
})

// ============ ETL Table ============
const etlColumns = [
  { title: '任务名', dataIndex: 'name', key: 'name' },
  { title: '类型', dataIndex: 'type', key: 'type' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '数据量', dataIndex: 'dataSize', key: 'dataSize' },
  { title: '完成时间', dataIndex: 'completedAt', key: 'completedAt' },
]

const etlTasks = ref([
  {
    id: '1',
    name: '影像数据增量同步',
    type: '增量同步',
    statusLabel: '已完成',
    statusType: 'success',
    dataSize: 128.5,
    completedAt: '2026-04-11 09:32:00',
  },
  {
    id: '2',
    name: '检验报告全量导入',
    type: '全量导入',
    statusLabel: '运行中',
    statusType: 'processing',
    dataSize: 45.2,
    completedAt: '--',
  },
  {
    id: '3',
    name: '电子病历数据清洗',
    type: '数据清洗',
    statusLabel: '已完成',
    statusType: 'success',
    dataSize: 86.7,
    completedAt: '2026-04-11 07:15:00',
  },
  {
    id: '4',
    name: '患者基本信息脱敏',
    type: '数据脱敏',
    statusLabel: '已完成',
    statusType: 'success',
    dataSize: 12.3,
    completedAt: '2026-04-10 22:48:00',
  },
  {
    id: '5',
    name: '用药记录增量同步',
    type: '增量同步',
    statusLabel: '失败',
    statusType: 'error',
    dataSize: 0,
    completedAt: '--',
  },
  {
    id: '6',
    name: '病理报告格式转换',
    type: '格式转换',
    statusLabel: '已完成',
    statusType: 'success',
    dataSize: 34.1,
    completedAt: '2026-04-10 18:20:00',
  },
  {
    id: '7',
    name: '基因组数据导入',
    type: '全量导入',
    statusLabel: '排队中',
    statusType: 'warning',
    dataSize: 0,
    completedAt: '--',
  },
  {
    id: '8',
    name: '影像数据标签提取',
    type: '数据清洗',
    statusLabel: '已完成',
    statusType: 'success',
    dataSize: 67.8,
    completedAt: '2026-04-10 14:55:00',
  },
])

// ============ Data Growth Chart ============
const dataGrowthOption = {
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'cross' },
  },
  legend: {
    data: ['临床数据', '研究数据', '影像数据'],
  },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: [
      '2025-10', '2025-11', '2025-12',
      '2026-01', '2026-02', '2026-03', '2026-04',
    ],
  },
  yAxis: {
    type: 'value',
    name: '数据量 (GB)',
    axisLabel: { formatter: '{value}' },
  },
  series: [
    {
      name: '临床数据',
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.15 },
      data: [1240, 1380, 1520, 1650, 1820, 2010, 2230],
      itemStyle: { color: '#1677ff' },
    },
    {
      name: '研究数据',
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.12 },
      data: [680, 750, 830, 920, 1010, 1130, 1280],
      itemStyle: { color: '#52c41a' },
    },
    {
      name: '影像数据',
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.10 },
      data: [2100, 2350, 2600, 2920, 3180, 3540, 3920],
      itemStyle: { color: '#722ed1' },
    },
  ],
}

// ============ Data Source Distribution Pie Chart ============
const dataSourceOption = {
  tooltip: { trigger: 'item' },
  legend: { bottom: 0 },
  series: [
    {
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}: {d}%' },
      data: [
        { value: 3920, name: 'DICOM 影像', itemStyle: { color: '#1677ff' } },
        { value: 2230, name: '电子病历', itemStyle: { color: '#52c41a' } },
        { value: 1280, name: '研究数据集', itemStyle: { color: '#722ed1' } },
        { value: 860, name: '检验报告', itemStyle: { color: '#faad14' } },
        { value: 540, name: '用药记录', itemStyle: { color: '#13c2c2' } },
        { value: 320, name: '基因组数据', itemStyle: { color: '#eb2f96' } },
      ],
    },
  ],
}
</script>

<style scoped>
</style>
