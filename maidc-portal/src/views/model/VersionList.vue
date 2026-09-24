<template>
  <div v-if="!modelId" style="text-align: center; padding: 60px 0; color: #94a3b8">
    <p style="font-size: 16px; margin-bottom: 8px">请从模型详情页进入版本管理</p>
    <el-button type="primary" @click="$router.push('/model/list')">前往模型列表</el-button>
  </div>
  <template v-else>
    <div style="margin-bottom: 16px; display: flex; justify-content: space-between">
      <div class="flex items-center gap-2">
        <el-button @click="loadVersions">刷新</el-button>
        <el-button type="primary" @click="compareVisible = true">版本对比</el-button>
      </div>
      <el-button type="primary" @click="uploadModal.open()">
        <el-icon class="mr-1"><Upload /></el-icon>上传新版本
      </el-button>
    </div>

    <el-table :data="versions" v-loading="loading" row-key="id" size="small">
      <el-table-column label="版本号" prop="version_no" />
      <el-table-column label="状态" prop="status">
        <template #default="{ row }">
          <StatusBadge :status="row.status" type="version" />
        </template>
      </el-table-column>
      <el-table-column label="文件大小" prop="file_size" />
      <el-table-column label="变更说明" prop="changelog" show-overflow-tooltip />
      <el-table-column label="创建时间" prop="created_at">
        <template #default="{ row }">{{ formatDateTime(row.created_at) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <div class="flex items-center gap-2">
            <el-button link type="primary" @click="viewDetail(row)">查看</el-button>
            <el-button link type="primary" @click="startCompare(row)">对比</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- Version Comparison Section -->
    <el-card shadow="never" class="mt-6 !rounded-xl !border-slate-200/80 shadow-clinical-sm">
      <template #header>
        <span class="font-semibold text-slate-900">版本对比</span>
      </template>
      <div class="flex items-center gap-4" style="margin-bottom: 16px">
        <el-select v-model="compareLeft" placeholder="选择版本1" class="flex-1">
          <el-option v-for="v in versions" :key="v.id" :value="v.id" :label="v.version_no" />
        </el-select>
        <el-select v-model="compareRight" placeholder="选择版本2" class="flex-1">
          <el-option v-for="v in versions" :key="v.id" :value="v.id" :label="v.version_no" />
        </el-select>
        <el-button type="primary" @click="showComparisonResult" :disabled="!compareLeft || !compareRight">对比</el-button>
      </div>

      <template v-if="showComparison">
        <!-- 超参数对比 -->
        <el-table
          :data="comparisonData.hyperparams"
          border
          size="small"
          style="margin-bottom: 16px"
        >
          <el-table-column label="指标" prop="metric" width="180" />
          <el-table-column :label="compareLabels.label1" prop="v1" />
          <el-table-column :label="compareLabels.label2" prop="v2" />
        </el-table>

        <!-- 训练指标对比 -->
        <el-table
          :data="comparisonData.training"
          border
          size="small"
          style="margin-bottom: 16px"
        >
          <el-table-column label="指标" prop="metric" width="180" />
          <el-table-column :label="compareLabels.label1" prop="v1" />
          <el-table-column :label="compareLabels.label2" prop="v2" />
        </el-table>

        <!-- 评估指标对比 -->
        <el-table
          :data="comparisonData.evaluation"
          border
          size="small"
        >
          <el-table-column label="指标" prop="metric" width="180" />
          <el-table-column :label="compareLabels.label1" prop="v1" />
          <el-table-column :label="compareLabels.label2" prop="v2" />
          <el-table-column label="差异" prop="diff" width="120">
            <template #default="{ row }">
              <span :style="{ color: getDiffColor(row.diff) }">{{ row.diff }}</span>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-card>

    <!-- Upload Modal -->
    <el-dialog v-model="uploadModal.visible" title="上传新版本" width="600px">
      <el-form label-width="100px">
        <el-form-item label="版本号" required>
          <el-input v-model="uploadForm.version_no" placeholder="例如: v1.0.0" />
        </el-form-item>
        <el-form-item label="模型文件" required>
          <FileUploader accept=".pt,.onnx,.pb,.pkl,.zip" :max-size="2048" bucket="maidc-models" @success="onFileUploaded" />
        </el-form-item>
        <el-form-item label="变更说明">
          <el-input v-model="uploadForm.changelog" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadModal.close()">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">确定</el-button>
      </template>
    </el-dialog>

    <!-- Compare Modal -->
    <el-dialog v-model="compareVisible" title="版本对比" width="800px">
      <div class="grid grid-cols-1 sm:grid-cols-2 gap-4" style="margin-bottom: 16px">
        <el-select v-model="compareV1" placeholder="选择版本1" style="width: 100%">
          <el-option v-for="v in versions" :key="v.id" :value="v.id" :label="v.version_no" />
        </el-select>
        <el-select v-model="compareV2" placeholder="选择版本2" style="width: 100%">
          <el-option v-for="v in versions" :key="v.id" :value="v.id" :label="v.version_no" />
        </el-select>
      </div>
      <el-button type="primary" @click="doCompare" :loading="comparing" :disabled="!compareV1 || !compareV2">开始对比</el-button>
      <div v-if="compareResult" style="margin-top: 16px">
        <el-descriptions border size="small" :column="1">
          <el-descriptions-item v-for="(val, key) in compareResult" :key="key" :label="key">{{ val }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </template>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue'
import { Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import StatusBadge from '@/components/StatusBadge/index.vue'
import FileUploader from '@/components/FileUploader/index.vue'
import { useModal } from '@/hooks/useModal'
import { getVersions, createVersion, compareVersions } from '@/api/model'
import { formatDateTime } from '@/utils/date'

const props = defineProps<{ modelId?: number }>()
const uploadModal = useModal()
const versions = ref<any[]>([])
const loading = ref(false)
const uploading = ref(false)
const uploadedFile = ref<any>(null)

const uploadForm = reactive({ version_no: '', changelog: '' })

async function loadVersions() {
  if (!props.modelId) return
  loading.value = true
  try {
    const res = await getVersions(props.modelId, { page: 1, page_size: 100 })
    versions.value = res.data.data.items ?? []
  } finally { loading.value = false }
}

function onFileUploaded(fileInfo: any) {
  uploadedFile.value = fileInfo
}

async function handleUpload() {
  if (!uploadForm.version_no) { ElMessage.warning('请输入版本号'); return }
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('version_no', uploadForm.version_no)
    formData.append('changelog', uploadForm.changelog)
    if (uploadedFile.value) formData.append('file', uploadedFile.value)
    await createVersion(props.modelId!, formData)
    ElMessage.success('版本上传成功')
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
    const res = await compareVersions(props.modelId!, compareV1.value!, compareV2.value!)
    compareResult.value = res.data.data
  } finally { comparing.value = false }
}

function viewDetail(record: any) {
  ElMessage.info('查看版本详情: ' + record.version_no)
}

// --- Inline comparison section ---
const compareLeft = ref<number>()
const compareRight = ref<number>()
const showComparison = ref(false)

const comparisonData = {
  hyperparams: [
    { metric: 'learning_rate', v1: '0.001', v2: '0.0005' },
    { metric: 'batch_size', v1: '32', v2: '64' },
    { metric: 'epochs', v1: '100', v2: '150' },
    { metric: 'optimizer', v1: 'Adam', v2: 'AdamW' },
  ],
  training: [
    { metric: 'loss', v1: '0.0823', v2: '0.1205' },
    { metric: 'accuracy', v1: '96.8%', v2: '95.2%' },
    { metric: 'val_loss', v1: '0.0912', v2: '0.1356' },
  ],
  evaluation: [
    { metric: 'AUC', v1: '0.983', v2: '0.923', diff: '+0.060' },
    { metric: 'F1 Score', v1: '0.948', v2: '0.929', diff: '+0.019' },
    { metric: 'Precision', v1: '0.951', v2: '0.938', diff: '+0.013' },
    { metric: 'Recall', v1: '0.945', v2: '0.921', diff: '+0.024' },
    { metric: '推理延迟', v1: '23ms', v2: '21ms', diff: '+2ms' },
    { metric: '模型大小', v1: '520MB', v2: '500MB', diff: '+20MB' },
  ],
}

function getCompareLabels() {
  const v1 = versions.value.find((v: any) => v.id === compareLeft.value)
  const v2 = versions.value.find((v: any) => v.id === compareRight.value)
  return { label1: v1?.version_no || '版本1', label2: v2?.version_no || '版本2' }
}

const compareLabels = computed(() => getCompareLabels())

function getDiffColor(diff: string): string {
  if (!diff) return ''
  if (diff.startsWith('+')) return '#10b981'
  if (diff.startsWith('-')) return '#ef4444'
  return ''
}

function showComparisonResult() {
  if (!compareLeft.value || !compareRight.value) {
    ElMessage.warning('请选择两个版本进行对比')
    return
  }
  if (compareLeft.value === compareRight.value) {
    ElMessage.warning('请选择不同的版本进行对比')
    return
  }
  showComparison.value = true
}

onMounted(loadVersions)
</script>
