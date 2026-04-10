<template>
  <a-upload
    :accept="accept"
    :multiple="multiple"
    :before-upload="handleBeforeUpload"
    :custom-request="handleUpload"
    :file-list="fileList"
    @remove="handleRemove"
  >
    <a-button>
      <UploadOutlined />
      点击上传
    </a-button>
    <template #itemRender="{ file }">
      <span :class="['custom-file-item', file.status]">
        <PaperClipOutlined />
        <span class="file-name">{{ file.name }}</span>
        <LoadingOutlined v-if="file.status === 'uploading'" />
        <CheckCircleOutlined v-else-if="file.status === 'done'" style="color: #52c41a" />
        <CloseCircleOutlined v-else-if="file.status === 'error'" style="color: #ff4d4f" />
      </span>
    </template>
  </a-upload>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  UploadOutlined,
  PaperClipOutlined,
  LoadingOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons-vue'
import type { UploadFile, UploadProps } from 'ant-design-vue'
import request from '@/utils/request'

interface FileInfo {
  name: string
  url: string
  size: number
  bucket?: string
}

interface Props {
  accept?: string
  maxSize?: number
  multiple?: boolean
  bucket?: string
}

interface Emits {
  (e: 'success', fileInfo: FileInfo): void
  (e: 'error', msg: string): void
}

const props = withDefaults(defineProps<Props>(), {
  accept: '*',
  maxSize: 100,
  multiple: false,
  bucket: 'default',
})

const emit = defineEmits<Emits>()
const fileList = ref<UploadFile[]>([])

function handleBeforeUpload(file: File) {
  const sizeMB = file.size / 1024 / 1024
  if (sizeMB > props.maxSize) {
    message.error(`文件大小不能超过 ${props.maxSize}MB`)
    emit('error', `文件大小不能超过 ${props.maxSize}MB`)
    return false
  }
  return true
}

async function handleUpload(options: any) {
  const { file, onSuccess, onError, onProgress } = options

  try {
    // Step 1: get presigned URL from backend
    const presignRes = await request.post('/files/presign', {
      file_name: file.name,
      content_type: file.type,
      bucket: props.bucket,
    })
    const { upload_url, file_url } = presignRes.data.data

    // Step 2: upload to MinIO via presigned URL
    await new Promise<void>((resolve, reject) => {
      const xhr = new XMLHttpRequest()
      xhr.open('PUT', upload_url)
      xhr.setRequestHeader('Content-Type', file.type)

      xhr.upload.addEventListener('progress', (e) => {
        if (e.lengthComputable) {
          onProgress({ percent: Math.round((e.loaded / e.total) * 100) }, file)
        }
      })

      xhr.onload = () => {
        if (xhr.status >= 200 && xhr.status < 300) {
          resolve()
        } else {
          reject(new Error(`上传失败: ${xhr.statusText}`))
        }
      }

      xhr.onerror = () => reject(new Error('网络异常'))
      xhr.send(file)
    })

    onSuccess({}, file)

    const fileInfo: FileInfo = {
      name: file.name,
      url: file_url,
      size: file.size,
      bucket: props.bucket,
    }
    emit('success', fileInfo)
  } catch (err: any) {
    onError(err)
    const msg = err?.message || '上传失败'
    emit('error', msg)
    message.error(msg)
  }
}

function handleRemove(file: UploadFile) {
  const idx = fileList.value.findIndex((f) => f.uid === file.uid)
  if (idx > -1) fileList.value.splice(idx, 1)
}
</script>

<style scoped>
.custom-file-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 0;
}
.file-name {
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
