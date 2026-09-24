<template>
  <el-upload
    :accept="accept"
    :multiple="multiple"
    :before-upload="handleBeforeUpload"
    :http-request="handleUpload"
    :file-list="fileList"
    :on-remove="handleRemove"
  >
    <el-button type="primary" plain>
      <el-icon class="mr-1"><Upload /></el-icon>
      点击上传
    </el-button>
  </el-upload>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadFile, UploadUserFile } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
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
const fileList = ref<UploadUserFile[]>([])

function handleBeforeUpload(file: File) {
  const sizeMB = file.size / 1024 / 1024
  if (sizeMB > props.maxSize) {
    ElMessage.error(`文件大小不能超过 ${props.maxSize}MB`)
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
    ElMessage.error(msg)
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
