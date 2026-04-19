<template>
  <div
    class="etl-node"
    :class="[statusClass, { selected: selected }]"
    :style="{ borderColor: categoryColor }"
  >
    <Handle
      v-for="port in inputPorts"
      :key="port.id"
      type="target"
      :position="Position.Left"
      :id="port.id"
      :style="{ top: handleTop(port.id) }"
      class="etl-handle"
    />

    <div class="etl-node-header" :style="{ background: headerBg }">
      <component :is="iconComp" class="etl-node-icon" :style="{ color: categoryColor }" />
      <span class="etl-node-label">{{ data.label }}</span>
    </div>

    <div v-if="summary" class="etl-node-summary">{{ summary }}</div>

    <Handle
      v-for="port in outputPorts"
      :key="port.id"
      type="source"
      :position="Position.Right"
      :id="port.id"
      :style="{ top: handleTop(port.id) }"
      class="etl-handle"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Handle, Position } from '@vue-flow/core'
import {
  DatabaseOutlined,
  FileTextOutlined,
  SwapOutlined,
  CodeOutlined,
  CalendarOutlined,
  NumberOutlined,
  SearchOutlined,
  FilterOutlined,
  MergeCellsOutlined,
  GroupOutlined,
  CloudUploadOutlined,
  ExportOutlined,
} from '@ant-design/icons-vue'
import {
  type EtlNodeData,
  type EtlComponentDef,
  getComponentDef,
  CATEGORY_COLORS,
} from '../../types/etl-designer'

const props = defineProps<{
  id: string
  data: EtlNodeData
  selected?: boolean
}>()

const def = computed<EtlComponentDef | undefined>(() =>
  getComponentDef(props.data.nodeType),
)

const categoryColor = computed(() => CATEGORY_COLORS[props.data.category] || '#d9d9d9')
const headerBg = computed(() => `${categoryColor.value}18`)

const inputPorts = computed(() => def.value?.inputPorts || [])
const outputPorts = computed(() => def.value?.outputPorts || [])

const statusClass = computed(() => `etl-node--${props.data.status || 'draft'}`)

const summary = computed(() => {
  const cfg = props.data.config
  if (!cfg) return ''
  switch (props.data.nodeType) {
    case 'TABLE_INPUT':
    case 'TABLE_OUTPUT':
      return cfg.schema && cfg.table ? `${cfg.schema}.${cfg.table}` : ''
    case 'CSV_INPUT':
      return cfg.filePath || ''
    case 'FILTER':
      return cfg.condition || ''
    case 'JOIN':
      return cfg.joinType || ''
    default:
      return ''
  }
})

const iconMap: Record<string, any> = {
  DatabaseOutlined,
  FileTextOutlined,
  SwapOutlined,
  CodeOutlined,
  CalendarOutlined,
  NumberOutlined,
  SearchOutlined,
  FilterOutlined,
  MergeCellsOutlined,
  GroupOutlined,
  CloudUploadOutlined,
  ExportOutlined,
}

const iconComp = computed(() => {
  const name = def.value?.icon || 'DatabaseOutlined'
  return iconMap[name] || DatabaseOutlined
})

function handleTop(portId: string): string {
  if (portId === 'in_right' || portId === 'reject') return '75%'
  return '50%'
}
</script>

<style scoped>
.etl-node {
  min-width: 160px;
  max-width: 220px;
  background: #fff;
  border: 2px solid #d9d9d9;
  border-radius: 8px;
  font-size: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: relative;
  transition: box-shadow 0.2s;
}

.etl-node:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.etl-node.selected {
  box-shadow: 0 0 0 2px #1890ff, 0 4px 12px rgba(24, 144, 255, 0.2);
}

.etl-node--draft { border-color: #d9d9d9; }
.etl-node--ready { border-color: #52c41a; }
.etl-node--error { border-color: #ff4d4f; }

.etl-node-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border-radius: 6px 6px 0 0;
  font-weight: 500;
}

.etl-node-icon { font-size: 14px; }

.etl-node-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.etl-node-summary {
  padding: 6px 12px 8px;
  color: rgba(0, 0, 0, 0.45);
  font-size: 11px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  border-top: 1px solid #f0f0f0;
}

.etl-handle {
  width: 10px;
  height: 10px;
  background: #fff;
  border: 2px solid #999;
  border-radius: 50%;
}

.etl-handle:hover {
  border-color: #1890ff;
  background: #e6f7ff;
}
</style>
