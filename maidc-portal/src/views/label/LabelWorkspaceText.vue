<template>
  <PageContainer title="文本标注工作台" :loading="loading">
    <template #extra>
      <div class="flex items-center gap-2">
        <el-button :disabled="!canUndo" size="small" @click="handleUndo">
          <el-icon class="mr-1"><Undo /></el-icon> 撤销
        </el-button>
        <el-button :disabled="!canRedo" size="small" @click="handleRedo">
          <el-icon class="mr-1"><Redo /></el-icon> 重做
        </el-button>
        <el-button @click="aiModal.open()" size="small">
          <el-icon class="mr-1"><MagicStick /></el-icon> AI 预标注
        </el-button>
      </div>
    </template>

    <!-- Top Navigation Bar -->
    <div class="top-nav-bar">
      <div class="top-nav-left">
        <span class="task-name">病理报告NER标注</span>
      </div>
      <div class="top-nav-center">
        <div class="flex items-center justify-center gap-2">
          <el-button size="small" :disabled="currentIndex <= 0" @click="navigateItem(-1)">上一条</el-button>
          <span class="doc-counter">DOC_{{ String(currentIndex + 1).padStart(4, '0') }} / {{ totalCount }}</span>
          <el-button size="small" :disabled="currentIndex >= totalCount - 1" @click="navigateItem(1)">下一条</el-button>
        </div>
      </div>
      <div class="top-nav-right">
        <el-button type="primary" @click="handleSave" :loading="saving">
          <el-icon class="mr-1"><DocumentChecked /></el-icon> 保存
        </el-button>
      </div>
    </div>

    <template v-if="task">
      <el-row :gutter="16" class="mt-3">
        <!-- Left Panel: Text Content -->
        <el-col :span="14">
          <el-card shadow="never" size="small" class="h-full !rounded-xl !border-slate-200/80 shadow-clinical-sm">
            <template #header>
              <span class="font-semibold text-slate-900">文本内容</span>
            </template>
            <div class="text-content-area" @mouseup="handleTextSelect">
              <span
                v-for="(segment, idx) in textSegments"
                :key="idx"
                class="text-segment"
                :class="{
                  'segment-selected': segment.selected,
                  'segment-annotated': segment.annotationLabel,
                }"
                :style="segment.annotationLabel ? { backgroundColor: getLabelColor(segment.annotationLabel) + '33', borderBottom: `2px solid ${getLabelColor(segment.annotationLabel)}` } : {}"
                @click="handleSegmentClick(idx)"
              >{{ segment.text }}</span>
            </div>
          </el-card>
        </el-col>

        <!-- Right Panel: Annotation Panel -->
        <el-col :span="10">
          <!-- Entity Types Card -->
          <el-card shadow="never" size="small" class="mb-3 !rounded-xl !border-slate-200/80 shadow-clinical-sm">
            <template #header>
              <span class="font-semibold text-slate-900">实体类型</span>
            </template>
            <div class="label-buttons">
              <div
                v-for="label in entityLabels"
                :key="label.name"
                class="label-button"
                :class="{ 'label-active': activeLabel === label.name }"
                :style="{ borderColor: label.color, backgroundColor: activeLabel === label.name ? label.color + '22' : 'transparent' }"
                @click="activeLabel = label.name"
              >
                <span class="label-dot" :style="{ backgroundColor: label.color }" />
                <span>{{ label.name }}</span>
              </div>
            </div>
            <p v-if="!entityLabels.length" class="py-4 text-center text-slate-400">
              暂无可用标签
            </p>
          </el-card>

          <!-- Annotated Entities Card -->
          <el-card shadow="never" size="small" class="!rounded-xl !border-slate-200/80 shadow-clinical-sm">
            <template #header>
              <span class="font-semibold text-slate-900">已标注实体</span>
            </template>
            <div v-for="(ann, idx) in annotations" :key="idx" class="annotation-item">
              <div class="annotation-item-header">
                <span class="inline-flex items-center gap-1.5">
                  <span class="inline-block h-2 w-2 rounded-full" :style="{ background: getLabelColor(ann.label) }" />
                  {{ ann.label }}
                </span>
                <el-button text size="small" type="danger" @click="removeAnnotation(idx)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
              <div class="annotation-text">"{{ ann.selectedText }}"</div>
            </div>
            <el-empty v-if="!annotations.length" description="选中文本后点击标签进行标注" :image-size="60" />
          </el-card>

          <!-- Action Buttons at Bottom of Right Panel -->
          <div class="mt-3 flex flex-col gap-2">
            <el-button type="primary" @click="handleSubmit" :loading="submitting">
              <el-icon class="mr-1"><Check /></el-icon> 提交审核
            </el-button>
            <el-button @click="navigateItem(1)">
              跳过
            </el-button>
          </div>
        </el-col>
      </el-row>

      <!-- Bottom: Annotation Review -->
      <el-card v-if="reviewAnnotations.length" shadow="never" size="small" class="mt-3 !rounded-xl !border-slate-200/80 shadow-clinical-sm">
        <template #header>
          <span class="font-semibold text-slate-900">标注审核</span>
        </template>
        <el-table :data="reviewAnnotations" size="small" row-key="id">
          <el-table-column label="标注文本" prop="selectedText" min-width="180" show-overflow-tooltip />
          <el-table-column label="标签" prop="label" width="110" />
          <el-table-column label="来源" prop="source" width="80" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button link size="small" type="primary" @click="handleApproveAnnotation(row)">
                <el-icon class="mr-1"><Check /></el-icon> 通过
              </el-button>
              <el-button link size="small" type="danger" @click="handleRejectAnnotation(row)">
                <el-icon class="mr-1"><Close /></el-icon> 驳回
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>

    <!-- AI Pre-Annotate Modal -->
    <el-dialog v-model="aiModal.visible" title="AI 预标注配置" width="500px">
      <el-form label-position="top">
        <el-form-item label="AI 模型">
          <el-select v-model="aiConfig.model" placeholder="选择模型" style="width: 100%">
            <el-option label="GPT-4" value="gpt-4" />
            <el-option label="GPT-3.5 Turbo" value="gpt-3.5-turbo" />
            <el-option label="Claude 3" value="claude-3" />
            <el-option label="本地 NER 模型" value="local-ner" />
          </el-select>
        </el-form-item>
        <el-form-item label="置信度阈值">
          <el-slider v-model="aiConfig.confidence" :min="50" :max="100" :marks="{ 50: '50%', 75: '75%', 100: '100%' }" />
        </el-form-item>
        <el-form-item label="预标注范围">
          <el-radio-group v-model="aiConfig.scope">
            <el-radio value="current">当前文档</el-radio>
            <el-radio value="all">全部未标注</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="覆盖已有标注">
          <el-switch v-model="aiConfig.overwrite" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="aiModal.close()">取消</el-button>
        <el-button type="primary" :loading="aiLoading" @click="handleAiPreAnnotate">确定</el-button>
      </template>
    </el-dialog>

    <!-- Annotation Review Modal -->
    <el-dialog v-model="reviewModal.visible" title="标注审核" width="480px">
      <template v-if="reviewModal.currentRecord?.value">
        <el-descriptions :column="1" size="small" border>
          <el-descriptions-item label="标注文本">{{ reviewModal.currentRecord.value.selectedText }}</el-descriptions-item>
          <el-descriptions-item label="标签">{{ reviewModal.currentRecord.value.label }}</el-descriptions-item>
          <el-descriptions-item label="标注人">{{ reviewModal.currentRecord.value.annotator_name }}</el-descriptions-item>
        </el-descriptions>
        <el-form label-position="top" class="mt-4">
          <el-form-item label="审核结果" required>
            <el-radio-group v-model="reviewForm.action">
              <el-radio value="APPROVE">通过</el-radio>
              <el-radio value="REJECT">驳回</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="审核意见">
            <el-input v-model="reviewForm.comment" type="textarea" :rows="2" />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="reviewModal.close()">取消</el-button>
        <el-button type="primary" :loading="reviewSubmitting" @click="handleReviewSubmit">确定</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Undo, Redo, MagicStick, DocumentChecked, Delete, Check, Close,
} from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import { useModal } from '@/hooks/useModal'
import { getLabelTask, triggerAiPreAnnotate } from '@/api/label'
import request from '@/utils/request'

