# MAIDC UI集成测试执行计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 使用 chrome-devtools MCP 工具执行 MAIDC 系统全部 43 页面 149 个操作点的 UI 集成测试，验证每个按钮、表单、跳转的 API 调用和返回结果。

**Architecture:** 按模块分10个测试任务，每个任务内按页面组织。每个测试步骤使用 chrome-devtools MCP 的 `navigate_page` → `take_snapshot` → `click`/`fill` → `list_network_requests` + `get_network_request` 流程执行。测试结果实时写入报告文件。

**Tech Stack:** chrome-devtools MCP (take_snapshot, click, fill, navigate_page, list_network_requests, get_network_request, evaluate_script, take_screenshot)

**Spec:** `docs/superpowers/specs/2026-04-12-maidc-ui-test-plan-design.md`

---

## 执行约定

### 测试步骤模板

每个测试点的执行流程：

```
1. navigate_page → 到目标页面（如已在当前页则跳过）
2. take_snapshot → 获取页面元素 uid
3. click/fill → 执行操作（通过 snapshot 获取的 uid）
4. list_network_requests → 确认 API 请求已发出
5. get_network_request → 获取响应，校验 status code + body
6. take_snapshot → 校验 UI 状态变化
7. evaluate_script → 校验 URL 变化（如需要）
```

### 结果记录格式

每个测试点记录到报告文件 `docs/superpowers/test-results/YYYY-MM-DD-test-report.md`：

```markdown
| 编号 | 状态 | 实际API | 实际响应 | UI结果 | 截图 | 备注 |
|------|------|---------|----------|--------|------|------|
| 1.1.1 | PASS/FAIL | GET /api/xxx | 200 {...} | 符合预期 | path.png | - |
```

### 前置条件

- 前端 dev server 运行在 `http://localhost:5173`
- 后端服务运行在 `http://localhost:8080`
- Chrome 浏览器已打开并连接 chrome-devtools MCP
- 数据库已初始化（包含测试数据）

---

## Task 1: 环境准备与登录测试（7个测试点）

**Files:**
- Create: `docs/superpowers/test-results/2026-04-12-module1-auth.md`

- [ ] **Step 1: 验证 chrome-devtools 连接**

调用 `mcp__chrome-devtools__list_pages` 确认浏览器已连接，获取当前页面列表。

- [ ] **Step 2: 创建测试报告文件**

创建 `docs/superpowers/test-results/2026-04-12-module1-auth.md`，写入报告头部：

```markdown
# 测试报告 - 认证模块

| 编号 | 操作 | 状态 | 实际API | 实际响应 | UI结果 | 截图 | 备注 |
|------|------|------|---------|----------|--------|------|------|
```

- [ ] **Step 3: 测试 1.1.1 - 页面加载**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/login
mcp__chrome-devtools__take_snapshot → 获取页面元素
```

校验：页面包含用户名输入框、密码输入框、登录按钮。
记录结果到报告。

- [ ] **Step 4: 测试 1.1.2 - 空表单提交**

```
mcp__chrome-devtools__take_snapshot → 获取登录按钮 uid
mcp__chrome-devtools__click → uid: [登录按钮uid]
mcp__chrome-devtools__take_snapshot → 检查错误提示
```

校验：显示"请输入用户名/密码"提示。
记录结果。

- [ ] **Step 5: 测试 1.1.3 - 只填用户名**

```
mcp__chrome-devtools__take_snapshot → 获取用户名输入框 uid
mcp__chrome-devtools__fill → uid: [用户名uid], value: "admin"
mcp__chrome-devtools__click → uid: [登录按钮uid]
mcp__chrome-devtools__take_snapshot → 检查错误提示
```

校验：提示"请输入密码"。
记录结果。

- [ ] **Step 6: 测试 1.1.4 - 错误凭据**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/login（刷新表单）
mcp__chrome-devtools__take_snapshot → 获取输入框 uid
mcp__chrome-devtools__fill_form → elements: [{uid: [用户名uid], value: "wrong"}, {uid: [密码uid], value: "wrong"}]
mcp__chrome-devtools__click → uid: [登录按钮uid]
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__get_network_request → 获取 POST /auth/login 的响应
```

校验：响应码 401，body 含 "用户名或密码错误"。UI 显示错误提示。
记录结果。

