# 功能实现记录：首页工作台 v2 角色化升级（P0）

| 项目 | 内容 |
|------|------|
| 日期 | 2026-09-09 |
| 分支 | feature/disease-kb |
| 需求来源 | `过程文件/20260909_首页工作台/`（PRD：FR1 角色化指标卡 / FR2 待办接入随访 / FR3 快捷入口角色化 / FR4 欢迎区补全） |
| 实施计划 | `docs/plans/2026-09-09-workspace-v2.md` |
| 原型 | `/proto/workspace`（`views/prototype/workspace/`，实现后删除） |

## 实现概要

`GET /api/v1/workspace/dashboard` 从固定"模型视角"升级为按角色组（CLINICAL/RESEARCH/DATA/GOVERNANCE）差异化返回，响应向后兼容（旧字段保留，新增 cards/todoStats 可选字段）。

- 角色透传链路：JWT roles claim（已有）→ 网关 AuthFilter 透传 `X-User-Roles`（逗号分隔）→ WorkspaceController 解析。多角色用户按 CLINICAL > RESEARCH > DATA > GOVERNANCE 取第一命中组；角色缺失回退 DATA 组（v1 行为）。
- 随访待办：CLINICAL 组待办中心合并 `cdr.c_followup_task`（新增 `FollowupTaskService.workspaceDigest`：护士优先/医生兜底、今日/超期/未来7天 + 患者姓名批量富化 + 本周完成统计）；随访待办不提供"完成"操作，提供"开始随访"跳转。随访源查询失败降级为仅 personal_task，不影响接口可用性。
- 指标卡：`cards[]` 数组通用渲染，CLINICAL=随访四卡、RESEARCH=队列/数据集四卡、DATA/GOVERNANCE=v1 模型四卡（GOVERNANCE 专属指标 P1）。
- 前端兼容：无 `cards` 字段时回退渲染 v1 模型四卡；QuickActions 以服务端角色过滤为准 + `hasPermission` 兜底。

## 修改文件清单

### 后端（maidc-parent）

| 文件 | 修改 |
|------|------|
| maidc-gateway `filter/AuthFilter.java` | 解析 roles claim，透传 `X-User-Roles` |
| maidc-gateway `filter/AuthFilterTest.java` | +2 用例（roles 透传 / 空 roles） |
| maidc-data `task/controller/WorkspaceController.java` | 读取 `X-User-Roles`/`X-Username` 传入 service |
| maidc-data `task/vo/WorkspaceDashboardVO.java` | +MetricCard/TodoStats/TodoItem（含随访字段）/roleGroup/QuickAction.permission |
| maidc-data `task/service/WorkspaceService.java` | 角色组路由；cards/todos/todoStats/quickActions 按组构建；welcome 补 userName/role；随访源降级 |
| maidc-data `data/service/followup/FollowupTaskService.java` | +`workspaceDigest`（统计 + 任务行 + 患者富化） |
| maidc-data `data/repository/FollowupTaskRepository.java` | +本周完成查询 |
| maidc-data `data/repository/DiseaseCohortRepository.java` | +`countByOrgId` |
| maidc-data `src/test/.../WorkspaceServiceTest.java` | 重写：4 角色组 + 兼容回退 + 降级共 5 用例 |

### 前端（maidc-portal）

| 文件 | 修改 |
|------|------|
| `src/api/workspace.ts` | VO 扩展：MetricCard/TodoStats/WorkspaceTodo/cards 可选 |
| `src/stores/workspace.ts` | completeTask 后 todoStats.total 同步 |
| `src/views/dashboard/workspace/icons.ts` | 新增：icon key → 组件映射 + tone 色值 |
| `src/views/dashboard/workspace/MetricCards.vue` | cards 数组通用渲染，无 cards 回退 v1 |
| `src/views/dashboard/workspace/TodoSection.vue` | 随访 tab/专用行渲染/超期升级提醒/开始随访跳转；stats 服务端优先本地兜底 |
| `src/views/dashboard/workspace/QuickActions.vue` | hasPermission 兜底过滤 |
| `src/views/dashboard/workspace/WelcomeSection.vue` | +角色标签 |

数据库无变更。

## 验证结果

- [x] maidc-data 编译通过（`mvn -pl maidc-data -am compile`）
- [x] WorkspaceServiceTest 5/5 通过（含临床组随访合并、降级、角色回退）
- [x] maidc-gateway AuthFilterTest 9/9 通过（含 roles 透传两新用例）
- [x] 前端 vue-tsc：workspace 相关改动文件零错误

## 已知限制 / 后续

- "开始随访"暂跳 `/data/cdr/disease`，随 CRS 前端平移改为正式随访工作台路由（前后端各一处 TODO 标记）
- GOVERNANCE 专属指标卡（审计事件/权限拒绝/告警）P1 经 Feign 补齐，当前复用 DATA 卡片
- P1 待办：快捷患者检索、队列动态卡（PRD FR5/FR6，原型已演示）
- 测试执行使用小堆 JVM（宿主机内存受限），如 CI 正常堆跑一遍更稳
