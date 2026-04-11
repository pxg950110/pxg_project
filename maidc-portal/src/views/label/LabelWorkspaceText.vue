<template>
  <PageContainer title="文本标注工作台" :loading="loading">
    <template #extra>
      <a-space>
        <a-button @click="handleUndo" :disabled="!canUndo" size="small">
          <UndoOutlined /> 撤销
        </a-button>
        <a-button @click="handleRedo" :disabled="!canRedo" size="small">
          <RedoOutlined /> 重做
        </a-button>
        <a-button @click="aiModal.open()" size="small">
          <RobotOutlined /> AI 预标注
        </a-button>
      </a-space>
    </template>

    <!-- Top Navigation Bar -->
    <div class="top-nav-bar">
      <div class="top-nav-left">
        <span class="task-name">病理报告NER标注</span>
      </div>
      <div class="top-nav-center">
        <a-space>
          <a-button size="small" :disabled="currentIndex <= 0" @click="navigateItem(-1)">上一条</a-button>
          <span class="doc-counter">DOC_{{ String(currentIndex + 1).padStart(4, '0') }} / {{ totalCount }}</span>
          <a-button size="small" :disabled="currentIndex >= totalCount - 1" @click="navigateItem(1)">下一条</a-button>
        </a-space>
      </div>
      <div class="top-nav-right">
        <a-button type="primary" @click="handleSave" :loading="saving">
          <SaveOutlined /> 保存
        </a-button>
      </div>
    </div>

    <template v-if="task">
      <a-row :gutter="16" style="margin-top: 12px">
        <!-- Left Panel: Text Content -->
        <a-col :span="14">
          <a-card title="文本内容" size="small" :style="{ height: '100%' }">
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
          </a-card>
        </a-col>

        <!-- Right Panel: Annotation Panel -->
        <a-col :span="10">
          <!-- Entity Types Card -->
          <a-card title="实体类型" size="small" style="margin-bottom: 12px">
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
            <p v-if="!entityLabels.length" style="color: rgba(0,0,0,0.25); text-align: center; padding: 16px 0">
              暂无可用标签
            </p>
          </a-card>

          <!-- Annotated Entities Card -->
          <a-card title="已标注实体" size="small">
            <div v-for="(ann, idx) in annotations" :key="idx" class="annotation-item">
              <div class="annotation-item-header">
                <a-tag :color="getLabelColor(ann.label)">{{ ann.label }}</a-tag>
                <a-button type="text" size="small" danger @click="removeAnnotation(idx)">
                  <DeleteOutlined />
                </a-button>
              </div>
              <div class="annotation-text">"{{ ann.selectedText }}"</div>
            </div>
            <a-empty v-if="!annotations.length" description="选中文本后点击标签进行标注" :image="null" />
          </a-card>

          <!-- Action Buttons at Bottom of Right Panel -->
          <div style="margin-top: 12px; display: flex; flex-direction: column; gap: 8px">
            <a-button type="primary" block @click="handleSubmit" :loading="submitting">
              <CheckOutlined /> 提交审核
            </a-button>
            <a-button block @click="navigateItem(1)">
              跳过
            </a-button>
          </div>
        </a-col>
      </a-row>

      <!-- Bottom: Annotation Review -->
      <a-card title="标注审核" size="small" style="margin-top: 12px" v-if="reviewAnnotations.length">
        <a-table
          :columns="reviewColumns"
          :data-source="reviewAnnotations"
          size="small"
          row-key="id"
          :pagination="false"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" @click="handleApproveAnnotation(record)">
                  <CheckOutlined /> 通过
                </a-button>
                <a-button type="link" size="small" danger @click="handleRejectAnnotation(record)">
                  <CloseOutlined /> 驳回
                </a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>
    </template>

    <!-- AI Pre-Annotate Modal -->
    <a-modal
      v-model:open="aiModal.visible"
      title="AI 预标注配置"
      @ok="handleAiPreAnnotate"
      :confirm-loading="aiLoading"
      width="500px"
    >
      <a-form layout="vertical">
        <a-form-item label="AI 模型">
          <a-select v-model:value="aiConfig.model" placeholder="选择模型">
            <a-select-option value="gpt-4">GPT-4</a-select-option>
            <a-select-option value="gpt-3.5-turbo">GPT-3.5 Turbo</a-select-option>
            <a-select-option value="claude-3">Claude 3</a-select-option>
            <a-select-option value="local-ner">本地 NER 模型</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="置信度阈值">
          <a-slider v-model:value="aiConfig.confidence" :min="50" :max="100" :marks="{ 50: '50%', 75: '75%', 100: '100%' }" />
        </a-form-item>
        <a-form-item label="预标注范围">
          <a-radio-group v-model:value="aiConfig.scope">
            <a-radio value="current">当前文档</a-radio>
            <a-radio value="all">全部未标注</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="覆盖已有标注">
          <a-switch v-model:checked="aiConfig.overwrite" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- Annotation Review Modal -->
    <a-modal
      v-model:open="reviewModal.visible"
      title="标注审核"
      @ok="handleReviewSubmit"
      :confirm-loading="reviewSubmitting"
      width="480px"
    >
      <template v-if="reviewModal.currentRecord?.value">
        <a-descriptions :column="1" size="small" bordered>
          <a-descriptions-item label="标注文本">{{ reviewModal.currentRecord.value.selectedText }}</a-descriptions-item>
          <a-descriptions-item label="标签">{{ reviewModal.currentRecord.value.label }}</a-descriptions-item>
          <a-descriptions-item label="标注人">{{ reviewModal.currentRecord.value.annotator_name }}</a-descriptions-item>
        </a-descriptions>
        <a-form layout="vertical" style="margin-top: 16px">
          <a-form-item label="审核结果" required>
            <a-radio-group v-model:value="reviewForm.action">
              <a-radio value="APPROVE">通过</a-radio>
              <a-radio value="REJECT">驳回</a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item label="审核意见">
            <a-textarea v-model:value="reviewForm.comment" :rows="2" />
          </a-form-item>
        </a-form>
      </template>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import {
  UndoOutlined,
  RedoOutlined,
  RobotOutlined,
  SaveOutlined,
  SendOutlined,
  DeleteOutlined,
  CheckOutlined,
  CloseOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
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
  { name: 'SYMPTOM', color: '#ff4d4f' },
  { name: 'SIZE', color: '#1677ff' },
  { name: 'SIGN', color: '#722ed1' },
  { name: 'DIAGNOSIS', color: '#52c41a' },
  { name: 'TEST', color: '#faad14' },
])

