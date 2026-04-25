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

-- ==================== 种子数据: 5 条编码体系 ====================

INSERT INTO masterdata.m_code_system (code, name, version, description, hierarchy_support, status) VALUES
('ICD10', 'ICD-10 国际疾病分类', '2024', 'WHO国际疾病分类第10次修订本中文版', true, 'ACTIVE'),
('ICD9CM', 'ICD-9-CM-3 手术编码', '2011', '国际疾病分类临床修订本第3卷手术与操作', true, 'ACTIVE'),
('LOINC', 'LOINC 检验观察编码', '2.78', 'Logical Observation Identifiers Names and Codes', false, 'ACTIVE'),
('SNOMEDCT', 'SNOMED CT 临床术语', '2024-01', 'Systematized Nomenclature of Medicine Clinical Terms', true, 'ACTIVE'),
('ATC', 'ATC 药品分类', '2024', 'Anatomical Therapeutic Chemical classification', true, 'ACTIVE');
