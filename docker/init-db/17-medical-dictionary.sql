-- ====================================================================
-- Medical Dictionary Tables (药品、诊断、收费、检验、检查字典)
-- Schema: masterdata
-- Version: 17
-- Date: 2026-05-05 (2026-09-18 迁入生效 init-db 目录并幂等化)
--
-- 说明：存量开发库的这批表此前由 Hibernate ddl-auto:update 隐式创建，
--       本文件服务全新部署（init-db 仅在数据库首次初始化时执行）。
--       幂等性：CREATE TABLE IF NOT EXISTS / CREATE INDEX IF NOT EXISTS /
--       种子数据 ON CONFLICT DO NOTHING。
-- ====================================================================

-- ==================== 药品分类（树形结构） ====================
CREATE TABLE IF NOT EXISTS masterdata.d_drug_category (
    id              BIGSERIAL    PRIMARY KEY,
    code            VARCHAR(64)  NOT NULL,
    name            VARCHAR(128) NOT NULL,
    name_en         VARCHAR(128),
    parent_id       BIGINT       REFERENCES masterdata.d_drug_category(id),
    level           INT          NOT NULL DEFAULT 1,
    sort_order      INT          NOT NULL DEFAULT 0,
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    remark          TEXT,
    -- 审计字段
    created_by      VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id          BIGINT       NOT NULL DEFAULT 0
);

COMMENT ON TABLE masterdata.d_drug_category IS '药品分类字典（树形结构）';

-- ==================== 药品字典 ====================
CREATE TABLE IF NOT EXISTS masterdata.d_drug (
    id                  BIGSERIAL    PRIMARY KEY,
    drug_code           VARCHAR(64)  NOT NULL,
    name                VARCHAR(256) NOT NULL,
    trade_name          VARCHAR(256),
    name_en             VARCHAR(256),
    name_pinyin         VARCHAR(512),

    -- 分类信息
    category_id         BIGINT       REFERENCES masterdata.d_drug_category(id),
    category_name       VARCHAR(128),
    atc_code            VARCHAR(16),

    -- 规格信息
    dosage_form         VARCHAR(64),
    specification       VARCHAR(128),
    unit                VARCHAR(32),
    pack_unit           VARCHAR(32),
    pack_quantity       DECIMAL(10,2),

    -- 价格信息
    price               DECIMAL(12,2),
    price_unit          VARCHAR(32),

    -- 医保信息
    insurance_code      VARCHAR(64),
    insurance_type      VARCHAR(16),  -- 甲类/乙类/丙类/自费
    insurance_ratio     DECIMAL(5,4),

    -- 厂家信息
    manufacturer        VARCHAR(256),
    manufacturer_code   VARCHAR(64),

    -- 多编码映射
    national_code       VARCHAR(64),
    hospital_code       VARCHAR(64),
    his_code            VARCHAR(64),

    -- 状态与扩展
    status              VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    is_otc              BOOLEAN      DEFAULT FALSE,
    is_prescription     BOOLEAN      DEFAULT TRUE,
    is_controlled       VARCHAR(16)  DEFAULT 'NORMAL',  -- NORMAL/精神/麻醉
    properties          JSONB,

    -- 有效期
    valid_from          DATE,
    valid_to            DATE,
    remark              TEXT,

    -- 审计字段
    created_by          VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id              BIGINT       NOT NULL DEFAULT 0
);

COMMENT ON TABLE masterdata.d_drug IS '药品字典';

-- 索引
CREATE INDEX IF NOT EXISTS idx_drug_category ON masterdata.d_drug(category_id) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_drug_atc ON masterdata.d_drug(atc_code) WHERE atc_code IS NOT NULL AND is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_drug_insurance ON masterdata.d_drug(insurance_code) WHERE insurance_code IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_drug_status ON masterdata.d_drug(status) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_drug_name ON masterdata.d_drug USING gin(to_tsvector('simple', name));

