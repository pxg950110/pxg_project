# MAIDC UI集成测试计划

## 文档信息

- **版本**: v1.0
- **日期**: 2026-04-12
- **执行工具**: chrome-devtools MCP
- **测试范围**: 全部10个模块，43个页面，149个操作点

## 测试环境

| 项目 | 值 |
|------|-----|
| 前端地址 | http://localhost:5173 (maidc-portal) |
| 后端地址 | http://localhost:8080 (gateway) |
| 浏览器 | Chrome (via chrome-devtools MCP) |
| 预置账号 | admin/admin123 (管理员), user/user123 (普通用户) |

## 执行状态汇总表

| 模块 | 页面数 | 操作点数 | 通过 | 失败 | 未执行 |
|------|--------|----------|------|------|--------|
| 1. 认证 | 1 | 7 | - | - | 7 |
| 2. 系统管理 | 6 | 18 | - | - | 18 |
| 3. 仪表盘 | 3 | 5 | - | - | 5 |
| 4. 模型管理 | 8 | 36 | - | - | 36 |
| 5. 数据管理 | 11 | 32 | - | - | 32 |
| 6. 标注管理 | 2 | 11 | - | - | 11 |
| 7. 任务调度 | 1 | 8 | - | - | 8 |
| 8. 告警中心 | 3 | 9 | - | - | 9 |
| 9. 审计日志 | 4 | 10 | - | - | 10 |
| 10. 消息中心 | 4 | 13 | - | - | 13 |
| **合计** | **43** | **149** | **0** | **0** | **149** |

## 校验方式说明

| 校验方式 | 工具 | 用途 |
|----------|------|------|
| snapshot | `take_snapshot` | 检查页面元素、文本、状态 |
| network | `list_network_requests` + `get_network_request` | 确认API请求已发出，校验response status和body |
| URL | `evaluate_script` 检查 `window.location.pathname` | 确认页面跳转 |

---

## 1. 认证模块

### 1.1 登录页 `/login`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 1.1.1 | 页面加载 | navigate到/login | 无 | - | 显示登录表单，含用户名/密码输入框和登录按钮 | snapshot检查表单元素 |
| 1.1.2 | 空表单提交 | 点击"登录"按钮 | 无（前端校验拦截） | - | 表单校验提示"请输入用户名/密码" | snapshot检查错误提示文本 |
| 1.1.3 | 只填用户名 | 填写用户名，密码留空，点击登录 | 无（前端校验拦截） | - | 提示"请输入密码" | snapshot检查 |
| 1.1.4 | 错误凭据 | 填写错误用户名+密码，点击登录 | `POST /api/v1/auth/login` | `401 {code:401, msg:"用户名或密码错误"}` | 表单上方显示错误提示 | network检查status和msg |
| 1.1.5 | 正确登录 | 填写正确用户名+密码，点击登录 | `POST /api/v1/auth/login` | `200 {code:200, data:{token:"xxx", user:{...}}}` | 跳转到 `/dashboard/overview`，localStorage存入token | network + URL变化 |
| 1.1.6 | 密码显隐切换 | 点击密码框右侧眼睛图标 | 无 | - | 密码明文/密文切换 | snapshot检查input type |
| 1.1.7 | 退出登录 | 点击用户头像→退出 | `POST /api/v1/auth/logout` | `200 {code:200, msg:"success"}` | 跳转回/login，localStorage清空token | network + URL |

---

## 2. 系统管理模块

### 2.1 用户列表 `/system/users`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 2.1.1 | 页面加载 | navigate到/system/users | `GET /api/v1/users?page=1&size=10` | `200 {data:{content:[...], totalElements:N}}` | 显示用户列表表格，含搜索栏 | network + snapshot |
| 2.1.2 | 搜索用户 | 输入关键词，点击搜索 | `GET /api/v1/users?keyword=xxx&page=1` | `200 {data:{content:[匹配项]}}` | 表格显示过滤结果 | network + snapshot |
| 2.1.3 | 新建用户-打开弹窗 | 点击"新建"按钮 | 无（前端打开弹窗） | - | 弹出新建用户表单弹窗 | snapshot |
| 2.1.4 | 新建用户-提交 | 填写表单，点击确定 | `POST /api/v1/users` body:{username,name,email,phone,roleId} | `200 {code:200, data:{id:"xxx"}}` | 弹窗关闭，列表刷新，提示"创建成功" | network + snapshot |
| 2.1.5 | 新建用户-用户名重复 | 填写已存在用户名，点击确定 | `POST /api/v1/users` | `409 {code:409, msg:"用户名已存在"}` | 表单提示错误信息 | network + snapshot |
| 2.1.6 | 新建用户-必填项为空 | 不填必填项，点击确定 | 无（前端校验） | - | 标红必填字段，提示"必填" | snapshot |
| 2.1.7 | 查看用户详情 | 点击某行"查看" | 无（跳转） | - | 跳转到`/system/users/{id}` | URL |
| 2.1.8 | 编辑用户 | 点击"编辑"，修改字段，确定 | `PUT /api/v1/users/{id}` body:{name,email,phone} | `200 {code:200}` | 弹窗关闭，列表刷新 | network + snapshot |
| 2.1.9 | 重置密码 | 点击"重置密码"，确认 | `PUT /api/v1/users/{id}/reset-password` | `200 {code:200}` | 提示"密码已重置为默认密码" | network + snapshot |
| 2.1.10 | 分页切换 | 点击第2页 | `GET /api/v1/users?page=2&size=10` | `200 {data:{content:[]}}` | 表格更新数据，分页高亮第2页 | network + snapshot |

