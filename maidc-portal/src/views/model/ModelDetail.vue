<template>
  <PageContainer :loading="loading" :breadcrumb="breadcrumb">
    <template #extra>
      <a-button @click="router.back()">返回</a-button>
    </template>

    <a-tabs v-model:activeKey="activeTab">
      <!-- Tab 1: 基本信息 -->
      <a-tab-pane key="info" tab="基本信息">
        <!-- Top Card: Model Overview -->
        <a-card class="overview-card">
          <a-row :gutter="32">
            <a-col :span="14">
              <div class="model-header">
                <h2 class="model-name">{{ modelInfo.name }}</h2>
                <StatusBadge :status="modelInfo.status" type="model" />
              </div>
              <p class="model-desc">{{ modelInfo.description }}</p>
              <a-space style="margin-top: 16px">
                <a-button type="primary" @click="showEditModal">编辑</a-button>
                <a-button type="primary" @click="showVersionModal">注册新版本</a-button>
              </a-space>
            </a-col>
            <a-col :span="10">
              <a-descriptions :column="1" size="small" bordered>
                <a-descriptions-item label="Model ID">{{ modelInfo.code }}</a-descriptions-item>
                <a-descriptions-item label="类型">{{ modelInfo.type }}</a-descriptions-item>
                <a-descriptions-item label="框架">{{ modelInfo.framework }}</a-descriptions-item>
                <a-descriptions-item label="任务">{{ modelInfo.task }}</a-descriptions-item>
                <a-descriptions-item label="所属项目">{{ modelInfo.project }}</a-descriptions-item>
                <a-descriptions-item label="负责人">{{ modelInfo.owner }}</a-descriptions-item>
                <a-descriptions-item label="创建时间">{{ modelInfo.createdAt }}</a-descriptions-item>
                <a-descriptions-item label="更新时间">{{ modelInfo.updatedAt }}</a-descriptions-item>
                <a-descriptions-item label="最新版本">{{ modelInfo.latestVersion }}</a-descriptions-item>
                <a-descriptions-item label="标签">
                  <a-tag v-for="tag in modelInfo.tags" :key="tag" color="blue">{{ tag }}</a-tag>
                </a-descriptions-item>
              </a-descriptions>
            </a-col>
          </a-row>
        </a-card>

        <!-- Performance Metrics Box -->
        <a-card title="最新评估指标 v2.3.1" style="margin-top: 16px">
          <a-row :gutter="16">
            <a-col v-for="metric in metrics" :key="metric.label" :span="6">
              <a-card size="small" class="metric-card">
                <a-statistic :title="metric.label" :value="metric.value" :suffix="metric.suffix" />
              </a-card>
            </a-col>
          </a-row>
        </a-card>
      </a-tab-pane>

      <!-- Tab 2: 版本列表 -->
      <a-tab-pane key="versions" tab="版本列表">
        <a-card>
          <a-table
            :columns="versionColumns"
            :data-source="versions"
            bordered
            row-key="version"
            size="middle"
            :pagination="false"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <StatusBadge :status="record.status" type="version" />
              </template>
              <template v-if="column.key === 'action'">
                <a-button type="link" size="small">详情</a-button>
                <a-divider type="vertical" />
                <a-button type="link" size="small" @click="handleDownload(record)">下载</a-button>
              </template>
            </template>
          </a-table>
        </a-card>

        <!-- Version Comparison -->
        <a-card title="版本对比" style="margin-top: 16px">
          <a-space style="margin-bottom: 16px">
            <a-select
              v-model:value="compareVerA"
              style="width: 160px"
              placeholder="选择版本 A"
            >
              <a-select-option v-for="v in versions" :key="v.version" :value="v.version">
                {{ v.version }}
              </a-select-option>
            </a-select>
            <span>vs</span>
            <a-select
              v-model:value="compareVerB"
              style="width: 160px"
              placeholder="选择版本 B"
            >
              <a-select-option v-for="v in versions" :key="v.version" :value="v.version">
                {{ v.version }}
              </a-select-option>
            </a-select>
            <a-button type="primary" @click="handleCompare">对比</a-button>
          </a-space>
          <a-table
            v-if="comparisonData.length"
            :columns="comparisonColumns"
            :data-source="comparisonData"
            bordered
            size="small"
            :pagination="false"
            row-key="metric"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'diff'">
                <span :class="record.diffClass">{{ record.diff }}</span>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>

      <!-- Tab 3: 评估记录 -->
      <a-tab-pane key="evaluations" tab="评估记录">
        <a-space direction="vertical" :size="16" style="width: 100%">
          <a-card v-for="evalItem in evaluations" :key="evalItem.title">
            <div class="eval-header">
              <span class="eval-title">{{ evalItem.title }}</span>
              <StatusBadge :status="evalItem.status" type="eval" />
              <a-tag :color="evalItem.type === '外部验证' ? 'purple' : 'blue'">{{ evalItem.type }}</a-tag>
            </div>
            <div class="eval-dataset">
              <a-tag color="cyan">{{ evalItem.dataset }}</a-tag>
            </div>

            <template v-if="evalItem.status === 'RUNNING'">
              <a-progress :percent="evalItem.progress" status="active" style="margin: 12px 0" />
            </template>

            <a-row v-else :gutter="24" class="eval-metrics">
              <a-col :span="6">
                <a-statistic title="AUC" :value="evalItem.auc" :precision="3" />
              </a-col>
              <a-col :span="6">
                <a-statistic title="F1" :value="evalItem.f1" :precision="3" />
              </a-col>
              <a-col :span="6">
                <a-statistic title="Precision" :value="evalItem.precision" :precision="3" />
              </a-col>
              <a-col :span="6">
                <a-statistic title="Recall" :value="evalItem.recall" :precision="3" />
              </a-col>
            </a-row>

            <div class="eval-footer">
              <span v-if="evalItem.duration" class="eval-duration">耗时: {{ evalItem.duration }}</span>
              <a-button type="primary" size="small">查看报告</a-button>
            </div>
          </a-card>
        </a-space>
      </a-tab-pane>

      <!-- Tab 4: 部署管理 -->
      <a-tab-pane key="deployments" tab="部署管理">
        <a-card>
          <template #extra>
            <a-button type="primary">新增部署</a-button>
          </template>
          <a-table
            :columns="deployColumns"
            :data-source="deployments"
            bordered
            row-key="name"
            size="middle"
            :pagination="false"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <StatusBadge :status="record.status" type="deploy" />
              </template>
              <template v-if="column.key === 'action'">
                <a-button type="link" size="small">详情</a-button>
                <a-divider type="vertical" />
                <a-button v-if="record.status === 'RUNNING'" type="link" size="small" danger>停止</a-button>
                <a-button v-else type="link" size="small">启动</a-button>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>
    </a-tabs>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import StatusBadge from '@/components/StatusBadge/index.vue'
