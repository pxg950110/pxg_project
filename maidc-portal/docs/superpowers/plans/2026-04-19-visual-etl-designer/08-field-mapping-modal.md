# Task 08: FieldMappingModal - Edge Double-Click Editor

**Files:**
- Create: `src/views/data-etl/components/FieldMappingModal.vue`

- [ ] **Step 1: Create FieldMappingModal.vue**

This modal opens when user double-clicks an edge. It reuses the existing field mapping logic from `EtlPipelineConfig.vue` but adapted for the visual designer context.

Create `src/views/data-etl/components/FieldMappingModal.vue`:

```vue
<template>
  <a-modal
    :open="open"
    title="字段映射配置"
    :width="720"
    @ok="handleSave"
    @cancel="handleCancel"
    destroy-on-close
  >
    <div v-if="edge" style="margin-bottom: 12px">
      <a-space>
        <a-tag color="blue">{{ edge.source }}</a-tag>
        <span>&rarr;</span>
        <a-tag color="green">{{ edge.target }}</a-tag>
      </a-space>
    </div>

    <div style="margin-bottom: 12px">
      <a-space>
        <a-button size="small" @click="handleAutoMap" :loading="autoMapLoading">
          <template #icon><ThunderboltOutlined /></template>
          自动映射
        </a-button>
        <a-button size="small" @click="addRow">
          <template #icon><PlusOutlined /></template>
          添加行
        </a-button>
        <a-popconfirm title="确定清空所有映射？" @confirm="clearRows">
          <a-button size="small" danger>清空</a-button>
        </a-popconfirm>
      </a-space>
    </div>

    <a-table
      :columns="columns"
      :data-source="mappings"
      :pagination="false"
      size="small"
      row-key="_rowKey"
      :scroll="{ y: 320 }"
    >
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.key === 'sourceColumn'">
          <a-select
            v-model:value="record.sourceColumn"
            placeholder="选择源字段"
            style="width: 100%"
            size="small"
            show-search
            :filter-option="filterOption"
          >
            <a-select-option
              v-for="c in sourceColumns"
              :key="c.columnName || c"
              :value="c.columnName || c"
            >
              {{ c.columnName || c }}
              <span v-if="c.dataType" style="color: rgba(0,0,0,0.35); margin-left: 4px; font-size: 12px">
                {{ c.dataType }}
              </span>
            </a-select-option>
          </a-select>
        </template>
        <template v-if="column.key === 'transformType'">
          <a-select
            v-model:value="record.transformType"
            placeholder="转换类型"
            style="width: 100%"
            size="small"
          >
            <a-select-option value="DIRECT">直接映射</a-select-option>
            <a-select-option value="MAP">值映射</a-select-option>
            <a-select-option value="EXPRESSION">表达式</a-select-option>
            <a-select-option value="CONSTANT">常量</a-select-option>
            <a-select-option value="DATE_FMT">日期格式</a-select-option>
          </a-select>
        </template>
        <template v-if="column.key === 'transformRule'">
          <a-input
            v-model:value="record.transformRule"
            :placeholder="rulePlaceholder(record.transformType)"
            size="small"
          />
        </template>
        <template v-if="column.key === 'targetColumn'">
          <a-select
            v-model:value="record.targetColumn"
            placeholder="选择目标字段"
            style="width: 100%"
            size="small"
            show-search
            :filter-option="filterOption"
          >
            <a-select-option
              v-for="c in targetColumns"
              :key="c.columnName || c"
              :value="c.columnName || c"
            >
              {{ c.columnName || c }}
              <span v-if="c.dataType" style="color: rgba(0,0,0,0.35); margin-left: 4px; font-size: 12px">
                {{ c.dataType }}
              </span>
            </a-select-option>
          </a-select>
        </template>
        <template v-if="column.key === 'action'">
          <a-button type="text" size="small" danger @click="removeRow(index)">
            <template #icon><DeleteOutlined /></template>
          </a-button>
        </template>
      </template>
    </a-table>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch, reactive } from 'vue'
import type { GraphEdge } from '@vue-flow/core'
import {
  PlusOutlined,
  DeleteOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons-vue'

const props = defineProps<{
  open: boolean
  edge: GraphEdge | null
  sourceColumns: any[]
  targetColumns: any[]
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  'save': [mappings: any[]]
}>()

let rowKeyCounter = 0
function newRowKey() {
  return `_row_${++rowKeyCounter}`
}

interface MappingRow {
  _rowKey: string
  sourceColumn: string
  targetColumn: string
  transformType: string
  transformRule: string
}

const mappings = reactive<MappingRow[]>([])
const autoMapLoading = ref(false)

const columns = [
  { title: '源字段', key: 'sourceColumn', width: 180 },
  { title: '转换', key: 'transformType', width: 120 },
  { title: '转换规则', key: 'transformRule', width: 180 },
  { title: '目标字段', key: 'targetColumn', width: 180 },
  { title: '操作', key: 'action', width: 60, align: 'center' as const },
]

watch(() => props.open, (val) => {
  if (val && props.edge?.data?.fieldMappings) {
    mappings.length = 0
    props.edge.data.fieldMappings.forEach((fm: any) => {
      mappings.push({
        _rowKey: newRowKey(),
        sourceColumn: fm.sourceColumn || '',
        targetColumn: fm.targetColumn || '',
        transformType: fm.transformType || 'DIRECT',
        transformRule: fm.transformRule || '',
      })
    })
  } else if (val) {
    mappings.length = 0
  }
})

function addRow() {
  mappings.push({
    _rowKey: newRowKey(),
    sourceColumn: '',
    targetColumn: '',
    transformType: 'DIRECT',
    transformRule: '',
  })
}

function removeRow(idx: number) {
  mappings.splice(idx, 1)
}

function clearRows() {
  mappings.length = 0
}

function handleAutoMap() {
  // Auto map by matching column names
  mappings.length = 0
  const sourceNames = props.sourceColumns.map(c => c.columnName || c)
  const targetNames = props.targetColumns.map(c => c.columnName || c)
  const matched = sourceNames.filter(s => targetNames.includes(s))
  matched.forEach(name => {
    mappings.push({
      _rowKey: newRowKey(),
      sourceColumn: name,
      targetColumn: name,
      transformType: 'DIRECT',
      transformRule: '',
    })
  })
}

function handleSave() {
  const result = mappings.map(m => ({
    sourceColumn: m.sourceColumn,
    targetColumn: m.targetColumn,
    transformType: m.transformType,
    transformRule: m.transformRule,
  }))
  emit('save', result)
  emit('update:open', false)
}

function handleCancel() {
  emit('update:open', false)
}

function rulePlaceholder(type: string): string {
  switch (type) {
    case 'EXPRESSION': return '如: source_val * 100'
    case 'CONSTANT': return '如: 2024'
    case 'DATE_FMT': return '如: yyyy-MM-dd → yyyy/MM/dd'
    default: return '转换规则'
  }
}

function filterOption(input: string, option: any) {
  const text = option.children?.[0]?.children || option.value || ''
  return String(text).toLowerCase().includes(input.toLowerCase())
}
</script>
```

- [ ] **Step 2: Commit**

```bash
git add src/views/data-etl/components/FieldMappingModal.vue
git commit -m "feat(etl): add FieldMappingModal for edge field mapping editing"
```
