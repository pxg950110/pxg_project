<template>
  <div class="etl-palette">
    <div class="etl-palette__header">组件面板</div>

    <div v-for="category in categories" :key="category" class="etl-palette__group">
      <div class="etl-palette__group-title">
        <span class="etl-palette__dot" :style="{ background: CATEGORY_COLORS[category] }" />
        {{ CATEGORY_LABELS[category] }}
      </div>
      <div class="etl-palette__items">
        <div
          v-for="comp in getComponentsByCategory(category)"
          :key="comp.nodeType"
          class="etl-palette__item"
          draggable="true"
          @dragstart="handleDragStart($event, comp.nodeType)"
        >
          <component :is="iconMap[comp.icon]" class="etl-palette__item-icon" />
          <span>{{ comp.label }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
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
  type EtlComponentCategory,
  type EtlNodeType,
  CATEGORY_COLORS,
  CATEGORY_LABELS,
  getComponentsByCategory,
} from '../types/etl-designer'

const emit = defineEmits<{
  'drag-start': [nodeType: EtlNodeType]
}>()

const categories: EtlComponentCategory[] = ['INPUT', 'TRANSFORM', 'PROCESSOR', 'OUTPUT']

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

function handleDragStart(event: DragEvent, nodeType: EtlNodeType) {
  event.dataTransfer?.setData('application/vueflow', nodeType)
  event.dataTransfer!.effectAllowed = 'move'
  emit('drag-start', nodeType)
}
</script>

<style scoped>
.etl-palette { padding: 12px; }
.etl-palette__header {
  font-size: 14px; font-weight: 600; color: rgba(0,0,0,0.85);
  margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px solid #f0f0f0;
}
.etl-palette__group { margin-bottom: 16px; }
.etl-palette__group-title {
  display: flex; align-items: center; gap: 6px;
  font-size: 12px; font-weight: 600; color: rgba(0,0,0,0.65);
  margin-bottom: 8px; text-transform: uppercase; letter-spacing: 0.5px;
}
.etl-palette__dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.etl-palette__items { display: flex; flex-direction: column; gap: 4px; }
.etl-palette__item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 10px; border: 1px solid #f0f0f0; border-radius: 6px;
  cursor: grab; font-size: 13px; transition: all 0.2s;
  background: #fff; user-select: none;
}
.etl-palette__item:hover { border-color: #4096ff; background: #f0f5ff; }
.etl-palette__item:active { cursor: grabbing; box-shadow: 0 2px 8px rgba(0,0,0,0.12); }
.etl-palette__item-icon { font-size: 14px; color: rgba(0,0,0,0.65); }
</style>
