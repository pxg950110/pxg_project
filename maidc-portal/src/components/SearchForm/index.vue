<template>
  <a-form layout="inline" class="search-form" :model="formState" @finish="handleSearch">
    <a-form-item v-for="field in fields" :key="field.name" :label="field.label">
      <!-- text input -->
      <a-input
        v-if="field.type === 'input' || !field.type"
        v-model:value="formState[field.name]"
        :placeholder="field.placeholder || `请输入${field.label}`"
        allow-clear
        @press-enter="handleSearch"
      />
      <!-- select -->
      <a-select
        v-else-if="field.type === 'select'"
        v-model:value="formState[field.name]"
        :placeholder="field.placeholder || `请选择${field.label}`"
        :options="field.options"
        allow-clear
        style="min-width: 160px"
      />
      <!-- date range -->
      <a-range-picker
        v-else-if="field.type === 'dateRange'"
        v-model:value="formState[field.name]"
        :placeholder="['开始日期', '结束日期']"
        style="width: 260px"
      />
    </a-form-item>
    <a-form-item>
      <a-space>
        <a-button type="primary" html-type="submit">查询</a-button>
        <a-button @click="handleReset">重置</a-button>
      </a-space>
    </a-form-item>
  </a-form>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import type { SelectProps } from 'ant-design-vue'

interface FieldDef {
  name: string
  label: string
  type?: 'input' | 'select' | 'dateRange' | string
  options?: SelectProps['options']
  placeholder?: string
}

interface Props {
  fields: FieldDef[]
}

interface Emits {
  (e: 'search', values: Record<string, any>): void
  (e: 'reset'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const formState = reactive<Record<string, any>>({})

// Initialize form state
props.fields.forEach((field) => {
  formState[field.name] = field.type === 'dateRange' ? [] : undefined
})

function handleSearch() {
  const values: Record<string, any> = {}
  props.fields.forEach((field) => {
    const val = formState[field.name]
    if (val !== undefined && val !== null && val !== '' && !(Array.isArray(val) && val.length === 0)) {
      values[field.name] = val
    }
  })
  emit('search', values)
}

function handleReset() {
  props.fields.forEach((field) => {
    formState[field.name] = field.type === 'dateRange' ? [] : undefined
  })
  emit('reset')
}
</script>

<style scoped>
.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 0;
  margin-bottom: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 6px;
}
.search-form :deep(.ant-form-item) {
  margin-bottom: 8px;
}
</style>
