<template>
  <PageContainer title="系统事件">
    <!-- Filter bar -->
    <div class="filter-bar">
      <div class="filter-controls">
        <el-select v-model="filters.eventType" placeholder="事件类型" clearable style="width: 160px">
          <el-option label="全部" value="" />
          <el-option label="越权拒绝" value="PERMISSION_DENIED" />
          <el-option label="服务启动" value="SERVICE_START" />
          <el-option label="服务停止" value="SERVICE_STOP" />
          <el-option label="配置变更" value="CONFIG_CHANGE" />
          <el-option label="部署" value="DEPLOY" />
          <el-option label="告警" value="ALERT" />
        </el-select>

        <el-select v-model="filters.eventLevel" placeholder="级别" clearable style="width: 120px">
          <el-option label="全部" value="" />
          <el-option label="INFO" value="INFO" />
          <el-option label="WARN" value="WARN" />
          <el-option label="ERROR" value="ERROR" />
          <el-option label="CRITICAL" value="CRITICAL" />
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
          v-model="filters.traceId"
          placeholder="Trace ID 全链路检索"
          clearable
          style="width: 230px"
        />
      </div>
      <el-button type="primary" @click="handleExport">
        <el-icon class="mr-1"><Download /></el-icon>
        导出
      </el-button>
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
      <el-table-column label="事件类型" width="120">
        <template #default="{ row }">
          <span class="event-type">{{ eventTypeLabels[row.eventType] || row.eventType }}</span>
        </template>
      </el-table-column>
      <el-table-column label="级别" width="100">
        <template #default="{ row }">
          <el-tag :type="levelTypes[row.eventLevel] || 'info'" :effect="row.eventLevel === 'CRITICAL' ? 'dark' : 'light'">{{ row.eventLevel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="来源" prop="source" width="140" />
      <el-table-column label="标题" prop="eventTitle" min-width="180" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <span class="inline-flex items-center gap-1.5">
            <span class="inline-block h-2 w-2 rounded-full" :style="{ background: row.resolved ? '#10b981' : '#f59e0b' }" />
            {{ row.resolved ? '已解决' : '待处理' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="70" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
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
      :title="currentEvent?.eventTitle || '事件详情'"
      size="640px"
      :destroy-on-close="true"
    >
      <template v-if="currentEvent">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="事件类型">{{ eventTypeLabels[currentEvent.eventType] || currentEvent.eventType }}</el-descriptions-item>
          <el-descriptions-item label="级别">
            <el-tag :type="levelTypes[currentEvent.eventLevel] || 'info'" :effect="currentEvent.eventLevel === 'CRITICAL' ? 'dark' : 'light'">{{ currentEvent.eventLevel }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="来源">{{ currentEvent.source }}</el-descriptions-item>
          <el-descriptions-item label="时间">{{ formatDateTime(currentEvent.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="Trace ID">
            <span v-if="currentEvent.traceId" class="trace-id" @click="copyTraceId(currentEvent.traceId)">{{ currentEvent.traceId }}</span>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="详情">{{ currentEvent.eventDetail || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <span class="inline-flex items-center gap-1.5">
              <span class="inline-block h-2 w-2 rounded-full" :style="{ background: currentEvent.resolved ? '#10b981' : '#f59e0b' }" />
              {{ currentEvent.resolved ? '已解决' : '待处理' }}
            </span>
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="currentEvent.eventData" class="drawer-section">
          <div class="section-title">事件数据</div>
          <div class="json-card">
            <pre><code>{{ formatJson(currentEvent.eventData) }}</code></pre>
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
import { getSystemEvents, exportSystemEvents } from '@/api/audit'

// --- Filters ---
const filters = reactive({
  eventType: undefined as string | undefined,
  eventLevel: undefined as string | undefined,
  dateRange: null as any,
  traceId: '',
})

// --- Level tag types (CRITICAL renders dark for extra emphasis) ---
const levelTypes: Record<string, 'primary' | 'warning' | 'danger'> = {
  INFO: 'primary',
  WARN: 'warning',
  ERROR: 'danger',
  CRITICAL: 'danger',
}

// --- Event type labels ---
const eventTypeLabels: Record<string, string> = {
  PERMISSION_DENIED: '越权拒绝',
  SERVICE_START: '服务启动',
  SERVICE_STOP: '服务停止',
  CONFIG_CHANGE: '配置变更',
  DEPLOY: '部署',
  ALERT: '告警',
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

// --- Detail drawer ---
const detailVisible = ref(false)
const currentEvent = ref<any>(null)

function handleDetail(record: any) {
  currentEvent.value = record
  detailVisible.value = true
}

// --- Format JSON ---
function formatJson(str: string | null): string {
  if (!str) return '-'
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch {
    return str
  }
}

// --- Export ---
async function handleExport() {
  try {
    const res = await exportSystemEvents({
      eventType: filters.eventType,
      eventLevel: filters.eventLevel,
      traceId: filters.traceId || undefined,
      startTime: filters.dateRange?.[0] ? formatDateTime(filters.dateRange[0]) : undefined,
      endTime: filters.dateRange?.[1] ? formatDateTime(filters.dateRange[1]) : undefined,
    })
    const blob = new Blob([res.data], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'system_events.csv'
    a.click()
    window.URL.revokeObjectURL(url)
  } catch { ElMessage.error('导出失败') }
}

// --- API integration ---
const { tableData, loading, pagination, fetchData } = useTable<any>(
  (params) => getSystemEvents({
    page: params.page,
    pageSize: params.pageSize,
    eventType: filters.eventType,
    eventLevel: filters.eventLevel,
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

.event-type {
  font-size: 13px;
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
  background: #f8fafc;
  border: 1px solid #e2e8f0;
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
</style>
