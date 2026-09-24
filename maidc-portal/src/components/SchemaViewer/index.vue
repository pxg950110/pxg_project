<template>
  <div class="schema-viewer">
    <div class="schema-header">
      <span class="schema-label">{{ mode === 'input' ? '输入 Schema' : '输出 Schema' }}</span>
    </div>
    <el-table :data="fields" border size="small" row-key="name">
      <el-table-column label="字段名" prop="name" min-width="150" />
      <el-table-column label="类型" min-width="100">
        <template #default="{ row }">
          <code class="field-type">{{ row.type }}</code>
        </template>
      </el-table-column>
      <el-table-column label="必填" width="70" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.required" type="danger" size="small">必填</el-tag>
          <el-tag v-else type="info" size="small">选填</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="默认值" prop="default" min-width="90" />
      <el-table-column label="说明" prop="description" min-width="150" />
    </el-table>
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
