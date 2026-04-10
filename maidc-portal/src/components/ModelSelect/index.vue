<template>
  <a-select
    :value="modelValue"
    :placeholder="placeholder || '请选择模型'"
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
      <span>{{ item.model_name }}</span>
      <span class="option-code">{{ item.model_code }}</span>
    </a-select-option>
  </a-select>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { getModels, type ModelVO } from '@/api/model'

interface Props {
  modelValue?: string
  placeholder?: string
}

interface Emits {
  (e: 'update:modelValue', value: string | undefined): void
  (e: 'change', model: ModelVO | undefined): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const options = ref<ModelVO[]>([])
const fetching = ref(false)
let searchTimer: ReturnType<typeof setTimeout> | null = null

async function fetchModels(keyword?: string) {
  fetching.value = true
  try {
    const res = await getModels({ keyword, page: 1, page_size: 50 })
    options.value = res.data.data.items
  } catch {
    options.value = []
  } finally {
    fetching.value = false
  }
}

function handleSearch(value: string) {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    fetchModels(value || undefined)
  }, 300)
}

function handleFocus() {
  if (options.value.length === 0) {
    fetchModels()
  }
}

function handleChange(value: string | undefined) {
  emit('update:modelValue', value)
  const selected = options.value.find((m) => String(m.id) === value)
  emit('change', selected)
}
</script>

<style scoped>
.option-code {
  margin-left: 8px;
  color: rgba(0, 0, 0, 0.35);
  font-size: 12px;
}
</style>
