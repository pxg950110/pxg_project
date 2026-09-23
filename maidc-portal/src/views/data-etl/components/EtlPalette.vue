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
          <el-icon :size="14" class="etl-palette__item-icon"><component :is="iconMap[comp.icon]" /></el-icon>
          <span>{{ comp.label }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  Coin,
  Document,
  Switch,
  Memo,
  Calendar,
  Histogram,
  Search,
  Filter,
  Grid,
  Collection,
  UploadFilled,
  Promotion,
} from '@element-plus/icons-vue'
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
  Coin,
  Document,
  Switch,
  Memo,
  Calendar,
  Histogram,
  Search,
  Filter,
  Grid,
  Collection,
  UploadFilled,
  Promotion,
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
  font-size: 14px; font-weight: 600; color: #0f172a;
  margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px solid #f1f5f9;
}
.etl-palette__group { margin-bottom: 16px; }
.etl-palette__group-title {
  display: flex; align-items: center; gap: 6px;
  font-size: 12px; font-weight: 600; color: #64748b;
  margin-bottom: 8px; text-transform: uppercase; letter-spacing: 0.5px;
}
.etl-palette__dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.etl-palette__items { display: flex; flex-direction: column; gap: 4px; }
.etl-palette__item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 10px; border: 1px solid #f1f5f9; border-radius: 6px;
  cursor: grab; font-size: 13px; transition: all 0.2s;
  background: #fff; user-select: none;
}
.etl-palette__item:hover { border-color: #38bdf8; background: #f0f9ff; }
.etl-palette__item:active { cursor: grabbing; box-shadow: 0 2px 8px rgba(15, 23, 42, 0.12); }
.etl-palette__item-icon { color: #64748b; }
</style>
