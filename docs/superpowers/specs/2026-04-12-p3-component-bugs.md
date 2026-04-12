# P3: 弹窗+组件bug修复

## 目标
修复测试发现的4个组件级bug，使CRUD弹窗、数据看板、标注路由、模型详情按钮正常工作。

## Bug清单

### Bug 1: useModal 类型不匹配（P0）

**文件**: `maidc-portal/src/hooks/useModal.ts`
**根因**: 返回 `Ref<boolean>` 类型的 `visible`，传给 Ant Design Vue 的 `<a-modal :open="xxx.visible">` 时，`:open` 期望 `boolean` 而非 `Ref<boolean>`，Vue 报类型错误，弹窗无法正常显示/隐藏。
**修复**: 在 useModal hook 中将 `visible` 改为 `computed<boolean>`，或在所有使用处加 `.value`。推荐在 hook 内部统一处理，避免逐个修改调用方。

### Bug 2: DataDashboard StatusBadge（P1）

**文件**: `maidc-portal/src/views/dashboard/DataDashboard.vue`
**根因**: ETL任务表格中 `statusLabel` 使用中文文本（"已完成"），`statusType` 使用颜色字符串（"success"），而非 StatusBadge 组件期望的枚举值。
**修复**: 将 `statusType` 改为有效的 StatusType 值（如 `'sync'`、`'active'`、`'error'`），`statusLabel` 改为对应的英文状态码。

### Bug 3: LabelTaskDetail 路由缺失（P1）

**文件**: `maidc-portal/src/router/asyncRoutes.ts`
**根因**: `LabelTaskList.vue` 点击"查看"时导航到 `/label/detail/{id}`，但路由表只定义了 `/label/workspace/:id`。`LabelTaskDetail.vue` 组件存在但未注册路由。
**修复**: 在 asyncRoutes 的 label 路由组中添加 `{ path: 'detail/:id', component: () => import('@/views/label/LabelTaskDetail.vue') }`。

### Bug 4: ModelDetail 按钮无事件（P2）

**文件**: `maidc-portal/src/views/model/ModelDetail.vue`
**根因**: 编辑、下载、注册版本、启动/停止部署等按钮只有视觉样式，没有 `@click` 事件处理。
**修复**: 为每个按钮添加对应的事件处理函数，调用对应的 API 接口。

## 修复顺序
1. Bug 1 (useModal) — 影响最广，22+测试点
2. Bug 2 (StatusBadge) — 导致页面crash
3. Bug 3 (路由缺失) — 导致404
4. Bug 4 (按钮事件) — 功能缺失

## 验证
- useModal 修复后：新建用户弹窗可正常打开/关闭
- StatusBadge 修复后：DataDashboard 页面无报错
- 路由修复后：点击标注任务"查看"可跳转到详情页
- 按钮修复后：模型详情页按钮可点击并触发API调用
