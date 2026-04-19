# MAIDC 可视化数据转换设计器设计

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将 ETL 管道配置页改造为类似 Kettle 的可视化拖拽式数据转换设计器

**Architecture:** 三栏式布局（组件面板 + Vue Flow 画布 + 属性面板），组件拖入画布连线，后端存储图结构并生成 Embulk YAML 执行

**Tech Stack:** @vue-flow/core + Vue 3 + Ant Design Vue 4 + TypeScript

---

## 1. 整体架构

### 1.1 三栏式布局

```
┌─────────────┬──────────────────────────┬──────────────┐
│  组件面板    │      画布 (Vue Flow)      │   属性面板    │
│  (240px)    │      (自适应)             │   (320px)    │
│             │                          │              │
│ 📥 输入源   │   [表输入]──→[转换]──→    │  选中节点    │
│  · 表输入   │              ↓           │  的配置表单  │
│  · CSV输入  │           [过滤]         │              │
│             │              ↓           │  表名/字段   │
│ 🔧 转换     │          [表输出]         │  映射等      │
│  · 值映射   │                          │              │
│  · 表达式   │                          │              │
│  · 日期格式 │                          │              │
│             │                          │              │
│ ⚡ 处理     │                          │              │
│  · 过滤器   │                          │              │
│  · JOIN    │                          │              │
│  · 聚合    │                          │              │
│             │                          │              │
│ 📤 输出    │                          │              │
│  · 表输出   │                          │              │
│  · CSV输出 │                          │              │
└─────────────┴──────────────────────────┴──────────────┘
```

### 1.2 核心概念映射

| Kettle 概念 | MAIDC 概念 | Vue Flow 对应 | 后端存储 |
|---|---|---|---|
| Step | 组件节点 | Node | r_etl_step.transform_config |
| Hop | 数据连线 | Edge | r_etl_edge |
| Transformation | 管道 | 整个画布 | r_etl_pipeline |

### 1.3 数据流

```
前端画布操作 → JSON 序列化(nodes+edges) → PUT /pipelines/{id}/graph
后端存储 → r_etl_step (节点) + r_etl_edge (连线) + r_etl_field_mapping (字段映射)
后端执行 → 拓扑排序 → 生成 Embulk YAML → ProcessBuilder 调用
```

---

## 2. 组件定义

### 2.1 组件类型枚举

```typescript
type EtlComponentCategory = 'INPUT' | 'TRANSFORM' | 'PROCESSOR' | 'OUTPUT'

type EtlNodeType =
  // 输入源
  | 'TABLE_INPUT'    // 数据库表输入
  | 'CSV_INPUT'      // CSV文件输入
  // 转换
  | 'VALUE_MAP'      // 值映射
  | 'EXPRESSION'     // 表达式计算
  | 'DATE_FMT'       // 日期格式化
  | 'CONSTANT'       // 常量赋值
  | 'LOOKUP'         // 字段查找
  // 处理
  | 'FILTER'         // 过滤器
  | 'JOIN'           // JOIN连接
  | 'AGGREGATE'      // 聚合分组
  // 输出
  | 'TABLE_OUTPUT'   // 数据库表输出
  | 'CSV_OUTPUT'     // CSV文件输出
```

### 2.2 组件端口与配置

| 组件 | 输入端口 | 输出端口 | 配置项 |
|---|---|---|---|
| TABLE_INPUT | 无 | out_1 | schema, table, where, columns[] |
| CSV_INPUT | 无 | out_1 | filePath, delimiter, encoding, columns[] |
| VALUE_MAP | in_1 | out_1 | mappings: [{source, target}] |
| EXPRESSION | in_1 | out_1 | expressions: [{field, expr}] |
| DATE_FMT | in_1 | out_1 | conversions: [{field, fromFmt, toFmt}] |
| CONSTANT | in_1 | out_1 | fields: [{name, value}] |
| LOOKUP | in_1 | out_1 | lookupTable, matchField, returnField |
| FILTER | in_1 | out_1(通过) / reject(拒绝) | condition |
| JOIN | in_left + in_right | out_1 | joinType(INNER/LEFT/RIGHT/FULL), on条件 |
| AGGREGATE | in_1 | out_1 | groupBy[], aggregations: [{field, func}] |
| TABLE_OUTPUT | in_1 | 无 | schema, table, writeMode(insert/upsert/truncate) |
| CSV_OUTPUT | in_1 | 无 | filePath, delimiter, encoding |

