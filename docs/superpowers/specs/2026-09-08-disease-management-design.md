# MAIDC 专病管理设计（随访管理 · 慢性鼻窦炎试点）

- 日期：2026-09-08
- 状态：已确认（§1-§8 分节评审通过）
- 范围：在既有"病种队列"之上构建随访管理层——随访方案、患者建档、任务执行、量表评估、治疗记录、结局统计；首批试点病种为慢性鼻窦炎（CRS，ICD-10 J32）

---

## 1. 背景与定位

### 1.1 现有基础（复用，不推翻）

| 已有 | 现状 |
|------|------|
| 病种队列 | `cdr.c_disease_cohort` / `c_disease_cohort_patient` / `system.s_disease_template`；入组规则 JSONB（`{groupLogic, groups:[{domain, logic, conditions:[{field,operator,value}]}]}`）；AUTO 重同步删除重插 / MANUAL 保留；16 个内置模板（**无 J32**） |
| 患者数据 | CDR `c_patient` / `c_encounter` / `c_diagnosis` / 检验检查用药视图（患者360 已有聚合出口） |
| 权限 | 8 角色 RBAC + `@RequirePermission` 切面 + DEPT 数据范围（2026-09-08 权限系统落地） |
| 消息 | RabbitMQ + maidc-msg 站内信/WebSocket 管道 |

### 1.2 已知队列引擎缺陷（试点前置修复项）

1. **DIAGNOSIS 域列名映射错误**：引擎映射 `diagnosis_code`，但 `c_diagnosis` 实际列为 `icd_code` / `icd_name` → J32 队列会静默匹配 0 人，**必须修复**试点才能入组
2. LAB 域仅匹配 test_code，无值域比较（本期 CRS 不依赖，不修）
3. 无时间窗条件（同上，不修）
4. `DiseaseAiService.suggestRules` 为空壳（不修）

### 1.3 定位与命名

- **队列负责"哪些患者入组"，专病管理负责"入组后怎么管"**：方案 → 建档 → 任务 → 评估/治疗 → 结局
- 前端菜单沿用"病种队列"名称，新能力以"随访管理"呈现（避免与既有模块撞名）
- 首批试点 CRS；架构按病种无关设计（方案/量表均可配置），后续病种零代码扩展

## 2. 总体架构

```
病种队列（已有）                    专病管理（本期新增）
c_disease_cohort ──锚定──▶ c_followup_protocol   随访方案（阶段序列，版本化）
c_disease_cohort_patient ┘  c_patient_followup   患者随访档案（含脱组状态）
                            c_followup_task      随访任务（按方案阶段生成）
                            c_scale_definition   量表定义（可配置引擎）
                            c_scale_assessment   量表评估记录（答案+得分快照）
                            c_treatment_record   治疗记录（手工 + CDR 带入快照）
```

- 后端：maidc-data（:8082），API 前缀 `/api/v1/cdr`，新增 controller/service/entity/repository 按 `Followup*` / `Scale*` / `TreatmentRecord*` 命名，与 `DiseaseCohort*` 并列
- 提醒链路：maidc-data `@Scheduled` 每日扫描 → MQ → maidc-msg 站内信（当日到期/超期 → 负责护士；超 7 天 → 升级负责医生）
- 前端信息架构（maidc-portal）：
  - DiseaseDetail 新增「**随访管理**」Tab（建档列表 + 档案详情：任务时间轴/评估曲线/治疗记录/方案信息）
  - 新「**随访工作台**」页（护士视图：今日到期 / 超期 / 未来 分组）
  - 新「**量表管理**」页（data_admin：量表定义 CRUD + 版本）
  - DiseaseDetail 新增「**结局看板**」区块（统计聚合展示）

## 3. 数据模型（6 新表，全部无外键，业务层保证完整性）

SQL 文件：`docker/init-db/19-disease-followup.sql`（18 已被审计事件类型补丁占用）。

### 3.1 c_followup_protocol（随访方案）

