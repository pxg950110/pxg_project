<template>
  <PageContainer title="操作审计">
    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <a-table :columns="columns" :data-source="tableData" :loading="loading" :pagination="pagination" @change="handleTableChange" row-key="id" size="small">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-badge :status="record.status === 1 ? 'success' : 'error'" :text="record.status === 1 ? '成功' : '失败'" />
        </template>
        <template v-if="column.key === 'created_at'">
          {{ formatDateTime(record.created_at) }}
        </template>
        <template v-if="column.key === 'action'">
          <a @click="openDetail(record)">详情</a>
        </template>
      </template>
    </a-table>

    <AuditDetailDrawer v-model:visible="detailVisible" :record="currentRecord" />
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import PageContainer from '@/components/PageContainer/index.vue'
import SearchForm from '@/components/SearchForm/index.vue'
import AuditDetailDrawer from '@/components/AuditDetailDrawer/index.vue'
import { useTable } from '@/hooks/useTable'
import { getAuditLogs } from '@/api/audit'
import { formatDateTime } from '@/utils/date'

const searchFields = [
  { name: 'module', label: '模块', type: 'select', options: [
    { label: '模型', value: 'model' }, { label: '认证', value: 'auth' },
    { label: '数据', value: 'data' }, { label: '系统', value: 'system' },
  ]},
  { name: 'status', label: '状态', type: 'select', options: [
    { label: '成功', value: '1' }, { label: '失败', value: '0' },
  ]},
]

const columns = [
  { title: '操作模块', dataIndex: 'module', key: 'module', width: 100 },
  { title: '操作类型', dataIndex: 'operation', key: 'operation', width: 120 },
  { title: '操作人', dataIndex: 'username', key: 'username', width: 100 },
  { title: '请求URL', dataIndex: 'request_url', key: 'request_url', ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '耗时', dataIndex: 'duration', key: 'duration', width: 80 },
  { title: '时间', dataIndex: 'created_at', key: 'created_at', width: 170 },
  { title: '操作', key: 'action', width: 60 },
]

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getAuditLogs({ page: params.page, page_size: params.pageSize })
)

const detailVisible = ref(false)
const currentRecord = ref<any>(null)

function handleSearch() { fetchData() }
function handleReset() { fetchData() }

function openDetail(record: any) {
  currentRecord.value = record
  detailVisible.value = true
}

onMounted(() => fetchData())
</script>
