# Sub-project 4: Annotation Management — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** Align all 4 annotation pages + 3 modals with .pen design for pixel-perfect accuracy.

**Status:** COMPLETED ✓ (2026-04-12)

**Architecture:** Vue 3 SFC components using Ant Design Vue. Major changes: task list switches to card grid, image workspace gets 3-panel DICOM layout, text workspace gets enhanced NER highlighting, task detail gets 5-tab layout with annotator table.

**Tech Stack:** Vue 3 + TypeScript + Ant Design Vue + ECharts

---

## Gap Summary

| Page | Design | Current Code | Gap |
|------|--------|-------------|-----|
| 标注管理-任务列表 | 4 metric cards + filter bar + 3-col card grid | Table + SearchForm | **MAJOR** |
| 标注管理-任务详情 | 4 metric cards + 5-tab layout + annotator progress table | 4 metric cards + 3-tab layout + basic tables | **MAJOR** |
| 标注工作台-影像 | 3-panel: tool sidebar + dark DICOM viewer + annotation panel | Placeholder "集成 Canvas 标注工具" | **MAJOR** |
| 标注工作台-文本 | 2-panel: text with colored entity highlights + entity annotation panel | Functional but needs design alignment | **MODERATE** |
| Modal-创建标注任务 | Two-column form with label tags, AI toggle, date picker | Single-column basic form | **MODERATE** |
| Modal-标注审核 | Two-column: image preview + annotator comparison with IoU | Basic descriptions + radio approve/reject | **MODERATE** |
| Modal-AI预标注 | Form with model select, confidence slider, scope radio, overwrite toggle | Already exists in LabelWorkspaceText, similar | **MINOR** |

---

## File Structure

### Files to Modify
| File | Responsibility |
|------|---------------|
| `maidc-portal/src/views/label/LabelTaskList.vue` | Rewrite: card grid + metric cards + enhanced create modal |
| `maidc-portal/src/views/label/LabelTaskDetail.vue` | Rewrite: 5-tab layout with annotator progress table |
| `maidc-portal/src/views/label/LabelWorkspace.vue` | Rewrite: 3-panel image annotation workspace |
| `maidc-portal/src/views/label/LabelWorkspaceText.vue` | Modify: design alignment with top bar, entity types, submit/skip |

---

## Task 1: Label Task List — Card Grid Redesign

**Files:**
- Rewrite: `maidc-portal/src/views/label/LabelTaskList.vue`

**Design spec:**
- **4 Metric cards** at top: 标注任务(18), 进行中(7), 已标注数据(23456), 平均一致性(0.92)
- **Filter bar**: 类型 dropdown (全部/影像标注/文本标注) + 格式 dropdown + 状态 dropdown (全部/待标注/进行中/已完成/已暂停) + 搜索 input
- **Card grid** (3-column, NOT table):
  - Task name (bold)
  - Type tag: 影像标注=blue, 文本标注=green
  - Format: 矩形框/多边形/NER等
  - Dataset name
  - Progress bar with percentage
  - Assignee names (头像 + 名字)
  - Deadline date (if set)
  - Status badge: 待标注=gray, 进行中=blue, 已完成=green, 已暂停=orange
  - "查看详情" link
- **Pagination**: "共 X 个任务", page numbers, "12/页"
- **Create task modal**: two-column form (see Task 5)

**Current code**: Simple table with search, basic modal. All data is from API but rendered as table rows.

**Steps:**
- [ ] Rewrite LabelTaskList.vue with 4 metric cards using MetricCard component
- [ ] Add filter bar row with 4 filter controls
- [ ] Replace a-table with 3-column card grid using a-row/a-col
- [ ] Create card component inline with all design fields
- [ ] Add "新建任务" modal with two-column form (enhanced from current simple form)
- [ ] Mock 6 task cards covering: 影像标注 (3), 文本标注 (2), mixed statuses
- [ ] Add pagination below cards
- [ ] Commit: `feat: redesign label task list with card grid and metric cards`

---

## Task 2: Label Task Detail — 5-Tab Layout with Annotator Progress

**Files:**
- Rewrite: `maidc-portal/src/views/label/LabelTaskDetail.vue`

