<template>
  <div>
    <div style="margin-bottom: 16px; display: flex; justify-content: space-between">
      <a-space>
        <a-button @click="loadVersions">刷新</a-button>
        <a-button type="primary" @click="compareVisible = true">版本对比</a-button>
      </a-space>
      <a-button type="primary" @click="uploadModal.open()">
        <UploadOutlined /> 上传新版本
      </a-button>
    </div>

    <a-table :columns="columns" :data-source="versions" :loading="loading" row-key="id" size="small">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <StatusBadge :status="record.status" type="version" />
        </template>
        <template v-if="column.key === 'created_at'">
          {{ formatDateTime(record.created_at) }}
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="viewDetail(record)">查看</a>
            <a @click="startCompare(record)">对比</a>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- Upload Modal -->
    <a-modal v-model:open="uploadModal.visible" title="上传新版本" @ok="handleUpload" :confirm-loading="uploading" width="600px">
      <a-form layout="vertical">
        <a-form-item label="版本号" required>
          <a-input v-model:value="uploadForm.version_no" placeholder="例如: v1.0.0" />
        </a-form-item>
        <a-form-item label="模型文件" required>
          <FileUploader accept=".pt,.onnx,.pb,.pkl,.zip" :max-size="2048" bucket="maidc-models" @success="onFileUploaded" />
        </a-form-item>
        <a-form-item label="变更说明">
          <a-textarea v-model:value="uploadForm.changelog" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- Compare Modal -->
    <a-modal v-model:open="compareVisible" title="版本对比" :footer="null" width="800px">
      <a-row :gutter="16" style="margin-bottom: 16px">
        <a-col :span="12">
          <a-select v-model:value="compareV1" placeholder="选择版本1" style="width: 100%">
            <a-select-option v-for="v in versions" :key="v.id" :value="v.id">{{ v.version_no }}</a-select-option>
          </a-select>
        </a-col>
        <a-col :span="12">
          <a-select v-model:value="compareV2" placeholder="选择版本2" style="width: 100%">
            <a-select-option v-for="v in versions" :key="v.id" :value="v.id">{{ v.version_no }}</a-select-option>
          </a-select>
        </a-col>
      </a-row>
      <a-button type="primary" @click="doCompare" :loading="comparing" :disabled="!compareV1 || !compareV2">开始对比</a-button>
      <div v-if="compareResult" style="margin-top: 16px">
        <a-descriptions bordered size="small" :column="1">
          <a-descriptions-item v-for="(val, key) in compareResult" :key="key" :label="key">{{ val }}</a-descriptions-item>
        </a-descriptions>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { UploadOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import StatusBadge from '@/components/StatusBadge/index.vue'
import FileUploader from '@/components/FileUploader/index.vue'
import { useModal } from '@/hooks/useModal'
import { getVersions, createVersion, compareVersions } from '@/api/model'
import { formatDateTime } from '@/utils/date'

const props = defineProps<{ modelId: number }>()
const uploadModal = useModal()
const versions = ref<any[]>([])
const loading = ref(false)
const uploading = ref(false)
const uploadedFile = ref<any>(null)

const uploadForm = reactive({ version_no: '', changelog: '' })

const columns = [
  { title: '版本号', dataIndex: 'version_no', key: 'version_no' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '文件大小', dataIndex: 'file_size', key: 'file_size' },
  { title: '变更说明', dataIndex: 'changelog', key: 'changelog', ellipsis: true },
  { title: '创建时间', dataIndex: 'created_at', key: 'created_at' },
  { title: '操作', key: 'action', width: 120 },
]

async function loadVersions() {
  loading.value = true
  try {
    const res = await getVersions(props.modelId, { page: 1, page_size: 100 })
    versions.value = res.data.data.items
  } finally { loading.value = false }
}

function onFileUploaded(fileInfo: any) {
  uploadedFile.value = fileInfo
}

async function handleUpload() {
  if (!uploadForm.version_no) { message.warning('请输入版本号'); return }
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('version_no', uploadForm.version_no)
    formData.append('changelog', uploadForm.changelog)
    if (uploadedFile.value) formData.append('file', uploadedFile.value)
    await createVersion(props.modelId, formData)
    message.success('版本上传成功')
    uploadModal.close()
    loadVersions()
  } finally { uploading.value = false }
}

const compareVisible = ref(false)
const compareV1 = ref<number>()
const compareV2 = ref<number>()
const compareResult = ref<any>(null)
const comparing = ref(false)

function startCompare(record: any) {
  compareV2.value = record.id
  compareVisible.value = true
}

async function doCompare() {
  comparing.value = true
  try {
    const res = await compareVersions(props.modelId, compareV1.value!, compareV2.value!)
    compareResult.value = res.data.data
  } finally { comparing.value = false }
}

function viewDetail(record: any) {
  message.info('查看版本详情: ' + record.version_no)
}

onMounted(loadVersions)
</script>
