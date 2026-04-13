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
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      size="small"
      :scroll="{ x: 800 }"
      @change="handleTableChange"
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
import { ref, reactive, onMounted, watch } from 'vue'
import { message } from 'ant-design-vue'
import { DownloadOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import { useTable } from '@/hooks/useTable'
import { formatDateTime } from '@/utils/date'
import { getSystemEvents } from '@/api/audit'

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

// --- Filters ---
const filters = reactive({
  eventType: undefined as string | undefined,
  level: undefined as string | undefined,
  dateRange: null as any,
  keyword: '',
})

// --- Detail ---
function handleDetail(record: any) {
  message.info('查看事件详情 #' + record.id)
}

// --- Export ---
function handleExport() {
  // TODO: implement export logic
}

// --- API integration ---
const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getSystemEvents({
    page: params.page,
    page_size: params.pageSize,
    event_type: filters.eventType,
    severity: filters.level,
    start_time: filters.dateRange?.[0] ? formatDateTime(filters.dateRange[0]) : undefined,
    end_time: filters.dateRange?.[1] ? formatDateTime(filters.dateRange[1]) : undefined
  })
)

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

:deep(.ant-table) {
  font-size: 13px;
}

:deep(.ant-table-cell) {
  white-space: nowrap;
}
</style>
