import type { Node, Edge } from '@vue-flow/core'

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
  INPUT: '#1890ff',
  TRANSFORM: '#fa8c16',
  PROCESSOR: '#722ed1',
  OUTPUT: '#52c41a',
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

export type EtlNode = Node<EtlNodeData>
export type EtlEdge = Edge

// ===== Component Registry =====

export const COMPONENT_REGISTRY: EtlComponentDef[] = [
  {
    nodeType: 'TABLE_INPUT',
    category: 'INPUT',
    label: '表输入',
    icon: 'DatabaseOutlined',
    inputPorts: [],
    outputPorts: [{ id: 'out_1', label: '输出' }],
    defaultConfig: { schema: '', table: '', where: '', columns: [] },
  },
  {
    nodeType: 'CSV_INPUT',
    category: 'INPUT',
    label: 'CSV输入',
    icon: 'FileTextOutlined',
    inputPorts: [],
    outputPorts: [{ id: 'out_1', label: '输出' }],
    defaultConfig: { filePath: '', delimiter: ',', encoding: 'UTF-8', columns: [] },
  },
  {
    nodeType: 'VALUE_MAP',
    category: 'TRANSFORM',
    label: '值映射',
    icon: 'SwapOutlined',
    inputPorts: [{ id: 'in_1', label: '输入' }],
    outputPorts: [{ id: 'out_1', label: '输出' }],
    defaultConfig: { mappings: [] },
  },
  {
    nodeType: 'EXPRESSION',
    category: 'TRANSFORM',
    label: '表达式',
    icon: 'CodeOutlined',
    inputPorts: [{ id: 'in_1', label: '输入' }],
    outputPorts: [{ id: 'out_1', label: '输出' }],
    defaultConfig: { expressions: [] },
  },
  {
    nodeType: 'DATE_FMT',
    category: 'TRANSFORM',
    label: '日期格式',
    icon: 'CalendarOutlined',
    inputPorts: [{ id: 'in_1', label: '输入' }],
    outputPorts: [{ id: 'out_1', label: '输出' }],
    defaultConfig: { conversions: [] },
  },
  {
    nodeType: 'CONSTANT',
    category: 'TRANSFORM',
    label: '常量赋值',
    icon: 'NumberOutlined',
    inputPorts: [{ id: 'in_1', label: '输入' }],
    outputPorts: [{ id: 'out_1', label: '输出' }],
    defaultConfig: { fields: [] },
  },
  {
    nodeType: 'LOOKUP',
    category: 'TRANSFORM',
    label: '字段查找',
    icon: 'SearchOutlined',
    inputPorts: [{ id: 'in_1', label: '输入' }],
    outputPorts: [{ id: 'out_1', label: '输出' }],
    defaultConfig: { lookupTable: '', matchField: '', returnField: '' },
  },
  {
    nodeType: 'FILTER',
    category: 'PROCESSOR',
    label: '过滤器',
    icon: 'FilterOutlined',
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
    icon: 'MergeCellsOutlined',
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
    icon: 'GroupOutlined',
    inputPorts: [{ id: 'in_1', label: '输入' }],
    outputPorts: [{ id: 'out_1', label: '输出' }],
    defaultConfig: { groupBy: [], aggregations: [] },
  },
  {
    nodeType: 'TABLE_OUTPUT',
    category: 'OUTPUT',
    label: '表输出',
    icon: 'CloudUploadOutlined',
    inputPorts: [{ id: 'in_1', label: '输入' }],
    outputPorts: [],
    defaultConfig: { schema: '', table: '', writeMode: 'insert' },
  },
  {
    nodeType: 'CSV_OUTPUT',
    category: 'OUTPUT',
    label: 'CSV输出',
    icon: 'ExportOutlined',
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
