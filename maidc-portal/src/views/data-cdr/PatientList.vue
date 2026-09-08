<template>
  <PageContainer title="患者管理">
    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <a-table :columns="columns" :data-source="tableData" :loading="loading" :pagination="pagination" @change="handleTableChange" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'gender'">
          {{ record.gender === 'M' ? '男' : record.gender === 'F' ? '女' : '未知' }}
        </template>
        <template v-if="column.key === 'age'">
          {{ calcAge(record.birthDate) }}
        </template>
        <template v-if="column.key === 'idCardHash'">
          {{ record.idCardHash ? '***（已脱敏）' : '-' }}
        </template>
        <template v-if="column.key === 'phoneHash'">
          {{ record.phoneHash ? '***（已脱敏）' : '-' }}
        </template>
        <template v-if="column.key === 'action'">
          <a @click="router.push(`/data/cdr/patients/${record.id}`)">详情</a>
        </template>
      </template>
    </a-table>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import PageContainer from '@/components/PageContainer/index.vue'
import SearchForm from '@/components/SearchForm/index.vue'
import { useTable } from '@/hooks/useTable'
import { getPatients } from '@/api/data'

const router = useRouter()

const searchFields = [
  { name: 'keyword', label: '关键词', type: 'input', placeholder: '姓名搜索' },
]

const formState = ref<Record<string, any>>({})

function calcAge(birthDate: string | null) {
  if (!birthDate) return '-'
  const birth = new Date(birthDate)
  const today = new Date()
  let age = today.getFullYear() - birth.getFullYear()
  if (today.getMonth() < birth.getMonth() || (today.getMonth() === birth.getMonth() && today.getDate() < birth.getDate())) age--
  return age
}

const columns = [
  { title: '患者ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '姓名', dataIndex: 'name', key: 'name', width: 100 },
  { title: '性别', dataIndex: 'gender', key: 'gender', width: 60 },
  { title: '年龄', key: 'age', width: 60 },
  { title: '出生日期', dataIndex: 'birthDate', key: 'birthDate', width: 110 },
  { title: '身份证', key: 'idCardHash', width: 120 },
  { title: '联系电话', key: 'phoneHash', width: 120 },
  { title: '住址', dataIndex: 'address', key: 'address', ellipsis: true },
  { title: '操作', key: 'action', width: 80 },
]

const { tableData, loading, pagination, fetchData, handleTableChange, setSearchParams } = useTable<any>(
  (params) => getPatients(params)
)

function handleSearch(values: Record<string, any>) {
  formState.value = { ...values }
  setSearchParams(values)
  fetchData()
}
function handleReset() {
  formState.value = {}
  setSearchParams({})
  fetchData()
}

onMounted(() => fetchData())
</script>
