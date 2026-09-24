<template>
  <PageContainer title="部署监控" subtitle="实时监控所有模型部署状态与推理性能">
    <!-- Time Filter + Auto-refresh -->
    <div class="filter-bar">
      <el-radio-group v-model="timeRange">
        <el-radio-button value="1h">近1h</el-radio-button>
        <el-radio-button value="6h">近6h</el-radio-button>
        <el-radio-button value="24h">近24h</el-radio-button>
        <el-radio-button value="7d">近7d</el-radio-button>
      </el-radio-group>
      <div class="auto-refresh">
        <span class="refresh-dot"></span>
        <span>自动刷新 30s</span>
      </div>
    </div>

    <!-- 4 Metric Cards -->
    <div class="metric-row grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <MetricCard title="部署实例" :value="summary.total" :icon="Promotion" :loading="loading" />
      <MetricCard title="运行中" :value="summary.running" :icon="MagicStick" :loading="loading" />
      <MetricCard title="已停止" :value="summary.stopped" :icon="Clock" :loading="loading" />
      <MetricCard title="异常" :value="summary.failed" :icon="DataBoard" :loading="loading" />
    </div>

    <!-- QPS Trend Chart -->
    <el-card shadow="never" class="section-card !rounded-xl !border-slate-200/80 shadow-clinical-sm">
      <template #header>
        <span class="font-semibold text-slate-900">QPS 趋势</span>
      </template>
      <MetricChart :option="qpsChartOption" height="320px" />
    </el-card>

    <!-- Deployment Status Panel -->
    <el-card shadow="never" class="section-card !rounded-xl !border-slate-200/80 shadow-clinical-sm">
      <template #header>
        <span class="font-semibold text-slate-900">部署状态</span>
      </template>
      <div class="deployment-list" v-loading="loading">
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
    </el-card>

    <!-- Alert Table -->
    <el-card shadow="never" class="section-card !rounded-xl !border-slate-200/80 shadow-clinical-sm">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-semibold text-slate-900">告警规则</span>
          <el-button type="primary" size="small">
            <el-icon class="mr-1"><Plus /></el-icon>新建规则
          </el-button>
        </div>
      </template>
      <el-table :data="alerts" row-key="rule" size="default">
        <el-table-column label="规则名称" prop="rule" />
        <el-table-column label="部署" prop="deployment" />
        <el-table-column label="指标" prop="metric" />
        <el-table-column label="阈值" prop="threshold" />
        <el-table-column label="当前值" prop="current" />
        <el-table-column label="状态" prop="status">
          <template #default="{ row }">
            <el-tag :type="alertStatusColorMap[row.status]">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" prop="time" />
        <el-table-column label="操作" width="120">
          <template #default>
            <div class="flex items-center gap-2">
              <el-button link type="primary">编辑</el-button>
              <el-button link type="primary">禁用</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { DataBoard, MagicStick, Clock, Plus, Promotion } from '@element-plus/icons-vue'
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
      itemStyle: { color: '#0ea5e9', borderRadius: [4, 4, 0, 0] },
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
  RUNNING: '#10b981',
  STOPPED: '#ef4444',
  FAILED: '#f59e0b',
  CREATING: '#0ea5e9',
  SCALING: '#0ea5e9',
  STOPPING: '#f59e0b',
}

const deployments = computed<DeploymentItem[]>(() =>
  deploymentData.value.map((item: any) => ({
    id: item.id,
    name: item.deploymentName || item.name,
    version: item.version || '--',
    status: item.status,
    color: statusColorMap[item.status] || '#e2e8f0',
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

// Alert data (to be connected to alert API later)
const alerts = ref<any[]>([])

// Alert status tag type mapping
const alertStatusColorMap: Record<string, string> = {
  Firing: 'danger',
  Warning: 'warning',
  Resolved: 'success',
}

async function handleRestart(id: number) {
  try {
    await restartDeployment(id)
    ElMessage.success('重启成功')
    fetchData()
  } catch {
    ElMessage.error('重启失败')
  }
}

async function handleScale(id: number, replicas: number) {
  try {
    await scaleDeployment(id, replicas)
    ElMessage.success('扩缩容成功')
    fetchData()
  } catch {
    ElMessage.error('扩缩容失败')
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
  color: #94a3b8;
  font-size: 14px;
}

.refresh-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #10b981;
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
  min-height: 60px;
}

.deployment-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border: 1px solid #f1f5f9;
  border-radius: 6px;
  transition: box-shadow 0.2s;
}

.deployment-item:hover {
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.06);
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
  color: #0f172a;
}

.deployment-version {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 400;
}

.deployment-detail {
  font-size: 12px;
  color: #94a3b8;
}

.status-badge {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  border: 1px solid;
  line-height: 20px;
}
</style>
