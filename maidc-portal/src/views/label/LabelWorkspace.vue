<template>
  <div class="workspace-root">
    <!-- Left Tool Sidebar -->
    <div class="tool-sidebar">
      <div class="tool-group">
        <template v-for="(tool, idx) in drawingTools" :key="tool.key">
          <div
            class="tool-btn"
            :class="{ active: activeTool === tool.key }"
            :title="tool.tooltip"
            @click="activeTool = tool.key"
          >
            <component :is="tool.icon" />
          </div>
        </template>
      </div>
      <div class="tool-divider"></div>
      <div class="tool-group">
        <template v-for="tool in utilityTools" :key="tool.key">
          <div
            class="tool-btn"
            :title="tool.tooltip"
            @click="handleUtility(tool.key)"
          >
            <component :is="tool.icon" />
          </div>
        </template>
      </div>
    </div>

    <!-- Central DICOM Viewer -->
    <div class="viewer-panel">
      <!-- Header bar -->
      <div class="viewer-header">
        <div class="header-left">
          <span class="task-title">{{ taskInfo?.name || '标注任务' }}</span>
        </div>
        <div class="header-center">
          <a-button size="small" :disabled="currentIndex <= 0" @click="navigateItem(-1)">上一张</a-button>
          <span class="image-counter">{{ currentIndex + 1 }} / {{ totalCount }}</span>
          <a-button size="small" :disabled="currentIndex >= totalCount - 1" @click="navigateItem(1)">下一张</a-button>
        </div>
        <div class="header-right">
          <a-button type="primary" size="small" @click="handleSave">保存</a-button>
        </div>
      </div>

      <!-- Viewer body -->
      <div class="viewer-body">
        <!-- Crosshair lines -->
        <div class="crosshair crosshair-h"></div>
        <div class="crosshair crosshair-v"></div>

        <!-- Mock annotation rectangles -->
        <div
          v-for="ann in annotations"
          :key="ann.id"
          class="annotation-rect"
          :style="{
            left: ann.x + 'px',
            top: ann.y + 'px',
            width: ann.w + 'px',
            height: ann.h + 'px',
            borderColor: tagColors[ann.label] || '#999'
          }"
        >
          <span
            class="annotation-label"
            :style="{ backgroundColor: tagColors[ann.label] || '#999' }"
          >{{ ann.label }}</span>
        </div>

        <!-- DICOM metadata bar -->
        <div class="dicom-meta">
          WL: -600 WW: 1500 &nbsp;|&nbsp; 800&times;600 &nbsp;|&nbsp; CT 胸部横断面
        </div>
      </div>
    </div>

    <!-- Right Annotation Panel -->
    <div class="right-panel">
      <div class="panel-section">
        <div class="section-title">标注列表</div>

        <!-- Tag legend -->
        <div class="tag-legend">
          <span
            v-for="(color, label) in tagColors"
            :key="label"
            class="tag-item"
          >
            <span class="tag-dot" :style="{ backgroundColor: color }"></span>
            {{ label }}
          </span>
          <a class="add-tag-link" @click="message.info('添加标签')">
            <PlusOutlined /> 添加标签
          </a>
        </div>

        <!-- Annotations list -->
        <div class="annotations-list">
          <div
            v-for="ann in annotations"
            :key="ann.id"
            class="annotation-item"
          >
            <span class="tag-dot" :style="{ backgroundColor: tagColors[ann.label] || '#999' }"></span>
            <span class="ann-label">{{ ann.label }}</span>
            <span class="ann-coords">x:{{ ann.x }} y:{{ ann.y }} w:{{ ann.w }} h:{{ ann.h }}</span>
            <span class="ann-delete" @click="removeAnnotation(ann.id)">
              <DeleteOutlined />
            </span>
          </div>
        </div>
      </div>

      <!-- Bottom buttons -->
      <div class="panel-footer">
        <a-button type="primary" block @click="handleSubmit">
          <CheckOutlined /> 提交审核
        </a-button>
        <a-button block style="margin-top: 8px" @click="handleSkip">
          <FastForwardOutlined /> 跳过
        </a-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  BorderOutlined, GatewayOutlined, RadiusSettingOutlined,
  EditOutlined, FontSizeOutlined, ZoomInOutlined, ZoomOutOutlined,
  UndoOutlined, RedoOutlined, DragOutlined,
  PlusOutlined, DeleteOutlined, CheckOutlined, FastForwardOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { useModal } from '@/hooks/useModal'
