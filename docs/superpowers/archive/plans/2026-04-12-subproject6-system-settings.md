# Sub-project 6: System Settings — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** Align all system settings pages with .pen design, add new detail pages for users/roles/orgs, redesign SystemConfig from table to categorized cards, and add Organization Management + Permission Management pages.

**Status:** COMPLETED ✓ (2026-04-12)

**Architecture:** Vue 3 SFC components using Ant Design Vue. Major changes: UserList gets inline filter bar + mock data; new UserDetail/RoleDetail two-panel pages; RoleList adds inline permission assignment; SystemConfig becomes 4 categorized cards; 3 new pages (Organization Management, Organization Detail, Permission Management).

**Tech Stack:** Vue 3 + TypeScript + Ant Design Vue

---

## Gap Summary

| Page | Design | Current Code | Gap |
|------|--------|-------------|-----|
| 用户管理 | Header + 4-filter bar + 7-col table + mock data | SearchForm + 7-col table + API data | **MAJOR** |
| 用户详情 | Two-panel: profile+avatar + roles+operations | Does not exist | **MAJOR** (new) |
| 角色管理 | Header + 7-col table + inline permission card | 5-col table + API data | **MAJOR** |
| 角色详情 | Two-panel: role info + permission matrix + users | Does not exist | **MAJOR** (new) |
| 系统配置 | 4 categorized config cards (基础/存储/安全/通知) | Table + SearchForm + add/edit modal | **MAJOR** |
| 组织管理 | Two-panel: tree(280px) + detail | Does not exist | **MAJOR** (new) |
| 组织详情 | Info card + 3 tabs + department table | Does not exist | **MAJOR** (new) |
| 权限管理 | Hierarchical table with type badges + pagination | Tree-based role permission only | **MAJOR** (new) |

---

## File Structure

### Files to Modify
| File | Responsibility |
|------|---------------|
| `maidc-portal/src/views/system/UserList.vue` | Redesign: inline filter bar + updated columns + mock data |
| `maidc-portal/src/views/system/RoleList.vue` | Redesign: new columns + inline permission assignment + mock data |
| `maidc-portal/src/views/system/SystemConfig.vue` | Redesign: 4 categorized config cards replacing table |
| `maidc-portal/src/router/asyncRoutes.ts` | Add routes for new pages |

### Files to Create
| File | Responsibility |
|------|---------------|
| `maidc-portal/src/views/system/UserDetail.vue` | New: two-panel user detail page |
| `maidc-portal/src/views/system/RoleDetail.vue` | New: two-panel role detail page |
| `maidc-portal/src/views/system/OrganizationList.vue` | New: two-panel org tree + detail |
| `maidc-portal/src/views/system/OrganizationDetail.vue` | New: org info + tabs + dept table |
| `maidc-portal/src/views/system/PermissionManagement.vue` | New: hierarchical permission table |

---

## Task 1: User List — Redesign with Inline Filter Bar + Mock Data

**Files:**
- Rewrite: `maidc-portal/src/views/system/UserList.vue`

**Design spec (from .pen fltE4):**
- **Page header**: "用户管理" title (22px/600) + "管理系统用户账号、角色分配与权限控制" subtitle (14px) + search icon button + "新建用户" primary button (right-aligned)
- **Filter bar** (inline, horizontal): 状态 dropdown + 角色 dropdown + 组织 dropdown + 搜索 input (fill remaining width, placeholder: "搜索用户名/姓名/邮箱...")
- **Table** (7 columns): #(60px), 用户名(120px), 姓名(120px), 邮箱(fill), 角色(120px), 状态(80px), 操作(100px)
  - 角色 column: blue tags (管理员, AI工程师, 研究员, 数据管理员)
  - 状态 column: green "启用" badge / red "禁用" badge
  - 操作 column: "编辑" + "查看" links (right-aligned)
- **Mock data** (4 rows):
  1. admin, 系统管理员, admin@maidc.cn, 管理员, 启用
  2. zhangsan, 张三, zhangsan@hospital.cn, AI工程师, 启用
  3. lisi, 李主任, lisi@hospital.cn, 研究员, 启用
  4. wangwu, 王五, wangwu@hospital.cn, 数据管理员, 禁用
