# Sub-project 2: Model Management (Full Chain) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Align all 13 model management pages with the .pen design for pixel-perfect accuracy.

**Architecture:** Vue 3 SFC components using Ant Design Vue. Major change: several pages switch from table-based to card-based layout as per design.

**Tech Stack:** Vue 3 + TypeScript + Ant Design Vue + ECharts

---

## Gap Summary

| Page | Design | Code | Gap |
|------|--------|------|-----|
| 03-模型列表 | Card grid (3col) + category tabs + sort | Table + SearchForm | **MAJOR** |
| 04-模型详情 | Rich detail with metrics + tabs + versions + evals + deployments | Basic descriptions + 2 tabs | **MAJOR** |
| 05-部署监控 | Dashboard: metrics + chart + status panel + alerts | Simple table | **MAJOR** |
| 06-版本管理 | Table + comparison with hyperparams/training/eval metrics | Basic table + compare modal | **MODERATE** |
| 07-模型评估 | Card list with metrics, progress, confusion matrix | Simple table | **MAJOR** |
| 08-审批流程 | Tab filter (待审批/已审批/全部) + badge count | Simple table | **MODERATE** |
| 09-流量路由 | Card config with traffic bars + JSON preview + detail section | Simple table | **MAJOR** |
| 推理日志 | (Not exported yet, assume similar to code) | Table + drawer | **MINOR** |
| Modals (13个) | Various modal designs | Various modals | **MODERATE** |

---

## Task 1: Model List — Card Grid Redesign

**Files:**
- Rewrite: `maidc-portal/src/views/model/ModelList.vue`

**Design spec:**
- **Layout**: 3-column card grid (NOT table)
- **Category tabs**: 全部 | 影像 | NLP | 结构化 | 多模态 | 基因组
- **Search box**: "搜索..." input
- **Sort dropdown**: "排序: 更新时间"
- **View toggle**: Grid/List icons (optional, default grid)
- **Register button**: "注册模型" blue, top right
- **Each card**:
  - Model name (bold)
  - Category tag (colored: 影像=blue, NLP=green, 结构化=orange, 多模态=purple, 基因组=cyan)
  - Description text (truncated)
  - Framework badge (PyTorch/TensorFlow/SKLearn/XGBoost)
  - Version (e.g., v2.3.1)
  - Status badge (已发布=green, 评估中=blue, 草稿=gray, 训练中=blue)
  - QPS metric (if deployed)
  - "查看详情 ->" link
- **Pagination**: "共 28 个模型", page numbers, "12/页"

**Mock data**: 6 model cards minimum, covering all categories and statuses.

**Steps:**
- [ ] Rewrite ModelList.vue with card grid layout
- [ ] Add category filter tabs as `a-radio-group` with button style
- [ ] Add search + sort in a row above cards
- [ ] Create card component inline using `a-card`
- [ ] Add "注册模型" modal (a-modal with form)
- [ ] Keep mock data consistent with design: 肺结节检测模型, 病理分类模型, 心电图分析模型, NLP实体识别, 糖尿病预测模型, 基因变异分类模型
- [ ] Commit: `feat: redesign model list with card grid and category tabs`

---

## Task 2: Model Detail — Rich Detail Page

**Files:**
- Rewrite: `maidc-portal/src/views/model/ModelDetail.vue`

**Design spec:**
- **Breadcrumb**: 模型管理 / 肺结节检测模型
- **Tabs**: 基本信息 | 版本列表 | 评估记录 | 部署管理
- **基本信息 tab — Top card (2 columns)**:
  - Left: name, description, action buttons (编辑, 注册新版本)
  - Left fields: Model ID, 类型, 框架, 任务, 输入, 输出, 标签
  - Right fields: 所属项目, 负责人, 创建/更新时间, 最新版本
  - Bottom: Performance metrics box (准确率, 灵敏度, 特异度, AUC)
- **版本列表 tab**: Version table (columns: 版本号, 描述, 框架, 文件大小, AUC, 状态, 创建时间, 操作)
  + Version comparison section (two dropdowns + compare button + metrics table)
- **评估记录 tab**: 3 evaluation cards with status, dataset, metrics, duration
- **部署管理 tab**: Deployment table + "新增部署" button

**Steps:**
- [ ] Rewrite ModelDetail.vue with 4-tab layout
- [ ] Implement 基本信息 tab with 2-column descriptions + performance box
- [ ] Implement 版本列表 tab with version table + comparison
- [ ] Implement 评估记录 tab with evaluation cards
- [ ] Implement 部署管理 tab with deployment table
- [ ] Add "编辑模型" modal and "注册新版本" modal
- [ ] Commit: `feat: redesign model detail with rich tabs and metrics`

---

## Task 3: Deployment Monitoring — Dashboard Layout

**Files:**
- Rewrite: `maidc-portal/src/views/model/DeploymentList.vue` (rename conceptually to "部署监控")

**Design spec:**
- **Time filter tabs**: 近1h | 近6h | 近24h | 近7d + auto-refresh indicator
- **4 Metric cards**: 部署实例(45), 总推理次数(128,456), 平均延迟(245ms), GPU利用率(67%)
- **Chart section**: Tabbed chart (QPS/延迟P50-P99/GPU利用率/错误率) with bar chart
- **Deployment status panel**: 3 items with colored status dots (Running=green, Stopped=red, Error=yellow)
- **Alert table**: columns: 规则名称, 部署, 指标, 阈值, 当前值, 状态, 时间, 操作
  - Status badges: Firing(red), Warning(yellow), Resolved(green)
  - "+ 新建规则" button