- [ ] **Step 7: 测试 1.1.5 - 正确登录**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/login（刷新表单）
mcp__chrome-devtools__take_snapshot → 获取输入框 uid
mcp__chrome-devtools__fill_form → elements: [{uid: [用户名uid], value: "admin"}, {uid: [密码uid], value: "admin123"}]
mcp__chrome-devtools__click → uid: [登录按钮uid]
mcp__chrome-devtools__wait_for → text: ["仪表盘", "概览", "dashboard"]
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__get_network_request → 获取 POST /auth/login 的响应
mcp__chrome-devtools__evaluate_script → function: () => window.location.pathname
```

校验：
- API 响应 200，body 含 `token` 和 `user` 字段
- URL 变为 `/dashboard/overview`
- localStorage 存入 token

记录结果。

- [ ] **Step 8: 测试 1.1.6 - 密码显隐切换**

先退出回到登录页：
```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/login
mcp__chrome-devtools__take_snapshot → 获取眼睛图标 uid 和密码输入框 uid
mcp__chrome-devtools__click → uid: [眼睛图标uid]
mcp__chrome-devtools__take_snapshot → 检查密码输入框 type 变化
```

校验：密码框 type 从 password 变为 text（或反之）。
记录结果。

- [ ] **Step 9: 测试 1.1.7 - 退出登录**

先登录：
```
mcp__chrome-devtools__fill_form → elements: [{uid: [用户名uid], value: "admin"}, {uid: [密码uid], value: "admin123"}]
mcp__chrome-devtools__click → uid: [登录按钮uid]
mcp__chrome-devtools__wait_for → text: ["仪表盘"]
```

然后退出：
```
mcp__chrome-devtools__take_snapshot → 获取用户头像/退出按钮 uid
mcp__chrome-devtools__click → uid: [头像uid]
mcp__chrome-devtools__take_snapshot → 获取退出按钮 uid
mcp__chrome-devtools__click → uid: [退出按钮uid]
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__get_network_request → 获取 POST /auth/logout 响应
mcp__chrome-devtools__evaluate_script → function: () => window.location.pathname
mcp__chrome-devtools__evaluate_script → function: () => localStorage.getItem('token')
```

校验：
- API 响应 200
- URL 变为 `/login`
- localStorage token 已清空

记录结果。

- [ ] **Step 10: 截图留档**

```
mcp__chrome-devtools__take_screenshot → filePath: docs/superpowers/test-results/screenshots/1.1-login.png
```

- [ ] **Step 11: 提交 Task 1 报告**

将完整测试报告写入文件，统计 PASS/FAIL 数量。
提交 git commit。

---

## Task 2: 系统管理模块测试（18个测试点）

**前置:** 已通过 Task 1 登录 admin 账号。

**Files:**
- Create: `docs/superpowers/test-results/2026-04-12-module2-system.md`

- [ ] **Step 1: 创建报告文件并登录**

创建 `docs/superpowers/test-results/2026-04-12-module2-system.md` 报告头部。
登录 admin 账号（复用 Task 1 的登录流程）。

- [ ] **Step 2: 测试 2.1.1 - 用户列表页面加载**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/system/users
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__get_network_request → 获取 GET /api/v1/users 响应
mcp__chrome-devtools__take_snapshot → 检查表格和搜索栏
```

校验：API 200，body 含 `content` 数组和 `totalElements`。UI 显示用户表格。
记录结果。

- [ ] **Step 3: 测试 2.1.2 - 搜索用户**

```
mcp__chrome-devtools__take_snapshot → 获取搜索框 uid
mcp__chrome-devtools__fill → uid: [搜索框uid], value: "admin"
mcp__chrome-devtools__take_snapshot → 获取搜索按钮 uid
mcp__chrome-devtools__click → uid: [搜索按钮uid]
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__get_network_request → 获取带 keyword 参数的请求响应
mcp__chrome-devtools__take_snapshot → 检查过滤结果
```

校验：API 请求含 `keyword=admin` 参数，表格只显示匹配结果。
记录结果。

- [ ] **Step 4: 测试 2.1.3 - 新建用户弹窗**

```
mcp__chrome-devtools__take_snapshot → 获取"新建"按钮 uid
mcp__chrome-devtools__click → uid: [新建按钮uid]
mcp__chrome-devtools__take_snapshot → 检查弹窗元素
```

校验：弹出新建用户表单弹窗，含 username/name/email/phone/role 字段。
记录结果。

- [ ] **Step 5: 测试 2.1.4 - 新建用户提交**