-- ==================== 诊断字典 ====================
CREATE TABLE IF NOT EXISTS masterdata.d_diagnosis (
    id                  BIGSERIAL    PRIMARY KEY,
    diagnosis_code      VARCHAR(32)  NOT NULL,
    name                VARCHAR(512) NOT NULL,
    name_en             VARCHAR(512),
    name_pinyin         VARCHAR(1024),

    -- ICD编码映射
    icd10_code          VARCHAR(16),
    icd10_name          VARCHAR(512),
    icd9cm_code         VARCHAR(16),

    -- 层级信息（ICD-10结构）
    chapter_code        VARCHAR(16),   -- 章节（如 A00-B99）
    chapter_name        VARCHAR(128),
    category_code       VARCHAR(16),   -- 类目（如 A00）
    parent_id           BIGINT         REFERENCES masterdata.d_diagnosis(id),
    level               INT            DEFAULT 1,

    -- 分类标记
    is_main             BOOLEAN        DEFAULT TRUE,
    diagnosis_type      VARCHAR(32),   -- 主诊断/次诊断/并发症
    gender_restriction  VARCHAR(8),    -- 男/女/ALL

    -- 科室关联
    related_dept_codes  TEXT[],

    -- 状态与扩展
    status              VARCHAR(16)    NOT NULL DEFAULT 'ACTIVE',
    severity_level      VARCHAR(16),   -- 轻/中/重
    properties          JSONB,

    valid_from          DATE,
    valid_to            DATE,
    remark              TEXT,

    -- 审计字段
    created_by          VARCHAR(64)    NOT NULL DEFAULT 'system',
    created_at          TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN        NOT NULL DEFAULT FALSE,
    org_id              BIGINT         NOT NULL DEFAULT 0
);

COMMENT ON TABLE masterdata.d_diagnosis IS '诊断字典（支持ICD-10层级）';

-- 索引
CREATE INDEX IF NOT EXISTS idx_diagnosis_icd10 ON masterdata.d_diagnosis(icd10_code) WHERE icd10_code IS NOT NULL AND is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_diagnosis_chapter ON masterdata.d_diagnosis(chapter_code);
CREATE INDEX IF NOT EXISTS idx_diagnosis_parent ON masterdata.d_diagnosis(parent_id);
CREATE INDEX IF NOT EXISTS idx_diagnosis_name ON masterdata.d_diagnosis USING gin(to_tsvector('simple', name));

-- ==================== 收费项目字典 ====================
CREATE TABLE IF NOT EXISTS masterdata.d_fee_item (
    id                  BIGSERIAL    PRIMARY KEY,
    fee_code            VARCHAR(64)  NOT NULL,
    name                VARCHAR(256) NOT NULL,
    name_en             VARCHAR(256),
    name_pinyin         VARCHAR(512),

    -- 分类信息
    fee_category        VARCHAR(32)  NOT NULL,  -- 药品/诊疗/检验/检查/材料/服务
    fee_type            VARCHAR(32),           -- 甲类/乙类/丙类/自费

    -- 价格信息
    price               DECIMAL(12,2),
    unit                VARCHAR(32),
    execution_unit      VARCHAR(32),

    -- 医保信息
    insurance_code      VARCHAR(64),
    insurance_type      VARCHAR(16),
    insurance_ratio     DECIMAL(5,4),

    -- 科室关联
    executing_dept      VARCHAR(64),
    executing_dept_name VARCHAR(128),

    -- 关联编码
    related_concept_id  BIGINT,

    -- 状态与扩展
    status              VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    is_batch            BOOLEAN      DEFAULT FALSE,
    properties          JSONB,

    valid_from          DATE,
    valid_to            DATE,
    remark              TEXT,

    -- 审计字段
    created_by          VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id              BIGINT       NOT NULL DEFAULT 0
);

COMMENT ON TABLE masterdata.d_fee_item IS '收费项目字典';

-- 索引
CREATE INDEX IF NOT EXISTS idx_fee_category ON masterdata.d_fee_item(fee_category) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_fee_insurance ON masterdata.d_fee_item(insurance_code) WHERE insurance_code IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_fee_name ON masterdata.d_fee_item USING gin(to_tsvector('simple', name));

