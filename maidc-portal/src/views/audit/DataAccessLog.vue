<template>
  <PageContainer title="数据访问审计">
    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <a-table :columns="columns" :data-source="tableData" :loading="loading" :pagination="pagination" @change="handleTableChange" row-key="id" size="small">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'created_at'">
          {{ formatDateTime(record.created_at) }}
        </template>
      </template>
    </a-table>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import PageContainer from '@/components/PageContainer/index.vue'
import SearchForm from '@/components/SearchForm/index.vue'
import { useTable } from '@/hooks/useTable'
import { getDataAccessLogs } from '@/api/audit'
import { formatDateTime } from '@/utils/date'

const searchFields = [
  { name: 'data_type', label: '数据类型', type: 'select', options: [
    { label: '患者数据', value: 'PATIENT' }, { label: '影像数据', value: 'IMAGING' },
    { label: '研究数据', value: 'RESEARCH' },
  ]},
]

const columns = [
  { title: '操作人', dataIndex: 'username', key: 'username', width: 100 },
  { title: '数据类型', dataIndex: 'data_type', key: 'data_type', width: 100 },
  { title: '数据ID', dataIndex: 'data_id', key: 'data_id', width: 120 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 80 },
  { title: '患者ID', dataIndex: 'patient_id', key: 'patient_id', width: 120 },
  { title: '访问目的', dataIndex: 'access_purpose', key: 'access_purpose', ellipsis: true },
  { title: 'IP', dataIndex: 'ip', key: 'ip', width: 120 },
  { title: '时间', dataIndex: 'created_at', key: 'created_at', width: 170 },
]

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getDataAccessLogs({ page: params.page, page_size: params.pageSize })
)

function handleSearch() { fetchData() }
function handleReset() { fetchData() }

onMounted(() => fetchData())
</script>