defineOptions({ name: 'LabelWorkspaceText' })

const route = useRoute()
const aiModal = useModal()
const reviewModal = useModal()

const task = ref<any>(null)
const loading = ref(false)
const saving = ref(false)
const submitting = ref(false)
const aiLoading = ref(false)
const reviewSubmitting = ref(false)

const currentIndex = ref(0)
const totalCount = ref(0)
const activeLabel = ref('')

// Undo/Redo stacks
const undoStack = ref<any[]>([])
const redoStack = ref<any[]>([])
const canUndo = computed(() => undoStack.value.length > 0)
const canRedo = computed(() => redoStack.value.length > 0)

// Text segments
interface TextSegment {
  text: string
  selected: boolean
  annotationLabel: string
}

const textSegments = ref<TextSegment[]>([])

// Annotations for current item
interface Annotation {
  selectedText: string
  label: string
  startIdx: number
  endIdx: number
}

const annotations = ref<Annotation[]>([])

// Review annotations from AI or other annotators
const reviewAnnotations = ref<any[]>([])

const entityLabels = ref<Array<{ name: string; color: string }>>([
  { name: 'SYMPTOM', color: '#ef4444' },
  { name: 'SIZE', color: '#0ea5e9' },
  { name: 'SIGN', color: '#8b5cf6' },
  { name: 'DIAGNOSIS', color: '#10b981' },
  { name: 'TEST', color: '#f59e0b' },
])

