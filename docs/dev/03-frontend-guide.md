# MAIDC 前端开发指南

> **版本**: v1.0
> **日期**: 2026-04-11
> **状态**: 已确认
> **技术栈**: Vue 3 + TypeScript + Vite + Pinia + Ant Design Vue 4.x
> **关联**: PRD `docs/superpowers/specs/2026-04-08-maidc-design.md`

---

## 目录

1. [项目脚手架](#1-项目脚手架)
2. [路由设计](#2-路由设计)
3. [组件规范](#3-组件规范)
4. [状态管理](#4-状态管理)
5. [API 层封装](#5-api-层封装)
6. [页面开发模板](#6-页面开发模板)

---

## 1. 项目脚手架

### 1.1 技术栈选型

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4+ | 渐进式前端框架 |
| TypeScript | 5.x | 类型安全 |
| Vite | 5.x | 构建工具（HMR / 按需编译） |
| Pinia | 2.x | 状态管理（取代 Vuex） |
| Ant Design Vue | 4.x | UI 组件库 |
| @ant-design/vue-pro-components | - | ProTable / ProForm / ProLayout |
| Vue Router | 4.x | 路由管理 |
| Axios | 1.x | HTTP 客户端 |
| Day.js | - | 日期处理（轻量级，AntDV 内置） |
| ECharts | 5.x | 图表（监控指标/评估报告） |
| ViteSSR / SSR | - | 预留（Phase 5 考虑） |

### 1.2 目录结构规范

```
maidc-portal/
├── public/
│   └── favicon.ico
│
├── src/
│   ├── api/                          ← 按后端服务拆分的 API 调用层
│   │   ├── auth.ts                   ← auth-service 接口
│   │   ├── model.ts                  ← model-service 接口
│   │   ├── data.ts                   ← data-service 接口
│   │   ├── task.ts                   ← task-service 接口
│   │   ├── label.ts                  ← label-service 接口
│   │   ├── audit.ts                  ← audit-service 接口
│   │   └── msg.ts                    ← msg-service 接口
│   │
│   ├── components/                   ← 全局公共组件
│   │   ├── StatusBadge/              ← 状态徽章（统一状态展示）
│   │   ├── MetricCard/               ← 指标卡片（监控数据展示）
│   │   ├── JsonViewer/               ← JSON 树形展示器
│   │   ├── FileUploader/             ← 文件上传（MinIO 直传）
│   │   ├── KeyValueEditor/           ← 键值对编辑器（环境变量等）
│   │   ├── SchemaViewer/             ← 输入/输出 Schema 展示
│   │   ├── SearchForm/               ← 通用搜索表单
│   │   └── PageContainer/            ← 页面容器（面包屑+标题）
│   │
│   ├── layouts/                      ← 布局组件
│   │   ├── BasicLayout.vue           ← 主布局（侧边栏+顶栏+内容区）
│   │   ├── BlankLayout.vue           ← 空白布局（登录页）
│   │   └── PageShell.vue             ← 页面壳（面包屑+Tab缓存）
│   │
│   ├── views/                        ← 按模块分目录的页面
│   │   ├── dashboard/                ← 仪表盘（3 页）
│   │   │   ├── Overview.vue          ← 系统总览
│   │   │   ├── ModelDashboard.vue    ← 模型监控看板
│   │   │   └── DataDashboard.vue     ← 数据监控看板
│   │   │
│   │   ├── model/                    ← 模型管理（14 页）
│   │   │   ├── ModelList.vue         ← 模型列表
│   │   │   ├── ModelDetail.vue       ← 模型详情
│   │   │   ├── ModelForm.vue         ← 模型注册/编辑
│   │   │   ├── VersionList.vue       ← 版本列表
│   │   │   ├── VersionDetail.vue     ← 版本详情
│   │   │   ├── VersionUpload.vue     ← 版本上传
│   │   │   ├── VersionCompare.vue    ← 版本对比
│   │   │   ├── EvalList.vue          ← 评估列表
│   │   │   ├── EvalDetail.vue        ← 评估详情
│   │   │   ├── ApprovalList.vue      ← 审批列表
│   │   │   ├── ApprovalDetail.vue    ← 审批详情
│   │   │   ├── DeploymentList.vue    ← 部署列表
│   │   │   ├── DeploymentDetail.vue  ← 部署详情
│   │   │   └── RouteConfig.vue       ← 路由配置
│   │   │
│   │   ├── data-cdr/                 ← CDR 数据管理（14 页）
│   │   │   ├── PatientList.vue       ← 患者列表
│   │   │   ├── PatientDetail.vue     ← 患者360视图
│   │   │   ├── PatientForm.vue       ← 患者录入
│   │   │   ├── EncounterDetail.vue   ← 就诊详情
│   │   │   ├── DiagnosisView.vue     ← 诊断信息
│   │   │   ├── LabResultView.vue     ← 检验结果
│   │   │   ├── MedicationView.vue    ← 用药记录
│   │   │   ├── ImagingView.vue       ← 影像检查
│   │   │   ├── VitalSignView.vue     ← 生命体征
│   │   │   ├── ClinicalNoteView.vue  ← 临床文书
│   │   │   ├── DataSourceList.vue    ← 数据源管理
│   │   │   ├── SyncTaskList.vue      ← 同步任务
│   │   │   ├── QualityRuleList.vue   ← 数据质量规则
│   │   │   └── QualityResultList.vue ← 质量检测结果
│   │   │
│   │   ├── data-rdr/                 ← RDR 科研管理（7 页）
│   │   │   ├── ProjectList.vue       ← 研究项目列表
│   │   │   ├── ProjectDetail.vue     ← 项目详情
│   │   │   ├── CohortList.vue        ← 队列管理
│   │   │   ├── DatasetList.vue       ← 数据集列表
│   │   │   ├── DatasetDetail.vue     ← 数据集详情
│   │   │   ├── EtlTaskList.vue       ← ETL 任务
│   │   │   └── DictManage.vue        ← 数据字典
│   │   │
│   │   ├── label/                    ← 标注管理（4 页）
│   │   │   ├── LabelTaskList.vue     ← 标注任务列表
│   │   │   ├── LabelTaskDetail.vue   ← 任务详情
│   │   │   ├── LabelWorkspace.vue    ← 标注工作台
│   │   │   └── LabelStats.vue        ← 标注统计
│   │   │
│   │   ├── schedule/                 ← 任务调度（2 页）
│   │   │   ├── TaskList.vue          ← 定时任务列表
│   │   │   └── TaskDetail.vue        ← 任务详情+执行记录
│   │   │
│   │   ├── audit/                    ← 审计日志（4 页）
│   │   │   ├── OperationLog.vue      ← 操作审计
│   │   │   ├── DataAccessLog.vue     ← 数据访问审计
│   │   │   ├── SystemEvent.vue       ← 系统事件
│   │   │   └── ComplianceReport.vue  ← 合规报表
│   │   │
│   │   ├── alert/                    ← 告警中心（2 页）
│   │   │   ├── AlertList.vue         ← 活跃告警
│   │   │   └── AlertRuleList.vue     ← 告警规则配置
│   │   │
│   │   ├── message/                  ← 消息中心（4 页）
│   │   │   ├── MessageList.vue       ← 消息列表
│   │   │   ├── NotificationSettings.vue ← 通知偏好
│   │   │   ├── TemplateList.vue      ← 消息模板
│   │   │   └── SendMessage.vue       ← 发送消息
│   │   │
│   │   └── system/                   ← 系统设置（8 页）
│   │       ├── UserList.vue          ← 用户管理
│   │       ├── UserForm.vue          ← 用户编辑
│   │       ├── RoleList.vue          ← 角色管理
│   │       ├── RolePermission.vue    ← 角色权限配置
│   │       ├── PermissionTree.vue    ← 权限树
│   │       ├── OrgList.vue           ← 机构管理
│   │       ├── DesensitizeRule.vue   ← 脱敏规则
│   │       └── SystemConfig.vue      ← 系统参数
│   │
│   ├── stores/                       ← Pinia 状态管理
│   │   ├── auth.ts                   ← 认证/用户信息
│   │   ├── permission.ts             ← 权限/菜单
│   │   ├── model.ts                  ← 模型管理状态
│   │   ├── data.ts                   ← 数据管理状态
│   │   └── ui.ts                     ← UI 全局状态
│   │
│   ├── router/                       ← 路由配置
│   │   ├── index.ts                  ← 路由入口
│   │   ├── guards.ts                 ← 路由守卫
│   │   ├── constantRoutes.ts         ← 静态路由（登录/404等）
│   │   └── asyncRoutes.ts            ← 动态路由（按权限加载）
│   │
│   ├── hooks/                        ← 通用 Hooks
│   │   ├── useTable.ts               ← 表格查询 Hook
│   │   ├── useForm.ts                ← 表单 Hook
│   │   ├── useModal.ts               ← 弹窗 Hook
│   │   ├── usePermission.ts          ← 权限检查 Hook
│   │   └── useWebSocket.ts           ← WebSocket Hook
│   │
│   ├── utils/                        ← 工具函数
│   │   ├── request.ts                ← Axios 实例
│   │   ├── auth.ts                   ← Token 存取
│   │   ├── date.ts                   ← 日期格式化
│   │   ├── dict.ts                   ← 字典缓存
│   │   └── validate.ts               ← 表单校验规则
│   │
│   ├── types/                        ← TypeScript 类型定义
│   │   ├── api.d.ts                  ← 统一响应体类型
│   │   ├── model.d.ts                ← 模型管理类型
│   │   ├── data.d.ts                 ← 数据管理类型
│   │   ├── auth.d.ts                 ← 认证类型
│   │   └── global.d.ts               ← 全局类型
│   │
│   ├── App.vue                       ← 根组件
│   └── main.ts                       ← 入口文件
│
├── index.html
├── vite.config.ts                    ← Vite 配置
├── tsconfig.json
├── .env.development                  ← 开发环境变量
├── .env.staging                      ← 预发环境变量
└── .env.production                   ← 生产环境变量
```

### 1.3 Vite 构建配置

```typescript
// vite.config.ts
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src'),
      },
    },
    server: {
      port: 3000,
      proxy: {
        // 开发环境代理到 Gateway
        '/api': {
          target: env.VITE_API_BASE_URL || 'http://localhost:8080',
          changeOrigin: true,
        },
        // WebSocket 代理
        '/ws': {
          target: env.VITE_WS_URL || 'ws://localhost:8087',
          ws: true,
        },
      },
    },
    build: {
      rollupOptions: {
        output: {
          // 按路由拆分代码
          manualChunks: {
            'vendor-vue': ['vue', 'vue-router', 'pinia'],
            'vendor-antd': ['ant-design-vue', '@ant-design/icons-vue'],
            'vendor-echarts': ['echarts'],
          },
        },
      },
    },
  }
})
```

### 1.4 环境变量

```bash
# .env.development
VITE_APP_TITLE=MAIDC 开发环境
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_URL=ws://localhost:8087
VITE_MINIO_ENDPOINT=http://localhost:9000

# .env.production
VITE_APP_TITLE=MAIDC 医疗AI数据中心
VITE_API_BASE_URL=
VITE_WS_URL=
VITE_MINIO_ENDPOINT=
```

---

## 2. 路由设计

### 2.1 路由总体结构

```typescript
// router/index.ts
import { createRouter, createWebHistory } from 'vue-router'
import { constantRoutes } from './constantRoutes'
import { setupGuards } from './guards'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: constantRoutes,
  scrollBehavior: () => ({ top: 0 }),
})

setupGuards(router)
export default router
```

### 2.2 静态路由（无需权限）

```typescript
// router/constantRoutes.ts
import BasicLayout from '@/layouts/BasicLayout.vue'
import BlankLayout from '@/layouts/BlankLayout.vue'

export const constantRoutes = [
  {
    path: '/login',
    component: BlankLayout,
    children: [
      { path: '', name: 'Login', component: () => import('@/views/login/LoginPage.vue') },
    ],
  },
  {
    path: '/redirect',
    component: BasicLayout,
    children: [
      { path: '/redirect/:path(.*)', component: () => import('@/views/redirect/index.vue') },
    ],
  },
  { path: '/403', component: () => import('@/views/error/403.vue') },
  { path: '/404', component: () => import('@/views/error/404.vue') },
  { path: '/500', component: () => import('@/views/error/500.vue') },
]
```

### 2.3 动态路由（按权限加载）

```typescript
// router/asyncRoutes.ts
import type { RouteRecordRaw } from 'vue-router'
import BasicLayout from '@/layouts/BasicLayout.vue'

// 8 大模块路由定义
export const asyncRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    component: BasicLayout,
    redirect: '/dashboard',
    children: [
      // ========== 仪表盘 ==========
      {
        path: 'dashboard',
        name: 'Dashboard',
        meta: { title: '仪表盘', icon: 'DashboardOutlined', sort: 1 },
        redirect: '/dashboard/overview',
        children: [
          { path: 'overview', name: 'DashboardOverview', meta: { title: '系统总览' },
            component: () => import('@/views/dashboard/Overview.vue') },
          { path: 'model', name: 'ModelDashboard', meta: { title: '模型看板' },
            component: () => import('@/views/dashboard/ModelDashboard.vue') },
          { path: 'data', name: 'DataDashboard', meta: { title: '数据看板' },
            component: () => import('@/views/dashboard/DataDashboard.vue') },
        ],
      },

      // ========== 模型管理 ==========
      {
        path: 'model',
        name: 'Model',
        meta: { title: '模型管理', icon: 'ExperimentOutlined', sort: 2, permission: 'model' },
        redirect: '/model/list',
        children: [
          { path: 'list', name: 'ModelList', meta: { title: '模型列表', permission: 'model:list' },
            component: () => import('@/views/model/ModelList.vue') },
          { path: ':id', name: 'ModelDetail', meta: { title: '模型详情', hidden: true },
            component: () => import('@/views/model/ModelDetail.vue') },
          { path: 'evaluations', name: 'EvalList', meta: { title: '评估管理', permission: 'model:eval' },
            component: () => import('@/views/model/EvalList.vue') },
          { path: 'evaluations/:id', name: 'EvalDetail', meta: { title: '评估详情', hidden: true },
            component: () => import('@/views/model/EvalDetail.vue') },
          { path: 'approvals', name: 'ApprovalList', meta: { title: '审批管理', permission: 'model:approval' },
            component: () => import('@/views/model/ApprovalList.vue') },
          { path: 'deployments', name: 'DeploymentList', meta: { title: '部署管理', permission: 'model:deploy' },
            component: () => import('@/views/model/DeploymentList.vue') },
          { path: 'deployments/:id', name: 'DeploymentDetail', meta: { title: '部署详情', hidden: true },
            component: () => import('@/views/model/DeploymentDetail.vue') },
          { path: 'routes', name: 'RouteConfig', meta: { title: '流量路由', permission: 'model:route' },
            component: () => import('@/views/model/RouteConfig.vue') },
        ],
      },

      // ========== 数据管理 ==========
      {
        path: 'data',
        name: 'Data',
        meta: { title: '数据管理', icon: 'DatabaseOutlined', sort: 3, permission: 'data' },
        redirect: '/data/cdr/patients',
        children: [
          { path: 'cdr/patients', name: 'PatientList', meta: { title: '患者管理' },
            component: () => import('@/views/data-cdr/PatientList.vue') },
          { path: 'cdr/patients/:id', name: 'PatientDetail', meta: { title: '患者详情', hidden: true },
            component: () => import('@/views/data-cdr/PatientDetail.vue') },
          { path: 'cdr/sync', name: 'SyncTaskList', meta: { title: '数据同步' },
            component: () => import('@/views/data-cdr/SyncTaskList.vue') },
          { path: 'cdr/quality', name: 'QualityRuleList', meta: { title: '数据质量' },
            component: () => import('@/views/data-cdr/QualityRuleList.vue') },
          { path: 'rdr/projects', name: 'ProjectList', meta: { title: '研究项目' },
            component: () => import('@/views/data-rdr/ProjectList.vue') },
          { path: 'rdr/datasets', name: 'DatasetList', meta: { title: '数据集' },
            component: () => import('@/views/data-rdr/DatasetList.vue') },
          { path: 'rdr/etl', name: 'EtlTaskList', meta: { title: 'ETL任务' },
            component: () => import('@/views/data-rdr/EtlTaskList.vue') },
        ],
      },

      // ========== 标注管理 ==========
      {
        path: 'label',
        name: 'Label',
        meta: { title: '标注管理', icon: 'EditOutlined', sort: 4, permission: 'label' },
        redirect: '/label/tasks',
        children: [
          { path: 'tasks', name: 'LabelTaskList', meta: { title: '标注任务' },
            component: () => import('@/views/label/LabelTaskList.vue') },
          { path: 'tasks/:id', name: 'LabelTaskDetail', meta: { title: '任务详情', hidden: true },
            component: () => import('@/views/label/LabelTaskDetail.vue') },
          { path: 'workspace/:id', name: 'LabelWorkspace', meta: { title: '标注工作台', hidden: true },
            component: () => import('@/views/label/LabelWorkspace.vue') },
        ],
      },

      // ========== 任务调度 ==========
      {
        path: 'schedule',
        name: 'Schedule',
        meta: { title: '任务调度', icon: 'ScheduleOutlined', sort: 5, permission: 'task' },
        redirect: '/schedule/tasks',
        children: [
          { path: 'tasks', name: 'ScheduleTaskList', meta: { title: '定时任务' },
            component: () => import('@/views/schedule/TaskList.vue') },
        ],
      },

      // ========== 告警中心 ==========
      {
        path: 'alert',
        name: 'Alert',
        meta: { title: '告警中心', icon: 'AlertOutlined', sort: 6, permission: 'alert' },
        redirect: '/alert/active',
        children: [
          { path: 'active', name: 'AlertList', meta: { title: '活跃告警' },
            component: () => import('@/views/alert/AlertList.vue') },
          { path: 'rules', name: 'AlertRuleList', meta: { title: '告警规则' },
            component: () => import('@/views/alert/AlertRuleList.vue') },
        ],
      },

      // ========== 审计日志 ==========
      {
        path: 'audit',
        name: 'Audit',
        meta: { title: '审计日志', icon: 'FileSearchOutlined', sort: 7, permission: 'audit' },
        redirect: '/audit/operations',
        children: [
          { path: 'operations', name: 'OperationLog', meta: { title: '操作审计' },
            component: () => import('@/views/audit/OperationLog.vue') },
          { path: 'data-access', name: 'DataAccessLog', meta: { title: '数据访问' },
            component: () => import('@/views/audit/DataAccessLog.vue') },
          { path: 'compliance', name: 'ComplianceReport', meta: { title: '合规报表' },
            component: () => import('@/views/audit/ComplianceReport.vue') },
        ],
      },

      // ========== 消息中心 ==========
      {
        path: 'message',
        name: 'Message',
        meta: { title: '消息中心', icon: 'BellOutlined', sort: 8 },
        redirect: '/message/list',
        children: [
          { path: 'list', name: 'MessageList', meta: { title: '我的消息' },
            component: () => import('@/views/message/MessageList.vue') },
          { path: 'settings', name: 'NotificationSettings', meta: { title: '通知设置' },
            component: () => import('@/views/message/NotificationSettings.vue') },
        ],
      },

      // ========== 系统设置 ==========
      {
        path: 'system',
        name: 'System',
        meta: { title: '系统设置', icon: 'SettingOutlined', sort: 9, permission: 'system' },
        redirect: '/system/users',
        children: [
          { path: 'users', name: 'UserList', meta: { title: '用户管理' },
            component: () => import('@/views/system/UserList.vue') },
          { path: 'roles', name: 'RoleList', meta: { title: '角色管理' },
            component: () => import('@/views/system/RoleList.vue') },
          { path: 'permissions', name: 'PermissionTree', meta: { title: '权限管理' },
            component: () => import('@/views/system/PermissionTree.vue') },
          { path: 'orgs', name: 'OrgList', meta: { title: '机构管理' },
            component: () => import('@/views/system/OrgList.vue') },
          { path: 'dict', name: 'DictManage', meta: { title: '数据字典' },
            component: () => import('@/views/data-rdr/DictManage.vue') },
          { path: 'config', name: 'SystemConfig', meta: { title: '系统参数' },
            component: () => import('@/views/system/SystemConfig.vue') },
        ],
      },
    ],
  },
]
```

### 2.4 路由守卫

```typescript
// router/guards.ts
import type { Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { usePermissionStore } from '@/stores/permission'
import { getToken } from '@/utils/auth'

const WHITE_LIST = ['/login', '/403', '/404', '/500']

export function setupGuards(router: Router) {
  router.beforeEach(async (to, from, next) => {
    // 启用进度条
    window.NProgress?.start()

    const token = getToken()

    if (token) {
      if (to.path === '/login') {
        next({ path: '/' })
      } else {
        const authStore = useAuthStore()
        if (authStore.userInfo) {
          // 已加载用户信息，检查页面权限
          const permissionStore = usePermissionStore()
          if (to.meta.permission && !permissionStore.hasPermission(to.meta.permission as string)) {
            next({ path: '/403' })
          } else {
            next()
          }
        } else {
          // 刷新页面，重新加载用户信息和动态路由
          try {
            await authStore.getUserInfo()
            const permissionStore = usePermissionStore()
            const routes = await permissionStore.generateRoutes()
            routes.forEach(route => router.addRoute(route))
            next({ ...to, replace: true })
          } catch {
            // Token 过期，跳转登录
            authStore.logout()
            next(`/login?redirect=${to.path}`)
          }
        }
      }
    } else {
      // 无 Token
      if (WHITE_LIST.includes(to.path)) {
        next()
      } else {
        next(`/login?redirect=${to.path}`)
      }
    }
  })

  router.afterEach((to) => {
    // 设置页面标题
    document.title = `${to.meta.title || ''} - MAIDC`
    window.NProgress?.done()
  })
}
```

### 2.5 Tab 缓存策略

```vue
<!-- layouts/PageShell.vue -->
<template>
  <router-view v-slot="{ Component, route }">
    <keep-alive :include="cachedViews">
      <component :is="Component" :key="route.path" />
    </keep-alive>
  </router-view>
</template>

<script setup lang="ts">
import { useUiStore } from '@/stores/ui'

const uiStore = useUiStore()
const cachedViews = computed(() => uiStore.cachedViews)
</script>
```

**缓存策略**:
- 列表页默认缓存（保留搜索条件和滚动位置）
- 详情页不缓存（数据实时性要求高）
- 通过 `defineOptions({ name: 'ModelList' })` 匹配缓存 key

---

## 3. 组件规范

### 3.1 公共组件一览

#### 核心展示组件

| 组件 | 路径 | 用途 | 使用频次 |
|------|------|------|----------|
| `StatusBadge` | `components/StatusBadge/` | 统一状态展示 | 高（所有列表页） |
| `MetricCard` | `components/MetricCard/` | 监控指标卡片 | 高（仪表盘/部署详情） |
| `JsonViewer` | `components/JsonViewer/` | JSON 树形展示 | 中（Schema/参数查看） |
| `SchemaViewer` | `components/SchemaViewer/` | 输入输出 Schema | 中（模型详情） |
| `KeyValueEditor` | `components/KeyValueEditor/` | 键值对编辑 | 中（环境变量/配置） |
| `FileUploader` | `components/FileUploader/` | 文件上传 | 中（版本上传/材料上传） |

#### 业务功能组件

| 组件 | 路径 | 用途 |
|------|------|------|
| `SearchForm` | `components/SearchForm/` | 通用搜索表单 |
| `PageContainer` | `components/PageContainer/` | 页面容器（面包屑+标题） |
| `ModelSelect` | `components/ModelSelect/` | 模型选择下拉 |
| `DatasetSelect` | `components/DatasetSelect/` | 数据集选择下拉 |
| `UserSelect` | `components/UserSelect/` | 用户选择下拉 |
| `VersionTag` | `components/VersionTag/` | 版本号标签 |
| `ConfusionMatrix` | `components/ConfusionMatrix/` | 混淆矩阵展示 |
| `MetricChart` | `components/MetricChart/` | 指标趋势图（ECharts） |
| `RocCurve` | `components/RocCurve/` | ROC 曲线图 |
| `ApprovalTimeline` | `components/ApprovalTimeline/` | 审批时间线 |
| `DeploymentStatus` | `components/DeploymentStatus/` | 部署状态面板 |
| `TrafficRuleEditor` | `components/TrafficRuleEditor/` | 流量规则编辑器 |
| `ResourceConfigForm` | `components/ResourceConfigForm/` | 资源配置表单（CPU/GPU/内存） |
| `PatientInfoCard` | `components/PatientInfoCard/` | 患者信息卡片（脱敏展示） |
| `DesensitizePreview` | `components/DesensitizePreview/` | 脱敏效果预览 |
| `AnnotationCanvas` | `components/AnnotationCanvas/` | 标注画布（影像标注） |
| `AuditDetailDrawer` | `components/AuditDetailDrawer/` | 审计详情抽屉 |

#### 通用工具组件

| 组件 | 路径 | 用途 |
|------|------|------|
| `ImagePreview` | `components/ImagePreview/` | DICOM 影像预览 |
| `PdfViewer` | `components/PdfViewer/` | PDF 报告查看 |
| `CodeEditor` | `components/CodeEditor/` | 代码/配置编辑器 |
| `DiffViewer` | `components/DiffViewer/` | 版本差异对比 |
| `CountDown` | `components/CountDown/` | 倒计时 |
| `EmptyState` | `components/EmptyState/` | 空状态占位 |
| `ErrorBoundary` | `components/ErrorBoundary/` | 错误边界 |
| `PermissionWrapper` | `components/PermissionWrapper/` | 权限包裹器 |

### 3.2 StatusBadge 组件映射表

StatusBadge 是系统中使用最广泛的组件，统一所有状态字段的颜色和图标：

| 状态字段 | 状态值 | 颜色 | 图标 |
|----------|--------|------|------|
| **ModelStatus** | `DRAFT` | `default` | `EditOutlined` |
| | `REGISTERED` | `processing` | `CheckCircleOutlined` |
| | `PUBLISHED` | `success` | `RocketOutlined` |
| | `DEPRECATED` | `warning` | `StopOutlined` |
| **VersionStatus** | `CREATED` | `default` | `PlusCircleOutlined` |
| | `TRAINING` | `processing` | `ThunderboltOutlined` |
| | `EVALUATING` | `processing` | `ExperimentOutlined` |
| | `APPROVED` | `success` | `SafetyCertificateOutlined` |
| | `DEPLOYED` | `success` | `CloudServerOutlined` |
| | `DEPRECATED` | `warning` | `StopOutlined` |
| **DeployStatus** | `CREATING` | `processing` | `LoadingOutlined` |
| | `RUNNING` | `success` | `PlayCircleOutlined` |
| | `STOPPING` | `processing` | `LoadingOutlined` |
| | `STOPPED` | `default` | `PauseCircleOutlined` |
| | `FAILED` | `error` | `CloseCircleOutlined` |
| **EvalStatus** | `PENDING` | `default` | `ClockCircleOutlined` |
| | `RUNNING` | `processing` | `LoadingOutlined` |
| | `COMPLETED` | `success` | `CheckCircleOutlined` |
| | `FAILED` | `error` | `CloseCircleOutlined` |
| **ApprovalStatus** | `PENDING` | `processing` | `ClockCircleOutlined` |
| | `APPROVED` | `success` | `SafetyCertificateOutlined` |
| | `REJECTED` | `error` | `CloseCircleOutlined` |
| **AlertStatus** | `FIRING` | `error` | `AlertOutlined` |
| | `ACKNOWLEDGED` | `processing` | `EyeOutlined` |
| | `RESOLVED` | `success` | `CheckCircleOutlined` |

**组件实现**:

```vue
<!-- components/StatusBadge/index.vue -->
<template>
  <a-badge :status="statusConfig.status" :text="statusConfig.text">
    <template #count>
      <component :is="statusConfig.icon" v-if="statusConfig.icon" :style="{ color: statusConfig.color }" />
    </template>
  </a-badge>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { STATUS_MAP } from './statusMap'

const props = defineProps<{
  type: 'model' | 'version' | 'deploy' | 'eval' | 'approval' | 'alert'
  value: string
}>()

const statusConfig = computed(() => {
  const typeMap = STATUS_MAP[props.type]
  return typeMap[props.value] ?? { status: 'default', text: props.value }
})
</script>
```

**使用方式**:
```vue
<StatusBadge type="model" :value="record.status" />
<StatusBadge type="deploy" value="RUNNING" />
```

### 3.3 组件命名与注册规范

**命名规则**:

| 类型 | 规范 | 示例 |
|------|------|------|
| 页面组件 | PascalCase，功能名+类型后缀 | `ModelList.vue`, `ModelDetail.vue` |
| 公共组件 | PascalCase | `StatusBadge/`, `MetricCard/` |
| 组件目录 | PascalCase，含 `index.vue` | `components/StatusBadge/index.vue` |

**全局注册**:

```typescript
// main.ts 中注册高频公共组件
import { App } from 'vue'
import StatusBadge from '@/components/StatusBadge/index.vue'
import PageContainer from '@/components/PageContainer/index.vue'
import PermissionWrapper from '@/components/PermissionWrapper/index.vue'

export function registerGlobalComponents(app: App) {
  app.component('StatusBadge', StatusBadge)
  app.component('PageContainer', PageContainer)
  app.component('PermissionWrapper', PermissionWrapper)
}
```

**局部引入**: 低频组件按需在页面中 `import`。

### 3.4 组件 Props/Events 类型定义规范

```typescript
// components/MetricCard/types.ts
export interface MetricCardProps {
  title: string
  value: number | string
  prefix?: string        // 前缀图标
  suffix?: string        // 后缀单位 (如 'ms', '%')
  precision?: number     // 小数位数
  trend?: 'up' | 'down' | 'flat'  // 趋势方向
  trendValue?: number    // 趋势变化值
  loading?: boolean
  status?: 'success' | 'warning' | 'error' | 'default'
}

export interface MetricCardEmits {
  (e: 'click'): void
}
```

```vue
<!-- components/MetricCard/index.vue -->
<script setup lang="ts">
import type { MetricCardProps, MetricCardEmits } from './types'

const props = withDefaults(defineProps<MetricCardProps>(), {
  precision: 2,
  loading: false,
  status: 'default',
})

const emit = defineEmits<MetricCardEmits>()
</script>
```

---

## 4. 状态管理

### 4.1 Store 拆分策略

| Store | 文件 | 职责 |
|-------|------|------|
| `useAuthStore` | `stores/auth.ts` | Token 管理、用户信息、登录登出 |
| `usePermissionStore` | `stores/permission.ts` | 权限列表、菜单路由、权限判断 |
| `useModelStore` | `stores/model.ts` | 模型列表缓存、当前模型、版本数据 |
| `useDataStore` | `stores/data.ts` | 字典缓存、患者缓存 |
| `useUiStore` | `stores/ui.ts` | 侧边栏折叠、Tab缓存列表、主题 |

### 4.2 AuthStore

```typescript
// stores/auth.ts
import { defineStore } from 'pinia'
import { login, logout, getUserInfo, refreshToken } from '@/api/auth'
import { getToken, setToken, removeToken } from '@/utils/auth'

interface UserInfo {
  id: number
  username: string
  realName: string
  roles: string[]
  orgId: number
  permissions: string[]
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(getToken())
  const userInfo = ref<UserInfo | null>(null)

  async function loginAction(username: string, password: string) {
    const res = await login({ username, password })
    token.value = res.data.access_token
    setToken(res.data.access_token, res.data.refresh_token, res.data.expires_in)
  }

  async function getUserInfoAction() {
    const res = await getUserInfo()
    userInfo.value = res.data
    return res.data
  }

  async function logoutAction() {
    try { await logout() } finally {
      token.value = ''
      userInfo.value = null
      removeToken()
    }
  }

  return {
    token, userInfo,
    loginAction, getUserInfoAction, logoutAction,
  }
})
```

### 4.3 PermissionStore

```typescript
// stores/permission.ts
import { defineStore } from 'pinia'
import { asyncRoutes } from '@/router/asyncRoutes'
import type { RouteRecordRaw } from 'vue-router'

export const usePermissionStore = defineStore('permission', () => {
  const routes = ref<RouteRecordRaw[]>([])
  const permissions = ref<string[]>([])

  // 根据用户权限过滤路由
  function filterRoutes(routes: RouteRecordRaw[], perms: string[]): RouteRecordRaw[] {
    return routes.filter(route => {
      if (route.meta?.permission) {
        if (!perms.includes(route.meta.permission as string)) return false
      }
      if (route.children) {
        route.children = filterRoutes(route.children, perms)
      }
      return true
    })
  }

  async function generateRoutes(): Promise<RouteRecordRaw[]> {
    const authStore = useAuthStore()
    permissions.value = authStore.userInfo?.permissions ?? []
    const filtered = filterRoutes(asyncRoutes, permissions.value)
    routes.value = filtered
    return filtered
  }

  function hasPermission(code: string): boolean {
    // admin 拥有所有权限
    const authStore = useAuthStore()
    if (authStore.userInfo?.roles?.includes('admin')) return true
    return permissions.value.includes(code)
  }

  return { routes, permissions, generateRoutes, hasPermission }
})
```

### 4.4 UiStore

```typescript
// stores/ui.ts
import { defineStore } from 'pinia'

export const useUiStore = defineStore('ui', () => {
  const sidebarCollapsed = ref(false)
  const cachedViews = ref<string[]>([])
  const theme = ref<'light' | 'dark'>('light')

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function addCachedView(name: string) {
    if (!cachedViews.value.includes(name)) {
      cachedViews.value.push(name)
    }
  }

  function removeCachedView(name: string) {
    cachedViews.value = cachedViews.value.filter(v => v !== name)
  }

  return { sidebarCollapsed, cachedViews, theme, toggleSidebar, addCachedView, removeCachedView }
})
```

### 4.5 数据缓存策略

| 场景 | 策略 | Store 方法 |
|------|------|------------|
| 模型列表 | 请求 + Store 缓存（5min） | `useModelStore().fetchModels()` |
| 数据字典 | 首次加载缓存到 Store（不设 TTL，手动刷新） | `useDataStore().loadDict(type)` |
| 用户权限 | 登录加载，权限变更时强制刷新 | `usePermissionStore().generateRoutes()` |
| 表格查询数据 | 不缓存，每次请求最新 | 直接调用 API |
| 部署状态 | 轮询更新（30s） | 组件内 `setInterval` + API |

---

## 5. API 层封装

### 5.1 Axios 实例配置

```typescript
// utils/request.ts
import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { getToken, getRefreshToken, setToken, removeToken } from '@/utils/auth'
import { message } from 'ant-design-vue'

export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  trace_id: string
}

export interface PageResult<T = any> {
  items: T[]
  total: number
  page: number
  page_size: number
  total_pages: number
}

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
})

// ========== 请求拦截器 ==========
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // 注入 TraceId
    if (!config.headers['X-Trace-Id']) {
      config.headers['X-Trace-Id'] = crypto.randomUUID()
    }
    return config
  },
  (error) => Promise.reject(error),
)

// ========== 响应拦截器 ==========
let isRefreshing = false
let refreshSubscribers: Array<(token: string) => void> = []

service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data
    // 业务错误码处理
    if (res.code !== 200 && res.code !== 201) {
      message.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return response
  },
  async (error) => {
    const { response, config } = error
    if (!response) {
      message.error('网络异常，请检查网络连接')
      return Promise.reject(error)
    }

    switch (response.status) {
      case 401: {
        // Token 过期，尝试刷新
        if (!isRefreshing) {
          isRefreshing = true
          try {
            const refreshTokenValue = getRefreshToken()
            const res = await axios.post('/api/v1/auth/refresh', {
              refresh_token: refreshTokenValue,
            })
            const newToken = res.data.data.access_token
            setToken(newToken, refreshTokenValue, res.data.data.expires_in)
            config.headers.Authorization = `Bearer ${newToken}`
            // 重试所有排队的请求
            refreshSubscribers.forEach(cb => cb(newToken))
            refreshSubscribers = []
            return service(config)
          } catch {
            removeToken()
            window.location.href = '/login'
            return Promise.reject(error)
          } finally {
            isRefreshing = false
          }
        } else {
          // 正在刷新，排队等待
          return new Promise((resolve) => {
            refreshSubscribers.push((token: string) => {
              config.headers.Authorization = `Bearer ${token}`
              resolve(service(config))
            })
          })
        }
      }
      case 403:
        message.error('无权限访问')
        break
      case 404:
        message.error('请求的资源不存在')
        break
      case 429:
        message.warning('请求过于频繁，请稍后再试')
        break
      case 500:
        message.error('服务器内部错误')
        break
      default:
        message.error(response.data?.message || '请求失败')
    }
    return Promise.reject(error)
  },
)

export default service
```

### 5.2 API 模块化拆分

```typescript
// api/model.ts
import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/utils/request'

// ========== 类型定义 ==========

export interface ModelListParams {
  page?: number
  page_size?: number
  model_type?: string
  status?: string
  keyword?: string
}

export interface ModelCreateParams {
  model_code: string
  model_name: string
  description?: string
  model_type: string
  task_type: string
  framework: string
  input_schema: Record<string, any>
  output_schema: Record<string, any>
  tags?: string
  license?: string
  project_id?: number
}

export interface ModelVO {
  id: number
  model_code: string
  model_name: string
  model_type: string
  task_type: string
  framework: string
  status: string
  latest_version: string
  tags: string[]
  owner_name: string
  created_at: string
  updated_at: string
}

export interface ModelDetailVO extends ModelVO {
  description: string
  input_schema: Record<string, any>
  output_schema: Record<string, any>
  owner: { id: number; name: string }
  project: { id: number; name: string }
  version_count: number
  latest_version: { version_no: string; status: string }
  active_deployment: { id: number; status: string; endpoint_url: string }
  statistics: { total_inferences: number; avg_latency_ms: number; last_24h_inferences: number }
}

// ========== API 方法 ==========

export function getModels(params: ModelListParams) {
  return request.get<ApiResponse<PageResult<ModelVO>>>('/models', { params })
}

export function getModel(id: number) {
  return request.get<ApiResponse<ModelDetailVO>>(`/models/${id}`)
}

export function createModel(data: ModelCreateParams) {
  return request.post<ApiResponse<ModelVO>>('/models', data)
}

export function updateModel(id: number, data: Partial<ModelCreateParams>) {
  return request.put<ApiResponse<ModelVO>>(`/models/${id}`, data)
}

export function deleteModel(id: number) {
  return request.delete<ApiResponse<void>>(`/models/${id}`)
}

// ========== 版本管理 ==========

export interface VersionCreateParams {
  version_no: string
  description?: string
  changelog?: string
  model_file: File
  config_file?: File
  hyper_params?: string
}

export function createVersion(modelId: number, data: FormData) {
  return request.post<ApiResponse<any>>(`/models/${modelId}/versions`, data, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 300000, // 文件上传超时 5 分钟
  })
}

export function getVersions(modelId: number, params?: { page?: number; page_size?: number }) {
  return request.get<ApiResponse<PageResult<any>>>(`/models/${modelId}/versions`, { params })
}

export function compareVersions(modelId: number, v1: number, v2: number) {
  return request.get<ApiResponse<any>>(`/models/${modelId}/versions/compare`, {
    params: { v1, v2 },
  })
}

// ========== 评估 ==========

export function createEvaluation(data: {
  model_version_id: number
  eval_name: string
  eval_type: string
  dataset_id: number
  metrics_config: Record<string, any>
}) {
  return request.post<ApiResponse<{ id: number; status: string }>>('/evaluations', data)
}

export function getEvaluation(id: number) {
  return request.get<ApiResponse<any>>(`/evaluations/${id}`)
}

export function getEvaluationReport(id: number) {
  return request.get(`/evaluations/${id}/report`, { responseType: 'blob' })
}

// ========== 部署 ==========

export function createDeployment(data: Record<string, any>) {
  return request.post<ApiResponse<{ id: number; status: string }>>('/deployments', data)
}

export function getDeploymentStatus(id: number) {
  return request.get<ApiResponse<any>>(`/deployments/${id}/status`)
}

export function startDeployment(id: number) {
  return request.put<ApiResponse<any>>(`/deployments/${id}/start`)
}

export function stopDeployment(id: number) {
  return request.put<ApiResponse<any>>(`/deployments/${id}/stop`)
}

export function scaleDeployment(id: number, data: { target_replicas: number; resource_config?: Record<string, any> }) {
  return request.put<ApiResponse<any>>(`/deployments/${id}/scale`, data)
}
```

### 5.3 API 类型定义文件

```typescript
// types/api.d.ts
declare namespace API {
  // 统一响应体
  interface Response<T = any> {
    code: number
    message: string
    data: T
    trace_id: string
  }

  // 分页结果
  interface PageResult<T = any> {
    items: T[]
    total: number
    page: number
    page_size: number
    total_pages: number
  }

  // 分页请求参数
  interface PageParams {
    page?: number
    page_size?: number
  }

  // 排序参数
  interface SortParams {
    sort_by?: string
    sort_order?: 'asc' | 'desc'
  }
}
```

---

## 6. 页面开发模板

### 6.1 列表页模板

列表页是系统中最常见的页面模式（搜索 + 表格 + 分页 + 操作弹窗）：

```vue
<!-- views/model/ModelList.vue -->
<template>
  <PageContainer title="模型管理">
    <template #extra>
      <a-button v-permission="'model:create'" type="primary" @click="handleCreate">
        <template #icon><PlusOutlined /></template>
        注册模型
      </a-button>
    </template>

    <!-- 搜索区域 -->
    <SearchForm :model="searchForm" @search="handleSearch" @reset="handleReset">
      <a-form-item label="关键词" name="keyword">
        <a-input v-model:value="searchForm.keyword" placeholder="模型编码/名称" allowClear />
      </a-form-item>
      <a-form-item label="模型类型" name="model_type">
        <a-select v-model:value="searchForm.model_type" placeholder="全部" allowClear style="width: 140px">
          <a-select-option v-for="item in modelTypeOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="状态" name="status">
        <a-select v-model:value="searchForm.status" placeholder="全部" allowClear style="width: 120px">
          <a-select-option v-for="item in modelStatusOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
    </SearchForm>

    <!-- 表格 -->
    <a-table
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      @change="handleTableChange"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <StatusBadge type="model" :value="record.status" />
        </template>
        <template v-if="column.key === 'tags'">
          <a-tag v-for="tag in record.tags" :key="tag">{{ tag }}</a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-button type="link" size="small" @click="handleDetail(record)">详情</a-button>
          <a-button v-permission="'model:update'" type="link" size="small" @click="handleEdit(record)">编辑</a-button>
          <a-popconfirm title="确定删除？" @confirm="handleDelete(record)">
            <a-button v-permission="'model:delete'" type="link" danger size="small">删除</a-button>
          </a-popconfirm>
        </template>
      </template>
    </a-table>

    <!-- 创建/编辑弹窗 -->
    <ModelFormModal
      v-model:open="modalVisible"
      :edit-data="currentRecord"
      @success="fetchData"
    />
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getModels, deleteModel } from '@/api/model'
import { useTable } from '@/hooks/useTable'
import type { ModelVO } from '@/api/model'

defineOptions({ name: 'ModelList' })

const router = useRouter()

// 搜索表单
const searchForm = reactive({
  keyword: undefined as string | undefined,
  model_type: undefined as string | undefined,
  status: undefined as string | undefined,
})

// 表格 Hook
const { tableData, loading, pagination, fetchData, handleTableChange } =
  useTable<ModelVO>((params) =>
    getModels({ page: params.page, page_size: params.pageSize, ...searchForm }),
  )

// 表格列定义
const columns = [
  { title: '模型编码', dataIndex: 'model_code', width: 180 },
  { title: '模型名称', dataIndex: 'model_name', width: 200, ellipsis: true },
  { title: '类型', dataIndex: 'model_type', width: 100 },
  { title: '框架', dataIndex: 'framework', width: 100 },
  { title: '最新版本', dataIndex: 'latest_version', width: 100 },
  { title: '状态', key: 'status', width: 100 },
  { title: '负责人', dataIndex: 'owner_name', width: 100 },
  { title: '更新时间', dataIndex: 'updated_at', width: 170 },
  { title: '操作', key: 'action', width: 180, fixed: 'right' },
]

// 弹窗控制
const modalVisible = ref(false)
const currentRecord = ref<ModelVO | null>(null)

function handleCreate() {
  currentRecord.value = null
  modalVisible.value = true
}

function handleEdit(record: ModelVO) {
  currentRecord.value = record
  modalVisible.value = true
}

function handleDetail(record: ModelVO) {
  router.push(`/model/${record.id}`)
}

async function handleDelete(record: ModelVO) {
  await deleteModel(record.id)
  message.success('删除成功')
  fetchData()
}

function handleSearch() { fetchData({ page: 1 }) }
function handleReset() {
  Object.assign(searchForm, { keyword: undefined, model_type: undefined, status: undefined })
  fetchData({ page: 1 })
}

onMounted(() => fetchData())
</script>
```

### 6.2 useTable Hook

```typescript
// hooks/useTable.ts
import { ref, reactive } from 'vue'
import type { ApiResponse, PageResult } from '@/utils/request'

interface TableParams {
  page: number
  pageSize: number
}

export function useTable<T>(
  fetchFn: (params: TableParams) => Promise<{ data: ApiResponse<PageResult<T>> }>,
) {
  const tableData = ref<T[]>([]) as Ref<T[]>
  const loading = ref(false)
  const pagination = reactive({
    current: 1,
    pageSize: 20,
    total: 0,
    showSizeChanger: true,
    showQuickJumper: true,
    showTotal: (total: number) => `共 ${total} 条`,
  })

  async function fetchData(extra?: Partial<TableParams>) {
    loading.value = true
    try {
      const params: TableParams = {
        page: extra?.page ?? pagination.current,
        pageSize: extra?.pageSize ?? pagination.pageSize,
      }
      const res = await fetchFn(params)
      tableData.value = res.data.data.items
      pagination.total = res.data.data.total
      pagination.current = res.data.data.page
    } finally {
      loading.value = false
    }
  }

  function handleTableChange(pag: any) {
    pagination.current = pag.current
    pagination.pageSize = pag.pageSize
    fetchData({ page: pag.current, pageSize: pag.pageSize })
  }

  return { tableData, loading, pagination, fetchData, handleTableChange }
}
```

### 6.3 详情页模板

```vue
<!-- views/model/ModelDetail.vue -->
<template>
  <PageContainer :title="modelData?.model_name || '模型详情'">
    <template #breadcrumb>
      <a-breadcrumb>
        <a-breadcrumb-item><router-link to="/model/list">模型管理</router-link></a-breadcrumb-item>
        <a-breadcrumb-item>{{ modelData?.model_name }}</a-breadcrumb-item>
      </a-breadcrumb>
    </template>

    <a-spin :spinning="loading">
      <!-- 基础信息卡片 -->
      <a-card title="基础信息" style="margin-bottom: 16px">
        <a-descriptions :column="3" bordered>
          <a-descriptions-item label="模型编码">{{ modelData?.model_code }}</a-descriptions-item>
          <a-descriptions-item label="状态"><StatusBadge type="model" :value="modelData?.status" /></a-descriptions-item>
          <a-descriptions-item label="框架">{{ modelData?.framework }}</a-descriptions-item>
          <a-descriptions-item label="模型类型">{{ modelData?.model_type }}</a-descriptions-item>
          <a-descriptions-item label="任务类型">{{ modelData?.task_type }}</a-descriptions-item>
          <a-descriptions-item label="负责人">{{ modelData?.owner?.name }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ modelData?.created_at }}</a-descriptions-item>
        </a-descriptions>
      </a-card>

      <!-- Tab 区域 -->
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="versions" tab="版本管理">
          <VersionList :model-id="modelId" />
        </a-tab-pane>
        <a-tab-pane key="schema" tab="数据Schema">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-card title="输入 Schema" size="small">
                <JsonViewer :data="modelData?.input_schema" />
              </a-card>
            </a-col>
            <a-col :span="12">
              <a-card title="输出 Schema" size="small">
                <JsonViewer :data="modelData?.output_schema" />
              </a-card>
            </a-col>
          </a-row>
        </a-tab-pane>
        <a-tab-pane key="deployment" tab="部署信息">
          <DeploymentPanel :model-id="modelId" :deployment="modelData?.active_deployment" />
        </a-tab-pane>
        <a-tab-pane key="stats" tab="运行统计">
          <ModelStatsPanel :statistics="modelData?.statistics" />
        </a-tab-pane>
      </a-tabs>
    </a-spin>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getModel } from '@/api/model'
import type { ModelDetailVO } from '@/api/model'

defineOptions({ name: 'ModelDetail' })

const route = useRoute()
const modelId = Number(route.params.id)
const modelData = ref<ModelDetailVO | null>(null)
const loading = ref(false)
const activeTab = ref('versions')

onMounted(async () => {
  loading.value = true
  try {
    const res = await getModel(modelId)
    modelData.value = res.data.data
  } finally {
    loading.value = false
  }
})
</script>
```

### 6.4 表单弹窗模板

```vue
<!-- views/model/ModelFormModal.vue -->
<template>
  <a-modal
    :open="open"
    :title="isEdit ? '编辑模型' : '注册模型'"
    :confirm-loading="submitLoading"
    :width="720"
    @ok="handleSubmit"
    @cancel="handleCancel"
    destroy-on-close
  >
    <a-form
      ref="formRef"
      :model="formState"
      :rules="formRules"
      :label-col="{ span: 5 }"
      :wrapper-col="{ span: 18 }"
    >
      <a-form-item label="模型编码" name="model_code">
        <a-input v-model:value="formState.model_code" placeholder="如: LUNG_NODULE_DET_001" :disabled="isEdit" />
      </a-form-item>

      <a-form-item label="模型名称" name="model_name">
        <a-input v-model:value="formState.model_name" placeholder="请输入模型名称" />
      </a-form-item>

      <a-form-item label="模型类型" name="model_type">
        <a-select v-model:value="formState.model_type" placeholder="请选择">
          <a-select-option value="IMAGING">影像</a-select-option>
          <a-select-option value="NLP">自然语言处理</a-select-option>
          <a-select-option value="GENOMIC">基因组</a-select-option>
          <a-select-option value="STRUCTURED">结构化数据</a-select-option>
          <a-select-option value="MULTIMODAL">多模态</a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item label="框架" name="framework">
        <a-select v-model:value="formState.framework" placeholder="请选择">
          <a-select-option value="PYTORCH">PyTorch</a-select-option>
          <a-select-option value="TENSORFLOW">TensorFlow</a-select-option>
          <a-select-option value="SKLEARN">scikit-learn</a-select-option>
          <a-select-option value="XGBOOST">XGBoost</a-select-option>
          <a-select-option value="ONNX">ONNX</a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item label="描述" name="description">
        <a-textarea v-model:value="formState.description" :rows="3" placeholder="请输入模型描述" />
      </a-form-item>

      <a-form-item label="标签" name="tags">
        <a-select
          v-model:value="formState.tagsArray"
          mode="tags"
          placeholder="输入标签后回车"
          :max-count="5"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import { createModel, updateModel } from '@/api/model'
import type { ModelVO } from '@/api/model'

const props = defineProps<{
  open: boolean
  editData: ModelVO | null
}>()

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'success'): void
}>()

const isEdit = computed(() => !!props.editData)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const formState = reactive({
  model_code: '',
  model_name: '',
  model_type: undefined as string | undefined,
  framework: undefined as string | undefined,
  description: '',
  tagsArray: [] as string[],
})

const formRules: Record<string, Rule[]> = {
  model_code: [{ required: true, message: '请输入模型编码' }],
  model_name: [{ required: true, message: '请输入模型名称' }],
  model_type: [{ required: true, message: '请选择模型类型' }],
  framework: [{ required: true, message: '请选择框架' }],
}

// 编辑模式回填数据
watch(() => props.open, (val) => {
  if (val && props.editData) {
    Object.assign(formState, {
      model_code: props.editData.model_code,
      model_name: props.editData.model_name,
      model_type: props.editData.model_type,
      framework: props.editData.framework,
      description: '',
      tagsArray: props.editData.tags ?? [],
    })
  }
})

async function handleSubmit() {
  await formRef.value?.validateFields()
  submitLoading.value = true
  try {
    const data = {
      ...formState,
      tags: formState.tagsArray.join(','),
    }
    if (isEdit.value) {
      await updateModel(props.editData!.id, data)
      message.success('更新成功')
    } else {
      await createModel(data as any)
      message.success('注册成功')
    }
    emit('success')
    handleCancel()
  } finally {
    submitLoading.value = false
  }
}

function handleCancel() {
  formRef.value?.resetFields()
  emit('update:open', false)
}
</script>
```

### 6.5 开发约定

**命名约定**:

| 类型 | 规范 | 示例 |
|------|------|------|
| 页面文件 | PascalCase + 功能后缀 | `ModelList.vue`, `ModelDetail.vue` |
| 组合式函数 | camelCase，use 前缀 | `useTable.ts`, `usePermission.ts` |
| 事件处理 | handle 前缀 | `handleSearch()`, `handleDelete()` |
| Props 接口 | 组件名 + Props | `MetricCardProps` |
| API 函数 | 动词 + 资源名 | `getModels()`, `createModel()` |
| 常量 | UPPER_SNAKE_CASE | `MODEL_TYPE_OPTIONS` |

**样式约定**:

```vue
<!-- 使用 scoped 样式 + BEM 命名 -->
<style scoped>
.model-detail__header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
}
.model-detail__actions {
  display: flex;
  gap: 8px;
}
</style>
```

- 使用 `scoped` 避免样式污染
- 全局样式统一在 `src/assets/styles/` 中定义
- 颜色/间距等使用 Ant Design Vue 的 Design Token

**国际化预留**:

```typescript
// 文本统一提取为常量，便于后续 i18n 迁移
const MODEL_STATUS_MAP: Record<string, string> = {
  DRAFT: '草稿',
  REGISTERED: '已注册',
  PUBLISHED: '已发布',
  DEPRECATED: '已废弃',
}
```

---

> **文档结束** - MAIDC 前端开发指南 v1.0
