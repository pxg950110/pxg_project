<template>
  <PageContainer title="部署详情" :loading="loading">
    <template #extra>
      <div class="flex items-center gap-2">
        <el-button v-if="deployment?.status === 'RUNNING'" type="danger" @click="handleStop">停止</el-button>
        <el-button @click="router.back()">返回</el-button>
      </div>
    </template>

    <template v-if="deployment">
      <el-card shadow="never" class="mb-4 !rounded-xl !border-slate-200/80 shadow-clinical-sm">
        <template #header>
          <span class="font-semibold text-slate-900">基本信息</span>
        </template>
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="部署名称">{{ deployment.name }}</el-descriptions-item>
          <el-descriptions-item label="模型">{{ deployment.model_name }}</el-descriptions-item>
          <el-descriptions-item label="版本">{{ deployment.version_no }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusBadge :status="deployment.status" type="deploy" />
          </el-descriptions-item>
          <el-descriptions-item label="副本数">{{ deployment.replicas }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(deployment.created_at) }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-4">
        <MetricCard title="今日推理" :value="metrics.todayInference" />
        <MetricCard title="平均延迟" :value="metrics.avgLatency" suffix="ms" />
        <MetricCard title="成功率" :value="metrics.successRate" suffix="%" />
        <MetricCard title="GPU利用率" :value="metrics.gpuUsage" suffix="%" />
      </div>

      <el-card shadow="never" class="!rounded-xl !border-slate-200/80 shadow-clinical-sm">
        <template #header>
          <span class="font-semibold text-slate-900">推理趋势（24h）</span>
        </template>
        <MetricChart :option="trendChartOption" height="300px" />
      </el-card>
    </template>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageContainer from '@/components/PageContainer/index.vue'
import StatusBadge from '@/components/StatusBadge/index.vue'
import MetricCard from '@/components/MetricCard/index.vue'
import MetricChart from '@/components/MetricChart/index.vue'
import { getDeploymentStatus, stopDeployment, getDeploymentMetrics } from '@/api/model'
import { formatDateTime } from '@/utils/date'

const route = useRoute()
const router = useRouter()
const deployment = ref<any>(null)
const loading = ref(false)

const metrics = reactive({ todayInference: 0, avgLatency: 0, successRate: 0, gpuUsage: 0 })

const trendChartOption = ref({
  tooltip: { trigger: 'axis' as const },
  xAxis: { type: 'category' as const, data: Array.from({ length: 24 }, (_, i) => `${i}:00`) },
  yAxis: { type: 'value' as const },
  series: [{ type: 'line' as const, data: [] as number[], smooth: true, areaStyle: { opacity: 0.3 }, lineStyle: { color: '#0ea5e9' }, itemStyle: { color: '#0ea5e9' } }],
})

async function loadDeployment() {
  loading.value = true
  try {
    const res = await getDeploymentStatus(Number(route.params.id))
    deployment.value = res.data.data
    // Load metrics
    const endTime = new Date()
    const startTime = new Date(endTime.getTime() - 24 * 60 * 60 * 1000)
    try {
      const metricsRes = await getDeploymentMetrics(Number(route.params.id), {
        start_time: startTime.toISOString(),
        end_time: endTime.toISOString(),
        interval: '1h',
      })
      const metricsData = metricsRes.data.data
      if (metricsData) {
        metrics.todayInference = metricsData.todayInference || 0
        metrics.avgLatency = metricsData.avgLatency || 0
        metrics.successRate = metricsData.successRate || 0
        metrics.gpuUsage = metricsData.gpuUsage || 0
        if (metricsData.trendData) {
          trendChartOption.value.xAxis.data = metricsData.trendData.timestamps || Array.from({ length: 24 }, (_, i) => `${i}:00`)
          trendChartOption.value.series[0].data = metricsData.trendData.values || []
        }
      }
    } catch {
      // Metrics not available - keep defaults
    }
  } finally { loading.value = false }
}

async function confirmStop() {
  await stopDeployment(Number(route.params.id))
  ElMessage.success('停止中...')
  loadDeployment()
}

function handleStop() {
  ElMessageBox.confirm('确认停止此部署？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
    confirmButtonClass: 'el-button--danger',
  }).then(() => confirmStop()).catch(() => {})
}

onMounted(loadDeployment)
</script>