- **Pagination**: "共 15 个用户", page 1/2

**Current code**: PageContainer with SearchForm (keyword + status), 7-column table (用户名/姓名/邮箱/角色/状态/最后登录/操作), create/edit/reset-pw modals, API data.

**Steps:**
- [ ] Replace PageContainer extra slot + SearchForm with custom header (title + subtitle + buttons)
- [ ] Add inline filter bar (4 controls: 状态/角色/组织 selects + 搜索 input)
- [ ] Update table columns to match design: add # column, remove 最后登录, reorder
- [ ] Replace API data with mock data (4 rows)
- [ ] Add pagination config showing "共 15 个用户"
- [ ] Keep existing modals (create user, edit user, reset password)
- [ ] Add "查看" link that navigates to `/system/users/:id`
- [ ] Update role tags to match design: 管理员/AI工程师/研究员/数据管理员
- [ ] Commit: `feat: redesign user list with inline filter bar and mock data`

---

## Task 2: User Detail — New Two-Panel Page

**Files:**
- Create: `maidc-portal/src/views/system/UserDetail.vue`

**Design spec (from .pen lNs6x):**
- **Header**: ← arrow back + "用户详情 - 张医生" (22px/600) + "编辑" outline button + "重置密码" warning-outline button
- **Two-panel layout** (left 520px, right fill):
  - **Left panel — 基本信息 card**:
    - Card header: "基本信息" (15px/600) with bottom border
    - Avatar section: circle avatar (blue) + username + email, with bottom border
    - Info rows (5 rows, label-value pairs): 用户名, 姓名, 邮箱, 手机, 组织
    - Mock: 张医生, zhangsan, zhangsan@hospital.cn, 138****1234, 放射科
  - **Right panel**:
    - **角色与权限 card**: header "角色与权限", 2 role rows showing assigned roles
    - **最近操作 card**: header "最近操作", 3 activity entries:
      1. 部署模型 CT肺结节检测v3 — 2小时前
      2. 提交审批申请 APR-2026-0015 — 昨天 10:00
      3. 上传版本 v3.2.1 — 昨天 09:15

**Steps:**
- [ ] Create UserDetail.vue with two-panel layout
- [ ] Build header with back arrow, title, edit/reset-password buttons
- [ ] Build left panel: profile card with avatar section and info rows
- [ ] Build right panel: roles card + recent operations card
- [ ] Add mock user data, role assignments, and activity log
- [ ] Commit: `feat: add user detail page with profile and activity log`

---

## Task 3: Role List — Redesign with Permission Assignment + Mock Data

**Files:**
- Rewrite: `maidc-portal/src/views/system/RoleList.vue`

**Design spec (from .pen PWi45):**
- **Page header**: "角色管理" (22px/600) + "管理系统角色与权限分配" subtitle (14px) + "新建角色" primary button
- **Table** (7 columns): 角色编码(120px), 角色名称(120px), 描述(fill), 用户数(80px center), 系统内置(110px center), 创建时间(120px), 操作(80px right)
  - 角色编码: bold, primary color for first row (ADMIN), regular for others
  - 系统内置 column: blue tag "系统内置"
  - 操作 column: "编辑" + "查看" links
  - First row (ADMIN): blue-tinted background ($--primary-bg)
- **Mock data** (6 rows):
  1. ADMIN, 平台管理员, 系统运维与权限管理, 3, 系统内置, 2026-01-01
  2. DATA_MANAGER, 数据管理员, 临床/科研数据治理, 5, 系统内置, 2026-01-01
  3. RESEARCHER, 研究员, 科研项目负责人, 12, 系统内置, 2026-01-01
  4. AI_ENGINEER, AI工程师, 模型开发与部署, 8, 系统内置, 2026-01-01
  5. CLINICIAN, 临床医生, AI辅助诊断使用者, 25, 系统内置, 2026-01-01
  6. AUDITOR, 审计员, 合规审查, 2, 系统内置, 2026-01-01
