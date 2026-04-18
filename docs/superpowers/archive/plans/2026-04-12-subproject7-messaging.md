# Sub-project 7: Messaging — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** Align messaging pages with .pen design, add new Message Detail page, redesign Notification Settings with switches, and add Template Management page. Docker deployment already complete.

**Status:** COMPLETED ✓ (2026-04-12)

**Architecture:** Vue 3 SFC components using Ant Design Vue. MessageList switches from a-list to card-based layout with custom tab pills. NotificationSettings gets toggle switches + type preference matrix. New pages for MessageDetail and TemplateManagement.

**Tech Stack:** Vue 3 + TypeScript + Ant Design Vue

---

## Gap Summary

| Page | Design | Current Code | Gap |
|------|--------|-------------|-----|
| 消息中心 | Card-based messages + custom tab pills | a-list with tabs | **MAJOR** |
| 消息详情 | Detail card with header + body | Does not exist | **MAJOR** (new) |
| 通知设置 | Toggle switches + type preference matrix + save | 3 channel cards with tables | **MAJOR** |
| 模板管理 | 7-column template table | Does not exist | **MAJOR** (new) |

---

## File Structure

### Files to Modify
| File | Responsibility |
|------|---------------|
| `maidc-portal/src/views/message/MessageList.vue` | Redesign: card-based messages + custom tab pills + mock data |
| `maidc-portal/src/views/message/NotificationSettings.vue` | Redesign: toggle switches + type preferences + save button |
| `maidc-portal/src/router/asyncRoutes.ts` | Add routes for new pages |

### Files to Create
| File | Responsibility |
|------|---------------|
| `maidc-portal/src/views/message/MessageDetail.vue` | New: message detail card |
| `maidc-portal/src/views/message/TemplateManagement.vue` | New: template management table |

---

## Task 1: Message List — Card-Based Redesign + Mock Data

**Files:**
- Rewrite: `maidc-portal/src/views/message/MessageList.vue`

**Design spec (from .pen gsUAE):**
- **Page header**: "消息中心" (22px/600) + "系统通知与操作提醒" (14px, muted) + "全部已读" button (right, blue-bg tint)
- **Custom tab pills** (NOT a-tabs, use custom div buttons):
  - "全部" (active: primary bg, white text)
  - "未读 (5)" (inactive: card bg, border, muted text)
  - "已读" (inactive: card bg, border, muted text)