```
mcp__chrome-devtools__take_snapshot → 获取表单字段 uid
mcp__chrome-devtools__fill_form → elements: [{uid: [username uid], value: "testuser"}, {uid: [name uid], value: "测试用户"}, {uid: [email uid], value: "test@test.com"}]
mcp__chrome-devtools__take_snapshot → 获取确定按钮 uid
mcp__chrome-devtools__click → uid: [确定按钮uid]
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__get_network_request → 获取 POST /api/v1/users 响应
mcp__chrome-devtools__take_snapshot → 检查弹窗关闭和列表刷新
```

校验：API 200，body 含新用户 `id`。弹窗关闭，提示"创建成功"。
记录结果。

- [ ] **Step 6: 测试 2.1.5 - 新建用户用户名重复**

```
mcp__chrome-devtools__take_snapshot → 获取"新建"按钮 uid
mcp__chrome-devtools__click → uid: [新建按钮uid]
mcp__chrome-devtools__take_snapshot → 获取表单字段 uid
mcp__chrome-devtools__fill_form → elements: [{uid: [username uid], value: "admin"}]
mcp__chrome-devtools__click → uid: [确定按钮uid]
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__get_network_request → 获取 POST /api/v1/users 响应
mcp__chrome-devtools__take_snapshot → 检查错误提示
```

校验：API 409，body 含"用户名已存在"。UI 显示错误。
记录结果。

- [ ] **Step 7: 测试 2.1.6 - 新建用户必填项为空**

```
mcp__chrome-devtools__take_snapshot → 获取"新建"按钮 uid
mcp__chrome-devtools__click → uid: [新建按钮uid]
mcp__chrome-devtools__take_snapshot → 获取确定按钮 uid
mcp__chrome-devtools__click → uid: [确定按钮uid]
mcp__chrome-devtools__take_snapshot → 检查必填校验
```

校验：username 字段标红，提示"必填"。无 API 请求发出。
记录结果。

- [ ] **Step 8: 测试 2.1.7 - 查看用户详情**

```
mcp__chrome-devtools__take_snapshot → 获取第一行"查看"按钮 uid
mcp__chrome-devtools__click → uid: [查看按钮uid]
mcp__chrome-devtools__evaluate_script → function: () => window.location.pathname
```

校验：URL 变为 `/system/users/{id}`。
记录结果。

- [ ] **Step 9: 测试 2.2.1-2.2.2 - 用户详情页**

在详情页：
```
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__get_network_request → 获取 GET /api/v1/users/{id} 响应
mcp__chrome-devtools__take_snapshot → 检查详情卡片
```

校验：API 200，body 含用户详细信息。UI 显示详情卡片。
点击返回：
```
mcp__chrome-devtools__take_snapshot → 获取返回按钮 uid
mcp__chrome-devtools__click → uid: [返回按钮uid]
mcp__chrome-devtools__evaluate_script → function: () => window.location.pathname
```

校验：URL 变回 `/system/users`。
记录结果。

- [ ] **Step 10: 测试 2.1.8 - 编辑用户**

```
mcp__chrome-devtools__take_snapshot → 获取第一行"编辑"按钮 uid
mcp__chrome-devtools__click → uid: [编辑按钮uid]
mcp__chrome-devtools__take_snapshot → 获取编辑表单字段 uid
mcp__chrome-devtools__fill → uid: [email uid], value: "updated@test.com"
mcp__chrome-devtools__take_snapshot → 获取确定按钮 uid
mcp__chrome-devtools__click → uid: [确定按钮uid]
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__get_network_request → 获取 PUT /api/v1/users/{id} 响应
mcp__chrome-devtools__take_snapshot → 检查列表刷新
```

校验：API 200。列表刷新。
记录结果。

- [ ] **Step 11: 测试 2.1.9 - 重置密码**

```
mcp__chrome-devtools__take_snapshot → 获取"重置密码"按钮 uid
mcp__chrome-devtools__click → uid: [重置密码按钮uid]
mcp__chrome-devtools__take_snapshot → 获取确认弹窗的确定按钮 uid
mcp__chrome-devtools__click → uid: [确定按钮uid]
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__get_network_request → 获取 PUT /api/v1/users/{id}/reset-password 响应
mcp__chrome-devtools__take_snapshot → 检查成功提示
```

校验：API 200。提示"密码已重置"。
记录结果。

