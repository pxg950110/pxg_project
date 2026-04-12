# P4: 设计稿对齐 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 对比 .pen 设计稿与前端实际页面，修复布局、颜色、间距、缺失组件等明显差异。

**Architecture:** 逐页面导出 .pen 设计截图，与 chrome-devtools 前端截图对比，生成差异清单后逐项修复。

**Tech Stack:** Pencil MCP (设计稿导出), chrome-devtools MCP (前端截图对比), Vue 3 + Ant Design Vue (修复)

**Spec:** `docs/superpowers/specs/2026-04-12-p4-design-alignment.md`

**前置依赖:** P2（布局路由修复）完成后执行，否则 layout 不显示无法对比。

---

## File Structure

| 操作 | 文件 | 职责 |
|------|------|------|
| 读取 | `pencil-new.pen` | 设计稿源文件 |
| 修改 | `maidc-portal/src/views/**/*.vue` | 根据差异清单修复 |
| 修改 | `maidc-portal/src/layouts/BasicLayout.vue` | 侧边栏/header 差异修复 |
| 创建 | `docs/superpowers/test-results/design-diff-report.md` | 差异对比报告 |

---

### Task 1: 导出设计稿截图

**Files:**
- Read: `pencil-new.pen` (Frame IDs: nR4Uw, QptC0, Syc8a, l2c3F, c2K7C, skaqI, fltE4)

- [ ] **Step 1: 导出全部7个设计稿页面**

使用 pencil MCP 导出每个 frame 为 PNG：

```
mcp__pencil__export_nodes → filePath: pencil-new.pen, nodeIds: ["nR4Uw","QptC0","Syc8a","l2c3F","c2K7C","skaqI","fltE4"], outputDir: docs/superpowers/test-results/design-screenshots/, format: png
```

Expected: 7张设计稿 PNG 截图生成到 `docs/superpowers/test-results/design-screenshots/` 目录。

- [ ] **Step 2: 检查导出结果**

确认7张截图对应：
1. `nR4Uw.png` — 登录页
2. `QptC0.png` — Dashboard 工作台
3. `Syc8a.png` — 模型列表
4. `l2c3F.png` — 部署监控
5. `c2K7C.png` — 患者列表
6. `skaqI.png` — 研究项目
7. `fltE4.png` — 用户管理

---

### Task 2: 截取前端页面截图并对比

- [ ] **Step 1: 登录前端并截取7个页面对照图**

用 chrome-devtools 逐页导航并截图：
1. `/login` → `docs/superpowers/test-results/screenshots/p4-01-login.png`
2. `/dashboard/overview` → `p4-02-dashboard.png`
3. `/model/list` → `p4-03-model-list.png`
4. `/model/deployments` → `p4-05-deployments.png`
5. `/data/cdr/patients` → `p4-10-patients.png`
6. `/data/rdr/projects` → `p4-11-projects.png`
7. `/system/users` → `p4-20-users.png`

- [ ] **Step 2: 生成差异对比报告**

逐页面对比设计稿和前端截图，记录差异到 `docs/superpowers/test-results/design-diff-report.md`：

```markdown
# 设计稿 vs 前端差异报告

## 01-登录页
| 差异项 | 设计稿 | 前端 | 严重度 |
|--------|--------|------|--------|
...

## 02-Dashboard
...
```

严重度分级：P0(布局错误) / P1(样式明显不一致) / P2(细节差异)

---

### Task 3-N: 按差异清单逐项修复

**说明:** Task 3 到 Task N 的具体内容取决于 Task 2 的差异报告。每个差异项作为一个独立的 Step 执行。

每个修复 Step 的流程：
1. 在对应 .vue 文件中修改 template 或 style
2. 用 chrome-devtools 截图验证
3. 对比设计稿确认差异已消除

**常见预期修复项（基于 .pen 设计稿分析）：**

- [ ] **侧边栏宽度**: 设计稿220px，确认前端一致
- [ ] **Header高度和样式**: 确认与设计稿匹配
- [ ] **统计卡片布局**: 设计稿使用特定间距和圆角
- [ ] **表格列宽**: 对齐设计稿定义的列顺序和宽度
- [ ] **状态标签颜色**: 确认与设计稿配色一致
- [ ] **按钮样式**: 确认主色/边框/圆角与设计稿一致
- [ ] **筛选栏布局**: 确认下拉和输入框的排列与设计稿一致
- [ ] **分页器位置**: 确认在表格下方右对齐

- [ ] **最终 Commit**

```bash
git add maidc-portal/src/
git commit -m "fix: align frontend UI with .pen design specifications"
```
