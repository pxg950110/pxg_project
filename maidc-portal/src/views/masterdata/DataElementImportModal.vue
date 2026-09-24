<template>
  <el-dialog
    v-model="visible"
    title="导入数据元"
    width="600px"
    :destroy-on-close="true"
    @close="handleClose"
  >
    <!-- Step 1: Upload -->
    <div v-if="step === 'upload'">
      <div style="margin-bottom: 12px; display: flex; justify-content: flex-end">
        <el-button size="small" @click="handleDownloadTemplate">
          <el-icon class="mr-1"><Download /></el-icon>
          下载模板
        </el-button>
      </div>
      <el-upload
        drag
        action="#"
        :before-upload="handleBeforeUpload"
        :show-file-list="false"
        accept=".xlsx"
      >
        <el-icon class="el-icon--upload" style="color: #0ea5e9"><UploadFilled /></el-icon>
        <div class="el-upload__text">点击或拖拽 Excel 文件上传</div>
        <template #tip>
          <div class="el-upload__tip">仅支持 .xlsx 格式，文件大小不超过 10MB</div>
        </template>
      </el-upload>
      <div v-if="file" style="margin-top: 12px; display: flex; align-items: center; gap: 8px">
        <el-icon style="color: #10b981; font-size: 20px"><Document /></el-icon>
        <span>{{ file.name }}</span>
        <span style="color: #94a3b8">({{ (file.size / 1024).toFixed(1) }} KB)</span>
      </div>
      <div style="margin-top: 16px; text-align: right">
        <div class="inline-flex items-center gap-3">
          <el-button @click="visible = false">取消</el-button>
          <el-button type="primary" :disabled="!file" :loading="uploading" @click="handleUpload">
            开始导入
          </el-button>
        </div>
      </div>
    </div>

    <!-- Step 2: Progress -->
    <div v-else-if="step === 'progress'">
      <el-result icon="info" title="正在导入...">
        <template #extra>
          <div style="width: 100%">
            <el-progress :percentage="progressPercent" :status="progressStatus" :stroke-width="6" />
            <div style="margin-top: 12px; color: #64748b; font-size: 13px">
              <div>总行数: {{ taskInfo.totalRows || '-' }}</div>
              <div>已处理: {{ taskInfo.processedRows || 0 }}</div>
              <div>失败: {{ taskInfo.failedRows || 0 }}</div>
            </div>
          </div>
        </template>
      </el-result>
    </div>

    <!-- Step 3: Result -->
    <div v-else-if="step === 'result'">
      <el-result
        :icon="taskInfo.status === 'COMPLETED' ? 'success' : 'error'"
        :title="taskInfo.status === 'COMPLETED' ? '导入完成' : '导入失败'"
      >
        <template #extra>
          <div v-if="taskInfo.status === 'COMPLETED'" class="grid grid-cols-4 gap-4" style="font-size: 14px">
            <div class="text-center">
              <div style="color: #64748b; font-size: 13px; margin-bottom: 4px">总行数</div>
              <div style="font-size: 20px; font-weight: 600">{{ taskInfo.totalRows }}</div>
            </div>
            <div class="text-center">
              <div style="color: #64748b; font-size: 13px; margin-bottom: 4px">成功</div>
              <div style="font-size: 20px; font-weight: 600; color: #10b981">{{ taskInfo.processedRows }}</div>
            </div>
            <div class="text-center">
              <div style="color: #64748b; font-size: 13px; margin-bottom: 4px">失败</div>
              <div style="font-size: 20px; font-weight: 600; color: #ef4444">{{ taskInfo.failedRows }}</div>
            </div>
            <div class="text-center">
              <div style="color: #64748b; font-size: 13px; margin-bottom: 4px">跳过</div>
              <div style="font-size: 20px; font-weight: 600">{{
                Math.max(0, (taskInfo.totalRows || 0) - (taskInfo.processedRows || 0) - (taskInfo.failedRows || 0))
              }}</div>
            </div>
          </div>
          <div v-else style="color: #ef4444; font-size: 13px; max-height: 200px; overflow-y: auto; text-align: left">
            {{ taskInfo.errorMessage || '未知错误' }}
          </div>
          <div style="margin-top: 16px">
            <el-button type="primary" @click="visible = false">关闭</el-button>
          </div>
        </template>
      </el-result>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  UploadFilled,
  Download,
  Document,
} from '@element-plus/icons-vue'
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
  return undefined
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
    ElMessage.error('下载模板失败')
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
    ElMessage.error('上传失败')
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