- **Below table — 权限分配 card**:
  - Header: "权限分配 - 平台管理员" (16px/600) + "已选中 15 项权限" subtitle
  - 2-column layout with 4 permission groups (checkboxes):
    - Column 1: 仪表盘 (3 items), 模型管理 (5 items)
    - Column 2: 数据管理 (4 items), 标注管理 (3 items)
  - Each group has a bold title + checkbox items below
- "查看" link navigates to `/system/roles/:id`

**Current code**: Simple table (5 columns) with create/edit modals, API data.

**Steps:**
- [ ] Replace PageContainer with custom header (title + subtitle + button)
- [ ] Update table columns to 7: 角色编码, 角色名称, 描述, 用户数, 系统内置, 创建时间, 操作
- [ ] Replace API data with 6 mock rows
- [ ] Add blue-tinted background for ADMIN row
- [ ] Add 系统内置 tags, edit/view links
- [ ] Add inline permission assignment card below table (2-column, 4 groups with checkboxes)
- [ ] Add mock permission data (checked/unchecked per group)
- [ ] Keep create/edit role modals
- [ ] Commit: `feat: redesign role list with permission assignment and mock data`

---

## Task 4: Role Detail — New Two-Panel Page

**Files:**
- Create: `maidc-portal/src/views/system/RoleDetail.vue`

**Design spec (from .pen 0YAz3):**
- **Header**: ← arrow back + "角色详情 - 模型管理员" (22px/600) + "编辑角色" outline button
- **Two-panel layout** (left 520px, right fill):
  - **Left panel — 角色信息 card**:
    - Card header: "角色信息" (15px/600)
    - 4 info rows: 角色编码, 角色名称, 描述, 创建时间
    - Mock: MODEL_ADMIN, 模型管理员, 模型生命周期管理, 2026-01-01
  - **Right panel**:
    - **权限矩阵 card**: header "权限矩阵", table with checkmarks showing CRUD permissions per module
      - Columns: 模块, 查看, 创建, 编辑, 删除
      - 3 rows: 模型管理, 部署管理, 评估管理 (with ✓/✗ checkmarks)
    - **已分配用户(5人) card**: header "已分配用户 (5人)", 3 user entries:
      1. 张医生, 呼吸内科 (blue avatar)
      2. 李医生, 影像科 (green avatar)
      3. 王工程师, AI研发部 (yellow avatar)

**Steps:**
- [ ] Create RoleDetail.vue with two-panel layout
- [ ] Build header with back arrow, title, edit button
- [ ] Build left panel: role info card with 4 description fields
- [ ] Build right panel: permission matrix card with checkmark table + assigned users card
- [ ] Add mock role data, permission matrix, and user list
- [ ] Commit: `feat: add role detail page with permission matrix and users`

---

## Task 5: System Config — Redesign as Categorized Config Cards

**Files:**
- Rewrite: `maidc-portal/src/views/system/SystemConfig.vue`

**Design spec (from .pen nZr4C):**
- **Page header**: "系统配置" (22px/600) + "管理系统全局参数与基础配置" subtitle (14px) — no extra buttons
- **4 config cards** (stacked vertically):
  1. **基础配置** (settings icon, 16px/600):
     - 系统名称: MAIDC医疗AI数据中心 → 编辑
     - 系统版本: v1.0.0 → 编辑
     - 默认语言: 中文 → 编辑
     - 会话超时: 30分钟 → 编辑
  2. **存储配置** (hard-drive icon):
     - MinIO地址: http://minio:9000 → 编辑
     - 默认Bucket: maidc → 编辑
     - 最大上传: 2GB → 编辑
  3. **安全配置** (shield icon):
     - 密码最小长度: 8位 → 编辑
     - 两步验证: 开启 → 编辑
     - 登录失败锁定: 5次 → 编辑
     - 锁定时间: 24小时 → 编辑
  4. **通知配置** (bell icon):
     - SMTP服务器: smtp.example.com → 编辑
     - SMS服务: 阿里云SMS → 编辑
     - Webhook: https://hook.example.com → 编辑
