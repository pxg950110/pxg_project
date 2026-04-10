<template>
  <PageContainer title="评估管理">
    <template #extra>
      <a-button type="primary" @click="evalModal.open()">
        <PlusOutlined /> 新建评估
      </a-button>
    </template>

    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <a-table :columns="columns" :data-source="tableData" :loading="loading" :pagination="pagination" @change="handleTableChange" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <StatusBadge :status="record.status" type="eval" />
        </template>
        <template v-if="column.key === 'created_at'">
          {{ formatDateTime(record.created_at) }}
        </template>
        <template v-if="column.key === 'action'">
          <a @click="router.push(`/model/evaluations/${record.id}`)">查看详情</a>
        </template>
      </template>
    </a-table>

    <!-- New Evaluation Modal -->
    <a-modal v-model:open="evalModal.visible" title="新建评估" @ok="handleCreateEval" :confirm-loading="submitting" width="600px">
      <a-form layout="vertical">
        <a-form-item label="选择模型" required>
          <ModelSelect v-model:value="evalForm.model_id" @change="onModelChange" />
        </a-form-item>
        <a-form-item label="选择版本" required>
          <a-select v-model:value="evalForm.version_id" placeholder="请先选择模型">
            <a-select-option v-for="v in versionOptions" :key="v.id" :value="v.id">{{ v.version_no }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="评估数据集">
          <DatasetSelect v-model:value="evalForm.dataset_id" />
        </a-form-item>
        <a-form-item label="评估指标">
          <a-checkbox-group v-model:value="evalForm.metrics" :options="metricOptions" />
        </a-form-item>
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
import StatusBadge from '@/components/StatusBadge/index.vue'
import ModelSelect from '@/components/ModelSelect/index.vue'
import DatasetSelect from '@/components/DatasetSelect/index.vue'
import { useTable } from '@/hooks/useTable'
import { useModal } from '@/hooks/useModal'
import { createEvaluation } from '@/api/model'
import request from '@/utils/request'
import { formatDateTime } from '@/utils/date'

const router = useRouter()
const evalModal = useModal()
const submitting = ref(false)
const versionOptions = ref<any[]>([])

const searchFields = [
  { name: 'status', label: '状态', type: 'select', options: [
    { label: '待执行', value: 'PENDING' }, { label: '执行中', value: 'RUNNING' },
    { label: '已完成', value: 'COMPLETED' }, { label: '失败', value: 'FAILED' },
  ]},
]

const columns = [
  { title: '评估ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '模型名称', dataIndex: 'model_name', key: 'model_name' },
  { title: '版本', dataIndex: 'version_no', key: 'version_no', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '准确率', dataIndex: 'accuracy', key: 'accuracy', width: 100 },
  { title: 'F1 Score', dataIndex: 'f1_score', key: 'f1_score', width: 100 },
  { title: '创建时间', dataIndex: 'created_at', key: 'created_at', width: 170 },
  { title: '操作', key: 'action', width: 100 },
]

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => request.get('/evaluations', { params: { page: params.page, page_size: params.pageSize } })
)

const evalForm = reactive({
  model_id: undefined as any,
  version_id: undefined as any,
  dataset_id: undefined as any,
  metrics: ['accuracy', 'f1_score', 'precision', 'recall'],
})

const metricOptions = [
  { label: 'Accuracy', value: 'accuracy' },
  { label: 'Precision', value: 'precision' },
  { label: 'Recall', value: 'recall' },
  { label: 'F1 Score', value: 'f1_score' },
  { label: 'AUC', value: 'auc' },
]

async function onModelChange(model: any) {
  if (model?.id) {
    const res = await request.get(`/models/${model.id}/versions`, { params: { page: 1, page_size: 100 } })
    versionOptions.value = res.data.data.items
  }
}

function handleSearch(values: any) { fetchData() }
function handleReset() { fetchData() }

async function handleCreateEval() {
  submitting.value = true
  try {
    await createEvaluation(evalForm)
    message.success('评估任务已创建')
    evalModal.close()
    fetchData()
  } finally { submitting.value = false }
}

onMounted(() => fetchData())
</script>