| 列 | 类型 | 说明 |
|---|---|---|
| id | BIGSERIAL PK | |
| cohort_id | BIGINT NOT NULL | 所属队列（业务层校验存在） |
| name | VARCHAR(128) NOT NULL | 方案名 |
| version | INT NOT NULL DEFAULT 1 | 版本（发布后修改 → 新版本行） |
| status | VARCHAR(16) NOT NULL DEFAULT 'DRAFT' | DRAFT / PUBLISHED / ARCHIVED |
| stages | JSONB NOT NULL | 阶段序列（见下） |
| 审计列 | | created_by / created_at / updated_at / is_deleted（与 BaseEntity 对齐） |

约束：`UNIQUE(cohort_id, version)`。

`stages` JSONB 结构：

```json
[
  {"stageCode": "BASELINE", "name": "基线", "offsetDays": 0,
   "requiredScales": ["SNOT22", "LUND_KENNEDY", "OLFACTION"], "optionalScales": ["LUND_MACKAY"], "note": "建档当日完成"},
  {"stageCode": "M3", "name": "3个月随访", "offsetDays": 90,
   "requiredScales": ["SNOT22"], "optionalScales": [], "note": ""},
  {"stageCode": "M6", "name": "6个月随访", "offsetDays": 180,
   "requiredScales": ["SNOT22", "LUND_KENNEDY"], "optionalScales": [], "note": ""},
  {"stageCode": "M12", "name": "12个月随访", "offsetDays": 365,
   "requiredScales": ["SNOT22"], "optionalScales": ["LUND_MACKAY"], "note": "复查CT可选"}
]
```

CRS 种子方案（J32 队列绑定）：基线 + 3/6/12 月四阶段，发布即 PUBLISHED。

### 3.2 c_patient_followup（患者随访档案）

| 列 | 类型 | 说明 |
|---|---|---|
| id | BIGSERIAL PK | |
| cohort_id | BIGINT NOT NULL | |
| patient_id | BIGINT NOT NULL | 必须在队列中（建档时校验，否则 404） |
| protocol_id | BIGINT NOT NULL | |
| protocol_version | INT NOT NULL | **快照**：方案升级不影响存量档案 |
| doctor_id | BIGINT NOT NULL | 负责医生 |
| nurse_id | BIGINT | 随访护士（可空 = 未分配） |
| status | VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' | ACTIVE / SUSPENDED / CLOSED / OUT_OF_COHORT |
| enroll_date | DATE NOT NULL | 建档日（任务 due 计算锚点） |
| close_reason | VARCHAR(512) | CLOSED 时必填 |
| out_of_cohort_at | TIMESTAMP | 脱组时间 |
| 审计列 | | |

约束：部分唯一索引 `uq_active_followup(cohort_id, patient_id) WHERE status = 'ACTIVE'`（同一队列一个活跃档案；脱组/结案后可重新建档）。

### 3.3 c_followup_task（随访任务）

| 列 | 类型 | 说明 |
|---|---|---|
| id | BIGSERIAL PK | |
| followup_id | BIGINT NOT NULL | |
| stage_code | VARCHAR(32) NOT NULL | 方案阶段码快照 |
| stage_name | VARCHAR(64) | 展示用快照 |
| due_date | DATE NOT NULL | `enroll_date + offsetDays` |
| status | VARCHAR(16) NOT NULL DEFAULT 'PENDING' | PENDING / DONE / SKIPPED |
| required_scales | JSONB | 该阶段量表码快照 `["SNOT22", ...]` |
| skip_reason | VARCHAR(512) | SKIPPED 时必填（医生操作） |
| completed_at / completed_by | TIMESTAMP / BIGINT | |
| 审计列 | | |

索引：`(followup_id)`、`(status, due_date)`。**OVERDUE 不落库**——查询时派生（`due_date < CURRENT_DATE AND status = 'PENDING'`），避免状态同步 bug；定时任务只发提醒不改状态。

### 3.4 c_scale_definition（量表定义）