### 2.2 用户详情 `/system/users/:id`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 2.2.1 | 页面加载 | navigate到详情页 | `GET /api/v1/users/{id}` | `200 {data:{id,username,name,email,phone,roles}}` | 显示用户详细信息卡片 | network + snapshot |
| 2.2.2 | 返回列表 | 点击返回按钮 | 无 | - | 跳转回/system/users | URL |

### 2.3 角色列表 `/system/roles`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 2.3.1 | 页面加载 | navigate到/system/roles | `GET /api/v1/roles` | `200 {data:[{id,name,description,permissions}]}` | 显示角色列表 | network + snapshot |
| 2.3.2 | 新建角色 | 点击新建，填写名称/描述，确定 | `POST /api/v1/roles` body:{name,description} | `200 {data:{id}}` | 列表刷新，提示成功 | network + snapshot |
| 2.3.3 | 编辑角色 | 点击编辑，修改描述，确定 | `PUT /api/v1/roles/{id}` body:{name,description} | `200` | 列表刷新 | network + snapshot |
| 2.3.4 | 分配权限 | 点击"权限"按钮，勾选菜单项，确定 | `PUT /api/v1/roles/{id}/permissions` body:{permissionIds:[]} | `200` | 提示"权限分配成功" | network + snapshot |

### 2.4 权限树 `/system/permissions`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 2.4.1 | 页面加载 | navigate到/system/permissions | `GET /api/v1/permissions/tree` | `200 {data:[{id,name,children:[]}]}` | 显示树形权限结构 | network + snapshot |
| 2.4.2 | 展开/折叠节点 | 点击树节点箭头 | 无 | - | 子节点展开/收起 | snapshot |

### 2.5 机构管理 `/system/organizations`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 2.5.1 | 页面加载 | navigate到/system/organizations | GET机构列表接口 | `200` | 显示机构列表 | network + snapshot |
| 2.5.2 | 新建机构 | 点击新建，填写名称/编码/类型，确定 | POST创建机构 | `200 {data:{id}}` | 列表刷新，提示成功 | network + snapshot |
| 2.5.3 | 查看详情 | 点击某行"查看" | 无（跳转） | - | 跳转详情页 | URL |
| 2.5.4 | 编辑机构 | 点击编辑，修改，确定 | PUT机构接口 | `200` | 列表刷新 | network + snapshot |
| 2.5.5 | 删除机构 | 点击删除，确认 | DELETE机构接口 | `200` | 列表刷新，提示成功 | network + snapshot |

### 2.6 系统配置 `/system/config`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 2.6.1 | 页面加载 | navigate到/system/config | GET配置接口 | `200 {data:{key:value,...}}` | 显示配置表单/列表 | network + snapshot |
| 2.6.2 | 保存配置 | 修改配置值，点击保存 | PUT配置接口 body:{key:value} | `200` | 提示"保存成功" | network + snapshot |

---

## 3. 仪表盘模块

### 3.1 系统总览 `/dashboard/overview`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 3.1.1 | 页面加载 | navigate到/dashboard/overview | 多个GET统计接口 | `200` 各接口返回统计数据 | 显示统计卡片（用户数/模型数/数据量/任务数）+ 趋势图表 | network + snapshot |
| 3.1.2 | 时间范围切换 | 点击"近7天/30天/90天"切换按钮 | `GET ...?range=7d` | `200 {data:[...]}` | 图表数据更新，按钮高亮变化 | network + snapshot |

### 3.2 模型看板 `/dashboard/model`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 3.2.1 | 页面加载 | navigate到/dashboard/model | `GET /api/v1/models` + `GET /api/v1/deployments` | `200` | 显示模型统计卡片、部署状态图表 | network + snapshot |
| 3.2.2 | 点击模型卡片 | 点击某个模型卡片 | 无（跳转） | - | 跳转到该模型详情页 `/model/{id}` | URL |

### 3.3 数据看板 `/dashboard/data`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 3.3.1 | 页面加载 | navigate到/dashboard/data | GET数据统计接口 | `200 {data:{patientCount,datasetCount,etlStats}}` | 显示数据量/患者数/ETL统计图表 | network + snapshot |
| 3.3.2 | 点击数据卡片 | 点击某个数据卡片 | 无（跳转） | - | 跳转到对应数据管理页 | URL |

---

## 4. 模型管理模块

