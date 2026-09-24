# 03 后端与权限模型

## Controller 清单（maidc-data，包 com.maidc.data.controller + com.maidc.task.controller）

### 工作台/聚合
| Controller | 路径 | 说明 |
|---|---|---|
| task/WorkspaceController | GET /api/v1/workspace/dashboard（`workspace:read`，读 X-User-Id/X-Org-Id/X-User-Roles/X-Username）<br>PUT /api/v1/workspace/todos/{id}/complete（`workspace:write`） | 工作台聚合（详见 KB 04） |
| task/TaskController | /api/v1/task | 调度任务 |

### CDR/专病/随访
| Controller | 路径 |
|---|---|
| CdrController | /api/v1/cdr |
| ClinicalSearchController | /api/v1/cdr/search |
| SmartSearchController | POST /api/v1/cdr/smart-search |
| DiseaseCohortController | /api/v1/cdr/disease-cohorts（CRUD + /sync + /match-preview + /patients + /statistics + /export + /ai-suggest） |
| DiseaseKnowledgeController | /api/v1/cdr/disease-kb（spaces/items/publish/file/recompute/search/qa，`/qa/ask` SSE） |
| DiseaseTemplateController | /api/v1/dict/disease-templates |
| cdr/Patient360Controller | /api/v1/cdr/patient-360（basic-info/encounter-stats/diagnosis-stats/lab-stats/imaging-stats/allergies/family-history/timeline/completeness-score/search） |
| cdr/CdrQualityCheck/QualityRule/Quarantine/TermMapping/DataLineage | /api/v1/cdr/quality-checks、quality-rules、quarantine、term-mappings、lineage |
| followup/*（7 个） | 见 KB 05 |
| DataSource/DataSourceType/SyncTask/DesensitizeRule | /api/v1/cdr/datasources 等 |

### 其他
- ETL：EtlController(/api/v1/etl)、EtlPipeline/Step/FieldMapping/Execution/Import/Metadata(/api/v1/cdr/etl/*)
- RDR：RdrController(/api/v1/rdr) + rdr/ 下 5 个子 Controller
- 主数据族 16 个：/api/v1/masterdata/*（code-systems/concepts/data-elements/value-domains/object-classes/properties/domains/mappings/local-concepts/reference-ranges/knowledge/institutions/drug-interactions/import…）
- 字典族：dictionary/Drug|Diagnosis|FeeItem|LabItem|ExamItem；DictController(/api/v1/system/dict-types)

## 角色模型（8 角色 × data_scope）

种子：docker/init-db/17-permission-system.sql:41-49；设计：docs/superpowers/specs/2026-09-08-permission-system-design.md:47-56。

| role_code | 名称 | data_scope |
|---|---|---|
| admin | 系统管理员 | ALL |
| doctor | 临床医生 | DEPT |
| nurse | 护士 | DEPT |
| researcher_pi | 科研负责人 | PROJECT |
| researcher | 科研成员 | PROJECT |
| data_admin | 数据管理员 | ALL |
| ai_engineer | AI 工程师 | SELF |
| auditor | 审计员 | ALL |

## DataScope（common/security/scope/DataScope.java:5-24）

- 枚举 `ALL / DEPT / SELF / PROJECT`；DEPT 语义=患者任一就诊科室 ∈ 用户 dept_id（∃ 语义，见 f88801c 提交修复）。
- 多角色取更宽：`widest()`，优先级 ALL > DEPT > PROJECT > SELF。
- 执行体：`DataScopeHelper`；科室名解析链路：`PermissionStore.load(userId)` → `PermissionContext.deptId` → `InstitutionRepository` → `masterdata.m_institution.name`（DeptScopeService.java:31-51）。
- `PermissionContext`（Redis `maidc:auth:perm:{userId}`）：userId/permissions/dataScope/deptId/roles/projectIds（PermissionContext.java:19-30）。
- 网关 AuthFilter 透传 `X-User-Roles`（逗号分隔）供聚合服务做角色组路由。

## 权限码

- 种子 55 码（17-permission-system.sql:56-111）：`workspace:read`、`dashboard:overview/data/model:read`、`cdr:patient:read/export`、`cdr:search/cohort/quality/term/lineage`、`etl:*`、`rdr:*`、`model:*`、`alert:*`、`label:task:*`、`audit:*`、`masterdata:*`、`system:*`、`schedule:task:manage`、`message:template:manage`。
- 随访 3 码（20-disease-followup.sql:257-259）：`disease:followup:manage`（doctor+admin）、`disease:followup:work`（doctor+nurse+admin）、`disease:scale:manage`（data_admin+admin）。
- KB 码（19-disease-kb.sql）：`cdr:diseasekb:read/manage/ai`。
- 代码注解现存粗粒度旧码并存：`cdr:read`(×131)、`cdr:create`(×83)、`rdr:read`(×75)、`masterdata:read/create` 等——新旧双轨，新功能一律用细粒度码。

## 脱敏 × 角色豁免

`cdr.r_desensitize_rule.exempt_role_codes`：NAME/ID_CARD/PHONE/ADDRESS 豁免 `admin,doctor,data_admin`；其余字段豁免 `admin,data_admin`；researcher 系不豁免（17-permission-system.sql:201-204）。

## 待办/任务实体

| 实体 | 表 | 说明 |
|---|---|---|
| task/PersonalTaskEntity | system.t_personal_task | 通用个人待办；task_type CHECK：APPROVAL/LABELING/DATA_QUERY/OTHER（16-personal-task.sql:12-14）；MQ 消费 approval.notify/label.notify 生成 |
| data/FollowupTaskEntity | cdr.c_followup_task | 随访任务（详见 KB 05） |
| task/TaskEntity + TaskExecutionEntity | 调度任务/执行记录 |
