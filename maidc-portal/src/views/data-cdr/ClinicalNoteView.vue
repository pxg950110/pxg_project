<template>
  <div class="clinical-note-view">
    <!-- Search -->
    <div class="note-search">
      <div class="flex items-center gap-2" style="max-width: 480px">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索临床笔记关键词..."
          clearable
          :prefix-icon="Search"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">搜索</el-button>
      </div>
    </div>

    <div v-loading="loading" class="min-h-[200px]">
      <!-- Notes Collapse -->
      <el-collapse
        v-if="filteredNotes.length > 0"
        v-model="activeKeys"
        class="note-collapse"
      >
        <el-collapse-item
          v-for="note in filteredNotes"
          :key="note.id"
          :name="note.id"
          class="note-panel"
        >
          <template #title>
            <div class="note-header">
              <div class="note-header-left">
                <el-tag
                  :type="noteTypeTagMap[note.note_type] || 'info'"
                  size="small"
                  :style="noteTypeStyleMap[note.note_type]"
                >
                  {{ noteTypeLabelMap[note.note_type] || note.note_type }}
                </el-tag>
                <span class="note-title">{{ note.title }}</span>
              </div>
              <div class="note-header-right">
                <span class="note-author">{{ note.author }}</span>
                <span class="note-date">{{ formatDateTime(note.created_time) }}</span>
              </div>
            </div>
          </template>

          <div class="note-body">
            <div class="note-content" v-html="formatNoteContent(note.content)" />
            <div v-if="note.attachments && note.attachments.length > 0" class="note-attachments">
              <el-divider content-position="left" class="attachments-divider">附件</el-divider>
              <div class="flex flex-wrap gap-2">
                <el-tag
                  v-for="(file, idx) in note.attachments"
                  :key="idx"
                  type="info"
                  class="attachment-tag"
                >
                  <el-icon class="mr-1"><Paperclip /></el-icon>{{ file.name }}
                </el-tag>
              </div>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>

      <el-empty v-else description="暂无临床笔记" :image-size="60" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Search, Paperclip } from '@element-plus/icons-vue'
import { getClinicalNotes } from '@/api/data'
import { formatDateTime } from '@/utils/date'

defineOptions({ name: 'ClinicalNoteView' })

interface Props {
  patientId: string
  encounterId: string
}

const props = defineProps<Props>()

const loading = ref(false)
const notes = ref<any[]>([])
const searchKeyword = ref('')
const activeKeys = ref<string[]>([])

type TagType = 'primary' | 'success' | 'warning' | 'danger' | 'info'

const noteTypeTagMap: Record<string, TagType> = {
  ADMISSION: 'primary',
  INITIAL_PROGRESS: 'success',
  DAILY_PROGRESS: 'success',
  OPERATION: 'danger',
  CONSULTATION: 'warning',
  OTHER: 'info',
  admission: 'primary',
  progress: 'success',
  operative: 'danger',
  consultation: 'warning',
}

const noteTypeStyleMap: Record<string, { color: string; background: string; borderColor: string }> = {
  DISCHARGE: { color: '#8b5cf6', background: '#f5f3ff', borderColor: '#ddd6fe' },
  SHIFT_HANDOVER: { color: '#06b6d4', background: '#ecfeff', borderColor: '#a5f3fc' },
  RESCUE: { color: '#ec4899', background: '#fdf2f8', borderColor: '#fbcfe8' },
  NURSING: { color: '#06b6d4', background: '#ecfeff', borderColor: '#a5f3fc' },
  RADIOLOGY: { color: '#4f46e5', background: '#eef2ff', borderColor: '#c7d2fe' },
  PATHOLOGY: { color: '#f97316', background: '#fff7ed', borderColor: '#fed7aa' },
  ADMISSION_ASSESSMENT: { color: '#f59e0b', background: '#fffbeb', borderColor: '#fde68a' },
  discharge: { color: '#8b5cf6', background: '#f5f3ff', borderColor: '#ddd6fe' },
  nursing: { color: '#06b6d4', background: '#ecfeff', borderColor: '#a5f3fc' },
  radiology: { color: '#4f46e5', background: '#eef2ff', borderColor: '#c7d2fe' },
}

