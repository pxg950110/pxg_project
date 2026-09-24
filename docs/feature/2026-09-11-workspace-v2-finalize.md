# 功能实现记录：首页工作台 v2 收尾（GOVERNANCE 卡 / 质控卡 / welcome 补全 / FR5 快捷患者检索）

## 基本信息

| 项目 | 内容 |
|------|------|
| 日期 | 2026-09-11 |
| 需求号 | 20260911（无 TFS，日期临时号） |
| 分支 | feature/disease-kb（延续 workspace v2 工作） |
| 需求来源 | `过程文件/20260909_首页工作台/`（PRD FR1 收尾 + FR4 + FR5）；v2 实现记录 `docs/feature/2026-09-09-workspace-v2.md` 已知限制清单 |
| 实施计划 | `docs/plans/2026-09-11-workspace-v2-finalize.md` |

## 实现概要

补齐 v2 交付时标注"降级/P1"的收尾项，使 4 个角色组的指标卡全部为 PRD 专属定义，并交付 FR5 快捷患者检索：

1. **GOVERNANCE 专属四卡**（替代复用 DATA 卡的降级）：平台用户（system.s_user）/ 今日审计事件（audit.a_audit_log 当日计数）/ 今日权限拒绝（audit.a_system_event event_type='PERMISSION_DENIED' 当日计数，tone=danger）/ 活跃告警（model.m_alert_record status∈PENDING,ACKNOWLEDGED，tone=warning）。
2. **DATA 组第 4 卡改"待处理质控"**（PRD FR1 卡片定义）：cdr.cdr_quarantine_data 中 status='PENDING' 的隔离记录数，route=/data/cdr/quality-results；待审批数仍保留在 metrics 字段（RESEARCH 组卡片亦在用）。
3. **welcome 补机构/科室**（FR4）：WelcomeInfo 新增 orgName/deptName（可空）。解析链路复用 `PermissionStore.load(userId)` → `PermissionContext.deptId` → `InstitutionRepository` → `m_institution.name`（与 DeptScopeService.deptFilterName 同源）；orgName 按 orgId 同表解析。任一步不可得置 null，逐项 try/catch 降级，欢迎区永不因此失败。
4. **FR5 快捷患者检索**：欢迎栏右侧 `a-input-search`（仅 `hasPermission('cdr:read')` 显示），回车跳 `/data/cdr/patients?keyword=xxx`；PatientList 挂载时读取 `route.query.keyword` 回填搜索表单并带参查询。唯一患者直达 360 按 PRD 属 P2 联想范畴，未做。

数据访问全部沿用 `WorkspaceMetricsRepository.safeQuery` 跨 schema 直查 + 失败降级 0 的既有模式；无新增表、无新增服务、无 Feign。

## 修改文件清单

### 后端（maidc-parent/maidc-data）

| 文件 | 修改 |
|------|------|
| `task/repository/WorkspaceMetricsRepository.java` | +5 计数：countUsersByOrgId / countTodayAuditEventsByOrgId / countTodayPermissionDeniedByOrgId / countActiveAlertsByOrgId / countPendingQuarantineByOrgId |
| `task/vo/WorkspaceDashboardVO.java` | WelcomeInfo +orgName/deptName（可空） |
| `task/service/WorkspaceService.java` | +governanceCards(orgId)；dataCards 第 4 卡改 quality_pending；buildWelcome 增加 userId/orgId 参数并解析机构/科室名（注入 PermissionStore/InstitutionRepository） |
| `src/test/.../task/service/WorkspaceServiceTest.java` | 新增 3 用例（GOVERNANCE 专属卡 / welcome 机构科室解析 / 无 dept 上下文名称为 null）；DATA 组用例断言更新为 quality_pending |

### 前端（maidc-portal）

| 文件 | 修改 |
|------|------|
| `src/api/workspace.ts` | WelcomeInfo +orgName?/deptName? |
| `src/views/dashboard/workspace/WelcomeSection.vue` | +机构·科室行（orgName/deptName 有值才展示）；+快捷患者检索框（cdr:read 门控，白色半透明样式嵌入渐变横幅） |
| `src/views/dashboard/workspace/WorkspaceView.vue` | 向 WelcomeSection 透传 org-name/dept-name |
| `src/views/data-cdr/PatientList.vue` | onMounted 读取 route.query.keyword → formState 回填 + setSearchParams + 查询 |

icon 复用 `icons.ts` 既有映射（user/file-search/shield/alert），零新增。

## 验证结果

- [x] `mvn -pl maidc-data -am test -Dtest=WorkspaceServiceTest`：**8/8 通过**（tests=8 errors=0 failures=0）
- [x] `npm run type-check`（vue-tsc）：本次改动 4 文件**零错误**（全量输出的存量错误集中于 data-etl/model/system/prototype 等未触及模块，与 v2 验收口径一致）
- [ ] 启动冒烟（需本地起 auth/data/gateway 三服务，未执行）

## 边界与降级语义