import { getLabelTask, getLabelItems, getLabelItemAnnotations, saveLabelAnnotations, submitLabelItem, skipLabelItem } from '@/api/label'

const route = useRoute()
const taskId = computed(() => Number(route.params.id))

// Tool definitions
const drawingTools = [
  { key: 'select', icon: DragOutlined, tooltip: '选择' },
  { key: 'rectangle', icon: BorderOutlined, tooltip: '矩形框' },
  { key: 'polygon', icon: GatewayOutlined, tooltip: '多边形' },
  { key: 'ellipse', icon: RadiusSettingOutlined, tooltip: '椭圆' },
  { key: 'freehand', icon: EditOutlined, tooltip: '自由绘制' },
  { key: 'text', icon: FontSizeOutlined, tooltip: '文本' },
]

const utilityTools = [
  { key: 'zoom-in', icon: ZoomInOutlined, tooltip: '放大' },
  { key: 'zoom-out', icon: ZoomOutOutlined, tooltip: '缩小' },
  { key: 'undo', icon: UndoOutlined, tooltip: '撤销' },
  { key: 'redo', icon: RedoOutlined, tooltip: '重做' },
]

// Tool state
const activeTool = ref('rectangle')

// Task & items data
const taskInfo = ref<any>(null)
const items = ref<any[]>([])
const currentIndex = ref(0)
const totalCount = ref(0)
const loading = ref(false)

// Annotations
const tagColors: Record<string, string> = {
  nodule: '#ff4d4f',
  mass: '#1677ff',
  effusion: '#722ed1',
}
const annotations = ref<any[]>([])

// Utility handler
function handleUtility(key: string) {
  if (key === 'undo') message.info('撤销')
  else if (key === 'redo') message.info('重做')
  else if (key === 'zoom-in') message.info('放大')
  else if (key === 'zoom-out') message.info('缩小')
}

async function loadTask() {
  loading.value = true
  try {
    const res = await getLabelTask(taskId.value)
    taskInfo.value = res.data.data
    // Load label tags from task config
    if (taskInfo.value?.tags) {
      taskInfo.value.tags.forEach((tag: string, idx: number) => {
        if (!tagColors[tag]) {
          const colors = ['#ff4d4f', '#1677ff', '#722ed1', '#fa8c16', '#13c2c2', '#eb2f96']
          tagColors[tag] = colors[idx % colors.length]
        }
      })
    }
  } catch { /* ignore */ }
}

async function loadItems() {
  try {
    const res = await getLabelItems(taskId.value, { page: 1, page_size: 1000 })
    items.value = res.data.data?.items || []
    totalCount.value = res.data.data?.total || 0
    if (items.value.length > 0) {
      await loadAnnotations()
    }
  } catch { /* ignore */ }
}

async function loadAnnotations() {
  if (!items.value[currentIndex.value]) return
  const itemId = items.value[currentIndex.value].id
  try {
    const res = await getLabelItemAnnotations(taskId.value, itemId)
    annotations.value = res.data.data || []
  } catch {
    annotations.value = []
  }
}

async function handleSave() {
  if (!items.value[currentIndex.value]) { message.warning('没有可保存的标注项'); return }
  const itemId = items.value[currentIndex.value].id
  try {
    await saveLabelAnnotations(taskId.value, itemId, { annotations: annotations.value })
    message.success('标注已保存')
  } catch {
    message.error('保存失败')
  }
}

async function handleSubmit() {
  if (!items.value[currentIndex.value]) return
  const itemId = items.value[currentIndex.value].id
  try {
    await handleSave()
    await submitLabelItem(taskId.value, itemId)
    message.success('已提交审核')
  } catch {
    message.error('提交失败')
  }
}

async function handleSkip() {
  if (!items.value[currentIndex.value]) return
  const itemId = items.value[currentIndex.value].id
  try {
    await skipLabelItem(taskId.value, itemId)
    message.info('已跳过')
    if (currentIndex.value < totalCount.value - 1) {
      currentIndex.value++
      await loadAnnotations()
    }
  } catch {
    message.error('操作失败')
  }
}

