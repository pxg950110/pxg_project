<template>
  <el-form :inline="true" class="search-form !rounded-xl !border !border-slate-200/80 !p-4 !bg-white !mb-4 shadow-clinical-sm" :model="formState" @submit.prevent="handleSearch">
    <el-form-item v-for="field in fields" :key="field.name" :label="field.label" class="!mb-2">
      <!-- text input -->
      <el-input
        v-if="field.type === 'input' || !field.type"
        v-model="formState[field.name]"
        :placeholder="field.placeholder || `请输入${field.label}`"
        clearable
        @keyup.enter="handleSearch"
      />
      <!-- select -->
      <el-select
        v-else-if="field.type === 'select'"
        v-model="formState[field.name]"
        :placeholder="field.placeholder || `请选择${field.label}`"
        clearable
        class="min-w-[160px]"
      >
        <el-option
          v-for="opt in (field.options || [])"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <!-- date range -->
      <el-date-picker
        v-else-if="field.type === 'dateRange'"
        v-model="formState[field.name]"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        class="!w-[260px]"
      />
    </el-form-item>
    <el-form-item class="!mb-2">
      <div class="flex items-center gap-2">
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { reactive } from 'vue'

interface SelectOption {
  label: string
  value: string | number
}

interface FieldDef {
  name: string
  label: string
  type?: 'input' | 'select' | 'dateRange' | string
  options?: SelectOption[]
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
  gap: 8px;
}
</style>