**Design spec:**
- **Header**: Back arrow + task title + subtitle (type · format · status badges) + "编辑任务"/"删除" buttons right-aligned
- **4 Metric cards**: 标注进度(650/1000, 65%), 标注员(3人), 标注数据(650条), 平均一致性(0.92)
- **5 Tabs**: 标注进度 | 质量控制 | 标注人员 | 标注统计 | 操作日志
- **Tab 1 — 标注进度**:
  - "总标注进度" progress bar: 450/600 (75%)
  - Annotator progress table: columns = 标注员, 已分配, 已完成, 进行中, 待处理, 完成率, 操作
  - 5 mock rows: 李医生(150/128/12/10/85%green), 王技师(180/162/8/10/90%green), 赵实习生(120/96/14/10/80%orange), 张主任(100/50/20/30/50%yellow), AI预标注(50/14/0/36/28%red)
  - Completion rate color: >=80% green, >=50% orange, <50% red
  - Bottom: "分配标注" blue button + "批量导出" gray button
- **Tab 2 — 质量控制**: Quality metrics, consistency scores (keep similar to current statistics tab)
- **Tab 3 — 标注人员**: Personnel management with roles and assignment
- **Tab 4 — 标注统计**: Label distribution chart (keep existing pie chart)
- **Tab 5 — 操作日志**: Activity log table

**Current code**: Has 4 metric cards (different labels: 总数/已完成/进行中/已审核), progress bar, 3 tabs (annotations/statistics/settings). Need to rename and expand to 5 tabs.

**Steps:**
- [ ] Rewrite header with back button + title + subtitle + action buttons
- [ ] Update 4 metric cards to match design: 标注进度, 标注员, 标注数据, 平均一致性
- [ ] Keep progress bar but update labels
- [ ] Expand to 5 tabs: 标注进度, 质量控制, 标注人员, 标注统计, 操作日志
- [ ] Implement 标注进度 tab with annotator progress table (5 mock rows with color-coded rates)
- [ ] Add "分配标注" + "批量导出" buttons below table
- [ ] Implement 质量控制 tab with quality metrics
- [ ] Implement 标注人员 tab with personnel list
- [ ] Move existing pie chart to 标注统计 tab
- [ ] Add 操作日志 tab with timestamped action list
- [ ] Move settings (currently separate tab) to a modal or keep in header "编辑任务" button
- [ ] Enhance review modal (see Task 5)
- [ ] Commit: `feat: redesign label task detail with 5-tab layout and annotator progress`

---

## Task 3: Image Annotation Workspace — 3-Panel Layout

**Files:**
- Rewrite: `maidc-portal/src/views/label/LabelWorkspace.vue`

