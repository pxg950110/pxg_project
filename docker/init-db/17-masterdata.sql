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
    category          VARCHAR(32)  DEFAULT 'STANDARD',
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

-- ==================== 知识体系管理 ====================

-- 知识库分类
CREATE TABLE IF NOT EXISTS masterdata.m_knowledge_category (
    id              BIGSERIAL    PRIMARY KEY,
    name            VARCHAR(128) NOT NULL,
    parent_id       BIGINT,
    sort_order      INT          NOT NULL DEFAULT 0,
    created_by      VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id          BIGINT       NOT NULL DEFAULT 0
);
COMMENT ON TABLE masterdata.m_knowledge_category IS '知识库分类（临床指南/文献/共识等）';

-- 知识条目
CREATE TABLE IF NOT EXISTS masterdata.m_knowledge_item (
    id              BIGSERIAL    PRIMARY KEY,
    title           VARCHAR(512) NOT NULL,
    category_id     BIGINT,
    item_type       VARCHAR(32)  NOT NULL DEFAULT 'GUIDELINE',
    summary         TEXT,
    content         TEXT,
    source          VARCHAR(256),
    authors         VARCHAR(512),
    publish_date    DATE,
    tags            JSONB,
    file_url        VARCHAR(512),
    file_name       VARCHAR(256),
    status          VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',
    created_by      VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id          BIGINT       NOT NULL DEFAULT 0
);
COMMENT ON TABLE masterdata.m_knowledge_item IS '知识条目（临床指南/文献/专家共识等）';

-- 知识-概念关联
CREATE TABLE IF NOT EXISTS masterdata.m_knowledge_concept (
    id              BIGSERIAL    PRIMARY KEY,
    knowledge_id    BIGINT       NOT NULL,
    concept_id      BIGINT       NOT NULL,
    relevance       VARCHAR(16)  NOT NULL DEFAULT 'RELATED',
    created_by      VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_knowledge_concept UNIQUE (knowledge_id, concept_id)
);
COMMENT ON TABLE masterdata.m_knowledge_concept IS '知识条目-概念关联';

CREATE INDEX idx_knowledge_category ON masterdata.m_knowledge_item(category_id);
CREATE INDEX idx_knowledge_type ON masterdata.m_knowledge_item(item_type, status);
CREATE INDEX idx_knowledge_tags ON masterdata.m_knowledge_item USING gin(tags);
CREATE INDEX idx_knowledge_title ON masterdata.m_knowledge_item USING gin(to_tsvector('simple', title));
CREATE INDEX idx_kc_knowledge ON masterdata.m_knowledge_concept(knowledge_id);
CREATE INDEX idx_kc_concept ON masterdata.m_knowledge_concept(concept_id);

INSERT INTO masterdata.m_code_system (code, name, version, description, hierarchy_support, category, status) VALUES
('ICD10', 'ICD-10 国际疾病分类', '2024', 'WHO国际疾病分类第10次修订本中文版', true, 'STANDARD', 'ACTIVE'),
('ICD9CM', 'ICD-9-CM-3 手术编码', '2011', '国际疾病分类临床修订本第3卷手术与操作', true, 'STANDARD', 'ACTIVE'),
('LOINC', 'LOINC 检验观察编码', '2.78', 'Logical Observation Identifiers Names and Codes', false, 'STANDARD', 'ACTIVE'),
('SNOMEDCT', 'SNOMED CT 临床术语', '2024-01', 'Systematized Nomenclature of Medicine Clinical Terms', true, 'STANDARD', 'ACTIVE'),
('ATC', 'ATC 药品分类', '2024', 'Anatomical Therapeutic Chemical classification', true, 'STANDARD', 'ACTIVE');

-- 10. 术语领域配置
CREATE TABLE IF NOT EXISTS masterdata.m_terminology_domain (
    id              BIGSERIAL    PRIMARY KEY,
    code            VARCHAR(64)  NOT NULL,
    name            VARCHAR(128) NOT NULL,
    name_en         VARCHAR(128),
    description     TEXT,
    icon            VARCHAR(64),
    sort_order      INT          NOT NULL DEFAULT 0,
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_by      VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_terminology_domain_code UNIQUE (code)
);
COMMENT ON TABLE masterdata.m_terminology_domain IS '术语领域配置（诊断/手术/检验/药品等）';

CREATE INDEX idx_term_domain_status ON masterdata.m_terminology_domain(status) WHERE is_deleted = false;

