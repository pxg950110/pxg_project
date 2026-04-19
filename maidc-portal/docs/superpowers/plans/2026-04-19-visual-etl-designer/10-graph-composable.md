# Task 10: useDesignerGraph Composable

**Files:**
- Create: `src/views/data-etl/composables/useDesignerGraph.ts`

- [ ] **Step 1: Create composables directory**

```bash
mkdir -p src/views/data-etl/composables
```

- [ ] **Step 2: Create useDesignerGraph.ts**

This composable manages the graph state (nodes + edges) and provides CRUD operations.

Create `src/views/data-etl/composables/useDesignerGraph.ts`:

```typescript
import { ref, computed } from 'vue'
import type { Connection } from '@vue-flow/core'
import {
  type EtlNodeData,
  type EtlNodeType,
  type EtlNode,
  type EtlEdge,
  createDefaultNodeData,
} from '../types/etl-designer'

export function useDesignerGraph() {
  const nodes = ref<EtlNode[]>([])
  const edges = ref<EtlEdge[]>([])

  const selectedNodeId = ref<string | null>(null)

  const selectedNode = computed(() =>
    nodes.value.find(n => n.id === selectedNodeId.value) || null,
  )

  // ===== Node Operations =====

  function addNode(nodeType: EtlNodeType, position: { x: number; y: number }): EtlNode {
    const data = createDefaultNodeData(nodeType)
    const node: EtlNode = {
      id: `node_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`,
      type: 'etlNode',
      position,
      data,
    }
    nodes.value = [...nodes.value, node]
    return node
  }

  function removeNode(nodeId: string) {
    nodes.value = nodes.value.filter(n => n.id !== nodeId)
    // Also remove connected edges
    edges.value = edges.value.filter(
      e => e.source !== nodeId && e.target !== nodeId,
    )
    if (selectedNodeId.value === nodeId) {
      selectedNodeId.value = null
    }
  }

  function updateNodeConfig(nodeId: string, config: Record<string, any>) {
    nodes.value = nodes.value.map(n =>
      n.id === nodeId
        ? { ...n, data: { ...n.data, config: { ...n.data.config, ...config } } }
        : n,
    )
  }

  function updateNodeLabel(nodeId: string, label: string) {
    nodes.value = nodes.value.map(n =>
      n.id === nodeId
        ? { ...n, data: { ...n.data, label } }
        : n,
    )
  }

  function updateNodePosition(nodeId: string, position: { x: number; y: number }) {
    nodes.value = nodes.value.map(n =>
      n.id === nodeId ? { ...n, position } : n,
    )
  }

  function selectNode(nodeId: string | null) {
    selectedNodeId.value = nodeId
  }

  // ===== Edge Operations =====

  function addEdge(connection: Connection) {
    const edge: EtlEdge = {
      id: `edge_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`,
      source: connection.source,
      target: connection.target,
      sourceHandle: connection.sourceHandle || 'out_1',
      targetHandle: connection.targetHandle || 'in_1',
    }

    // Prevent duplicate edges
    const exists = edges.value.some(
      e => e.source === edge.source &&
           e.target === edge.target &&
           e.sourceHandle === edge.sourceHandle &&
           e.targetHandle === edge.targetHandle,
    )
    if (!exists) {
      edges.value = [...edges.value, edge]
    }
  }

  function removeEdge(edgeId: string) {
    edges.value = edges.value.filter(e => e.id !== edgeId)
  }

  function updateEdgeFieldMappings(edgeId: string, mappings: any[]) {
    edges.value = edges.value.map(e =>
      e.id === edgeId
        ? { ...e, data: { ...e.data, fieldMappings: mappings } }
        : e,
    )
  }

  // ===== Validation =====

  function getNodeStatus(nodeId: string): 'draft' | 'ready' | 'error' {
    const node = nodes.value.find(n => n.id === nodeId)
    if (!node) return 'error'

    const cfg = node.data.config
    switch (node.data.nodeType) {
      case 'TABLE_INPUT':
      case 'TABLE_OUTPUT':
        return cfg.schema && cfg.table ? 'ready' : 'draft'
      case 'CSV_INPUT':
      case 'CSV_OUTPUT':
        return cfg.filePath ? 'ready' : 'draft'
      case 'FILTER':
        return cfg.condition ? 'ready' : 'draft'
      default:
        return 'draft'
    }
  }

  function refreshAllNodeStatuses() {
    nodes.value = nodes.value.map(n => ({
      ...n,
      data: { ...n.data, status: getNodeStatus(n.id) },
    }))
  }

  // ===== Serialization =====

  function serialize(): { nodes: any[]; edges: any[] } {
    return {
      nodes: nodes.value.map(n => ({
        id: n.id,
        type: n.type,
        position: n.position,
        data: n.data,
      })),
      edges: edges.value.map(e => ({
        id: e.id,
        source: e.source,
        target: e.target,
        sourceHandle: e.sourceHandle,
        targetHandle: e.targetHandle,
        data: e.data,
      })),
    }
  }

  function deserialize(graph: { nodes: any[]; edges: any[] }) {
    nodes.value = graph.nodes || []
    edges.value = graph.edges || []
  }

  function clear() {
    nodes.value = []
    edges.value = []
    selectedNodeId.value = null
  }

  return {
    nodes,
    edges,
    selectedNodeId,
    selectedNode,
    addNode,
    removeNode,
    updateNodeConfig,
    updateNodeLabel,
    updateNodePosition,
    selectNode,
    addEdge,
    removeEdge,
    updateEdgeFieldMappings,
    getNodeStatus,
    refreshAllNodeStatuses,
    serialize,
    deserialize,
    clear,
  }
}
```

- [ ] **Step 3: Verify TypeScript compiles**

```bash
cd E:/pxg_project/maidc-portal
npx vue-tsc --noEmit 2>&1 | grep -i "useDesignerGraph" || echo "No errors"
```

Expected: No errors for useDesignerGraph.ts

- [ ] **Step 4: Commit**

```bash
git add src/views/data-etl/composables/useDesignerGraph.ts
git commit -m "feat(etl): add useDesignerGraph composable for graph state management"
```
