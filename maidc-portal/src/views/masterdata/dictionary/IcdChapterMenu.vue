<template>
  <div class="dict-type-panel">
    <a-input-search v-model:value="search" placeholder="搜索章节" allow-clear
      style="margin-bottom: 12px" />
    <div class="chapter-list">
      <a-spin :spinning="loading">
        <a-menu v-model:selectedKeys="selectedKeys" mode="inline">
          <a-menu-item v-for="chapter in filteredChapters" :key="chapter.chapterCode || ''">
            <div class="chapter-item">
              <div class="chapter-code">{{ chapter.chapterCode }}</div>
              <div class="chapter-name">{{ chapter.chapterName }}</div>
            </div>
          </a-menu-item>
        </a-menu>
      </a-spin>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getDiagnoses, type Diagnosis } from '@/api/medical-dictionary'

const emit = defineEmits<{
  (e: 'select', chapterCode: string | undefined): void
}>()

const loading = ref(false)
const search = ref('')
const chapters = ref<Diagnosis[]>([])
const selectedKeys = ref<string[]>([])

const filteredChapters = computed(() => {
  if (!search.value) return chapters.value
  const keyword = search.value.toLowerCase()
  return chapters.value.filter(c =>
    c.chapterCode?.toLowerCase().includes(keyword) ||
    c.chapterName?.toLowerCase().includes(keyword),
  )
})

async function fetchChapters() {
  loading.value = true
  try {
    const res = await getDiagnoses({ page_size: 100 })
    const all = res.data.data?.content || []
    const chapterMap = new Map<string, Diagnosis>()
    all.forEach(d => {
      if (d.chapterCode && !chapterMap.has(d.chapterCode)) {
        chapterMap.set(d.chapterCode, d)
      }
    })
    chapters.value = Array.from(chapterMap.values())
  } finally {
    loading.value = false
  }
}

function onSelect(keys: (number | string)[]) {
  emit('select', keys.length ? String(keys[0]) : undefined)
}

function clearSelection() {
  selectedKeys.value = []
}

defineExpose({ clearSelection })

onMounted(fetchChapters)
</script>

<style lang="scss" scoped>
.dict-type-panel {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: var(--tech-panel-bg);
  border: 1px solid var(--tech-panel-border);
  border-radius: 12px;
  padding: 14px;

  :deep(.ant-input-affix-wrapper),
  :deep(.ant-input) {
    background: var(--tech-input-bg) !important;
    border-color: var(--tech-input-border) !important;
    color: var(--tech-text-body) !important;
  }

  :deep(.ant-input::placeholder) {
    color: var(--tech-text-dim);
  }

  :deep(.ant-input-search-button .anticon) {
    color: var(--tech-text-muted);
  }
}

.chapter-list {
  flex: 1;
  overflow-y: auto;
  border-radius: 6px;

  :deep(.ant-menu) {
    background: transparent;
    border-inline-end: none !important;

    .ant-menu-item {
      height: auto;
      line-height: normal;
      padding: 8px 12px;
      margin: 4px 0;
      border-radius: 6px;
      color: var(--tech-text-body);

      &:hover {
        background: rgba(34, 211, 238, 0.08);
        color: var(--tech-primary);
      }

      &-selected {
        background: rgba(34, 211, 238, 0.16);
        color: var(--tech-primary);
      }
    }
  }
}

.chapter-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.chapter-code {
  font-size: 11px;
  color: #38bdf8;
  font-family: 'JetBrains Mono', Consolas, monospace;
}

.chapter-name {
  font-size: 13px;
  color: #f1f5f9;
}
</style>