- Each card: header row with icon + title, then content rows with label (left 180px) + value (center) + "编辑" link (right)
- **Edit interaction**: Clicking "编辑" opens inline edit or a modal for that single config item

**Current code**: Table with SearchForm + add/edit config modal, API data.

**Steps:**
- [ ] Remove SearchForm, table, and config modal
- [ ] Create page header with title + subtitle (no buttons)
- [ ] Build 4 categorized config cards with icons (using SettingOutlined, DatabaseOutlined, SafetyCertificateOutlined, BellOutlined)
- [ ] Each card has header (icon + title) + rows (label + value + edit link)
- [ ] Replace API data with mock config data organized by category
- [ ] Add inline edit functionality: clicking "编辑" toggles input mode for that row
- [ ] Commit: `feat: redesign system config as categorized config cards`

---

## Task 6: Organization Management — New Two-Panel Tree + Detail Page

**Files:**
- Create: `maidc-portal/src/views/system/OrganizationList.vue`

**Design spec (from .pen oSRs1):**
- **Page header**: "组织管理" (22px/600) + "管理医院、科室与部门组织架构" subtitle (14px) + "新增组织" primary button
- **Two-panel layout**:
  - **Left panel (280px) — 组织树 card**:
    - Search input at top: "搜索组织..."
    - Tree hierarchy:
      - XX医院 (root, bold, with chevron-down)
        - 内科 (building icon)
        - 外科 (building icon)
        - 放射科 (active/selected, with sub-items):
          - CT室
          - MRI室
        - 检验科 (building icon)
        - 病理科 (building icon)
    - Active item has blue background tint
  - **Right panel (fill) — 组织详情 card**:
    - Header: "放射科" (18px/600) + "放射诊断科室，含CT室、MRI室" subtitle + "编辑" icon button
    - **基本信息 section**: 4 info rows (2-column grid) showing 编码, 类型, 负责人, 联系方式
    - **下级组织 section**: "下级组织（2个）" header, list of sub-organizations with edit buttons

**Steps:**
- [ ] Create OrganizationList.vue with two-panel layout
- [ ] Build left panel: search input + tree structure using a-tree or custom list
- [ ] Build right panel: selected org detail with info section + sub-org section
- [ ] Add mock org tree data (hospital → departments → sub-departments)
- [ ] Add click handler to select tree item and display details
- [ ] Style active tree item with blue background
- [ ] Commit: `feat: add organization management with tree and detail panels`

---

## Task 7: Organization Detail — New Page with Tabs

**Files:**
- Create: `maidc-portal/src/views/system/OrganizationDetail.vue`

**Design spec (from .pen EXQK4):**
- **Header**: "组织详情" (20px/600) + "← 返回列表" link
- **Info card**: 
  - Title: "北京协和医院" (18px/600) + green "已连接" status badge
  - 3 info rows (pipe-separated):
    - 编码: BJXH-001 | 类型: 三级甲等 | 地区: 北京市东城区
    - HIS: 卫宁健康 | PACS: 锐珂医疗 | 床位: 2000
    - 地址: 北京市东城区帅府园1号 | 电话: 010-69156699
- **Tab bar** (3 tabs): 关联科室(active/underline) | 数据源配置 | 系统用户
- **Tab 1 content — 关联科室**:
  - Header: "科室列表 (共 28 个)" + "添加科室" button
  - Table (6 columns): 科室名称, 科室编码, 负责人, 医生数, 数据源, 操作
  - 4 mock rows:
    1. 放射科, DEPT-FS-001, 张主任, 45, HIS/PACS(blue), 编辑
    2. 心内科, DEPT-XN-001, 李主任, 32, HIS/LIS(blue), 编辑
    3. 呼吸内科, DEPT-HX-001, 王主任, 28, HIS(blue), 编辑
    4. 病理科, DEPT-BL-001, 赵主任, 15, HIS/PACS(blue), 编辑
  - 数据源 column values shown in blue