- [ ] **Step 12: 测试 2.1.10 - 分页切换**

```
mcp__chrome-devtools__take_snapshot → 获取第2页按钮 uid
mcp__chrome-devtools__click → uid: [第2页uid]
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__get_network_request → 获取 GET /api/v1/users?page=2 响应
mcp__chrome-devtools__take_snapshot → 检查分页高亮
```

校验：API 请求含 `page=2`，表格数据更新。
记录结果。

- [ ] **Step 13: 测试 2.3.1-2.3.4 - 角色管理**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/system/roles
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__get_network_request → 获取 GET /api/v1/roles 响应
mcp__chrome-devtools__take_snapshot → 检查角色列表
```

逐个测试：新建角色、编辑角色、分配权限。每个操作记录 API 调用和响应。
记录结果。

- [ ] **Step 14: 测试 2.4.1-2.4.2 - 权限树**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/system/permissions
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__get_network_request → 获取 GET /api/v1/permissions/tree 响应
mcp__chrome-devtools__take_snapshot → 检查树形结构
```

测试展开/折叠节点。
记录结果。

- [ ] **Step 15: 测试 2.5.1-2.5.5 - 机构管理**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/system/organizations
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__take_snapshot → 检查机构列表
```

逐个测试：加载、新建、查看详情、编辑、删除。每个操作记录 API 和响应。
记录结果。

- [ ] **Step 16: 测试 2.6.1-2.6.2 - 系统配置**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/system/config
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__take_snapshot → 检查配置表单
```

测试加载配置、修改值并保存。
记录结果。

- [ ] **Step 17: 截图留档**

为关键页面截图：
```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/system/users
mcp__chrome-devtools__take_screenshot → filePath: docs/superpowers/test-results/screenshots/2.1-user-list.png
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/system/roles
mcp__chrome-devtools__take_screenshot → filePath: docs/superpowers/test-results/screenshots/2.3-role-list.png
```

- [ ] **Step 18: 提交 Task 2 报告**

统计 PASS/FAIL，提交 git commit。

---

## Task 3: 仪表盘模块测试（5个测试点）

**前置:** 已登录 admin 账号。

**Files:**
- Create: `docs/superpowers/test-results/2026-04-12-module3-dashboard.md`

- [ ] **Step 1: 创建报告文件**

创建 `docs/superpowers/test-results/2026-04-12-module3-dashboard.md`。

- [ ] **Step 2: 测试 3.1.1 - 系统总览页面加载**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/dashboard/overview
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__get_network_request → 获取统计 API 响应
mcp__chrome-devtools__take_snapshot → 检查统计卡片和图表
mcp__chrome-devtools__take_screenshot → filePath: docs/superpowers/test-results/screenshots/3.1-overview.png
```

校验：API 200。UI 显示统计卡片（用户数/模型数/数据量/任务数）和趋势图表。
记录结果。

- [ ] **Step 3: 测试 3.1.2 - 时间范围切换**

```
mcp__chrome-devtools__take_snapshot → 获取"近30天"按钮 uid
mcp__chrome-devtools__click → uid: [近30天按钮uid]
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__get_network_request → 获取带 range 参数的请求
mcp__chrome-devtools__take_snapshot → 检查图表更新
```

校验：API 请求含时间范围参数，图表数据更新。
记录结果。

- [ ] **Step 4: 测试 3.2.1-3.2.2 - 模型看板**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/dashboard/model
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__get_network_request → 获取 GET /api/v1/models 和 deployments 响应
mcp__chrome-devtools__take_snapshot → 检查模型统计和部署状态
```

点击模型卡片，校验跳转到模型详情页。
记录结果。

- [ ] **Step 5: 测试 3.3.1-3.3.2 - 数据看板**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/dashboard/data
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__take_snapshot → 检查数据统计图表
```

点击数据卡片，校验跳转。
记录结果。

- [ ] **Step 6: 提交 Task 3 报告**

统计 PASS/FAIL，提交 git commit。

---

## Task 4: 模型管理模块测试（36个测试点）

**前置:** 已登录 admin 账号。

**Files:**
- Create: `docs/superpowers/test-results/2026-04-12-module4-model.md`

这是最大的模块（36个测试点，8个页面），按页面分步执行。

- [ ] **Step 1: 创建报告文件**

创建 `docs/superpowers/test-results/2026-04-12-module4-model.md`。

- [ ] **Step 2: 测试 4.1.1-4.1.3 - 模型列表加载/搜索/筛选**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/model/list
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__get_network_request → 获取 GET /api/v1/models 响应
mcp__chrome-devtools__take_snapshot → 检查模型表格
```

