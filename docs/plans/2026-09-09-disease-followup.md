# 实施计划: CRS 专病管理（随访管理）后端开发

**日期**: 2026-09-09
**设计依据**: `docs/superpowers/specs/2026-09-08-disease-management-design.md`（已确认）+ PRD（过程文件/20260909/02）
**分支**: feature/disease-kb（延续当前工作分支）

## 范围（本轮 = 后端 P0）

### Task 1 前置修复（队列引擎 J32 缺陷）
- `entity/DiagnosisEntity.java`: 列名 `diagnosis_code/diagnosis_name` → `icd_code/icd_name`（对齐 04-cdr.sql DDL）
- `DiseaseCohortService.DOMAIN_FIELD_MAP`: DIAGNOSIS 域映射 `icd_code/icd_name`
- `SmartSearchService`: DIAGNOSIS 分支 SQL 同步改列名
- J32 疾病模板种子（s_disease_template）

### Task 2 DDL `docker/init-db/20-disease-followup.sql`
- 6 表: c_followup_protocol / c_patient_followup / c_followup_task / c_scale_definition / c_scale_assessment / c_treatment_record
- 种子: 5 量表（SNOT22/LUND_KENNEDY/LUND_MACKAY/OLFACTION/CRS_AUX 含 binding）、J32 队列 + 随访方案 v1、3 权限码（disease:followup:manage/work/scale:manage）+ 角色矩阵增量

### Task 3 实体 + Repository（6 组）
- 按 DiseaseCohortEntity 模式：@Where(is_deleted=false)/@SQLDelete/jsonb String 列
- 关键唯一约束: UNIQUE(scale_code,version)、UNIQUE(task_id,scale_code)、部分唯一 uq_active_followup

### Task 4 量表引擎 + ScaleService
- ScaleEngine: definition 解析（items/options/binding）、answers 逐条校验、服务端计分
- ScaleService: 列表/详情/保存草稿/发布新版本

### Task 5 方案 + 建档 + 任务
- FollowupProtocolService: 方案 CRUD/发布（PUT=新版本行）
- PatientFollowupService: 建档（校验在队列、快照 version、按 stages 生成任务 due=enroll+offset）、close/suspend/resume/upgrade-protocol（只重建未来 PENDING）

### Task 6 任务执行 + 评估 + 治疗
- complete: 一个事务（assessments 批量 + task DONE），409/400 边界
- skip: manage + 原因必填
- 计划外评估（task_id NULL）
- 治疗记录 CRUD + CDR 候选/带入（重复跳过）

### Task 7 结局统计 OutcomeStatsService
- 依从率/SNOT-22 改善率(MCID)/手术率/再手术/量表趋势/药物分布

### Task 8 Controller（/api/v1/cdr 前缀，@PreAuthorize 三码）
- FollowupProtocolController / FollowupController / FollowupTaskController / ScaleDefinitionController / TreatmentRecordController

### Task 9 验证
- mvn 编译 + 核心单测（量表引擎计分、任务生成、状态机）
- 启动冒烟（DDL 已在 docker init-db，需手动 psql 执行 20 号脚本）

## 后续轮次（不在本轮）
- 提醒 @Scheduled + MQ、前端平移（原型 → 正式页面）、DEPT 数据范围细化（复用 DataScopeHelper）
