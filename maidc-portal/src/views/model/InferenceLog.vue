<template>
  <PageContainer title="推理日志">
    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <a-table :columns="columns" :data-source="tableData" :loading="loading" :pagination="pagination" @change="handleTableChange" row-key="id" size="small">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-badge :status="record.status === 'SUCCESS' ? 'success' : 'error'" :text="record.status" />
        </template>
        <template v-if="column.key === 'latency'">
          {{ record.latency }}ms
        </template>
        <template v-if="column.key === 'created_at'">
          {{ formatDateTime(record.created_at) }}
        </template>
        <template v-if="column.key === 'action'">
          <a @click="viewDetail(record)">详情</a>
        </template>
      </template>
    </a-table>

    <a-drawer v-model:open="detailVisible" title="推理详情" width="600px">
      <template v-if="currentRecord">
        <a-descriptions bordered :column="1" size="small">
          <a-descriptions-item label="请求ID">{{ currentRecord.request_id }}</a-descriptions-item>
          <a-descriptions-item label="部署">{{ currentRecord.deployment_name }}</a-descriptions-item>
          <a-descriptions-item label="状态">{{ currentRecord.status }}</a-descriptions-item>
          <a-descriptions-item label="延迟">{{ currentRecord.latency }}ms</a-descriptions-item>
          <a-descriptions-item label="时间">{{ formatDateTime(currentRecord.created_at) }}</a-descriptions-item>
        </a-descriptions>
        <a-divider />
        <h4>输入</h4>
        <JsonViewer :data="currentRecord.input_data" :collapsed="false" />
        <h4 style="margin-top: 16px">输出</h4>
        <JsonViewer :data="currentRecord.output_data" :collapsed="false" />
      </template>
    </a-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import PageContainer from '@/components/PageContainer/index.vue'
import SearchForm from '@/components/SearchForm/index.vue'
import JsonViewer from '@/components/JsonViewer/index.vue'
import { useTable } from '@/hooks/useTable'
import request from '@/utils/request'
import { formatDateTime } from '@/utils/date'

const searchFields = [
  { name: 'status', label: '状态', type: 'select', options: [
    { label: '成功', value: 'SUCCESS' }, { label: '失败', value: 'FAILED' },
  ]},
  { name: 'date_range', label: '时间范围', type: 'dateRange' },
]

const columns = [
  { title: '请求ID', dataIndex: 'request_id', key: 'request_id', width: 160, ellipsis: true },
  { title: '部署', dataIndex: 'deployment_name', key: 'deployment_name', width: 150 },
  { title: '模型', dataIndex: 'model_name', key: 'model_name', width: 150 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '延迟', dataIndex: 'latency', key: 'latency', width: 80 },
  { title: '时间', dataIndex: 'created_at', key: 'created_at', width: 170 },
  { title: '操作', key: 'action', width: 80 },
]

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => request.get('/monitoring/inference-logs', { params: { page: params.page, page_size: params.pageSize } })
)

const detailVisible = ref(false)
const currentRecord = ref<any>(null)

function handleSearch() { fetchData() }
function handleReset() { fetchData() }

function viewDetail(record: any) {
  currentRecord.value = record
  detailVisible.value = true
}

onMounted(() => fetchData())
</script>