### 4.1 模型列表 `/model/list`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 4.1.1 | 页面加载 | navigate到/model/list | `GET /api/v1/models?page=1&size=10` | `200 {data:{content:[],totalElements}}` | 显示模型表格 | network + snapshot |
| 4.1.2 | 搜索模型 | 输入名称，点击搜索 | `GET /api/v1/models?name=xxx` | `200` | 表格过滤结果 | network + snapshot |
| 4.1.3 | 按状态筛选 | 选择状态下拉（训练中/已部署/已下线） | `GET /api/v1/models?status=xxx` | `200` | 表格过滤 | network + snapshot |
| 4.1.4 | 新建模型-打开弹窗 | 点击"新建" | 无（前端打开弹窗） | - | 弹出新建模型表单弹窗 | snapshot |
| 4.1.5 | 新建模型-提交 | 填写name/type/description，确定 | `POST /api/v1/models` body:{name,type,description,...} | `200 {data:{id}}` | 提示成功，列表刷新 | network + snapshot |
| 4.1.6 | 新建模型-必填项为空 | 不填name，点击确定 | 无（前端校验） | - | name字段标红提示"请输入模型名称" | snapshot |
| 4.1.7 | 查看模型详情 | 点击模型名或"查看" | 无（跳转） | - | 跳转`/model/{id}` | URL |
| 4.1.8 | 编辑模型 | 点击编辑，修改description，确定 | `PUT /api/v1/models/{id}` body:{description:"new"} | `200` | 提示成功 | network + snapshot |
| 4.1.9 | 删除模型 | 点击删除，确认弹窗点确定 | `DELETE /api/v1/models/{id}` | `200` | 提示成功，列表刷新，该行消失 | network + snapshot |
| 4.1.10 | 删除模型-取消 | 点击删除，确认弹窗点取消 | 无 | - | 无操作，列表不变 | snapshot |

### 4.2 模型详情 `/model/:id`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 4.2.1 | 页面加载 | navigate到/model/{id} | `GET /api/v1/models/{id}` | `200 {data:{id,name,type,description,status,...}}` | 显示模型详情信息+Tab页（版本/评估/部署/审批） | network + snapshot |
| 4.2.2 | 版本Tab | 点击"版本"Tab | `GET /api/v1/models/{id}/versions` | `200 {data:[]}` | 显示版本列表 | network + snapshot |
| 4.2.3 | 上传新版本 | 点击上传，选择文件，确定 | `POST /api/v1/models/{id}/versions` (multipart) | `200 {data:{versionId}}` | 显示上传进度条，完成后提示成功 | network + snapshot |
| 4.2.4 | 下载版本 | 点击下载按钮 | `GET /api/v1/models/{id}/versions/{vid}/download` | `200` 文件流 | 浏览器触发文件下载 | network |
| 4.2.5 | 版本对比 | 选择两个版本，点击对比 | `GET /api/v1/models/{id}/versions/compare?v1=x&v2=y` | `200 {data:{diff}}` | 显示Diff对比视图 | network + snapshot |
| 4.2.6 | 返回列表 | 点击面包屑或返回按钮 | 无 | - | 跳转/model/list | URL |

### 4.3 部署列表 `/model/deployments`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 4.3.1 | 页面加载 | navigate到/model/deployments | `GET /api/v1/deployments` | `200 {data:[]}` | 显示部署列表 | network + snapshot |
| 4.3.2 | 新建部署 | 点击新建，选择模型+版本+配置，确定 | `POST /api/v1/deployments` body:{modelId,versionId,config} | `200 {data:{id}}` | 提示成功，列表刷新 | network + snapshot |
| 4.3.3 | 启动部署 | 点击"启动"按钮 | `PUT /api/v1/deployments/{id}/start` | `200` | 状态变为"运行中"，绿色指示 | network + snapshot |
| 4.3.4 | 停止部署 | 点击"停止"，确认 | `PUT /api/v1/deployments/{id}/stop` | `200` | 状态变为"已停止"，灰色指示 | network + snapshot |
| 4.3.5 | 扩缩容 | 点击"扩缩容"，修改实例数，确定 | `PUT /api/v1/deployments/{id}/scale` body:{replicas:N} | `200` | 实例数更新 | network + snapshot |
| 4.3.6 | 重启部署 | 点击"重启"，确认 | `POST /api/v1/deployments/{id}/restart` | `200` | 提示成功，状态刷新 | network + snapshot |
| 4.3.7 | 查看日志 | 点击"日志" | `GET /api/v1/monitoring/deployments/{id}/logs` | `200 {data:[logLines]}` | 弹出日志面板，显示实时日志 | network + snapshot |
| 4.3.8 | 查看指标 | 点击"指标" | `GET /api/v1/monitoring/deployments/{id}/metrics` | `200 {data:{cpu,mem,qps}}` | 显示CPU/内存/QPS图表 | network + snapshot |

### 4.4 部署详情 `/model/deployments/:id`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 4.4.1 | 页面加载 | navigate到详情页 | `GET /api/v1/deployments/{id}/status` | `200 {data:{status,replicas,createTime}}` | 显示部署详情卡片+状态 | network + snapshot |
| 4.4.2 | 在线推理 | 输入JSON参数，点击"推理" | `POST /api/v1/inference/{deploymentId}` body:{input} | `200 {data:{result}}` | 显示推理结果JSON | network + snapshot |
| 4.4.3 | 推理-空参数 | 不填参数，点击推理 | 无（前端校验） | - | 提示"请输入推理参数" | snapshot |

