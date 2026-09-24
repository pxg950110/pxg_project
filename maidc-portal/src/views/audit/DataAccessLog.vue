<template>
  <PageContainer title="数据访问审计">
    <!-- Filter bar -->
    <div class="filter-bar">
      <div class="filter-controls">
        <el-select v-model="filters.dataDomain" placeholder="数据域" clearable style="width: 140px">
          <el-option label="全部" value="" />
          <el-option label="CDR" value="CDR" />
          <el-option label="RDR" value="RDR" />
          <el-option label="模型数据" value="MODEL" />
        </el-select>

        <el-select v-model="filters.accessType" placeholder="操作类型" clearable style="width: 130px">
          <el-option label="全部" value="" />
          <el-option label="查询" value="QUERY" />
          <el-option label="导出" value="EXPORT" />
          <el-option label="下载" value="DOWNLOAD" />
          <el-option label="共享" value="SHARE" />
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
          placeholder="搜索操作人ID..."
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
      <el-table-column label="操作人ID" prop="userId" width="100" />
      <el-table-column label="数据域" width="110">
        <template #default="{ row }">
          <span v-if="row.dataDomain" class="inline-flex items-center gap-1.5">
            <span class="inline-block h-2 w-2 rounded-full" :style="{ background: domainDots[row.dataDomain] || '#94a3b8' }" />
            {{ row.dataDomain }}
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="访问类型" width="100">
        <template #default="{ row }">
          <span :style="{ color: accessColors[row.accessType] || '#475569', fontWeight: 500 }">{{ accessLabels[row.accessType] || row.accessType || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="表名" prop="tableName" width="160" show-overflow-tooltip />
      <el-table-column label="患者ID" width="110">
        <template #default="{ row }">{{ row.patientId ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="访问目的" prop="purpose" width="160" show-overflow-tooltip />
      <el-table-column label="IP地址" prop="ipAddress" width="130" />
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
  </PageContainer>
</template>

<script setup lang="ts">
import { reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer/index.vue'
import { useTable } from '@/hooks/useTable'
import { formatDateTime } from '@/utils/date'
import { getDataAccessLogs, exportDataAccessLogs } from '@/api/audit'

// --- Filters ---
const filters = reactive({
  dataDomain: undefined as string | undefined,
  accessType: undefined as string | undefined,
  dateRange: null as any,
  keyword: '',
  traceId: '',
})

// --- Domain dot colors ---
const domainDots: Record<string, string> = {
  CDR: '#0ea5e9',
  RDR: '#8b5cf6',
  MODEL: '#06b6d4',
}

// --- Access type colors / labels ---
const accessColors: Record<string, string> = {
  QUERY: '#475569',
  EXPORT: '#0ea5e9',
  DOWNLOAD: '#06b6d4',
  SHARE: '#8b5cf6',
}

const accessLabels: Record<string, string> = {
  QUERY: '查询',
  EXPORT: '导出',
  DOWNLOAD: '下载',
  SHARE: '共享',
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

// --- Export ---
async function handleExport() {
  try {
    const res = await exportDataAccessLogs({
      dataDomain: filters.dataDomain,
      accessType: filters.accessType,
      userId: filters.keyword || undefined,
      traceId: filters.traceId || undefined,
      startTime: filters.dateRange?.[0] ? formatDateTime(filters.dateRange[0]) : undefined,
      endTime: filters.dateRange?.[1] ? formatDateTime(filters.dateRange[1]) : undefined,
    })
    const blob = new Blob([res.data], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'data_access_logs.csv'
    a.click()
    window.URL.revokeObjectURL(url)
  } catch { ElMessage.error('导出失败') }
}

// --- API integration ---
const { tableData, loading, pagination, fetchData } = useTable<any>(
  (params) => getDataAccessLogs({
    page: params.page,
    pageSize: params.pageSize,
    dataDomain: filters.dataDomain,
    accessType: filters.accessType,
    userId: filters.keyword || undefined,
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