**Steps:**
- [ ] Rewrite DeploymentList.vue as monitoring dashboard
- [ ] Add time filter tabs + auto-refresh indicator
- [ ] Add 4 metric cards row
- [ ] Add tabbed chart with MetricChart component
- [ ] Add deployment status panel
- [ ] Add alert table with status badges
- [ ] Add "新建规则" modal
- [ ] Commit: `feat: redesign deployment monitoring as dashboard layout`

---

## Task 4: Version Management — Add Comparison Section

**Files:**
- Modify: `maidc-portal/src/views/model/VersionList.vue`

**Design spec:**
- **Version table**: Keep existing but add comparison section below
- **Comparison section**:
  - Two version dropdown selectors
  - "对比" button
  - 3 comparison sub-tables:
    1. 超参数对比 (learning_rate, batch_size, epochs, optimizer)
    2. 训练指标对比 (loss, accuracy, val_loss)
    3. 评估指标对比 (AUC, F1, Precision, Recall, 推理延迟, 模型大小) with "差异" column

**Steps:**
- [ ] Add comparison section below version table
- [ ] Add version selector dropdowns and compare button
- [ ] Add 3 comparison tables with mock data
- [ ] Style comparison section with proper spacing
- [ ] Commit: `feat: add version comparison section with metric tables`

---

## Task 5: Model Evaluation — Card Layout

**Files:**
- Rewrite: `maidc-portal/src/views/model/EvalList.vue`

**Design spec:**
- **Filters**: 评估类型 dropdown + 状态 dropdown + 搜索 input
- **New Evaluation button**: blue, top right
- **Evaluation cards** (not table!):
  - Title: "vX.X.X [评估名称]"
  - Status badge: COMPLETED(green), RUNNING(blue), FAILED(red)
  - Labels/tags: 评估类型 tag + 数据集名称(条数) tag
  - Metrics row: AUC, F1, Precision, Recall
  - Progress bar for RUNNING status
  - Error message for FAILED status
  - Action buttons: 查看报告/导出报告, or 取消/重试
- **Expanded detail section** (visible for completed eval):
  - Performance metrics grid
  - Confusion matrix (2x2 table)

**Steps:**
- [ ] Rewrite EvalList.vue with card layout
- [ ] Add filter row with dropdowns and search
- [ ] Create evaluation card component inline
- [ ] Add expanded detail section with confusion matrix
- [ ] Add "新建评估" modal
- [ ] Mock 4 evaluations: 2 COMPLETED, 1 RUNNING, 1 FAILED
- [ ] Commit: `feat: redesign evaluation list with card layout and confusion matrix`

---

## Task 6: Approval Workflow — Tab Filters

**Files:**
- Modify: `maidc-portal/src/views/model/ApprovalList.vue`

**Design spec:**
- **Tab filter**: 待审批(active) | 已审批 | 全部
- **Badge**: "待审批 5 项" (yellow/orange badge)
- **Table columns**: 模型名称, 版本, 审批类型, 申请人, 提交时间, 状态, 操作
- **Status badge**: 审批中(yellow/orange)
- **Action**: "审批" blue link per row

**Steps:**
- [ ] Add tab filter above table using `a-tabs` or `a-radio-group`
- [ ] Add badge count for pending items
- [ ] Update table columns to match design (模型名称 instead of id)
- [ ] Update approval types to: 上线审批, 发布审批, 临床使用
- [ ] Keep approval modal
- [ ] Commit: `feat: add tab filters and badge to approval list`

---

## Task 7: Traffic Routing — Card Config with Visual Bars

**Files:**
- Rewrite: `maidc-portal/src/views/model/RouteConfig.vue`

**Design spec:**
- **Route cards** (3 cards, not table):
  - Title + Type badge (CANARY=blue, AB_TEST=green, WEIGHTED=purple)
  - Model name
  - Status badge (启用=green, 禁用=red)
  - Traffic distribution bar (visual percentage bar with labels)
  - Actions: 编辑, 详情
- **Detail section** (for selected route):
  - Route rule configuration fields (默认部署, 金丝雀百分比, 成功率阈值, 自动提升)
  - Traffic distribution visualization (bar chart)
  - JSON preview of route config
- **New Route button**: top right

**Steps:**
- [ ] Rewrite RouteConfig.vue with card layout
- [ ] Create route card with type badge, status, traffic bar
- [ ] Add traffic distribution bar using colored divs
- [ ] Add detail section with form fields and JSON preview
- [ ] Add "新建路由" modal
- [ ] Mock 3 routes: CANARY, AB_TEST, WEIGHTED
- [ ] Commit: `feat: redesign traffic routing with card layout and visual traffic bars`

---

## Task 8: Final Verification + Build

**Files:** None

- [ ] Run `npx vite build` — must pass
- [ ] Verify all 7 model pages render without errors
- [ ] Commit any fixes

---

## Scope Note

The following pages are NOT included in this sub-project (deferred):
- 05a-部署详情 (DeploymentDetail.vue) — minor, keep existing
- 06a-版本对比 (part of Task 4)
- 07a-评估详情 (part of Task 5 expanded section)
- 08a-审批详情 (keep existing drawer)
- 推理日志 (InferenceLog.vue) — already reasonable
- AI Worker 集群管理 — new page, defer to later phase