### 4.5 审批列表 `/model/approvals`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 4.5.1 | 页面加载 | navigate到/model/approvals | `GET /api/v1/approvals` | `200 {data:[]}` | 显示审批列表 | network + snapshot |
| 4.5.2 | 提交审批 | 选择模型+版本，填写说明，提交 | `POST /api/v1/approvals` body:{modelId,versionId,description} | `200 {data:{id}}` | 提示成功，列表刷新 | network + snapshot |
| 4.5.3 | 审批通过 | 点击"通过"，填写意见，确定 | `PUT /api/v1/approvals/{id}/review` body:{approved:true,comment:"同意"} | `200` | 状态变为"已通过"，绿色标签 | network + snapshot |
| 4.5.4 | 审批驳回 | 点击"驳回"，填写原因，确定 | `PUT /api/v1/approvals/{id}/review` body:{approved:false,comment:"不符合要求"} | `200` | 状态变为"已驳回"，红色标签 | network + snapshot |
| 4.5.5 | 查看审批详情 | 点击某条审批 | `GET /api/v1/approvals/{id}` | `200 {data:{timeline:[...]}}` | 显示审批详情+时间线 | network + snapshot |

### 4.6 评估列表 `/model/evaluations`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 4.6.1 | 页面加载 | navigate到/model/evaluations | `GET /api/v1/evaluations` | `200 {data:[]}` | 显示评估任务列表 | network + snapshot |
| 4.6.2 | 新建评估 | 选择模型+数据集+配置参数，提交 | `POST /api/v1/evaluations` body:{modelId,datasetId,config} | `200 {data:{id}}` | 提示成功 | network + snapshot |
| 4.6.3 | 查看评估报告 | 点击"查看报告" | `GET /api/v1/evaluations/{id}/report` | `200 {data:{metrics,confusionMatrix,rocCurve}}` | 显示混淆矩阵+ROC曲线+指标卡 | network + snapshot |
| 4.6.4 | 查看评估详情 | 点击某条评估 | `GET /api/v1/evaluations/{id}` | `200` | 显示评估详情页 | network + snapshot |

### 4.7 路由配置 `/model/routes`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 4.7.1 | 页面加载 | navigate到/model/routes | GET路由配置接口 | `200 {data:[{id,rule,weight}]}` | 显示流量路由规则列表 | network + snapshot |
| 4.7.2 | 添加规则 | 点击添加，配置目标/权重/条件，保存 | POST路由规则 | `200` | 列表刷新 | network + snapshot |
| 4.7.3 | 编辑规则 | 修改权重，保存 | PUT路由规则 | `200` | 列表刷新 | network + snapshot |
| 4.7.4 | 删除规则 | 点击删除，确认 | DELETE路由规则 | `200` | 列表刷新，规则消失 | network + snapshot |

### 4.8 版本管理 `/model/versions`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 4.8.1 | 页面加载 | navigate到/model/versions | `GET /api/v1/models` + 版本列表 | `200` | 显示所有模型版本汇总 | network + snapshot |
| 4.8.2 | 版本对比 | 选择两个版本，点击对比 | `GET compare?v1=x&v2=y` | `200 {data:{diff}}` | 显示Diff对比视图 | network + snapshot |

---

## 5. 数据管理模块

### 5.1 患者列表 `/data/cdr/patients`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 5.1.1 | 页面加载 | navigate到/data/cdr/patients | `GET /api/v1/cdr/patients?page=1&size=10` | `200 {data:{content:[],totalElements}}` | 显示患者表格 | network + snapshot |
| 5.1.2 | 搜索患者 | 输入姓名/ID，点击搜索 | `GET /api/v1/cdr/patients?keyword=xxx` | `200` | 表格过滤 | network + snapshot |
| 5.1.3 | 查看详情 | 点击某行"查看" | 无（跳转） | - | 跳转`/data/cdr/patients/{id}` | URL |
| 5.1.4 | 查看患者360视图 | 点击"360视图"按钮 | `GET /api/v1/cdr/patients/{id}/360` | `200 {data:{patient,encounters,labs,medications,...}}` | 显示患者360全景视图（基本信息+就诊+检验+用药） | network + snapshot |
| 5.1.5 | 分页切换 | 点击第2页 | `GET /api/v1/cdr/patients?page=2&size=10` | `200` | 表格更新 | network + snapshot |

### 5.2 患者详情 `/data/cdr/patients/:id`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 5.2.1 | 页面加载 | navigate到详情页 | `GET /api/v1/cdr/patients/{id}` | `200 {data:{id,name,gender,birthDate,...}}` | 显示患者基本信息卡片+就诊记录Tab | network + snapshot |
| 5.2.2 | 查看就诊列表 | 点击"就诊记录"Tab | `GET /api/v1/cdr/patients/{id}/encounters` | `200 {data:[]}` | 显示就诊列表 | network + snapshot |
| 5.2.3 | 查看就诊详情 | 点击某次就诊 | 无（跳转） | - | 跳转`/data/cdr/patients/{id}/encounters/{eid}` | URL |
| 5.2.4 | 返回列表 | 点击返回 | 无 | - | 跳转回患者列表 | URL |

### 5.3 就诊详情 `/data/cdr/patients/:id/encounters/:encounterId`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 5.3.1 | 页面加载 | navigate到就诊详情 | `GET /api/v1/cdr/encounters/{id}` | `200 {data:{id,admissionDate,diagnosis,...}}` | 显示就诊详情（诊断+检验+用药+影像等Tab） | network + snapshot |

