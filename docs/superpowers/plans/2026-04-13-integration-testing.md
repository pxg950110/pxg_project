# 全局联调+交互逻辑 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 启动前后端服务，将14个mock页面全部替换为真实API调用，实现完整的CRUD交互和菜单导航。

**Architecture:** 分5层逐步联调：L1基础设施→L2认证→L3系统管理→L4模型管理→L5其余模块。每层独立验证后再进入下一层。前端统一使用 `useTable` hook + API模块的模式替换mock数据。

**Tech Stack:** Docker Compose, Spring Boot, Vue 3, Ant Design Vue, Pinia, Axios

**Spec:** `docs/superpowers/specs/2026-04-13-integration-testing-design.md`

---

## File Structure

| 操作 | 文件 | 职责 |
|------|------|------|
| 使用 | `docker/docker-compose-full.yml` | 基础设施容器 |
| 修改 | `maidc-portal/src/views/system/UserList.vue` | 用户管理 mock→API |
| 修改 | `maidc-portal/src/views/system/RoleList.vue` | 角色管理 mock→API |
| 修改 | `maidc-portal/src/views/system/SystemConfig.vue` | 系统配置 mock→API |
| 修改 | `maidc-portal/src/views/model/ModelList.vue` | 模型列表 mock→API |
| 修改 | `maidc-portal/src/views/model/ModelDetail.vue` | 模型详情 mock→API |
| 修改 | `maidc-portal/src/views/model/EvalList.vue` | 评估列表 mock→API |
| 修改 | `maidc-portal/src/views/model/ApprovalList.vue` | 审批列表 mock→API |
| 修改 | `maidc-portal/src/views/model/DeploymentList.vue` | 部署列表 mock→API |
| 修改 | `maidc-portal/src/views/audit/OperationLog.vue` | 操作审计 mock→API |
| 修改 | `maidc-portal/src/views/audit/DataAccessLog.vue` | 数据访问审计 mock→API |
| 修改 | `maidc-portal/src/views/audit/SystemEventLog.vue` | 系统事件 mock→API |
| 修改 | `maidc-portal/src/views/message/MessageList.vue` | 消息列表 mock→API |
| 修改 | `maidc-portal/src/views/alert/AlertList.vue` | 告警列表 mock→API |
| 修改 | `maidc-portal/src/views/data-rdr/ProjectList.vue` | 研究项目 mock→API |
| 修改 | `maidc-portal/src/api/model.ts` | 补充评估/审批/部署列表API |

---

## Task 1: L1 基础设施启动

**Files:**
- Use: `docker/docker-compose-full.yml`

- [ ] **Step 1: 启动 Docker 基础设施**

```bash
cd E:/pxg_project/docker && docker-compose -f docker-compose-full.yml up -d maidc-postgres maidc-redis maidc-nacos maidc-rabbitmq maidc-minio
```

Expected: 5个容器全部 running。

- [ ] **Step 2: 验证基础设施**

```bash
docker exec maidc-postgres pg_isready -U maidc
docker exec maidc-redis redis-cli -a maidc123 ping
```

Expected: `accepting connections` 和 `PONG`

- [ ] **Step 3: 验证数据库初始化**

```bash
docker exec maidc-postgres psql -U maidc -d maidc -c "\dn"
docker exec maidc-postgres psql -U maidc -d maidc -c "SELECT username FROM system.users LIMIT 1"
```

Expected: 列出5个schema（system/cdr/model/audit/rdr），admin用户存在。

- [ ] **Step 4: 按顺序启动后端服务（8个）**

依次在8个终端中启动：

```bash
cd E:/pxg_project/maidc-parent/maidc-gateway && mvn spring-boot:run -DskipTests
cd E:/pxg_project/maidc-parent/maidc-auth && mvn spring-boot:run -DskipTests
cd E:/pxg_project/maidc-parent/maidc-data && mvn spring-boot:run -DskipTests
cd E:/pxg_project/maidc-parent/maidc-model && mvn spring-boot:run -DskipTests
cd E:/pxg_project/maidc-parent/maidc-task && mvn spring-boot:run -DskipTests
cd E:/pxg_project/maidc-parent/maidc-label && mvn spring-boot:run -DskipTests
cd E:/pxg_project/maidc-parent/maidc-audit && mvn spring-boot:run -DskipTests
cd E:/pxg_project/maidc-parent/maidc-msg && mvn spring-boot:run -DskipTests
```

