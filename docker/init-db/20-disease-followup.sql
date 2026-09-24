-- ============================================================
-- 专病管理：随访管理（CRS 试点）
-- 设计: docs/superpowers/specs/2026-09-08-disease-management-design.md
-- 6 表 + 5 量表种子 + J32 模板/队列/方案种子 + 3 权限码
-- ============================================================

-- ==================== 1. c_followup_protocol 随访方案 ====================
CREATE TABLE IF NOT EXISTS cdr.c_followup_protocol (
    id                BIGSERIAL    PRIMARY KEY,
    cohort_id         BIGINT       NOT NULL,
    name              VARCHAR(128) NOT NULL,
    version           INT          NOT NULL DEFAULT 1,
    status            VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED')),
    stages            JSONB        NOT NULL,
    created_by        VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by        VARCHAR(64),
    updated_at        TIMESTAMP,
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_protocol_cohort_version UNIQUE (cohort_id, version)
);
COMMENT ON TABLE cdr.c_followup_protocol IS '随访方案（阶段序列，版本化）';
COMMENT ON COLUMN cdr.c_followup_protocol.stages IS '[{stageCode,name,offsetDays,requiredScales[],optionalScales[],note}]';

-- ==================== 2. c_patient_followup 患者随访档案 ====================
CREATE TABLE IF NOT EXISTS cdr.c_patient_followup (
    id                BIGSERIAL    PRIMARY KEY,
    cohort_id         BIGINT       NOT NULL,
    patient_id        BIGINT       NOT NULL,
    protocol_id       BIGINT       NOT NULL,
    protocol_version  INT          NOT NULL,
    doctor_id         BIGINT       NOT NULL,
    nurse_id          BIGINT,
    status            VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','SUSPENDED','CLOSED','OUT_OF_COHORT')),
    enroll_date       DATE         NOT NULL,
    close_reason      VARCHAR(512),
    out_of_cohort_at  TIMESTAMP,
    created_by        VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by        VARCHAR(64),
    updated_at        TIMESTAMP,
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id            BIGINT       NOT NULL DEFAULT 0
);
COMMENT ON TABLE cdr.c_patient_followup IS '患者随访档案（含脱组状态）';
-- 同一队列一个活跃档案；脱组/结案后可重新建档
CREATE UNIQUE INDEX IF NOT EXISTS uq_active_followup ON cdr.c_patient_followup (cohort_id, patient_id) WHERE status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS idx_followup_cohort ON cdr.c_patient_followup(cohort_id);
CREATE INDEX IF NOT EXISTS idx_followup_patient ON cdr.c_patient_followup(patient_id);

-- ==================== 3. c_followup_task 随访任务 ====================
CREATE TABLE IF NOT EXISTS cdr.c_followup_task (
    id                BIGSERIAL    PRIMARY KEY,
    followup_id       BIGINT       NOT NULL,
    stage_code        VARCHAR(32)  NOT NULL,
    stage_name        VARCHAR(64),
    due_date          DATE         NOT NULL,
    status            VARCHAR(16)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','DONE','SKIPPED')),
    required_scales   JSONB,
    optional_scales   JSONB,
    skip_reason       VARCHAR(512),
    completed_at      TIMESTAMP,
    completed_by      BIGINT,
    created_by        VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by        VARCHAR(64),
    updated_at        TIMESTAMP,
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id            BIGINT       NOT NULL DEFAULT 0
);
COMMENT ON TABLE cdr.c_followup_task IS '随访任务（OVERDUE 为派生态不落库：due_date<CURRENT_DATE AND status=PENDING）';
CREATE INDEX IF NOT EXISTS idx_task_followup ON cdr.c_followup_task(followup_id);
CREATE INDEX IF NOT EXISTS idx_task_status_due ON cdr.c_followup_task(status, due_date);

