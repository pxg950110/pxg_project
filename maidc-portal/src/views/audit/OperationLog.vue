<template>
  <PageContainer title="操作审计">
    <!-- Filter bar -->
    <div class="filter-bar">
      <div class="filter-controls">
        <a-select v-model:value="filters.module" placeholder="模块" style="width: 140px" allow-clear>
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="auth">认证服务</a-select-option>
          <a-select-option value="data">数据服务</a-select-option>
          <a-select-option value="model">模型服务</a-select-option>
          <a-select-option value="label">标注服务</a-select-option>
          <a-select-option value="task">任务服务</a-select-option>
          <a-select-option value="audit">审计服务</a-select-option>
        </a-select>

        <a-select v-model:value="filters.operation" placeholder="操作类型" style="width: 140px" allow-clear>
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="login">登录</a-select-option>
          <a-select-option value="logout">登出</a-select-option>
          <a-select-option value="refresh">刷新令牌</a-select-option>
        </a-select>

        <a-select v-model:value="filters.status" placeholder="状态" style="width: 120px" allow-clear>
          <a-select-option value="">全部</a-select-option>
          <a-select-option :value="1">成功</a-select-option>
          <a-select-option :value="0">失败</a-select-option>
        </a-select>

        <a-range-picker v-model:value="filters.dateRange" style="width: 260px" />

        <a-input-search
          v-model:value="filters.keyword"
          placeholder="搜索操作人..."
          style="width: 200px"
          allow-clear
        />
      </div>
      <a-button type="primary" @click="handleExport">导出</a-button>
    </div>

    <!-- Table -->
    <a-table
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      size="small"
      :scroll="{ x: 1260 }"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'createdAt'">
          {{ formatDateTime(record.createdAt) }}
        </template>
        <template v-if="column.key === 'module'">
          <a-tag :color="moduleColors[record.module as keyof typeof moduleColors] || 'default'">{{ record.module }}</a-tag>
        </template>
        <template v-if="column.key === 'requestMethod'">
          <a-badge :color="methodColors[record.requestMethod as keyof typeof methodColors] || 'default'" :text="record.requestMethod" />
        </template>
        <template v-if="column.key === 'duration'">
          <span :style="{ color: durationColor(record.duration), fontWeight: 500 }">{{ record.duration }}ms</span>
        </template>
        <template v-if="column.key === 'status'">
          <a-badge v-if="record.status === 1" status="success" text="成功" />
          <a-badge v-else status="error" text="失败" />
        </template>
        <template v-if="column.key === 'action'">
          <a @click="openDetail(record)">详情</a>
        </template>
      </template>
    </a-table>

    <!-- Detail Drawer -->
    <a-drawer
      v-model:open="detailVisible"
      title="操作详情"
      width="640"
      :destroy-on-close="true"
    >
      <template v-if="currentRecord">
        <!-- Section 1: Basic info -->
        <a-descriptions title="操作基本信息" :column="2" bordered size="small">
          <a-descriptions-item label="操作类型">{{ currentRecord.operation }}</a-descriptions-item>
          <a-descriptions-item label="操作人">{{ currentRecord.username }}</a-descriptions-item>
          <a-descriptions-item label="时间">{{ formatDateTime(currentRecord.createdAt) }}</a-descriptions-item>
          <a-descriptions-item label="IP地址">{{ currentRecord.ip }}</a-descriptions-item>
          <a-descriptions-item label="模块">{{ currentRecord.module }}</a-descriptions-item>
          <a-descriptions-item label="请求路径">
            <span style="font-family: monospace; font-size: 13px">{{ currentRecord.requestUrl }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="请求方法">{{ currentRecord.requestMethod }}</a-descriptions-item>
          <a-descriptions-item label="调用方法">
            <span style="font-family: monospace; font-size: 13px">{{ currentRecord.method }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="耗时">
            <span :style="{ color: durationColor(currentRecord.duration), fontWeight: 500 }">{{ currentRecord.duration }}ms</span>
          </a-descriptions-item>
          <a-descriptions-item label="操作结果">
            <a-badge v-if="currentRecord.status === 1" status="success" text="成功" />
            <a-badge v-else status="error" text="失败" />
          </a-descriptions-item>
        </a-descriptions>

        <!-- Section 2: Request params -->
        <div v-if="currentRecord.params" class="drawer-section">
          <div class="section-title">请求参数</div>
          <div class="json-card json-card-gray">
            <pre><code>{{ currentRecord.params }}</code></pre>
          </div>
        </div>

        <!-- Section 3: Error detail (only on failure) -->
        <div v-if="currentRecord.status === 0 && currentRecord.errorMsg" class="drawer-section">
          <div class="section-title">错误详情</div>
          <div class="json-card json-card-red">
            <pre><code>{{ currentRecord.errorMsg }}</code></pre>
          </div>
        </div>
      </template>
    </a-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import { useTable } from '@/hooks/useTable'
import { formatDateTime } from '@/utils/date'
import { getAuditLogs, exportAuditLogs } from '@/api/audit'

// --- Columns ---
const columns = [
  { title: '时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '操作人', dataIndex: 'username', key: 'username', width: 100 },
  { title: '模块', dataIndex: 'module', key: 'module', width: 100 },
  { title: '操作', dataIndex: 'operation', key: 'operation', width: 100 },
  { title: '请求方法', dataIndex: 'requestMethod', key: 'requestMethod', width: 90 },
  { title: '请求路径', dataIndex: 'requestUrl', key: 'requestUrl', width: 220, ellipsis: true },
  { title: 'IP', dataIndex: 'ip', key: 'ip', width: 130 },
  { title: '耗时', dataIndex: 'duration', key: 'duration', width: 90 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 60, fixed: 'right' as const },
]

// --- Filters ---
const filters = reactive({
  module: undefined as string | undefined,
  operation: undefined as string | undefined,
  status: undefined as number | undefined,
  dateRange: null as any,
  keyword: '',
})

// --- Method badge colors ---
const methodColors: Record<string, string> = {
  GET: 'blue',
  POST: 'green',
  PUT: 'orange',
  DELETE: 'red',
}

// --- Module tag colors ---
const moduleColors: Record<string, string> = {
  auth: 'purple',
  data: 'cyan',
  model: 'blue',
  label: 'geekblue',
  task: 'orange',
  audit: 'gold',
  msg: 'green',
  gateway: 'volcano',
}

// --- Duration color ---
function durationColor(ms: number): string {
  if (ms < 100) return '#52c41a'
  if (ms < 500) return 'rgba(0,0,0,0.65)'
  if (ms < 1000) return '#faad14'
  return '#ff4d4f'
}

// --- API integration ---
const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getAuditLogs({
    page: params.page,
    pageSize: params.pageSize,
    module: filters.module,
    operation: filters.operation,
    username: filters.keyword,
    status: filters.status,
    startTime: filters.dateRange?.[0] ? formatDateTime(filters.dateRange[0]) : undefined,
    endTime: filters.dateRange?.[1] ? formatDateTime(filters.dateRange[1]) : undefined
  })
)

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
  } catch { message.error('导出失败') }
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

/* URL column monospace */
:deep(.ant-table) {
  font-size: 13px;
}

.drawer-section {
  margin-top: 24px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
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
  background: #f5f5f5;
  border: 1px solid #d9d9d9;
}

.json-card-green {
  background: #f6ffed;
  border: 1px solid #b7eb8f;
}

.json-card-red {
  background: #fff2f0;
  border: 1px solid #ffccc7;
}

/* Ensure fixed action column alignment */
:deep(.ant-table-cell) {
  white-space: nowrap;
}
</style>
