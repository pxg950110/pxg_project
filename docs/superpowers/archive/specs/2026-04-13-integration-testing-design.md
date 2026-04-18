# 全局联调+交互逻辑 设计文档

> **日期**: 2026-04-13
> **目标**: 启动前后端服务，所有页面对接真实API，菜单导航和按钮逻辑全部连通
> **策略**: 分层联调（L1→L5），每层独立验证

---

## 现状分析

### 前端（49个视图）
- **26个页面**已接入真实API（useTable + api模块）
- **14个页面**仍使用mock数据，需替换
- **3个TODO**（审计导出功能）

### 后端（8个微服务）
- 全部编译通过，API已实现
- 配置文件已就绪（bootstrap.yml + application-dev.yml）
- 需要实际启动验证

### API模块（8个）
`auth.ts` `model.ts` `data.ts` `task.ts` `label.ts` `audit.ts` `msg.ts` `system.ts`

---

## L1: 基础设施启动

### 目标
Docker基础设施 + 后端8服务 + 前端dev server全部运行。

### 步骤

1. **启动Docker基础设施**
   ```bash
   cd docker && docker-compose -f docker-compose-full.yml up -d maidc-postgres maidc-redis maidc-nacos maidc-rabbitmq maidc-minio
   ```
   验证：5个容器running，PostgreSQL accept connections，Redis PONG，Nacos UP

2. **验证数据库初始化**
   - 5个schema存在：system, cdr, model, audit, rdr
   - admin用户存在
   - 权限树数据完整

3. **按顺序启动后端服务**
   | 顺序 | 服务 | 端口 | 依赖 |
   |------|------|------|------|
   | 1 | Gateway | 8080 | Nacos, Redis |
   | 2 | Auth | 8081 | PostgreSQL, Redis |
   | 3 | Data | 8082 | PostgreSQL, Redis, MinIO |
   | 4 | Model | 8083 | PostgreSQL, Redis, MinIO, RabbitMQ |
   | 5 | Task | 8084 | PostgreSQL, Redis |
   | 6 | Label | 8085 | PostgreSQL, Redis, RabbitMQ |
   | 7 | Audit | 8086 | PostgreSQL, Redis |
   | 8 | Msg | 8087 | PostgreSQL, Redis, RabbitMQ |

4. **启动前端**
   ```bash
   cd maidc-portal && npm run dev
   ```

### 验证标准
- `curl -X POST localhost:8080/api/v1/auth/login -d '{"username":"admin","password":"Admin@123"}'` 返回200 + JWT token
- `localhost:3000` 可访问，显示登录页

---

## L2: 认证链路

### 目标
登录 → JWT签发 → Token存储 → 路由守卫 → 动态菜单生成 → 页面渲染 全链路跑通。

### 当前状态
- 前端代码已完善：auth store、permission store、guards、LoginPage
- 后端Auth服务已实现JWT签发（Access 2h + Refresh 7d）

### 步骤

1. **验证登录API响应格式**
   - 后端返回：`{ code: 200, data: { token, refreshToken, user } }`
   - 前端auth store期望：`{ token, user }`
   - 如格式不匹配则调整前端store

2. **验证用户信息API**
   - `/users/me` 返回用户角色和权限列表
   - 前端permission store用于动态路由过滤

3. **验证路由守卫流程**
   - Token检查 → 白名单放行 → getUserInfo → generateRoutes → addRoute → 渲染

4. **验证侧边栏菜单**
   - BasicLayout从permissionStore.routes渲染菜单
   - 所有顶级菜单项可点击展开
   - 子菜单项点击后正确路由

### 验证标准
- 浏览器登录 admin/Admin@123
- 自动跳转到 `/dashboard/overview`
- 侧边栏显示8个顶级菜单（仪表盘/模型/数据/标注/调度/审计/消息/系统）
- 每个菜单可展开并导航到子页面

---

## L3: 系统管理（4个页面 → 真实CRUD）

### 目标
用户/角色/权限/配置管理全部对接真实后端API。

### 页面改造计划

#### UserList.vue（Mock → API）
- 替换mockUsers为useTable + getUsers API
- 新建用户弹窗：调用createUser API
- 编辑用户弹窗：调用updateUser API
- 重置密码：调用resetPassword API
- 删除确认：调用deleteUser API
- 搜索/筛选：通过API参数传递

#### RoleList.vue（Mock → API）
- 替换mockRoles为useTable + getRoles API
- 新建角色：调用createRole API
- 编辑角色：调用updateRole API
- 删除角色：调用deleteRole API

