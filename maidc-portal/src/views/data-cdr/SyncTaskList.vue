<template>
  <PageContainer title="数据同步任务">
    <template #extra>
      <a-badge :dot="hasRunningTasks" :offset="[6, 0]">
        <a-button @click="fetchData()">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-badge>
    </template>

    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <a-alert
      v-if="hasRunningTasks"
      type="info"
      show-icon
      style="margin-bottom: 16px"
      message="存在正在运行的同步任务，数据每30秒自动刷新"
    />

    <a-table
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      @change="handleTableChange"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <StatusBadge :status="record.status" type="sync" />
        </template>
        <template v-if="column.key === 'progress'">
          <a-progress
            :percent="record.progress || 0"
            :status="progressStatusMap[record.status] || 'active'"
            :stroke-color="record.status === 'FAILED' ? '#ff4d4f' : '#1677ff'"
            size="small"
          />
        </template>
        <template v-if="column.key === 'startTime'">
          {{ record.start_time ? formatDateTime(record.start_time) : '-' }}
        </template>
        <template v-if="column.key === 'endTime'">
          {{ record.end_time ? formatDateTime(record.end_time) : '-' }}
        </template>
        <template v-if="column.key === 'recordsProcessed'">
          <span>{{ record.records_processed?.toLocaleString() || 0 }}</span>
          <span v-if="record.records_total" style="color: rgba(0,0,0,0.45)">
            / {{ record.records_total.toLocaleString() }}
          </span>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="handleViewLogs(record)">查看日志</a-button>
            <a-button
              v-if="record.status === 'FAILED'"
              type="link"
              size="small"
              @click="handleRetry(record)"
            >
              重试
            </a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 日志弹窗 -->
    <a-modal
      v-model:open="logModalVisible"
      title="同步日志"
      :footer="null"
      :width="720"
      destroy-on-close
    >
      <div class="log-content">
        <a-spin :spinning="logLoading">
          <pre v-if="logContent" class="log-text">{{ logContent }}</pre>
          <a-empty v-else description="暂无日志" />
        </a-spin>
      </div>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
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
const progressStatusMap: Record<string, string> = {
  RUNNING: 'active',
  COMPLETED: 'success',
  FAILED: 'exception',
  PENDING: 'normal',
  CANCELLED: 'normal',
}

const columns = [
  { title: '任务名称', dataIndex: 'task_name', key: 'task_name', width: 200, ellipsis: true },
  { title: '数据源', dataIndex: 'source_name', key: 'source_name', width: 150 },
  { title: '状态', key: 'status', width: 100 },
  { title: '进度', key: 'progress', width: 200 },
  { title: '开始时间', key: 'startTime', width: 170 },
  { title: '结束时间', key: 'endTime', width: 170 },
  { title: '处理记录数', key: 'recordsProcessed', width: 160 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' as const },
]

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getSyncTasks({
    page: params.page,
    page_size: params.pageSize,
    ...currentSearchParams,
  }),
)

const hasRunningTasks = computed(() =>
  tableData.value.some((item: any) => item.status === 'RUNNING'),
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
    message.success('重试任务已启动')
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
.log-content {
  max-height: 500px;
  overflow: auto;
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
