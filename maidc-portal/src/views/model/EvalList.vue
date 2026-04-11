<template>
  <PageContainer title="模型评估" subtitle="评估管理">
    <template #extra>
      <a-button type="primary" @click="evalModal.open()">
        <PlusOutlined /> 新建评估
      </a-button>
    </template>

    <!-- Filter row -->
    <div class="filter-row">
      <div class="filter-left">
        <a-select v-model:value="filters.evalType" placeholder="评估类型" style="width: 140px" allow-clear>
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="内部评估">内部评估</a-select-option>
          <a-select-option value="外部验证">外部验证</a-select-option>
        </a-select>
        <a-select v-model:value="filters.status" placeholder="状态" style="width: 140px" allow-clear>
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="COMPLETED">已完成</a-select-option>
          <a-select-option value="RUNNING">运行中</a-select-option>
          <a-select-option value="FAILED">失败</a-select-option>
        </a-select>
        <a-input-search
          v-model:value="filters.keyword"
          placeholder="搜索评估名称"
          style="width: 220px"
          @search="handleSearch"
          allow-clear
        />
      </div>
    </div>

    <!-- Evaluation cards -->
    <div class="eval-cards">
      <a-card
        v-for="item in filteredEvaluations"
        :key="item.id"
        class="eval-card"
        :class="{ 'eval-card--selected': selectedId === item.id }"
        hoverable
        @click="selectedId = item.id"
      >
        <!-- Header row -->
        <div class="eval-card__header">
          <span class="eval-card__title">{{ item.title }}</span>
          <a-tag :color="statusColorMap[item.status]">{{ statusTextMap[item.status] }}</a-tag>
          <a-tag :color="item.evalType === '外部验证' ? 'purple' : 'blue'">{{ item.evalType }}</a-tag>
        </div>

        <!-- Dataset row -->
        <div class="eval-card__dataset">
          <FolderOutlined style="margin-right: 4px; color: rgba(0,0,0,0.45)" />
          <span>{{ item.dataset }}</span>
        </div>

        <!-- Metrics row (COMPLETED) -->
        <div v-if="item.status === 'COMPLETED'" class="eval-card__metrics">
          <span class="metric-item">AUC: <strong>{{ item.auc?.toFixed(4) }}</strong></span>
          <span class="metric-item">F1: <strong>{{ item.f1?.toFixed(4) }}</strong></span>
          <span class="metric-item">P: <strong>{{ item.precision?.toFixed(4) }}</strong></span>
          <span class="metric-item">R: <strong>{{ item.recall?.toFixed(4) }}</strong></span>
        </div>

        <!-- Progress row (RUNNING) -->
        <div v-if="item.status === 'RUNNING'" class="eval-card__progress">
          <a-progress :percent="item.progress" :stroke-color="{ '0%': '#108ee9', '100%': '#87d068' }" />
        </div>

        <!-- Error row (FAILED) -->
        <div v-if="item.status === 'FAILED'" class="eval-card__error">
          <ExclamationCircleOutlined style="margin-right: 6px" />
          <span>{{ item.error }}</span>
        </div>

        <!-- Footer row -->
        <div class="eval-card__footer">
          <span class="eval-card__duration">
            <ClockCircleOutlined style="margin-right: 4px" />
            耗时 {{ item.duration || '--' }}
          </span>
          <div class="eval-card__actions">
            <a-button v-if="item.status === 'COMPLETED'" type="link" size="small" @click.stop="viewReport(item)">
              查看报告
            </a-button>
            <a-button v-if="item.status === 'COMPLETED'" type="link" size="small" @click.stop="exportReport(item)">
              导出报告
            </a-button>
          </div>
        </div>
      </a-card>
    </div>

    <!-- Expanded detail section -->
    <div v-if="selectedEval && selectedEval.status === 'COMPLETED'" class="eval-detail">
      <a-divider />
      <h3 class="eval-detail__title">评估详情 — {{ selectedEval.title }}</h3>

      <!-- Performance metrics grid -->
      <a-row :gutter="[16, 16]" class="eval-detail__metrics-grid">
        <a-col :span="4">
          <a-statistic title="AUC" :value="selectedEval.auc" :precision="4" />
        </a-col>
        <a-col :span="4">
          <a-statistic title="F1 Score" :value="selectedEval.f1" :precision="4" />
        </a-col>
        <a-col :span="4">
          <a-statistic title="Precision" :value="selectedEval.precision" :precision="4" />
        </a-col>
        <a-col :span="4">
          <a-statistic title="Recall" :value="selectedEval.recall" :precision="4" />
        </a-col>
        <a-col :span="4">
          <a-statistic title="Sensitivity" :value="selectedEval.sensitivity" :precision="4" />
        </a-col>
        <a-col :span="4">
          <a-statistic title="Specificity" :value="selectedEval.specificity" :precision="4" />
        </a-col>
      </a-row>

      <!-- Confusion matrix -->
      <div v-if="selectedEval.tp != null" class="eval-detail__confusion">
        <h4 style="margin-bottom: 12px">混淆矩阵</h4>
        <table class="confusion-table">
          <thead>
            <tr>
              <th class="corner-cell"></th>
              <th>阳性 (P)</th>
              <th>阴性 (N)</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td class="row-header">阳性 (P)</td>
              <td class="cell-tp">TP: {{ selectedEval.tp }}</td>
              <td class="cell-fp">FP: {{ selectedEval.fp }}</td>
            </tr>
            <tr>
              <td class="row-header">阴性 (N)</td>
              <td class="cell-fn">FN: {{ selectedEval.fn }}</td>
              <td class="cell-tn">TN: {{ selectedEval.tn }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- New Evaluation Modal -->
    <a-modal
      v-model:open="evalModal.visible"
      title="新建评估"
      @ok="handleCreateEval"
      :confirm-loading="submitting"
      width="600px"
    >
      <a-form layout="vertical">
        <a-form-item label="选择模型" required>
          <a-select v-model:value="evalForm.model_id" placeholder="请选择模型">
            <a-select-option v-for="m in modelOptions" :key="m.id" :value="m.id">{{ m.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="选择版本" required>
          <a-select v-model:value="evalForm.version_id" placeholder="请选择版本">
            <a-select-option v-for="v in versionOptions" :key="v.id" :value="v.id">{{ v.version_no }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="评估数据集">
          <a-select v-model:value="evalForm.dataset_id" placeholder="请选择数据集">
            <a-select-option v-for="d in datasetOptions" :key="d.id" :value="d.id">{{ d.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="评估指标">
          <a-checkbox-group v-model:value="evalForm.metrics" :options="metricOptions" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import {
  PlusOutlined,
  FolderOutlined,
  ClockCircleOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import { useModal } from '@/hooks/useModal'

interface Evaluation {
  id: number
  title: string
  version: string
  status: 'COMPLETED' | 'RUNNING' | 'FAILED'
  evalType: string
  dataset: string
  auc?: number
  f1?: number
  precision?: number
  recall?: number
  sensitivity?: number
  specificity?: number
  duration?: string
  progress?: number
  error?: string
  tp?: number
  fp?: number
  fn?: number
  tn?: number
}

const statusColorMap: Record<string, string> = {
  COMPLETED: 'green',
  RUNNING: 'blue',
  FAILED: 'red',
}

const statusTextMap: Record<string, string> = {
  COMPLETED: '已完成',
  RUNNING: '运行中',
  FAILED: '失败',
}

// Mock data
const evaluations = ref<Evaluation[]>([
  {
    id: 1,
    title: 'v2.3.1 外部验证集评估',
    version: 'v2.3.1',
    status: 'COMPLETED',
    evalType: '外部验证',
    dataset: '外部验证集-2026Q1 (1500条)',
    auc: 0.9234,
    f1: 0.8912,
    precision: 0.9045,
    recall: 0.8786,
    sensitivity: 0.8786,
    specificity: 0.9512,
    duration: '28分45秒',
    tp: 442,
    fp: 46,
    fn: 61,
    tn: 951,
  },
  {
    id: 2,
    title: 'v2.3.1 交叉验证',
    version: 'v2.3.1',
    status: 'RUNNING',
    evalType: '内部评估',
    dataset: '内部训练集 (5000条)',
    progress: 72,
  },
  {
    id: 3,
    title: 'v2.2.0 内部测试集评估',
    version: 'v2.2.0',
    status: 'COMPLETED',
    evalType: '内部评估',
    dataset: '测试集-2026 (800条)',
    auc: 0.908,
    f1: 0.872,
    precision: 0.889,
    recall: 0.856,
    duration: '15分20秒',
  },
  {
    id: 4,
    title: 'v1.1.0 外部验证集评估',
    version: 'v1.1.0',
    status: 'FAILED',
    evalType: '外部验证',
    dataset: '外部验证集-2025Q4 (1200条)',
    error: '评估失败：数据格式异常，请检查数据集',
  },
])

// Filters
const filters = reactive({
  evalType: '' as string,
  status: '' as string,
  keyword: '' as string,
})

const filteredEvaluations = computed(() => {
  return evaluations.value.filter((item) => {
    if (filters.evalType && item.evalType !== filters.evalType) return false
    if (filters.status && item.status !== filters.status) return false
    if (filters.keyword && !item.title.includes(filters.keyword)) return false
    return true
  })
})

function handleSearch() {
  // Filtering is reactive via computed
}

// Selection
const selectedId = ref<number>(1)

const selectedEval = computed(() => {
  return evaluations.value.find((e) => e.id === selectedId.value)
})

// Modal
const evalModal = useModal()
const submitting = ref(false)

const evalForm = reactive({
  model_id: undefined as any,
  version_id: undefined as any,
  dataset_id: undefined as any,
  metrics: ['auc', 'f1_score', 'precision', 'recall'],
})

const metricOptions = [
  { label: 'AUC', value: 'auc' },
  { label: 'F1 Score', value: 'f1_score' },
  { label: 'Precision', value: 'precision' },
  { label: 'Recall', value: 'recall' },
]

// Mock select options
const modelOptions = ref<any[]>([])
const versionOptions = ref<any[]>([])
const datasetOptions = ref<any[]>([])

function viewReport(item: Evaluation) {
  message.info(`查看报告: ${item.title}`)
}

function exportReport(item: Evaluation) {
  message.info(`导出报告: ${item.title}`)
}

async function handleCreateEval() {
  submitting.value = true
  try {
    // Simulate creation
    await new Promise((resolve) => setTimeout(resolve, 500))
    message.success('评估任务已创建')
    evalModal.close()
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.filter-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.filter-left {
  display: flex;
  gap: 12px;
  align-items: center;
}

.eval-cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.eval-card {
  border-radius: 8px;
  transition: box-shadow 0.2s;
}
.eval-card--selected {
  box-shadow: 0 0 0 2px #1677ff;
}

.eval-card__header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.eval-card__title {
  font-size: 15px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
}

.eval-card__dataset {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
  margin-bottom: 12px;
  display: flex;
  align-items: center;
}

.eval-card__metrics {
  display: flex;
  gap: 24px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.metric-item {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
}
.metric-item strong {
  color: rgba(0, 0, 0, 0.88);
  font-variant-numeric: tabular-nums;
}

.eval-card__progress {
  margin-bottom: 12px;
}

.eval-card__error {
  display: flex;
  align-items: flex-start;
  color: #cf1322;
  font-size: 13px;
  margin-bottom: 12px;
  background: #fff2f0;
  border-radius: 4px;
  padding: 8px 12px;
}

.eval-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid #f5f5f5;
  padding-top: 10px;
}
.eval-card__duration {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
}
.eval-card__actions {
  display: flex;
  gap: 4px;
}

.eval-detail {
  margin-top: 8px;
}
.eval-detail__title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 20px;
  color: rgba(0, 0, 0, 0.88);
}
.eval-detail__metrics-grid {
  margin-bottom: 32px;
}

.eval-detail__confusion {
  margin-top: 8px;
}

/* Confusion matrix table */
.confusion-table {
  border-collapse: collapse;
  margin: 0 auto;
}
.confusion-table th,
.confusion-table td {
  padding: 12px 24px;
  border: 1px solid #f0f0f0;
  text-align: center;
  font-size: 14px;
}
.confusion-table th {
  background: #fafafa;
  font-weight: 500;
}
.confusion-table .corner-cell {
  width: 60px;
  background: #fafafa;
}
.confusion-table .row-header {
  background: #fafafa;
  font-weight: 500;
  text-align: right;
}
.confusion-table .cell-tp {
  background: rgba(82, 196, 26, 0.15);
  font-weight: 600;
}
.confusion-table .cell-tn {
  background: rgba(82, 196, 26, 0.08);
  font-weight: 600;
}
.confusion-table .cell-fp {
  background: rgba(250, 173, 20, 0.12);
}
.confusion-table .cell-fn {
  background: rgba(250, 173, 20, 0.12);
}
</style>
