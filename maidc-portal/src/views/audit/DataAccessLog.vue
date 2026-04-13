<template>
  <PageContainer title="数据访问审计">
    <!-- Filter bar -->
    <div class="filter-bar">
      <div class="filter-controls">
        <a-select v-model:value="filters.dataType" placeholder="数据类型" style="width: 140px" allow-clear>
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="患者数据">患者数据</a-select-option>
          <a-select-option value="影像数据">影像数据</a-select-option>
          <a-select-option value="研究数据">研究数据</a-select-option>
          <a-select-option value="标注数据">标注数据</a-select-option>
          <a-select-option value="模型数据">模型数据</a-select-option>
        </a-select>

        <a-select v-model:value="filters.actionType" placeholder="操作类型" style="width: 130px" allow-clear>
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="查看">查看</a-select-option>
          <a-select-option value="导出">导出</a-select-option>
          <a-select-option value="下载">下载</a-select-option>
          <a-select-option value="修改">修改</a-select-option>
          <a-select-option value="删除">删除</a-select-option>
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
      :scroll="{ x: 1050 }"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'created_at'">
          {{ formatDateTime(record.created_at) }}
        </template>
        <template v-if="column.key === 'data_type'">
          <a-tag :color="record.data_type_color">{{ record.data_type }}</a-tag>
        </template>
        <template v-if="column.key === 'action_type'">
          <span :style="{ color: actionColors[record.action_type] || 'rgba(0,0,0,0.65)', fontWeight: 500 }">{{ record.action_type }}</span>
        </template>
        <template v-if="column.key === 'patient_id'">
          {{ record.patient_id || '-' }}
        </template>
      </template>
    </a-table>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import PageContainer from '@/components/PageContainer/index.vue'
import { useTable } from '@/hooks/useTable'
import { formatDateTime } from '@/utils/date'
import { getDataAccessLogs } from '@/api/audit'

// --- Columns ---
const columns = [
  { title: '时间', dataIndex: 'created_at', key: 'created_at', width: 170 },
  { title: '操作人', dataIndex: 'username', key: 'username', width: 100 },
  { title: '数据类型', dataIndex: 'data_type', key: 'data_type', width: 110 },
  { title: '操作', dataIndex: 'action_type', key: 'action_type', width: 90 },
  { title: '数据ID/名称', dataIndex: 'data_name', key: 'data_name', width: 160 },
  { title: '患者ID', dataIndex: 'patient_id', key: 'patient_id', width: 130 },
  { title: '访问目的', dataIndex: 'purpose', key: 'purpose', width: 160, ellipsis: true },
  { title: 'IP地址', dataIndex: 'ip', key: 'ip', width: 130 },
]

// --- Filters ---
const filters = reactive({
  dataType: undefined as string | undefined,
  actionType: undefined as string | undefined,
  dateRange: null as any,
  keyword: '',
  patientId: undefined as string | undefined,
})

// --- Action type colors ---
const actionColors: Record<string, string> = {
  '查看': 'rgba(0,0,0,0.65)',
  '导出': '#1677ff',
  '下载': '#13c2c2',
  '修改': '#fa8c16',
  '删除': '#ff4d4f',
}

// --- Export ---
function handleExport() {
  // TODO: implement export logic
}

// --- API integration ---
const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getDataAccessLogs({
    page: params.page,
    page_size: params.pageSize,
    data_type: filters.dataType,
    user_id: filters.keyword,
    patient_id: filters.patientId,
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