搜索模型：
```
mcp__chrome-devtools__take_snapshot → 获取搜索框 uid
mcp__chrome-devtools__fill → uid: [搜索框uid], value: "test"
mcp__chrome-devtools__click → uid: [搜索按钮uid]
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__get_network_request → 获取带 name 参数的请求
```

状态筛选：
```
mcp__chrome-devtools__take_snapshot → 获取状态下拉 uid
mcp__chrome-devtools__fill → uid: [状态uid], value: "DEPLOYED"
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
```

每个操作记录结果。

- [ ] **Step 3: 测试 4.1.4-4.1.6 - 新建模型（含异常）**

新建弹窗：
```
mcp__chrome-devtools__take_snapshot → 获取"新建"按钮 uid
mcp__chrome-devtools__click → uid: [新建按钮uid]
mcp__chrome-devtools__take_snapshot → 检查弹窗
```

必填校验：
```
mcp__chrome-devtools__click → uid: [确定按钮uid]
mcp__chrome-devtools__take_snapshot → 检查校验提示
```

正常提交：
```
mcp__chrome-devtools__fill_form → elements: [{uid: [name uid], value: "测试模型"}, {uid: [type uid], value: "CLASSIFICATION"}, {uid: [desc uid], value: "测试描述"}]
mcp__chrome-devtools__click → uid: [确定按钮uid]
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__get_network_request → 获取 POST /api/v1/models 响应
```

每个操作记录结果。

- [ ] **Step 4: 测试 4.1.7-4.1.10 - 查看/编辑/删除模型**

查看详情（URL跳转校验）：
```
mcp__chrome-devtools__take_snapshot → 获取第一行"查看"按钮 uid
mcp__chrome-devtools__click → uid: [查看按钮uid]
mcp__chrome-devtools__evaluate_script → function: () => window.location.pathname
```

返回列表后测试编辑：
```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/model/list
mcp__chrome-devtools__take_snapshot → 获取"编辑"按钮 uid
mcp__chrome-devtools__click → uid: [编辑按钮uid]
# 修改字段 → 确定 → 校验 PUT 请求
```

删除-取消（确认无操作）：
```
mcp__chrome-devtools__take_snapshot → 获取"删除"按钮 uid
mcp__chrome-devtools__click → uid: [删除按钮uid]
mcp__chrome-devtools__take_snapshot → 获取"取消"按钮 uid
mcp__chrome-devtools__click → uid: [取消按钮uid]
mcp__chrome-devtools__take_snapshot → 校验列表不变
```

每个操作记录结果。

- [ ] **Step 5: 测试 4.2.1-4.2.6 - 模型详情页**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/model/list
mcp__chrome-devtools__take_snapshot → 获取第一行模型名称 uid
mcp__chrome-devtools__click → uid: [模型名uid]
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__get_network_request → 获取 GET /api/v1/models/{id} 响应
mcp__chrome-devtools__take_snapshot → 检查详情页 + Tab
```

测试各 Tab（版本/评估/部署/审批），上传、下载、对比等操作。
每个操作记录结果。

- [ ] **Step 6: 测试 4.3.1-4.3.8 - 部署列表**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/model/deployments
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__take_snapshot → 检查部署列表
```

逐个测试：新建部署、启动、停止、扩缩容、重启、查看日志、查看指标。
每个操作记录 API 调用和响应。

- [ ] **Step 7: 测试 4.4.1-4.4.3 - 部署详情（含在线推理）**

```
mcp__chrome-devtools__take_snapshot → 获取第一行部署的"查看"按钮 uid
mcp__chrome-devtools__click → uid: [查看按钮uid]
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__get_network_request → 获取 GET /api/v1/deployments/{id}/status 响应
mcp__chrome-devtools__take_snapshot → 检查详情
```

测试在线推理（正常+空参数）。

- [ ] **Step 8: 测试 4.5.1-4.5.5 - 审批列表**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/model/approvals
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
```

测试：提交审批、审批通过、审批驳回、查看审批详情。
每个操作记录 API 和响应。

- [ ] **Step 9: 测试 4.6.1-4.6.4 - 评估列表**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/model/evaluations
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
```

测试：加载列表、新建评估、查看报告（含混淆矩阵和ROC曲线）。

