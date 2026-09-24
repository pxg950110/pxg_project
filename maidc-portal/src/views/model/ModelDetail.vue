<template>
  <PageContainer :loading="loading" :breadcrumb="breadcrumb">
    <template #extra>
      <el-button @click="router.back()">返回</el-button>
    </template>

    <el-tabs v-model="activeTab">
      <!-- Tab 1: 基本信息 -->
      <el-tab-pane label="基本信息" name="info">
        <!-- Top Card: Model Overview -->
        <el-card shadow="never" class="overview-card !rounded-xl !border-slate-200/80 shadow-clinical-sm">
          <div class="grid grid-cols-1 lg:grid-cols-12 gap-8">
            <div class="lg:col-span-7">
              <div class="model-header">
                <h2 class="model-name">{{ modelInfo.name }}</h2>
                <StatusBadge :status="modelInfo.status" type="model" />
              </div>
              <p class="model-desc">{{ modelInfo.description }}</p>
              <div class="flex items-center gap-2" style="margin-top: 16px">
                <el-button type="primary" @click="showEditModal">编辑</el-button>
                <el-button type="primary" @click="showVersionModal">注册新版本</el-button>
              </div>
            </div>
            <div class="lg:col-span-5">
              <el-descriptions :column="1" size="small" border>
                <el-descriptions-item label="Model ID">{{ modelInfo.code }}</el-descriptions-item>
                <el-descriptions-item label="类型">{{ modelInfo.type }}</el-descriptions-item>
                <el-descriptions-item label="框架">{{ modelInfo.framework }}</el-descriptions-item>
                <el-descriptions-item label="任务">{{ modelInfo.task }}</el-descriptions-item>
                <el-descriptions-item label="所属项目">{{ modelInfo.project }}</el-descriptions-item>
                <el-descriptions-item label="负责人">{{ modelInfo.owner }}</el-descriptions-item>
                <el-descriptions-item label="创建时间">{{ modelInfo.createdAt }}</el-descriptions-item>
                <el-descriptions-item label="更新时间">{{ modelInfo.updatedAt }}</el-descriptions-item>
                <el-descriptions-item label="最新版本">{{ modelInfo.latestVersion }}</el-descriptions-item>
                <el-descriptions-item label="标签">
                  <el-tag v-for="tag in modelInfo.tags" :key="tag" type="primary" size="small" class="mr-1">{{ tag }}</el-tag>
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </div>
        </el-card>

        <!-- Performance Metrics Box -->
        <el-card shadow="never" class="mt-4 !rounded-xl !border-slate-200/80 shadow-clinical-sm">
          <template #header>
            <span class="font-semibold text-slate-900">最新评估指标 v2.3.1</span>
          </template>
          <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <el-card v-for="metric in metrics" :key="metric.label" shadow="never" class="metric-card !rounded-lg !border-slate-200/80">
              <div class="stat-label">{{ metric.label }}</div>
              <div class="stat-value">{{ metric.value }}<span v-if="metric.suffix" class="stat-suffix">{{ metric.suffix }}</span></div>
            </el-card>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- Tab 2: 版本列表 -->
      <el-tab-pane label="版本列表" name="versions">
        <el-card shadow="never" class="!rounded-xl !border-slate-200/80 shadow-clinical-sm">
          <el-table
            :data="versions"
            border
            row-key="version"
            size="default"
          >
            <el-table-column label="版本号" prop="version" />
            <el-table-column label="描述" prop="desc" />
            <el-table-column label="框架版本" prop="framework" />
            <el-table-column label="文件大小" prop="size" />
            <el-table-column label="训练指标(AUC)" prop="auc" />
            <el-table-column label="状态" prop="status">
              <template #default="{ row }">
                <StatusBadge :status="row.status" type="version" />
              </template>
            </el-table-column>
            <el-table-column label="创建时间" prop="date" />
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button link type="primary" size="small">详情</el-button>
                <el-divider direction="vertical" />
                <el-button link type="primary" size="small" @click="handleDownload(row)">下载</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- Version Comparison -->
        <el-card shadow="never" class="mt-4 !rounded-xl !border-slate-200/80 shadow-clinical-sm">
          <template #header>
            <span class="font-semibold text-slate-900">版本对比</span>
          </template>
          <div class="flex items-center gap-2" style="margin-bottom: 16px">
            <el-select
              v-model="compareVerA"
              style="width: 160px"
              placeholder="选择版本 A"
            >
              <el-option v-for="v in versions" :key="v.version" :value="v.version" :label="v.version" />
            </el-select>
            <span>vs</span>
            <el-select
              v-model="compareVerB"
              style="width: 160px"
              placeholder="选择版本 B"
            >
              <el-option v-for="v in versions" :key="v.version" :value="v.version" :label="v.version" />
            </el-select>
            <el-button type="primary" @click="handleCompare">对比</el-button>
          </div>
          <el-table
            v-if="comparisonData.length"
            :data="comparisonData"
            border
            size="small"
            row-key="metric"
          >
            <el-table-column label="指标" prop="metric" />
            <el-table-column :label="compareVerA" prop="valA" />
            <el-table-column :label="compareVerB" prop="valB" />
            <el-table-column label="差异" prop="diff">
              <template #default="{ row }">
                <span :class="row.diffClass">{{ row.diff }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- Tab 3: 评估记录 -->
      <el-tab-pane label="评估记录" name="evaluations">
        <div v-if="evaluations.length" class="flex flex-col gap-4" style="width: 100%">
          <el-card v-for="evalItem in evaluations" :key="evalItem.title" shadow="never" class="!rounded-xl !border-slate-200/80 shadow-clinical-sm">
            <div class="eval-header">
              <span class="eval-title">{{ evalItem.title }}</span>
              <StatusBadge :status="evalItem.status" type="eval" />
              <el-tag
                v-if="evalItem.type === '外部验证'"
                size="small"
                :style="{ color: '#8b5cf6', backgroundColor: '#f5f3ff', borderColor: '#ddd6fe' }"
              >{{ evalItem.type }}</el-tag>
              <el-tag v-else type="primary" size="small">{{ evalItem.type }}</el-tag>
            </div>
            <div class="eval-dataset">
              <el-tag size="small" :style="{ color: '#0e7490', backgroundColor: '#ecfeff', borderColor: '#a5f3fc' }">{{ evalItem.dataset }}</el-tag>
            </div>

            <template v-if="evalItem.status === 'RUNNING'">
              <el-progress :percentage="evalItem.progress" :stroke-width="6" style="margin: 12px 0" />
            </template>

            <div v-else class="eval-metrics grid grid-cols-2 lg:grid-cols-4 gap-6">
              <div class="stat-item">
                <div class="stat-label">AUC</div>
                <div class="stat-value">{{ evalItem.auc?.toFixed(3) }}</div>
              </div>
              <div class="stat-item">
                <div class="stat-label">F1</div>
                <div class="stat-value">{{ evalItem.f1?.toFixed(3) }}</div>
              </div>
              <div class="stat-item">
                <div class="stat-label">Precision</div>
                <div class="stat-value">{{ evalItem.precision?.toFixed(3) }}</div>
              </div>
              <div class="stat-item">
                <div class="stat-label">Recall</div>
                <div class="stat-value">{{ evalItem.recall?.toFixed(3) }}</div>
              </div>
            </div>

            <div class="eval-footer">
              <span v-if="evalItem.duration" class="eval-duration">耗时: {{ evalItem.duration }}</span>
              <el-button type="primary" size="small">查看报告</el-button>
            </div>
          </el-card>
        </div>
        <el-empty v-else description="暂无评估记录" :image-size="60" style="padding: 60px 0" />
      </el-tab-pane>

      <!-- Tab 4: 部署管理 -->
      <el-tab-pane label="部署管理" name="deployments">
        <el-card shadow="never" class="!rounded-xl !border-slate-200/80 shadow-clinical-sm">
          <template #header>
            <div class="flex justify-end">
              <el-button type="primary">新增部署</el-button>
            </div>
          </template>
          <el-table
            :data="deployments"
            border
            row-key="name"
            size="default"
          >
            <el-table-column label="部署名称" prop="name" />
            <el-table-column label="版本" prop="version" />
            <el-table-column label="类型" prop="type" />
            <el-table-column label="集群" prop="cluster" />
            <el-table-column label="状态" prop="status">
              <template #default="{ row }">
                <StatusBadge :status="row.status" type="deploy" />
              </template>
            </el-table-column>
            <el-table-column label="QPS" prop="qps" />
            <el-table-column label="延迟" prop="latency" />
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button link type="primary" size="small">详情</el-button>
                <el-divider direction="vertical" />
                <el-button v-if="row.status === 'RUNNING'" link type="danger" size="small">停止</el-button>
                <el-button v-else link type="primary" size="small">启动</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editModal.visible" title="编辑模型" width="600px">
      <el-form label-width="100px">
        <el-form-item label="模型名称">
          <el-input v-model="editForm.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editModal.close()">取消</el-button>
        <el-button type="primary" @click="handleEditSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 注册版本弹窗 -->
    <el-dialog v-model="versionModal.visible" title="注册新版本" width="600px">
      <el-form label-width="100px">
        <el-form-item label="版本号" required>
          <el-input v-model="versionForm.version_no" placeholder="例如 v1.0.0" />
        </el-form-item>
        <el-form-item label="变更说明">
          <el-input v-model="versionForm.changelog" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="versionModal.close()">取消</el-button>
        <el-button type="primary" @click="handleVersionSubmit">确定</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
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
  ElMessage.success('模型信息已更新')
  editModal.close()
}

