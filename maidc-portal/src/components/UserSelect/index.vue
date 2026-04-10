<template>
  <a-select
    :value="modelValue"
    :placeholder="placeholder || '请选择用户'"
    show-search
    :filter-option="false"
    :loading="fetching"
    allow-clear
    :mode="multiple ? 'multiple' : undefined"
    style="width: 100%"
    @search="handleSearch"
    @change="handleChange"
    @focus="handleFocus"
  >
    <a-select-option v-for="item in options" :key="item.id" :value="String(item.id)">
      <span>{{ item.real_name || item.username }}</span>
      <span v-if="item.real_name" class="option-username">{{ item.username }}</span>
    </a-select-option>
  </a-select>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { getUsers } from '@/api/system'

interface UserItem {
  id: number
  username: string
  real_name?: string
  [key: string]: any
}

interface Props {
  modelValue?: string
  placeholder?: string
  multiple?: boolean
}

interface Emits {
  (e: 'update:modelValue', value: string | string[] | undefined): void
}

const props = withDefaults(defineProps<Props>(), {
  multiple: false,
})
const emit = defineEmits<Emits>()

const options = ref<UserItem[]>([])
const fetching = ref(false)
let searchTimer: ReturnType<typeof setTimeout> | null = null

async function fetchUsers(keyword?: string) {
  fetching.value = true
  try {
    const res = await getUsers({ keyword, page: 1, page_size: 50 })
    options.value = (res.data.data.items as UserItem[]) ?? []
  } catch {
    options.value = []
  } finally {
    fetching.value = false
  }
}

function handleSearch(value: string) {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    fetchUsers(value || undefined)
  }, 300)
}

function handleFocus() {
  if (options.value.length === 0) {
    fetchUsers()
  }
}

function handleChange(value: string | string[] | undefined) {
  emit('update:modelValue', value)
}
</script>

<style scoped>
.option-username {
  margin-left: 8px;
  color: rgba(0, 0, 0, 0.35);
  font-size: 12px;
}
</style>