- [ ] **Step 10: 测试 4.7.1-4.7.4 - 路由配置**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/model/routes
```

测试：加载、添加规则、编辑规则、删除规则。

- [ ] **Step 11: 测试 4.8.1-4.8.2 - 版本管理**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/model/versions
```

测试：加载、版本对比。

- [ ] **Step 12: 截图留档**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/model/list
mcp__chrome-devtools__take_screenshot → filePath: docs/superpowers/test-results/screenshots/4.1-model-list.png
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/model/deployments
mcp__chrome-devtools__take_screenshot → filePath: docs/superpowers/test-results/screenshots/4.3-deployment-list.png
```

- [ ] **Step 13: 提交 Task 4 报告**

统计 PASS/FAIL，提交 git commit。

---

## Task 5: 数据管理模块测试（32个测试点）

**前置:** 已登录 admin 账号。

**Files:**
- Create: `docs/superpowers/test-results/2026-04-12-module5-data.md`

- [ ] **Step 1: 创建报告文件**

创建 `docs/superpowers/test-results/2026-04-12-module5-data.md`。

- [ ] **Step 2: 测试 5.1.1-5.1.5 - 患者列表**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/data/cdr/patients
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__get_network_request → 获取 GET /api/v1/cdr/patients 响应
mcp__chrome-devtools__take_snapshot → 检查患者表格
```

测试：搜索、查看详情（URL跳转）、360视图、分页。
每个操作记录结果。

- [ ] **Step 3: 测试 5.2.1-5.2.4 - 患者详情**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/data/cdr/patients
mcp__chrome-devtools__take_snapshot → 获取第一行"查看"按钮 uid
mcp__chrome-devtools__click → uid: [查看按钮uid]
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__get_network_request → 获取 GET /api/v1/cdr/patients/{id} 响应
```

测试：加载详情、就诊记录Tab、就诊详情跳转、返回。

- [ ] **Step 4: 测试 5.3-5.4 - 就诊详情 + 数据源管理**

就诊详情：
```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/data/cdr/patients
# 找一个患者 → 进入详情 → 点就诊 → 进入就诊详情
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__get_network_request → 获取 GET /api/v1/cdr/encounters/{id}
```

数据源管理：
```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/data/cdr/datasources
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
```

测试：加载数据源、新建、测试连接（成功+失败）、查看详情。

- [ ] **Step 5: 测试 5.5-5.6 - 数据源详情 + 同步任务**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/data/cdr/sync
```

测试：加载同步任务、触发同步、查看日志。

- [ ] **Step 6: 测试 5.7-5.8 - 质量规则 + 质量结果**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/data/cdr/quality-rules
```

测试：加载规则列表、新建、编辑、删除。

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/data/cdr/quality-results
```

测试：加载结果、按状态筛选、查看详情。

- [ ] **Step 7: 测试 5.9 - 脱敏规则**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/data/cdr/desensitize
```

测试：加载规则、新建、预览脱敏效果、编辑/删除。

- [ ] **Step 8: 测试 5.10 - 字典管理**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/data/cdr/dict
```

测试：加载字典、新增、编辑、删除。

- [ ] **Step 9: 测试 5.11-5.12 - 科研项目 + 数据集**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/data/rdr/projects
```

测试：加载项目、新建、添加/移除成员、删除项目。

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/data/rdr/datasets
```

测试：加载数据集、新建、查看详情、删除。

- [ ] **Step 10: 测试 5.13 - ETL任务**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/data/rdr/etl
```

测试：加载ETL任务、新建、触发执行、暂停、删除。

- [ ] **Step 11: 截图留档**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/data/cdr/patients
mcp__chrome-devtools__take_screenshot → filePath: docs/superpowers/test-results/screenshots/5.1-patient-list.png
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/data/rdr/projects
mcp__chrome-devtools__take_screenshot → filePath: docs/superpowers/test-results/screenshots/5.11-project-list.png
```

- [ ] **Step 12: 提交 Task 5 报告**

统计 PASS/FAIL，提交 git commit。

---

## Task 6: 标注管理模块测试（11个测试点）

**前置:** 已登录 admin 账号。

**Files:**
- Create: `docs/superpowers/test-results/2026-04-12-module6-label.md`

- [ ] **Step 1: 创建报告文件**

创建 `docs/superpowers/test-results/2026-04-12-module6-label.md`。