-- ==================== 检验项目字典 ====================
CREATE TABLE IF NOT EXISTS masterdata.d_lab_item (
    id                  BIGSERIAL    PRIMARY KEY,
    lab_code            VARCHAR(64)  NOT NULL,
    name                VARCHAR(256) NOT NULL,
    name_en             VARCHAR(256),
    name_pinyin         VARCHAR(512),

    -- LOINC编码映射
    loinc_code          VARCHAR(16),
    loinc_name          VARCHAR(512),

    -- 检验信息
    specimen_type       VARCHAR(64),
    specimen_type_code  VARCHAR(32),
    method              VARCHAR(128),
    is_panel            BOOLEAN      DEFAULT FALSE,

    -- 结果信息
    result_type         VARCHAR(32),  -- 数值/文本/定性
    unit                VARCHAR(32),

    -- 参考范围（通用）
    ref_range_low       DECIMAL(12,4),
    ref_range_high      DECIMAL(12,4),
    ref_range_text      VARCHAR(256),
    critical_low        DECIMAL(12,4),
    critical_high       DECIMAL(12,4),

    -- 关联信息
    lab_category        VARCHAR(64),
    parent_id           BIGINT        REFERENCES masterdata.d_lab_item(id),
    fee_item_id         BIGINT,

    -- 状态与扩展
    status              VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE',
    properties          JSONB,

    valid_from          DATE,
    valid_to            DATE,
    remark              TEXT,

    -- 审计字段
    created_by          VARCHAR(64)   NOT NULL DEFAULT 'system',
    created_at          TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN       NOT NULL DEFAULT FALSE,
    org_id              BIGINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE masterdata.d_lab_item IS '检验项目字典';

-- 索引
CREATE INDEX IF NOT EXISTS idx_lab_loinc ON masterdata.d_lab_item(loinc_code) WHERE loinc_code IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_lab_specimen ON masterdata.d_lab_item(specimen_type_code);
CREATE INDEX IF NOT EXISTS idx_lab_parent ON masterdata.d_lab_item(parent_id);
CREATE INDEX IF NOT EXISTS idx_lab_name ON masterdata.d_lab_item USING gin(to_tsvector('simple', name));

-- ==================== 检验参考范围（按人群细分） ====================
CREATE TABLE IF NOT EXISTS masterdata.d_lab_reference (
    id              BIGSERIAL    PRIMARY KEY,
    lab_item_id     BIGINT       NOT NULL REFERENCES masterdata.d_lab_item(id),
    gender          VARCHAR(8)   NOT NULL DEFAULT 'ALL',  -- 男/女/ALL
    age_min         DECIMAL(8,2),
    age_max         DECIMAL(8,2),
    age_unit        VARCHAR(8)   DEFAULT 'YEAR',
    ref_low         DECIMAL(12,4),
    ref_high        DECIMAL(12,4),
    ref_text        VARCHAR(256),
    critical_low    DECIMAL(12,4),
    critical_high   DECIMAL(12,4),
    source          VARCHAR(128),
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    remark          TEXT,

    -- 审计字段
    created_by      VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id          BIGINT       NOT NULL DEFAULT 0
);

COMMENT ON TABLE masterdata.d_lab_reference IS '检验参考范围（按人群细分）';

CREATE INDEX IF NOT EXISTS idx_lab_reference_item ON masterdata.d_lab_reference(lab_item_id);

-- ==================== 检查项目字典 ====================
CREATE TABLE IF NOT EXISTS masterdata.d_exam_item (
    id                  BIGSERIAL    PRIMARY KEY,
    exam_code           VARCHAR(64)  NOT NULL,
    name                VARCHAR(256) NOT NULL,
    name_en             VARCHAR(256),
    name_pinyin         VARCHAR(512),

    -- 检查信息
    exam_type           VARCHAR(32)  NOT NULL,  -- CT/MRI/X线/超声/心电/内镜
    body_site           VARCHAR(128),
    body_site_code      VARCHAR(32),
    method              VARCHAR(128),
    contrast_type       VARCHAR(32),  -- 无/增强/造影

    -- 设备信息
    equipment_type      VARCHAR(64),
    modality            VARCHAR(32),

    -- 关联信息
    exam_category       VARCHAR(64),
    fee_item_id         BIGINT,

    -- 状态与扩展
    status              VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    properties          JSONB,

    valid_from          DATE,
    valid_to            DATE,
    remark              TEXT,

    -- 审计字段
    created_by          VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id              BIGINT       NOT NULL DEFAULT 0
);

COMMENT ON TABLE masterdata.d_exam_item IS '检查项目字典';

-- 索引
CREATE INDEX IF NOT EXISTS idx_exam_type ON masterdata.d_exam_item(exam_type) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_exam_body_site ON masterdata.d_exam_item(body_site_code);
CREATE INDEX IF NOT EXISTS idx_exam_name ON masterdata.d_exam_item USING gin(to_tsvector('simple', name));

-- 软删友好：业务编码唯一约束改为局部唯一索引（仅约束未删行，逻辑删除后编码可复用）
CREATE UNIQUE INDEX IF NOT EXISTS uk_drug_category_code ON masterdata.d_drug_category (code) WHERE is_deleted = false;
CREATE UNIQUE INDEX IF NOT EXISTS uk_drug_code ON masterdata.d_drug (drug_code) WHERE is_deleted = false;
CREATE UNIQUE INDEX IF NOT EXISTS uk_diagnosis_code ON masterdata.d_diagnosis (diagnosis_code) WHERE is_deleted = false;
CREATE UNIQUE INDEX IF NOT EXISTS uk_fee_item_code ON masterdata.d_fee_item (fee_code) WHERE is_deleted = false;
CREATE UNIQUE INDEX IF NOT EXISTS uk_lab_item_code ON masterdata.d_lab_item (lab_code) WHERE is_deleted = false;
CREATE UNIQUE INDEX IF NOT EXISTS uk_exam_item_code ON masterdata.d_exam_item (exam_code) WHERE is_deleted = false;

-- ==================== 初始化数据 ====================

-- 药品分类示例数据
INSERT INTO masterdata.d_drug_category (code, name, name_en, parent_id, level, sort_order) VALUES
('ATC-A', '消化系统及代谢药', 'ALIMENTARY TRACT AND METABOLISM', NULL, 1, 1),
('ATC-B', '血液系统药', 'BLOOD AND BLOOD FORMING ORGANS', NULL, 1, 2),
('ATC-C', '心血管系统药', 'CARDIOVASCULAR SYSTEM', NULL, 1, 3),
('ATC-D', '皮肤病用药', 'DERMATOLOGICALS', NULL, 1, 4),
('ATC-G', '泌尿生殖系统药', 'GENITO URINARY SYSTEM', NULL, 1, 5),
('ATC-H', '激素类药', 'SYSTEMIC HORMONAL PREPARATIONS', NULL, 1, 6),
('ATC-J', '抗感染药', 'ANTIINFECTIVES FOR SYSTEMIC USE', NULL, 1, 7),
('ATC-L', '抗肿瘤药', 'ANTINEOPLASTIC AND IMMUNOMODULATING', NULL, 1, 8),
('ATC-M', '肌肉骨骼系统药', 'MUSCULO-SKELETAL SYSTEM', NULL, 1, 9),
('ATC-N', '神经系统药', 'NERVOUS SYSTEM', NULL, 1, 10),
('ATC-P', '抗寄生虫药', 'ANTIPARASITIC PRODUCTS', NULL, 1, 11),
('ATC-R', '呼吸系统药', 'RESPIRATORY SYSTEM', NULL, 1, 12),
('ATC-S', '感觉器官药', 'SENSORY ORGANS', NULL, 1, 13),
('ATC-V', '其他药品', 'VARIOUS', NULL, 1, 14)
ON CONFLICT (code) DO NOTHING;

-- ICD-10章节示例数据
INSERT INTO masterdata.d_diagnosis (diagnosis_code, name, icd10_code, chapter_code, chapter_name, level, is_main) VALUES
('ICD-A', '传染病和寄生虫病', 'A00-B99', 'A00-B99', '第一章 传染病和寄生虫病', 1, TRUE),
('ICD-B', '传染病和寄生虫病（续）', 'A00-B99', 'A00-B99', '第一章 传染病和寄生虫病', 1, TRUE),
('ICD-C', '肿瘤', 'C00-D48', 'C00-D48', '第二章 肿瘤', 1, TRUE),
('ICD-D', '血液和造血器官疾病', 'D50-D89', 'D50-D89', '第三章 血液和造血器官疾病', 1, TRUE),
('ICD-E', '内分泌、营养和代谢疾病', 'E00-E90', 'E00-E90', '第四章 内分泌、营养和代谢疾病', 1, TRUE),
('ICD-F', '精神和行为障碍', 'F00-F99', 'F00-F99', '第五章 精神和行为障碍', 1, TRUE),
('ICD-G', '神经系统疾病', 'G00-G99', 'G00-G99', '第六章 神经系统疾病', 1, TRUE),
('ICD-H', '眼和附器疾病', 'H00-H59', 'H00-H59', '第七章 眼和附器疾病', 1, TRUE),
('ICD-I', '循环系统疾病', 'I00-I99', 'I00-I99', '第九章 循环系统疾病', 1, TRUE),
('ICD-J', '呼吸系统疾病', 'J00-J99', 'J00-J99', '第十章 呼吸系统疾病', 1, TRUE),
('ICD-K', '消化系统疾病', 'K00-K93', 'K00-K93', '第十一章 消化系统疾病', 1, TRUE),
('ICD-L', '皮肤病和皮下组织疾病', 'L00-L99', 'L00-L99', '第十二章 皮肤病和皮下组织疾病', 1, TRUE),
('ICD-M', '肌肉骨骼系统和结缔组织疾病', 'M00-M99', 'M00-M99', '第十三章 肌肉骨骼系统和结缔组织疾病', 1, TRUE),
('ICD-N', '泌尿生殖系统疾病', 'N00-N99', 'N00-N99', '第十四章 泌尿生殖系统疾病', 1, TRUE),
('ICD-O', '妊娠、分娩和产褥期', 'O00-O99', 'O00-O99', '第十五章 妊娠、分娩和产褥期', 1, TRUE),
('ICD-P', '起源于围生期的某些情况', 'P00-P96', 'P00-P96', '第十六章 起源于围生期的某些情况', 1, TRUE),
('ICD-Q', '先天性畸形、变形和染色体异常', 'Q00-Q99', 'Q00-Q99', '第十七章 先天性畸形、变形和染色体异常', 1, TRUE),
('ICD-R', '症状、体征和异常临床检验结果', 'R00-R99', 'R00-R99', '第十八章 症状、体征和异常临床检验结果', 1, TRUE),
('ICD-S', '损伤、中毒和外因的某些其他后果', 'S00-T98', 'S00-T98', '第十九章 损伤、中毒和外因的某些其他后果', 1, TRUE),
('ICD-Z', '影响健康状态和与保健机构接触的因素', 'Z00-Z99', 'Z00-Z99', '第二十一章 影响健康状态和与保健机构接触的因素', 1, TRUE)
ON CONFLICT (diagnosis_code) DO NOTHING;

-- 收费类别示例数据
INSERT INTO masterdata.d_fee_item (fee_code, name, fee_category, fee_type, price, unit) VALUES
('FEE-TEST-001', '测试收费项目', '诊疗', '甲类', 100.00, '次')
ON CONFLICT (fee_code) DO NOTHING;

-- 检验类别示例数据
INSERT INTO masterdata.d_lab_item (lab_code, name, specimen_type, result_type, unit, lab_category) VALUES
('LAB-TEST-001', '测试检验项目', '血液', '数值', 'mg/L', '生化检验')
ON CONFLICT (lab_code) DO NOTHING;

-- 检查类别示例数据
INSERT INTO masterdata.d_exam_item (exam_code, name, exam_type, body_site, exam_category) VALUES
('EXAM-TEST-001', '测试检查项目', 'CT', '胸部', '影像检查')
ON CONFLICT (exam_code) DO NOTHING;