| 列 | 类型 | 说明 |
|---|---|---|
| id | BIGSERIAL PK | |
| scale_code | VARCHAR(32) NOT NULL | SNOT22 / LUND_KENNEDY / LUND_MACKAY / OLFACTION |
| name | VARCHAR(128) NOT NULL | |
| version | INT NOT NULL DEFAULT 1 | 定义变更 → 新版本 |
| status | VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' | ACTIVE / DISABLED（停用后不可被新方案引用，历史快照照常展示） |
| definition | JSONB NOT NULL | 见下 |
| 审计列 | | |

约束：`UNIQUE(scale_code, version)`。

`definition` JSONB 结构（通用条目型引擎）：

```json
{
  "type": "LIKERT_ITEMS",
  "items": [
    {"no": "1", "text": "需要擤鼻涕", "options": [{"value": 0, "label": "没有"}, {"value": 1, "label": "轻微"}, ... {"value": 5, "label": "非常严重"}]},
    ...共22条...
  ],
  "maxScore": 110,
  "mcid": 9,
  "interpretation": [{"min": 0, "max": 20, "label": "轻微"}, {"min": 21, "max": 50, "label": "中度"}, {"min": 51, "max": 110, "label": "重度"}]
}
```

双侧量表（Lund-Kennedy / Lund-Mackay）用 `side` 字段区分条目：`{"no": "L1", "side": "LEFT", "text": "鼻息肉", "options": 0-2}`。计分统一 = 条目值求和，服务端计算。

### 3.5 c_scale_assessment（量表评估记录）

| 列 | 类型 | 说明 |
|---|---|---|
| id | BIGSERIAL PK | |
| followup_id | BIGINT NOT NULL | |
| task_id | BIGINT | NULL = 计划外评估（医生随时发起） |
| scale_code | VARCHAR(32) NOT NULL | |
| scale_version | INT NOT NULL | 快照 |
| answers | JSONB NOT NULL | `{"1": 3, "2": 2, ...}` 条目号 → 值 |
| total_score | INT NOT NULL | **服务端计算**，不信任前端 |
| assessed_at | TIMESTAMP NOT NULL | |
| assessed_by | BIGINT NOT NULL | |
| 审计列 | | |

约束：`UNIQUE(task_id, scale_code)`（计划内任务每量表一条；task_id 为 NULL 不受约束）。

### 3.6 c_treatment_record（治疗记录）

| 列 | 类型 | 说明 |
|---|---|---|
| id | BIGSERIAL PK | |
| followup_id | BIGINT NOT NULL | |
| task_id | BIGINT | 可关联随访任务 |
| category | VARCHAR(32) NOT NULL | MEDICATION / SURGERY / OTHER |
| name | VARCHAR(256) NOT NULL | 鼻用糠酸莫米松 / ESS（功能性内镜鼻窦手术）等 |
| detail | JSONB | `{dosage, route, startDate, endDate, surgeon, hospitalDays}` |
| source | VARCHAR(8) NOT NULL DEFAULT 'MANUAL' | MANUAL / **CDR（带入只读快照）** |
| source_ref | JSONB | CDR 来源引用 `{resourceType, recordId, encounterId}` |
| occurred_date | DATE NOT NULL | |
| 审计列 | | |

CRS 治疗类别枚举（可扩展）：MEDICATION（鼻用激素/口服激素/大环内酯类/抗白三烯/生物制剂/盐水冲洗）、SURGERY（ESS/FESS、球囊扩张）、OTHER（免疫治疗等）。

### 3.7 四量表种子

| 量表 | 结构 | 满分 | MCID |
|---|---|---|---|
| SNOT-22 | 22 条 × 0-5 | 110 | 9（最小临床重要差异） |
| Lund-Kennedy（内镜） | 双侧 ×（息肉/水肿/分泌物）× 0-2 | 12 | —（越低越好） |
| Lund-Mackay（CT） | 双侧 ×（上颌窦/前组筛窦/后组筛窦/蝶窦/额窦/OMC）× 0-2 | 24 | — |
| 嗅觉主观分级 | 单条 0-3（正常/轻/中/完全丧失） | 3 | — |

