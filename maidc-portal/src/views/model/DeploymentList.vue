<template>
  <PageContainer title="部署监控" subtitle="实时监控所有模型部署状态与推理性能">
    <!-- Time Filter + Auto-refresh -->
    <div class="filter-bar">
      <a-radio-group v-model:value="timeRange" button-style="solid">
        <a-radio-button value="1h">近1h</a-radio-button>
        <a-radio-button value="6h">近6h</a-radio-button>
        <a-radio-button value="24h">近24h</a-radio-button>
        <a-radio-button value="7d">近7d</a-radio-button>
      </a-radio-group>
      <div class="auto-refresh">
        <span class="refresh-dot"></span>
        <span>自动刷新 30s</span>
      </div>
    </div>

    <!-- 4 Metric Cards -->
    <a-row :gutter="16" class="metric-row">
      <a-col :span="6">
        <MetricCard title="部署实例" :value="summary.total" :icon="RocketOutlined" :loading="loading" />
      </a-col>
      <a-col :span="6">
        <MetricCard title="运行中" :value="summary.running" :icon="ThunderboltOutlined" :loading="loading" />
      </a-col>
      <a-col :span="6">
        <MetricCard title="已停止" :value="summary.stopped" :icon="ClockCircleOutlined" :loading="loading" />
      </a-col>
      <a-col :span="6">
        <MetricCard title="异常" :value="summary.failed" :icon="DashboardOutlined" :loading="loading" />
      </a-col>
    </a-row>

    <!-- QPS Trend Chart -->
    <a-card title="QPS 趋势" class="section-card">
      <MetricChart :option="qpsChartOption" height="320px" />
    </a-card>

    <!-- Deployment Status Panel -->
    <a-card title="部署状态" class="section-card" :loading="loading">
      <div class="deployment-list">
        <div v-for="item in deployments" :key="item.id" class="deployment-item">
          <div class="deployment-left">
            <span class="status-dot" :style="{ backgroundColor: item.color }"></span>
            <div class="deployment-info">
              <span class="deployment-name">{{ item.name }} <span class="deployment-version">{{ item.version }}</span></span>
              <span class="deployment-detail">{{ item.status }} &middot; {{ item.detail }}</span>
            </div>
          </div>
          <span class="status-badge" :style="{ color: item.color, borderColor: item.color }">{{ item.status }}</span>
        </div>
      </div>
    </a-card>

    <!-- Alert Table -->
    <a-card title="告警规则" class="section-card">
      <template #extra>
        <a-button type="primary" size="small">
          <PlusOutlined /> 新建规则
        </a-button>
      </template>
      <a-table :columns="alertColumns" :data-source="alerts" :pagination="false" row-key="rule" size="middle">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="alertStatusColorMap[record.status]">{{ record.status }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a>编辑</a>
              <a>禁用</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  RocketOutlined,
  ThunderboltOutlined,
  ClockCircleOutlined,
  DashboardOutlined,
  PlusOutlined,
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import MetricCard from '@/components/MetricCard/index.vue'
import MetricChart from '@/components/MetricChart/index.vue'
import { getDeployments, scaleDeployment, restartDeployment } from '@/api/model'

// Time range filter
const timeRange = ref<string>('24h')
const loading = ref(false)
const deploymentData = ref<any[]>([])

const summary = reactive({
  total: 0,
  running: 0,
  stopped: 0,
  failed: 0,
})

// QPS chart option
const qpsChartOption = {
  tooltip: { trigger: 'axis' },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: {
    type: 'category',
    data: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', '24:00'],
  },
  yAxis: { type: 'value', name: 'QPS' },
  series: [
    {
      type: 'bar',
      data: [120, 85, 340, 580, 620, 450, 380],
      itemStyle: { color: '#1677ff', borderRadius: [4, 4, 0, 0] },
    },
  ],
}

// Deployment status items via API
interface DeploymentItem {
  id: number
  name: string
  version: string
  status: string
  color: string
  detail: string
}

const statusColorMap: Record<string, string> = {
  RUNNING: '#52c41a',
  STOPPED: '#ff4d4f',
  FAILED: '#faad14',
  CREATING: '#1677ff',
  SCALING: '#1677ff',
  STOPPING: '#faad14',
}

const deployments = computed<DeploymentItem[]>(() =>
  deploymentData.value.map((item: any) => ({
    id: item.id,
    name: item.deploymentName || item.name,
    version: item.version || '--',
    status: item.status,
    color: statusColorMap[item.status] || '#d9d9d9',
    detail: `${item.environment || '--'} · 副本: ${item.replicas || 1}`,
  }))
)

async function fetchData() {
  loading.value = true
  try {
    const res = await getDeployments({ page: 1, page_size: 100 })
    const data = res.data.data
    deploymentData.value = Array.isArray(data) ? data : (data?.items || [])
    summary.total = deploymentData.value.length
    summary.running = deploymentData.value.filter(d => d.status === 'RUNNING').length
    summary.stopped = deploymentData.value.filter(d => d.status === 'STOPPED').length
    summary.failed = deploymentData.value.filter(d => d.status === 'FAILED').length
  } finally {
    loading.value = false
  }
}

// Alert table columns
const alertColumns = [
  { title: '规则名称', dataIndex: 'rule', key: 'rule' },
  { title: '部署', dataIndex: 'deployment', key: 'deployment' },
  { title: '指标', dataIndex: 'metric', key: 'metric' },
  { title: '阈值', dataIndex: 'threshold', key: 'threshold' },
  { title: '当前值', dataIndex: 'current', key: 'current' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '时间', dataIndex: 'time', key: 'time' },
  { title: '操作', key: 'action', width: 120 },
]

// Alert data (to be connected to alert API later)
const alerts = ref<any[]>([])

// Alert status color mapping
const alertStatusColorMap: Record<string, string> = {
  Firing: 'red',
  Warning: 'orange',
  Resolved: 'green',
}

async function handleRestart(id: number) {
  try {
    await restartDeployment(id)
    message.success('重启成功')
    fetchData()
  } catch {
    message.error('重启失败')
  }
}

async function handleScale(id: number, replicas: number) {
  try {
    await scaleDeployment(id, replicas)
    message.success('扩缩容成功')
    fetchData()
  } catch {
    message.error('扩缩容失败')
  }
}

onMounted(() => fetchData())
</script>

<style scoped>
.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.auto-refresh {
  display: flex;
  align-items: center;
  gap: 6px;
  color: rgba(0, 0, 0, 0.45);
  font-size: 14px;
}

.refresh-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #52c41a;
  display: inline-block;
}

.metric-row {
  margin-bottom: 16px;
}

.section-card {
  margin-bottom: 16px;
}

.deployment-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.deployment-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  transition: box-shadow 0.2s;
}

.deployment-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.deployment-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.deployment-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.deployment-name {
  font-size: 14px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.88);
}

.deployment-version {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  font-weight: 400;
}

.deployment-detail {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.status-badge {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  border: 1px solid;
  line-height: 20px;
}
</style>
