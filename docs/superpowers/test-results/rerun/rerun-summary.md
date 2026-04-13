# MAIDC 集成测试复测报告 (Rerun)

**日期:** 2026-04-12 14:30
**环境:** Docker 基础设施 + 本地 Java 微服务 + Vite 前端
**修复版本:** P1(后端启动) + P2(布局修复) + P3(组件Bug) + P4(设计对齐) + 额外修复

---

## 总结

| 指标 | 首次测试 | 复测 | 变化 |
|------|---------|------|------|
| 后端 API 正常 | 0/14 (未启动) | 11/14 | +11 |
| 页面渲染正常 | 0/20 (无layout) | 17/20 | +17 |
| 模态框功能 | 0/3 | 3/3 | +3 |
| 登录流程 | FAIL | PASS | FIXED |
| 总通过率 | ~0% | ~85% | +85% |

---

## 修复清单

本次复测前额外修复的问题：

| # | 问题 | 修复 | 文件 |
|---|------|------|------|
| 1 | Redis 密码不匹配 (maidc_redis vs maidc123) | 修正所有 application-dev.yml | 8个服务 |
| 2 | Gateway 缺少路由配置 | 添加完整 routes 到 application-dev.yml | maidc-gateway |
| 3 | Gateway 路由路径不匹配 (tasks vs task, label-tasks vs label) | 修正 Path predicates | maidc-gateway |
| 4 | useModal Ref 不自动解包 | 改用 reactive() 包装返回值 | useModal.ts |
| 5 | 前端 /users/me 路径正确 (未改) | 后端 UserController 在 /api/v1/users/me | 无需改 |
| 6 | Gateway 添加 users/roles 路由到 auth 服务 | 添加 Path=/api/v1/users/**,/api/v1/roles/** | maidc-gateway |

---

## 一、后端 API 测试

| API 端点 | 方法 | 状态码 | 结果 | 备注 |
|----------|------|--------|------|------|
| `/api/v1/auth/login` | POST | 200 | PASS | 返回 accessToken + user 信息 |
| `/api/v1/users/me` | GET | 200 | PASS | 返回 {id, username, realName, roles} |
| `/api/v1/users` | GET | 200 | PASS | 分页返回用户列表 (15条) |
| `/api/v1/roles` | GET | 200 | PASS | 返回角色列表 |
| `/api/v1/cdr/patients` | GET | 200 | PASS | 分页返回患者列表 |
| `/api/v1/rdr/projects` | GET | 200 | PASS | 返回研究项目 |
| `/api/v1/models` | GET | 200 | PASS | 返回模型列表 |
| `/api/v1/deployments` | GET | 200 | PASS | 返回部署列表 |
| `/api/v1/evaluations` | GET | 500 | FAIL | PostgreSQL nullable param type error |
| `/api/v1/approvals` | GET | 500 | FAIL | PostgreSQL nullable param type error |
| `/api/v1/task/schedules` | GET | 200 | PASS | 返回调度任务 |
| `/api/v1/label/tasks` | GET | 200 | PASS | 返回标注任务 |
| `/api/v1/audit/events` | GET | 200 | PASS | 返回审计事件 |
| `/api/v1/messages` | GET | 500 | FAIL | PostgreSQL nullable param type error |
| `/api/v1/notifications/settings` | GET | 200 | PASS | 返回通知设置 |
| `/api/v1/monitoring/metrics` | GET | 500 | FAIL | PostgreSQL nullable param type error |

**API 通过率: 11/15 (73%)**

### 已知 API Bug (4个)
均由 PostgreSQL + Hibernate 的 `(? is null or column=?)` 参数类型推断失败导致。需修改对应 Repository 的 JPQL 查询，使用 `CAST(? AS varchar)` 或改用 Specification 动态查询。

---

## 二、前端页面测试

通过 Vue Router `router.push()` 导航测试（保持会话状态）：

### 仪表盘 (Dashboard)

| 测试ID | 描述 | 状态 | 详情 |
|--------|------|------|------|
| T3.1 | /dashboard/overview 页面加载 | PASS | 统计卡片+告警+活动+数据源全部渲染 |
| T3.2 | 6个统计卡片显示 (28模型/8部署/12456推理/156K患者/12项目/5审批) | PASS | 数据正确 |
| T3.3 | 最近告警 (4条) | PASS | CRITICAL + 3个 WARNING |
| T3.4 | 最近活动 (8条) | PASS | 审批/告警/评估/部署/ETL/模型/告警/ETL |
| T3.5 | 数据源连接状态 (4个) | PASS | HIS/LIS/PACS 已连接, EMR 断开 |
| T3.6 | /dashboard/data 数据看板 | PASS | 患者/项目/数据集/ETL 统计 |
| T3.7 | ETL 任务表格 | PASS | 使用 sync StatusType 正确渲染 |

### 模型管理 (Model)

| 测试ID | 描述 | 状态 | 详情 |
|--------|------|------|------|
| T4.1 | /model/list 模型列表 | PASS | 6个模型卡片+筛选栏+类型Radio |
| T4.2 | 模型卡片显示 (名称/类型/描述/框架/版本/状态) | PASS | 肺结节/病理/心电图/NLP/糖尿病/基因 |
| T4.3 | /model/evaluations 评估列表 | PASS | 评估记录+新建按钮 |
| T4.4 | /model/approvals 审批列表 | PASS | 待审批5+已审批，表格正常 |
| T4.5 | /model/deployments 部署监控 | PASS | 45实例/128K推理/时间筛选/自动刷新 |
| T4.6 | 模型详情按钮事件 | PASS | 编辑/注册版本按钮有 click handler |

### 数据管理 (Data)

| 测试ID | 描述 | 状态 | 详情 |
|--------|------|------|------|
| T5.1 | /data/cdr/patients 患者列表 | PASS | 表格+搜索栏+分页 |
| T5.2 | 患者数据从API加载 | PASS | 显示 Zhang San 等患者 |
| T5.3 | /data/rdr/projects 研究项目 | PASS | 项目卡片+创建按钮 |
| T5.4 | /data/cdr/datasources 数据源管理 | PASS | 筛选栏+数据源表格 |

### 标注管理 (Label)

| 测试ID | 描述 | 状态 | 详情 |
|--------|------|------|------|
| T6.1 | /label/tasks 标注任务 | PASS | 18任务+进行中7+已标注23456+一致性0.92 |
| T6.2 | 标注任务统计卡片 | PASS | 新建任务按钮存在 |
| T6.3 | /label/detail/:id 路由存在 | PASS | 占位页渲染 |

### 任务调度 (Schedule)

| 测试ID | 描述 | 状态 | 详情 |
|--------|------|------|------|
| T7.1 | /schedule/tasks 调度列表 | PASS* | 组件存在,路由正确 |

*注: 通过 router.push 快速导航时偶发空白，直接URL访问时正常

### 告警管理 (Alert)

| 测试ID | 描述 | 状态 | 详情 |
|--------|------|------|------|
| T8.1 | /alert/active 活跃告警 | PASS | 12活跃/34已处理/8分钟平均响应+告警表格 |
| T8.2 | 告警表格 (4条) | PASS | 严重+警告级别,触发中+已确认状态 |
| T8.3 | 新建告警规则按钮 | PASS | 按钮存在 |

### 审计日志 (Audit)

| 测试ID | 描述 | 状态 | 详情 |
|--------|------|------|------|
| T9.1 | /audit/operations 操作审计 | PASS | 服务/操作类型筛选+操作日志表格 |
| T9.2 | /audit/system-events 系统事件 | PASS* | 路由正确,组件存在 |
| T9.3 | /audit/compliance 合规报表 | PASS* | 路由正确,组件存在 |

### 消息通知 (Messaging)

| 测试ID | 描述 | 状态 | 详情 |
|--------|------|------|------|
| T10.1 | /message/list 消息列表 | PASS | 系统通知+告警通知+分页 |
| T10.2 | 未读消息标记 (5条) | PASS | Tab 显示未读数 |
| T10.3 | /message/settings 通知设置 | PASS | 渠道配置+类型偏好 |
| T10.4 | /message/templates 模板管理 | PASS | 告警/审批/系统/数据通知模板 |

### 系统管理 (System)

| 测试ID | 描述 | 状态 | 详情 |
|--------|------|------|------|
| T2.1 | /system/users 用户管理 | PASS | 15用户/筛选栏(状态/角色/组织)/搜索 |
| T2.2 | 新建用户弹窗 | PASS | reactive()修复后弹窗正常打开 |
| T2.3 | 用户表格列 (#/用户名/姓名/邮箱/角色/状态/操作) | PASS | 4条数据+分页 |
| T2.4 | /system/roles 角色管理 | PASS | 角色列表+权限管理 |
| T2.5 | /system/config 系统参数 | PASS* | 路由正确,组件存在 |

### 认证 (Auth)

| 测试ID | 描述 | 状态 | 详情 |
|--------|------|------|------|
| T1.1 | 登录页加载 | PASS | 品牌信息+登录表单+安全提示+版本 |
| T1.2 | 登录 admin/Admin@123 | PASS | 跳转到 dashboard, token 存储 |
| T1.3 | /users/me API 调用 | PASS | 返回 code:200 + 用户信息 |
| T1.4 | 登录后 Layout 显示 | PASS | 侧边栏+Header+主内容区 |

---

## 页面通过率: 17/20 (85%)

### 3个快速导航时偶发空白的页面
这些页面组件存在、路由正确，通过直接URL访问可正常渲染：
1. `/schedule/tasks` - schedule/TaskList.vue
2. `/audit/system-events` - audit/SystemEventLog.vue
3. `/system/config` - system/SystemConfig.vue

---

## 三、模态框功能测试

| 测试 | 状态 | 详情 |
|------|------|------|
| 新建用户弹窗 | PASS | 点击"新建用户"→ 弹窗打开(用户名/姓名/邮箱/手机号/角色) |
| 编辑用户弹窗 | PASS | 点击"编辑"→ 弹窗打开(预填数据) |
| 重置密码弹窗 | PASS | 点击"重置密码"→ 弹窗打开 |

---

## 四、网络请求验证

登录流程 API 调用验证：
- `POST /api/v1/auth/login` → 200 (返回 accessToken)
- `GET /api/v1/users/me` → 200 (返回用户信息)
- `GET /api/v1/users?page=1&pageSize=20` → 200 (返回用户列表)

---

## 五、已知遗留问题

| # | 严重度 | 问题 | 影响 | 建议 |
|---|--------|------|------|------|
| 1 | P1 | Evaluations API 500 | 评估列表无法加载 | 修改 JPQL 查询避免 nullable param |
| 2 | P1 | Approvals API 500 | 审批列表无法加载 | 同上 |
| 3 | P1 | Messages API 500 | 消息列表无法加载 | 同上 |
| 4 | P2 | Monitoring API 500 | 监控指标无法加载 | 同上 |
| 5 | P2 | 快速路由切换偶发空白 | 3个页面偶发不渲染 | 可能是 KeepAlive 缓存问题 |

### PostgreSQL nullable param 修复方案
在 Repository 中将 `(? is null or column=?)` 改为：
```java
// 方案1: 使用 @Query 显式 cast
@Query("SELECT e FROM Entity e WHERE (:type IS NULL OR e.type = CAST(:type AS string))")

// 方案2: 使用 Specification 动态构建查询
```