import { useModal } from '@/hooks/useModal'

const route = useRoute()
const router = useRouter()

const loading = ref(false)

const editModal = useModal()
const versionModal = useModal()

function showEditModal() {
  editModal.open()
}

function showVersionModal() {
  versionModal.open()
}

function handleDownload(record: any) {
  message.info(`开始下载版本 ${record.version}`)
}
const activeTab = ref('info')

// Breadcrumb
const breadcrumb = [
  { title: '模型管理', path: '/model' },
  { title: '肺结节检测模型' },
]

// Tab 1: Model Overview
const modelInfo = reactive({
  name: '肺结节检测模型',
  status: 'PUBLISHED',
  description: '基于3D卷积神经网络和注意力机制的肺结节自动检测模型，用于胸部CT影像中肺结节的智能识别与定位，支持多尺度结节检测，具备高灵敏度和低误检率。',
  code: 'MODEL-LN-DET-001',
  type: '影像分析',
  framework: 'PyTorch 2.1',
  task: '目标检测',
  project: '肺结节检测项目',
  owner: '张医生',
  createdAt: '2025-08-15 10:30',
  updatedAt: '2026-03-22 16:45',
  latestVersion: 'v2.3.1',
  tags: ['CT', '肺结节', '目标检测'],
})

const metrics = [
  { label: '准确率', value: 96.8, suffix: '%' },
  { label: '灵敏度', value: 94.5, suffix: '%' },
  { label: '特异度', value: 97.3, suffix: '%' },
  { label: 'AUC', value: 0.983, suffix: '' },
]

// Tab 2: Version List
const versionColumns = [
  { title: '版本号', dataIndex: 'version', key: 'version' },
  { title: '描述', dataIndex: 'desc', key: 'desc' },
  { title: '框架版本', dataIndex: 'framework', key: 'framework' },
  { title: '文件大小', dataIndex: 'size', key: 'size' },
  { title: '训练指标(AUC)', dataIndex: 'auc', key: 'auc' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '创建时间', dataIndex: 'date', key: 'date' },
  { title: '操作', key: 'action', width: 140 },
]

const versions = ref([
  { version: 'v2.3.1', desc: '3D卷积+注意力机制', framework: 'PyTorch 2.1', size: '520MB', auc: 0.983, status: 'DEPLOYED', date: '2026-04-07' },
  { version: 'v2.2.0', desc: '新型注意力机制', framework: 'PyTorch 2.0', size: '500MB', auc: 0.923, status: 'DEPLOYED', date: '2026-03-15' },
  { version: 'v2.1.0', desc: '多尺度特征融合', framework: 'PyTorch 2.0', size: '490MB', auc: 0.908, status: 'DEPLOYED', date: '2026-02-20' },
  { version: 'v2.0.0', desc: '基础ResNet50', framework: 'PyTorch 1.13', size: '480MB', auc: 0.895, status: 'DEPRECATED', date: '2026-01-10' },
  { version: 'v1.0.0', desc: '初始版本', framework: 'PyTorch 1.13', size: '475MB', auc: 0.882, status: 'DEPRECATED', date: '2025-11-01' },
])

// Version Comparison
const compareVerA = ref<string>('v2.3.1')
const compareVerB = ref<string>('v2.2.0')
const comparisonData = ref<any[]>([])

