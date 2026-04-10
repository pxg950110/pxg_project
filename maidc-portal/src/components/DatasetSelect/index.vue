<template>
  <a-select
    :value="modelValue"
    :placeholder="placeholder || '请选择数据集'"
    show-search
    :filter-option="false"
    :loading="fetching"
    allow-clear
    style="width: 100%"
    @search="handleSearch"
    @change="handleChange"
    @focus="handleFocus"
  >
    <a-select-option v-for="item in options" :key="item.id" :value="String(item.id)">
      {{ item.name }}
    </a-select-option>
  </a-select>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { getDatasets } from '@/api/data'

interface DatasetItem {
  id: number
  name: string
  [key: string]: any
}

interface Props {
  modelValue?: string
  placeholder?: string
}

interface Emits {
  (e: 'update:modelValue', value: string | undefined): void
  (e: 'change', dataset: DatasetItem | undefined): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const options = ref<DatasetItem[]>([])
const fetching = ref(false)
let searchTimer: ReturnType<typeof setTimeout> | null = null

async function fetchDatasets(keyword?: string) {
  fetching.value = true
  try {
    const res = await getDatasets({ keyword, page: 1, page_size: 50 })
    options.value = (res.data.data.items as DatasetItem[]) ?? []
  } catch {
    options.value = []
  } finally {
    fetching.value = false
  }
}

function handleSearch(value: string) {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    fetchDatasets(value || undefined)
  }, 300)
}

function handleFocus() {
  if (options.value.length === 0) {
    fetchDatasets()
  }
}

function handleChange(value: string | undefined) {
  emit('update:modelValue', value)
  const selected = options.value.find((d) => String(d.id) === value)
  emit('change', selected)
}
</script>
