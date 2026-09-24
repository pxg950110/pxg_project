# 实施计划: 首页工作台 v2 角色化升级（P0）

**需求来源**: `过程文件/20260909_首页工作台/`（PRD + 评估报告）
**日期**: 2026-09-09
**分支**: feature/disease-kb（沿用当前工作分支，不做提交/推送）
**目标**: `/workspace/dashboard` 按角色返回差异化 cards/todos/quickActions，接入随访待办

## 现状复用（不重复建设）

- `FollowupTaskService.myWorkbench`（我负责的=护士优先/医生兜底、今日/超期/未来7天、OVERDUE 派生）→ 新增 `workspaceDigest` 供聚合
- JWT 已含 `roles` claim（`JwtUtils.generateAccessToken`）→ 网关只需透传
- `WorkspaceMetricsRepository.safeQuery` 失败返回 0 的降级模式 → 新增计数沿用

## 修改文件

| 文件 | 修改 |
|------|------|
| maidc-gateway `AuthFilter.java` | 解析 roles claim，透传 `X-User-Roles`（逗号分隔）+ 同步测试 |
| maidc-data `WorkspaceController.java` | 读取 `X-User-Roles`/`X-Username` 传入 service |
| maidc-data `WorkspaceDashboardVO.java` | +roleGroup；+`MetricCard`/`TodoStats`/`TodoItem`（含随访字段）；QuickAction+permission；todos→`List<TodoItem>` |
| maidc-data `FollowupTaskRepository.java` | +本周完成查询（DONE by me since 周一） |
| maidc-data `FollowupTaskService.java` | +`workspaceDigest(userId, today)`：统计 + 任务行（含 patientName 批量富化） |
| maidc-data `DiseaseCohortRepository.java` | +`countByOrgId` |
| maidc-data `WorkspaceService.java` | 角色组解析（CLINICAL>RESEARCH>DATA>GOVERNANCE，空=DATA 兼容 v1）；按组构建 cards/todos/todoStats/quickActions；welcome 补 userName/role |
| 前端 `api/workspace.ts`/`stores/workspace.ts`/workspace 5 组件 | VO 适配；cards 通用渲染（无 cards 时回退 v1 metrics）；待办随访 tab/行渲染/开始随访；QuickActions 权限兜底 |

## 角色组卡片与待办

- CLINICAL：随访四卡（今日/超期/在管患者/本周完成，来自 workspaceDigest）；待办合并 followup+personal
- RESEARCH：在管队列/队列患者/待审批导出/数据集
- DATA & GOVERNANCE：v1 模型四卡（GOVERNANCE 专属指标 P1，见 PRD 7.3）
- todos 合并排序按 dueDate 升序，上限 20；随访源失败降级（仅 personal）

## 验证

- `mvn -pl maidc-gateway,maidc-data -am compile` + 相关测试（AuthFilterTest / workspace 相关）
- 前端 `vue-tsc --noEmit`（改动文件零错误）
- 数据库无变更（`c_followup_task` 已存在）