- 指标计数：单源查询失败仅该卡显示 0，不影响聚合接口可用性（safeQuery）。
- welcome 机构/科室：PermissionStore 未命中（旧 token/内部调用）或机构表无对应记录 → 字段为 null，前端隐藏对应展示。
- 质控口径：仅统计 status='PENDING'（新入隔离区待处理）；ASSIGNED/PROCESSING 属处理中不计入。
- 告警口径：m_alert_record 无 is_deleted 列，活跃 = status != RESOLVED（PENDING/ACKNOWLEDGED）。
- 审计口径：今日审计事件 = a_audit_log 当日全部操作日志条数（含 READ）；权限拒绝为独立 a_system_event 事件流。

## 已知限制 / 后续

- ~~FR6 cohortDigest 未做~~ → **本轮已完成**（见下节"遗留项收尾"）
- ~~"开始随访"仍暂跳 /data/cdr/disease~~ → **本轮已完成**（正式路由 /followup/workbench）
- FR7 剩余：sparkline 7 日趋势迷你图（需后端趋势接口，未做）；FR8 公告条/密度切换（P3 远期）
- GOVERNANCE"平台用户"按 org_id 统计；若后续多机构入驻需明确"平台用户"口径（全量 vs 本组织）

---

## 遗留项收尾（同日第二轮）

### 1. FR6 专病队列动态卡（cohortDigest，含前置"队列事件落库"）

**事件表**：新增 `docker/init-db/21-cohort-event.sql` → `cdr.c_cohort_event`（cohort_id 可空 / event_type CHECK：SYNC_DONE、KB_ITEM_PUBLISHED、AI_SUGGEST_PENDING / event_title / event_detail jsonb；org_id+created_at DESC 索引；无外键对齐 20 号脚本惯例）。**部署需手动执行该脚本**（与 20 号同流程）。

**新增代码**：
- `entity/DiseaseCohortEventEntity.java`、`repository/DiseaseCohortEventRepository.java`（findTop3ByOrgId...OrderByCreatedAtDesc）
- `service/DiseaseCohortEventService.java`：`record(...)` best-effort（埋点失败仅 log，不阻断主流程）、`latest(orgId)` 查询失败降级空列表

**事件埋点**（两个真实事件源）：
- `DiseaseCohortService.matchPatients` 末尾 → SYNC_DONE："队列「name」同步完成：新增 n 人，在管 m 人"（inserted 为本次新增 AUTO 记录数；创建/更新队列触发的首次同步自然覆盖）
- `DiseaseKnowledgeService.publishItem` PUBLISH 分支 → KB_ITEM_PUBLISHED："知识库《title》已发布"（cohortId 经 item.spaceId → space.cohortId 关联，未绑定队列的空间为 null）
- AI_SUGGEST_PENDING 仅预留类型：AI 入组建议当前为无状态调用（`DiseaseAiService.suggestRules`），无待审实体可挂事件；待建议持久化后接入

**聚合与响应**：`WorkspaceDashboardVO` + `cohortDigest: CohortDigestItem[]{type,title,time(MM-dd HH:mm),cohortId}`；`WorkspaceService.buildCohortDigest` 仅 CLINICAL/RESEARCH 组返回（其余组 null），整段 try/catch 降级空列表。

**前端**：新增 `views/dashboard/workspace/CohortDigest.vue`（通知卡下方可折叠卡：类型 tag 同步=success/知识库=geekblue/AI 建议=warning + 标题 + 时间，点击带 cohortId 跳队列详情）；`WorkspaceView.vue` 按 `cohortDigest?.length` 条件渲染；通知点击 routeMap 增加 COHORT → `/data/cdr/disease/:bizId`。

### 2. "开始随访"正式路由替换

随访工作台正式路由已随随访前端平移存在：`/followup/workbench`（`views/followup/FollowupWorkbench.vue`，asyncRoutes.ts，permission `disease:followup:work`）。替换两处：
- 前端 `TodoSection.vue`：`goFollowupWorkbench(item)` 跳 `/followup/workbench` 并携带 `query.patientId/taskId` 供工作台定位（原暂跳 /data/cdr/disease 的 TODO 删除）
- 后端 `WorkspaceService` CLINICAL 快捷入口 followup_workbench route 改 `/followup/workbench`

### 3. FR7（部分）：指标卡自定义

`MetricCards.vue` 增加右上角设置入口（a-popover）：逐卡显隐 Switch + 上移/下移排序 + 重置；偏好持久化 localStorage `maidc-workspace-cards`（PRD 指定 key，存 `{hidden:[],order:[]}`，读取容错、写入失败仅本会话生效）。对 cards 与 v1 回退卡同样适用。**未做**：sparkline 趋势迷你图（需后端 7 日趋势接口）。

### 遗留项收尾验证

- [x] `mvn -pl maidc-data -am test -Dtest='WorkspaceServiceTest,DiseaseKnowledgeServiceTest'`：**10/10 + 21/21 通过**（新增 cohortDigest 映射/降级/null 3 断言用例；KB 测试补 @Mock DiseaseCohortEventService 适配构造注入）
- [x] `npm run type-check`：本轮前端改动文件（TodoSection/MetricCards/CohortDigest/WorkspaceView/api）**零错误**
- [ ] 启动冒烟（需先执行 21 号 DDL + 起 auth/data/gateway）
