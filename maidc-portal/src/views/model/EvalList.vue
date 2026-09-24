<template>
  <PageContainer title="模型评估" subtitle="评估管理">
    <template #extra>
      <el-button type="primary" @click="evalModal.open()">
        <el-icon class="mr-1"><Plus /></el-icon>新建评估
      </el-button>
    </template>

    <!-- Filter row -->
    <div class="filter-row">
      <div class="filter-left">
        <el-select v-model="filters.evalType" placeholder="评估类型" style="width: 140px" clearable>
          <el-option label="全部" value="" />
          <el-option label="内部评估" value="内部评估" />
          <el-option label="外部验证" value="外部验证" />
        </el-select>
        <el-select v-model="filters.status" placeholder="状态" style="width: 140px" clearable>
          <el-option label="全部" value="" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="运行中" value="RUNNING" />
          <el-option label="失败" value="FAILED" />
        </el-select>
        <el-input
          v-model="filters.keyword"
          placeholder="搜索评估名称"
          style="width: 220px"
          clearable
          :suffix-icon="Search"
          @keyup.enter="handleSearch"
        />
      </div>
    </div>

    <!-- Evaluation cards -->
    <div class="eval-cards">
      <el-card
        v-for="item in filteredEvaluations"
        :key="item.id"
        class="eval-card"
        :class="{ 'eval-card--selected': selectedId === item.id }"
        shadow="hover"
        @click="selectedId = item.id"
      >
        <!-- Header row -->
        <div class="eval-card__header">
          <span class="eval-card__title">{{ item.title }}</span>
          <el-tag :type="statusColorMap[item.status]" size="small">{{ statusTextMap[item.status] }}</el-tag>
          <el-tag
            v-if="item.evalType === '外部验证'"
            size="small"
            :style="{ color: '#8b5cf6', backgroundColor: '#f5f3ff', borderColor: '#ddd6fe' }"
          >{{ item.evalType }}</el-tag>
          <el-tag v-else type="primary" size="small">{{ item.evalType }}</el-tag>
        </div>

        <!-- Dataset row -->
        <div class="eval-card__dataset">
          <el-icon class="mr-1" style="color: #94a3b8"><Folder /></el-icon>
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
          <el-progress
            :percentage="item.progress"
            :stroke-width="6"
            :color="[{ color: '#0ea5e9', percentage: 0 }, { color: '#10b981', percentage: 100 }]"
          />
        </div>

        <!-- Error row (FAILED) -->
        <div v-if="item.status === 'FAILED'" class="eval-card__error">
          <el-icon class="mr-1.5"><Warning /></el-icon>
          <span>{{ item.error }}</span>
        </div>

        <!-- Footer row -->
        <div class="eval-card__footer">
          <span class="eval-card__duration">
            <el-icon class="mr-1"><Clock /></el-icon>
            耗时 {{ item.duration || '--' }}
          </span>
          <div class="eval-card__actions">
            <el-button v-if="item.status === 'COMPLETED'" link type="primary" size="small" @click.stop="viewReport(item)">
              查看报告
            </el-button>
            <el-button v-if="item.status === 'COMPLETED'" link type="primary" size="small" @click.stop="exportReport(item)">
              导出报告
            </el-button>
          </div>
        </div>
      </el-card>
    </div>

    <!-- Expanded detail section -->
    <div v-if="selectedEval && selectedEval.status === 'COMPLETED'" class="eval-detail">
      <el-divider />
      <h3 class="eval-detail__title">评估详情 — {{ selectedEval.title }}</h3>

      <!-- Performance metrics grid -->
      <div class="eval-detail__metrics-grid grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-4">
        <div class="stat-item">
          <div class="stat-label">AUC</div>
          <div class="stat-value">{{ selectedEval.auc?.toFixed(4) }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">F1 Score</div>
          <div class="stat-value">{{ selectedEval.f1?.toFixed(4) }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">Precision</div>
          <div class="stat-value">{{ selectedEval.precision?.toFixed(4) }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">Recall</div>
          <div class="stat-value">{{ selectedEval.recall?.toFixed(4) }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">Sensitivity</div>
          <div class="stat-value">{{ selectedEval.sensitivity?.toFixed(4) }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">Specificity</div>
          <div class="stat-value">{{ selectedEval.specificity?.toFixed(4) }}</div>
        </div>
      </div>

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
    <el-dialog
      v-model="evalModal.visible"
      title="新建评估"
      width="600px"
    >
      <el-form label-width="100px">
        <el-form-item label="选择模型" required>
          <el-select v-model="evalForm.model_id" placeholder="请选择模型">
            <el-option v-for="m in modelOptions" :key="m.id" :value="m.id" :label="m.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择版本" required>
          <el-select v-model="evalForm.version_id" placeholder="请选择版本">
            <el-option v-for="v in versionOptions" :key="v.id" :value="v.id" :label="v.version_no" />
          </el-select>
        </el-form-item>
        <el-form-item label="评估数据集">
          <el-select v-model="evalForm.dataset_id" placeholder="请选择数据集">
            <el-option v-for="d in datasetOptions" :key="d.id" :value="d.id" :label="d.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="评估指标">
          <el-checkbox-group v-model="evalForm.metrics">
            <el-checkbox v-for="o in metricOptions" :key="o.value" :value="o.value">{{ o.label }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="evalModal.close()">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreateEval">确定</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus, Folder, Clock, Warning, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer/index.vue'
import { useModal } from '@/hooks/useModal'
import { useTable } from '@/hooks/useTable'
import { getEvaluations, createEvaluation } from '@/api/model'

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
  COMPLETED: 'success',
  RUNNING: 'primary',
  FAILED: 'danger',
}

const statusTextMap: Record<string, string> = {
  COMPLETED: '已完成',
  RUNNING: '运行中',
  FAILED: '失败',
}

// API data via useTable
const { tableData: evaluations, loading, fetchData } = useTable<Evaluation>(
  (params) => getEvaluations({ page: params.page, page_size: params.pageSize })
)

// Filters
const filters = reactive({
  evalType: '' as string,
  status: '' as string,
  keyword: '' as string,
})

const filteredEvaluations = computed(() => {
  return evaluations.value.filter((item: Evaluation) => {
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
const selectedId = ref<number | undefined>(undefined)

const selectedEval = computed(() => {
  return evaluations.value.find((e: Evaluation) => e.id === selectedId.value)
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

// Select options (to be loaded from API later)
const modelOptions = ref<any[]>([])
const versionOptions = ref<any[]>([])
const datasetOptions = ref<any[]>([])

function viewReport(item: Evaluation) {
  selectedId.value = item.id
}

function exportReport(item: Evaluation) {
  const rows = [
    ['指标', '值'],
    ['AUC', item.auc?.toFixed(4) ?? ''],
    ['F1 Score', item.f1?.toFixed(4) ?? ''],
    ['Precision', item.precision?.toFixed(4) ?? ''],
    ['Recall', item.recall?.toFixed(4) ?? ''],
    ['Sensitivity', item.sensitivity?.toFixed(4) ?? ''],
    ['Specificity', item.specificity?.toFixed(4) ?? ''],
  ]
  if (item.tp != null) {
    rows.push([], ['混淆矩阵', ''], ['标签\\预测', '阳性(P)', '阴性(N)'])
    rows.push(['阳性(P)', `TP: ${item.tp}`, `FP: ${item.fp}`])
    rows.push(['阴性(N)', `FN: ${item.fn}`, `TN: ${item.tn}`])
  }
  const csv = '﻿' + rows.map(r => r.join(',')).join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${item.title}_report.csv`
  a.click()
  window.URL.revokeObjectURL(url)
}

async function handleCreateEval() {
  submitting.value = true
  try {
    await createEvaluation({
      model_id: evalForm.model_id,
      version_id: evalForm.version_id,
      dataset_id: evalForm.dataset_id,
      metrics: evalForm.metrics,
    })
    ElMessage.success('评估任务已创建')
    evalModal.close()
    fetchData()
  } finally {
    submitting.value = false
  }
}

onMounted(() => fetchData())
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
  cursor: pointer;
}
.eval-card--selected {
  box-shadow: 0 0 0 2px #0ea5e9;
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
  color: #0f172a;
}

.eval-card__dataset {
  font-size: 13px;
  color: #94a3b8;
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
  color: #64748b;
}
.metric-item strong {
  color: #0f172a;
  font-variant-numeric: tabular-nums;
}

.eval-card__progress {
  margin-bottom: 12px;
}

.eval-card__error {
  display: flex;
  align-items: flex-start;
  color: #ef4444;
  font-size: 13px;
  margin-bottom: 12px;
  background: #fef2f2;
  border-radius: 4px;
  padding: 8px 12px;
}

.eval-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid #f8fafc;
  padding-top: 10px;
}
.eval-card__duration {
  font-size: 13px;
  color: #94a3b8;
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
  color: #0f172a;
}
.eval-detail__metrics-grid {
  margin-bottom: 32px;
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
  border: 1px solid #f1f5f9;
  text-align: center;
  font-size: 14px;
}
.confusion-table th {
  background: #f8fafc;
  font-weight: 500;
}
.confusion-table .corner-cell {
  width: 60px;
  background: #f8fafc;
}
.confusion-table .row-header {
  background: #f8fafc;
  font-weight: 500;
  text-align: right;
}
.confusion-table .cell-tp {
  background: rgba(16, 185, 129, 0.15);
  font-weight: 600;
}
.confusion-table .cell-tn {
  background: rgba(16, 185, 129, 0.08);
  font-weight: 600;
}
.confusion-table .cell-fp {
  background: rgba(245, 158, 11, 0.12);
}
.confusion-table .cell-fn {
  background: rgba(245, 158, 11, 0.12);
}
</style>