-- ==================== 4. c_scale_definition 量表定义 ====================
CREATE TABLE IF NOT EXISTS cdr.c_scale_definition (
    id                BIGSERIAL    PRIMARY KEY,
    scale_code        VARCHAR(32)  NOT NULL,
    name              VARCHAR(128) NOT NULL,
    version           INT          NOT NULL DEFAULT 1,
    status            VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','DISABLED')),
    definition        JSONB        NOT NULL,
    created_by        VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by        VARCHAR(64),
    updated_at        TIMESTAMP,
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_scale_code_version UNIQUE (scale_code, version)
);
COMMENT ON TABLE cdr.c_scale_definition IS '量表定义（条目型引擎 + 数据绑定）';
COMMENT ON COLUMN cdr.c_scale_definition.definition IS '{type,items:[{no,text,type?,side?,options?,max?,required?,binding?{domain,field,mode,windowDays}}],maxScore,mcid,interpretation[]}';

-- ==================== 5. c_scale_assessment 量表评估记录 ====================
CREATE TABLE IF NOT EXISTS cdr.c_scale_assessment (
    id                BIGSERIAL    PRIMARY KEY,
    followup_id       BIGINT       NOT NULL,
    task_id           BIGINT,
    scale_code        VARCHAR(32)  NOT NULL,
    scale_version     INT          NOT NULL,
    answers           JSONB        NOT NULL,
    binding_sources   JSONB,
    total_score       INT          NOT NULL,
    assessed_at       TIMESTAMP    NOT NULL,
    assessed_by       BIGINT       NOT NULL,
    created_by        VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by        VARCHAR(64),
    updated_at        TIMESTAMP,
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id            BIGINT       NOT NULL DEFAULT 0
);
COMMENT ON TABLE cdr.c_scale_assessment IS '量表评估（服务端计分，binding_sources 记录业务数据带入来源快照）';
-- 计划内任务每量表一条；task_id 为 NULL（计划外）不受约束
CREATE UNIQUE INDEX IF NOT EXISTS uq_task_scale ON cdr.c_scale_assessment(task_id, scale_code) WHERE task_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_assessment_followup ON cdr.c_scale_assessment(followup_id, scale_code);

-- ==================== 6. c_treatment_record 治疗记录 ====================
CREATE TABLE IF NOT EXISTS cdr.c_treatment_record (
    id                BIGSERIAL    PRIMARY KEY,
    followup_id       BIGINT       NOT NULL,
    task_id           BIGINT,
    category          VARCHAR(32)  NOT NULL CHECK (category IN ('MEDICATION','SURGERY','OTHER')),
    name              VARCHAR(256) NOT NULL,
    detail            JSONB,
    source            VARCHAR(8)   NOT NULL DEFAULT 'MANUAL' CHECK (source IN ('MANUAL','CDR')),
    source_ref        JSONB,
    occurred_date     DATE         NOT NULL,
    created_by        VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by        VARCHAR(64),
    updated_at        TIMESTAMP,
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id            BIGINT       NOT NULL DEFAULT 0
);
COMMENT ON TABLE cdr.c_treatment_record IS '治疗记录（手工 + CDR 带入只读快照）';
CREATE INDEX IF NOT EXISTS idx_treatment_followup ON cdr.c_treatment_record(followup_id);

-- ============================================================
-- 量表种子（5 个，WHERE NOT EXISTS 幂等）
-- definition 结构与前端原型 mock.ts 同构
-- ============================================================

