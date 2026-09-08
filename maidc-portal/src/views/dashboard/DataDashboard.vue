<template>
  <PageContainer title="数据看板" subtitle="临床数据与研究数据监控">
    <!-- Top Row: Metric Cards -->
    <a-row :gutter="[16, 16]">
      <a-col :span="6">
        <MetricCard
          title="患者总数"
          :value="metrics.totalPatients"
          suffix="人"
          :trend="{ value: 5, type: 'up' }"
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
import { ref, reactive, onMounted } from 'vue'
import {
  TeamOutlined,
  ProjectOutlined,
  DatabaseOutlined,
  SyncOutlined,
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import MetricCard from '@/components/MetricCard/index.vue'
import MetricChart from '@/components/MetricChart/index.vue'
import StatusBadge from '@/components/StatusBadge/index.vue'
import { getPatients, getProjects, getDatasets, getDataGrowthTrend, getDataSourceDistribution } from '@/api/data'
import { getEtlExecutions } from '@/api/etl'

const loading = ref(false)

// ============ Metrics ============
const metrics = reactive({
  totalPatients: 0,
  researchProjects: 0,
  datasets: 0,
  etlTasks: 0,
})

// ============ ETL Table ============
const etlColumns = [
  { title: '任务名', dataIndex: 'name', key: 'name' },
  { title: '类型', dataIndex: 'type', key: 'type' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '数据量', dataIndex: 'dataSize', key: 'dataSize' },
  { title: '完成时间', dataIndex: 'completedAt', key: 'completedAt' },
]

const etlTasks = ref<any[]>([])

// ============ Data Growth Chart ============
const dataGrowthOption = ref({
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
    data: [] as string[],
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
      data: [] as number[],
      itemStyle: { color: '#1677ff' },
    },
    {
      name: '研究数据',
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.12 },
      data: [] as number[],
      itemStyle: { color: '#52c41a' },
    },
    {
      name: '影像数据',
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.10 },
      data: [] as number[],
      itemStyle: { color: '#722ed1' },
    },
  ],
})

// ============ Data Source Distribution Pie Chart ============
const dataSourceOption = ref({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0 },
  series: [
    {
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}: {d}%' },
      data: [] as { value: number; name: string; itemStyle?: { color: string } }[],
    },
  ],
})

const PIE_COLORS = ['#1677ff', '#52c41a', '#722ed1', '#faad14', '#13c2c2', '#eb2f96']

// ============ Fetch All Data ============
async function fetchDashboardData() {
  loading.value = true
  try {
    const [patientsRes, projectsRes, datasetsRes, executionsRes, growthRes, distributionRes] = await Promise.all([
      getPatients({ page: 1, page_size: 1 }).catch(() => ({ data: { data: { total: 0 } } })),
      getProjects({ page: 1, page_size: 1 }).catch(() => ({ data: { data: { total: 0 } } })),
      getDatasets({ page: 1, page_size: 1 }).catch(() => ({ data: { data: { total: 0 } } })),
      getEtlExecutions({ page: 1, page_size: 5 }).catch(() => ({ data: { data: { total: 0, items: [] } } })),
      getDataGrowthTrend({ months: 6 }).catch(() => ({ data: { data: null } })),
      getDataSourceDistribution().catch(() => ({ data: { data: [] } })),
    ])

    // Metric cards
    metrics.totalPatients = (patientsRes as any).data?.data?.total ?? 0
    metrics.researchProjects = (projectsRes as any).data?.data?.total ?? 0
    metrics.datasets = (datasetsRes as any).data?.data?.total ?? 0
    metrics.etlTasks = (executionsRes as any).data?.data?.total ?? 0

    // ETL task table
    const execItems = (executionsRes as any).data?.data?.items ?? []
    etlTasks.value = execItems.map((item: any) => ({
      id: item.id,
      name: item.pipeline_name ?? item.pipelineName ?? '--',
      type: item.trigger_type ?? item.triggerType ?? '--',
      status: item.status,
      statusLabel: (item.status ?? 'UNKNOWN').toUpperCase(),
      statusType: 'sync' as const,
      dataSize: item.rows_processed ?? item.rowsProcessed ?? 0,
      completedAt: item.end_time ?? item.endTime ?? '--',
    }))

    // Data growth chart
    const growthData = (growthRes as any).data?.data
    if (growthData) {
      dataGrowthOption.value = {
        ...dataGrowthOption.value,
        xAxis: {
          ...dataGrowthOption.value.xAxis,
          data: growthData.months ?? [],
        },
        series: [
          { ...dataGrowthOption.value.series[0], data: growthData.clinical ?? [] },
          { ...dataGrowthOption.value.series[1], data: growthData.research ?? [] },
          { ...dataGrowthOption.value.series[2], data: growthData.imaging ?? [] },
        ],
      }
    }

    // Data source distribution pie chart
    const distData = (distributionRes as any).data?.data
    if (Array.isArray(distData) && distData.length > 0) {
      dataSourceOption.value = {
        ...dataSourceOption.value,
        series: [{
          ...dataSourceOption.value.series[0],
          data: distData.map((item: any, index: number) => ({
            value: item.value,
            name: item.name,
            itemStyle: { color: PIE_COLORS[index % PIE_COLORS.length] },
          })),
        }],
      }
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchDashboardData()
})
</script>

<style scoped>
</style>
