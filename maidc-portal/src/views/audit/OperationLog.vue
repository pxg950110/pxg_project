<template>
  <PageContainer title="操作审计">
    <!-- Filter bar -->
    <div class="filter-bar">
      <div class="filter-controls">
        <el-select v-model="filters.module" placeholder="模块" clearable style="width: 140px">
          <el-option label="全部" value="" />
          <el-option label="认证服务" value="auth" />
          <el-option label="数据服务" value="data" />
          <el-option label="模型服务" value="model" />
          <el-option label="标注服务" value="label" />
          <el-option label="任务服务" value="task" />
          <el-option label="审计服务" value="audit" />
        </el-select>

        <el-select v-model="filters.operation" placeholder="操作类型" clearable style="width: 140px">
          <el-option label="全部" value="" />
          <el-option label="登录" value="login" />
          <el-option label="登出" value="logout" />
          <el-option label="刷新令牌" value="refresh" />
        </el-select>

        <el-select v-model="filters.status" placeholder="状态" clearable style="width: 120px">
          <el-option label="全部" value="" />
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAILURE" />
        </el-select>

        <el-date-picker
          v-model="filters.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 260px"
        />

        <el-input
          v-model="filters.keyword"
          placeholder="搜索操作人..."
          clearable
          style="width: 200px"
        />

        <el-input
          v-model="filters.traceId"
          placeholder="Trace ID 全链路检索"
          clearable
          style="width: 230px"
        />
      </div>
      <el-button type="primary" @click="handleExport">导出</el-button>
    </div>

    <!-- Table -->
    <el-table
      :data="tableData"
      v-loading="loading"
      row-key="id"
      size="small"
    >
      <el-table-column label="时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="Trace ID" width="130">
        <template #default="{ row }">
          <el-tooltip v-if="row.traceId" :content="`${row.traceId}（点击复制）`">
            <span class="trace-id" @click="copyTraceId(row.traceId)">{{ shortTraceId(row.traceId) }}</span>
          </el-tooltip>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作人" prop="username" width="100" />
      <el-table-column label="模块" width="110">
        <template #default="{ row }">
          <span class="inline-flex items-center gap-1.5">
            <span class="inline-block h-2 w-2 rounded-full" :style="{ background: moduleDots[row.serviceName] || '#94a3b8' }" />
            {{ row.serviceName }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="操作" prop="operation" width="100" />
      <el-table-column label="请求方法" width="100">
        <template #default="{ row }">
          <span class="inline-flex items-center gap-1.5">
            <span class="inline-block h-2 w-2 rounded-full" :style="{ background: methodDots[row.requestMethod] || '#94a3b8' }" />
            {{ row.requestMethod }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="请求路径" prop="requestUrl" width="220" show-overflow-tooltip />
      <el-table-column label="IP" prop="ipAddress" width="130" />
      <el-table-column label="耗时" width="90">
        <template #default="{ row }">
          <span :style="{ color: durationColor(row.durationMs), fontWeight: 500 }">{{ row.durationMs }}ms</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <span class="inline-flex items-center gap-1.5">
            <span class="inline-block h-2 w-2 rounded-full" :style="{ background: statusMeta(row.status).color }" />
            {{ statusMeta(row.status).text }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="70" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
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

    <!-- Detail Drawer -->
    <el-drawer
      v-model="detailVisible"
      title="操作详情"
      size="640px"
      :destroy-on-close="true"
    >
      <template v-if="currentRecord">
        <!-- Section 1: Basic info -->
        <el-descriptions title="操作基本信息" :column="2" border size="small">
          <el-descriptions-item label="操作类型">{{ currentRecord.operation }}</el-descriptions-item>
          <el-descriptions-item label="操作人">{{ currentRecord.username }}</el-descriptions-item>
          <el-descriptions-item label="时间">{{ formatDateTime(currentRecord.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="IP地址">{{ currentRecord.ipAddress }}</el-descriptions-item>
          <el-descriptions-item label="Trace ID" :span="2">
            <span v-if="currentRecord.traceId" class="trace-id trace-id-full" @click="copyTraceId(currentRecord.traceId)">{{ currentRecord.traceId }}</span>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="模块">{{ currentRecord.serviceName }}</el-descriptions-item>
          <el-descriptions-item label="请求路径">
            <span style="font-family: monospace; font-size: 13px">{{ currentRecord.requestUrl }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="请求方法">{{ currentRecord.requestMethod }}</el-descriptions-item>
          <el-descriptions-item label="耗时">
            <span :style="{ color: durationColor(currentRecord.durationMs), fontWeight: 500 }">{{ currentRecord.durationMs }}ms</span>
          </el-descriptions-item>
          <el-descriptions-item label="操作结果">
            <span class="inline-flex items-center gap-1.5">
              <span class="inline-block h-2 w-2 rounded-full" :style="{ background: statusMeta(currentRecord.status).color }" />
              {{ statusMeta(currentRecord.status).text }}
            </span>
          </el-descriptions-item>
        </el-descriptions>

        <!-- Section 2: Request params -->
        <div v-if="currentRecord.requestParams" class="drawer-section">
          <div class="section-title">请求参数</div>
          <div class="json-card json-card-gray">
            <pre><code>{{ formatJson(currentRecord.requestParams) }}</code></pre>
          </div>
        </div>

        <!-- Section 3: Error detail (only on failure) -->
        <div v-if="currentRecord.status !== 'SUCCESS' && currentRecord.errorMessage" class="drawer-section">
          <div class="section-title">错误详情</div>
          <div class="json-card json-card-red">
            <pre><code>{{ currentRecord.errorMessage }}</code></pre>
          </div>
        </div>
      </template>
    </el-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer/index.vue'
import { useTable } from '@/hooks/useTable'
import { formatDateTime } from '@/utils/date'
import { getAuditLogs, exportAuditLogs } from '@/api/audit'

// --- Filters ---
const filters = reactive({
  module: undefined as string | undefined,
  operation: undefined as string | undefined,
  status: undefined as string | undefined,
  dateRange: null as any,
  keyword: '',
  traceId: '',
})

// --- Method / module dot colors ---
const methodDots: Record<string, string> = {
  GET: '#0ea5e9',
  POST: '#10b981',
  PUT: '#f59e0b',
  DELETE: '#ef4444',
}

const moduleDots: Record<string, string> = {
  auth: '#8b5cf6',
  data: '#06b6d4',
  model: '#0ea5e9',
  label: '#6366f1',
  task: '#f59e0b',
  audit: '#eab308',
  msg: '#10b981',
  gateway: '#f97316',
}

// --- Duration color ---
function durationColor(ms: number): string {
  if (ms < 100) return '#10b981'
  if (ms < 500) return '#475569'
  if (ms < 1000) return '#f59e0b'
  return '#ef4444'
}

// --- Status dot/text ---
function statusMeta(status: string) {
  return status === 'SUCCESS' ? { color: '#10b981', text: '成功' } : { color: '#ef4444', text: '失败' }
}

// --- Trace ID helpers ---
function shortTraceId(id: string): string {
  return id.length > 14 ? `${id.slice(0, 8)}…${id.slice(-4)}` : id
}

async function copyTraceId(id: string) {
  try {
    await navigator.clipboard.writeText(id)
    ElMessage.success('已复制 Trace ID')
  } catch {
    const input = document.createElement('textarea')
    input.value = id
    document.body.appendChild(input)
    input.select()
    document.execCommand('copy')
    document.body.removeChild(input)
    ElMessage.success('已复制 Trace ID')
  }
}

// --- API integration ---
const { tableData, loading, pagination, fetchData } = useTable<any>(
  (params) => getAuditLogs({
    page: params.page,
    pageSize: params.pageSize,
    module: filters.module,
    operation: filters.operation,
    username: filters.keyword,
    status: filters.status,
    traceId: filters.traceId || undefined,
    startTime: filters.dateRange?.[0] ? formatDateTime(filters.dateRange[0]) : undefined,
    endTime: filters.dateRange?.[1] ? formatDateTime(filters.dateRange[1]) : undefined
  })
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

watch(filters, () => fetchData({ page: 1 }))

onMounted(() => {
  fetchData()
})

// --- Format JSON ---
function formatJson(str: string | null): string {
  if (!str) return '-'
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch {
    return str
  }
}

// --- Detail drawer ---
const detailVisible = ref(false)
const currentRecord = ref<any>(null)

function openDetail(record: any) {
  currentRecord.value = record
  detailVisible.value = true
}

// --- Export ---
async function handleExport() {
  try {
    const res = await exportAuditLogs({
      module: filters.module,
      operation: filters.operation,
      username: filters.keyword || undefined,
      status: filters.status,
      traceId: filters.traceId || undefined,
      startTime: filters.dateRange?.[0] ? formatDateTime(filters.dateRange[0]) : undefined,
      endTime: filters.dateRange?.[1] ? formatDateTime(filters.dateRange[1]) : undefined,
    })
    const blob = new Blob([res.data], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'operation_logs.csv'
    a.click()
    window.URL.revokeObjectURL(url)
  } catch { ElMessage.error('导出失败') }
}
</script>

<style scoped>
.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-controls {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

:deep(.el-table .cell) {
  white-space: nowrap;
}

/* Trace ID：monospace + 青色调（呼应深色科技风主色），可点击复制 */
.trace-id {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
  color: #08979c;
  cursor: pointer;
}

.trace-id:hover {
  text-decoration: underline;
}

.trace-id-full {
  font-size: 13px;
  word-break: break-all;
}

.drawer-section {
  margin-top: 24px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 8px;
}

.json-card {
  border-radius: 6px;
  padding: 12px 16px;
  overflow-x: auto;
}

.json-card pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.json-card code {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
  line-height: 1.6;
}

.json-card-gray {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.json-card-red {
  background: #fef2f2;
  border: 1px solid #fecaca;
}
</style>
