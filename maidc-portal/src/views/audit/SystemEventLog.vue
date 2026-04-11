<template>
  <PageContainer title="系统事件">
    <!-- Filter bar -->
    <div class="filter-bar">
      <div class="filter-controls">
        <a-select v-model:value="filters.eventType" placeholder="事件类型" style="width: 150px" allow-clear>
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="系统启动">系统启动</a-select-option>
          <a-select-option value="系统停止">系统停止</a-select-option>
          <a-select-option value="配置变更">配置变更</a-select-option>
          <a-select-option value="服务状态">服务状态</a-select-option>
          <a-select-option value="安全事件">安全事件</a-select-option>
        </a-select>

        <a-select v-model:value="filters.level" placeholder="级别" style="width: 120px" allow-clear>
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="INFO">INFO</a-select-option>
          <a-select-option value="WARN">WARN</a-select-option>
          <a-select-option value="ERROR">ERROR</a-select-option>
        </a-select>

        <a-range-picker v-model:value="filters.dateRange" style="width: 260px" />

        <a-input-search
          v-model:value="filters.keyword"
          placeholder="搜索..."
          style="width: 200px"
          allow-clear
        />
      </div>
      <a-button type="primary" @click="handleExport">
        <template #icon><DownloadOutlined /></template>
        导出
      </a-button>
    </div>

    <!-- Table -->
    <a-table
      :columns="columns"
      :data-source="filteredData"
      :pagination="pagination"
      row-key="id"
      size="small"
      :scroll="{ x: 800 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'created_at'">
          {{ formatDateTime(record.created_at) }}
        </template>
        <template v-if="column.key === 'level'">
          <a-tag :color="record.level_color">{{ record.level }}</a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a @click="handleDetail(record)">详情</a>
        </template>
      </template>
    </a-table>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { message } from 'ant-design-vue'
import { DownloadOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import { useTable } from '@/hooks/useTable'
import { formatDateTime } from '@/utils/date'

// --- Mock data ---
interface SystemEvent {
  id: number
  event_type: string
  level: string
  level_color: string
  service: string
  description: string
  operator: string
  created_at: string
}

const mockData = ref<SystemEvent[]>([
  { id: 1, event_type: '系统启动', level: 'INFO', level_color: 'blue', service: 'maidc-gateway', description: '网关服务启动完成，监听端口 8080', operator: '系统', created_at: '2026-04-12 08:00:00' },
  { id: 2, event_type: '配置变更', level: 'WARN', level_color: 'orange', service: 'maidc-data', description: '数据库连接池参数调整：max_connections 200→300, timeout 30s→60s', operator: 'admin', created_at: '2026-04-12 09:15:30' },
  { id: 3, event_type: '服务状态', level: 'ERROR', level_color: 'red', service: 'maidc-model', description: '模型推理服务心跳超时(30s)，自动重启中', operator: '系统', created_at: '2026-04-12 09:45:00' },
  { id: 4, event_type: '安全事件', level: 'WARN', level_color: 'orange', service: 'maidc-auth', description: '连续登录失败5次，IP: 203.0.113.45，已自动锁定30分钟', operator: '系统', created_at: '2026-04-12 10:00:15' },
  { id: 5, event_type: '系统停止', level: 'INFO', level_color: 'blue', service: 'maidc-task', description: '定时任务服务优雅停机，已完成 3 个正在执行的任务', operator: 'admin', created_at: '2026-04-12 10:30:00' },
])

// --- Filters ---
const filters = reactive({
  eventType: undefined as string | undefined,
  level: undefined as string | undefined,
  dateRange: undefined as any,
  keyword: undefined as string | undefined,
})

// --- Computed filtered data ---
const filteredData = computed(() => {
  return mockData.value.filter((item) => {
    if (filters.eventType && item.event_type !== filters.eventType) return false
    if (filters.level && item.level !== filters.level) return false
    if (filters.keyword) {
      const kw = filters.keyword.toLowerCase()
      if (
        !item.description.toLowerCase().includes(kw) &&
        !item.service.toLowerCase().includes(kw) &&
        !item.operator.toLowerCase().includes(kw)
      ) return false
    }
    return true
  })
})

// --- Columns ---
const columns = [
  { title: '时间', dataIndex: 'created_at', key: 'created_at', width: 170 },
  { title: '事件类型', dataIndex: 'event_type', key: 'event_type', width: 120 },
  { title: '级别', dataIndex: 'level', key: 'level', width: 90 },
  { title: '服务', dataIndex: 'service', key: 'service', width: 140 },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '操作人', dataIndex: 'operator', key: 'operator', width: 100 },
  { title: '操作', key: 'action', width: 60, fixed: 'right' as const },
]

// --- Pagination ---
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 1245,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total.toLocaleString()} 条记录`,
})

// --- Detail ---
function handleDetail(record: SystemEvent) {
  message.info('查看事件详情 #' + record.id)
}

// --- Export ---
function handleExport() {
  // TODO: implement export logic
}

// Keep useTable for future API integration
const { tableData, loading, fetchData, handleTableChange } = useTable<any>(
  () => Promise.resolve({ data: { code: 0, data: { items: [], total: 0, page: 1 } } })
)
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

:deep(.ant-table) {
  font-size: 13px;
}

:deep(.ant-table-cell) {
  white-space: nowrap;
}
</style>
