<template>
  <PageContainer title="推理日志">
    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <el-table :data="tableData" v-loading="loading" row-key="id" size="small">
      <el-table-column label="请求ID" prop="request_id" width="160" show-overflow-tooltip />
      <el-table-column label="部署" prop="deployment_name" width="150" />
      <el-table-column label="模型" prop="model_name" width="150" />
      <el-table-column label="状态" prop="status" width="80">
        <template #default="{ row }">
          <span class="inline-flex items-center gap-1.5">
            <span class="inline-block h-2 w-2 rounded-full" :style="{ background: row.status === 'SUCCESS' ? '#10b981' : '#ef4444' }" />
            {{ row.status }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="延迟" prop="latency" width="80">
        <template #default="{ row }">{{ row.latency }}ms</template>
      </el-table-column>
      <el-table-column label="时间" prop="created_at" width="170">
        <template #default="{ row }">{{ formatDateTime(row.created_at) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="80">
        <template #default="{ row }">
          <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
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

    <el-drawer v-model="detailVisible" title="推理详情" size="600px" :destroy-on-close="true">
      <template v-if="currentRecord">
        <el-descriptions border :column="1" size="small">
          <el-descriptions-item label="请求ID">{{ currentRecord.request_id }}</el-descriptions-item>
          <el-descriptions-item label="部署">{{ currentRecord.deployment_name }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ currentRecord.status }}</el-descriptions-item>
          <el-descriptions-item label="延迟">{{ currentRecord.latency }}ms</el-descriptions-item>
          <el-descriptions-item label="时间">{{ formatDateTime(currentRecord.created_at) }}</el-descriptions-item>
        </el-descriptions>
        <el-divider />
        <h4>输入</h4>
        <JsonViewer :data="currentRecord.input_data" :collapsed="false" />
        <h4 style="margin-top: 16px">输出</h4>
        <JsonViewer :data="currentRecord.output_data" :collapsed="false" />
      </template>
    </el-drawer>
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

const { tableData, loading, pagination, fetchData } = useTable<any>(
  (params) => request.get('/monitoring/inference-logs', { params: { page: params.page, page_size: params.pageSize } })
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
