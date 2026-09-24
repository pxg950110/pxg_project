# 04 首页工作台专题（/dashboard/workspace）

## 演进史

| 阶段 | 提交/文档 | 内容 |
|------|-----------|------|
| v1（2026-04-21） | efc8876 spec → 4f226f4 实现 | 固定"模型视角"4 卡 + personal_task 待办 + 4 硬编码快捷入口；spec：docs/superpowers/specs/2026-04-21-personal-workspace-design.md |
| v2（2026-09-10，2880873） | PRD 过程文件/20260909_首页工作台/ + docs/feature|plans/2026-09-09-workspace-v2.md | 角色组路由（X-User-Roles 透传）+ cards/todos/quickActions 按组下发 + 随访待办接入 + welcome 补全 |
| 收尾（2026-09-11） | docs/feature|plans/2026-09-11-workspace-v2-finalize.md | GOVERNANCE 专属四卡、DATA 质控卡、welcome 机构/科室、FR5 快捷患者检索、FR6 cohortDigest（含事件表）、随访路由替换、FR7 卡片自定义 |

## 产品定义

- 定位：登录后第一屏；"同一骨架，角色化内容"（PRD §一）。
- 角色组（WorkspaceService.java:38-54）：doctor/nurse→CLINICAL；researcher_pi/researcher→RESEARCH；data_admin/ai_engineer→DATA；admin/auditor→GOVERNANCE。多角色取第一命中（CLINICAL>RESEARCH>DATA>GOVERNANCE），角色缺失回退 DATA（v1 兼容）。

## 接口契约：GET /api/v1/workspace/dashboard

权限 `workspace:read`；响应（WorkspaceDashboardVO.java，全部字段实测）：

```jsonc
{
  "welcome": { "userName", "date", "role", "roleGroup", "orgName?", "deptName?" },
  "metrics": { "modelCount", "activeDeployments", "dailyInferences", "pendingApprovals" },  // v1 字段保留
  "cards":   [ { "key", "label", "value", "suffix", "icon", "route", "tone(primary/danger/success/warning)" } ],
  "todos":   [ { "id", "taskType(APPROVAL/LABELING/DATA_QUERY/OTHER/FOLLOWUP)", "title", "priority", "status",
                 "sourceId", "sourceType", "dueDate", "patientId?", "patientName?", "stageName?", "scales?", "overdueDays?" } ],  // ≤20，dueDate 升序
  "todoStats": { "today", "overdue", "total" },
  "notifications": [ { "id", "type", "title", "content", "isRead", "createdAt" } ],       // m_message 最近 5 条
  "quickActions": [ { "key", "label", "icon", "route", "permission?" } ],
  "cohortDigest": [ { "type(SYNC_DONE/KB_ITEM_PUBLISHED/AI_SUGGEST_PENDING)", "title", "time(MM-dd HH:mm)", "cohortId?" } ]  // 仅 CLINICAL/RESEARCH；其余组 null
}
```
向后兼容：无 cards/todoStats/cohortDigest 时前端回退 v1 渲染。

## 各角色组内容（当前实现）

| 组 | 指标卡 cards | 快捷入口 |
|----|--------------|----------|
| CLINICAL | followup_today / followup_overdue(danger) / followup_patients / followup_week_done（数据=FollowupTaskService.workspaceDigest） | 患者检索、临床检索、专病知识库、随访工作台(/followup/workbench) |
| RESEARCH | cohort_active / cohort_patients / pending_approvals(warning) / datasets | 专病队列、临床检索、知识库问答、研究项目、数据集 |
| DATA | model_count / active_deployments / daily_inferences / **quality_pending(warning，隔离区 PENDING 数)** | ETL 管道、数据源、质量检测、新建模型、新建评估 |
| GOVERNANCE | user_count / audit_today / perm_denied_today(danger) / active_alerts(warning) | 系统总览、操作审计、数据访问审计、用户管理、告警中心 |