-- SNOT-22（22 条 × 0-5，满分 110，MCID 9）
INSERT INTO cdr.c_scale_definition (scale_code, name, version, status, definition, created_by, org_id)
SELECT 'SNOT22', 'SNOT-22 鼻窦炎结局测试（中文版）', 1, 'ACTIVE',
'{"type":"LIKERT_ITEMS","maxScore":110,"mcid":9,
"interpretation":[{"min":0,"max":20,"label":"轻微"},{"min":21,"max":50,"label":"中度"},{"min":51,"max":110,"label":"重度"}],
"items":[
{"no":"1","text":"需要擤鼻涕"},{"no":"2","text":"鼻塞"},{"no":"3","text":"流涕"},{"no":"4","text":"打喷嚏"},
{"no":"5","text":"咳嗽"},{"no":"6","text":"鼻涕倒流"},{"no":"7","text":"耳闷"},{"no":"8","text":"耳痛"},
{"no":"9","text":"头晕/不稳"},{"no":"10","text":"头痛"},{"no":"11","text":"面部疼痛或压迫感"},{"no":"12","text":"嗅觉减退"},
{"no":"13","text":"味觉减退"},{"no":"14","text":"睡眠困难"},{"no":"15","text":"夜间反复醒来"},{"no":"16","text":"睡眠质量差"},
{"no":"17","text":"晨起疲乏"},{"no":"18","text":"疲劳"},{"no":"19","text":"效率下降"},{"no":"20","text":"注意力不集中"},
{"no":"21","text":"沮丧/烦躁"},{"no":"22","text":"悲伤或尴尬"}
],
"itemOptions":[{"value":0,"label":"没有"},{"value":1,"label":"轻微"},{"value":2,"label":"轻度"},{"value":3,"label":"中度"},{"value":4,"label":"重度"},{"value":5,"label":"非常严重"}]}'
::jsonb, 'system', 0
WHERE NOT EXISTS (SELECT 1 FROM cdr.c_scale_definition WHERE scale_code='SNOT22' AND version=1);

-- Lund-Kennedy 内镜评分（双侧 × 3 项 × 0-2，满分 12）
INSERT INTO cdr.c_scale_definition (scale_code, name, version, status, definition, created_by, org_id)
SELECT 'LUND_KENNEDY', 'Lund-Kennedy 内镜评分', 1, 'ACTIVE',
'{"type":"LIKERT_ITEMS","maxScore":12,"mcid":null,
"interpretation":[{"min":0,"max":12,"label":"分值越低越好"}],
"items":[
{"no":"L1","side":"LEFT","text":"鼻息肉（左）"},{"no":"L2","side":"LEFT","text":"鼻甲/黏膜水肿（左）"},{"no":"L3","side":"LEFT","text":"分泌物（左）"},
{"no":"R1","side":"RIGHT","text":"鼻息肉（右）"},{"no":"R2","side":"RIGHT","text":"鼻甲/黏膜水肿（右）"},{"no":"R3","side":"RIGHT","text":"分泌物（右）"}
],
"itemOptions":[{"value":0,"label":"无"},{"value":1,"label":"轻"},{"value":2,"label":"重"}]}'::jsonb, 'system', 0
WHERE NOT EXISTS (SELECT 1 FROM cdr.c_scale_definition WHERE scale_code='LUND_KENNEDY' AND version=1);

-- Lund-Mackay CT 评分（双侧 × 6 窦 × 0-2，满分 24）
INSERT INTO cdr.c_scale_definition (scale_code, name, version, status, definition, created_by, org_id)
SELECT 'LUND_MACKAY', 'Lund-Mackay CT 评分', 1, 'ACTIVE',
'{"type":"LIKERT_ITEMS","maxScore":24,"mcid":null,
"interpretation":[{"min":0,"max":24,"label":"分值越低越好"}],
"items":[
{"no":"L1","side":"LEFT","text":"上颌窦（左）"},{"no":"L2","side":"LEFT","text":"前组筛窦（左）"},{"no":"L3","side":"LEFT","text":"后组筛窦（左）"},
{"no":"L4","side":"LEFT","text":"蝶窦（左）"},{"no":"L5","side":"LEFT","text":"额窦（左）"},{"no":"L6","side":"LEFT","text":"窦口鼻道复合体（左）"},
{"no":"R1","side":"RIGHT","text":"上颌窦（右）"},{"no":"R2","side":"RIGHT","text":"前组筛窦（右）"},{"no":"R3","side":"RIGHT","text":"后组筛窦（右）"},
{"no":"R4","side":"RIGHT","text":"蝶窦（右）"},{"no":"R5","side":"RIGHT","text":"额窦（右）"},{"no":"R6","side":"RIGHT","text":"窦口鼻道复合体（右）"}
],
"itemOptions":[{"value":0,"label":"无"},{"value":1,"label":"轻"},{"value":2,"label":"重"}]}'::jsonb, 'system', 0
WHERE NOT EXISTS (SELECT 1 FROM cdr.c_scale_definition WHERE scale_code='LUND_MACKAY' AND version=1);

