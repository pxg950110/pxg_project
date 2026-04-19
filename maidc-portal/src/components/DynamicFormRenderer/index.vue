<template>
  <template v-for="field in schema.fields" :key="field.key">
    <a-form-item v-if="field.type === 'text'" :label="field.label">
      <a-input v-model:value="params[field.key]" :placeholder="field.placeholder || `请输入${field.label}`" />
    </a-form-item>

    <a-form-item v-else-if="field.type === 'password'" :label="field.label">
      <a-input-password v-model:value="params[field.key]" :placeholder="field.placeholder || `请输入${field.label}`" />
    </a-form-item>

    <a-form-item v-else-if="field.type === 'number'" :label="field.label">
      <a-input-number v-model:value="params[field.key]" :min="field.min" :max="field.max"
        :placeholder="field.placeholder || `请输入${field.label}`" style="width: 100%" />
    </a-form-item>

    <a-form-item v-else-if="field.type === 'select'" :label="field.label">
      <a-select v-model:value="params[field.key]" :placeholder="`请选择${field.label}`">
        <a-select-option v-for="opt in field.options" :key="opt" :value="opt">{{ opt }}</a-select-option>
      </a-select>
    </a-form-item>

    <a-form-item v-else-if="field.type === 'textarea'" :label="field.label">
      <a-textarea v-model:value="params[field.key]" :rows="3" :placeholder="field.placeholder" />
    </a-form-item>

    <a-form-item v-else-if="field.type === 'keyvalue'" :label="field.label">
      <div v-for="(kv, idx) in getKeyValuePairs(field.key)" :key="idx" style="display: flex; gap: 8px; margin-bottom: 4px;">
        <a-input v-model:value="kv.key" placeholder="Key" style="flex: 1" @change="syncKeyValue(field.key)" />
        <a-input v-model:value="kv.value" placeholder="Value" style="flex: 1" @change="syncKeyValue(field.key)" />
        <a-button type="text" danger size="small" @click="removeKeyValue(field.key, idx)">删除</a-button>
      </div>
      <a-button type="dashed" size="small" @click="addKeyValue(field.key)">+ 添加</a-button>
    </a-form-item>
  </template>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'

interface FieldDef {
  key: string
  label: string
  type: string
  required?: boolean
  placeholder?: string
  default?: any
  min?: number
  max?: number
  options?: string[]
}

interface Schema {
  fields: FieldDef[]
}

const props = defineProps<{
  schema: Schema
  modelValue: Record<string, any>
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: Record<string, any>): void
}>()

const params = reactive<Record<string, any>>({})

const kvStore = reactive<Record<string, Array<{ key: string; value: string }>>>({})

watch(() => props.schema, (schema) => {
  if (!schema?.fields) return
  Object.keys(params).forEach(k => delete params[k])
  schema.fields.forEach(field => {
    if (field.default !== undefined) {
      params[field.key] = field.default
    }
    if (field.type === 'keyvalue') {
      kvStore[field.key] = []
    }
  })
  emit('update:modelValue', { ...params })
}, { immediate: true })

watch(params, () => {
  emit('update:modelValue', { ...params })
}, { deep: true })

watch(() => props.modelValue, (val) => {
  if (val) {
    Object.keys(val).forEach(k => {
      if (params[k] === undefined) params[k] = val[k]
    })
  }
}, { immediate: true })

function getKeyValuePairs(fieldKey: string) {
  if (!kvStore[fieldKey]) kvStore[fieldKey] = []
  return kvStore[fieldKey]
}

function addKeyValue(fieldKey: string) {
  if (!kvStore[fieldKey]) kvStore[fieldKey] = []
  kvStore[fieldKey].push({ key: '', value: '' })
  syncKeyValue(fieldKey)
}

function removeKeyValue(fieldKey: string, idx: number) {
  kvStore[fieldKey]?.splice(idx, 1)
  syncKeyValue(fieldKey)
}

function syncKeyValue(fieldKey: string) {
  const pairs = kvStore[fieldKey] || []
  const obj: Record<string, string> = {}
  pairs.forEach(kv => {
    if (kv.key) obj[kv.key] = kv.value
  })
  params[fieldKey] = Object.keys(obj).length > 0 ? obj : undefined
  emit('update:modelValue', { ...params })
}
</script>