const labelColorMap: Record<string, string> = {}
entityLabels.value.forEach((l) => { labelColorMap[l.name] = l.color })

function getLabelColor(label: string): string {
  return labelColorMap[label] || '#0ea5e9'
}

const aiConfig = reactive({
  model: 'gpt-4',
  confidence: 75,
  scope: 'current',
  overwrite: false,
})

const reviewForm = reactive({ action: 'APPROVE' as string, comment: '' })

async function loadTask() {
  loading.value = true
  try {
    const res = await getLabelTask(Number(route.params.id))
    task.value = res.data.data
    totalCount.value = res.data.data.total_count || 10

    // Load labels from task config
    if (res.data.data.labels?.length) {
      entityLabels.value = res.data.data.labels.map((label: any, idx: number) => {
        const colors = ['#ef4444', '#f59e0b', '#10b981', '#0ea5e9', '#8b5cf6', '#06b6d4', '#ec4899', '#f97316']
        return typeof label === 'string'
          ? { name: label, color: colors[idx % colors.length] }
          : label
      })
      entityLabels.value.forEach((l) => { labelColorMap[l.name] = l.color })
    }

    loadTextItem()
  } finally {
    loading.value = false
  }
}

async function loadTextItem() {
  try {
    const res = await request.get(`/label/tasks/${route.params.id}/items/${currentIndex.value}`)
    const data = res.data.data
    if (data) {
      // Build segments from text
      const text = data.text || data.content || ''
      textSegments.value = text.split('').map((char: string) => ({
        text: char,
        selected: false,
        annotationLabel: '',
      }))
      annotations.value = data.annotations || []
      // Apply existing annotations to segments
      annotations.value.forEach((ann) => {
        for (let i = ann.startIdx; i < ann.endIdx && i < textSegments.value.length; i++) {
          textSegments.value[i].annotationLabel = ann.label
        }
      })
      reviewAnnotations.value = data.review_annotations || []
    }
  } catch {
    // Fallback demo content - pathology report
    const demoText = '患者，男，68岁。主诉：反复咳嗽、胸痛2月余。CT检查示右肺上叶3.2×2.8cm占位性病变，边缘可见毛刺征，纵隔淋巴结肿大。术后病理：中分化腺癌，淋巴结转移2/12。'
    textSegments.value = demoText.split('').map((char) => ({
      text: char,
      selected: false,
      annotationLabel: '',
    }))

    // Pre-annotations with start/end indices in the demo text
    const preAnnotations = [
      { text: '咳嗽', label: 'SYMPTOM' },
      { text: '胸痛', label: 'SYMPTOM' },
      { text: '3.2×2.8cm', label: 'SIZE' },
      { text: '毛刺征', label: 'SIGN' },
      { text: '中分化腺癌', label: 'DIAGNOSIS' },
    ]

    annotations.value = []
    preAnnotations.forEach((pa) => {
      const startIdx = demoText.indexOf(pa.text)
      if (startIdx >= 0) {
        const endIdx = startIdx + pa.text.length
        annotations.value.push({
          selectedText: pa.text,
          label: pa.label,
          startIdx,
          endIdx,
        })
        for (let i = startIdx; i < endIdx && i < textSegments.value.length; i++) {
          textSegments.value[i].annotationLabel = pa.label
        }
      }
    })

    reviewAnnotations.value = []
  }
}

function handleTextSelect() {
  const selection = window.getSelection()
  if (!selection || selection.isCollapsed) return

  // Find which segments are selected
  // Simple approach: clear previous selection, mark new
  textSegments.value.forEach((s) => { s.selected = false })

  // The browser handles selection visually, we use it for context
}

function handleSegmentClick(idx: number) {
  if (!activeLabel.value) {
    ElMessage.info('请先选择一个实体标签')
    return
  }

  // Find contiguous text around click for simple annotation
  // For a more advanced approach, this would handle mouse drag selection
  const segment = textSegments.value[idx]
  if (segment.annotationLabel) {
    ElMessage.info('该文本段已有标注')
    return
  }

  // Save state for undo
  saveUndoState()

  // Annotate the clicked segment (single character for demo)
  segment.annotationLabel = activeLabel.value
  annotations.value.push({
    selectedText: segment.text,
    label: activeLabel.value,
    startIdx: idx,
    endIdx: idx + 1,
  })
}