- [ ] **Step 2: 测试 6.1.1-6.1.7 - 标注任务列表**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/label/tasks
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__get_network_request → 获取 GET /api/v1/label/tasks 响应
mcp__chrome-devtools__take_snapshot → 检查任务列表
```

逐个测试：新建、编辑、删除、查看统计、AI预标注、进入工作台（URL跳转）。
每个操作记录结果。

- [ ] **Step 3: 测试 6.2.1-6.2.6 - 标注工作台**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/label/tasks
# 找一个任务 → 点击"标注" → 进入工作台
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__take_snapshot → 检查工作台（画布+工具栏+属性面板）
```

测试：选择工具、执行标注、保存、提交审核、撤销/重做。

- [ ] **Step 4: 截图留档 + 提交报告**

```
mcp__chrome-devtools__take_screenshot → filePath: docs/superpowers/test-results/screenshots/6.1-label-tasks.png
mcp__chrome-devtools__take_screenshot → filePath: docs/superpowers/test-results/screenshots/6.2-label-workspace.png
```

统计 PASS/FAIL，提交 git commit。

---

## Task 7: 任务调度模块测试（8个测试点）

**前置:** 已登录 admin 账号。

**Files:**
- Create: `docs/superpowers/test-results/2026-04-12-module7-schedule.md`

- [ ] **Step 1: 创建报告文件**

创建 `docs/superpowers/test-results/2026-04-12-module7-schedule.md`。

- [ ] **Step 2: 测试 7.1.1-7.1.8 - 任务列表全操作**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/schedule/tasks
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__get_network_request → 获取 GET /api/v1/task/schedules 响应
mcp__chrome-devtools__take_snapshot → 检查任务列表
```

逐个测试8个操作点：新建、编辑、删除、手动触发、暂停、恢复、查看执行历史。
每个操作记录 API 调用和响应。

- [ ] **Step 3: 截图留档 + 提交报告**

```
mcp__chrome-devtools__take_screenshot → filePath: docs/superpowers/test-results/screenshots/7.1-schedule-tasks.png
```

统计 PASS/FAIL，提交 git commit。

---

## Task 8: 告警中心模块测试（9个测试点）

**前置:** 已登录 admin 账号。

**Files:**
- Create: `docs/superpowers/test-results/2026-04-12-module8-alert.md`

- [ ] **Step 1: 创建报告文件**

创建 `docs/superpowers/test-results/2026-04-12-module8-alert.md`。

- [ ] **Step 2: 测试 8.1.1-8.1.5 - 活跃告警**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/alert/active
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__get_network_request → 获取 GET /api/v1/alerts 响应
mcp__chrome-devtools__take_snapshot → 检查告警列表（级别标签）
```

测试：按级别筛选、确认告警、查看详情（URL跳转）、告警历史。

- [ ] **Step 3: 测试 8.2.1-8.2.4 - 告警规则**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/alert/rules
```

测试：加载规则、新建规则、编辑规则、启用/禁用开关。

- [ ] **Step 4: 测试 8.3.1-8.3.2 - 告警详情**

```
# 从活跃告警列表点击某条 → 进入详情页
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__get_network_request → 获取告警详情 API 响应
mcp__chrome-devtools__take_snapshot → 检查详情+时间线
```

测试返回列表。

- [ ] **Step 5: 截图留档 + 提交报告**

截图并提交 git commit。

---

## Task 9: 审计日志模块测试（10个测试点）

**前置:** 已登录 admin 账号。

**Files:**
- Create: `docs/superpowers/test-results/2026-04-12-module9-audit.md`

- [ ] **Step 1: 创建报告文件**

创建 `docs/superpowers/test-results/2026-04-12-module9-audit.md`。

- [ ] **Step 2: 测试 9.1.1-9.1.4 - 操作日志**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/audit/operations
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__get_network_request → 获取 GET /api/v1/audit/operations 响应
```

测试：加载、按时间筛选、按类型筛选、查看详情抽屉。

- [ ] **Step 3: 测试 9.2.1-9.2.3 - 数据访问日志**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/audit/data-access
```

测试：加载、按用户筛选、按数据类型筛选。

- [ ] **Step 4: 测试 9.3.1-9.3.2 - 系统事件**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/audit/system-events
```

测试：加载、按级别筛选。