function handleVersionSubmit() {
  if (!versionForm.version_no) { ElMessage.warning('请输入版本号'); return }
  ElMessage.success('版本注册成功')
  versionModal.close()
}

function handleDownload(record: any) {
  ElMessage.info(`开始下载版本 ${record.version}`)
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
    modelInfo.name = data.model_name || ''
    modelInfo.status = data.status || ''
    modelInfo.description = data.description || ''
    modelInfo.code = data.model_code || ''
    modelInfo.type = data.model_type || ''
    modelInfo.framework = data.framework || ''
    modelInfo.task = data.task_type || ''
    modelInfo.project = ''
    modelInfo.owner = data.owner_name || ''
    modelInfo.createdAt = data.created_at ? dayjs(data.created_at).format('YYYY-MM-DD HH:mm:ss') : ''
    modelInfo.updatedAt = data.updated_at ? dayjs(data.updated_at).format('YYYY-MM-DD HH:mm:ss') : ''
    modelInfo.latestVersion = data.latest_version || ''
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
  color: #0f172a;
  margin: 0;
}
.model-desc {
  color: #64748b;
  font-size: 14px;
  line-height: 1.6;
  margin: 0;
}
.metric-card {
  text-align: center;
  background: #f8fafc;
}
.stat-label {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 4px;
}
.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #0f172a;
  font-variant-numeric: tabular-nums;
}
.stat-suffix {
  font-size: 13px;
  color: #94a3b8;
  margin-left: 4px;
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
  color: #0f172a;
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
  border-top: 1px solid #f1f5f9;
}
.eval-duration {
  color: #94a3b8;
  font-size: 13px;
}
.diff-positive {
  color: #10b981;
  font-weight: 500;
}
.diff-negative {
  color: #ef4444;
  font-weight: 500;
}
</style>
