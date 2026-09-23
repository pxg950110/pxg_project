<template>
  <PageContainer title="数据集管理">
    <template #extra>
      <el-button type="primary" @click="datasetModal.open()">
        <el-icon class="mr-1"><Plus /></el-icon> 新建数据集
      </el-button>
    </template>

    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <el-table :data="tableData" v-loading="loading" row-key="id" size="default">
      <el-table-column label="数据集名称" prop="name" />
      <el-table-column label="所属项目" prop="project_name" />
      <el-table-column label="版本数" width="80">
        <template #default="{ row }">
          <el-tag type="primary">{{ row.version_count ?? 0 }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="样本数" prop="sample_count" width="80" />
      <el-table-column label="大小" prop="size" width="100" />
      <el-table-column label="创建人" prop="creator_name" width="100" />
      <el-table-column label="更新时间" prop="updated_at" width="170" />
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

    <el-dialog v-model="datasetModal.visible" title="新建数据集" width="520px">
      <el-form label-position="top">
        <el-form-item label="数据集名称" required><el-input v-model="datasetForm.name" /></el-form-item>
        <el-form-item label="所属项目"><el-select v-model="datasetForm.project_id" placeholder="选择项目" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="datasetForm.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="datasetModal.close()">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer/index.vue'
import SearchForm from '@/components/SearchForm/index.vue'
import { useTable } from '@/hooks/useTable'
import { useModal } from '@/hooks/useModal'
import { getDatasets } from '@/api/data'
import request from '@/utils/request'

defineOptions({ name: 'DatasetList' })

const datasetModal = useModal()
const submitting = ref(false)

const searchFields = [
  { name: 'keyword', label: '关键词', type: 'input', placeholder: '数据集名称' },
]

const { tableData, loading, pagination, fetchData } = useTable<any>(
  (params) => getDatasets({ page: params.page, page_size: params.pageSize })
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

const datasetForm = reactive({ name: '', project_id: undefined as any, description: '' })

function handleSearch() { fetchData() }
function handleReset() { fetchData() }

async function handleCreate() {
  submitting.value = true
  try {
    await request.post('/rdr/datasets', datasetForm)
    ElMessage.success('数据集创建成功')
    datasetModal.close()
    fetchData()
  } finally { submitting.value = false }
}

function viewDetail(record: any) { ElMessage.info('查看数据集: ' + record.name) }

onMounted(() => fetchData())
</script>