**Steps:**
- [ ] Create OrganizationDetail.vue with info card + tabs
- [ ] Build header with title + back link
- [ ] Build info card with hospital name, status badge, and 3 detail rows
- [ ] Build 3 tabs: 关联科室, 数据源配置, 系统用户
- [ ] Implement 关联科室 tab with department table and mock data
- [ ] Add placeholder content for other 2 tabs
- [ ] Commit: `feat: add organization detail page with department table`

---

## Task 8: Permission Management — New Hierarchical Table Page

**Files:**
- Create: `maidc-portal/src/views/system/PermissionManagement.vue`

**Design spec (from .pen suAVK):**
- **Page header**: "权限管理" (20px/600) + "新增权限" primary button
- **Description text**: "管理菜单权限、API权限和数据权限。权限通过角色分配给用户。" (13px, muted)
- **Filter row**: 类型 dropdown ("类型: 全部 ▼") + 搜索 input ("搜索权限名称...")
- **Table** (5 columns): 权限名称, 编码, 类型, 关联角色数, 操作
  - **Type badges** (small colored tags):
    - MENU → blue (#1677FF)
    - API → purple (#8b5cf6)
    - DATA → orange (#f59e0b)
    - BUTTON → green (#10b981)
  - **Hierarchical display** with folder/document icons:
    - Parent items (MENU type): 📂 prefix, bold, no "编辑" link (has expand/collapse icon)
    - Child items: indented with 📄 or 🔧 prefix, lighter background (#f8fafc)
  - 关联角色数: number or "—" for parent items
- **Mock data** (hierarchical, 3 groups):
  - 📂 模型管理 (model:*, MENU, —)
    - 📄 模型列表 (model:list, MENU, 6, 编辑)
    - 🔧 注册模型 (model:create, API, 3, 编辑)
    - 🔧 删除模型 (model:delete, API, 2, 编辑)
  - 📂 数据管理 (data:*, MENU, —)
    - 🔧 患者数据查看 (data:patient:read, DATA, 4, 编辑)
    - 🔧 数据导出 (data:export, BUTTON, 2, 编辑)
  - 📂 系统设置 (system:*, MENU, —)
- **Pagination**: "共 48 个权限项"

**Steps:**
- [ ] Create PermissionManagement.vue with header, description, filter row
- [ ] Build hierarchical table with expandable rows using a-table children/indentation
- [ ] Add type badges with correct colors: MENU=blue, API=purple, DATA=orange, BUTTON=green
- [ ] Add folder/document icons for hierarchy (📂 for parents, 📄/🔧 for children)
- [ ] Add mock permission data in hierarchical structure
- [ ] Add pagination: "共 48 个权限项"
- [ ] Commit: `feat: add permission management with hierarchical table`

---

## Task 9: Router Updates + Build Verification

**Files:**
- Modify: `maidc-portal/src/router/asyncRoutes.ts`

### Router Updates

Add to system children in `asyncRoutes.ts`:
```typescript
{ path: 'users/:id', name: 'UserDetail', meta: { title: '用户详情', hidden: true }, component: () => import('@/views/system/UserDetail.vue') },
{ path: 'roles/:id', name: 'RoleDetail', meta: { title: '角色详情', hidden: true }, component: () => import('@/views/system/RoleDetail.vue') },
{ path: 'organizations', name: 'OrganizationList', meta: { title: '组织管理' }, component: () => import('@/views/system/OrganizationList.vue') },
{ path: 'organizations/:id', name: 'OrganizationDetail', meta: { title: '组织详情', hidden: true }, component: () => import('@/views/system/OrganizationDetail.vue') },
{ path: 'permissions', name: 'PermissionManagement', meta: { title: '权限管理' }, component: () => import('@/views/system/PermissionManagement.vue') },
```

### Build Verification

- [ ] Add all new routes to asyncRoutes.ts under system children
- [ ] Run `npx vite build` from maidc-portal — must pass
- [ ] Verify all system settings pages render without errors
- [ ] Commit: `feat: add routes and verify build for system settings pages`

---

## Scope Note

The following are NOT included (deferred):
- Real-time organization sync with external HIS/PACS — mock data only
- Permission cascade logic (inheriting from parent) — visual display only
- Config change audit trail — deferred
- Role-based data row filtering — backend concern