### 2.3 节点视觉设计

每个自定义节点使用 Ant Design Card 风格：
- **顶部**：分类图标 + 组件名称（可编辑），背景色按分类区分（输入蓝、转换橙、处理紫、输出绿）
- **左侧**：输入端口（小圆点 Handle）
- **右侧**：输出端口（小圆点 Handle）
- **底部**：简要摘要（如 `ods.o3_admissions → 15列`）
- **状态色**：边框灰色=草稿、绿色=已配置、红色=配置缺失
- **选中态**：蓝色边框高亮

---

## 3. 后端数据模型

### 3.1 新增连线表

```sql
CREATE TABLE IF NOT EXISTS cdr.r_etl_edge (
    id              BIGSERIAL PRIMARY KEY,
    pipeline_id     BIGINT NOT NULL,
    source_step_id  BIGINT NOT NULL,
    source_port     VARCHAR(32) DEFAULT 'out_1',
    target_step_id  BIGINT NOT NULL,
    target_port     VARCHAR(32) DEFAULT 'in_1',
    field_mappings  JSONB,            -- 连线上的字段映射配置
    sort_order      INT NOT NULL DEFAULT 0,
    created_by      VARCHAR(64) NOT NULL DEFAULT 'system',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    org_id          BIGINT NOT NULL
);

CREATE INDEX idx_etl_edge_pipeline ON cdr.r_etl_edge(pipeline_id) WHERE NOT is_deleted;
```

### 3.2 transform_config JSONB 结构

`r_etl_step.transform_config` 存储节点配置：

```json
{
  "nodeType": "TABLE_INPUT",
  "position": { "x": 100, "y": 50 },
  "ports": {
    "inputs": [],
    "outputs": ["out_1"]
  },
  "config": {
    "schema": "ods",
    "table": "o3_admissions",
    "columns": ["subject_id", "hadm_id", "admittime"],
    "where": "admittime > '2020-01-01'"
  }
}
```

### 3.3 字段映射归属调整

`r_etl_field_mapping` 增加 `edge_id` 字段，字段映射从挂在 step 上改为挂在 edge 上：

```sql
ALTER TABLE cdr.r_etl_field_mapping ADD COLUMN IF NOT EXISTS edge_id BIGINT;
```

字段映射是连线上的转换关系，双击连线打开映射编辑器。

### 3.4 新增 API

```
GET    /api/v1/cdr/etl/pipelines/{id}/graph      # 加载完整图结构
PUT    /api/v1/cdr/etl/pipelines/{id}/graph      # 保存完整图结构
GET    /api/v1/cdr/etl/pipelines/{id}/preview     # 预览 Embulk YAML
```

**GET /graph 响应格式：**

```json
{
  "nodes": [
    {
      "id": "step_1",
      "type": "tableInput",
      "position": { "x": 100, "y": 50 },
      "data": {
        "label": "ODS Admissions",
        "nodeType": "TABLE_INPUT",
        "config": { "schema": "ods", "table": "o3_admissions" },
        "status": "ready"
      }
    }
  ],
  "edges": [
    {
      "id": "edge_1",
      "source": "step_1",
      "target": "step_2",
      "sourceHandle": "out_1",
      "targetHandle": "in_1",
      "data": { "fieldMappings": [...] }
    }
  ]
}
```

### 3.5 Embulk 配置生成

