<template>
  <PageContainer title="执行监控">
    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <el-table :data="tableData" v-loading="loading" row-key="id" size="default">
      <el-table-column label="管道" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.pipelineName || `管道#${row.pipelineId}` }}
        </template>
      </el-table-column>
      <el-table-column label="步骤" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.stepName || '管道级' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusColorMap[row.status] || 'info'">
            {{ statusLabelMap[row.status] || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="触发" width="90">
        <template #default="{ row }">
          <el-tag type="info">{{ row.triggerType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="进度" width="160">
        <template #default="{ row }">
          <template v-if="row.status === 'RUNNING'">
            <el-progress :percentage="calcProgress(row)" :stroke-width="6" />
          </template>
          <template v-else-if="isCompleted(row) && row.rowsRead > 0">
            {{ formatRows(row.rowsWritten) }}/{{ formatRows(row.rowsRead) }}
          </template>
          <template v-else>-</template>
        </template>
      </el-table-column>
      <el-table-column label="耗时" width="100">
        <template #default="{ row }">
          {{ calcDuration(row) }}
        </template>
      </el-table-column>
      <el-table-column label="开始时间" width="170">
        <template #default="{ row }">
          {{ row.startTime ? formatDateTime(row.startTime) : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleViewDetail(row)">详情</el-button>
          <el-button link type="primary" size="small" @click="handleViewLogs(row)">日志</el-button>
          <el-button v-if="row.status === 'RUNNING'" link type="danger" size="small" @click="handleCancel(row)">取消</el-button>
          <el-button v-if="row.status === 'FAILED'" link type="primary" size="small" @click="handleRetry(row)">重试</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      class="mt-4 justify-end"
      background
      layout="total, sizes, prev, pager, next, jumper"
      :total="pagination.total"
      :current-page="pagination.current"
      :page-size="pagination.pageSize"
      :page-sizes="[10, 20, 50, 100]"
      @current-change="handlePageChange"
      @size-change="handleSizeChange"
    />

    <!-- 执行详情 Drawer -->
    <el-drawer
      v-model="detailVisible"
      title="执行详情"
      size="640px"
      :destroy-on-close="true"
    >
      <div v-loading="detailLoading" style="min-height: 120px">
        <template v-if="detailData">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="管道">{{ detailData.pipelineName || `管道#${detailData.pipelineId}` }}</el-descriptions-item>
            <el-descriptions-item label="步骤">{{ detailData.stepName || '管道级' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusColorMap[detailData.status] || 'info'">
                {{ statusLabelMap[detailData.status] || detailData.status }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="触发方式">
              <el-tag type="info">{{ detailData.triggerType }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="开始时间">{{ detailData.startTime ? formatDateTime(detailData.startTime) : '-' }}</el-descriptions-item>
            <el-descriptions-item label="结束时间">{{ detailData.endTime ? formatDateTime(detailData.endTime) : '-' }}</el-descriptions-item>
            <el-descriptions-item label="读取行数">{{ formatRows(detailData.rowsRead) }}</el-descriptions-item>
            <el-descriptions-item label="写入行数">{{ formatRows(detailData.rowsWritten) }}</el-descriptions-item>
            <el-descriptions-item label="跳过行数">{{ formatRows(detailData.rowsSkipped) }}</el-descriptions-item>
            <el-descriptions-item label="错误行数">{{ formatRows(detailData.errorRows) }}</el-descriptions-item>
          </el-descriptions>

          <template v-if="detailData.errorMessage">
            <el-divider content-position="left">错误信息</el-divider>
            <div class="code-block">
              <div class="code-block__bar">
                <el-button link type="primary" size="small" @click="copyText(detailData.errorMessage)">复制</el-button>
              </div>
              <pre class="code-block__body">{{ detailData.errorMessage }}</pre>
            </div>
          </template>

          <template v-if="detailData.engineConfig">
            <el-divider content-position="left">引擎配置</el-divider>
            <div class="code-block">
              <div class="code-block__bar">
                <el-button link type="primary" size="small" @click="copyText(detailData.engineConfig)">复制</el-button>
              </div>
              <pre class="code-block__body">{{ detailData.engineConfig }}</pre>
            </div>
          </template>
        </template>
      </div>
    </el-drawer>

    <!-- 日志 Drawer -->
    <el-drawer
      v-model="logVisible"
      title="执行日志"
      size="640px"
      :destroy-on-close="true"
    >
      <div v-loading="logLoading" style="min-height: 120px">
        <pre v-if="!logLoading" class="log-viewer">{{ logContent || '暂无日志' }}</pre>
      </div>
    </el-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer/index.vue'
import SearchForm from '@/components/SearchForm/index.vue'
import { useTable } from '@/hooks/useTable'
import {
  getEtlExecutions,
  getEtlExecution,
  getEtlExecutionLogs,
  cancelEtlExecution,
  retryEtlExecution,
} from '@/api/etl'
import { formatDateTime } from '@/utils/date'

defineOptions({ name: 'EtlExecutionList' })

// ===== 常量 =====
const statusColorMap: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
  PENDING: 'info',
  RUNNING: 'primary',
  SUCCESS: 'success',
  FAILED: 'danger',
  CANCELLED: 'warning',
  SKIPPED: 'info',
}

const statusLabelMap: Record<string, string> = {
  PENDING: '等待中',
  RUNNING: '运行中',
  SUCCESS: '成功',
  FAILED: '失败',
  CANCELLED: '已取消',
  SKIPPED: '已跳过',
}

// ===== 搜索 =====
const searchFields = [
  {
    name: 'status',
    label: '状态',
    type: 'select' as const,
    options: [
      { label: '等待中', value: 'PENDING' },
      { label: '运行中', value: 'RUNNING' },
      { label: '成功', value: 'SUCCESS' },
      { label: '失败', value: 'FAILED' },
      { label: '已取消', value: 'CANCELLED' },
      { label: '已跳过', value: 'SKIPPED' },
    ],
  },
  {
    name: 'triggerType',
    label: '触发方式',
    type: 'select' as const,
    options: [
      { label: '手动', value: 'MANUAL' },
      { label: '定时', value: 'SCHEDULE' },
      { label: '重试', value: 'RETRY' },
    ],
  },
]

let currentSearchParams: Record<string, any> = {}

function handleSearch(values: Record<string, any>) {
  currentSearchParams = values
  fetchData({ page: 1 })
}

function handleReset() {
  currentSearchParams = {}
  fetchData({ page: 1 })
}

// ===== 表格 =====
const { tableData, loading, pagination, fetchData } = useTable<any>(
  (params) => getEtlExecutions({
    page: params.page,
    page_size: params.pageSize,
    ...currentSearchParams,
  }),
)

function handlePageChange(page: number) {
  pagination.current = page
  fetchData({ page })
}

function handleSizeChange(size: number) {
  pagination.pageSize = size
  pagination.current = 1
  fetchData({ page: 1, pageSize: size })
}

// ===== 辅助函数 =====
function isCompleted(record: any): boolean {
  return ['SUCCESS', 'FAILED', 'CANCELLED', 'SKIPPED'].includes(record.status)
}

function calcProgress(record: any): number {
  if (record.rowsRead > 0) {
    return Math.round(record.rowsWritten / record.rowsRead * 100)
  }
  return 0
}

function calcDuration(record: any): string {
  if (!record.startTime) return '-'
  const end = record.endTime ? new Date(record.endTime).getTime() : Date.now()
  const start = new Date(record.startTime).getTime()
  const diff = end - start
  if (diff < 0) return '-'
  if (diff < 1000) return `${diff}ms`
  if (diff < 60000) return `${(diff / 1000).toFixed(1)}s`
  const minutes = Math.floor(diff / 60000)
  const seconds = Math.floor((diff % 60000) / 1000)
  return `${minutes}m${seconds}s`
}

function formatRows(n: number | undefined | null): string {
  if (n == null) return '0'
  if (n < 1000) return String(n)
  if (n < 1000000) return `${(n / 1000).toFixed(1)}K`
  return `${(n / 1000000).toFixed(2)}M`
}

async function copyText(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败')
  }
}

// ===== 详情 Drawer =====
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<any>(null)

async function handleViewDetail(record: any) {
  detailVisible.value = true
  detailLoading.value = true
  detailData.value = null
  try {
    const res = await getEtlExecution(record.id)
    detailData.value = res.data.data
  } finally {
    detailLoading.value = false
  }
}

// ===== 日志 Drawer =====
const logVisible = ref(false)
const logLoading = ref(false)
const logContent = ref('')

async function handleViewLogs(record: any) {
  logVisible.value = true
  logLoading.value = true
  logContent.value = ''
  try {
    const res = await getEtlExecutionLogs(record.id)
    logContent.value = res.data.data || ''
  } finally {
    logLoading.value = false
  }
}

// ===== 操作 =====
async function handleCancel(record: any) {
  try {
    await cancelEtlExecution(record.id)
    ElMessage.success('已发送取消请求')
    fetchData()
  } catch {
    // error handled by request interceptor
  }
}

async function handleRetry(record: any) {
  try {
    await retryEtlExecution(record.id)
    ElMessage.success('重试任务已启动')
    fetchData()
  } catch {
    // error handled by request interceptor
  }
}
</script>

<style scoped>
.log-viewer {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 16px;
  border-radius: 6px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: calc(100vh - 120px);
  overflow-y: auto;
  margin: 0;
}

.code-block {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
}

.code-block__bar {
  display: flex;
  justify-content: flex-end;
  padding: 4px 8px;
  background: #f8fafc;
  border-bottom: 1px solid #f1f5f9;
}

.code-block__body {
  margin: 0;
  padding: 12px;
  background: #1e1e1e;
  color: #d4d4d4;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 260px;
  overflow-y: auto;
}
</style>
