# Sub-project 5: Audit + Alerts — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** Align audit log pages + alert center with .pen design, add metric cards, enhanced tables, mock data, and 2 new sub-pages.

**Status:** COMPLETED ✓ (2026-04-12)

**Architecture:** Vue 3 SFC components using Ant Design Vue. Audit pages get enhanced filter bars + expanded columns + export buttons. Alert center gets metric cards + severity badges. 2 new audit sub-pages (System Events, Compliance Report) and 1 new alert detail page added.

**Tech Stack:** Vue 3 + TypeScript + Ant Design Vue + ECharts (for compliance charts)

---

## Gap Summary

| Page | Design | Current Code | Gap |
|------|--------|-------------|-----|
| 操作审计 | 10-column table + enhanced filter bar + export + mock data | 8-column table + basic SearchForm | **MAJOR** |
| 操作详情 | 4-section card (info + request JSON + response JSON + error) | Simple drawer with basic info | **MODERATE** |
| 数据访问审计 | Enhanced filter bar + export + mock data | Basic table + SearchForm | **MODERATE** |
| 系统事件 | New page with system event table | Does not exist | **MAJOR** (new) |
| 合规报表 | New page with charts + summary cards | Does not exist | **MAJOR** (new) |
| 告警中心 | 4 metric cards + enhanced alert table with severity badges | Tabs + basic table + rule modal | **MAJOR** |
| 告警详情 | Two-panel: alert info + timeline | message.info placeholder | **MAJOR** (new) |
| 创建告警规则 | Enhanced modal with notification config | Already exists, functional | **MINOR** |

---

## File Structure

### Files to Modify
| File | Responsibility |
|------|---------------|
| `maidc-portal/src/views/audit/OperationLog.vue` | Rewrite: enhanced filter bar + 10-column table + export + mock data |
| `maidc-portal/src/views/audit/DataAccessLog.vue` | Rewrite: enhanced filter bar + export + mock data |
| `maidc-portal/src/views/alert/AlertList.vue` | Rewrite: metric cards + enhanced table + mock data |
| `maidc-portal/src/router/asyncRoutes.ts` | Add routes for new pages |

### Files to Create
| File | Responsibility |
|------|---------------|
| `maidc-portal/src/views/audit/SystemEventLog.vue` | New: system event log page |
| `maidc-portal/src/views/audit/ComplianceReport.vue` | New: compliance report with charts |
| `maidc-portal/src/views/alert/AlertDetail.vue` | New: alert detail with info + timeline |

---

## Task 1: Operation Audit — Enhanced Table + Filter Bar

**Files:**
- Rewrite: `maidc-portal/src/views/audit/OperationLog.vue`

**Design spec (from .pen rL8XE + Xa61Z):**
- **Filter bar**: 5 controls in a row:
  - 服务 dropdown (全部/认证服务/数据服务/模型服务/标注服务/系统服务)
  - 操作类型 dropdown (全部/创建/更新/删除/查询/登录/登出/导出)
  - 状态 dropdown (全部/成功/失败)
  - 时间范围 date range picker
  - 搜索 input (placeholder: "搜索操作人...")
- **Export button**: "导出" gray button, right-aligned
- **Table** (10 columns): 时间, 操作人, 服务, 操作类型, 资源, 请求方法, URL, 耗时, 状态, 操作
  - 请求方法 column: colored badges — GET=blue, POST=green, PUT=orange, DELETE=red
  - 状态 column: 成功=green badge, 失败=red badge
  - 耗时 column: color code — <100ms green, 100-500ms default, >500ms orange, >1s red
  - 操作 column: "详情" link
- **Mock data**: 8 rows covering different services, methods, statuses:
  1. admin, 认证服务, 登录, POST /auth/login, 45ms, 成功
  2. 李医生, 数据服务, 查询, GET /data/patients/123, 120ms, 成功
  3. admin, 模型服务, 创建, POST /model/register, 2300ms, 失败
  4. 王技师, 标注服务, 更新, PUT /label/tasks/5, 89ms, 成功
  5. 张主任, 模型服务, 删除, DELETE /model/versions/12, 56ms, 成功
  6. 系统, 系统服务, 导出, POST /system/export, 8500ms, 成功
  7. 赵实习生, 数据服务, 查询, GET /data/datasets, 340ms, 成功
  8. admin, 认证服务, 登录, POST /auth/login, 23ms, 失败
- **Pagination**: "共 15,234 条记录", page numbers, "20/页"
- **Detail drawer enhancement**: When clicking "详情", show drawer with 4 sections:
  1. 操作基本信息: 6 fields in 2 columns (操作类型, 操作人, 时间, IP地址, 操作结果, 请求路径)
  2. 请求参数: JSON viewer with syntax highlighting
  3. 响应结果: JSON viewer with green background
  4. 错误详情: JSON viewer with red background (shown only for failed operations)

**Current code**: Simple table with SearchForm (2 filters), 8 columns, basic detail drawer. All data from API.