Expected: 每个服务输出 `Started XxxApplication`。

- [ ] **Step 5: 验证 Gateway 路由**

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"Admin@123"}'
```

Expected: `{"code":200,"data":{"token":"xxx"}}`

- [ ] **Step 6: 启动前端**

```bash
cd E:/pxg_project/maidc-portal && npm run dev
```

Expected: `Local: http://localhost:3000/`

---

## Task 2: L2 认证链路验证

**Files:**
- Use: `maidc-portal/src/stores/auth.ts`
- Use: `maidc-portal/src/stores/permission.ts`
- Use: `maidc-portal/src/router/guards.ts`

- [ ] **Step 1: 浏览器登录测试**

1. 打开 `http://localhost:3000`
2. 输入 admin / Admin@123
3. 点击登录

Expected: 跳转到 `/dashboard/overview`，侧边栏显示菜单。

- [ ] **Step 2: 如登录失败，调试API响应格式**

打开浏览器 DevTools Network 标签，检查 `/auth/login` 响应。可能需要调整：

- `auth.ts` store 中 `loginAction` 的响应解析逻辑
- Token 存储字段名（`token` vs `access_token`）
- `getUserInfoAction` 的 API 路径和响应格式

- [ ] **Step 3: 验证侧边栏菜单渲染**

登录后检查侧边栏是否显示以下菜单组：
- 仪表盘（3个子项）
- 模型管理（10个子项）
- 数据管理（CDR + RDR）
- 标注管理（3个子项）
- 任务调度（1个子项）
- 审计日志（4个子项）
- 消息中心（4个子项）
- 系统设置（6个子项）

- [ ] **Step 4: 逐个点击菜单验证路由**

点击每个菜单项，确认：
- URL 正确变化
- 页面正常渲染（不白屏、不404）
- Layout（侧边栏+Header）不消失

---

## Task 3: L3 - UserList mock→API

**Files:**
- Modify: `maidc-portal/src/views/system/UserList.vue`

**说明:** UserList.vue 已有 useTable + getUsers API 导入，但仍在使用 mockUsers。需要将 mock 数据替换为 useTable 的 tableData。

- [ ] **Step 1: 删除 mockUsers 数据**

删除 `mockUsers` 变量定义（约 lines 198-260 的 hardcoded 数组）。

- [ ] **Step 2: 将表格数据源改为 tableData**

将 `<a-table>` 的 `:data-source` 从 mock 数据改为 `tableData`：

```vue
<a-table
  :columns="columns"
  :data-source="tableData"
  :loading="loading"
  :pagination="pagination"
  @change="handleTableChange"
  row-key="id"
/>
```

- [ ] **Step 3: 添加 onMounted 加载**

在 script setup 中添加：

```typescript
import { onMounted } from 'vue'

onMounted(() => {
  fetchData()
})
```

- [ ] **Step 4: 连接新建/编辑/重置密码到真实API**

确认以下按钮已对接API（应已存在，只需确认调用正确）：
- 新建用户：`createUser(data)` → 刷新列表 `fetchData()`
- 编辑用户：`updateUser(id, data)` → 刷新列表
- 重置密码：`resetPassword(id, { new_password })` → message.success

- [ ] **Step 5: 浏览器验证**

1. 导航到 `/system/users`
2. 确认列表加载（应有 admin 用户）
3. 点击"新建用户"，填写表单，提交成功
4. 列表刷新显示新用户

- [ ] **Step 6: Commit**

```bash
git add maidc-portal/src/views/system/UserList.vue
git commit -m "fix: UserList replace mock data with real API"
```

---

## Task 4: L3 - RoleList mock→API

**Files:**
- Modify: `maidc-portal/src/views/system/RoleList.vue`

- [ ] **Step 1: 删除 mockRoles 数据**