`EtlConfigGenerator` 改造：
1. 从图结构中找所有无入边的节点（输入源）作为起点
2. 按拓扑排序遍历节点链
3. 根据节点 nodeType 生成对应的 Embulk 配置段：
   - TABLE_INPUT → `in` 插件
   - FILTER → `filters` 下的 `row` 过滤
   - TABLE_OUTPUT → `out` 插件
   - JOIN/AGGREGATE → Embulk 不原生支持，降级为 SQL 预处理

---

## 4. 前端实现架构

### 4.1 文件结构

```
src/views/data-etl/
├── EtlPipelineList.vue              # 管道列表（保留）
├── EtlPipelineConfig.vue            # 重写：可视化设计器入口
├── components/
│   ├── EtlDesigner.vue              # 设计器主组件（三栏布局）
│   ├── EtlPalette.vue               # 左侧组件面板
│   ├── EtlCanvas.vue                # 中间画布（Vue Flow）
│   ├── EtlPropertyPanel.vue         # 右侧属性面板
│   ├── FieldMappingModal.vue        # 字段映射弹窗（复用现有逻辑）
│   └── nodes/                       # 自定义节点组件
│       ├── TableInputNode.vue
│       ├── CsvInputNode.vue
│       ├── TransformNode.vue        # 值映射/表达式/日期/常量/查找通用
│       ├── FilterNode.vue
│       ├── JoinNode.vue
│       ├── AggregateNode.vue
│       └── TableOutputNode.vue
├── composables/
│   ├── useDesignerGraph.ts          # 画布状态管理（nodes/edges CRUD）
│   ├── useNodeConfig.ts             # 各组件配置表单逻辑
│   └── useEmbulkPreview.ts          # Embulk YAML 预览
└── types/
    └── etl-designer.ts              # 节点类型、端口定义、组件枚举
```

### 4.2 交互流程

1. **拖入组件**：从 Palette 拖到画布 → `onDrop` 创建节点 + 默认配置
2. **连线**：拖动端口 Handle → `onConnect` 创建 Edge → 双击连线打开字段映射
3. **编辑属性**：点击节点 → 右侧 PropertyPanel 显示对应配置表单
4. **删除**：选中节点/连线 → Delete 键或右键菜单 → 确认删除
5. **保存**：Ctrl+S 或保存按钮 → 序列化图 → `PUT /graph`
6. **预览**：预览按钮 → `GET /preview` → 展示 Embulk YAML
7. **运行**：运行按钮 → 沿用现有 `POST /pipelines/{id}/run`

### 4.3 Vue Flow 节点注册

```typescript
const nodeTypes: NodeTypes = {
  tableInput: markRaw(TableInputNode),
  csvInput: markRaw(CsvInputNode),
  transform: markRaw(TransformNode),
  filter: markRaw(FilterNode),
  join: markRaw(JoinNode),
  aggregate: markRaw(AggregateNode),
  tableOutput: markRaw(TableOutputNode),
}
```

---

## 5. 实施阶段

### 阶段一：核心设计器

目标：覆盖 "表输入 → 表输出" 的核心流程

- 安装 `@vue-flow/core` + `@vue-flow/background` + `@vue-flow/controls`
- 实现 EtlDesigner 三栏布局
- 实现 EtlPalette 组件面板（拖拽源）
- 实现 EtlCanvas 画布（拖放目标、连线）
- 实现 TableInputNode + TableOutputNode 自定义节点
- 实现 EtlPropertyPanel（表名选择、基础配置）
- 实现 FieldMappingModal（复用现有字段映射逻辑）
- 后端：新增 r_etl_edge 表 + graph API
- 后端：改造 EtlConfigGenerator 支持图结构

### 阶段二：扩展组件

目标：丰富组件类型和处理能力

- CSV 输入/输出节点
- 转换节点（值映射、表达式、日期格式、常量、查找）
- 处理节点（过滤器、JOIN、聚合）
- Embulk YAML 预览面板
- 撤销/重做（Ctrl+Z/Y）
- 画布小地图（MiniMap）
- 节点右键菜单（复制、删除、禁用）
- 画布缩放适应（FitView）
