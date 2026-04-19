<template>
  <PageContainer title="执行监控">
    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <a-table
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      @change="handleTableChange"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'pipelineName'">
          {{ record.pipelineName || `管道#${record.pipelineId}` }}
        </template>
        <template v-if="column.key === 'stepName'">
          {{ record.stepName || '管道级' }}
        </template>
        <template v-if="column.key === 'status'">
          <a-tag :color="statusColorMap[record.status] || 'default'">
            {{ statusLabelMap[record.status] || record.status }}
          </a-tag>
        </template>
        <template v-if="column.key === 'triggerType'">
          <a-tag>{{ record.triggerType }}</a-tag>
        </template>
        <template v-if="column.key === 'progress'">
          <template v-if="record.status === 'RUNNING'">
            <a-progress :percent="calcProgress(record)" size="small" />
          </template>
          <template v-else-if="isCompleted(record) && record.rowsRead > 0">
            {{ formatRows(record.rowsWritten) }}/{{ formatRows(record.rowsRead) }}
          </template>
          <template v-else>-</template>
        </template>
        <template v-if="column.key === 'duration'">
          {{ calcDuration(record) }}
        </template>
        <template v-if="column.key === 'startTime'">
          {{ record.startTime ? formatDateTime(record.startTime) : '-' }}
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="handleViewDetail(record)">详情</a-button>
            <a-button type="link" size="small" @click="handleViewLogs(record)">日志</a-button>
            <a-button v-if="record.status === 'RUNNING'" type="link" danger size="small" @click="handleCancel(record)">取消</a-button>
            <a-button v-if="record.status === 'FAILED'" type="link" size="small" @click="handleRetry(record)">重试</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 执行详情 Drawer -->
    <a-drawer
      v-model:open="detailVisible"
      title="执行详情"
      :width="640"
      destroy-on-close
    >
      <template v-if="detailLoading">
        <div style="text-align: center; padding: 40px 0;">
          <a-spin />
        </div>
      </template>
      <template v-else-if="detailData">
        <a-descriptions bordered :column="2" size="small">
          <a-descriptions-item label="管道">{{ detailData.pipelineName || `管道#${detailData.pipelineId}` }}</a-descriptions-item>
          <a-descriptions-item label="步骤">{{ detailData.stepName || '管道级' }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="statusColorMap[detailData.status] || 'default'">
              {{ statusLabelMap[detailData.status] || detailData.status }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="触发方式">
            <a-tag>{{ detailData.triggerType }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="开始时间">{{ detailData.startTime ? formatDateTime(detailData.startTime) : '-' }}</a-descriptions-item>
          <a-descriptions-item label="结束时间">{{ detailData.endTime ? formatDateTime(detailData.endTime) : '-' }}</a-descriptions-item>
          <a-descriptions-item label="读取行数">{{ formatRows(detailData.rowsRead) }}</a-descriptions-item>
          <a-descriptions-item label="写入行数">{{ formatRows(detailData.rowsWritten) }}</a-descriptions-item>
          <a-descriptions-item label="跳过行数">{{ formatRows(detailData.rowsSkipped) }}</a-descriptions-item>
          <a-descriptions-item label="错误行数">{{ formatRows(detailData.errorRows) }}</a-descriptions-item>
        </a-descriptions>

        <template v-if="detailData.errorMessage">
          <a-divider orientation="left">错误信息</a-divider>
          <a-typography-paragraph
            :content="detailData.errorMessage"
            :ellipsis="{ rows: 6, expandable: true }"
            copyable
            code
          />
        </template>

        <template v-if="detailData.engineConfig">
          <a-divider orientation="left">引擎配置</a-divider>
          <a-typography-paragraph
            :content="detailData.engineConfig"
            :ellipsis="{ rows: 8, expandable: true }"
            copyable
            code
          />
        </template>
      </template>
    </a-drawer>

    <!-- 日志 Drawer -->
    <a-drawer
      v-model:open="logVisible"
      title="执行日志"
      :width="640"
      destroy-on-close
    >
      <div v-if="logLoading" style="text-align: center; padding: 40px 0;">
        <a-spin />
      </div>
      <pre v-else class="log-viewer">{{ logContent || '暂无日志' }}</pre>
    </a-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
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
const statusColorMap: Record<string, string> = {
  PENDING: 'default',
  RUNNING: 'processing',
  SUCCESS: 'success',
  FAILED: 'error',
  CANCELLED: 'warning',
  SKIPPED: 'default',
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
const columns = [
  { title: '管道', key: 'pipelineName', width: 150, ellipsis: true },
  { title: '步骤', key: 'stepName', width: 120, ellipsis: true },
  { title: '状态', key: 'status', width: 100 },
  { title: '触发', key: 'triggerType', width: 90 },
  { title: '进度', key: 'progress', width: 160 },
  { title: '耗时', key: 'duration', width: 100 },
  { title: '开始时间', key: 'startTime', width: 170 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' as const },
]

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getEtlExecutions({
    page: params.page,
    page_size: params.pageSize,
    ...currentSearchParams,
  }),
)

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
    message.success('已发送取消请求')
    fetchData()
  } catch {
    // error handled by request interceptor
  }
}

async function handleRetry(record: any) {
  try {
    await retryEtlExecution(record.id)
    message.success('重试任务已启动')
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
</style>
