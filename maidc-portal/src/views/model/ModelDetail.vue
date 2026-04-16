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

    <!-- 编辑弹窗 -->
    <a-modal v-model:open="editModal.visible" title="编辑模型" @ok="handleEditSubmit" width="600px">
      <a-form layout="vertical">
        <a-form-item label="模型名称">
          <a-input v-model:value="editForm.name" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="editForm.description" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 注册版本弹窗 -->
    <a-modal v-model:open="versionModal.visible" title="注册新版本" @ok="handleVersionSubmit" width="600px">
      <a-form layout="vertical">
        <a-form-item label="版本号" required>
          <a-input v-model:value="versionForm.version_no" placeholder="例如 v1.0.0" />
        </a-form-item>
        <a-form-item label="变更说明">
          <a-textarea v-model:value="versionForm.changelog" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import StatusBadge from '@/components/StatusBadge/index.vue'
import { useModal } from '@/hooks/useModal'
import { getModel, getVersions, getEvaluations, getDeployments } from '@/api/model'

const route = useRoute()
const router = useRouter()

const modelId = Number(route.params.id)
const loading = ref(false)

const editModal = useModal()
const versionModal = useModal()

const editForm = reactive({ name: '', description: '' })
const versionForm = reactive({ version_no: '', changelog: '' })

function showEditModal() {
  editForm.name = modelInfo.name
  editForm.description = modelInfo.description
  editModal.open()
}

function showVersionModal() {
  versionForm.version_no = ''
  versionForm.changelog = ''
  versionModal.open()
}

function handleEditSubmit() {
  modelInfo.name = editForm.name
  modelInfo.description = editForm.description
  message.success('模型信息已更新')
  editModal.close()
}

function handleVersionSubmit() {
  if (!versionForm.version_no) { message.warning('请输入版本号'); return }
  message.success('版本注册成功')
  versionModal.close()
}

function handleDownload(record: any) {
  message.info(`开始下载版本 ${record.version}`)
}
const activeTab = ref('info')

// --- Load model detail from API ---
const modelInfo = reactive({
  name: '',
  status: '',
  description: '',
  code: '',
  type: '',
  framework: '',
  task: '',
  project: '',
  owner: '',
  createdAt: '',
  updatedAt: '',
  latestVersion: '',
  tags: [] as string[],
})

const metrics = ref<{ label: string; value: number; suffix: string }[]>([])

async function loadModelDetail() {
  loading.value = true
  try {
    const res = await getModel(modelId)
    const data = res.data.data
    modelInfo.name = data.modelName || data.model_name || ''
    modelInfo.status = data.status || ''
    modelInfo.description = data.description || ''
    modelInfo.code = data.modelCode || data.model_code || ''
    modelInfo.type = data.modelType || data.model_type || ''
    modelInfo.framework = data.framework || ''
    modelInfo.task = data.taskType || data.task_type || ''
    modelInfo.project = data.project || ''
    modelInfo.owner = data.ownerName || data.owner_name || ''
    modelInfo.createdAt = data.createdAt || data.created_at || ''
    modelInfo.updatedAt = data.updatedAt || data.updated_at || ''
    modelInfo.latestVersion = data.latestVersion || data.latest_version || ''
    modelInfo.tags = data.tags || []
  } finally {
    loading.value = false
  }
}

// Breadcrumb
const breadcrumb = computed(() => [
  { title: '模型管理', path: '/model' },
  { title: modelInfo.name || '模型详情' },
])

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

const versions = ref<any[]>([])

async function loadVersions() {
  try {
    const res = await getVersions(modelId, { page: 1, page_size: 100 })
    versions.value = (res.data.data.items || []).map((v: any) => ({
      version: v.version || v.versionNumber || v.version_number,
      desc: v.description,
      framework: v.framework,
      size: v.fileSize || v.file_size,
      auc: v.auc,
      status: v.status,
      date: v.createdAt || v.created_at,
    }))
  } catch {
    // Silently fail - versions will be empty
  }
}

// Version Comparison
const compareVerA = ref<string>('')
const compareVerB = ref<string>('')
const comparisonData = ref<any[]>([])

const comparisonColumns = computed(() => [
  { title: '指标', dataIndex: 'metric', key: 'metric' },
  { title: compareVerA.value, dataIndex: 'valA', key: 'valA' },
  { title: compareVerB.value, dataIndex: 'valB', key: 'valB' },
  { title: '差异', dataIndex: 'diff', key: 'diff' },
])

const versionMetricsMap = computed<Record<string, Record<string, any>>>(() => {
  const map: Record<string, Record<string, any>> = {}
  for (const v of versions.value) {
    if (v.version) {
      map[v.version] = {
        auc: v.auc,
        accuracy: v.accuracy,
        recall: v.recall,
        precision: v.precision,
        f1: v.f1,
        params: v.params,
        size: v.size,
      }
    }
  }
  return map
})

function handleCompare() {
  const dataA = versionMetricsMap.value[compareVerA.value]
  const dataB = versionMetricsMap.value[compareVerB.value]
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
const evaluations = ref<any[]>([])

async function loadEvaluations() {
  try {
    const res = await getEvaluations({ page: 1, page_size: 50, model_id: modelId })
    evaluations.value = (res.data.data.items || []).map((e: any) => ({
      title: e.title || e.name,
      status: e.status,
      type: e.type || '内部评估',
      dataset: e.dataset || '',
      auc: e.auc,
      f1: e.f1,
      precision: e.precision,
      recall: e.recall,
      duration: e.duration,
      progress: e.progress,
    }))
  } catch {
    // Silently fail
  }
}

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

const deployments = ref<any[]>([])

async function loadDeployments() {
  try {
    const res = await getDeployments({ page: 1, page_size: 50, status: undefined })
    // Filter by model if the API supports it; otherwise show all
    deployments.value = (res.data.data.items || []).filter((d: any) => d.modelId === modelId || d.model_id === modelId || !d.modelId).map((d: any) => ({
      name: d.name || d.deploymentName || d.deployment_name,
      version: d.version || d.latestVersion || d.latest_version,
      type: d.type || 'ONLINE',
      cluster: d.cluster || '',
      status: d.status,
      qps: d.qps ?? '--',
      latency: d.latency ?? '--',
    }))
  } catch {
    // Silently fail
  }
}

onMounted(async () => {
  await loadModelDetail()
  loadVersions()
  loadEvaluations()
  loadDeployments()
})
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