### 5.4 数据源管理 `/data/cdr/datasources`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 5.4.1 | 页面加载 | navigate到/data/cdr/datasources | GET数据源列表 | `200` | 显示数据源列表 | network + snapshot |
| 5.4.2 | 新建数据源 | 点击新建，填写连接信息，保存 | POST创建数据源 | `200 {data:{id}}` | 列表刷新 | network + snapshot |
| 5.4.3 | 测试连接 | 点击"测试连接" | POST测试连接 | `200 {data:{success:true}}` | 提示"连接成功" | network + snapshot |
| 5.4.4 | 测试连接失败 | 填写错误信息，测试 | POST测试连接 | `500 {msg:"连接失败"}` | 提示"连接失败" | network + snapshot |
| 5.4.5 | 查看详情 | 点击某行 | 无（跳转） | - | 跳转详情页 | URL |

### 5.5 数据源详情 `/data/cdr/datasources/:id`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 5.5.1 | 页面加载 | navigate到详情页 | GET数据源详情 | `200 {data:{id,name,type,config}}` | 显示数据源配置详情 | network + snapshot |
| 5.5.2 | 编辑数据源 | 修改配置，保存 | PUT数据源 | `200` | 提示成功 | network + snapshot |

### 5.6 同步任务 `/data/cdr/sync`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 5.6.1 | 页面加载 | navigate到/data/cdr/sync | GET同步任务列表 | `200` | 显示同步任务列表 | network + snapshot |
| 5.6.2 | 触发同步 | 点击"立即同步" | POST触发同步 | `200` | 状态变为"同步中" | network + snapshot |
| 5.6.3 | 查看同步日志 | 点击"日志" | GET同步日志 | `200 {data:[logLines]}` | 弹出日志面板 | network + snapshot |

### 5.7 质量规则 `/data/cdr/quality-rules`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 5.7.1 | 页面加载 | navigate到/data/cdr/quality-rules | `GET /api/v1/rdr/quality-rules` | `200 {data:[]}` | 显示规则列表 | network + snapshot |
| 5.7.2 | 新建规则 | 点击新建，配置字段/规则类型/阈值，保存 | `POST /api/v1/rdr/quality-rules` | `200` | 列表刷新 | network + snapshot |
| 5.7.3 | 编辑规则 | 修改阈值，保存 | `PUT /api/v1/rdr/quality-rules/{id}` | `200` | 列表刷新 | network + snapshot |
| 5.7.4 | 删除规则 | 点击删除，确认 | `DELETE /api/v1/rdr/quality-rules/{id}` | `200` | 列表刷新 | network + snapshot |

### 5.8 质量结果 `/data/cdr/quality-results`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 5.8.1 | 页面加载 | navigate到/data/cdr/quality-results | `GET /api/v1/rdr/quality-results` | `200` | 显示质量检测结果 | network + snapshot |
| 5.8.2 | 按状态筛选 | 选择通过/失败 | `GET ...?status=PASSED` | `200` | 列表过滤 | network + snapshot |
| 5.8.3 | 查看详情 | 点击某条结果 | `GET /api/v1/rdr/quality-results/{id}` | `200` | 显示检测详情弹窗 | network + snapshot |

### 5.9 脱敏规则 `/data/cdr/desensitize`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 5.9.1 | 页面加载 | navigate到/data/cdr/desensitize | GET脱敏规则列表 | `200` | 显示脱敏规则列表 | network + snapshot |
| 5.9.2 | 新建规则 | 配置字段+脱敏方式，保存 | POST脱敏规则 | `200` | 列表刷新 | network + snapshot |
| 5.9.3 | 预览脱敏效果 | 点击"预览" | POST预览接口 | `200 {data:{original:"张三",masked:"张*"}}` | 显示原文→脱敏对比 | network + snapshot |
| 5.9.4 | 编辑/删除 | 操作并确认 | `PUT/DELETE` | `200` | 列表刷新 | network + snapshot |

### 5.10 字典管理 `/data/cdr/dict`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 5.10.1 | 页面加载 | navigate到/data/cdr/dict | GET字典列表 | `200` | 显示字典树/列表 | network + snapshot |
| 5.10.2 | 新增字典项 | 点击添加，填写编码/名称/值，保存 | POST字典 | `200` | 列表刷新 | network + snapshot |
| 5.10.3 | 编辑字典项 | 修改值，保存 | PUT字典 | `200` | 列表刷新 | network + snapshot |
| 5.10.4 | 删除字典项 | 点击删除，确认 | DELETE字典 | `200` | 列表刷新 | network + snapshot |

### 5.11 科研项目 `/data/rdr/projects`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 5.11.1 | 页面加载 | navigate到/data/rdr/projects | `GET /api/v1/rdr/projects` | `200 {data:{content:[]}}` | 显示项目列表 | network + snapshot |
| 5.11.2 | 新建项目 | 填写项目名/描述/负责人，确定 | `POST /api/v1/rdr/projects` body:{name,description,leader} | `200 {data:{id}}` | 提示成功 | network + snapshot |
| 5.11.3 | 添加成员 | 点击"成员管理"，添加用户，确定 | `POST /api/v1/rdr/projects/{id}/members` body:{userId,role} | `200` | 成员列表刷新 | network + snapshot |
| 5.11.4 | 移除成员 | 点击移除按钮，确认 | `DELETE /api/v1/rdr/projects/{id}/members/{userId}` | `200` | 成员列表刷新 | network + snapshot |
| 5.11.5 | 删除项目 | 点击删除，确认 | `DELETE /api/v1/rdr/projects/{id}` | `200` | 列表刷新 | network + snapshot |

