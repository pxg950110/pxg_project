<template>
  <div class="etl-canvas" ref="canvasRef">
    <VueFlow
      v-model:nodes="internalNodes"
      v-model:edges="internalEdges"
      :node-types="nodeTypes"
      :default-viewport="{ zoom: 1, x: 50, y: 50 }"
      :min-zoom="0.2"
      :max-zoom="2"
      :snap-to-grid="true"
      :snap-grid="[16, 16]"
      fit-view-on-init
      :delete-key-code="'Delete'"
      @node-click="onNodeClick"
      @edge-double-click="onEdgeDoubleClick"
      @connect="onConnect"
      @dragover="onDragOver"
      @drop="onDrop"
      @nodes-change="onNodesChange"
      @edges-change="onEdgesChange"
      @pane-click="onPaneClick"
    >
      <Background :gap="16" />
      <Controls position="bottom-left" />
      <MiniMap position="bottom-right" :pannable="true" :zoomable="true" />
    </VueFlow>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { VueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { MiniMap } from '@vue-flow/minimap'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'
import '@vue-flow/minimap/dist/style.css'
import type { Connection, NodeChange, EdgeChange } from '@vue-flow/core'
import type { EtlNodeData, EtlNodeType } from '../types/etl-designer'

const props = defineProps<{
  nodes: any[]
  edges: any[]
  nodeTypes: Record<string, any>
}>()

const emit = defineEmits<{
  'node-click': [payload: any]
  'edge-double-click': [payload: any]
  'connect': [params: Connection]
  'drop': [payload: { x: number; y: number; nodeType: EtlNodeType }]
  'nodes-change': [changes: NodeChange[]]
  'edges-change': [changes: EdgeChange[]]
}>()

const canvasRef = ref<HTMLDivElement>()
const internalNodes = ref([...props.nodes])
const internalEdges = ref([...props.edges])

watch(() => props.nodes, (val) => { internalNodes.value = [...val] }, { deep: true })
watch(() => props.edges, (val) => { internalEdges.value = [...val] }, { deep: true })

function onNodeClick(payload: any) { emit('node-click', payload) }
function onEdgeDoubleClick(payload: any) { emit('edge-double-click', payload) }
function onConnect(params: Connection) { emit('connect', params) }
function onNodesChange(changes: NodeChange[]) { emit('nodes-change', changes) }
function onEdgesChange(changes: EdgeChange[]) { emit('edges-change', changes) }
function onPaneClick() { /* deselect handled by parent */ }

function onDragOver(event: DragEvent) {
  event.preventDefault()
  if (event.dataTransfer) event.dataTransfer.dropEffect = 'move'
}

function onDrop(event: DragEvent) {
  const nodeType = event.dataTransfer?.getData('application/vueflow') as EtlNodeType
  if (!nodeType) return
  const bounds = canvasRef.value?.getBoundingClientRect()
  if (!bounds) return
  emit('drop', {
    x: event.clientX - bounds.left - 50,
    y: event.clientY - bounds.top - 20,
    nodeType,
  })
}
</script>

<style scoped>
.etl-canvas { width: 100%; height: 100%; }
.etl-canvas :deep(.vue-flow) { background: #fafafa; }
.etl-canvas :deep(.vue-flow__minimap) { border-radius: 8px; overflow: hidden; }
</style>
