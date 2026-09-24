<template>
  <PageContainer title="患者管理">
    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <el-table :data="tableData" v-loading="loading" row-key="id" size="default">
      <el-table-column label="患者ID" prop="id" width="80" />
      <el-table-column label="姓名" prop="name" width="100" />
      <el-table-column label="性别" width="60">
        <template #default="{ row }">
          {{ row.gender === 'M' ? '男' : row.gender === 'F' ? '女' : '未知' }}
        </template>
      </el-table-column>
      <el-table-column label="年龄" width="60">
        <template #default="{ row }">
          {{ calcAge(row.birthDate) }}
        </template>
      </el-table-column>
      <el-table-column label="出生日期" prop="birthDate" width="110" />
      <el-table-column label="身份证" width="120">
        <template #default="{ row }">
          {{ row.idCardHash ? '***（已脱敏）' : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="联系电话" width="120">
        <template #default="{ row }">
          {{ row.phoneHash ? '***（已脱敏）' : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="住址" prop="address" show-overflow-tooltip />
      <el-table-column label="操作" width="80">
        <template #default="{ row }">
          <el-link type="primary" :underline="false" @click="router.push(`/data/cdr/patients/${row.id}`)">详情</el-link>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      class="mt-4 justify-end"
      background
      layout="total, sizes, prev, pager, next, jumper"
      :total="pagination.total"
      :current-page="pagination.current"
      :page-size="pagination.pageSize"
      :page-sizes="[10, 20, 50, 100]"
      @current-change="handlePageChange"
      @size-change="handleSizeChange"
    />
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import PageContainer from '@/components/PageContainer/index.vue'
import SearchForm from '@/components/SearchForm/index.vue'
import { useTable } from '@/hooks/useTable'
import { getPatients } from '@/api/data'

const router = useRouter()
const route = useRoute()

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

const { tableData, loading, pagination, fetchData, setSearchParams } = useTable<any>(
  (params) => getPatients(params)
)

function handlePageChange(page: number) {
  pagination.current = page
  fetchData({ page })
}
function handleSizeChange(size: number) {
  pagination.pageSize = size
  pagination.current = 1
  fetchData({ page: 1, pageSize: size })
}

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

onMounted(() => {
  // 工作台快捷检索（FR5）跳转透传：/data/cdr/patients?keyword=xxx
  const kw = typeof route.query.keyword === 'string' ? route.query.keyword.trim() : ''
  if (kw) {
    formState.value = { keyword: kw }
    setSearchParams({ keyword: kw })
  }
  fetchData()
})
</script>