const labelColorMap: Record<string, string> = {}
entityLabels.value.forEach((l) => { labelColorMap[l.name] = l.color })

function getLabelColor(label: string): string {
  return labelColorMap[label] || '#1677ff'
}

const aiConfig = reactive({
  model: 'gpt-4',
  confidence: 75,
  scope: 'current',
  overwrite: false,
})

const reviewForm = reactive({ action: 'APPROVE' as string, comment: '' })

const reviewColumns = [
  { title: '标注文本', dataIndex: 'selectedText', key: 'selectedText', ellipsis: true },
  { title: '标签', dataIndex: 'label', key: 'label', width: 100 },
  { title: '来源', dataIndex: 'source', key: 'source', width: 80 },
  { title: '操作', key: 'action', width: 160 },
]

async function loadTask() {
  loading.value = true
  try {
    const res = await getLabelTask(Number(route.params.id))
    task.value = res.data.data
    totalCount.value = res.data.data.total_count || 10

    // Load labels from task config
    if (res.data.data.labels?.length) {
      entityLabels.value = res.data.data.labels.map((label: any, idx: number) => {
        const colors = ['#ff4d4f', '#faad14', '#52c41a', '#1677ff', '#722ed1', '#13c2c2', '#eb2f96', '#fa541c']
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
    message.info('请先选择一个实体标签')
    return
  }

  // Find contiguous text around click for simple annotation
  // For a more advanced approach, this would handle mouse drag selection
  const segment = textSegments.value[idx]
  if (segment.annotationLabel) {
    message.info('该文本段已有标注')
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
    message.success('标注已保存')
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
    message.success('标注已提交')
  } finally {
    submitting.value = false
  }
}

async function handleAiPreAnnotate() {
  aiLoading.value = true
  try {
    await triggerAiPreAnnotate(Number(route.params.id))
    message.success('AI 预标注已提交，请稍候刷新')
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
    message.success('标注已通过')
    reviewAnnotations.value = reviewAnnotations.value.filter((r) => r.id !== record.id)
  } catch {
    // error handled by request interceptor
  }
}

async function handleRejectAnnotation(record: any) {
  try {
    await request.post(`/label/annotations/${record.id}/review`, { action: 'REJECT' })
    message.info('标注已驳回')
    reviewAnnotations.value = reviewAnnotations.value.filter((r) => r.id !== record.id)
  } catch {
    // error handled by request interceptor
  }
}

function handleReviewSubmit() {
  message.success(reviewForm.action === 'APPROVE' ? '审核通过' : '已驳回')
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
  color: rgba(0, 0, 0, 0.85);
}

.top-nav-center {
  flex: 1;
  text-align: center;
}

.doc-counter {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.65);
  min-width: 120px;
  display: inline-block;
  text-align: center;
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
  background-color: rgba(22, 119, 255, 0.08);
}

.segment-selected {
  background-color: rgba(22, 119, 255, 0.15);
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
  color: rgba(0, 0, 0, 0.65);
  font-style: italic;
}
</style>
