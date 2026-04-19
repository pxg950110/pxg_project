<template>
  <div class="etl-designer">
    <!-- Left: Palette -->
    <div class="etl-designer__palette" :class="{ collapsed: paletteCollapsed }">
      <div class="etl-designer__palette-toggle" @click="paletteCollapsed = !paletteCollapsed">
        <LeftOutlined v-if="!paletteCollapsed" />
        <RightOutlined v-else />
      </div>
      <div v-show="!paletteCollapsed" class="etl-designer__palette-content">
        <EtlPalette @drag-start="onDragStart" />
      </div>
    </div>

    <!-- Center: Canvas -->
    <div class="etl-designer__canvas">
      <EtlCanvas
        :nodes="nodes"
        :edges="edges"
        :node-types="nodeTypes"
        @node-click="onNodeClick"
        @edge-double-click="onEdgeDoubleClick"
        @connect="onConnect"
        @drop="onDrop"
        @dragover="onDragOver"
      />
    </div>

    <!-- Right: Property Panel -->
    <div class="etl-designer__props" :class="{ collapsed: propsCollapsed }">
      <div class="etl-designer__props-toggle" @click="propsCollapsed = !propsCollapsed">
        <RightOutlined v-if="!propsCollapsed" />
        <LeftOutlined v-else />
      </div>
      <div v-show="!propsCollapsed" class="etl-designer__props-content">
        <EtlPropertyPanel
          :selected-node="selectedNode"
          @update:config="onUpdateConfig"
          @update:label="onUpdateLabel"
        />
      </div>
    </div>

    <!-- Field Mapping Modal -->
    <FieldMappingModal
      v-model:open="fieldMappingVisible"
      :edge="selectedEdge"
      :source-columns="edgeSourceColumns"
      :target-columns="edgeTargetColumns"
      @save="onFieldMappingSave"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, markRaw } from 'vue'
import { LeftOutlined, RightOutlined } from '@ant-design/icons-vue'
import EtlPalette from './EtlPalette.vue'
import EtlCanvas from './EtlCanvas.vue'
import EtlPropertyPanel from './EtlPropertyPanel.vue'
import FieldMappingModal from './FieldMappingModal.vue'
import EtlNode from './nodes/EtlNode.vue'
import {
  type EtlNodeData,
  type EtlNodeType,
  createDefaultNodeData,
} from '../types/etl-designer'
import { getEtlColumns } from '@/api/etl'

const nodeTypes = { etlNode: markRaw(EtlNode) }

const props = defineProps<{
  nodes: any[]
  edges: any[]
}>()

const emit = defineEmits<{
  'update:nodes': [nodes: any[]]
  'update:edges': [edges: any[]]
}>()

// Panel state
const paletteCollapsed = ref(false)
const propsCollapsed = ref(false)

// Selection
const selectedNode = ref<any>(null)
const selectedEdge = ref<any>(null)
const fieldMappingVisible = ref(false)
const edgeSourceColumns = ref<any[]>([])
const edgeTargetColumns = ref<any[]>([])

// Drag & Drop
let draggedNodeType: EtlNodeType | null = null

function onDragStart(nodeType: EtlNodeType) {
  draggedNodeType = nodeType
}

function onDragOver(event: DragEvent) {
  event.preventDefault()
  if (event.dataTransfer) event.dataTransfer.dropEffect = 'move'
}

function onDrop({ x, y, nodeType }: { x: number; y: number; nodeType: EtlNodeType }) {
  const data = createDefaultNodeData(nodeType)
  const newNode = {
    id: `node_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`,
    type: 'etlNode',
    position: { x, y },
    data,
  }
  emit('update:nodes', [...props.nodes, newNode])
}

// Canvas events
function onNodeClick({ node }: any) {
  selectedNode.value = node
}

async function onEdgeDoubleClick({ edge }: any) {
  selectedEdge.value = edge
  // Load source/target columns for the edge
  const srcNode = props.nodes.find(n => n.id === edge.source)
  const tgtNode = props.nodes.find(n => n.id === edge.target)
  if (srcNode?.data?.config?.schema && srcNode?.data?.config?.table) {
    try {
      const res = await getEtlColumns(srcNode.data.config.schema, srcNode.data.config.table)
      edgeSourceColumns.value = res.data?.data || []
    } catch { edgeSourceColumns.value = [] }
  }
  if (tgtNode?.data?.config?.schema && tgtNode?.data?.config?.table) {
    try {
      const res = await getEtlColumns(tgtNode.data.config.schema, tgtNode.data.config.table)
      edgeTargetColumns.value = res.data?.data || []
    } catch { edgeTargetColumns.value = [] }
  }
  fieldMappingVisible.value = true
}

function onConnect(params: any) {
  const newEdge = {
    id: `edge_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`,
    source: params.source,
    target: params.target,
    sourceHandle: params.sourceHandle || 'out_1',
    targetHandle: params.targetHandle || 'in_1',
  }
  emit('update:edges', [...props.edges, newEdge])
}

// Property panel
function onUpdateConfig(config: Record<string, any>) {
  if (!selectedNode.value) return
  const nodeId = selectedNode.value.id
  emit('update:nodes', props.nodes.map(n =>
    n.id === nodeId ? { ...n, data: { ...n.data, config: { ...n.data.config, ...config } } } : n,
  ))
}

function onUpdateLabel(label: string) {
  if (!selectedNode.value) return
  const nodeId = selectedNode.value.id
  emit('update:nodes', props.nodes.map(n =>
    n.id === nodeId ? { ...n, data: { ...n.data, label } } : n,
  ))
}

function onFieldMappingSave(mappings: any[]) {
  if (!selectedEdge.value) return
  const edgeId = selectedEdge.value.id
  emit('update:edges', props.edges.map(e =>
    e.id === edgeId ? { ...e, data: { ...e.data, fieldMappings: mappings } } : e,
  ))
  fieldMappingVisible.value = false
}
</script>

<style scoped>
.etl-designer {
  display: flex;
  height: calc(100vh - 220px);
  min-height: 500px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
}
.etl-designer__palette {
  width: 240px; position: relative;
  border-right: 1px solid #e8e8e8; background: #fff;
  transition: width 0.2s; flex-shrink: 0;
}
.etl-designer__palette.collapsed { width: 24px; }
.etl-designer__palette-toggle {
  position: absolute; top: 50%; right: -12px; transform: translateY(-50%);
  width: 24px; height: 48px; background: #fff; border: 1px solid #e8e8e8;
  border-radius: 0 4px 4px 0; display: flex; align-items: center;
  justify-content: center; cursor: pointer; z-index: 10;
}
.etl-designer__palette-content { height: 100%; overflow-y: auto; }
.etl-designer__canvas { flex: 1; min-width: 0; }
.etl-designer__props {
  width: 320px; position: relative;
  border-left: 1px solid #e8e8e8; background: #fff;
  transition: width 0.2s; flex-shrink: 0;
}
.etl-designer__props.collapsed { width: 24px; }
.etl-designer__props-toggle {
  position: absolute; top: 50%; left: -12px; transform: translateY(-50%);
  width: 24px; height: 48px; background: #fff; border: 1px solid #e8e8e8;
  border-radius: 4px 0 0 4px; display: flex; align-items: center;
  justify-content: center; cursor: pointer; z-index: 10;
}
.etl-designer__props-content { height: 100%; overflow-y: auto; }
</style>