删除 `mockRoles` 变量（约 lines 191-198）。

- [ ] **Step 2: 表格数据源改为 tableData**

同 Task 3 Step 2 模式：`:data-source="tableData"` + `:loading="loading"` + `:pagination="pagination"` + `@change="handleTableChange"`

- [ ] **Step 3: 添加 onMounted 加载**

```typescript
onMounted(() => { fetchData() })
```

- [ ] **Step 4: 连接新建/编辑到真实API**

- 新建角色：`createRole(data)` → `fetchData()`
- 编辑角色：`updateRole(id, data)` → `fetchData()`

- [ ] **Step 5: 浏览器验证**

导航到 `/system/roles`，确认角色列表加载。

- [ ] **Step 6: Commit**

```bash
git add maidc-portal/src/views/system/RoleList.vue
git commit -m "fix: RoleList replace mock data with real API"
```

---

## Task 5: L3 - SystemConfig mock→API

**Files:**
- Modify: `maidc-portal/src/views/system/SystemConfig.vue`

- [ ] **Step 1: 导入 API 函数**

在 script setup 顶部添加：

```typescript
import { getConfigs, updateConfig } from '@/api/system'
```

- [ ] **Step 2: 替换 configGroups 为 API 加载**

删除硬编码的 `configGroups`，改为：

```typescript
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'

const loading = ref(false)
const configGroups = ref<any[]>([])

async function loadConfigs() {
  loading.value = true
  try {
    const res = await getConfigs({ page: 1, page_size: 100 })
    // 按group分组展示
    const groups: Record<string, any[]> = {}
    for (const item of res.data.data.items) {
      const g = item.config_group || 'default'
      if (!groups[g]) groups[g] = []
      groups[g].push(item)
    }
    configGroups.value = Object.entries(groups).map(([group, items]) => ({ group, items }))
  } finally {
    loading.value = false
  }
}

onMounted(loadConfigs)
```

- [ ] **Step 3: 编辑保存对接API**

将保存按钮的 click handler 改为：

```typescript
async function handleSave(configId: number, data: Record<string, any>) {
  await updateConfig(configId, data)
  message.success('配置已保存')
  loadConfigs()
}
```

- [ ] **Step 4: 浏览器验证**

导航到 `/system/config`，确认配置项加载。

- [ ] **Step 5: Commit**

```bash
git add maidc-portal/src/views/system/SystemConfig.vue
git commit -m "fix: SystemConfig replace mock data with real API"
```

---

## Task 6: L3 系统管理验证

- [ ] **Step 1: 验证用户管理完整流程**

1. 用户列表加载 → 显示admin
2. 新建用户 → 列表刷新
3. 编辑用户 → 保存成功
4. 重置密码 → 成功
5. 角色分配 → 权限树正确

- [ ] **Step 2: 验证角色管理完整流程**

1. 角色列表加载
2. 新建角色 → 列表刷新
3. 点击角色 → 权限树加载
4. 勾选权限 → 保存成功

- [ ] **Step 3: Commit（如有修复）**

```bash
git add -u && git commit -m "fix: L3 system management integration fixes"
```

---

## Task 7: L4 - 补充 model API 接口

**Files:**
- Modify: `maidc-portal/src/api/model.ts`

**说明:** 当前 model.ts 缺少评估列表、审批列表、部署列表的查询API。需要补充。

- [ ] **Step 1: 添加缺失的列表API**

在 `model.ts` 末尾添加：