function removeAnnotation(idx: number) {
  saveUndoState()
  const ann = annotations.value[idx]
  // Clear segments
  for (let i = ann.startIdx; i < ann.endIdx && i < textSegments.value.length; i++) {
    textSegments.value[i].annotationLabel = ''
  }
  annotations.value.splice(idx, 1)
}

function saveUndoState() {
  undoStack.value.push({
    segments: JSON.parse(JSON.stringify(textSegments.value)),
    annotations: JSON.parse(JSON.stringify(annotations.value)),
  })
  if (undoStack.value.length > 50) undoStack.value.shift()
  redoStack.value = []
}

function handleUndo() {
  if (!canUndo.value) return
  redoStack.value.push({
    segments: JSON.parse(JSON.stringify(textSegments.value)),
    annotations: JSON.parse(JSON.stringify(annotations.value)),
  })
  const state = undoStack.value.pop()!
  textSegments.value = state.segments
  annotations.value = state.annotations
}

function handleRedo() {
  if (!canRedo.value) return
  undoStack.value.push({
    segments: JSON.parse(JSON.stringify(textSegments.value)),
    annotations: JSON.parse(JSON.stringify(annotations.value)),
  })
  const state = redoStack.value.pop()!
  textSegments.value = state.segments
  annotations.value = state.annotations
}

function navigateItem(delta: number) {
  currentIndex.value += delta
  undoStack.value = []
  redoStack.value = []
  loadTextItem()
}

async function handleSave() {
  saving.value = true
  try {
    await request.post(`/label/tasks/${route.params.id}/annotations`, {
      item_index: currentIndex.value,
      annotations: annotations.value,
    })
    ElMessage.success('标注已保存')
  } finally {
    saving.value = false
  }
}

async function handleSubmit() {
  submitting.value = true
  try {
    await request.post(`/label/tasks/${route.params.id}/annotations`, {
      item_index: currentIndex.value,
      annotations: annotations.value,
      submit: true,
    })
    ElMessage.success('标注已提交')
  } finally {
    submitting.value = false
  }
}

async function handleAiPreAnnotate() {
  aiLoading.value = true
  try {
    await triggerAiPreAnnotate(Number(route.params.id))
    ElMessage.success('AI 预标注已提交，请稍候刷新')
    aiModal.close()
    // Reload after a short delay
    setTimeout(() => loadTextItem(), 2000)
  } finally {
    aiLoading.value = false
  }
}

async function handleApproveAnnotation(record: any) {
  try {
    await request.post(`/label/annotations/${record.id}/review`, { action: 'APPROVE' })
    ElMessage.success('标注已通过')
    reviewAnnotations.value = reviewAnnotations.value.filter((r) => r.id !== record.id)
  } catch {
    // error handled by request interceptor
  }
}

async function handleRejectAnnotation(record: any) {
  try {
    await request.post(`/label/annotations/${record.id}/review`, { action: 'REJECT' })
    ElMessage.info('标注已驳回')
    reviewAnnotations.value = reviewAnnotations.value.filter((r) => r.id !== record.id)
  } catch {
    // error handled by request interceptor
  }
}

function handleReviewSubmit() {
  ElMessage.success(reviewForm.action === 'APPROVE' ? '审核通过' : '已驳回')
  reviewModal.close()
}

onMounted(loadTask)
</script>

<style scoped>
.top-nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  padding: 12px 24px;
  border-radius: 6px;
}

.top-nav-left {
  flex: 1;
}

.task-name {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.top-nav-center {
  flex: 1;
  text-align: center;
}

.doc-counter {
  font-size: 14px;
  color: #475569;
  min-width: 120px;
  display: inline-block;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

.top-nav-right {
  flex: 1;
  text-align: right;
}

.text-content-area {
  min-height: 350px;
  padding: 16px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  line-height: 2;
  font-size: 15px;
  user-select: text;
}

.text-segment {
  cursor: pointer;
  border-radius: 2px;
  transition: background-color 0.15s;
}

.text-segment:hover {
  background-color: rgba(14, 165, 233, 0.08);
}

.segment-selected {
  background-color: rgba(14, 165, 233, 0.15);
}

.segment-annotated {
  cursor: pointer;
}

.label-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.label-button {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 13px;
}

.label-button:hover {
  opacity: 0.85;
}

.label-active {
  font-weight: 500;
}

.label-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.annotation-item {
  padding: 8px;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  margin-bottom: 6px;
}

.annotation-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.annotation-text {
  font-size: 13px;
  color: #64748b;
  font-style: italic;
}
</style>
