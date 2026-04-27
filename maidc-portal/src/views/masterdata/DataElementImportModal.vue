<template>
  <a-modal
    v-model:open="visible"
    title="导入数据元"
    :footer="null"
    width="600"
    destroy-on-close
    @cancel="handleClose"
  >
    <!-- Step 1: Upload -->
    <div v-if="step === 'upload'">
      <div style="margin-bottom: 12px; display: flex; justify-content: flex-end">
        <a-button size="small" @click="handleDownloadTemplate">
          <template #icon><DownloadOutlined /></template>
          下载模板
        </a-button>
      </div>
      <a-upload-dragger
        :before-upload="handleBeforeUpload"
        :show-upload-list="false"
        accept=".xlsx"
      >
        <p class="ant-upload-drag-icon"><InboxOutlined /></p>
        <p class="ant-upload-text">点击或拖拽 Excel 文件上传</p>
        <p class="ant-upload-hint">仅支持 .xlsx 格式，文件大小不超过 10MB</p>
      </a-upload-dragger>
      <div v-if="file" style="margin-top: 12px; display: flex; align-items: center; gap: 8px">
        <FileExcelOutlined style="color: #52c41a; font-size: 20px" />
        <span>{{ file.name }}</span>
        <span style="color: #999">({{ (file.size / 1024).toFixed(1) }} KB)</span>
      </div>
      <div style="margin-top: 16px; text-align: right">
        <a-space>
          <a-button @click="handleClose">取消</a-button>
          <a-button type="primary" :disabled="!file" :loading="uploading" @click="handleUpload">
            开始导入
          </a-button>
        </a-space>
      </div>
    </div>

    <!-- Step 2: Progress -->
    <div v-else-if="step === 'progress'">
      <a-result status="info" title="正在导入...">
        <template #extra>
          <div style="width: 100%">
            <a-progress :percent="progressPercent" :status="progressStatus" />
            <div style="margin-top: 12px; color: #666; font-size: 13px">
              <div>总行数: {{ taskInfo.totalRows || '-' }}</div>
              <div>已处理: {{ taskInfo.processedRows || 0 }}</div>
              <div>失败: {{ taskInfo.failedRows || 0 }}</div>
            </div>
          </div>
        </template>
      </a-result>
    </div>

    <!-- Step 3: Result -->
    <div v-else-if="step === 'result'">
      <a-result
        :status="taskInfo.status === 'COMPLETED' ? 'success' : 'error'"
        :title="taskInfo.status === 'COMPLETED' ? '导入完成' : '导入失败'"
      >
        <template #extra>
          <div v-if="taskInfo.status === 'COMPLETED'" style="font-size: 14px">
            <a-row :gutter="16">
              <a-col :span="6"><a-statistic title="总行数" :value="taskInfo.totalRows" /></a-col>
              <a-col :span="6"><a-statistic title="成功" :value="taskInfo.processedRows" value-style="color: #52c41a" /></a-col>
              <a-col :span="6"><a-statistic title="失败" :value="taskInfo.failedRows" value-style="color: #ff4d4f" /></a-col>
              <a-col :span="6">
                <a-statistic
                  title="跳过"
                  :value="Math.max(0, (taskInfo.totalRows || 0) - (taskInfo.processedRows || 0) - (taskInfo.failedRows || 0))"
                />
              </a-col>
            </a-row>
          </div>
          <div v-else style="color: #ff4d4f; font-size: 13px; max-height: 200px; overflow-y: auto; text-align: left">
            {{ taskInfo.errorMessage || '未知错误' }}
          </div>
          <div style="margin-top: 16px">
            <a-button type="primary" @click="handleClose">关闭</a-button>
          </div>
        </template>
      </a-result>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import {
  InboxOutlined,
  DownloadOutlined,
  FileExcelOutlined,
} from '@ant-design/icons-vue'
import {
  importDataElements,
  getDataElementImportStatus,
  downloadDataElementTemplate,
} from '@/api/masterdata'

const emit = defineEmits<{
  (e: 'success'): void
}>()

const visible = ref(false)
const step = ref<'upload' | 'progress' | 'result'>('upload')
const file = ref<File | null>(null)
const uploading = ref(false)
const taskInfo = ref<any>({})
let pollTimer: ReturnType<typeof setInterval> | null = null

const progressPercent = computed(() => {
  if (!taskInfo.value.totalRows) return 0
  if (taskInfo.value.status === 'COMPLETED') return 100
  return Math.round(((taskInfo.value.processedRows || 0) / taskInfo.value.totalRows) * 100)
})

const progressStatus = computed(() => {
  if (taskInfo.value.status === 'FAILED') return 'exception' as const
  if (taskInfo.value.status === 'COMPLETED') return 'success' as const
  return 'active' as const
})

function open() {
  step.value = 'upload'
  file.value = null
  taskInfo.value = {}
  visible.value = true
}

function handleClose() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  visible.value = false
  if (taskInfo.value.status === 'COMPLETED') {
    emit('success')
  }
}

function handleBeforeUpload(f: File) {
  file.value = f
  return false
}

async function handleDownloadTemplate() {
  try {
    const res = await downloadDataElementTemplate()
    const blob = new Blob([res.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'data-element-template.xlsx'
    a.click()
    window.URL.revokeObjectURL(url)
  } catch {
    message.error('下载模板失败')
  }
}

async function handleUpload() {
  if (!file.value) return
  uploading.value = true
  try {
    const res = await importDataElements(file.value)
    const task = res.data.data
    taskInfo.value = task
    step.value = 'progress'
    startPolling(task.id)
  } catch {
    message.error('上传失败')
  } finally {
    uploading.value = false
  }
}

function startPolling(taskId: number) {
  pollTimer = setInterval(async () => {
    try {
      const res = await getDataElementImportStatus(taskId)
      taskInfo.value = res.data.data
      if (['COMPLETED', 'FAILED'].includes(taskInfo.value.status)) {
        if (pollTimer) clearInterval(pollTimer)
        pollTimer = null
        step.value = 'result'
      }
    } catch {
      if (pollTimer) clearInterval(pollTimer)
      pollTimer = null
      step.value = 'result'
      taskInfo.value = { status: 'FAILED', errorMessage: '查询导入状态失败' }
    }
  }, 2000)
}

defineExpose({ open })
</script>