```typescript
// Evaluation list
export function getEvaluations(params: { page?: number; page_size?: number; status?: string; model_id?: number }) {
  return request.get<ApiResponse<PageResult<any>>>('/evaluations', { params })
}

// Approval APIs
export function getApprovals(params: { page?: number; page_size?: number; status?: string }) {
  return request.get<ApiResponse<PageResult<any>>>('/approvals', { params })
}

export function submitApproval(data: Record<string, any>) {
  return request.post<ApiResponse<any>>('/approvals', data)
}

export function reviewApproval(id: number, data: Record<string, any>) {
  return request.put<ApiResponse<any>>(`/approvals/${id}/review`, data)
}

export function getApproval(id: number) {
  return request.get<ApiResponse<any>>(`/approvals/${id}`)
}

// Deployment APIs
export function getDeployments(params: { page?: number; page_size?: number; status?: string }) {
  return request.get<ApiResponse<PageResult<any>>>('/deployments', { params })
}

export function getDeployment(id: number) {
  return request.get<ApiResponse<any>>(`/deployments/${id}`)
}

export function scaleDeployment(id: number, replicas: number) {
  return request.put<ApiResponse<any>>(`/deployments/${id}/scale`, { replicas })
}

export function restartDeployment(id: number) {
  return request.put<ApiResponse<any>>(`/deployments/${id}/restart`)
}

// Monitoring APIs
export function getInferenceLogs(params: { page?: number; page_size?: number; model_id?: number; deployment_id?: number }) {
  return request.get<ApiResponse<PageResult<any>>>('/monitoring/inference-logs', { params })
}

export function getMetricsOverview() {
  return request.get<ApiResponse<any>>('/monitoring/metrics')
}
```

- [ ] **Step 2: Commit**

```bash
git add maidc-portal/src/api/model.ts
git commit -m "feat: add evaluation/approval/deployment list APIs to model.ts"
```

---

## Task 8: L4 - ModelList mock→API

**Files:**
- Modify: `maidc-portal/src/views/model/ModelList.vue`

- [ ] **Step 1: 替换数据源为 useTable**

将 `allModels` mock 数据替换为 useTable：

```typescript
import { useTable } from '@/hooks/useTable'
import { getModels, createModel, updateModel, deleteModel } from '@/api/model'
import { onMounted } from 'vue'

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getModels({ ...params, keyword: searchKeyword.value })
)

onMounted(() => fetchData())
```

- [ ] **Step 2: 更新表格绑定**

```vue
<a-table
  :columns="columns"
  :data-source="tableData"
  :loading="loading"
  :pagination="pagination"
  @change="handleTableChange"
  row-key="id"
/>
```

- [ ] **Step 3: 连接搜索/筛选**

搜索时调用 `fetchData({ page: 1 })` 重置到第一页。

- [ ] **Step 4: 连接新建/编辑弹窗到API**

确认 `handleRegister` 调用 `createModel(data)` + `fetchData()`。

- [ ] **Step 5: 连接行点击导航到详情**

确认表格行或"查看"按钮调用 `router.push(\`/model/${record.id}\`)`。

- [ ] **Step 6: 浏览器验证**

导航到 `/model/list`，确认空列表或模型数据加载。

- [ ] **Step 7: Commit**

```bash
git add maidc-portal/src/views/model/ModelList.vue
git commit -m "fix: ModelList replace mock data with real API"
```

---

## Task 9: L4 - ModelDetail mock→API

**Files:**
- Modify: `maidc-portal/src/views/model/ModelDetail.vue`

- [ ] **Step 1: 导入 API 并加载模型详情**

```typescript
import { getModel, getVersions } from '@/api/model'
import { useRoute } from 'vue-router'

const route = useRoute()
const modelId = Number(route.params.id)
const modelInfo = ref<any>(null)
const loading = ref(false)

async function loadModelDetail() {
  loading.value = true
  try {
    const res = await getModel(modelId)
    modelInfo.value = res.data.data
  } finally {
    loading.value = false
  }
}

onMounted(loadModelDetail)
```

- [ ] **Step 2: 删除 mock 数据**

删除 `modelInfo`、`metrics`、`versions`、`evaluations`、`deployments` 的 hardcoded 赋值。

- [ ] **Step 3: Tab 数据按需加载**

在 Tab 切换时加载对应数据：
- 版本Tab：`getVersions(modelId)`
- 其他Tab暂显示空状态

- [ ] **Step 4: 浏览器验证**

从模型列表点击进入详情页，确认基础信息展示。

- [ ] **Step 5: Commit**

```bash
git add maidc-portal/src/views/model/ModelDetail.vue
git commit -m "fix: ModelDetail replace mock data with real API"
```

---

## Task 10: L4 - EvalList + ApprovalList + DeploymentList mock→API

