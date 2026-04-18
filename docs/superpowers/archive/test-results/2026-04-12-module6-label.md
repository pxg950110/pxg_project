# Module 6: Label Management Test Report

**Date**: 2026-04-12
**Tester**: Claude Agent
**Environment**: http://localhost:3000, admin / Admin@123
**Data Source**: Frontend mock data (no backend API for label tasks)

---

## 6.1 Label Task List /label/tasks

### 6.1.1 Page Load

| Item | Result |
|------|--------|
| Status | **PASS** |
| URL | http://localhost:3000/label/tasks |
| Title | "标注任务 - MAIDC" |
| API Calls | No specific API call for label tasks; page uses frontend mock data. GET /api/v1/users/me returned 200 |
| Evidence | Page heading "标注任务" displayed, 6 task cards rendered with correct mock data |

**Details**:
- Page loads successfully with mock data
- 4 metric cards displayed: 标注任务(18), 进行中(7), 已标注数据(23,456), 平均一致性(0.92)
- 6 task cards: 肺结节CT标注, 病理切片多边形标注, 病理报告NER标注, DR胸片标注, 心电图异常检测标注, 检验报告实体标注
- 3 filter dropdowns (类型, 格式, 状态) and 1 search input
- Pagination shows "共 6 个任务", 6/page

**Screenshot**: `screenshots/label/6.1.1-label-tasks-loaded.png`

---

### 6.1.2 Create Task (New Task Modal)

| Item | Result |
|------|--------|
| Status | **PARTIAL PASS** (useModal bug) |
| Trigger | Click "新建任务" button |
| Modal | Opens with title "新建标注任务" |
| API | No POST request (mock data, useModal bug prevents proper interaction) |

**Details**:
- "新建任务" button click successfully triggers modal
- Modal title: "新建标注任务"
- 10 form fields rendered: 任务名称, 关联数据集, 标签列表, 分配审核员, AI预标注, 标注类型, 标注格式, 分配标注员, 截止日期, 描述
- **BUG - useModal**: Modal `open` prop receives `Ref<boolean>` instead of `boolean`. This causes the modal to render but with incorrect visibility behavior. Cancel/OK buttons are visible from useModal's default state.
- Form submission (OK click) did not trigger a POST API call - modal remains open (likely due to form validation and useModal bug)

**Screenshot**: `screenshots/label/6.1.2-modal-open.png`

---

### 6.1.3 Edit Task

| Item | Result |
|------|--------|
| Status | **FAIL** |
| Reason | No edit button found on task cards |

**Details**:
- Task cards display: task name, type tag, format, dataset, progress bar, assignees, deadline, status, "查看详情 →" link
- No explicit edit/delete/action buttons visible on the card list view
- Cards are clickable and navigate to `/label/detail/${id}` on click

---

### 6.1.4 Delete Task

| Item | Result |
|------|--------|
| Status | **FAIL** |
| Reason | No delete button found on task cards |

**Details**:
- Same as 6.1.3 - no action buttons for delete on the list view

---

### 6.1.5 View Statistics

| Item | Result |
|------|--------|
| Status | **FAIL** |
| Reason | No statistics button found on task cards |

**Details**:
- No explicit "统计" button visible on task cards in list view

---

### 6.1.6 AI Pre-annotation

| Item | Result |
|------|--------|
| Status | **FAIL** |
| Reason | No AI pre-annotation button found on task cards |

**Details**:
- The "新建标注任务" modal has an "AI 预标注" toggle field, but no per-task AI pre-annotation trigger in the list

---

### 6.1.7 Enter Workspace

| Item | Result |
|------|--------|
| Status | **FAIL** (Route bug) |
| Expected | Click task card -> navigate to /label/workspace/{id} |
| Actual | Click task card -> navigates to /label/detail/{id} which is NOT a registered route |

**Details**:
- `LabelTaskList.vue` line 107: `router.push(\`/label/detail/${task.id}\`)`
- Route configuration only defines `/label/workspace/:id` (LabelWorkspace), NOT `/label/detail/:id`
- The `/label/detail/:id` route does not exist in `asyncRoutes.ts`
- A `LabelTaskDetail.vue` component exists but has no corresponding route definition
- Clicking a card results in an unmatched route (blank page)

