<template>
  <PageContainer title="数据同步任务">
    <template #extra>
      <span class="relative inline-flex">
        <span
          v-if="hasRunningTasks"
          class="refresh-dot"
        />
        <el-button @click="fetchData()">
          <el-icon class="mr-1"><Refresh /></el-icon>
          刷新
        </el-button>
      </span>
    </template>

    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <el-alert
      v-if="hasRunningTasks"
      type="info"
      show-icon
      :closable="false"
      class="mb-4"
      title="存在正在运行的同步任务，数据每30秒自动刷新"
    />

    <el-table :data="tableData" v-loading="loading" row-key="id" size="default">
      <el-table-column label="任务名称" prop="task_name" width="200" show-overflow-tooltip />
      <el-table-column label="数据源" prop="source_name" width="150" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <StatusBadge :status="row.status" type="sync" />
        </template>
      </el-table-column>
      <el-table-column label="进度" width="200">
        <template #default="{ row }">
          <el-progress
            :percentage="row.progress || 0"
            :stroke-width="6"
            :status="progressStatusMap[row.status]"
            :color="row.status === 'FAILED' ? '#ef4444' : '#0ea5e9'"
          />
        </template>
      </el-table-column>
      <el-table-column label="开始时间" width="170">
        <template #default="{ row }">
          {{ row.start_time ? formatDateTime(row.start_time) : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="结束时间" width="170">
        <template #default="{ row }">
          {{ row.end_time ? formatDateTime(row.end_time) : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="处理记录数" width="160">
        <template #default="{ row }">
          <span>{{ row.records_processed?.toLocaleString() || 0 }}</span>
          <span v-if="row.records_total" class="records-total">
            / {{ row.records_total.toLocaleString() }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleViewLogs(row)">查看日志</el-button>
          <el-button
            v-if="row.status === 'FAILED'"
            link
            type="primary"
            size="small"
            @click="handleRetry(row)"
          >
            重试
          </el-button>
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

    <!-- 日志弹窗 -->
    <el-dialog
      v-model="logModalVisible"
      title="同步日志"
      width="720px"
      :destroy-on-close="true"
    >
      <div class="log-content" v-loading="logLoading">
        <pre v-if="logContent" class="log-text">{{ logContent }}</pre>
        <el-empty v-else description="暂无日志" :image-size="60" />
      </div>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import SearchForm from '@/components/SearchForm/index.vue'
import StatusBadge from '@/components/StatusBadge/index.vue'
import { useTable } from '@/hooks/useTable'
import { getSyncTasks, getSyncTaskLogs, retrySyncTask } from '@/api/data'
import { formatDateTime } from '@/utils/date'

defineOptions({ name: 'SyncTaskList' })

// ===== 搜索 =====
const searchFields = [
  { name: 'status', label: '任务状态', type: 'select' as const, options: [
    { label: '运行中', value: 'RUNNING' },
    { label: '已完成', value: 'COMPLETED' },
    { label: '失败', value: 'FAILED' },
    { label: '等待中', value: 'PENDING' },
  ] },
  { name: 'timeRange', label: '时间范围', type: 'dateRange' as const },
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
const progressStatusMap: Record<string, 'success' | 'exception' | undefined> = {
  RUNNING: undefined,
  COMPLETED: 'success',
  FAILED: 'exception',
  PENDING: undefined,
  CANCELLED: undefined,
}

const { tableData, loading, pagination, fetchData } = useTable<any>(
  (params) => getSyncTasks({
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

const hasRunningTasks = computed(() =>
  (tableData.value || []).some((item: any) => item.status === 'RUNNING'),
)

// ===== 自动刷新 =====
let timer: ReturnType<typeof setInterval> | null = null

function startAutoRefresh() {
  timer = setInterval(() => {
    if (hasRunningTasks.value) {
      fetchData()
    } else {
      stopAutoRefresh()
    }
  }, 30000)
}

function stopAutoRefresh() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

// ===== 日志弹窗 =====
const logModalVisible = ref(false)
const logLoading = ref(false)
const logContent = ref('')

async function handleViewLogs(record: any) {
  logModalVisible.value = true
  logLoading.value = true
  logContent.value = ''
  try {
    const res = await getSyncTaskLogs(record.id)
    logContent.value = res.data.data?.content || res.data.data?.logs || '暂无日志内容'
  } catch {
    logContent.value = '加载日志失败'
  } finally {
    logLoading.value = false
  }
}

// ===== 重试 =====
async function handleRetry(record: any) {
  try {
    await retrySyncTask(record.id)
    ElMessage.success('重试任务已启动')
    fetchData()
  } catch {
    // error handled by request interceptor
  }
}

onMounted(() => {
  fetchData()
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.refresh-dot {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #10b981;
  z-index: 1;
}
.records-total {
  color: #94a3b8;
}
.log-content {
  max-height: 500px;
  overflow: auto;
  min-height: 120px;
}
.log-text {
  margin: 0;
  padding: 12px;
  background: #1e1e1e;
  color: #d4d4d4;
  border-radius: 6px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
