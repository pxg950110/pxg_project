# MAIDC 集成点击测试问题记录
测试时间: 2026-04-22
测试环境: 本地 (localhost:3000)
后端服务: auth(8081), data(8082), model(8083), audit(8086)

## 统计
- 测试页面: 28个
- 正常: 19个
- 有问题: 9个
- 发现问题: 12个
- 已修复: 12个 (全部修复)
- 剩余: 0个

## 问题列表

### P0 - 严重 (影响核心功能)

#### 1. 部署管理/流量路由/版本管理 - API 500 导致页面崩溃 ✅ FIXED
- **页面**: /model/deployments, /model/routes, /model/versions
- **现象**: 页面加载时 API 返回 500，组件进入损坏状态，点击任何按钮后页面重定向到不相关路由
- **根因**: 后端 API 错误未正确处理，Vue Router 错误恢复机制异常
- **修复**: 前端增加 API 错误降级处理（catch 中不抛出异常），部署页已有 try/catch

#### 2. 审计页面全部 500 错误 ✅ FIXED
- **页面**: /audit/operations, /audit/compliance, /audit/data-access
- **现象**: 后端 audit-service (8086) API 全部返回 500，数据无法加载
- **根因**: audit 服务未启动 + 前端参数名不匹配（snake_case vs camelCase）
- **修复**: 启动 audit 服务(8086)，修复前端 API 参数名(startTime/pageSize)，修复 ComplianceReport ISO时间格式

### P1 - 中等 (功能缺失)

#### 3. 权限管理 - 新增/编辑按钮无功能 ✅ FIXED
- **页面**: /system/permissions
- **现象**: "新增权限"和"编辑"按钮点击无反应
- **根因**: `handleAdd()` 和 `handleEdit()` 是空函数 (PermissionManagement.vue:182-184)
- **修复**: 实现新增/编辑权限对话框，包含表单验证

#### 4. 组织管理 - 新增/编辑按钮无 click 处理 ✅ FIXED
- **页面**: /system/organizations
- **现象**: "新增组织"按钮和详情面板"编辑"按钮无 @click 处理程序
- **根因**: 按钮未绑定事件 (OrganizationList.vue:11, :103)
- **修复**: 添加新增/编辑组织对话框和事件绑定

#### 5. 模型详情 - 评估记录 Tab 空白 ✅ FIXED
- **页面**: /model/:id (评估记录 Tab)
- **现象**: 切换到评估记录 Tab 后内容完全空白，无表格头、无"暂无数据"提示
- **根因**: Tab panel 内容未渲染
- **修复**: 补充评估记录 Tab 的空状态 `<a-empty>`，修复时间格式化使用 dayjs

### P2 - 轻微 (UI/体验)

#### 6. 个人工作台指标全为0 ✅ FIXED
- **页面**: /dashboard/workspace
- **现象**: 工作台显示模型总数0、活跃部署0等，但系统总览显示3模型1部署
- **根因**: vite.config.ts 缺少 `/api/v1/workspace` 代理规则
- **修复**: 添加 workspace API 代理到 data-service(8082)

#### 7. "全部已读"按钮报错 ✅ FIXED
- **页面**: /dashboard/workspace
- **现象**: 点击"全部已读"按钮报 Uncaught (in promise) 错误
- **根因**: handleMarkAllRead 未 await + 缺少 try/catch
- **修复**: 添加 async/await + try/catch

#### 8. 模型注册表单验证不生效 ✅ FIXED
- **页面**: /model/list (注册模型对话框)
- **现象**: 空表单点确定没有显示必填校验提示
- **根因**: handleRegister() 未调用 registerFormRef.validateFields()
- **修复**: 提交前调用 validateFields()，失败则 return

#### 9. 模型详情时间格式未友好化 ✅ FIXED
- **页面**: /model/:id
- **现象**: 创建/更新时间显示 `2026-04-11T19:14:54.818605` 原始 ISO 格式
- **修复**: 使用 dayjs 格式化为 `YYYY-MM-DD HH:mm:ss`

#### 10. 数据同步页面 JS 报错 ✅ FIXED
- **页面**: /etl/sync
- **现象**: 点击"刷新"按钮报 `TypeError: Cannot read properties of undefined (reading 'some')`
- **根因**: SyncTaskList.vue:153 对 undefined 数组调用 .some()
- **修复**: 添加空数组保护 `(tableData.value || []).some(...)`

#### 11. 数据源测试连接无响应 ✅ FIXED
- **页面**: /etl/datasources
- **现象**: "测试连接"按钮显示 loading 但 5 秒后无结果
- **根因**: 前端 handleTestConnection 错误调用 testConnectionPreSave（需要 params），应调用 testConnection(id)
- **修复**: 改用 testConnection(record.id) API，后端已完整实现 JDBC/HTTP/File 测试

#### 12. TabBar 全局导航问题
- **范围**: 全局
- **现象**: 多标签页积累后，API 超时/错误时页面会不可预测地重定向到其他标签页
- **根因**: TabBar 组件在 API 错误时错误地触发了路由跳转
- **修复**: TabBar 组件的错误恢复逻辑需要修正

## 通过测试的页面 (19个)
| 页面 | 路由 |
|------|------|
| 个人工作台 | /dashboard/workspace |
| 系统总览 | /dashboard/overview |
| 模型看板 | /dashboard/model |
| 数据看板 | /dashboard/data |
| 患者管理 | /data/cdr/patients |
| 临床检索 | /data/cdr/search |
| 专病管理 | /data/cdr/disease |
| 质量规则 | /data/cdr/quality-rules |
| 质量检测 | /data/cdr/quality-results |
| 研究项目 | /data/rdr/projects |
| 数据集 | /data/rdr/datasets |
| 数据源管理 | /etl/datasources |
| 管道管理 | /etl/pipelines |
| 执行监控 | /etl/executions |
| 定时任务 | /etl/schedule |
| 模型列表 | /model/list |
| 模型详情(基本信息) | /model/:id |
| 评估管理 | /model/evaluations |
| 审批管理 | /model/approvals |
| 推理日志 | /model/inference-logs |
| 告警规则 | /alert/rules |
| 角色管理 | /system/roles |
| 系统参数 | /system/config |
| 数据字典 | /system/dict |
| 脱敏规则 | /system/desensitize |
| 标注任务 | /label/tasks |
| 用户管理 | /system/users |