**Screenshot**: `screenshots/label/6.1.7-label-detail-not-found.png`

---

## 6.2 Label Workspace /label/workspace/:id

### 6.2.1 Page Load

| Item | Result |
|------|--------|
| Status | **PASS** |
| URL | http://localhost:3000/label/workspace/1 |
| Title | "标注工作台 - MAIDC" |
| API | No specific API call (mock data) |

**Details**:
- Workspace loads with mock data for "肺结节CT标注任务"
- Navigation panel: 上一张, IMG_0344/600, 下一张
- Image viewer info: WL: -600, WW: 1500, 800x600, CT 胸部横断面
- Annotation list shows: nodule, mass, effusion (with "添加标签" option)
- Existing annotations: nodule (x:300 y:200 w:120 h:80), mass (x:450 y:280 w:90 h:60)
- Action buttons: 保存, 提交审核, 跳过
- No canvas element rendered (image viewer uses a different approach)

**Screenshot**: `screenshots/label/6.2.1-workspace.png`

---

### 6.2.2 Select Tool

| Item | Result |
|------|--------|
| Status | **FAIL** |
| Reason | No tool selection buttons visible |

**Details**:
- No explicit tool buttons (rectangle, circle, polygon, etc.) found in the workspace
- The annotation list suggests tools exist but toolbar buttons are not rendered or use a different mechanism
- No `[class*="tool"]` elements found

---

### 6.2.3 Save Annotations

| Item | Result |
|------|--------|
| Status | **PARTIAL PASS** |
| Trigger | Click "保 存" button |
| API | No PUT request (mock data) |

**Details**:
- "保 存" button exists and is clickable
- No API call made after clicking (mock/frontend-only implementation)
- No success/error message displayed (likely needs backend)

---

### 6.2.4 Submit for Review

| Item | Result |
|------|--------|
| Status | **PARTIAL PASS** |
| Trigger | Click "提交审核" button |
| API | No PUT request (mock data) |

**Details**:
- "提交审核" button exists and is clickable
- No API call or status change observed (mock/frontend-only)
- Expected behavior: status change to "待审核" but cannot verify without backend

---

## Summary

| Test | Result | Notes |
|------|--------|-------|
| 6.1.1 Page Load | PASS | Mock data displays correctly |
| 6.1.2 Create Task | PARTIAL PASS | Modal opens but useModal bug affects interaction |
| 6.1.3 Edit Task | FAIL | No edit button in list view |
| 6.1.4 Delete Task | FAIL | No delete button in list view |
| 6.1.5 View Statistics | FAIL | No statistics button |
| 6.1.6 AI Pre-annotate | FAIL | No per-task AI pre-annotation trigger |
| 6.1.7 Enter Workspace | FAIL | Route bug: navigates to /label/detail/{id} instead of /label/workspace/{id} |
| 6.2.1 Workspace Load | PASS | Mock workspace displays correctly |
| 6.2.2 Select Tool | FAIL | No tool selection buttons rendered |
| 6.2.3 Save Annotations | PARTIAL PASS | Button exists but no backend integration |
| 6.2.4 Submit Review | PARTIAL PASS | Button exists but no backend integration |

**Pass Rate**: 2/11 PASS, 2/11 PARTIAL PASS, 7/11 FAIL

## Critical Bugs Found

1. **Route Bug (6.1.7)**: `LabelTaskList.vue` navigates to `/label/detail/{id}` but route only has `/label/workspace/:id`. Missing route definition.
2. **useModal Bug (6.1.2)**: `useModal` returns `Ref<boolean>` for `visible`/`open` prop, which is passed directly to `a-modal :open` instead of `.value`. Causes incorrect modal behavior across all modals in the app.
3. **Missing CRUD Actions (6.1.3-6.1.6)**: Task cards have no edit, delete, statistics, or AI pre-annotation action buttons. Only a "查看详情 →" link is available.

## Known Issues Affecting Testing

- Chrome DevTools MCP creates extra browser tabs during interaction, causing page navigation issues. Required workaround using `initScript` and closing extra tabs.
- All data is frontend mock - no backend API integration for label tasks.