**Files:**
- Modify: `maidc-portal/src/views/model/EvalList.vue`
- Modify: `maidc-portal/src/views/model/ApprovalList.vue`
- Modify: `maidc-portal/src/views/model/DeploymentList.vue`

**说明:** 这三个页面模式相同，都是：删除mock数据 → 导入API → 用useTable或直接ref加载。

- [ ] **Step 1: EvalList mock→API**

```typescript
import { getEvaluations, createEvaluation } from '@/api/model'

// 替换 mock evaluations 数组
const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getEvaluations(params)
)
onMounted(() => fetchData())
```

表格绑定同前：`:data-source="tableData"` + `:loading` + `:pagination` + `@change`。

- [ ] **Step 2: ApprovalList mock→API**

```typescript
import { getApprovals, reviewApproval } from '@/api/model'

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getApprovals(params)
)
onMounted(() => fetchData())
```

审批操作按钮：`reviewApproval(id, { status: 'APPROVED'/'REJECTED', comment })` → `fetchData()`

- [ ] **Step 3: DeploymentList mock→API**

```typescript
import { getDeployments, startDeployment, stopDeployment } from '@/api/model'

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getDeployments(params)
)
onMounted(() => fetchData())
```

启停按钮：`startDeployment(id)` / `stopDeployment(id)` → `fetchData()`

- [ ] **Step 4: 浏览器验证三个页面**

- `/model/evaluations` → 评估列表
- `/model/approvals` → 审批列表
- `/model/deployments` → 部署列表

- [ ] **Step 5: Commit**

```bash
git add maidc-portal/src/views/model/EvalList.vue maidc-portal/src/views/model/ApprovalList.vue maidc-portal/src/views/model/DeploymentList.vue
git commit -m "fix: EvalList/ApprovalList/DeploymentList replace mock with API"
```

---

## Task 11: L4 模型管理验证

- [ ] **Step 1: 验证模型管理完整流程**

1. 模型列表加载
2. 新建模型 → 列表刷新
3. 点击进入详情 → 信息展示
4. 评估列表/审批列表/部署列表可加载
5. 各页面操作按钮可点击且有反馈

- [ ] **Step 2: Commit（如有修复）**

```bash
git add -u && git commit -m "fix: L4 model management integration fixes"
```

---

## Task 12: L5 - 审计模块 mock→API

**Files:**
- Modify: `maidc-portal/src/views/audit/OperationLog.vue`
- Modify: `maidc-portal/src/views/audit/DataAccessLog.vue`
- Modify: `maidc-portal/src/views/audit/SystemEventLog.vue`

**说明:** 三个审计页面都已有 useTable + mock mockAPI。只需将 mock Promise 替换为真实API。

- [ ] **Step 1: OperationLog 替换 mock API**

将底部 mock useTable：

```typescript
const { tableData, loading, fetchData, handleTableChange } = useTable<any>(
  () => Promise.resolve({ data: { code: 0, message: '', data: { items: [], total: 0, page: 1, pageSize: 20, totalPages: 0 }, traceId: '' } })
)
```

替换为：

```typescript
import { getAuditLogs } from '@/api/audit'

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getAuditLogs({ ...params, ...filters })
)
onMounted(() => fetchData())
```

删除 `mockData` ref 和 `filteredData` computed。表格绑定 `tableData`。

- [ ] **Step 2: DataAccessLog 替换 mock API**

同上模式，将 mock Promise 替换为：

```typescript
import { getDataAccessLogs } from '@/api/audit'

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getDataAccessLogs({ ...params, ...filters })
)
onMounted(() => fetchData())
```

- [ ] **Step 3: SystemEventLog 替换 mock API**

```typescript
import { getSystemEvents } from '@/api/audit'

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getSystemEvents({ ...params, ...filters })
)
onMounted(() => fetchData())
```

- [ ] **Step 4: 浏览器验证**

导航到 `/audit/operations`、`/audit/data-access`、`/audit/system-events`。

- [ ] **Step 5: Commit**

```bash
git add maidc-portal/src/views/audit/
git commit -m "fix: audit module replace mock with real API"
```

---

## Task 13: L5 - 消息+告警 mock→API

