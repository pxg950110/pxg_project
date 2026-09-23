

// ===== Component Categories =====

export type EtlComponentCategory = 'INPUT' | 'TRANSFORM' | 'PROCESSOR' | 'OUTPUT'

// ===== Component Node Types =====

export type EtlNodeType =
  | 'TABLE_INPUT'
  | 'CSV_INPUT'
  | 'VALUE_MAP'
  | 'EXPRESSION'
  | 'DATE_FMT'
  | 'CONSTANT'
  | 'LOOKUP'
  | 'FILTER'
  | 'JOIN'
  | 'AGGREGATE'
  | 'TABLE_OUTPUT'
  | 'CSV_OUTPUT'

// ===== Category Colors =====

export const CATEGORY_COLORS: Record<EtlComponentCategory, string> = {
  INPUT: '#0ea5e9',
  TRANSFORM: '#f97316',
  PROCESSOR: '#8b5cf6',
  OUTPUT: '#10b981',
}

export const CATEGORY_LABELS: Record<EtlComponentCategory, string> = {
  INPUT: '输入源',
  TRANSFORM: '转换',
  PROCESSOR: '处理',
  OUTPUT: '输出',
}

// ===== Port Definition =====

export interface PortDef {
  id: string
  label: string
}

// ===== Component Definition =====

export interface EtlComponentDef {
  nodeType: EtlNodeType
  category: EtlComponentCategory
  label: string
  icon: string
  inputPorts: PortDef[]
  outputPorts: PortDef[]
  defaultConfig: Record<string, any>
}

// ===== Node Status =====

export type EtlNodeStatus = 'draft' | 'ready' | 'error'

// ===== Node Data (stored in Vue Flow node.data) =====

export interface EtlNodeData {
  label: string
  nodeType: EtlNodeType
  category: EtlComponentCategory
  config: Record<string, any>
  status: EtlNodeStatus
}

// ===== Custom Node Type for Vue Flow =====

// 不直接沿用 @vue-flow/core 的 Node 泛型：其深层条件类型会让 TS 触发
// TS2589 深度实例化错误；此扁平结构与 VueFlow 的 Node 双向结构兼容。
export interface EtlNode {
  id: string
  type: string
  position: { x: number; y: number }
  data: EtlNodeData
  [key: string]: unknown
}
export interface EtlEdgeData {
  fieldMappings?: any[]
  [key: string]: unknown
}

export interface EtlEdge {
  id: string
  source: string
  target: string
  sourceHandle?: string | null
  targetHandle?: string | null
  data?: EtlEdgeData
  [key: string]: unknown
}

// ===== Component Registry =====

export const COMPONENT_REGISTRY: EtlComponentDef[] = [
  {
    nodeType: 'TABLE_INPUT',
    category: 'INPUT',
    label: '表输入',
    icon: 'Coin',
    inputPorts: [],
    outputPorts: [{ id: 'out_1', label: '输出' }],
    defaultConfig: { schema: '', table: '', where: '', columns: [] },
  },
  {
    nodeType: 'CSV_INPUT',
    category: 'INPUT',
    label: 'CSV输入',
    icon: 'Document',
    inputPorts: [],
    outputPorts: [{ id: 'out_1', label: '输出' }],
    defaultConfig: { filePath: '', delimiter: ',', encoding: 'UTF-8', columns: [] },
  },
  {
    nodeType: 'VALUE_MAP',
    category: 'TRANSFORM',
    label: '值映射',
    icon: 'Switch',
    inputPorts: [{ id: 'in_1', label: '输入' }],
    outputPorts: [{ id: 'out_1', label: '输出' }],
    defaultConfig: { mappings: [] },
  },
  {
    nodeType: 'EXPRESSION',
    category: 'TRANSFORM',
    label: '表达式',
    icon: 'Memo',
    inputPorts: [{ id: 'in_1', label: '输入' }],
    outputPorts: [{ id: 'out_1', label: '输出' }],
    defaultConfig: { expressions: [] },
  },
  {
    nodeType: 'DATE_FMT',
    category: 'TRANSFORM',
    label: '日期格式',
    icon: 'Calendar',
    inputPorts: [{ id: 'in_1', label: '输入' }],
    outputPorts: [{ id: 'out_1', label: '输出' }],
    defaultConfig: { conversions: [] },
  },
  {
    nodeType: 'CONSTANT',
    category: 'TRANSFORM',
    label: '常量赋值',
    icon: 'Histogram',
    inputPorts: [{ id: 'in_1', label: '输入' }],
    outputPorts: [{ id: 'out_1', label: '输出' }],
    defaultConfig: { fields: [] },
  },
  {
    nodeType: 'LOOKUP',
    category: 'TRANSFORM',
    label: '字段查找',
    icon: 'Search',
    inputPorts: [{ id: 'in_1', label: '输入' }],
    outputPorts: [{ id: 'out_1', label: '输出' }],
    defaultConfig: { lookupTable: '', matchField: '', returnField: '' },
  },
  {
    nodeType: 'FILTER',
    category: 'PROCESSOR',
    label: '过滤器',
    icon: 'Filter',
    inputPorts: [{ id: 'in_1', label: '输入' }],
    outputPorts: [
      { id: 'out_1', label: '通过' },
      { id: 'reject', label: '拒绝' },
    ],
    defaultConfig: { condition: '' },
  },
  {
    nodeType: 'JOIN',
    category: 'PROCESSOR',
    label: 'JOIN',
    icon: 'Grid',
    inputPorts: [
      { id: 'in_left', label: '左' },
      { id: 'in_right', label: '右' },
    ],
    outputPorts: [{ id: 'out_1', label: '输出' }],
    defaultConfig: { joinType: 'INNER', onCondition: '' },
  },
  {
    nodeType: 'AGGREGATE',
    category: 'PROCESSOR',
    label: '聚合',
    icon: 'Collection',
    inputPorts: [{ id: 'in_1', label: '输入' }],
    outputPorts: [{ id: 'out_1', label: '输出' }],
    defaultConfig: { groupBy: [], aggregations: [] },
  },
  {
    nodeType: 'TABLE_OUTPUT',
    category: 'OUTPUT',
    label: '表输出',
    icon: 'UploadFilled',
    inputPorts: [{ id: 'in_1', label: '输入' }],
    outputPorts: [],
    defaultConfig: { schema: '', table: '', writeMode: 'insert' },
  },
  {
    nodeType: 'CSV_OUTPUT',
    category: 'OUTPUT',
    label: 'CSV输出',
    icon: 'Promotion',
    inputPorts: [{ id: 'in_1', label: '输入' }],
    outputPorts: [],
    defaultConfig: { filePath: '', delimiter: ',', encoding: 'UTF-8' },
  },
]

// ===== Helpers =====

export function getComponentDef(nodeType: EtlNodeType): EtlComponentDef | undefined {
  return COMPONENT_REGISTRY.find(c => c.nodeType === nodeType)
}

export function getComponentsByCategory(category: EtlComponentCategory): EtlComponentDef[] {
  return COMPONENT_REGISTRY.filter(c => c.category === category)
}

export function createDefaultNodeData(nodeType: EtlNodeType): EtlNodeData {
  const def = getComponentDef(nodeType)
  return {
    label: def?.label || nodeType,
    nodeType,
    category: def?.category || 'INPUT',
    config: def ? { ...def.defaultConfig } : {},
    status: 'draft',
  }
}
