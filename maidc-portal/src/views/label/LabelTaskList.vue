<template>
  <PageContainer title="标注任务">
    <template #extra>
      <a-button type="primary" @click="taskModal.open()">
        <PlusOutlined /> 新建任务
      </a-button>
    </template>

    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <a-table :columns="columns" :data-source="tableData" :loading="loading" :pagination="pagination" @change="handleTableChange" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="router.push(`/label/workspace/${record.id}`)">工作台</a>
            <a @click="viewStats(record)">统计</a>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="taskModal.visible" title="新建标注任务" @ok="handleCreate" :confirm-loading="submitting" width="600px">
      <a-form layout="vertical">
        <a-form-item label="任务名称" required><a-input v-model:value="taskForm.name" /></a-form-item>
        <a-form-item label="标注类型"><a-select v-model:value="taskForm.task_type">
          <a-select-option value="IMAGE">影像标注</a-select-option>
          <a-select-option value="TEXT">文本标注</a-select-option>
        </a-select></a-form-item>
        <a-form-item label="数据集"><DatasetSelect v-model:value="taskForm.dataset_id" /></a-form-item>
        <a-form-item label="分配给"><UserSelect v-model:value="taskForm.assignee_id" /></a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import SearchForm from '@/components/SearchForm/index.vue'
import DatasetSelect from '@/components/DatasetSelect/index.vue'
import UserSelect from '@/components/UserSelect/index.vue'
import { useTable } from '@/hooks/useTable'
import { useModal } from '@/hooks/useModal'
import { getLabelTasks, createLabelTask } from '@/api/label'

const router = useRouter()
const taskModal = useModal()
const submitting = ref(false)

const searchFields = [
  { name: 'task_type', label: '标注类型', type: 'select', options: [
    { label: '影像标注', value: 'IMAGE' }, { label: '文本标注', value: 'TEXT' },
  ]},
  { name: 'status', label: '状态', type: 'select', options: [
    { label: '待标注', value: 'PENDING' }, { label: '标注中', value: 'IN_PROGRESS' },
    { label: '已完成', value: 'COMPLETED' },
  ]},
]

const columns = [
  { title: '任务名称', dataIndex: 'name', key: 'name' },
  { title: '类型', dataIndex: 'task_type', key: 'task_type', width: 100 },
  { title: '数据集', dataIndex: 'dataset_name', key: 'dataset_name' },
  { title: '标注人', dataIndex: 'assignee_name', key: 'assignee_name', width: 100 },
  { title: '进度', dataIndex: 'progress', key: 'progress', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 130 },
]

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getLabelTasks({ page: params.page, page_size: params.pageSize })
)

const taskForm = reactive({ name: '', task_type: 'IMAGE', dataset_id: undefined as any, assignee_id: undefined as any })

function handleSearch() { fetchData() }
function handleReset() { fetchData() }

async function handleCreate() {
  submitting.value = true
  try {
    await createLabelTask(taskForm)
    message.success('标注任务创建成功')
    taskModal.close()
    fetchData()
  } finally { submitting.value = false }
}

function viewStats(record: any) { message.info('标注统计: ' + record.name) }

onMounted(() => fetchData())
</script>