### 5.12 数据集 `/data/rdr/datasets`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 5.12.1 | 页面加载 | navigate到/data/rdr/datasets | `GET /api/v1/rdr/datasets` | `200` | 显示数据集列表 | network + snapshot |
| 5.12.2 | 新建数据集 | 填写名称/类型/关联项目，确定 | `POST /api/v1/rdr/datasets` body:{name,type,projectId} | `200` | 提示成功 | network + snapshot |
| 5.12.3 | 查看数据集详情 | 点击某行 | `GET /api/v1/rdr/datasets/{id}` | `200` | 显示数据集详情 | network + snapshot |
| 5.12.4 | 删除数据集 | 点击删除，确认 | `DELETE /api/v1/rdr/datasets/{id}` | `200` | 列表刷新 | network + snapshot |

### 5.13 ETL任务 `/data/rdr/etl`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 5.13.1 | 页面加载 | navigate到/data/rdr/etl | `GET /api/v1/etl/tasks` | `200 {data:[]}` | 显示ETL任务列表 | network + snapshot |
| 5.13.2 | 新建ETL任务 | 配置源/目标/字段映射，保存 | `POST /api/v1/etl/tasks` body:{source,target,mapping} | `200 {data:{id}}` | 提示成功 | network + snapshot |
| 5.13.3 | 触发执行 | 点击"执行" | `POST /api/v1/etl/tasks/{id}/trigger` | `200` | 状态变为"执行中" | network + snapshot |
| 5.13.4 | 暂停任务 | 点击"暂停" | `PUT /api/v1/etl/tasks/{id}/pause` | `200` | 状态变为"已暂停" | network + snapshot |
| 5.13.5 | 删除任务 | 点击删除，确认 | `DELETE /api/v1/etl/tasks/{id}` | `200` | 列表刷新 | network + snapshot |

---

## 6. 标注管理模块

### 6.1 标注任务列表 `/label/tasks`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 6.1.1 | 页面加载 | navigate到/label/tasks | `GET /api/v1/label/tasks` | `200 {data:[]}` | 显示标注任务列表 | network + snapshot |
| 6.1.2 | 新建任务 | 填写名称/选择数据集/配置，确定 | `POST /api/v1/label/tasks` body:{name,datasetId,config} | `200 {data:{id}}` | 提示成功 | network + snapshot |
| 6.1.3 | 编辑任务 | 点击编辑，修改配置，确定 | `PUT /api/v1/label/tasks/{id}` body:{name,config} | `200` | 列表刷新 | network + snapshot |
| 6.1.4 | 删除任务 | 点击删除，确认 | `DELETE /api/v1/label/tasks/{id}` | `200` | 列表刷新 | network + snapshot |
| 6.1.5 | 查看统计 | 点击"统计"按钮 | `GET /api/v1/label/tasks/{id}/stats` | `200 {data:{total,labeled,unlabeled,progress}}` | 显示统计弹窗（进度条+数字） | network + snapshot |
| 6.1.6 | AI预标注 | 点击"AI预标注"，确认 | `POST /api/v1/label/tasks/{id}/ai-preannotate` | `200` | 提示"预标注已开始" | network + snapshot |
| 6.1.7 | 进入工作台 | 点击"标注"按钮 | 无（跳转） | - | 跳转`/label/workspace/{id}` | URL |

### 6.2 标注工作台 `/label/workspace/:id`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 6.2.1 | 页面加载 | navigate到工作台 | `GET /api/v1/label/tasks/{id}` | `200 {data:{...}}` | 显示标注工作台（画布+左侧工具栏+右侧属性面板） | network + snapshot |
| 6.2.2 | 选择标注工具 | 点击矩形/圆形/多边形工具 | 无 | - | 工具高亮选中状态 | snapshot |
| 6.2.3 | 执行标注 | 在画布上拖拽绘制 | 无（前端渲染） | - | 画布上显示标注框 | snapshot |
| 6.2.4 | 保存标注 | 点击"保存" | PUT保存标注结果 | `200` | 提示"保存成功" | network + snapshot |
| 6.2.5 | 提交审核 | 点击"提交" | PUT提交标注 | `200` | 状态变为"待审核" | network + snapshot |
| 6.2.6 | 撤销/重做 | 点击撤销/重做按钮 | 无 | - | 标注操作撤销/恢复 | snapshot |

---

## 7. 任务调度模块

