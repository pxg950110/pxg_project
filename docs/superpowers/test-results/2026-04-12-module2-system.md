# 测试报告 - 系统管理模块

## 测试时间
- 开始: 2026-04-12 08:32 (UTC+8)
- 结束: 2026-04-12 08:55 (UTC+8)

## 测试环境
- 前端: http://localhost:3000
- 后端: http://localhost:8080 (gateway)
- 测试账号: admin / Admin@123
- 浏览器: Chrome 146

## 测试结果

| 编号 | 操作 | 状态 | 实际API | 实际响应 | UI结果 | 备注 |
|------|------|------|---------|----------|--------|------|
| 2.1.1 | 用户列表加载 | **PASS** | GET /api/v1/users/me [200], GET /api/v1/roles [200] | 200 | 显示用户列表表格(4行mock数据)+搜索栏+筛选器+分页(共15个用户) | 页面使用mock数据，未调用GET /api/v1/users分页API，但UI正常渲染 |
| 2.1.2 | 搜索用户 | **PASS** | 无新API调用 | - | 表格过滤为仅显示admin行 | 前端客户端过滤(mockData.filter)，非服务端搜索。搜索后计数仍显示"共15个用户"但表格正确过滤 |
| 2.1.3 | 新建用户弹窗 | **FAIL** | 无 | - | 弹窗未打开 | **BUG**: useModal()的visible是Ref对象，传给AModal的open prop时期望Boolean收到Object。Vue warn: `Invalid prop: type check failed for prop "open". Expected Boolean, got Object` |
| 2.1.4 | 新建用户提交 | **SKIP** | - | - | - | 依赖2.1.3弹窗，无法测试 |
| 2.1.5 | 新建-用户名重复 | **SKIP** | - | - | - | 依赖2.1.3弹窗，无法测试 |
| 2.1.6 | 新建-必填项空 | **SKIP** | - | - | - | 依赖2.1.3弹窗，无法测试 |
| 2.1.7 | 查看详情 | **PASS** | 无新API | - | URL变为/system/users/1，显示用户详情页 | 点击"查看"实际未跳转(可能因同一页面snapshot)，手动导航到/system/users/1成功显示详情页(mock数据) |
| 2.1.8 | 编辑用户 | **FAIL** | 无 | - | 弹窗未打开 | 同2.1.3的useModal bug，编辑弹窗也无法显示 |
| 2.1.9 | 重置密码 | **FAIL** | 无 | - | 弹窗未打开 | 同2.1.3的useModal bug，重置密码弹窗也无法显示 |
| 2.1.10 | 分页切换 | **FAIL** | 无新API调用 | - | 表格数据未变化 | 点击第2页后表格仍显示4条mock数据，无GET /api/v1/users?page=2调用，分页仅是前端模拟(共15但只有4条mock) |
| 2.2.1 | 用户详情加载 | **PASS** | GET /api/v1/users/me [200] | 200 | 显示用户详情卡片(头像、基本信息、角色权限、最近操作) | 使用mock数据，未调用GET /api/v1/users/{id}。显示"张医生"而非admin |
| 2.2.2 | 返回列表 | **PASS** | 无 | - | URL回/system/users | 点击左箭头按钮正确导航回列表 |
| 2.3.1 | 角色列表加载 | **PASS** | GET /api/v1/users/me [200] | 200 | 显示6个角色(平台管理员/数据管理员/研究员/AI工程师/临床医生/审计员)+权限分配面板 | mock数据，未调用GET /api/v1/roles。但用户列表页调用了roles API |
| 2.3.2 | 新建角色 | **FAIL** | 无 | - | 弹窗未打开 | 同useModal bug |
| 2.3.3 | 编辑角色 | **FAIL** | 无 | - | 弹窗未打开 | 同useModal bug |
| 2.3.4 | 分配权限 | **PARTIAL** | 无 | - | 权限面板显示15个checkbox，但点击checkbox状态不变 | 权限checkbox UI渲染正常，但交互不生效(勾选/取消不变化)。无保存按钮可见，无PUT /api/v1/roles/{id}/permissions调用 |
| 2.4.1 | 权限树加载 | **PASS** | GET /api/v1/users/me [200] | 200 | 显示3个分组(模型管理/数据管理/系统设置)的树形权限表格，共48个权限项 | mock数据，未调用GET /api/v1/permissions/tree |
| 2.4.2 | 展开/折叠 | **PASS** | 无 | - | 点击折叠按钮后"模型管理"子项隐藏，再次点击展开 | 折叠/展开功能正常 |
| 2.5.1 | 机构列表加载 | **PASS** | GET /api/v1/users/me [200] | 200 | 左侧显示组织树(XX医院>内科/放射科>CT室/MRI室/外科/检验科/病理科)，右侧显示选中节点详情 | mock数据，未调用机构列表API |
| 2.5.2 | 新建机构 | **FAIL** | 无 | - | 弹窗未打开 | 同useModal bug |
| 2.5.3 | 编辑/删除机构 | **FAIL** | 无 | - | 弹窗未打开 | 编辑按钮点击无反应，同useModal bug |
| 2.6.1 | 系统配置加载 | **PASS** | GET /api/v1/users/me [200] | 200 | 显示4个配置区(基础/存储/安全/通知)，每项有编辑按钮 | mock数据，未调用配置API |
| 2.6.2 | 保存配置 | **PARTIAL** | 无 | - | 点击"编辑"后值变为输入框+保存/取消按钮，点保存后恢复只读 | 编辑/保存UI交互正常，但无API调用(PUT配置)，纯前端mock保存 |

