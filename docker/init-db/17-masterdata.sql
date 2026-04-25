-- ==================== 主数据管理: code_system + concept ====================

CREATE SCHEMA IF NOT EXISTS masterdata;

COMMENT ON SCHEMA masterdata IS '主数据管理（编码体系/标准概念/映射）';

GRANT USAGE ON SCHEMA masterdata TO maidc;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA masterdata TO maidc;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA masterdata TO maidc;
ALTER DEFAULT PRIVILEGES IN SCHEMA masterdata GRANT ALL PRIVILEGES ON TABLES TO maidc;
ALTER DEFAULT PRIVILEGES IN SCHEMA masterdata GRANT ALL PRIVILEGES ON SEQUENCES TO maidc;

-- m_code_system: 编码体系
CREATE TABLE IF NOT EXISTS masterdata.m_code_system (
    id                BIGSERIAL    PRIMARY KEY,
    code              VARCHAR(32)  NOT NULL,
    name              VARCHAR(128) NOT NULL,
    version           VARCHAR(32),
    description       TEXT,
    hierarchy_support BOOLEAN      NOT NULL DEFAULT FALSE,
    status            VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_by        VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by        VARCHAR(64),
    updated_at        TIMESTAMP,
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_code_system_code UNIQUE (code)
);
COMMENT ON TABLE masterdata.m_code_system IS '编码体系（ICD-10/LOINC/SNOMED CT/ATC等）';

-- m_concept: 标准概念
CREATE TABLE IF NOT EXISTS masterdata.m_concept (
    id                BIGSERIAL    PRIMARY KEY,
    concept_code      VARCHAR(64)  NOT NULL,
    code_system_id    BIGINT       NOT NULL,
    name              VARCHAR(512) NOT NULL,
    name_en           VARCHAR(512),
    domain            VARCHAR(64),
    standard_class    VARCHAR(64),
    properties        JSONB,
    status            VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    valid_from        DATE,
    valid_to          DATE,
    created_by        VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by        VARCHAR(64),
    updated_at        TIMESTAMP,
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_concept_code_system UNIQUE (concept_code, code_system_id)
);
COMMENT ON TABLE masterdata.m_concept IS '标准概念（编码体系下的具体术语/编码条目）';

-- Indexes
CREATE INDEX idx_concept_system_code ON masterdata.m_concept(code_system_id, concept_code);
CREATE INDEX idx_concept_domain ON masterdata.m_concept(domain) WHERE status = 'ACTIVE' AND is_deleted = false;
CREATE INDEX idx_concept_properties ON masterdata.m_concept USING gin(properties);

-- 3. 概念关系
CREATE TABLE masterdata.m_concept_relationship (
    id                  BIGSERIAL    PRIMARY KEY,
    concept_id_1        BIGINT       NOT NULL,
    concept_id_2        BIGINT       NOT NULL,
    relationship_type   VARCHAR(64)  NOT NULL,
    is_hierarchical     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by          VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    is_deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id              BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_concept_rel UNIQUE (concept_id_1, concept_id_2, relationship_type)
);
COMMENT ON TABLE masterdata.m_concept_relationship IS '概念关系';

-- 4. 祖先闭包
CREATE TABLE masterdata.m_concept_ancestor (
    id                          BIGSERIAL PRIMARY KEY,
    ancestor_concept_id         BIGINT    NOT NULL,
    descendant_concept_id       BIGINT    NOT NULL,
    min_levels_of_separation    INT       NOT NULL DEFAULT 0,
    max_levels_of_separation    INT       NOT NULL DEFAULT 0
);
COMMENT ON TABLE masterdata.m_concept_ancestor IS '概念祖先闭包表';

-- 5. 概念同义词
CREATE TABLE masterdata.m_concept_synonym (
    id              BIGSERIAL    PRIMARY KEY,
    concept_id      BIGINT       NOT NULL,
    synonym         VARCHAR(512) NOT NULL,
    language_code   VARCHAR(8)   NOT NULL DEFAULT 'zh',
    is_preferred    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by      VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id          BIGINT       NOT NULL DEFAULT 0
);
COMMENT ON TABLE masterdata.m_concept_synonym IS '概念同义词';

CREATE INDEX idx_rel_type ON masterdata.m_concept_relationship(concept_id_1, relationship_type);
CREATE INDEX idx_rel_reverse ON masterdata.m_concept_relationship(concept_id_2, relationship_type);
CREATE INDEX idx_ancestor_desc ON masterdata.m_concept_ancestor(descendant_concept_id);
CREATE INDEX idx_ancestor_asc ON masterdata.m_concept_ancestor(ancestor_concept_id);
CREATE INDEX idx_synonym_concept ON masterdata.m_concept_synonym(concept_id);

