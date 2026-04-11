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
      :data-source="filteredData"
      :pagination="pagination"
      row-key="id"
      size="small"
      :scroll="{ x: 1050 }"
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
import { ref, reactive, computed } from 'vue'
import PageContainer from '@/components/PageContainer/index.vue'
import { useTable } from '@/hooks/useTable'
import { formatDateTime } from '@/utils/date'

// --- Mock data ---
interface DataAccessRecord {
  id: number
  username: string
  data_type: string
  data_type_color: string
  action_type: string
  data_name: string
  patient_id: string
  purpose: string
  ip: string
  created_at: string
}

const mockData = ref<DataAccessRecord[]>([
  { id: 1, username: '李医生', data_type: '患者数据', data_type_color: 'blue', action_type: '查看', data_name: 'PAT-2026-00123', patient_id: 'PAT-2026-00123', purpose: '日常诊疗查阅', ip: '192.168.1.105', created_at: '2026-04-12 10:30:15' },
  { id: 2, username: '王技师', data_type: '影像数据', data_type_color: 'purple', action_type: '导出', data_name: 'IMG-2026-04567', patient_id: '-', purpose: '科研项目数据导出', ip: '192.168.1.108', created_at: '2026-04-12 10:25:30' },
  { id: 3, username: '张主任', data_type: '研究数据', data_type_color: 'green', action_type: '下载', data_name: 'DS-2026-LUNG-01', patient_id: '-', purpose: '离线分析下载', ip: '10.0.0.5', created_at: '2026-04-12 10:20:00' },
  { id: 4, username: '赵实习生', data_type: '标注数据', data_type_color: 'orange', action_type: '修改', data_name: 'TASK-2026-0089', patient_id: '-', purpose: '标注工作修改', ip: '192.168.1.112', created_at: '2026-04-12 10:15:18' },
  { id: 5, username: 'admin', data_type: '模型数据', data_type_color: 'cyan', action_type: '删除', data_name: 'MODEL-V2.3.1', patient_id: '-', purpose: '过期版本清理', ip: '10.0.0.1', created_at: '2026-04-12 10:10:45' },
  { id: 6, username: '李医生', data_type: '患者数据', data_type_color: 'blue', action_type: '查看', data_name: 'PAT-2026-00456', patient_id: 'PAT-2026-00456', purpose: '日常诊疗查阅', ip: '192.168.1.105', created_at: '2026-04-12 10:05:20' },
])

// --- Filters ---
const filters = reactive({
  dataType: undefined as string | undefined,
  actionType: undefined as string | undefined,
  dateRange: undefined as any,
  keyword: undefined as string | undefined,
})

// --- Computed filtered data ---
const filteredData = computed(() => {
  return mockData.value.filter((item) => {
    if (filters.dataType && item.data_type !== filters.dataType) return false
    if (filters.actionType && item.action_type !== filters.actionType) return false
    if (filters.keyword) {
      const kw = filters.keyword.toLowerCase()
      if (!item.username.toLowerCase().includes(kw)) return false
    }
    return true
  })
})

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

// --- Pagination ---
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 8567,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total.toLocaleString()} 条记录`,
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
