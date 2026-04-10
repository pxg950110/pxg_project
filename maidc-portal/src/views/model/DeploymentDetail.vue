<template>
  <PageContainer title="部署详情" :loading="loading">
    <template #extra>
      <a-space>
        <a-button v-if="deployment?.status === 'RUNNING'" danger @click="handleStop">停止</a-button>
        <a-button @click="router.back()">返回</a-button>
      </a-space>
    </template>

    <template v-if="deployment">
      <a-card title="基本信息" style="margin-bottom: 16px">
        <a-descriptions :column="3" bordered size="small">
          <a-descriptions-item label="部署名称">{{ deployment.name }}</a-descriptions-item>
          <a-descriptions-item label="模型">{{ deployment.model_name }}</a-descriptions-item>
          <a-descriptions-item label="版本">{{ deployment.version_no }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <StatusBadge :status="deployment.status" type="deploy" />
          </a-descriptions-item>
          <a-descriptions-item label="副本数">{{ deployment.replicas }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ formatDateTime(deployment.created_at) }}</a-descriptions-item>
        </a-descriptions>
      </a-card>

      <a-row :gutter="16" style="margin-bottom: 16px">
        <a-col :span="6"><MetricCard title="今日推理" :value="metrics.todayInference" /></a-col>
        <a-col :span="6"><MetricCard title="平均延迟" :value="metrics.avgLatency" suffix="ms" /></a-col>
        <a-col :span="6"><MetricCard title="成功率" :value="metrics.successRate" suffix="%" /></a-col>
        <a-col :span="6"><MetricCard title="GPU利用率" :value="metrics.gpuUsage" suffix="%" /></a-col>
      </a-row>

      <a-card title="推理趋势（24h）">
        <MetricChart :option="trendChartOption" height="300px" />
      </a-card>
    </template>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import StatusBadge from '@/components/StatusBadge/index.vue'
import MetricCard from '@/components/MetricCard/index.vue'
import MetricChart from '@/components/MetricChart/index.vue'
import { getDeploymentStatus, stopDeployment } from '@/api/model'
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
  series: [{ type: 'line', data: Array.from({ length: 24 }, () => Math.floor(Math.random() * 200 + 50)), smooth: true, areaStyle: { opacity: 0.3 }, lineStyle: { color: '#1677ff' }, itemStyle: { color: '#1677ff' } }],
})

async function loadDeployment() {
  loading.value = true
  try {
    const res = await getDeploymentStatus(Number(route.params.id))
    deployment.value = res.data.data
  } finally { loading.value = false }
}

async function handleStop() {
  Modal.confirm({ title: '确认停止此部署？', async onOk() {
    await stopDeployment(Number(route.params.id))
    message.success('停止中...')
    loadDeployment()
  }})
}

onMounted(loadDeployment)
</script>
