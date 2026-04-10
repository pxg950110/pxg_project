<template>
  <div class="clinical-note-view">
    <!-- Search -->
    <div class="note-search">
      <a-input-search
        v-model:value="searchKeyword"
        placeholder="搜索临床笔记关键词..."
        enter-button="搜索"
        allow-clear
        style="max-width: 400px"
        @search="handleSearch"
        @press-enter="handleSearch"
      />
    </div>

    <a-spin :spinning="loading">
      <!-- Notes Collapse -->
      <a-collapse
        v-if="filteredNotes.length > 0"
        v-model:activeKey="activeKeys"
        :bordered="false"
        class="note-collapse"
        expand-icon-position="start"
      >
        <a-collapse-panel
          v-for="note in filteredNotes"
          :key="note.id"
          :header="undefined"
          class="note-panel"
        >
          <template #header>
            <div class="note-header">
              <div class="note-header-left">
                <a-tag :color="noteTypeColorMap[note.note_type] || 'default'" size="small">
                  {{ noteTypeLabelMap[note.note_type] || note.note_type }}
                </a-tag>
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
              <a-divider orientation="left" :style="{ margin: '12px 0 8px', fontSize: '13px', color: 'rgba(0,0,0,0.45)' }">
                附件
              </a-divider>
              <a-space wrap>
                <a-tag
                  v-for="(file, idx) in note.attachments"
                  :key="idx"
                  color="default"
                  class="attachment-tag"
                >
                  <PaperClipOutlined /> {{ file.name }}
                </a-tag>
              </a-space>
            </div>
          </div>
        </a-collapse-panel>
      </a-collapse>

      <a-empty v-else description="暂无临床笔记" />
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { PaperClipOutlined } from '@ant-design/icons-vue'
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

const noteTypeColorMap: Record<string, string> = {
  admission: 'blue',
  progress: 'green',
  discharge: 'purple',
  operative: 'red',
  consultation: 'orange',
  nursing: 'cyan',
  radiology: 'geekblue',
}

const noteTypeLabelMap: Record<string, string> = {
  admission: '入院记录',
  progress: '病程记录',
  discharge: '出院小结',
  operative: '手术记录',
  consultation: '会诊记录',
  nursing: '护理记录',
  radiology: '影像报告',
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
}
.note-panel {
  margin-bottom: 8px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px !important;
  overflow: hidden;
}
.note-panel :deep(.ant-collapse-header) {
  padding: 12px 16px !important;
  align-items: center !important;
}
.note-panel :deep(.ant-collapse-content-box) {
  padding: 0 16px 16px !important;
}
.note-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 12px;
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
  color: rgba(0, 0, 0, 0.85);
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
  color: rgba(0, 0, 0, 0.65);
}
.note-date {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
}
.note-body {
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}
.note-content {
  font-size: 14px;
  line-height: 1.8;
  color: rgba(0, 0, 0, 0.65);
  white-space: pre-line;
  padding: 8px 12px;
  background: #fafafa;
  border-radius: 6px;
}
.note-attachments {
  margin-top: 4px;
}
.attachment-tag {
  cursor: pointer;
}
.attachment-tag:hover {
  color: #1677ff;
  border-color: #1677ff;
}
</style>