const noteTypeLabelMap: Record<string, string> = {
  ADMISSION: '入院记录',
  INITIAL_PROGRESS: '首次病程',
  DAILY_PROGRESS: '日常病程',
  DISCHARGE: '出院记录',
  OPERATION: '手术记录',
  CONSULTATION: '会诊记录',
  SHIFT_HANDOVER: '交接班记录',
  RESCUE: '抢救记录',
  NURSING: '护理记录',
  RADIOLOGY: '影像报告',
  PATHOLOGY: '病理报告',
  ADMISSION_ASSESSMENT: '入院评估',
  OTHER: '其他',
  admission: '入院记录',
  progress: '病程记录',
  discharge: '出院小结',
  operative: '手术记录',
  consultation: '会诊记录',
  nursing: '护理记录',
  radiology: '影像报告',
}

const signStatusLabelMap: Record<string, { color: string; text: string }> = {
  UNSIGNED: { color: '#94a3b8', text: '未签' },
  SIGNED: { color: '#10b981', text: '已签' },
  COUNTERSIGNED: { color: '#0ea5e9', text: '双签' },
}

const urgencyLabelMap: Record<string, { color: string; text: string }> = {
  NORMAL: { color: '#94a3b8', text: '' },
  URGENT: { color: '#f59e0b', text: '紧急' },
  CRITICAL: { color: '#ef4444', text: '危重' },
}

const filteredNotes = computed(() => {
  if (!searchKeyword.value) return notes.value
  const kw = searchKeyword.value.toLowerCase()
  return notes.value.filter((note: any) =>
    note.title?.toLowerCase().includes(kw) ||
    note.content?.toLowerCase().includes(kw) ||
    note.author?.toLowerCase().includes(kw)
  )
})

function formatNoteContent(content: string): string {
  if (!content) return ''
  // Convert newlines to <br> tags for HTML display
  return content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br />')
}

async function loadData(keyword?: string) {
  loading.value = true
  try {
    const params: { keyword?: string } = {}
    if (keyword) params.keyword = keyword
    const res = await getClinicalNotes(props.patientId, props.encounterId, params)
    notes.value = res.data.data || []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadData(searchKeyword.value)
}

onMounted(() => loadData())
</script>

<style scoped>
.clinical-note-view {
  padding-top: 8px;
}
.note-search {
  margin-bottom: 16px;
}
.note-collapse {
  background: transparent;
  border-top: none;
}
.note-panel {
  margin-bottom: 8px;
  background: #fff;
  border: 1px solid #f1f5f9;
  border-radius: 8px !important;
  overflow: hidden;
}
.note-panel :deep(.el-collapse-item__header) {
  padding: 12px 16px !important;
  height: auto;
  min-height: 48px;
  align-items: center;
  border-bottom: 1px solid #f1f5f9;
}
.note-panel :deep(.el-collapse-item__wrap) {
  border-bottom: none;
}
.note-panel :deep(.el-collapse-item__content) {
  padding: 0 16px 16px !important;
}
.note-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 12px;
  padding-right: 8px;
}
.note-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}
.note-title {
  font-size: 14px;
  font-weight: 500;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.note-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}
.note-author {
  font-size: 13px;
  color: #64748b;
}
.note-date {
  font-size: 13px;
  color: #94a3b8;
}
.note-body {
  padding-top: 8px;
}
.note-content {
  font-size: 14px;
  line-height: 1.8;
  color: #64748b;
  white-space: pre-line;
  padding: 8px 12px;
  background: #f8fafc;
  border-radius: 6px;
}
.note-attachments {
  margin-top: 4px;
}
.attachments-divider {
  margin: 12px 0 8px;
}
.attachments-divider :deep(.el-divider__text) {
  font-size: 13px;
  color: #94a3b8;
}
.attachment-tag {
  cursor: pointer;
}
.attachment-tag:hover {
  color: #0ea5e9;
  border-color: #0ea5e9;
}
</style>
