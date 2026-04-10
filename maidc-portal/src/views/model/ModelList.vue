<template>
  <PageContainer title="模型管理">
    <template #extra>
      <a-button type="primary" @click="registerModal.open()">
        <PlusOutlined /> 注册模型
      </a-button>
    </template>

    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <a-table
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      @change="handleTableChange"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <StatusBadge :status="record.status" type="model" />
        </template>
        <template v-if="column.key === 'model_type'">
          <a-tag>{{ record.model_type }}</a-tag>
        </template>
        <template v-if="column.key === 'framework'">
          <a-tag color="blue">{{ record.framework }}</a-tag>
        </template>
        <template v-if="column.key === 'created_at'">
          {{ formatDateTime(record.created_at) }}
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="router.push(`/model/${record.id}`)">详情</a>
            <a @click="editModal.open(record)">编辑</a>
            <a-popconfirm title="确定删除此模型？" @confirm="handleDelete(record.id)">
              <a class="danger-link">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- Register Modal -->
    <a-modal
      v-model:open="registerModal.visible"
      title="注册模型"
      @ok="handleRegister"
      :confirm-loading="submitting"
      width="640px"
    >
      <a-form :model="registerForm" :rules="registerRules" ref="registerFormRef" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="模型名称" name="model_name">
              <a-input v-model:value="registerForm.model_name" placeholder="请输入模型名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="模型编码" name="model_code">
              <a-input v-model:value="registerForm.model_code" placeholder="自动生成或手动输入" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="模型类型" name="model_type">
              <a-select v-model:value="registerForm.model_type" placeholder="请选择">
                <a-select-option value="IMAGE_CLASSIFICATION">图像分类</a-select-option>
                <a-select-option value="OBJECT_DETECTION">目标检测</a-select-option>
                <a-select-option value="SEGMENTATION">分割</a-select-option>
                <a-select-option value="NLP">自然语言处理</a-select-option>
                <a-select-option value="SERIES_PREDICTION">时序预测</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="框架" name="framework">
              <a-select v-model:value="registerForm.framework" placeholder="请选择">
                <a-select-option value="PYTORCH">PyTorch</a-select-option>
                <a-select-option value="TENSORFLOW">TensorFlow</a-select-option>
                <a-select-option value="ONNX">ONNX</a-select-option>
                <a-select-option value="SKLEARN">Scikit-learn</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="registerForm.description" :rows="3" placeholder="请输入模型描述" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- Edit Modal -->
    <a-modal
      v-model:open="editModal.visible"
      title="编辑模型"
      @ok="handleEdit"
      :confirm-loading="submitting"
      width="640px"
    >
      <a-form :model="editForm" layout="vertical">
        <a-form-item label="模型名称" name="model_name">
          <a-input v-model:value="editForm.model_name" />
        </a-form-item>
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="editForm.description" :rows="3" />
        </a-form-item>
        <a-form-item label="标签">
          <a-select v-model:value="editForm.tags" mode="tags" placeholder="输入标签后回车" />
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
import { useTable } from '@/hooks/useTable'
import { useModal } from '@/hooks/useModal'
import { getModels, createModel, updateModel, deleteModel } from '@/api/model'
import { formatDateTime } from '@/utils/date'

const router = useRouter()
const registerModal = useModal()
const editModal = useModal<any>()
const submitting = ref(false)

const searchFields = [
  { name: 'keyword', label: '关键词', type: 'input', placeholder: '模型名称/编码' },
  { name: 'model_type', label: '模型类型', type: 'select', options: [
    { label: '图像分类', value: 'IMAGE_CLASSIFICATION' },
    { label: '目标检测', value: 'OBJECT_DETECTION' },
    { label: '分割', value: 'SEGMENTATION' },
    { label: 'NLP', value: 'NLP' },
  ]},
  { name: 'status', label: '状态', type: 'select', options: [
    { label: '草稿', value: 'DRAFT' },
    { label: '训练中', value: 'TRAINING' },
    { label: '评估中', value: 'EVALUATING' },
    { label: '已上线', value: 'ACTIVE' },
  ]},
]

const columns = [
  { title: '模型编码', dataIndex: 'model_code', key: 'model_code', width: 140 },
  { title: '模型名称', dataIndex: 'model_name', key: 'model_name', width: 180 },
  { title: '类型', dataIndex: 'model_type', key: 'model_type', width: 120 },
  { title: '框架', dataIndex: 'framework', key: 'framework', width: 100 },
  { title: '最新版本', dataIndex: 'latest_version', key: 'latest_version', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '负责人', dataIndex: 'owner_name', key: 'owner_name', width: 100 },
  { title: '更新时间', dataIndex: 'updated_at', key: 'created_at', width: 170 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' },
]

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getModels({ page: params.page, page_size: params.pageSize })
)

const searchParams = ref<Record<string, any>>({})

function handleSearch(values: Record<string, any>) {
  searchParams.value = values
  fetchData()
}

function handleReset() {
  searchParams.value = {}
  fetchData()
}

const registerForm = reactive({
  model_name: '',
  model_code: '',
  model_type: undefined as string | undefined,
  framework: undefined as string | undefined,
  description: '',
})
const registerRules = {
  model_name: [{ required: true, message: '请输入模型名称' }],
  model_type: [{ required: true, message: '请选择模型类型' }],
  framework: [{ required: true, message: '请选择框架' }],
}
const registerFormRef = ref()

async function handleRegister() {
  submitting.value = true
  try {
    await createModel(registerForm)
    message.success('模型注册成功')
    registerModal.close()
    fetchData()
  } finally {
    submitting.value = false
  }
}

const editForm = reactive({
  model_name: '',
  description: '',
  tags: [] as string[],
})
let editingId = 0

function onEditOpen() {
  if (editModal.currentRecord.value) {
    const r = editModal.currentRecord.value
    editingId = r.id
    editForm.model_name = r.model_name
    editForm.description = r.description
    editForm.tags = r.tags || []
  }
}

watch(() => editModal.visible, (v) => { if (v) onEditOpen() })

async function handleEdit() {
  submitting.value = true
  try {
    await updateModel(editingId, editForm)
    message.success('模型更新成功')
    editModal.close()
    fetchData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id: number) {
  await deleteModel(id)
  message.success('模型已删除')
  fetchData()
}

onMounted(() => fetchData())
</script>

<style scoped>
.danger-link {
  color: #ff4d4f;
}
</style>