function removeAnnotation(id: number) {
  annotations.value = annotations.value.filter(a => a.id !== id)
}

async function navigateItem(delta: number) {
  const newIndex = currentIndex.value + delta
  if (newIndex < 0 || newIndex >= totalCount.value) return
  await handleSave()
  currentIndex.value = newIndex
  await loadAnnotations()
}

onMounted(async () => {
  await loadTask()
  await loadItems()
})
</script>

<style scoped>
.workspace-root {
  display: flex;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  background: #1e1e2e;
}

/* =================== Left Tool Sidebar =================== */
.tool-sidebar {
  width: 48px;
  min-width: 48px;
  height: 100vh;
  background: #1e1e2e;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 0;
  border-right: 1px solid #333;
}

.tool-group {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.tool-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  cursor: pointer;
  color: #888;
  font-size: 18px;
  border: none;
  background: transparent;
  transition: all 0.2s;
}

.tool-btn:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}

.tool-btn.active {
  color: #fff;
  background: #1677ff;
}

.tool-divider {
  width: 24px;
  height: 1px;
  background: #444;
  margin: 8px 0;
}

/* =================== Central DICOM Viewer =================== */
.viewer-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-width: 0;
}

.viewer-header {
  height: 48px;
  min-height: 48px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid #e8e8e8;
}

.header-left {
  flex: 1;
}

.task-title {
  font-weight: 600;
  font-size: 15px;
  color: #000;
}

.header-center {
  display: flex;
  align-items: center;
  gap: 12px;
}

.image-counter {
  font-size: 14px;
  color: #333;
  font-variant-numeric: tabular-nums;
}

.header-right {
  flex: 1;
  display: flex;
  justify-content: flex-end;
}

.viewer-body {
  flex: 1;
  background: #1e1e2e;
  position: relative;
  overflow: hidden;
}

/* Crosshair */
.crosshair {
  position: absolute;
  z-index: 1;
  pointer-events: none;
}

.crosshair-h {
  top: 50%;
  left: 0;
  right: 0;
  height: 1px;
  border-top: 1px dashed rgba(255, 255, 255, 0.2);
}

.crosshair-v {
  left: 50%;
  top: 0;
  bottom: 0;
  width: 1px;
  border-left: 1px dashed rgba(255, 255, 255, 0.2);
}

/* Annotation rectangles */
.annotation-rect {
  position: absolute;
  border: 2px solid;
  z-index: 2;
  pointer-events: none;
}

.annotation-label {
  position: absolute;
  top: -22px;
  left: -2px;
  padding: 1px 6px;
  font-size: 11px;
  color: #fff;
  border-radius: 2px 2px 0 0;
  white-space: nowrap;
  line-height: 18px;
}

/* DICOM metadata bar */
.dicom-meta {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 28px;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  padding: 0 12px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  z-index: 3;
}

/* =================== Right Annotation Panel =================== */
.right-panel {
  width: 280px;
  min-width: 280px;
  height: 100vh;
  background: #fff;
  display: flex;
  flex-direction: column;
  border-left: 1px solid #e8e8e8;
  overflow: hidden;
}

.panel-section {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #000;
  margin-bottom: 12px;
}

/* Tag legend */
.tag-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 16px;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #333;
}

.tag-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.add-tag-link {
  font-size: 12px;
  color: #1677ff;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.add-tag-link:hover {
  color: #4096ff;
}

/* Annotations list */
.annotations-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.annotation-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px;
  background: #fafafa;
  border-radius: 4px;
  font-size: 12px;
  color: #333;
}

.ann-label {
  font-weight: 500;
  min-width: 48px;
}

.ann-coords {
  color: #999;
  flex: 1;
}

.ann-delete {
  color: #999;
  cursor: pointer;
  padding: 2px;
  display: flex;
  align-items: center;
}

.ann-delete:hover {
  color: #ff4d4f;
}

/* Bottom buttons */
.panel-footer {
  padding: 16px;
  border-top: 1px solid #f0f0f0;
}
</style>