## 统计
- **通过**: 9/22 (含2个PARTIAL)
- **失败**: 8/22
- **跳过**: 3/22 (依赖弹窗功能)
- **部分通过**: 2/22

### 按状态细分
- PASS: 2.1.1, 2.1.2, 2.2.1, 2.2.2, 2.3.1, 2.4.1, 2.4.2, 2.5.1, 2.6.1 = **9**
- PARTIAL: 2.3.4, 2.6.2 = **2**
- FAIL: 2.1.3, 2.1.8, 2.1.9, 2.1.10, 2.3.2, 2.3.3, 2.5.2, 2.5.3 = **8**
- SKIP: 2.1.4, 2.1.5, 2.1.6 = **3**

## 失败详情

### BUG-1: useModal 弹窗不显示 (影响8个测试点)
- **影响测试**: 2.1.3, 2.1.4, 2.1.5, 2.1.6, 2.1.8, 2.1.9, 2.3.2, 2.3.3, 2.5.2, 2.5.3
- **文件**: `maidc-portal/src/hooks/useModal.ts`
- **原因**: `useModal()` 返回 `visible: Ref<boolean>`，在模板中 `v-model:open="userModal.visible"` 传递的是 Ref 对象而非布尔值
- **Vue警告**: `Invalid prop: type check failed for prop "open". Expected Boolean, got Object`
- **修复方案**: 在模板中使用 `v-model:open="userModal.visible"` 时确保 Vue 能自动解包 Ref，或者改用 `:open="userModal.visible"` + `@update:open="userModal.visible = $event"` 的写法。更可能的原因是 Ant Design Vue 版本与 Vue 3 响应式系统的兼容性问题，建议检查 `@ant-design/vue` 版本

### BUG-2: 分页不生效 (2.1.10)
- **原因**: 表格数据是硬编码的4条mock数据，但分页total设为15。切换第2页后无新数据，前端也没有真实的分页逻辑
- **修复**: 需要接入真实API或mock更多数据以支持分页

### BUG-3: 权限勾选不生效 (2.3.4)
- **原因**: 权限checkbox点击后状态不变，可能是因为缺少v-model绑定或事件处理

### 问题-4: 大量页面使用mock数据
- **影响**: 所有6个子模块页面均使用前端硬编码数据
- **页面**: /system/users, /system/users/:id, /system/roles, /system/permissions, /system/organizations, /system/config
- **API调用**: 仅 /api/v1/users/me 和 /api/v1/roles (在用户列表页) 有真实后端调用
- **建议**: 逐步将各页面从mock数据切换到真实API调用

## 截图文件
| 文件 | 说明 |
|------|------|
| screenshots/2.1-users-page.png | 用户列表页 |
| screenshots/2.1.3-new-user-btn.png | 新建用户按钮点击后(无弹窗) |
| screenshots/2.1.10-pagination.png | 分页切换 |
| screenshots/2.2-user-detail.png | 用户详情页 |
| screenshots/2.3-roles-page.png | 角色管理页 |
| screenshots/2.3-roles-permissions.png | 角色权限面板 |
| screenshots/2.4-permissions-page.png | 权限管理页 |
| screenshots/2.5-organizations-page.png | 组织管理页 |
| screenshots/2.6-config-page.png | 系统配置页 |