### 7.1 任务列表 `/schedule/tasks`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 7.1.1 | 页面加载 | navigate到/schedule/tasks | `GET /api/v1/task/schedules` | `200 {data:[]}` | 显示任务列表 | network + snapshot |
| 7.1.2 | 新建任务 | 配置任务名+Cron表达式+执行器，保存 | `POST /api/v1/task/schedules` body:{name,cron,executor} | `200 {data:{id}}` | 提示成功 | network + snapshot |
| 7.1.3 | 编辑任务 | 修改Cron或执行器，保存 | `PUT /api/v1/task/schedules/{id}` body:{cron,executor} | `200` | 列表刷新 | network + snapshot |
| 7.1.4 | 删除任务 | 点击删除，确认 | `DELETE /api/v1/task/schedules/{id}` | `200` | 列表刷新 | network + snapshot |
| 7.1.5 | 手动触发 | 点击"立即执行" | `POST /api/v1/task/schedules/{id}/trigger` | `200` | 提示"已触发" | network + snapshot |
| 7.1.6 | 暂停任务 | 点击"暂停" | `PUT /api/v1/task/schedules/{id}/pause` | `200` | 状态变为"已暂停" | network + snapshot |
| 7.1.7 | 恢复任务 | 点击"恢复" | `PUT /api/v1/task/schedules/{id}/resume` | `200` | 状态变为"运行中" | network + snapshot |
| 7.1.8 | 查看执行历史 | 点击"历史" | `GET /api/v1/task/schedules/{id}/executions` | `200 {data:[{id,status,startTime,endTime}]}` | 显示执行历史列表 | network + snapshot |

---

## 8. 告警中心模块

### 8.1 活跃告警 `/alert/active`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 8.1.1 | 页面加载 | navigate到/alert/active | `GET /api/v1/alerts` | `200 {data:[]}` | 显示告警列表（含严重级别标签） | network + snapshot |
| 8.1.2 | 按级别筛选 | 选择严重/警告/信息 | `GET /api/v1/alerts?level=CRITICAL` | `200` | 列表过滤 | network + snapshot |
| 8.1.3 | 确认告警 | 点击"确认" | `PUT /api/v1/alerts/{id}/acknowledge` | `200` | 状态变为"已确认"，标签变色 | network + snapshot |
| 8.1.4 | 查看详情 | 点击某条告警 | 无（跳转） | - | 跳转`/alert/detail/{id}` | URL |
| 8.1.5 | 查看告警历史 | 点击"历史"Tab | `GET /api/v1/alerts/history` | `200 {data:[]}` | 显示历史告警 | network + snapshot |

### 8.2 告警规则 `/alert/rules`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 8.2.1 | 页面加载 | navigate到/alert/rules | `GET /api/v1/alert-rules` | `200 {data:[]}` | 显示规则列表 | network + snapshot |
| 8.2.2 | 新建规则 | 配置指标/阈值/通知方式，保存 | `POST /api/v1/alert-rules` body:{metric,threshold,notify} | `200 {data:{id}}` | 提示成功 | network + snapshot |
| 8.2.3 | 编辑规则 | 修改阈值，保存 | `PUT /api/v1/alert-rules/{id}` body:{threshold} | `200` | 列表刷新 | network + snapshot |
| 8.2.4 | 启用/禁用规则 | 切换启用开关 | PUT规则状态 body:{enabled:true/false} | `200` | 开关状态变化 | network + snapshot |

### 8.3 告警详情 `/alert/detail/:id`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 8.3.1 | 页面加载 | navigate到/alert/detail/{id} | `GET /api/v1/alerts/{id}` + 历史接口 | `200` | 显示告警详情（基本信息+处理时间线+关联资源） | network + snapshot |
| 8.3.2 | 返回列表 | 点击返回 | 无 | - | 跳转回/alert/active | URL |

---

## 9. 审计日志模块

### 9.1 操作日志 `/audit/operations`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 9.1.1 | 页面加载 | navigate到/audit/operations | `GET /api/v1/audit/operations` | `200 {data:{content:[]}}` | 显示操作日志表格 | network + snapshot |
| 9.1.2 | 按时间筛选 | 选择日期范围 | `GET ...?startTime=x&endTime=y` | `200` | 列表过滤 | network + snapshot |
| 9.1.3 | 按操作类型筛选 | 选择类型下拉（创建/修改/删除/查询） | `GET ...?type=CREATE` | `200` | 列表过滤 | network + snapshot |
| 9.1.4 | 查看详情 | 点击某条日志 | `GET /api/v1/audit/operations/{id}` | `200 {data:{operator,action,detail,ip,time}}` | 弹出详情抽屉 | network + snapshot |

### 9.2 数据访问日志 `/audit/data-access`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 9.2.1 | 页面加载 | navigate到/audit/data-access | `GET /api/v1/audit/data-access` | `200` | 显示访问日志列表 | network + snapshot |
| 9.2.2 | 按用户筛选 | 选择用户下拉 | `GET ...?userId=xxx` | `200` | 列表过滤 | network + snapshot |
| 9.2.3 | 按数据类型筛选 | 选择患者/数据集/模型 | `GET ...?dataType=xxx` | `200` | 列表过滤 | network + snapshot |

### 9.3 系统事件 `/audit/system-events`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 9.3.1 | 页面加载 | navigate到/audit/system-events | `GET /api/v1/audit/events` | `200` | 显示系统事件列表 | network + snapshot |
| 9.3.2 | 按级别筛选 | 选择ERROR/WARN/INFO | `GET ...?level=ERROR` | `200` | 列表过滤 | network + snapshot |