#### RolePermission.vue（已用API，需验证）
- 权限树加载：getPermissionTree
- 保存权限分配：assignPermissions
- 确保树组件正确渲染和勾选

#### SystemConfig.vue（Mock → API）
- 替换硬编码配置组为getConfigs API
- 编辑保存：调用updateConfig API

### 验证标准
- 用户列表显示真实数据（初始应有admin用户）
- 新建用户 → 列表刷新显示新用户
- 编辑角色权限 → 保存后权限生效
- 配置项编辑保存成功

---

## L4: 模型管理（6个mock页面 → API）

### 目标
模型管理的核心业务流程全部对接真实API。

### 页面改造计划

| 页面 | 改造内容 |
|------|---------|
| ModelList.vue | useTable + getModels, 新建/编辑/删除对接API |
| ModelDetail.vue | getModel获取详情, Tab切换展示版本/部署/统计 |
| EvalList.vue | useTable + getEvaluations, 发起评估对接API |
| ApprovalList.vue | useTable + getApprovals, 审批操作对接API |
| DeploymentList.vue | useTable + getDeployments, 创建/启停对接API |
| ApprovalDetail.vue | getApproval详情 + 审批时间线 |

### 已完成的页面（无需修改）
- VersionList.vue（已用API）
- EvalDetail.vue（已用API）
- DeploymentDetail.vue（已用API）
- RouteConfig.vue（已用API）
- InferenceLog.vue（已用API）

### 验证标准
- 模型列表显示空列表（新系统无数据），可新建模型
- 新建模型后列表刷新，可点击进入详情
- 详情页Tab可切换，版本管理可上传
- 评估列表可发起评估任务

---

## L5: 其余模块（mock → API）

### 按优先级排序

#### 1. 审计模块（3页）
- OperationLog.vue: 已有mock数据展示，接入getOperationLogs API
- DataAccessLog.vue: 接入getDataAccessLogs API
- SystemEventLog.vue: 接入getSystemEvents API

#### 2. 消息模块（3页）
- MessageList.vue: 接入getMessages + markRead + delete API
- MessageDetail.vue: 接入getMessageDetail API
- NotificationSettings.vue: 接入getNotificationSettings API

#### 3. 告警模块（2页）
- AlertList.vue: 接入getAlerts + acknowledgeAlert API
- AlertRuleList.vue: 接入getAlertRules + createAlertRule API

#### 4. 标注模块（2页）
- LabelTaskList.vue: 已有API导入，替换mock为useTable
- LabelWorkspace.vue: 接入标注相关API

#### 5. 数据RDR（2页）
- ProjectList.vue: 替换mockProjects为useTable + getProjects API
- CohortList.vue: 接入getCohorts API

#### 6. 调度模块（1页）
- TaskList.vue: 已有API导入，替换mock为useTable

### 验证标准
- 所有列表页显示真实数据（初始可能为空）
- 新建/编辑操作成功后列表刷新
- 详情页可正常打开

---

## 菜单导航和按钮逻辑

### 侧边栏菜单
- 从asyncRoutes自动生成，基于meta.title和meta.icon
- 权限控制：meta.permission字段决定菜单可见性
- admin用户看到所有菜单

### 需要确保的导航逻辑
1. **列表 → 详情**: 表格行点击或"查看"按钮 → router.push到详情页
2. **详情 → 列表**: 返回按钮 → router.back()
3. **新建/编辑弹窗**: useModal打开，表单提交后关闭并刷新列表
4. **删除确认**: Modal.confirm确认后调用删除API
5. **Tab切换**: 详情页内Tab切换展示不同数据

### 全局一致的交互模式
- 列表页: SearchForm + Table + Pagination + Modal(新建/编辑)
- 详情页: Descriptions/Tab + 返回按钮
- 操作反馈: message.success/error 提示

---

## 风险和注意事项

1. **API响应格式不一致**: 后端R<T>包装的响应格式需与前端ApiResponse<T>完全匹配
2. **分页参数差异**: 后端用page/pageSize，前端useTable用current/pageSize，需确认映射
3. **权限数据结构**: 后端返回的权限格式需与前端permission store期望的一致
4. **Token刷新**: Access token 2小时过期，需确保refresh token机制正常工作
5. **跨服务调用**: 模型评估/部署等异步操作涉及MQ，需确保RabbitMQ消息流转正常
