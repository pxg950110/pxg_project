<template>
  <PageContainer title="数据集管理">
    <template #extra>
      <a-button type="primary" @click="datasetModal.open()">
        <PlusOutlined /> 新建数据集
      </a-button>
    </template>

    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <a-table :columns="columns" :data-source="tableData" :loading="loading" :pagination="pagination" @change="handleTableChange" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'version_count'">
          <a-tag color="blue">{{ record.version_count ?? 0 }}</a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a @click="viewDetail(record)">详情</a>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="datasetModal.visible" title="新建数据集" @ok="handleCreate" :confirm-loading="submitting">
      <a-form layout="vertical">
        <a-form-item label="数据集名称" required><a-input v-model:value="datasetForm.name" /></a-form-item>
        <a-form-item label="所属项目"><a-select v-model:value="datasetForm.project_id" placeholder="选择项目" /></a-form-item>
        <a-form-item label="描述"><a-textarea v-model:value="datasetForm.description" :rows="2" /></a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import SearchForm from '@/components/SearchForm/index.vue'
import { useTable } from '@/hooks/useTable'
import { useModal } from '@/hooks/useModal'
import { getDatasets } from '@/api/data'
import request from '@/utils/request'

const datasetModal = useModal()
const submitting = ref(false)

const searchFields = [
  { name: 'keyword', label: '关键词', type: 'input', placeholder: '数据集名称' },
]

const columns = [
  { title: '数据集名称', dataIndex: 'name', key: 'name' },
  { title: '所属项目', dataIndex: 'project_name', key: 'project_name' },
  { title: '版本数', dataIndex: 'version_count', key: 'version_count', width: 80 },
  { title: '样本数', dataIndex: 'sample_count', key: 'sample_count', width: 80 },
  { title: '大小', dataIndex: 'size', key: 'size', width: 100 },
  { title: '创建人', dataIndex: 'creator_name', key: 'creator_name', width: 100 },
  { title: '更新时间', dataIndex: 'updated_at', key: 'updated_at', width: 170 },
  { title: '操作', key: 'action', width: 80 },
]

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getDatasets({ page: params.page, page_size: params.pageSize })
)

const datasetForm = reactive({ name: '', project_id: undefined as any, description: '' })

function handleSearch() { fetchData() }
function handleReset() { fetchData() }

async function handleCreate() {
  submitting.value = true
  try {
    await request.post('/rdr/datasets', datasetForm)
    message.success('数据集创建成功')
    datasetModal.close()
    fetchData()
  } finally { submitting.value = false }
}

function viewDetail(record: any) { message.info('查看数据集: ' + record.name) }

onMounted(() => fetchData())
</script>
