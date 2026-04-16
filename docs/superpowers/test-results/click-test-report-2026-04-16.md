# MAIDC 点击测试报告

> **日期**: 2026-04-16
> **分支**: `feat/p1-p4-backend-and-frontend-fixes`
> **测试环境**: 本地 (Docker 基础设施 + 8 Java 微服务 + Vite 前端)
> **浏览器**: Chrome

---

## 测试概要

| 统计 | 数量 |
|------|------|
| 测试页面数 | 30+ |
| P0 严重 | 1 |
| P1 高优 | 1 |
| P2 中等 | 3 |
| 总问题数 | 5 |

---

## BUG-001: SPA 侧边栏导航组件不更新 (P0 严重)

**现象**: 点击侧边栏菜单项切换页面时，页面内容不更新，继续显示上一个页面的组件。从部署管理点击到流量路由/版本管理/推理日志/患者管理均复现。

**URL 直接访问**: 全部正常，页面正确渲染。

**控制台错误**:
```
TypeError: Cannot read properties of null (reading 'parentNode')
Vue Router warn: uncaught error during route navigation
```
每次导航累积一个 `Uncaught (in promise)` 错误。

**根因**: `BasicLayout.vue:41` 中：
```vue
<component :is="Component" :key="route.path" />
```
移除 `keep-alive` 后，`:key="route.path"` 导致 Vue 在卸载旧组件时 DOM 节点引用已为 null，组件无法正确替换。

**影响范围**: 所有侧边栏菜单导航（影响 30+ 页面）。

**修复方案**:
```vue
<!-- 方案 A: 添加 Transition 包裹 -->
<router-view v-slot="{ Component, route }">
  <transition name="fade" mode="out-in">
    <component :is="Component" :key="route.path" />
  </transition>
</router-view>

<!-- 方案 B: 移除 key 绑定 -->
<router-view v-slot="{ Component }">
  <component :is="Component" />
</router-view>
```

**文件**: `maidc-portal/src/layouts/BasicLayout.vue:40-42`

---

## BUG-002: 模型详情页"编辑"和"注册新版本"弹窗不弹出 (P1 高)

**现象**: 点击"编辑"或"注册新版本"按钮无任何反应。

**根因**: `ModelDetail.vue` 中定义了 `editModal = useModal()` 和 `versionModal = useModal()`，以及 `showEditModal()` / `showVersionModal()` 函数，但 **template 中没有对应的 `<a-modal>` 组件**。`useModal.visible` 被设为 `true`，但没有任何 UI 元素绑定它。

**对比**: 用户管理的"新建用户"弹窗正常工作（UserList.vue 有完整的 `<a-modal>` 定义）。

**影响**: 模型详情页无法编辑模型信息、无法注册新版本。

**修复方案**: 在 `</a-tabs>` 之后、`</PageContainer>` 之前添加两个 `<a-modal>` 组件。

**文件**: `maidc-portal/src/views/model/ModelDetail.vue:20-22, 208-217`

---

## BUG-003: 模型详情页多个字段值为空 (P2 中)

**现象**: 模型详情页的 Descriptions 组件中多个字段只显示标签不显示值：Model ID、类型、任务、所属项目、负责人、创建时间、更新时间、最新版本、标签。

**已显示的字段**: 框架=PYTORCH，状态=草稿，描述=AI lung nodule detection model

**根因分析**: API 返回 `{"modelCode":"CT-LUNG-001","modelType":"IMAGING","taskType":"OBJECT_DETECTION",...}`（camelCase），而 `loadModelDetail()` 映射时使用 `data.model_code`（snake_case）。Vite proxy 没有对 JSON key 做转换。

**API 实际返回**:
```json
{
  "modelCode": "CT-LUNG-001",
  "modelName": "CT Lung Nodule Detection",
  "modelType": "IMAGING",
  "taskType": "OBJECT_DETECTION",
  "framework": "PYTORCH",
  "status": "DRAFT",
  "ownerName": null,
  "createdAt": "2026-04-11T19:14:54.818605",
  "updatedAt": "2026-04-11T19:14:54.818605"
}
```

**修复方案**: 将 `loadModelDetail()` 中的 `data.model_code` 改为 `data.modelCode`，`data.model_type` 改为 `data.modelType` 等，全部使用 camelCase。

**文件**: `maidc-portal/src/views/model/ModelDetail.vue:247-263`

---

## BUG-004: 用户列表多列数据为空 (P2 中)

**现象**: admin 用户行只显示了 `#`(1)、用户名(admin)、状态(ACTIVE)。姓名、邮箱、角色列全部为空。

**API 返回**:
```json
{"id":1,"username":"admin","realName":"系统管理员","roles":["admin"],"orgId":0}
```

**根因**: API 返回 camelCase（`realName`），前端列定义使用 `dataIndex` 可能指向了 snake_case 字段名，或 API 未返回邮箱字段。

**文件**: `maidc-portal/src/views/system/UserList.vue`

---

## BUG-005: 数据看板患者总数增长率显示异常 (P2 低)

**现象**: 数据看板中"患者总数"增长显示 **234%**，数值不合理（可能为 mock 数据或百分比计算错误）。

**影响**: 仅影响仪表盘展示，不影响功能。

**文件**: `maidc-portal/src/views/dashboard/DataDashboard.vue`

---

## 正常功能确认清单

以下页面/功能通过 URL 直接访问均正常：

| 模块 | 页面 | 状态 |
|------|------|------|
| 登录 | LoginPage | ✅ 正常登录 |
| 仪表盘 | 系统总览/模型看板/数据看板 | ✅ 3页正常 |
| 模型管理 | 模型列表 | ✅ 显示2条API数据 |
| 模型管理 | 模型详情 | ✅ 基本信息加载正常（部分字段空） |
| 模型管理 | 评估管理/审批管理 | ✅ 表格结构正常，无数据 |
| 模型管理 | 部署管理 | ✅ 统计卡片+表格正常 |
| 模型管理 | 流量路由 | ✅ 3条mock路由数据展示 |
| 模型管理 | 版本管理 | ✅ 表格+对比功能正常 |
| 模型管理 | 推理日志 | ✅ 搜索+表格正常 |
| 数据管理 | 患者管理 | ✅ 显示1条API数据 |
| 数据管理 | 研究项目/数据集 | ✅ 结构正常，无数据 |
| 标注管理 | 标注任务 | ✅ 6条mock数据丰富展示 |
| 任务调度 | 定时任务 | ✅ 正常 |
| 告警中心 | 活跃告警 | ✅ 统计+表格正常 |
| 审计日志 | 操作审计 | ✅ 搜索+表格正常 |
| 消息中心 | 我的消息 | ✅ 消息分类正常 |
| 系统设置 | 用户管理 | ✅ 弹窗正常 |
| 系统设置 | 角色管理 | ✅ 权限树正常 |
| 系统设置 | 系统配置 | ✅ API数据加载正常 |
| API | 登录/模型/数据/审计/消息 | ✅ 全部200 |

---

## 修复优先级建议

| 优先级 | Bug | 预估工作量 |
|--------|-----|-----------|
| **立即修复** | BUG-001 SPA导航失败 | 5 min（改 BasicLayout.vue 2行） |
| **立即修复** | BUG-002 弹窗缺失 | 15 min（添加2个 Modal 模板） |
| **本轮修复** | BUG-003 字段映射 | 10 min（改 camelCase 映射） |
| **本轮修复** | BUG-004 用户列映射 | 10 min（检查列定义） |
| **可延后** | BUG-005 增长率异常 | 5 min（修正 mock 数据） |

> **文档结束** — MAIDC 点击测试报告 2026-04-16