### 9.4 合规报告 `/audit/compliance`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 9.4.1 | 页面加载 | navigate到/audit/compliance | `GET /api/v1/audit/reports/compliance` | `200 {data:{score,items:[],trends:[]}}` | 显示合规评分+检查项列表+趋势图 | network + snapshot |
| 9.4.2 | 选择时间范围 | 切换月份/季度 | `GET ...?period=2026-Q1` | `200` | 图表和数据更新 | network + snapshot |
| 9.4.3 | 导出报告 | 点击"导出" | GET导出接口 | `200` 文件流 | 浏览器下载PDF/Excel文件 | network |

---

## 10. 消息中心模块

### 10.1 消息列表 `/message/list`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 10.1.1 | 页面加载 | navigate到/message/list | `GET /api/v1/messages` | `200 {data:{content:[]}}` | 显示消息列表（已读/未读样式区分） | network + snapshot |
| 10.1.2 | 标记已读 | 点击某条未读消息 | `PUT /api/v1/messages/{id}/read` | `200` | 消息变为已读样式（灰色） | network + snapshot |
| 10.1.3 | 全部已读 | 点击"全部已读"按钮 | `PUT /api/v1/messages/read-all` | `200` | 所有消息变为已读样式 | network + snapshot |
| 10.1.4 | 查看未读数 | 页面顶部badge | `GET /api/v1/messages/unread-count` | `200 {data:5}` | 未读数badge显示正确数字 | network + snapshot |
| 10.1.5 | 查看消息详情 | 点击消息标题 | 无（跳转） | - | 跳转`/message/detail/{id}` | URL |
| 10.1.6 | 按类型筛选 | 选择系统/审批/告警消息 | `GET ...?type=SYSTEM` | `200` | 列表过滤 | network + snapshot |

### 10.2 消息详情 `/message/detail/:id`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 10.2.1 | 页面加载 | navigate到详情页 | `GET /api/v1/messages/{id}` | `200 {data:{title,content,from,time,type}}` | 显示消息详情内容 | network + snapshot |
| 10.2.2 | 返回列表 | 点击返回 | 无 | - | 跳转/message/list | URL |

### 10.3 通知设置 `/message/settings`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 10.3.1 | 页面加载 | navigate到/message/settings | `GET /api/v1/notifications/settings` | `200 {data:[{id,type,enabled}]}` | 显示通知类型开关列表 | network + snapshot |
| 10.3.2 | 切换通知开关 | 点击某个开关 | `PUT /api/v1/notifications/settings/{id}` body:{enabled:true/false} | `200` | 开关状态变化 | network + snapshot |
| 10.3.3 | 新增通知设置 | 点击添加，选择通知类型，保存 | `POST /api/v1/notifications/settings` body:{type,channel} | `200` | 列表刷新 | network + snapshot |

### 10.4 模板管理 `/message/templates`

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| 10.4.1 | 页面加载 | navigate到/message/templates | `GET /api/v1/notifications/templates` | `200 {data:[]}` | 显示模板列表 | network + snapshot |
| 10.4.2 | 新建模板 | 填写模板名+内容+变量，保存 | `POST /api/v1/notifications/templates` body:{name,content,variables} | `200` | 列表刷新 | network + snapshot |
| 10.4.3 | 编辑模板 | 修改内容，保存 | `PUT /api/v1/notifications/templates/{id}` body:{content} | `200` | 列表刷新 | network + snapshot |
| 10.4.4 | 预览模板 | 点击"预览" | 无或POST预览 | `200 {data:{rendered}}` | 显示渲染后的模板效果 | network + snapshot |

---

## 附录：错误页面测试

### 错误页面

| 编号 | 操作 | 步骤 | 预期API调用 | 预期响应 | UI预期结果 | 校验方式 |
|------|------|------|-------------|----------|------------|----------|
| E.1 | 403页面 | navigate到无权限页面 | 返回403 | - | 显示403禁止访问页面 | snapshot |
| E.2 | 404页面 | navigate到不存在路径 | 返回404 | - | 显示404页面未找到 | snapshot |
| E.3 | 500页面 | 触发服务端错误 | 返回500 | - | 显示500服务器错误页面 | snapshot |

---

## 附录：通用校验模式

### 列表页通用校验

每个列表页均需验证以下模式：

1. **加载态**: 数据加载中显示骨架屏或loading
2. **空状态**: 无数据时显示EmptyState组件
3. **分页**: 第1页/切换页码/总页数显示
4. **搜索**: 输入关键词搜索结果正确
5. **刷新**: 新增/编辑/删除后列表自动刷新

### 表单页通用校验

每个表单页均需验证以下模式：

1. **必填校验**: 必填项为空提交时标红提示
2. **格式校验**: 邮箱/手机号等格式错误时提示
3. **提交成功**: 成功后弹窗关闭+列表刷新+成功提示
4. **提交失败**: 失败时显示错误信息
5. **取消操作**: 点击取消不提交数据

### 删除操作通用校验

每个删除操作均需验证：

1. **确认弹窗**: 弹出确认对话框
2. **确认删除**: 删除成功，列表刷新，数据消失
3. **取消删除**: 取消后无操作
4. **关联提示**: 如有依赖数据，提示无法删除
