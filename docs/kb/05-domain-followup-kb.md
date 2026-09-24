# 05 专病域：CRS 随访试点 + 专病知识库

> 设计权威：docs/superpowers/specs/2026-09-08-disease-management-design.md（随访）、19/20 号 DDL、docs/feature/2026-09-08-disease-kb.md。

## CRS 随访（慢性鼻窦炎试点）

### 数据模型（cdr schema，6 表，无外键）

| 表 | 要点 |
|----|------|
| c_followup_protocol | 方案，PUT=新版本行 |
| c_patient_followup | 患者随访档案（nurse_id/doctor_id、status ACTIVE/SUSPENDED/CLOSED、protocol version 快照） |
| c_followup_task | 任务：stage_code/stage_name、due_date、status CHECK(PENDING/DONE/SKIPPED)（20-disease-followup.sql:59）、required_scales/optional_scales jsonb、skip_reason、completed_at/by |
| c_scale_definition | 量表，UNIQUE(scale_code,version)；种子 5 个：SNOT22/LUND_KENNEDY/LUND_MACKAY/OLFACTION/CRS_AUX |
| c_scale_assessment | 评估记录，UNIQUE(task_id,scale_code)；计划外评估 task_id=NULL |
| c_treatment_record | 治疗记录 + CDR 候选/带入 |

### API（controller/followup/，前缀 /api/v1/cdr，权限 disease:followup:work / :manage）

| Controller | 关键端点 |
|---|---|
| FollowupTaskController | **GET /followup-tasks/my**（工作台：今日/超期/未来7天，OVERDUE 派生）、GET /followups/{fid}/tasks（时间轴）、POST /followup-tasks/reminders/run、POST /followup-tasks/{taskId}/complete（assessments+treatments+DONE 同事务）、/skip（医生，原因必填）、assessments GET/POST |
| PatientFollowupController | 建档/列表（/disease-cohorts/{cohortId}/followups）、close/suspend/resume/upgrade-protocol、**GET /disease-cohorts/{cohortId}/outcome-stats** |
| FollowupProtocolController | /disease-cohorts/{cohortId}/protocols(+latest) |
| ScaleDefinitionController / ScaleBindingController / FollowupDetailController / TreatmentRecordController | 量表 CRUD+版本、绑定、档案聚合详情、治疗记录(+cdr-candidates/import-cdr) |

### 核心服务

- `FollowupTaskService.myWorkbench(userId,all,today)`（:53-94）：护士优先/医生兜底 → DEPT ∃ 过滤（DeptScopeService）→ {today/overdue/upcoming + stats}。
- `FollowupTaskService.workspaceDigest(userId,today)`（:96-140）：**首页工作台专用摘要** {todayCount, overdueCount, weekDoneCount(周一以来 DONE by me), activePatients, tasks[≤7天 PENDING 行，患者姓名富化]}。
- `FollowupReminderScheduler`（每日 8 点 Asia/Shanghai）：当日/超期 PENDING → 站内信负责护士（无则医生）；**超 7 天 → 升级通知医生**；内存去重 (taskId,日期)，多实例需换 Redis。手动触发：POST /followup-tasks/reminders/run。
- `FollowupNotifyProducer` → maidc.msg exchange/system.notify 队列 → msg 转站内信。
- `OutcomeStatsService.stats(cohortId)`：{totalFollowups, complianceRate%, improvementRate%(SNOT-22 MCID), surgeryRate, reoperationCount, scaleTrends, medicationDistribution}。

### 前端（已平移）

路由：/followup/workbench（FollowupWorkbench.vue，正式工作台）、/data/cdr/scales、/data/cdr/scales/design、/data/cdr/followup/:fid；原型 proto/crs/* 保留至评审结束。工作台待办"开始随访"→ `/followup/workbench?patientId&taskId`。

## 专病知识库（disease-kb P0，提交 2ecad8b）

- 表（19-disease-kb.sql，5 张）：空间（icdCodes text[]、cohortId 队列联动）、条目（四类 GUIDELINE/LITERATURE/PATHWAY/SCALE；状态机 DRAFT→PUBLISHED→ARCHIVED；ai_summary/ai_extract/ai_status）、附件（MinIO diseasekb 桶 ≤50MB PDF/DOCX/XLSX）、QA 会话/消息。
- 检索：zhparser FTS + ILIKE 回退（14 号脚本 FTS 基建）。
- Controller：DiseaseKnowledgeController /api/v1/cdr/disease-kb（spaces/items CRUD + publish + file + recompute + search + qa/sessions + **/qa/ask SSE**），20 接口全量 @RequirePermission(`cdr:diseasekb:read/manage/ai`)。
- **AI 编排未接线**：DiseaseKnowledgeAiService → ai-worker(/llm/summary,/embedding,/rag/chat) 设计就绪；内容变更置 ai_status=PENDING，/qa/ask 降级 KB_AI_UNAVAILABLE(5034)。
- 发布事件：publishItem PUBLISH → `cdr.c_cohort_event` 记 KB_ITEM_PUBLISHED（工作台 cohortDigest 源，2026-09-11 接入）。
