<template>
  <PageContainer title="部署管理">
    <template #extra>
      <a-button type="primary" @click="deployModal.open()">
        <PlusOutlined /> 新建部署
      </a-button>
    </template>

    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <a-table :columns="columns" :data-source="tableData" :loading="loading" :pagination="pagination" @change="handleTableChange" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <StatusBadge :status="record.status" type="deploy" />
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="router.push(`/model/deployments/${record.id}`)">详情</a>
            <a v-if="record.status === 'RUNNING'" @click="handleStop(record.id)">停止</a>
            <a v-if="record.status === 'STOPPED'" @click="handleStart(record.id)">启动</a>
            <a-dropdown>
              <a>更多<DownOutlined /></a>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="handleRestart(record.id)">重启</a-menu-item>
                  <a-menu-item @click="openScaleModal(record)">扩缩容</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- New Deploy Modal -->
    <a-modal v-model:open="deployModal.visible" title="新建部署" @ok="handleCreateDeploy" :confirm-loading="submitting" width="700px">
      <a-form layout="vertical">
        <a-form-item label="模型" required>
          <ModelSelect v-model:value="deployForm.model_id" />
        </a-form-item>
        <a-form-item label="版本" required>
          <a-select v-model:value="deployForm.version_id" placeholder="选择版本" />
        </a-form-item>
        <a-form-item label="部署名称" required>
          <a-input v-model:value="deployForm.name" placeholder="请输入部署名称" />
        </a-form-item>
        <ResourceConfigForm v-model="deployForm.resource_config" />
      </a-form>
    </a-modal>

    <!-- Scale Modal -->
    <a-modal v-model:open="scaleModal.visible" title="扩缩容" @ok="handleScale" :confirm-loading="scaling">
      <a-form-item label="副本数">
        <a-input-number v-model:value="scaleForm.replicas" :min="1" :max="10" />
      </a-form-item>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { PlusOutlined, DownOutlined } from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import SearchForm from '@/components/SearchForm/index.vue'
import StatusBadge from '@/components/StatusBadge/index.vue'
import ModelSelect from '@/components/ModelSelect/index.vue'
import ResourceConfigForm from '@/components/ResourceConfigForm/index.vue'
import { useTable } from '@/hooks/useTable'
import { useModal } from '@/hooks/useModal'
import { createDeployment, startDeployment, stopDeployment } from '@/api/model'
import request from '@/utils/request'

const router = useRouter()
const deployModal = useModal()
const scaleModal = useModal<any>()
const submitting = ref(false)
const scaling = ref(false)

const searchFields = [
  { name: 'status', label: '状态', type: 'select', options: [
    { label: '创建中', value: 'CREATING' }, { label: '启动中', value: 'STARTING' },
    { label: '运行中', value: 'RUNNING' }, { label: '已停止', value: 'STOPPED' },
  ]},
]

const columns = [
  { title: '部署名称', dataIndex: 'name', key: 'name' },
  { title: '模型', dataIndex: 'model_name', key: 'model_name' },
  { title: '版本', dataIndex: 'version_no', key: 'version_no', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '副本数', dataIndex: 'replicas', key: 'replicas', width: 80 },
  { title: 'CPU/内存', key: 'resources', width: 120 },
  { title: '创建时间', dataIndex: 'created_at', key: 'created_at', width: 170 },
  { title: '操作', key: 'action', width: 200 },
]

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => request.get('/deployments', { params: { page: params.page, page_size: params.pageSize } })
)

const deployForm = reactive({
  model_id: undefined as any,
  version_id: undefined as any,
  name: '',
  resource_config: { cpu: 2, memory: 4096, gpu: 0, replicas: 1 },
})

function handleSearch() { fetchData() }
function handleReset() { fetchData() }

async function handleCreateDeploy() {
  submitting.value = true
  try {
    await createDeployment(deployForm)
    message.success('部署创建成功')
    deployModal.close()
    fetchData()
  } finally { submitting.value = false }
}

async function handleStart(id: number) {
  await startDeployment(id)
  message.success('启动中...')
  fetchData()
}

async function handleStop(id: number) {
  Modal.confirm({ title: '确认停止部署？', async onOk() { await stopDeployment(id); message.success('停止中...'); fetchData() } })
}

async function handleRestart(id: number) {
  await request.post(`/deployments/${id}/restart`)
  message.success('重启中...')
  fetchData()
}

const scaleForm = reactive({ replicas: 1 })
let scalingId = 0

function openScaleModal(record: any) {
  scalingId = record.id
  scaleForm.replicas = record.replicas || 1
  scaleModal.open(record)
}

async function handleScale() {
  scaling.value = true
  try {
    await request.put(`/deployments/${scalingId}/scale`, scaleForm)
    message.success('扩缩容已提交')
    scaleModal.close()
    fetchData()
  } finally { scaling.value = false }
}

onMounted(() => fetchData())
</script>