- **6 message cards** (NOT list items, card-style):
  - First unread card: primary-bg tint (#e6f4ff), primary border
  - Other unread cards: white bg, gray border
  - Read cards: white bg, no border
  - Each card has: top row (type icon + type label + title + time right-aligned) + action row ("查看详情" + "标记已读" links)
- **Mock data** (6 messages):
  1. UNREAD, SYSTEM, 模型部署完成通知, 肺结节检测v3.3已成功部署至生产环境, 10分钟前 (highlighted)
  2. UNREAD, ALERT, GPU内存告警, GPU-Node-03内存使用率已超过90%阈值, 30分钟前
  3. UNREAD, APPROVAL, 审批待处理, 张医生提交了新模型注册审批(APR-2026-0018), 1小时前
  4. UNREAD, SYSTEM, 数据同步完成, 患者数据增量同步完成，新增128条记录, 2小时前
  5. UNREAD, ALERT, 数据质量异常, 数据集"CT肺结节数据集v2"质量评分低于阈值, 3小时前
  6. READ, SYSTEM, 系统维护通知, 系统将于本周六凌晨2:00-4:00进行维护升级, 昨天

**Steps:**
- [ ] Replace a-tabs with custom tab pill buttons
- [ ] Replace a-list with card-based layout
- [ ] Add 6 mock message cards with different states
- [ ] Add click handler: clicking card navigates to /message/detail/:id
- [ ] Add "标记已读" per card and "全部已读" at top
- [ ] Keep tab filtering logic (computed filter by tab)
- [ ] Commit: `feat: redesign message list with card-based layout and mock data`

---

## Task 2: Message Detail — New Page

**Files:**
- Create: `maidc-portal/src/views/message/MessageDetail.vue`

**Design spec (from .pen pMRgm):**
- **Header**: ← arrow back + "消息详情" (22px/600)
- **Message card**:
  - Card header: type tag (colored) + title + time (right-aligned), with bottom border
  - Card body: full message content text + action buttons at bottom

**Mock data**: Use first message — 模型部署完成通知, SYSTEM type, full content about model deployment

**Steps:**
- [ ] Create MessageDetail.vue with header and message card
- [ ] Add back navigation
- [ ] Add mock message data with full content
- [ ] Commit: `feat: add message detail page`

---

## Task 3: Notification Settings — Toggle Switches + Type Preferences

**Files:**
- Rewrite: `maidc-portal/src/views/message/NotificationSettings.vue`

**Design spec (from .pen CD0G9):**
- **Page header**: "通知设置" (20px/600)
- **Description**: "配置您的通知偏好和接收渠道。修改后立即生效。" (13px, muted)
- **通知渠道 card**: 
  - Header: "通知渠道" (16px/600)
  - 4 rows with icon + label (left) + toggle switch (right):
    - 站内通知 (BellOutlined) → enabled (primary switch)
    - 邮件通知 (MailOutlined) → enabled
    - 短信通知 (MessageOutlined) → disabled (gray switch)
    - Webhook (SendOutlined) → enabled
- **通知类型偏好 card**:
  - Header: "通知类型偏好" (16px/600)
  - Table with columns: 通知类型 | 站内 | 邮件 | 短信
  - 4 rows: 告警通知, 审批通知, 任务通知, 系统通知
  - Each cell has a checkbox (some checked, some not)
- **"保存设置" primary button** (right-aligned)

**Steps:**
- [ ] Remove current table-based layout
- [ ] Build 通知渠道 card with 4 toggle switch rows
- [ ] Build 通知类型偏好 card with checkbox matrix
- [ ] Add "保存设置" button
- [ ] Add mock data for all toggles and checkboxes
- [ ] Commit: `feat: redesign notification settings with toggle switches`

---

## Task 4: Template Management — New Page

**Files:**
- Create: `maidc-portal/src/views/message/TemplateManagement.vue`

**Design spec (from .pen kSlzk):**
- **Header**: "消息模板管理" (16px/600) + "+ 新建模板" primary button
- **Table** (7 columns): 模板名称, 模板编码, 消息类型, 通知渠道, 状态, 更新时间, 操作
  - 消息类型 column: colored text — 告警通知=red, 审批通知=blue, 任务通知=green, 系统通知=muted
  - 状态 column: 已启用=green, 已禁用=muted
  - 操作 column: "编辑 | 预览" links in primary/blue
- **Mock data** (5 rows):
  1. 告警通知模板, TPL_ALERT_001, 告警通知(red), 邮件/短信/Webhook, 已启用(green), 2026-04-08
  2. 审批通知模板, TPL_APPROVAL_001, 审批通知(blue), 邮件/站内信, 已启用, 2026-04-05
  3. 任务完成通知, TPL_TASK_001, 任务通知(green), 邮件/站内信, 已启用, 2026-04-03
  4. 系统维护通知, TPL_SYSTEM_001, 系统通知(muted), 全渠道, 已启用, 2026-03-20
  5. 数据质量告警, TPL_QUALITY_001, 系统通知(muted), 邮件, 已禁用(muted), 2026-03-15
- Table header has #f9fafb background

**Steps:**
- [ ] Create TemplateManagement.vue with header and table
- [ ] Add 5 mock template rows
- [ ] Add colored type text and status text
- [ ] Add "编辑 | 预览" links
- [ ] Commit: `feat: add message template management page`

---

## Task 5: Router Updates + Build Verification

**Files:**
- Modify: `maidc-portal/src/router/asyncRoutes.ts`

Add to message children:
```typescript
{ path: 'detail/:id', name: 'MessageDetail', meta: { title: '消息详情', hidden: true }, component: () => import('@/views/message/MessageDetail.vue') },
{ path: 'templates', name: 'TemplateManagement', meta: { title: '模板管理' }, component: () => import('@/views/message/TemplateManagement.vue') },
```

- [ ] Add 2 new routes to asyncRoutes.ts
- [ ] Run `npx vite build` — must pass
- [ ] Commit: `feat: add routes and verify build for messaging pages`
