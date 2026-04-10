<template>
  <div class="schema-viewer">
    <div class="schema-header">
      <span class="schema-label">{{ mode === 'input' ? '输入 Schema' : '输出 Schema' }}</span>
    </div>
    <a-table
      :columns="columns"
      :data-source="fields"
      :pagination="false"
      size="small"
      bordered
      row-key="name"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'required'">
          <a-tag v-if="record.required" color="red">必填</a-tag>
          <a-tag v-else color="default">选填</a-tag>
        </template>
        <template v-if="column.dataIndex === 'type'">
          <code class="field-type">{{ record.type }}</code>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface SchemaField {
  name: string
  type: string
  description?: string
  required?: boolean
  default?: any
  enum?: string[]
}

interface Props {
  schema: Record<string, any>
  mode?: 'input' | 'output'
}

const props = withDefaults(defineProps<Props>(), {
  mode: 'input',
})

const columns = [
  { title: '字段名', dataIndex: 'name', width: '25%' },
  { title: '类型', dataIndex: 'type', width: '15%' },
  { title: '必填', dataIndex: 'required', width: '10%', align: 'center' as const },
  { title: '默认值', dataIndex: 'default', width: '15%' },
  { title: '说明', dataIndex: 'description', width: '25%' },
]

const fields = computed(() => {
  if (!props.schema) return []
  // Support OpenAPI / JSON Schema style
  const properties = props.schema.properties ?? props.schema.fields ?? {}
  const requiredList: string[] = props.schema.required ?? []

  return Object.entries(properties).map(([name, field]: [string, any]) => ({
    name,
    type: field.type ?? (field.items ? `${field.type || 'array'}<${field.items?.type ?? 'any'}>` : 'any'),
    description: field.description ?? field.title ?? '',
    required: requiredList.includes(name) ?? !!field.required,
    default: field.default ?? '',
    enum: field.enum,
  }))
})
</script>

<style scoped>
.schema-viewer {
  width: 100%;
}
.schema-header {
  margin-bottom: 12px;
}
.schema-label {
  font-size: 14px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.85);
}
.field-type {
  background: #f5f5f5;
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 12px;
  font-family: 'SFMono-Regular', Consolas, monospace;
}
</style>