待办：personal 待办 + （仅 CLINICAL）随访待办合并，随访行专用渲染（量表 tag 红/超期红/>7 天 ⚠ 升级 tooltip），"开始随访"跳 `/followup/workbench?patientId&taskId`，**不可在工作台直接完成**。

## 数据源 SQL 清单（WorkspaceMetricsRepository.java，全部 safeQuery 跨 schema 直查、失败降级 0）

| 指标 | 表 |
|------|----|
| 模型数 / 活跃部署 | model.m_model / m_deployment(status=RUNNING) |
| 今日推理 | model.m_model_daily_stats（SUM invocation_count, stat_date=CURRENT_DATE） |
| 待审批 | model.m_approval(status=PENDING) |
| 通知 | model.m_message（近 5 条，is_read ASC, created_at DESC） |
| 平台用户 | system.s_user |
| 今日审计事件 | audit.a_audit_log（created_at≥CURRENT_DATE） |
| 今日权限拒绝 | audit.a_system_event(event_type='PERMISSION_DENIED') |
| 活跃告警 | model.m_alert_record(status IN PENDING,ACKNOWLEDGED；**表无 is_deleted**) |
| 待处理质控 | cdr.cdr_quarantine_data(status='PENDING') |
| 队列动态 | cdr.c_cohort_event（21 号 DDL，近 3 条；埋点：matchPatients→SYNC_DONE、publishItem→KB_ITEM_PUBLISHED；AI_SUGGEST_PENDING 预留） |
| 随访四卡/随访待办 | FollowupTaskService.workspaceDigest（护士优先/医生兜底、今日/超期/未来7天/本周完成、患者姓名富化） |
| 机构/科室名 | PermissionContext.deptId / orgId → masterdata.m_institution.name |

## 前端结构（views/dashboard/workspace/）

WorkspaceView（编排）→ WelcomeSection（问候+角色标签+机构·科室+FR5 检索框[cdr:read 门控]→跳 /data/cdr/patients?keyword=，PatientList onMounted 透传 route.query.keyword）→ MetricCards（cards 通用渲染+v1 回退+**FR7 显隐/排序**，localStorage `maidc-workspace-cards`）→ TodoSection + NotifySection + CohortDigest（右列，可折叠）→ QuickActions（服务端角色过滤 + hasPermission 兜底）。icon/tone 映射：icons.ts。

## FR1-FR8 状态总表

| FR | 内容 | 状态 |
|----|------|------|
| FR1 | 角色化指标卡 | ✅ 四组均为专属卡（2026-09-11 收尾后） |
| FR2 | 待办中心（随访接入） | ✅ |
| FR3 | 快捷入口角色化 | ✅ |
| FR4 | 欢迎区补全 | ✅（机构/科室已补） |
| FR5 | 快捷患者检索 | ✅（唯一命中直达 360 的联想增强留 P2） |
| FR6 | 队列动态卡 | ✅（AI_SUGGEST_PENDING 事件源待 AI 建议持久化后接入） |
| FR7 | 卡片自定义 + sparkline | ◐ 自定义已完成；sparkline 需后端 7 日趋势接口，未做 |
| FR8 | 密度切换/公告条 | ❌ P3 远期 |

## 迭代清单（加一张指标卡的标准动作）

1. `WorkspaceMetricsRepository` 加计数（safeQuery 模式）
2. `WorkspaceService` 对应组 cards 加 `card(key,label,value,suffix,icon,route,tone)`
3. 前端 `icons.ts` 补 icon key 映射（如缺）
4. `WorkspaceServiceTest` 补断言；特殊逻辑补降级用例
5. 若新表：docker/init-db 增 NNN 脚本并手动执行

## 非功能约束（PRD §7.4）

聚合接口 P95<800ms（可 CompletableFuture 并行化）；待办≤20、通知 5；单源失败仅该区块降级（safeQuery / try-catch 空值），接口永不 500。
