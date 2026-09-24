<template>
  <div class="dict-type-panel">
    <el-input v-model="search" placeholder="搜索章节" clearable :suffix-icon="Search" class="mb-3" />
    <div v-loading="loading" class="chapter-list min-h-[200px]">
      <el-menu :default-active="selectedKeys[0] ?? ''" @select="onMenuSelect">
        <el-menu-item v-for="chapter in filteredChapters" :key="chapter.chapterCode || ''" :index="chapter.chapterCode || ''">
          <div class="chapter-item">
            <div class="chapter-code">{{ chapter.chapterCode }}</div>
            <div class="chapter-name">{{ chapter.chapterName }}</div>
          </div>
        </el-menu-item>
      </el-menu>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
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

/** el-menu 选中项变化时同步 selectedKeys（保持原先菜单选中键的状态语义） */
function onMenuSelect(index: string) {
  selectedKeys.value = [index]
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
  background: #fff;
  border: 1px solid #f1f5f9;
  border-radius: 8px;
  padding: 14px;
}

.chapter-list {
  flex: 1;
  overflow-y: auto;
  border-radius: 6px;

  :deep(.el-menu) {
    border-right: none;
    background: transparent;

    .el-menu-item {
      height: auto;
      line-height: normal;
      padding: 8px 12px;
      margin: 4px 0;
      border-radius: 6px;
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
  color: #0ea5e9;
  font-family: 'JetBrains Mono', Consolas, monospace;
}

.chapter-name {
  font-size: 13px;
  color: #0f172a;
}
</style>