INSERT INTO masterdata.m_terminology_domain (code, name, name_en, description, icon, sort_order, status) VALUES
('Diagnosis',   '诊断',   'Diagnosis',   '疾病诊断领域（ICD编码等）',           'HeartOutlined',      1, 'ACTIVE'),
('Procedure',   '手术操作', 'Procedure',   '手术与操作领域',                      'ScissorOutlined',    2, 'ACTIVE'),
('Laboratory',  '检验',   'Laboratory',  '临床检验领域（LOINC编码等）',          'ExperimentOutlined',  3, 'ACTIVE'),
('Medication',  '药品',   'Medication',  '药品领域（ATC编码等）',               'MedicineBoxOutlined', 4, 'ACTIVE'),
('Observation', '观察',   'Observation', '临床观察领域',                         'EyeOutlined',        5, 'ACTIVE'),
('BodySite',    '身体部位', 'BodySite',    '身体部位领域',                         'UserOutlined',       6, 'ACTIVE'),
('Specimen',    '标本',   'Specimen',    '标本领域',                             'AimOutlined',        7, 'ACTIVE'),
('Other',       '其他',   'Other',       '其他领域',                             'AppstoreOutlined',   8, 'ACTIVE');

-- ==================== 数据元管理 ====================

CREATE TABLE masterdata.m_data_element (
    id                    BIGSERIAL    PRIMARY KEY,
    element_code          VARCHAR(64)  NOT NULL,
    name                  VARCHAR(256) NOT NULL,
    name_en               VARCHAR(256),
    definition            TEXT         NOT NULL,
    object_class_name     VARCHAR(128),
    object_class_id       VARCHAR(64),
    property_name         VARCHAR(128),
    property_id           VARCHAR(64),
    data_type             VARCHAR(32)  NOT NULL,
    representation_class  VARCHAR(32),
    value_domain_name     VARCHAR(128),
    value_domain_id       VARCHAR(64),
    min_length            INT,
    max_length            INT,
    format                VARCHAR(64),
    unit_of_measure       VARCHAR(32),
    category              VARCHAR(64),
    standard_source       VARCHAR(128),
    registration_status   VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',
    version               VARCHAR(16)  NOT NULL DEFAULT '1.0',
    synonyms              TEXT[],
    keywords              TEXT[],
    extra_attrs           JSONB,
    status                VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_by            VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by            VARCHAR(64),
    updated_at            TIMESTAMP,
    is_deleted            BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id                BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_data_element_code UNIQUE (element_code)
);
COMMENT ON TABLE masterdata.m_data_element IS '数据元';
CREATE INDEX idx_de_category ON masterdata.m_data_element(category) WHERE status = 'ACTIVE' AND is_deleted = false;
CREATE INDEX idx_de_status ON masterdata.m_data_element(registration_status) WHERE is_deleted = false;
CREATE INDEX idx_de_name ON masterdata.m_data_element USING gin(to_tsvector('simple', name));

CREATE TABLE masterdata.m_data_element_value (
    id                BIGSERIAL    PRIMARY KEY,
    data_element_id   BIGINT       NOT NULL,
    value_code        VARCHAR(64)  NOT NULL,
    value_meaning     VARCHAR(256) NOT NULL,
    sort_order        INT          NOT NULL DEFAULT 0,
    created_by        VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id            BIGINT       NOT NULL DEFAULT 0
);
COMMENT ON TABLE masterdata.m_data_element_value IS '数据元允许值';
CREATE INDEX idx_dev_element ON masterdata.m_data_element_value(data_element_id);
CREATE UNIQUE INDEX uk_dev_element_code ON masterdata.m_data_element_value(data_element_id, value_code) WHERE is_deleted = false;

CREATE TABLE masterdata.m_data_element_mapping (
    id                BIGSERIAL    PRIMARY KEY,
    data_element_id   BIGINT       NOT NULL,
    schema_name       VARCHAR(64)  NOT NULL,
    table_name        VARCHAR(128) NOT NULL,
    column_name       VARCHAR(128) NOT NULL,
    mapping_type      VARCHAR(16)  NOT NULL DEFAULT 'MANUAL',
    confidence        DECIMAL(3,2),
    mapping_status    VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    transform_rule    TEXT,
    mapped_by         VARCHAR(64),
    mapped_at         TIMESTAMP,
    created_by        VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by        VARCHAR(64),
    updated_at        TIMESTAMP,
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_de_mapping UNIQUE (data_element_id, schema_name, table_name, column_name) WHERE is_deleted = false
);
COMMENT ON TABLE masterdata.m_data_element_mapping IS '数据元字段映射';
CREATE INDEX idx_dem_element ON masterdata.m_data_element_mapping(data_element_id);
CREATE INDEX idx_dem_status ON masterdata.m_data_element_mapping(mapping_status);
CREATE INDEX idx_dem_table ON masterdata.m_data_element_mapping(schema_name, table_name);