SNOT-22 中文条目（22 项：鼻塞、流涕、打喷嚏、咳嗽、鼻涕倒流、耳闷、耳痛、头晕/不稳、头痛、面部疼痛压迫感、嗅觉减退、味觉减退、睡眠困难、夜间反复醒、睡眠质量差、晨起疲乏、疲劳、效率下降、注意力不集中、沮丧/烦躁、悲伤、尴尬）入库前与临床确认正式中文版措辞。

## 4. 随访工作流与状态机

```
医生（disease:followup:manage）                护士（disease:followup:work）
─────────────────────────────                ─────────────────────────
定义/绑定方案 → PUBLISHED                     随访工作台（今日到期/超期/未来）
队列患者一键建档 ─┐                           打开任务 → 填 requiredScales
  · 校验患者在队列（否则404）                 · 可选记治疗
  · 快照 protocol_version                     提交（一个事务：
  · 按 stages 全量生成任务                       assessments 批量 + task DONE）
    due = enroll_date + offsetDays
  · 指派负责医生/护士
```

**状态机：**

- followup：`ACTIVE ⇄ SUSPENDED`（暂停=任务冻结不可执行）`→ CLOSED(原因必填)` / `→ OUT_OF_COHORT`
- task：`PENDING → DONE` / `PENDING → SKIPPED`（医生操作，原因必填）
- OVERDUE：派生态，不落库（见 §3.3）

**关键语义：**

1. **方案变更 = 新版本**：已 PUBLISHED 方案修改 → `version+1` 新行；存量档案不受影响（快照语义）；新建档用最新 PUBLISHED 版本
2. **升级方案**：医生对单个档案执行 `upgrade-protocol` → 切到最新版本，**只重新生成未来未完成任务**（PENDING 且 due_date ≥ 今天，删除重建；DONE/SKIPPED 与已过期任务不动）
3. **脱组**：队列 AUTO 重同步后患者不再匹配 → 对应档案标 `OUT_OF_COHORT`（数据全保留，任务冻结只读展示）；MANUAL 模式不自动脱组；患者重新入队后可重新建档（历史档案保留）
4. **计划外评估**：医生可随时发起（task_id=NULL，不受 UNIQUE 约束）
5. **提醒分级**：每日定时扫描 → 当日到期/已超期 → 站内信负责护士；超 7 天 → 升级通知负责医生

## 5. 权限对接（复用 2026-09-08 权限系统）

| 新权限码 | 用途 | 授予角色 |
|---|---|---|
| `disease:followup:manage` | 方案定义/建档/分配/跳过/结案/升级方案/计划外评估/结局看板 | doctor, admin |
| `disease:followup:work` | 工作台执行、填量表与治疗记录、完成任务 | nurse, doctor, admin |
| `disease:scale:manage` | 量表定义管理 | data_admin, admin（医生只读） |

- 数据范围：doctor/nurse 沿用 **DEPT**——随访实例可见性 = 患者可见性（患者任一就诊科室 ∈ 用户科室，与权限系统 ∃ 语义一致）
- researcher 不授予任何专病管理码——**科研与临床通道隔离**（科研走 RDR 脱敏数据集）
- 种子增量进 `s_permission` + 角色矩阵（后续 DDL 文件追加，不动已提交的 17-permission-system.sql）

## 6. 结局统计（DiseaseDetail「结局看板」区块）

全部从 assessment / treatment / task 聚合，**无独立统计表**：

| 指标 | 定义 |
|---|---|
| 随访依从率 | 应完成任务（due_date ≤ 统计日）中 DONE 占比 |
| SNOT-22 改善率 | 有基线且末次评估距基线 ≥ 3 个月的患者中，`基线 − 末次 ≥ MCID(9)` 占比 |
| 量表趋势 | SNOT-22 / Lund-Kennedy / Lund-Mackay / 嗅觉 按基线/3/6/12 月均分折线 |
| 手术率 | 建档患者中有 SURGERY 记录占比（ESS 单列） |
| 再手术 | ≥ 2 条 ESS 记录的患者数 |
| 药物分布 | MEDICATION 记录按 name 聚类计数（鼻用激素/生物制剂等） |