**Steps:**
- [ ] Replace SearchForm with inline filter bar (4 selects + date range + search + export button)
- [ ] Update columns to 10: 时间, 操作人, 服务, 操作类型, 资源, 请求方法, URL, 耗时, 状态, 操作
- [ ] Add colored badges for 请求方法 (GET=blue, POST=green, PUT=orange, DELETE=red)
- [ ] Add color-coded 耗时 column (<100ms green, >500ms orange, >1s red)
- [ ] Add 8 mock data rows with realistic audit entries
- [ ] Enhance detail drawer: 4 sections with JSON viewers for request/response/error
- [ ] Add mock detail data for drawer (request JSON, response JSON, error for failed items)
- [ ] Add pagination config: pageSize 20, total 15234
- [ ] Commit: `feat: redesign operation audit with enhanced filter bar and 10-column table`

---

## Task 2: Data Access Audit — Enhanced Table + Filter Bar

**Files:**
- Rewrite: `maidc-portal/src/views/audit/DataAccessLog.vue`

**Design spec (from .pen JyWj6):**
- **Filter bar**: 4 controls + export:
  - 数据类型 dropdown (全部/患者数据/影像数据/研究数据/标注数据/模型数据)
  - 操作类型 dropdown (全部/查看/导出/下载/修改/删除)
  - 时间范围 date range picker
  - 搜索 input (placeholder: "搜索操作人...")
  - "导出" button (right-aligned)
- **Table** (8 columns): 时间, 操作人, 数据类型, 操作, 数据ID/名称, 患者ID, 访问目的, IP地址
  - 数据类型 column: colored tags — 患者数据=blue, 影像数据=purple, 研究数据=green, 标注数据=orange, 模型数据=cyan
  - 操作 column: colored text — 查看=default, 导出=blue, 下载=cyan, 修改=orange, 删除=red
- **Mock data**: 6 rows:
  1. 李医生, 患者数据, 查看, PAT-2026-00123, PAT-2026-00123, 日常诊疗, 192.168.1.105
  2. 王技师, 影像数据, 导出, IMG-2026-04567, -, 科研项目导出, 192.168.1.108
  3. 张主任, 研究数据, 下载, DS-2026-LUNG-01, -, 离线分析, 10.0.0.5
  4. 赵实习生, 标注数据, 修改, TASK-2026-0089, -, 标注工作, 192.168.1.112
  5. admin, 模型数据, 删除, MODEL-V2.3.1, -, 版本清理, 10.0.0.1
  6. 李医生, 患者数据, 查看, PAT-2026-00456, PAT-2026-00456, 日常诊疗, 192.168.1.105
- **Pagination**: "共 8,567 条记录", page numbers, "20/页"

**Current code**: Simple table with SearchForm (1 filter), 8 columns, no mock data.

**Steps:**
- [ ] Replace SearchForm with inline filter bar (2 selects + date range + search + export button)
- [ ] Update column labels and add colored tags for 数据类型
- [ ] Add colored text for 操作 column
- [ ] Add 6 mock data rows
- [ ] Add pagination config: pageSize 20, total 8567
- [ ] Commit: `feat: redesign data access audit with enhanced filter bar and mock data`

---

## Task 3: Alert Center — Metric Cards + Enhanced Table

**Files:**
- Rewrite: `maidc-portal/src/views/alert/AlertList.vue`

**Design spec (from .pen Skt7y):**
- **4 Metric cards** at top:
  - 活跃告警: 12 (yellow icon/background tint)
  - 今日已处理: 34 (green icon/background tint)
  - 平均响应: 8分钟 (blue icon/background tint)
  - 告警规则: 12 (default icon)
- **Tab filter**: 活跃告警(active) | 历史告警
- **"新建告警规则"** button top right
- **Table** columns: 告警名称, 级别, 关联资源, 指标, 当前值/阈值, 触发时间, 状态, 操作
  - 级别 column: severity badges — CRITICAL=red, WARNING=orange, INFO=blue
  - 状态 column: FIRING=red badge, ACKNOWLEDGED=yellow badge, RESOLVED=green badge
  - 操作 column: "详情" + "确认"(for FIRING items)
- **Mock data** (6 rows):
  1. 推理延迟过高, CRITICAL, 肺结节检测-v2, 推理延迟, 850ms/>500ms, FIRING, 10:30
  2. GPU内存使用率告警, WARNING, GPU-Node-03, GPU利用率, 92%/>85%, FIRING, 10:15
  3. 错误率异常, WARNING, 心电图分析-v1, 错误率, 5.2%/>3%, FIRING, 09:45
  4. 模型服务不可用, CRITICAL, 病理分类-v3, 可用性, 0%/>99%, ACKNOWLEDGED, 09:20
  5. 数据库连接池耗尽, CRITICAL, DB-Primary, 连接数, 198/200, RESOLVED, 08:50
  6. API响应超时, WARNING, Gateway, 响应时间, 3200ms/>3000ms, RESOLVED, 08:30

**Current code**: Tabs + table + create rule modal. No metric cards. Simple severity display via StatusBadge. No mock data.