-- 嗅觉主观分级（0-3）
INSERT INTO cdr.c_scale_definition (scale_code, name, version, status, definition, created_by, org_id)
SELECT 'OLFACTION', '嗅觉主观分级', 1, 'ACTIVE',
'{"type":"LIKERT_ITEMS","maxScore":3,"mcid":null,
"interpretation":[{"min":0,"max":0,"label":"正常"},{"min":1,"max":3,"label":"减退"}],
"items":[{"no":"1","text":"当前嗅觉状况"}],
"itemOptions":[{"value":0,"label":"正常"},{"value":1,"label":"轻度减退"},{"value":2,"label":"中度减退"},{"value":3,"label":"完全丧失"}]}'::jsonb, 'system', 0
WHERE NOT EXISTS (SELECT 1 FROM cdr.c_scale_definition WHERE scale_code='OLFACTION' AND version=1);

-- CRS 随访辅助采集（业务数据绑定演示：items[].binding）
INSERT INTO cdr.c_scale_definition (scale_code, name, version, status, definition, created_by, org_id)
SELECT 'CRS_AUX', 'CRS 随访辅助采集（业务数据带入）', 1, 'ACTIVE',
'{"type":"MIXED","maxScore":0,"mcid":null,"interpretation":[],
"items":[
{"no":"1","type":"NUMBER","text":"血清总IgE（自动带入最近一次）","max":5000,
 "binding":{"domain":"LAB","field":"TIGE","mode":"AUTO_READONLY","windowDays":180}},
{"no":"2","type":"NUMBER","text":"嗜酸性粒细胞比例（自动带入，可复核修正）","max":100,
 "binding":{"domain":"LAB","field":"EOS_PCT","mode":"AUTO_EDITABLE","windowDays":180}},
{"no":"3","type":"NUMBER","text":"体重（自动带入，现场复测后可改）","max":300,
 "binding":{"domain":"VITAL","field":"WEIGHT","mode":"AUTO_EDITABLE","windowDays":30}},
{"no":"4","type":"CHECKBOX","text":"当前在用药物（从用药记录枚举，请核对勾选）","options":[],
 "binding":{"domain":"MEDICATION","field":"ACTIVE_MEDS","mode":"OPTIONS"}},
{"no":"5","type":"INPUT","text":"补充说明","required":false}
]}'::jsonb, 'system', 0
WHERE NOT EXISTS (SELECT 1 FROM cdr.c_scale_definition WHERE scale_code='CRS_AUX' AND version=1);

-- ============================================================
-- J32 疾病模板种子（s_disease_template，字段名用 icd_code/icd_name）
-- ============================================================
INSERT INTO system.s_disease_template (disease_name, icd_codes, description, created_by, org_id)
SELECT '慢性鼻窦炎', ARRAY['J32'], '慢性鼻窦炎（CRS）专病队列模板：J32 确诊且含"鼻窦炎"诊断', 'system', 0
WHERE NOT EXISTS (SELECT 1 FROM system.s_disease_template WHERE disease_name = '慢性鼻窦炎');

-- ============================================================
-- J32 队列 + CRS 随访方案种子（队列不存在时创建，方案绑定后发布）
-- ============================================================
DO $$
DECLARE
    v_cohort_id BIGINT;