## 7. API 端点（maidc-data，前缀 /api/v1/cdr）

| 资源 | 端点 | 权限码 |
|---|---|---|
| 方案 | `GET/POST/PUT /disease-cohorts/{id}/protocols`（PUT=发布新版本） | manage |
| 建档 | `POST /disease-cohorts/{id}/followups`、`GET /disease-cohorts/{id}/followups`（分页/状态筛选） | manage |
| 档案操作 | `POST /followups/{fid}/close` `suspend` `resume` `upgrade-protocol` | manage |
| 任务 | `GET /followup-tasks/my`（工作台，按护士/医生）、`GET /followups/{fid}/tasks`（时间轴） | work / manage |
| 执行 | `POST /followup-tasks/{tid}/complete`（body: assessments[] + treatments?[]，一个事务）、`POST /followup-tasks/{tid}/skip`（body: reason） | complete=work，skip=manage |
| 评估 | `GET /followups/{fid}/assessments`、`POST /followups/{fid}/assessments`（计划外，manage） | work / manage |
| 量表 | `GET /scales`、`POST/PUT /scales`、`GET /scales/{code}/versions` | 读=work，写=scale:manage |
| 治疗 | `GET/POST /followups/{fid}/treatments`、`POST /followups/{fid}/treatments/import-cdr`（勾选条目带入） | work |
| 统计 | `GET /disease-cohorts/{id}/outcome-stats` | manage |

所有端点同时受 DEPT 数据范围约束（Service 层复用 Task 5 的患者可见性判定，范围外 → 404 不暴露存在性）。

## 8. 边界与错误处理

| 场景 | 行为 |
|---|---|
| 任务已 DONE 再提交 | 409 |
| answers 缺条目 / 值越界 / 引用不存在量表 | 400（服务端逐条校验 + 计分） |
| 建档时患者不在队列 | 404（不暴露存在性） |
| 建档时方案含 DISABLED 量表 | 400，阻止建档 |
| CDR 带入重复（同 name + occurred_date 已存在 source=CDR 记录） | 跳过并在响应中列出跳过项 |
| SUSPENDED/OUT_OF_COHORT/CLOSED 档案的任务提交 | 409（任务冻结） |
| 护士提交他人科室患者任务 | 404 + 审计事件（复用权限系统模式） |
| complete 事务性 | assessments 批量插入 + task 更新同一事务，失败整体回滚 |
| 脱敏 | 沿用规则配置联动（doctor/nurse 豁免角色看明文），零新逻辑 |

## 9. 测试策略

- **单元**：量表引擎（计分/校验/版本快照）、任务生成（offsetDays 边界）、状态机流转（全部合法/非法迁移）、脱组/升级方案语义
- **集成**（MockMvc + mock PermissionStore，同权限系统 Task 12 模式）：建档 → 任务生成 → 护士完成 → 统计聚合全链路；DEPT 越权 → 404
- **边界**：OVERDUE 派生正确性、重复提交 409、UNIQUE(task_id, scale_code)、方案升级不覆盖已完成任务

## 10. 实施顺序建议（供 writing-plans 展开）

1. 前置：修复队列引擎 DIAGNOSIS 域列名映射（`icd_code`），新增 J32 队列模板种子，验证 J32 匹配非空
2. DDL：`19-disease-followup.sql`（6 表 + 4 量表种子 + J32 方案种子 + 3 权限码/矩阵增量）
3. 后端：实体/仓库/量表引擎 → 方案与建档 → 任务生成与执行 → 治疗记录与 CDR 带入 → 统计聚合 → 提醒定时任务
4. 前端：DiseaseDetail 随访管理 Tab → 随访工作台 → 量表管理页 → 结局看板区块
5. 集成测试 + 回归

## 11. 不纳入本期（YAGNI）

患者自填端（微信/短信问卷）、RDR 科研抽取联动、DICOM 影像内嵌查看（仅 CT 评分）、消息模板配置化、量表多语言、阶段窗口期强制拦截（windowDays 仅后续如需再加）。