**Steps:**
- [ ] Add 4 metric cards row using MetricCard component with appropriate icon colors
- [ ] Keep tab filter (活跃告警/历史告警)
- [ ] Update table columns to match design: 告警名称, 级别, 关联资源, 指标, 当前值/阈值, 触发时间, 状态, 操作
- [ ] Add severity badges: CRITICAL=red(#ff4d4f), WARNING=orange(#faad14), INFO=blue(#1677ff)
- [ ] Add status badges: FIRING=red, ACKNOWLEDGED=yellow, RESOLVED=green
- [ ] Add 6 mock alert rows
- [ ] Keep create rule modal (already functional)
- [ ] Add mock metric card data
- [ ] Commit: `feat: redesign alert center with metric cards and enhanced alert table`

---

## Task 4: New Pages — System Events, Compliance Report, Alert Detail

**Files:**
- Create: `maidc-portal/src/views/audit/SystemEventLog.vue`
- Create: `maidc-portal/src/views/audit/ComplianceReport.vue`
- Create: `maidc-portal/src/views/alert/AlertDetail.vue`
- Modify: `maidc-portal/src/router/asyncRoutes.ts`

### 4a: System Event Log

**Design spec (from .pen N1ww2):**
- **Filter bar**: 事件类型 dropdown (全部/系统启动/系统停止/配置变更/服务状态/安全事件) + 级别 dropdown (全部/INFO/WARN/ERROR) + 时间范围 + 搜索
- **Table** columns: 时间, 事件类型, 级别, 服务, 描述, 操作人, 操作
  - 级别 badges: INFO=blue, WARN=orange, ERROR=red
- **Mock data** (5 rows):
  1. 系统启动, INFO, maidc-gateway, 网关服务启动完成, 系统
  2. 配置变更, WARN, maidc-data, 数据库连接池参数调整 max=200→300, admin
  3. 服务状态, ERROR, maidc-model, 模型推理服务心跳超时(30s), 系统
  4. 安全事件, WARN, maidc-auth, 连续登录失败5次，IP: 203.0.113.45, 系统
  5. 系统停止, INFO, maidc-task, 定时任务服务优雅停机, admin

### 4b: Compliance Report

**Design spec (from .pen dkYi4):**
- **Summary cards** row: 审计覆盖率 98.5%(green), 合规得分 92分(green), 待整改项 3(yellow), 审计周期 2026-Q1
- **Charts section** (2 charts side by side):
  - Left: 操作类型分布 pie chart (查询45%, 创建20%, 更新18%, 删除8%, 其他9%)
  - Right: 合规趋势 line chart (monthly scores: Jan 88, Feb 90, Mar 91, Apr 92)
- **Report table**: 检查项, 类别, 状态, 得分, 最后检查时间
  - 5 rows covering data access controls, audit logging, encryption, access review, data retention
  - Status: 合格=green, 待整改=yellow

### 4c: Alert Detail Page

**Design spec (from .pen znlwg):**
- **Two-panel layout** (60/40 split):
  - **Left panel — 告警信息**:
    - 告警名称 title + severity badge
    - Info fields: 规则名称, 触发时间, 当前值, 阈值, 关联模型/部署, 通知方式
    - Mock data: 推理延迟过高, CRITICAL, 规则:推理延迟监控, 850ms, >500ms, 肺结节检测-v2, 邮件+钉钉
  - **Right panel — 处理时间线**:
    - Timeline with 3 events:
      1. 10:30 告警触发 — 推理延迟超过阈值 (850ms > 500ms)
      2. 10:31 通知已发送 — 已通知: 李医生(邮件), 张主任(钉钉)
      3. 10:35 等待处理 — 等待相关人员确认处理
    - Each event has timestamp, title, description
  - **Bottom buttons**: "确认处理" blue + "标记误报" gray + "返回" gray

### 4d: Router Updates

Add to `asyncRoutes.ts`:
- Under audit children: `{ path: 'system-events', name: 'SystemEventLog', meta: { title: '系统事件' }, component: () => import('@/views/audit/SystemEventLog.vue') }`
- Under audit children: `{ path: 'compliance', name: 'ComplianceReport', meta: { title: '合规报表' }, component: () => import('@/views/audit/ComplianceReport.vue') }`
- Under alert children: `{ path: 'detail/:id', name: 'AlertDetail', meta: { title: '告警详情', hidden: true }, component: () => import('@/views/alert/AlertDetail.vue') }`

**Steps:**
- [ ] Create SystemEventLog.vue with filter bar, table, mock data, severity badges
- [ ] Create ComplianceReport.vue with summary cards, pie chart, line chart, report table
- [ ] Create AlertDetail.vue with two-panel layout and timeline
- [ ] Add 3 new routes to asyncRoutes.ts
- [ ] Commit: `feat: add system events, compliance report, and alert detail pages`

---

## Task 5: Final Build Verification

**Files:** None

- [ ] Run `npx vite build` from maidc-portal — must pass
- [ ] Verify all audit/alert pages render without errors
- [ ] Commit any fixes

---

## Scope Note

The following are NOT included (deferred):
- Real-time alert push (WebSocket) — deferred to future phase
- Compliance report PDF export — mock only
- Audit log real-time streaming — deferred
- Advanced audit log search (full-text search on request/response body) — deferred