**Files:**
- Modify: `maidc-portal/src/views/message/MessageList.vue`
- Modify: `maidc-portal/src/views/alert/AlertList.vue`

- [ ] **Step 1: MessageList mock→API**

```typescript
import { getMessages, markAsRead, markAllAsRead } from '@/api/msg'

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getMessages({ ...params })
)
onMounted(() => fetchData())
```

标记已读：`markAsRead(id)` → `fetchData()`
全部已读：`markAllAsRead()` → `fetchData()`

- [ ] **Step 2: AlertList mock→API**

在 `api/model.ts` 中补充告警API（如未定义）：

```typescript
// Alert APIs（加在model.ts末尾或新建alert.ts）
export function getAlerts(params: { page?: number; page_size?: number; status?: string; severity?: string }) {
  return request.get<ApiResponse<PageResult<any>>>('/alerts', { params })
}

export function acknowledgeAlert(id: number) {
  return request.put<ApiResponse<any>>(`/alerts/${id}/acknowledge`)
}
```

AlertList.vue 中：

```typescript
import { getAlerts, acknowledgeAlert } from '@/api/model'

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getAlerts(params)
)
onMounted(() => fetchData())
```

- [ ] **Step 3: 浏览器验证**

- `/message/list` → 消息列表
- `/alert/active` → 告警列表

- [ ] **Step 4: Commit**

```bash
git add maidc-portal/src/views/message/ maidc-portal/src/views/alert/ maidc-portal/src/api/model.ts
git commit -m "fix: message and alert modules replace mock with API"
```

---

## Task 14: L5 - ProjectList mock→API

**Files:**
- Modify: `maidc-portal/src/views/data-rdr/ProjectList.vue`

- [ ] **Step 1: 替换 mockProjects 为 API**

```typescript
import { getProjects, createProject } from '@/api/data'

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getProjects({ ...params })
)
onMounted(() => fetchData())
```

新建项目：`createProject(data)` → `fetchData()`

- [ ] **Step 2: 浏览器验证**

导航到 `/data/rdr/projects`。

- [ ] **Step 3: Commit**

```bash
git add maidc-portal/src/views/data-rdr/ProjectList.vue
git commit -m "fix: ProjectList replace mock with real API"
```

---

## Task 15: 全局导航和按钮逻辑验证

- [ ] **Step 1: 验证所有菜单可点击导航**

逐一点击侧边栏每个菜单项，确认URL变化且页面渲染。

- [ ] **Step 2: 验证列表→详情导航**

从各列表页点击"查看"链接，确认跳转到详情页：
- 用户列表 → 用户详情
- 模型列表 → 模型详情
- 患者列表 → 患者详情
- 审批列表 → 审批详情

- [ ] **Step 3: 验证弹窗交互**

各页面的新建/编辑弹窗可正常打开、填写、提交、关闭。

- [ ] **Step 4: 验证返回导航**

详情页返回按钮可回到列表页。

- [ ] **Step 5: 最终 Commit**

```bash
git add -u && git commit -m "fix: complete integration - all pages connected to real APIs"
```

---

## Commit 统计

| Task | 描述 | Commits |
|------|------|---------|
| Task 1 | L1 基础设施启动 | 0 (验证) |
| Task 2 | L2 认证链路验证 | 0 (验证) |
| Task 3 | UserList mock→API | 1 |
| Task 4 | RoleList mock→API | 1 |
| Task 5 | SystemConfig mock→API | 1 |
| Task 6 | L3 系统管理验证 | 0-1 |
| Task 7 | 补充 model API | 1 |
| Task 8 | ModelList mock→API | 1 |
| Task 9 | ModelDetail mock→API | 1 |
| Task 10 | Eval/Approval/Deploy mock→API | 1 |
| Task 11 | L4 模型管理验证 | 0-1 |
| Task 12 | 审计模块 mock→API | 1 |
| Task 13 | 消息+告警 mock→API | 1 |
| Task 14 | ProjectList mock→API | 1 |
| Task 15 | 全局导航验证 | 1 |
| **合计** | **15个Task** | **~11 commits** |
