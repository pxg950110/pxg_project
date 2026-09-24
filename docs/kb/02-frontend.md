# 02 前端 maidc-portal

## 技术栈（package.json）

- Vue **3.4.27** + TS 5.4.5 + Vite 5.2.12 + vue-tsc（package.json:24-35）
- UI：**ant-design-vue 4.2.3** + @ant-design/icons-vue 7（非 element-plus / 非 win-design）
- 图表：echarts 5.5.0 + vue-echarts 6.7.3（当前仅 components/MetricChart、components/RocCurve 在用）
- 其他：pinia 2.1.7、vue-router 4.3.2、axios、dayjs、nprogress、@vue-flow/*（ETL 可视化设计器）
- dev 端口 3000；构建 `npm run build` = vue-tsc && vite build；类型检查 `npm run type-check`

## 路由

**常量路由**（src/router/constantRoutes.ts）：/login、/403、/404、/500；`/`（Root，BasicLayout，**redirect: '/dashboard'**，:14-17）；Root 下原型页（不进菜单，实现后删除）：`proto/crs/*`（5 页，:25-53）、`proto/workspace`（:56-59）、`proto/health`（:63-66）、`proto/p360`（:70-73，未提交部分）。

**动态路由**（src/router/asyncRoutes.ts，登录后按权限注入，`meta.permission` 过滤）：

| 顶级 | sort | permission | 主要子路由 |
|------|------|-----------|-----------|
| dashboard | 1 | — | workspace（个人工作台）/ overview（系统总览）/ model / data 四看板 |
| model | 2 | model | list/evaluations/approvals/deployments/routes/versions/inference-logs |
| data | 3 | data | cdr/search、cdr/disease、cdr/disease-kb、cdr/scales、cdr/followup/:fid、cdr/patients、cdr/patient/:patientId（患者360）、cdr/quality-*、rdr/projects、rdr/datasets |
| followup | 3 | disease:followup:work | **workbench（随访工作台正式页 `/followup/workbench`）** |
| etl | 4 | data | datasources/pipelines/executions/sync/schedule |
| label | 5 | label | tasks/workspace/:id |
| system | 6 | — | masterdata / dictionaries / sys（users/roles/permissions/config） |
| alert / audit / message | 7/8/9 | —（audit 组 permission: audit） | 见 asyncRoutes.ts:85-170 |

**登录跳转链**：guards.ts（token 校验→getUserInfo→generateRoutes→addRoute）→ `/` → `/dashboard` → `/dashboard/workspace`（登录后第一屏=个人工作台）。

## 权限（前端侧）

- auth store（src/stores/auth.ts:7-33）：token + userInfo；`GET /users/me` 返回 `{id, username, realName, roles[], orgId, permissions[]}`（api/auth.ts:23-30）。前端**无 deptId 字段**，DEPT 概念仅服务端。
- permission store（src/stores/permission.ts:19-33）：`roles.includes('admin')` 全放行；否则按 `meta.permission` 过滤路由；`hasPermission(code)` 供组件兜底。
- 守卫 DEV_MODE：`VITE_MOCK_AUTH=true` 时注入 admin 假用户（guards.ts:14-16,27-35）。

## API 层（src/api/* → 后端端口，vite proxy :15-39）

| 模块 | 后端 | 内容 |
|------|------|------|
| auth.ts / system.ts | 8081 auth | 登录/用户/角色/权限 |
| data.ts、cdr/patientEncounter.ts、patient360.ts、diseaseKb.ts、masterdata.ts、dataElementStandard.ts、medical-dictionary.ts、etl.ts | 8082 data | CDR/RDR/主数据/字典/ETL/KB |
| model.ts | 8083 | 模型/评估/审批/部署/告警/监控 |
| **workspace.ts** | 8084 task | 工作台聚合 + 待办完成 |
| task.ts | 8084 | 调度任务 |
| label.ts / audit.ts / msg.ts | 8085/8086/8087 | 标注 / 审计 / 站内信 |

## 布局与主题

- BasicLayout.vue:1-49：左侧浅色 Sider（220px/64px，logo+SidebarMenu）+ 60px 顶栏（折叠钮+页题+HeaderActions[通知铃铛/设置/用户下拉]）+ TabBar 多页签 + 内容区（#f0f2f5）。
- **主题色默认 `#1677ff`**：localStorage `maidc-primary-color`（stores/ui.ts:15）→ a-config-provider token.colorPrimary + CSS 变量 `--ant-color-primary`（App.vue:14-25）。全局样式仅 assets/styles/global.css。
- 组件库惯例：页面用 `PageContainer` 包裹；列表用 `SearchForm` + `useTable` hook；通用组件在 src/components/（MetricCard/MetricChart/PermissionWrapper/StatusBadge 等 33 个）。

## 原型目录（实现后整目录+路由块删除）

`src/views/prototype/`：crs（随访 5 页+mock）、workspace（WorkspaceV2 评审用）、health-analysis（智能健康分析）、patient360（患者360 重设计）。