BEGIN
    SELECT id INTO v_cohort_id FROM cdr.c_disease_cohort WHERE name = '慢性鼻窦炎' AND is_deleted = false LIMIT 1;
    IF v_cohort_id IS NULL THEN
        INSERT INTO cdr.c_disease_cohort (name, description, inclusion_rules, patient_count, auto_sync, status, created_by, org_id)
        VALUES ('慢性鼻窦炎', '慢性鼻窦炎（CRS）专病队列（J32 试点）',
                '{"groupLogic":"AND","groups":[{"domain":"DIAGNOSIS","logic":"AND","conditions":[{"field":"icd_code","operator":"LIKE","value":"J32%"}]}]}'::jsonb,
                0, true, 'ACTIVE', 'system', 0)
        RETURNING id INTO v_cohort_id;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM cdr.c_followup_protocol WHERE cohort_id = v_cohort_id AND version = 1) THEN
        INSERT INTO cdr.c_followup_protocol (cohort_id, name, version, status, stages, created_by, org_id)
        VALUES (v_cohort_id, 'CRS 慢性鼻窦炎随访方案', 1, 'PUBLISHED',
                '[
                  {"stageCode":"BASELINE","name":"基线","offsetDays":0,
                   "requiredScales":["SNOT22","LUND_KENNEDY","OLFACTION"],"optionalScales":["LUND_MACKAY","CRS_AUX"],"note":"建档当日完成"},
                  {"stageCode":"M3","name":"3个月随访","offsetDays":90,
                   "requiredScales":["SNOT22"],"optionalScales":[],"note":""},
                  {"stageCode":"M6","name":"6个月随访","offsetDays":180,
                   "requiredScales":["SNOT22","LUND_KENNEDY"],"optionalScales":[],"note":""},
                  {"stageCode":"M12","name":"12个月随访","offsetDays":365,
                   "requiredScales":["SNOT22"],"optionalScales":["LUND_MACKAY"],"note":"复查CT可选"}
                ]'::jsonb, 'system', 0);
    END IF;
END $$;

-- ============================================================
-- 权限码种子（沿用 17-permission-system.sql 结构）
-- ============================================================
INSERT INTO system.s_permission (permission_code, permission_name, resource_type, resource_key, action, sort_order, created_by, org_id) VALUES
('disease:followup:manage','随访管理（方案/建档/状态/看板）','MENU','/data/cdr/disease/followup','UPDATE',16,'system',0),
('disease:followup:work','随访执行（工作台/填表/提交）','BUTTON','/data/cdr/disease/followup/work','EXECUTE',17,'system',0),
('disease:scale:manage','量表定义管理','MENU','/data/cdr/scales','UPDATE',18,'system',0)
ON CONFLICT DO NOTHING;

-- doctor: manage + work；nurse: work；data_admin: scale:manage；admin: 三码全量
INSERT INTO system.s_role_permission (role_id, permission_id, org_id)
SELECT r.id, p.id, 0
FROM system.s_role r
JOIN system.s_permission p ON p.permission_code IN ('disease:followup:manage','disease:followup:work','disease:scale:manage')
WHERE r.role_code = 'admin'
  AND NOT EXISTS (SELECT 1 FROM system.s_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO system.s_role_permission (role_id, permission_id, org_id)
SELECT r.id, p.id, 0
FROM system.s_role r
JOIN system.s_permission p ON p.permission_code IN ('disease:followup:manage','disease:followup:work')
WHERE r.role_code = 'doctor'
  AND NOT EXISTS (SELECT 1 FROM system.s_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO system.s_role_permission (role_id, permission_id, org_id)
SELECT r.id, p.id, 0
FROM system.s_role r
JOIN system.s_permission p ON p.permission_code = 'disease:followup:work'
WHERE r.role_code = 'nurse'
  AND NOT EXISTS (SELECT 1 FROM system.s_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO system.s_role_permission (role_id, permission_id, org_id)
SELECT r.id, p.id, 0
FROM system.s_role r
JOIN system.s_permission p ON p.permission_code = 'disease:scale:manage'
WHERE r.role_code = 'data_admin'
  AND NOT EXISTS (SELECT 1 FROM system.s_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