const comparisonColumns = [
  { title: '指标', dataIndex: 'metric', key: 'metric' },
  { title: compareVerA.value, dataIndex: 'valA', key: 'valA' },
  { title: compareVerB.value, dataIndex: 'valB', key: 'valB' },
  { title: '差异', dataIndex: 'diff', key: 'diff' },
]

const versionMetricsMap: Record<string, Record<string, any>> = {
  'v2.3.1': { auc: 0.983, accuracy: 0.968, recall: 0.965, precision: 0.958, f1: 0.961, params: '45.2M', size: '520MB' },
  'v2.2.0': { auc: 0.923, accuracy: 0.921, recall: 0.919, precision: 0.905, f1: 0.912, params: '42.8M', size: '500MB' },
  'v2.1.0': { auc: 0.908, accuracy: 0.912, recall: 0.905, precision: 0.898, f1: 0.901, params: '40.1M', size: '490MB' },
  'v2.0.0': { auc: 0.895, accuracy: 0.897, recall: 0.891, precision: 0.885, f1: 0.888, params: '38.5M', size: '480MB' },
  'v1.0.0': { auc: 0.882, accuracy: 0.883, recall: 0.878, precision: 0.870, f1: 0.874, params: '36.2M', size: '475MB' },
}

function handleCompare() {
  const dataA = versionMetricsMap[compareVerA.value]
  const dataB = versionMetricsMap[compareVerB.value]
  if (!dataA || !dataB) {
    comparisonData.value = []
    return
  }

  const metricLabels: Record<string, string> = {
    auc: 'AUC', accuracy: 'Accuracy', recall: 'Recall',
    precision: 'Precision', f1: 'F1', params: '参数量', size: '文件大小',
  }

  comparisonData.value = Object.keys(metricLabels).map((key) => {
    const vA = dataA[key]
    const vB = dataB[key]
    let diff = ''
    let diffClass = ''
    if (typeof vA === 'number' && typeof vB === 'number') {
      const delta = vA - vB
      diff = (delta >= 0 ? '+' : '') + delta.toFixed(3)
      diffClass = delta > 0 ? 'diff-positive' : delta < 0 ? 'diff-negative' : ''
    } else {
      diff = `${vA} / ${vB}`
    }
    return {
      metric: metricLabels[key],
      valA: vA,
      valB: vB,
      diff,
      diffClass,
    }
  })
}

// Tab 3: Evaluation Records
const evaluations = reactive([
  {
    title: 'v2.3.1 外部验证集评估',
    status: 'COMPLETED',
    type: '外部验证',
    dataset: '外部验证集-2026Q1 (1500条)',
    auc: 0.983, f1: 0.961, precision: 0.958, recall: 0.965,
    duration: '28分45秒',
  },
  {
    title: 'v2.3.1 交叉验证',
    status: 'RUNNING',
    type: '内部评估',
    dataset: '内部训练集 (5000条)',
    progress: 72,
  },
  {
    title: 'v2.2.0 内部测试集评估',
    status: 'COMPLETED',
    type: '内部评估',
    dataset: '测试集-2026 (800条)',
    auc: 0.923, f1: 0.912, precision: 0.905, recall: 0.919,
    duration: '15分20秒',
  },
])

// Tab 4: Deployments
const deployColumns = [
  { title: '部署名称', dataIndex: 'name', key: 'name' },
  { title: '版本', dataIndex: 'version', key: 'version' },
  { title: '类型', dataIndex: 'type', key: 'type' },
  { title: '集群', dataIndex: 'cluster', key: 'cluster' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: 'QPS', dataIndex: 'qps', key: 'qps' },
  { title: '延迟', dataIndex: 'latency', key: 'latency' },
  { title: '操作', key: 'action', width: 140 },
]

const deployments = ref([
  { name: '肺结节检测-生产', version: 'v2.1.0', type: 'ONLINE', cluster: 'TRITON', status: 'RUNNING', qps: 58, latency: '23ms' },
  { name: '肺结节检测-灰度', version: 'v2.3.1', type: 'ONLINE', cluster: 'TRITON', status: 'RUNNING', qps: 5, latency: '25ms' },
  { name: '肺结节检测-测试', version: 'v2.3.1', type: 'ONLINE', cluster: 'FASTAPI', status: 'STOPPED', qps: '--', latency: '--' },
])
</script>

<style scoped>
.overview-card {
  margin-bottom: 0;
}
.model-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.model-name {
  font-size: 22px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  margin: 0;
}
.model-desc {
  color: rgba(0, 0, 0, 0.55);
  font-size: 14px;
  line-height: 1.6;
  margin: 0;
}
.metric-card {
  text-align: center;
  background: #fafafa;
}
.eval-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.eval-title {
  font-size: 16px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
}
.eval-dataset {
  margin-bottom: 16px;
}
.eval-metrics {
  padding: 12px 0;
}
.eval-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}
.eval-duration {
  color: rgba(0, 0, 0, 0.45);
  font-size: 13px;
}
.diff-positive {
  color: #52c41a;
  font-weight: 500;
}
.diff-negative {
  color: #ff4d4f;
  font-weight: 500;
}
</style>
