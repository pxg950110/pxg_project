<template>
  <PageContainer title="患者管理">
    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <a-table :columns="columns" :data-source="tableData" :loading="loading" :pagination="pagination" @change="handleTableChange" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'gender'">
          {{ record.gender === 'M' ? '男' : record.gender === 'F' ? '女' : '未知' }}
        </template>
        <template v-if="column.key === 'action'">
          <a @click="router.push(`/data/cdr/patients/${record.id}`)">详情</a>
        </template>
      </template>
    </a-table>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import PageContainer from '@/components/PageContainer/index.vue'
import SearchForm from '@/components/SearchForm/index.vue'
import { useTable } from '@/hooks/useTable'
import { getPatients } from '@/api/data'

const router = useRouter()

const searchFields = [
  { name: 'keyword', label: '关键词', type: 'input', placeholder: '姓名/身份证/手机号' },
]

const columns = [
  { title: '患者ID', dataIndex: 'id', key: 'id', width: 120 },
  { title: '姓名', dataIndex: 'name', key: 'name', width: 100 },
  { title: '性别', dataIndex: 'gender', key: 'gender', width: 60 },
  { title: '年龄', dataIndex: 'age', key: 'age', width: 60 },
  { title: '身份证', dataIndex: 'id_card', key: 'id_card', width: 180 },
  { title: '联系电话', dataIndex: 'phone', key: 'phone', width: 120 },
  { title: '最近就诊', dataIndex: 'last_visit', key: 'last_visit', width: 170 },
  { title: '操作', key: 'action', width: 80 },
]

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getPatients({ page: params.page, page_size: params.pageSize })
)

function handleSearch() { fetchData() }
function handleReset() { fetchData() }

onMounted(() => fetchData())
</script>