**Design spec:**
- **Full-page layout** (NOT inside PageContainer with padding):
  - Left: Tool sidebar (narrow, ~48px)
  - Center: Dark DICOM viewer (#1E1E2E background)
  - Right: Annotation panel (~280px)
- **Tool sidebar** (vertical icon buttons):
  - Select (arrow icon)
  - Rectangle (active/highlighted)
  - Polygon
  - Ellipse
  - Freehand
  - Text
  - Zoom in
  - Zoom out
  - Undo
  - Redo
- **DICOM viewer header bar**:
  - Task name: "肺结节CT标注任务"
  - Navigation: "IMG_0345 / 600" + "上一张"/"下一张" buttons
  - "保存" blue button
  - DICOM metadata bar: "WL: -600 WW: 1500 | 800×600 | CT 胸部横断面"
- **DICOM viewer body**: Dark background (#1E1E2E), mock DICOM image area with:
  - Two annotation rectangles: red "nodule" (x:300 y:200 w:120 h:80), blue "mass" (x:450 y:280 w:90 h:60)
  - Dashed crosshair lines (center guides)
- **Right annotation panel**:
  - "标注列表" header
  - Tag legend: nodule=red, mass=blue, effusion=purple
  - "+添加标签" link
  - Annotations list (2 items): each showing label + coordinates
  - "✓ 提交审核" blue button + "▶ 跳过" gray button

**Current code**: Placeholder with "影像标注区域（集成 Canvas 标注工具）" text. No actual DICOM viewer or tool sidebar.

**Important**: This is a visual mockup — we build the UI structure with CSS, not a real DICOM viewer. The dark viewer area will show a placeholder with mock annotation rectangles drawn via CSS.

**Steps:**
- [ ] Remove PageContainer wrapper — use full-page layout with custom styling
- [ ] Create 3-panel layout: tool sidebar (48px) + viewer (flex:1) + annotation panel (280px)
- [ ] Build tool sidebar with vertical icon buttons (10 tools)
- [ ] Build DICOM viewer header bar with task name, navigation, save button, metadata
- [ ] Build dark viewer area with mock annotation rectangles using absolute-positioned divs
- [ ] Build right annotation panel with tag legend, annotations list, submit/skip buttons
- [ ] Add mock data: 2 annotations (nodule red, mass blue), 3 tags
- [ ] Style with dark theme for viewer, white for panels
- [ ] Commit: `feat: create 3-panel image annotation workspace with DICOM viewer mockup`

---

## Task 4: Text Annotation Workspace — Design Alignment

**Files:**
- Modify: `maidc-portal/src/views/label/LabelWorkspaceText.vue`

**Design spec:**
- **Top bar** (full-width):
  - Task name: "病理报告NER标注"
  - Navigation: "DOC_0089 / 450" + "上一条"/"下一条" buttons
  - "保存" blue button
- **2-Panel layout** below top bar:
  - Left (~60%): Text display panel
  - Right (~40%): Entity annotation panel
- **Text display panel**:
  - White card with pathology report text
  - Color-highlighted entity spans:
    - 咳嗽 → red (SYMPTOM)
    - 胸痛 → red (SYMPTOM)
    - 3.2×2.8cm → blue (SIZE)
    - 毛刺征 → purple (SIGN)
    - 中分化腺癌 → green (DIAGNOSIS)
- **Right panel sections**:
  - "实体类型" section: colored tag legend (SYMPTOM=red, SIZE=blue, SIGN=purple, DIAGNOSIS=green, TEST=orange)
  - "已标注实体" section: list of 5 annotated entities with colored tags
  - "✓ 提交审核" blue button + "▶ 跳过" gray button at bottom

**Current code**: Already has functional text annotation with segments, entity labels, annotation list, AI modal, undo/redo. Needs visual redesign to match the specific design layout.

**Steps:**
- [ ] Restructure layout: add top navigation bar with task name, progress counter, save button
- [ ] Update entity label names to match design: SYMPTOM, SIZE, SIGN, DIAGNOSIS, TEST
- [ ] Update entity colors: SYMPTOM=red(#ff4d4f), SIZE=blue(#1677ff), SIGN=purple(#722ed1), DIAGNOSIS=green(#52c41a), TEST=orange(#faad14)
- [ ] Update text demo content to pathology report with pre-annotated entities
- [ ] Add "✓ 提交审核" + "▶ 跳过" buttons in right panel (replace current header-level submit)
- [ ] Keep existing functional code (undo/redo, annotation logic, AI modal, save)
- [ ] Update styling for cleaner two-panel layout
- [ ] Commit: `feat: align text annotation workspace with design layout`

---

## Task 5: Enhanced Modals

**Files:**
- Modify: create task modal in `LabelTaskList.vue`
- Modify: review modal in `LabelTaskDetail.vue`

### 5a: Create Task Modal

**Design spec:**
- Two-column form layout in modal
- **Left column**: 任务名称*(text), 关联数据集*(dropdown "CT肺结节数据集v2"), 标签列表(blue pill tags: nodule, mass, effusio + input to add), 分配审核员(dropdown), AI预标注(toggle switch)
- **Right column**: 标注类型*(dropdown "影像标注"), 标注格式*(dropdown "矩形框"), 分配标注员(dropdown multi "李医生, 王技师"), 截止日期(date picker)
- **Bottom**: Description textarea (full width), "取消" + "创建任务" buttons

### 5b: Annotation Review Modal

**Design spec:**
- **Two-column layout**:
  - Left: "样本预览" — image preview area (placeholder with camera icon)
  - Right: "标注结果对比" — two annotator cards
- **Annotator cards**:
  - Card A: "标注员 A — 李医生" (blue text) + JSON data (label, bbox, confidence)
  - Card B: "标注员 B — 王技师" (green text) + JSON data (label, bbox, confidence)
- **Consistency score**: "IoU / 一致性得分: 0.87" (green)
- **Bottom buttons**: "驳回" red-outline + "通过" green-solid

**Steps:**
- [ ] Rewrite create task modal with two-column layout
- [ ] Add all fields: task name, dataset dropdown, label tags with input, reviewer dropdown, AI toggle, annotation type, format, multi-assignee, deadline, description
- [ ] Rewrite review modal with two-column: preview area + annotator comparison
- [ ] Add IoU consistency score display
- [ ] Add mock annotator data with JSON annotation display
- [ ] Style buttons: 驳回(red outline) + 通过(green solid)
- [ ] Commit: `feat: enhance annotation modals with two-column layout and reviewer comparison`

---

## Task 6: Final Build Verification

**Files:** None

- [ ] Run `npx vite build` from maidc-portal — must pass
- [ ] Verify all 4 label pages render without errors
- [ ] Commit any fixes

---

## Scope Note

The following are NOT included (deferred):
- Real DICOM viewer integration (Canvas/WebGL) — this is a visual mockup only
- Real-time collaborative annotation — deferred to future phase
- AI Worker pre-annotation backend integration — frontend modal only
- Annotation export/download functionality — mock buttons only
