<template>
  <PageContainer title="本地编码映射">
    <!-- Top selectors -->
    <a-card :bordered="false" style="margin-bottom: 16px">
      <a-row :gutter="16" align="middle">
        <a-col :span="6">
          <span style="margin-right: 8px; white-space: nowrap">机构:</span>
          <a-select v-model:value="institutionId" placeholder="选择机构" allow-clear
            style="width: calc(100% - 48px)" @change="handleSelectorChange">
            <a-select-option v-for="inst in institutions" :key="inst.id" :value="inst.id">
              {{ inst.name }}
            </a-select-option>
          </a-select>
        </a-col>
        <a-col :span="6">
          <span style="margin-right: 8px; white-space: nowrap">编码体系:</span>
          <a-select v-model:value="codeSystemId" placeholder="选择编码体系" allow-clear
            style="width: calc(100% - 70px)" @change="handleSelectorChange">
            <a-select-option v-for="cs in codeSystems" :key="cs.id" :value="cs.id">
              {{ cs.name }}
            </a-select-option>
          </a-select>
        </a-col>
        <a-col :span="5">
          <a-select v-model:value="statusFilter" placeholder="映射状态" allow-clear
            style="width: 100%" @change="fetchLocalConcepts">
            <a-select-option value="CONFIRMED">已确认</a-select-option>
            <a-select-option value="AUTO">自动映射</a-select-option>
            <a-select-option value="SUSPECTED">疑似映射</a-select-option>
            <a-select-option value="UNMAPPED">未映射</a-select-option>
          </a-select>
        </a-col>
        <a-col :span="7" style="text-align: right">
          <a-space>
            <a-button @click="handleUpload">
              <template #icon><UploadOutlined /></template>
              批量导入
            </a-button>
          </a-space>
        </a-col>
      </a-row>
    </a-card>

    <!-- Stats row -->
    <a-row :gutter="16" style="margin-bottom: 16px" v-if="institutionId && codeSystemId">
      <a-col :span="6">
        <a-card>
          <a-statistic title="已确认" :value="stats.CONFIRMED || 0" :value-style="{ color: '#52c41a' }" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="自动映射" :value="stats.AUTO || 0" :value-style="{ color: '#1890ff' }" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="疑似映射" :value="stats.SUSPECTED || 0" :value-style="{ color: '#faad14' }" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="未映射" :value="stats.UNMAPPED || 0" :value-style="{ color: '#ff4d4f' }" />
        </a-card>
      </a-col>
    </a-row>

    <!-- Main table -->
    <a-table :columns="columns" :data-source="localConcepts" :loading="loading"
      row-key="id" :pagination="pagination" @change="handleTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'mappingStatus'">
          <a-tag :color="statusColor(record.mappingStatus)">{{ statusLabel(record.mappingStatus) }}</a-tag>
        </template>
        <template v-if="column.key === 'mappingConfidence'">
          <span v-if="record.mappingConfidence != null">{{ (record.mappingConfidence * 100).toFixed(0) }}%</span>
          <span v-else>-</span>
        </template>
        <template v-if="column.key === 'action'">
          <a-button v-if="record.mappingStatus === 'UNMAPPED' || !record.mappingStatus"
            type="link" size="small" @click="handleMap(record)">映射</a-button>
          <a-button v-else type="link" size="small" @click="handleMap(record)">重新映射</a-button>
        </template>
      </template>
    </a-table>

    <!-- Mapping modal -->
    <a-modal v-model:open="mapModalVisible" title="映射到标准概念" :width="500"
      @ok="handleMapSubmit" @cancel="mapModalVisible = false" destroy-on-close>
      <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="本地编码">
          <a-input :value="mappingRecord?.localCode" disabled />
        </a-form-item>
        <a-form-item label="本地名称">
          <a-input :value="mappingRecord?.localName" disabled />
        </a-form-item>
        <a-form-item label="标准概念ID" required>
          <a-input-number v-model:value="mapForm.standardConceptId" placeholder="输入标准概念ID" style="width: 100%" />
        </a-form-item>
        <a-form-item label="映射状态">
          <a-select v-model:value="mapForm.mappingStatus">
            <a-select-option value="CONFIRMED">已确认</a-select-option>
            <a-select-option value="AUTO">自动映射</a-select-option>
            <a-select-option value="SUSPECTED">疑似映射</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- Upload modal -->
    <a-modal v-model:open="uploadModalVisible" title="批量导入本地编码" :width="500"
      @ok="handleUploadSubmit" @cancel="uploadModalVisible = false">
      <a-upload-dragger :before-upload="handleFileSelect" :max-count="1" accept=".csv,.xlsx"
        :file-list="fileList">
        <p class="ant-upload-drag-icon"><UploadOutlined /></p>
        <p class="ant-upload-text">点击或拖拽CSV/XLSX文件至此处</p>
      </a-upload-dragger>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { UploadOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import {
  getCodeSystems, getInstitutions, getLocalConcepts, getLocalConceptStats,
  updateLocalConcept, uploadMasterData,
} from '@/api/masterdata'

defineOptions({ name: 'LocalConceptMapping' })

const institutions = ref<any[]>([])
const codeSystems = ref<any[]>([])
const institutionId = ref<number | undefined>(undefined)
const codeSystemId = ref<number | undefined>(undefined)
const statusFilter = ref<string | undefined>(undefined)

const loading = ref(false)
const localConcepts = ref<any[]>([])
const stats = ref<Record<string, number>>({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true, showTotal: (total: number) => `共 ${total} 条` })

const columns = [
  { title: '本地编码', dataIndex: 'localCode', key: 'localCode', width: 130 },
  { title: '本地名称', dataIndex: 'localName', key: 'localName', width: 180, ellipsis: true },
  { title: '标准概念编码', dataIndex: 'standardConceptCode', key: 'standardConceptCode', width: 140 },
  { title: '标准概念名称', dataIndex: 'standardConceptName', key: 'standardConceptName', width: 180, ellipsis: true },
  { title: '映射状态', key: 'mappingStatus', width: 100 },
  { title: '置信度', key: 'mappingConfidence', width: 80 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' as const },
]

function statusColor(status: string) {
  const colors: Record<string, string> = { CONFIRMED: 'green', AUTO: 'blue', SUSPECTED: 'orange', UNMAPPED: 'red' }
  return colors[status] || 'default'
}

function statusLabel(status: string) {
  const labels: Record<string, string> = { CONFIRMED: '已确认', AUTO: '自动映射', SUSPECTED: '疑似', UNMAPPED: '未映射' }
  return labels[status] || status
}

async function fetchLookups() {
  const [instRes, csRes] = await Promise.allSettled([getInstitutions(), getCodeSystems()])
  institutions.value = instRes.status === 'fulfilled' ? (instRes.value.data.data || []) : []
  codeSystems.value = csRes.status === 'fulfilled' ? (csRes.value.data.data || []) : []
}

async function handleSelectorChange() {
  if (institutionId.value && codeSystemId.value) {
    await Promise.all([fetchStats(), fetchLocalConcepts()])
  } else {
    localConcepts.value = []
    stats.value = {}
  }
}

async function fetchStats() {
  if (!institutionId.value || !codeSystemId.value) return
  try {
    const res = await getLocalConceptStats(institutionId.value, codeSystemId.value)
    stats.value = res.data.data || {}
  } catch { stats.value = {} }
}

async function fetchLocalConcepts(page = 1) {
  if (!institutionId.value || !codeSystemId.value) return
  loading.value = true
  try {
    const params: any = {
      institutionId: institutionId.value,
      codeSystemId: codeSystemId.value,
      page,
      pageSize: pagination.pageSize,
    }
    if (statusFilter.value) params.mappingStatus = statusFilter.value

    const res = await getLocalConcepts(params)
    const data = res.data.data
    localConcepts.value = data?.items || []
    pagination.total = data?.total || 0
    pagination.current = data?.page || page
  } finally {
    loading.value = false
  }
}

function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchLocalConcepts(pag.current)
}

// Mapping modal
const mapModalVisible = ref(false)
const mappingRecord = ref<any>(null)
const mapForm = reactive({ standardConceptId: undefined as number | undefined, mappingStatus: 'CONFIRMED' })

function handleMap(record: any) {
  mappingRecord.value = record
  mapForm.standardConceptId = record.standardConceptId || undefined
  mapForm.mappingStatus = 'CONFIRMED'
  mapModalVisible.value = true
}

async function handleMapSubmit() {
  if (!mapForm.standardConceptId) {
    message.warning('请输入标准概念ID')
    return
  }
  await updateLocalConcept(mappingRecord.value.id, {
    standardConceptId: mapForm.standardConceptId,
    mappingStatus: mapForm.mappingStatus,
  })
  message.success('映射成功')
  mapModalVisible.value = false
  handleSelectorChange()
}

// Upload modal
const uploadModalVisible = ref(false)
const fileList = ref<any[]>([])
const selectedFile = ref<File | null>(null)

function handleUpload() {
  uploadModalVisible.value = true
  fileList.value = []
  selectedFile.value = null
}

function handleFileSelect(file: File) {
  selectedFile.value = file
  fileList.value = [file]
  return false // prevent auto upload
}

async function handleUploadSubmit() {
  if (!selectedFile.value) {
    message.warning('请选择文件')
    return
  }
  if (!codeSystemId.value) {
    message.warning('请先选择编码体系')
    return
  }
  try {
    await uploadMasterData(selectedFile.value, codeSystemId.value)
    message.success('上传成功，正在处理...')
    uploadModalVisible.value = false
    handleSelectorChange()
  } catch { /* error already handled by interceptor */ }
}

onMounted(() => fetchLookups())
</script>
