<template>
  <el-dialog
    v-model="dialogVisible"
    title="字段映射配置"
    width="720px"
    :destroy-on-close="true"
  >
    <div v-if="edge" class="mb-3 flex items-center gap-2">
      <el-tag type="primary">{{ edge.source }}</el-tag>
      <span>&rarr;</span>
      <el-tag type="success">{{ edge.target }}</el-tag>
    </div>

    <div class="mb-3 flex items-center gap-2">
      <el-button size="small" @click="handleAutoMap">
        <el-icon class="mr-1"><MagicStick /></el-icon>
        自动映射
      </el-button>
      <el-button size="small" @click="addRow">
        <el-icon class="mr-1"><Plus /></el-icon>
        添加行
      </el-button>
      <el-popconfirm title="确定清空所有映射？" @confirm="clearRows">
        <template #reference>
          <el-button size="small" type="danger" plain>清空</el-button>
        </template>
      </el-popconfirm>
    </div>

    <el-table :data="mappings" size="small" row-key="_rowKey" max-height="320">
      <el-table-column label="源字段" width="180">
        <template #default="{ row }">
          <el-select v-model="row.sourceColumn" placeholder="选择源字段" style="width: 100%" size="small" filterable>
            <el-option
              v-for="c in sourceColumns"
              :key="c.columnName || c"
              :value="c.columnName || c"
              :label="c.columnName || c"
            >
              {{ c.columnName || c }}
              <span v-if="c.dataType" class="option-type">{{ c.dataType }}</span>
            </el-option>
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="转换" width="120">
        <template #default="{ row }">
          <el-select v-model="row.transformType" placeholder="转换类型" style="width: 100%" size="small">
            <el-option value="DIRECT" label="直接映射" />
            <el-option value="MAP" label="值映射" />
            <el-option value="EXPRESSION" label="表达式" />
            <el-option value="CONSTANT" label="常量" />
            <el-option value="DATE_FMT" label="日期格式" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="转换规则" width="180">
        <template #default="{ row }">
          <el-input v-model="row.transformRule" :placeholder="rulePlaceholder(row.transformType)" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="目标字段" width="180">
        <template #default="{ row }">
          <el-select v-model="row.targetColumn" placeholder="选择目标字段" style="width: 100%" size="small" filterable>
            <el-option
              v-for="c in targetColumns"
              :key="c.columnName || c"
              :value="c.columnName || c"
              :label="c.columnName || c"
            >
              {{ c.columnName || c }}
              <span v-if="c.dataType" class="option-type">{{ c.dataType }}</span>
            </el-option>
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="60" align="center">
        <template #default="{ $index }">
          <el-button type="danger" link size="small" @click="removeRow($index)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" @click="handleSave">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import type { GraphEdge } from '@vue-flow/core'
import { Plus, Delete, MagicStick } from '@element-plus/icons-vue'

const props = defineProps<{
  modelValue: boolean
  edge: GraphEdge | null
  sourceColumns: any[]
  targetColumns: any[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'save': [mappings: any[]]
}>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit('update:modelValue', v),
})

let rowKeyCounter = 0
function newRowKey() { return `_row_${++rowKeyCounter}` }

interface MappingRow { _rowKey: string; sourceColumn: string; targetColumn: string; transformType: string; transformRule: string }
const mappings = reactive<MappingRow[]>([])

watch(() => props.modelValue, (val) => {
  if (val && props.edge?.data?.fieldMappings) {
    mappings.length = 0
    props.edge.data.fieldMappings.forEach((fm: any) => {
      mappings.push({ _rowKey: newRowKey(), sourceColumn: fm.sourceColumn || '', targetColumn: fm.targetColumn || '', transformType: fm.transformType || 'DIRECT', transformRule: fm.transformRule || '' })
    })
  } else if (val) { mappings.length = 0 }
})

function addRow() { mappings.push({ _rowKey: newRowKey(), sourceColumn: '', targetColumn: '', transformType: 'DIRECT', transformRule: '' }) }
function removeRow(idx: number) { mappings.splice(idx, 1) }
function clearRows() { mappings.length = 0 }
function handleAutoMap() {
  mappings.length = 0
  const src = props.sourceColumns.map(c => c.columnName || c)
  const tgt = props.targetColumns.map(c => c.columnName || c)
  src.filter(s => tgt.includes(s)).forEach(name => {
    mappings.push({ _rowKey: newRowKey(), sourceColumn: name, targetColumn: name, transformType: 'DIRECT', transformRule: '' })
  })
}
function handleSave() {
  emit('save', mappings.map(m => ({ sourceColumn: m.sourceColumn, targetColumn: m.targetColumn, transformType: m.transformType, transformRule: m.transformRule })))
  emit('update:modelValue', false)
}
function handleCancel() { emit('update:modelValue', false) }
function rulePlaceholder(type: string): string {
  switch (type) {
    case 'EXPRESSION': return '如: source_val * 100'
    case 'CONSTANT': return '如: 2024'
    case 'DATE_FMT': return '如: yyyy-MM-dd → yyyy/MM/dd'
    default: return '转换规则'
  }
}
</script>

<style scoped>
.option-type {
  color: #94a3b8;
  margin-left: 4px;
  font-size: 12px;
}
</style>