- [ ] **Step 5: 测试 9.4.1-9.4.3 - 合规报告**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/audit/compliance
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__get_network_request → 获取 GET /api/v1/audit/reports/compliance 响应
mcp__chrome-devtools__take_snapshot → 检查合规评分+检查项+趋势图
```

测试：加载报告、切换时间范围、导出报告。

- [ ] **Step 6: 截图留档 + 提交报告**

截图并提交 git commit。

---

## Task 10: 消息中心模块测试（13个测试点）

**前置:** 已登录 admin 账号。

**Files:**
- Create: `docs/superpowers/test-results/2026-04-12-module10-message.md`

- [ ] **Step 1: 创建报告文件**

创建 `docs/superpowers/test-results/2026-04-12-module10-message.md`。

- [ ] **Step 2: 测试 10.1.1-10.1.6 - 消息列表**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/message/list
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__get_network_request → 获取 GET /api/v1/messages 响应
mcp__chrome-devtools__take_snapshot → 检查消息列表（已读/未读样式）
```

逐个测试：标记已读、全部已读、未读数badge、查看详情（URL跳转）、按类型筛选。

- [ ] **Step 3: 测试 10.2.1-10.2.2 - 消息详情**

```
# 从消息列表点击某条消息 → 进入详情
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
mcp__chrome-devtools__get_network_request → 获取 GET /api/v1/messages/{id} 响应
mcp__chrome-devtools__take_snapshot → 检查消息详情
```

测试返回列表。

- [ ] **Step 4: 测试 10.3.1-10.3.3 - 通知设置**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/message/settings
mcp__chrome-devtools__list_network_requests → resourceTypes: ["fetch","xhr"]
```

测试：加载设置、切换开关、新增设置。

- [ ] **Step 5: 测试 10.4.1-10.4.4 - 模板管理**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/message/templates
```

测试：加载模板、新建模板、编辑模板、预览模板。

- [ ] **Step 6: 截图留档 + 提交报告**

截图并提交 git commit。

---

## Task 11: 汇总报告 + 错误页面测试

**Files:**
- Create: `docs/superpowers/test-results/2026-04-12-summary.md`
- Modify: `docs/superpowers/specs/2026-04-12-maidc-ui-test-plan-design.md`（更新汇总表）

- [ ] **Step 1: 测试 E.1-E.3 - 错误页面**

```
mcp__chrome-devtools__navigate_page → url: http://localhost:5173/nonexistent-page-404
mcp__chrome-devtools__take_snapshot → 检查404页面
mcp__chrome-devtools__take_screenshot → filePath: docs/superpowers/test-results/screenshots/E.2-404.png
```

类似测试 403 和 500 页面。

- [ ] **Step 2: 汇总所有模块结果**

读取所有模块报告文件，统计总 PASS/FAIL/未执行 数量。
创建汇总报告 `docs/superpowers/test-results/2026-04-12-summary.md`：

```markdown
# MAIDC UI集成测试汇总报告

## 总体统计
| 指标 | 数值 |
|------|------|
| 总测试点 | 149 |
| 通过 | X |
| 失败 | Y |
| 通过率 | Z% |

## 各模块结果
| 模块 | 通过 | 失败 | 通过率 |
|------|------|------|--------|
...

## 失败项清单
| 编号 | 模块 | 操作 | 失败原因 | 截图 |
...

## 建议
...
```

- [ ] **Step 3: 更新 spec 文件汇总表**

更新 `docs/superpowers/specs/2026-04-12-maidc-ui-test-plan-design.md` 中的"执行状态汇总表"，填入实际 PASS/FAIL 数据。

- [ ] **Step 4: 最终提交**

```bash
git add docs/superpowers/test-results/
git commit -m "test: complete MAIDC UI integration test - X passed, Y failed out of 149"
```

---

## 附录：通用校验检查表

每个列表页执行时均检查：

- [ ] 加载态：首次加载显示 loading/骨架屏
- [ ] 空状态：无数据时显示 EmptyState 组件
- [ ] 分页功能正常
- [ ] 搜索功能正常
- [ ] CRUD后列表自动刷新

每个表单页执行时均检查：

- [ ] 必填项为空时提交标红提示
- [ ] 格式校验（邮箱/手机号等）
- [ ] 提交成功：弹窗关闭+提示
- [ ] 提交失败：显示错误信息
- [ ] 取消操作不提交

每个删除操作执行时均检查：

- [ ] 弹出确认对话框
- [ ] 确认后删除成功
- [ ] 取消后无操作
