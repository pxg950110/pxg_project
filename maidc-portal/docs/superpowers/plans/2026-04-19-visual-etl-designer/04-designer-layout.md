# Task 04: EtlDesigner Three-Panel Layout Shell

**Files:**
- Create: `src/views/data-etl/components/EtlDesigner.vue`

- [ ] **Step 1: Create EtlDesigner.vue**

This is the main shell component that holds the three panels. Each panel will be implemented in subsequent tasks. For now, create the layout with placeholder slots.

Create `src/views/data-etl/components/EtlDesigner.vue`:

```vue
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
        @nodes-change="onNodesChange"
        @edges-change="onEdgesChange"
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
        />
      </div>
    </div>

    <!-- Field Mapping Modal -->
    <FieldMappingModal
      v-model:open="fieldMappingVisible"
      :edge="selectedEdge"
      :source-columns="sourceColumns"
      :target-columns="targetColumns"
      @save="onFieldMappingSave"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, markRaw } from 'vue'
import { LeftOutlined, RightOutlined } from '@ant-design/icons-vue'
import type { GraphNode, GraphEdge } from '@vue-flow/core'
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

const nodeTypes = {
  etlNode: markRaw(EtlNode),
}

// ===== Props / Emits =====
const props = defineProps<{
  nodes: any[]
  edges: any[]
}>()

const emit = defineEmits<{
  'update:nodes': [nodes: any[]]
  'update:edges': [edges: any[]]
  'save': []
}>()

// ===== Panel State =====
const paletteCollapsed = ref(false)
const propsCollapsed = ref(false)

// ===== Selection =====
const selectedNode = ref<GraphNode<EtlNodeData> | null>(null)
const selectedEdge = ref<GraphEdge | null>(null)
const fieldMappingVisible = ref(false)
const sourceColumns = ref<any[]>([])
const targetColumns = ref<any[]>([])

// ===== Drag & Drop =====
let draggedNodeType: EtlNodeType | null = null

function onDragStart(nodeType: EtlNodeType) {
  draggedNodeType = nodeType
}

function onDragOver(event: DragEvent) {
  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
}

function onDrop({ x, y }: { x: number; y: number }) {
  if (!draggedNodeType) return
  const nodeData = createDefaultNodeData(draggedNodeType)
  const newNode = {
    id: `node_${Date.now()}`,
    type: 'etlNode',
    position: { x, y },
    data: nodeData,
  }
  emit('update:nodes', [...props.nodes, newNode])
  draggedNodeType = null
}

// ===== Canvas Events =====
function onNodeClick({ node }: { node: GraphNode<EtlNodeData> }) {
  selectedNode.value = node
}

function onEdgeDoubleClick({ edge }: { edge: GraphEdge }) {
  selectedEdge.value = edge
  fieldMappingVisible.value = true
}

function onConnect(params: { source: string; target: string; sourceHandle?: string; targetHandle?: string }) {
  const newEdge = {
    id: `edge_${Date.now()}`,
    source: params.source,
    target: params.target,
    sourceHandle: params.sourceHandle || 'out_1',
    targetHandle: params.targetHandle || 'in_1',
  }
  emit('update:edges', [...props.edges, newEdge])
}

function onNodesChange(changes: any) {
  // Delegate to parent via v-model pattern
}

function onEdgesChange(changes: any) {
  // Delegate to parent via v-model pattern
}

// ===== Property Panel =====
function onUpdateConfig(config: Record<string, any>) {
  if (!selectedNode.value) return
  const node = selectedNode.value
  const updatedNodes = props.nodes.map(n =>
    n.id === node.id
      ? { ...n, data: { ...n.data, config } }
      : n,
  )
  emit('update:nodes', updatedNodes)
}

function onFieldMappingSave(mappings: any[]) {
  if (!selectedEdge.value) return
  const edge = selectedEdge.value
  const updatedEdges = props.edges.map(e =>
    e.id === edge.id
      ? { ...e, data: { ...e.data, fieldMappings: mappings } }
      : e,
  )
  emit('update:edges', updatedEdges)
  fieldMappingVisible.value = false
}
</script>

<style scoped>
.etl-designer {
  display: flex;
  height: calc(100vh - 200px);
  min-height: 500px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
}

.etl-designer__palette {
  width: 240px;
  position: relative;
  border-right: 1px solid #e8e8e8;
  background: #fff;
  transition: width 0.2s;
  flex-shrink: 0;
}

.etl-designer__palette.collapsed {
  width: 24px;
}

.etl-designer__palette-toggle {
  position: absolute;
  top: 50%;
  right: -12px;
  transform: translateY(-50%);
  width: 24px;
  height: 48px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 0 4px 4px 0;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 10;
}

.etl-designer__palette-content {
  height: 100%;
  overflow-y: auto;
}

.etl-designer__canvas {
  flex: 1;
  min-width: 0;
}

.etl-designer__props {
  width: 320px;
  position: relative;
  border-left: 1px solid #e8e8e8;
  background: #fff;
  transition: width 0.2s;
  flex-shrink: 0;
}

.etl-designer__props.collapsed {
  width: 24px;
}

.etl-designer__props-toggle {
  position: absolute;
  top: 50%;
  left: -12px;
  transform: translateY(-50%);
  width: 24px;
  height: 48px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 4px 0 0 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 10;
}

.etl-designer__props-content {
  height: 100%;
  overflow-y: auto;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add src/views/data-etl/components/EtlDesigner.vue
git commit -m "feat(etl): add EtlDesigner three-panel layout shell"
```