-- 6. 参考范围
CREATE TABLE masterdata.m_reference_range (
    id              BIGSERIAL    PRIMARY KEY,
    concept_id      BIGINT       NOT NULL,
    gender          VARCHAR(8)   NOT NULL DEFAULT 'ALL',
    age_min         DECIMAL,
    age_max         DECIMAL,
    range_low       DECIMAL,
    range_high      DECIMAL,
    unit            VARCHAR(32),
    critical_low    DECIMAL,
    critical_high   DECIMAL,
    source          VARCHAR(128),
    created_by      VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id          BIGINT       NOT NULL DEFAULT 0
);
COMMENT ON TABLE masterdata.m_reference_range IS '参考范围';

-- 7. 药物相互作用
CREATE TABLE masterdata.m_drug_interaction (
    id                    BIGSERIAL    PRIMARY KEY,
    drug_concept_id_1     BIGINT       NOT NULL,
    drug_concept_id_2     BIGINT       NOT NULL,
    severity              VARCHAR(16)  NOT NULL,
    interaction_type      VARCHAR(32),
    description           TEXT,
    evidence_level        VARCHAR(16),
    clinical_action       TEXT,
    created_by            VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by            VARCHAR(64),
    updated_at            TIMESTAMP,
    is_deleted            BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id                BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_drug_pair UNIQUE (drug_concept_id_1, drug_concept_id_2)
);
COMMENT ON TABLE masterdata.m_drug_interaction IS '药物相互作用';

CREATE INDEX idx_refrange_concept ON masterdata.m_reference_range(concept_id, gender);
CREATE INDEX idx_drug_int_pair ON masterdata.m_drug_interaction(drug_concept_id_1, drug_concept_id_2);
CREATE INDEX idx_drug_int_reverse ON masterdata.m_drug_interaction(drug_concept_id_2, drug_concept_id_1);

-- 8. 医疗机构注册
CREATE TABLE masterdata.m_institution (
    id              BIGSERIAL    PRIMARY KEY,
    inst_code       VARCHAR(32)  NOT NULL,
    name            VARCHAR(128) NOT NULL,
    short_name      VARCHAR(64),
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_by      VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_inst_code UNIQUE (inst_code)
);
COMMENT ON TABLE masterdata.m_institution IS '医疗机构注册';

-- 9. 院内本地编码
CREATE TABLE masterdata.m_local_concept (
    id                    BIGSERIAL    PRIMARY KEY,
    institution_id        BIGINT       NOT NULL,
    code_system_id        BIGINT       NOT NULL,
    local_code            VARCHAR(64)  NOT NULL,
    local_name            VARCHAR(512) NOT NULL,
    standard_concept_id   BIGINT,
    mapping_confidence    DECIMAL(3,2),
    mapping_status        VARCHAR(16)  NOT NULL DEFAULT 'UNMAPPED',
    mapped_by             VARCHAR(64),
    mapped_at             TIMESTAMP,
    created_by            VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by            VARCHAR(64),
    updated_at            TIMESTAMP,
    is_deleted            BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id                BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_local_concept UNIQUE (institution_id, code_system_id, local_code)
);
COMMENT ON TABLE masterdata.m_local_concept IS '院内本地编码映射';

CREATE INDEX idx_local_inst ON masterdata.m_local_concept(institution_id, code_system_id);
CREATE INDEX idx_local_translate ON masterdata.m_local_concept(institution_id, code_system_id, local_code);
CREATE INDEX idx_local_status ON masterdata.m_local_concept(mapping_status);
CREATE INDEX idx_local_standard ON masterdata.m_local_concept(standard_concept_id);

-- 导入任务
CREATE TABLE masterdata.m_import_task (
    id              BIGSERIAL    PRIMARY KEY,
    code_system_id  BIGINT       NOT NULL,
    file_name       VARCHAR(256) NOT NULL,
    file_path       VARCHAR(512),
    total_rows      INT          DEFAULT 0,
    processed_rows  INT          DEFAULT 0,
    failed_rows     INT          DEFAULT 0,
    status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    error_message   TEXT,
    created_by      VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id          BIGINT       NOT NULL DEFAULT 0
);
COMMENT ON TABLE masterdata.m_import_task IS '主数据导入任务';

-- ==================== 种子数据: 5 条编码体系 ====================

INSERT INTO masterdata.m_code_system (code, name, version, description, hierarchy_support, status) VALUES
('ICD10', 'ICD-10 国际疾病分类', '2024', 'WHO国际疾病分类第10次修订本中文版', true, 'ACTIVE'),
('ICD9CM', 'ICD-9-CM-3 手术编码', '2011', '国际疾病分类临床修订本第3卷手术与操作', true, 'ACTIVE'),
('LOINC', 'LOINC 检验观察编码', '2.78', 'Logical Observation Identifiers Names and Codes', false, 'ACTIVE'),
('SNOMEDCT', 'SNOMED CT 临床术语', '2024-01', 'Systematized Nomenclature of Medicine Clinical Terms', true, 'ACTIVE'),
('ATC', 'ATC 药品分类', '2024', 'Anatomical Therapeutic Chemical classification', true, 'ACTIVE');
