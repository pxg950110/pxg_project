-- ============================================================
-- 卫生健康信息数据元标准化数据模型
-- 基于 WS/T 303-2023 《卫生健康信息数据元标准化规则》
-- ============================================================

-- ============================================================
-- 1. 概念层 (Concept Layer)
-- ============================================================

-- 1.1 概念域 (Conceptual Domain, CD)
-- 有效的值含义的集合
CREATE TABLE IF NOT EXISTS concept_domain (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,           -- 概念域代码
    name VARCHAR(200) NOT NULL,                  -- 概念域名称
    name_en VARCHAR(200),                        -- 英文名称
    definition TEXT NOT NULL,                    -- 定义
    domain_type VARCHAR(20) NOT NULL DEFAULT 'ENUMERABLE',  -- 类型: ENUMERABLE(可枚举), NON_ENUMERABLE(不可枚举)
    description_rule TEXT,                       -- 不可枚举概念域描述规则
    dimension VARCHAR(100),                      -- 维度(等价计量单位的共同特征)
    parent_id BIGINT REFERENCES concept_domain(id),  -- 父概念域ID(用于概念体系)
    version VARCHAR(20) DEFAULT '1.0',           -- 版本
    status VARCHAR(20) DEFAULT 'DRAFT',          -- 状态: DRAFT, REVIEWED, APPROVED, RETIRED
    registration_authority VARCHAR(100),         -- 注册机构
    created_by VARCHAR(100),                     -- 创建人
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark TEXT
);

COMMENT ON TABLE concept_domain IS '概念域-有效的值含义的集合';
COMMENT ON COLUMN concept_domain.domain_type IS 'ENUMERABLE=可枚举(由值含义列表规定), NON_ENUMERABLE=不可枚举(由描述规定)';
COMMENT ON COLUMN concept_domain.dimension IS '维度-等价计量单位的共同特征,如长度、重量、温度等';

-- 1.2 值含义 (Value Meaning, VM)
-- 一个值的含义或语义内容
CREATE TABLE IF NOT EXISTS value_meaning (
    id BIGSERIAL PRIMARY KEY,
    concept_domain_id BIGINT NOT NULL REFERENCES concept_domain(id),
    code VARCHAR(50) NOT NULL,                   -- 值含义代码
    name VARCHAR(200) NOT NULL,                  -- 值含义名称
    name_en VARCHAR(200),                        -- 英文名称
    definition TEXT,                             -- 定义
    sort_order INT DEFAULT 0,                    -- 排序号
    is_active BOOLEAN DEFAULT TRUE,              -- 是否有效
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(concept_domain_id, code)
);

COMMENT ON TABLE value_meaning IS '值含义-一个值的含义或语义内容';

-- 1.3 数据元概念 (Data Element Concept, DEC)
-- 能以数据元的形式表示的概念，其表述与任何特定表示法无关
CREATE TABLE IF NOT EXISTS data_element_concept (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,            -- 数据元概念代码
    name VARCHAR(200) NOT NULL,                  -- 数据元概念名称
    name_en VARCHAR(200),                        -- 英文名称
    definition TEXT NOT NULL,                    -- 定义

    -- 对象类 (Object Class)
    object_class_code VARCHAR(50),               -- 对象类代码
    object_class_name VARCHAR(200),              -- 对象类名称
    object_class_definition TEXT,                -- 对象类定义

    -- 特性 (Property)
    property_code VARCHAR(50),                   -- 特性代码
    property_name VARCHAR(200),                  -- 特性名称
    property_definition TEXT,                    -- 特性定义

    -- 关联概念域
    concept_domain_id BIGINT REFERENCES concept_domain(id),  -- 关联的概念域

    version VARCHAR(20) DEFAULT '1.0',
    status VARCHAR(20) DEFAULT 'DRAFT',
    registration_authority VARCHAR(100),
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark TEXT
);

COMMENT ON TABLE data_element_concept IS '数据元概念-由对象类和特性组成,与任何特定表示法无关';

-- ============================================================
-- 2. 表示层 (Representation Layer)
-- ============================================================

-- 2.1 值域 (Value Domain, VD)
-- 数据元允许值的集合
CREATE TABLE IF NOT EXISTS value_domain (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,            -- 值域代码
    name VARCHAR(200) NOT NULL,                  -- 值域名称
    name_en VARCHAR(200),                        -- 英文名称
    definition TEXT NOT NULL,                    -- 定义
    domain_type VARCHAR(20) NOT NULL DEFAULT 'ENUMERABLE',  -- 类型: ENUMERABLE, NON_ENUMERABLE
    description TEXT,                            -- 不可枚举值域描述

    -- 数据类型
    data_type VARCHAR(50) NOT NULL,              -- 数据类型: STRING, INTEGER, DECIMAL, DATE, DATETIME, BOOLEAN, CODE
    max_length INT,                              -- 最大长度
    min_length INT,                              -- 最小长度
    format VARCHAR(100),                         -- 表示格式

    -- 计量单位
    unit_of_measure VARCHAR(50),                 -- 计量单位
    unit_name VARCHAR(100),                      -- 计量单位名称

    -- 表示类
    representation_class VARCHAR(50),            -- 表示类: AMOUNT, CODE, COUNT, DATE, TIME, TEXT, NUMBER, etc.

    -- 关联概念域
    concept_domain_id BIGINT NOT NULL REFERENCES concept_domain(id),

    version VARCHAR(20) DEFAULT '1.0',
    status VARCHAR(20) DEFAULT 'DRAFT',
    registration_authority VARCHAR(100),
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark TEXT
);

COMMENT ON TABLE value_domain IS '值域-数据元允许值的集合';
COMMENT ON COLUMN value_domain.domain_type IS 'ENUMERABLE=可枚举(由允许值列表规定), NON_ENUMERABLE=不可枚举(由描述规定)';

-- 2.2 值集/允许值 (Permissible Value, PV)
-- 在一个特定值域中允许的一个值含义的表达
CREATE TABLE IF NOT EXISTS permissible_value (
    id BIGSERIAL PRIMARY KEY,
    value_domain_id BIGINT NOT NULL REFERENCES value_domain(id),
    value VARCHAR(200) NOT NULL,                 -- 值
    value_meaning_id BIGINT REFERENCES value_meaning(id),  -- 关联值含义
    value_meaning_name VARCHAR(200),             -- 值含义名称(冗余存储)
    sort_order INT DEFAULT 0,                    -- 排序号
    is_active BOOLEAN DEFAULT TRUE,              -- 是否有效
    effective_date DATE,                         -- 生效日期
    expiry_date DATE,                            -- 失效日期
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(value_domain_id, value)
);

COMMENT ON TABLE permissible_value IS '允许值-在特定值域中允许的一个值含义的表达';

-- 2.3 值集版本 (Code Set Version)
-- 值集的版本管理
CREATE TABLE IF NOT EXISTS code_set_version (
    id BIGSERIAL PRIMARY KEY,
    value_domain_id BIGINT NOT NULL REFERENCES value_domain(id),
    version_number VARCHAR(20) NOT NULL,         -- 版本号
    effective_date DATE,                         -- 生效日期
    expiry_date DATE,                            -- 失效日期
    change_description TEXT,                     -- 变更说明
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(value_domain_id, version_number)
);

COMMENT ON TABLE code_set_version IS '值集版本-值域的版本管理';

-- ============================================================
-- 3. 数据元层 (Data Element Layer)
-- ============================================================

-- 3.1 数据元 (Data Element, DE)
-- 用一组属性规定其定义、标识、表示和允许值的数据单元
CREATE TABLE IF NOT EXISTS data_element (
    id BIGSERIAL PRIMARY KEY,

    -- 标识类属性
    code VARCHAR(50) NOT NULL UNIQUE,            -- 数据元标识符
    name VARCHAR(200) NOT NULL,                  -- 数据元名称
    name_en VARCHAR(200),                        -- 英文名称
    identifier VARCHAR(100),                     -- 唯一标识符
    version VARCHAR(20) DEFAULT '1.0',           -- 版本
    registration_authority VARCHAR(100),         -- 注册机构
    synonym_name VARCHAR(200),                   -- 同义名称
    context VARCHAR(200),                        -- 相关环境

    -- 定义类属性
    definition TEXT NOT NULL,                    -- 定义

    -- 关系类属性
    classification_scheme VARCHAR(100),          -- 分类模式
    keywords VARCHAR(500),                       -- 关键字
    related_data_ref VARCHAR(200),               -- 相关数据参照
    relationship_type VARCHAR(100),              -- 关系类型

    -- 表示类属性
    representation_category VARCHAR(50),         -- 表示类别
    representation_form VARCHAR(50),             -- 表示形式: 数值, 代码, 文本, 图标
    data_type VARCHAR(50) NOT NULL,              -- 数据元值的数据类型
    max_length INT,                              -- 数据元值的最大长度
    min_length INT,                              -- 数据元值的最小长度
    format VARCHAR(100),                         -- 表示格式

    -- 关联
    data_element_concept_id BIGINT REFERENCES data_element_concept(id),  -- 数据元概念
    value_domain_id BIGINT REFERENCES value_domain(id),  -- 值域

    -- 管理类属性
    governing_body VARCHAR(100),                 -- 主管机构
    registration_status VARCHAR(20) DEFAULT 'DRAFT',  -- 注册状态
    submitting_organization VARCHAR(100),        -- 提交机构
    remark TEXT,                                 -- 备注

    -- 附加类属性(卫生健康领域扩展)
    collection_method VARCHAR(200),              -- 收集方法
    data_source VARCHAR(200),                    -- 数据来源

    status VARCHAR(20) DEFAULT 'DRAFT',
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE data_element IS '数据元-用一组属性规定其定义、标识、表示和允许值的数据单元';

-- ============================================================
-- 4. 对象类和特性 (Object Class & Property)
-- ============================================================

-- 4.1 对象类 (Object Class)
-- 可以对其界限和含义进行明确的标识,且特性和行为遵循相同规则的事物的集合
CREATE TABLE IF NOT EXISTS object_class (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,            -- 对象类代码
    name VARCHAR(200) NOT NULL,                  -- 对象类名称
    name_en VARCHAR(200),                        -- 英文名称
    definition TEXT NOT NULL,                    -- 定义
    concept_type VARCHAR(20) DEFAULT 'GENERAL',  -- 概念类型: GENERAL(一般概念), INDIVIDUAL(个别概念)
    parent_id BIGINT REFERENCES object_class(id),
    version VARCHAR(20) DEFAULT '1.0',
    status VARCHAR(20) DEFAULT 'DRAFT',
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark TEXT
);

COMMENT ON TABLE object_class IS '对象类-可以对其界限和含义进行明确的标识的事物的集合';

-- 4.2 特性 (Property)
-- 一个对象类所有成员所共有的特征
CREATE TABLE IF NOT EXISTS property (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,            -- 特性代码
    name VARCHAR(200) NOT NULL,                  -- 特性名称
    name_en VARCHAR(200),                        -- 英文名称
    definition TEXT NOT NULL,                    -- 定义
    concept_type VARCHAR(20) DEFAULT 'GENERAL',  -- 概念类型: GENERAL, INDIVIDUAL
    parent_id BIGINT REFERENCES property(id),
    version VARCHAR(20) DEFAULT '1.0',
    status VARCHAR(20) DEFAULT 'DRAFT',
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark TEXT
);

COMMENT ON TABLE property IS '特性-一个对象类所有成员所共有的特征';

-- ============================================================
-- 5. 表示类 (Representation Class)
-- ============================================================

-- 5.1 表示类 (Representation Class)
-- 表示类型的分类
CREATE TABLE IF NOT EXISTS representation_class_dict (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,            -- 表示类代码
    name VARCHAR(100) NOT NULL,                  -- 表示类名称
    name_en VARCHAR(100),                        -- 英文名称
    definition TEXT,                             -- 定义
    sort_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 初始化表示类字典数据
INSERT INTO representation_class_dict (code, name, name_en, definition, sort_order) VALUES
('AMOUNT', '金额', 'Amount', '表示货币金额', 1),
('CODE', '代码', 'Code', '表示编码值', 2),
('COUNT', '计数', 'Count', '表示计数', 3),
('DATE', '日期', 'Date', '表示日期', 4),
('TIME', '时间', 'Time', '表示时间', 5),
('DATETIME', '日期时间', 'DateTime', '表示日期和时间', 6),
('TEXT', '文本', 'Text', '表示文本描述', 7),
('NUMBER', '数值', 'Number', '表示数值', 8),
('QUANTITY', '数量', 'Quantity', '表示数量', 9),
('PERCENT', '百分比', 'Percent', '表示百分比', 10),
('RATE', '比率', 'Rate', '表示比率', 11),
('MEASUREMENT', '测量值', 'Measurement', '表示测量值', 12),
('IDENTIFIER', '标识符', 'Identifier', '表示标识符', 13),
('NAME', '名称', 'Name', '表示名称', 14),
('DESCRIPTION', '描述', 'Description', '表示描述', 15);

COMMENT ON TABLE representation_class_dict IS '表示类字典-表示类型的分类';

-- ============================================================
-- 6. 索引
-- ============================================================

CREATE INDEX idx_concept_domain_type ON concept_domain(domain_type);
CREATE INDEX idx_concept_domain_parent ON concept_domain(parent_id);
CREATE INDEX idx_value_meaning_domain ON value_meaning(concept_domain_id);
CREATE INDEX idx_data_element_concept_domain ON data_element_concept(concept_domain_id);
CREATE INDEX idx_value_domain_concept_domain ON value_domain(concept_domain_id);
CREATE INDEX idx_value_domain_type ON value_domain(domain_type);
CREATE INDEX idx_permissible_value_domain ON permissible_value(value_domain_id);
CREATE INDEX idx_permissible_value_meaning ON permissible_value(value_meaning_id);
CREATE INDEX idx_data_element_concept ON data_element(data_element_concept_id);
CREATE INDEX idx_data_element_value_domain ON data_element(value_domain_id);
CREATE INDEX idx_object_class_parent ON object_class(parent_id);
CREATE INDEX idx_property_parent ON property(parent_id);

-- ============================================================
-- 7. 视图
-- ============================================================

-- 7.1 数据元完整视图
CREATE OR REPLACE VIEW v_data_element_full AS
SELECT
    de.id,
    de.code,
    de.name,
    de.name_en,
    de.definition,
    de.data_type,
    de.max_length,
    de.min_length,
    de.format,
    de.representation_form,
    vd.code AS value_domain_code,
    vd.name AS value_domain_name,
    cd.code AS concept_domain_code,
    cd.name AS concept_domain_name,
    dec.code AS data_element_concept_code,
    dec.name AS data_element_concept_name,
    dec.object_class_name,
    dec.property_name,
    de.registration_status,
    de.version,
    de.created_at
FROM data_element de
LEFT JOIN value_domain vd ON de.value_domain_id = vd.id
LEFT JOIN concept_domain cd ON vd.concept_domain_id = cd.id
LEFT JOIN data_element_concept dec ON de.data_element_concept_id = dec.id;

-- 7.2 值域完整视图(包含允许值)
CREATE OR REPLACE VIEW v_value_domain_with_values AS
SELECT
    vd.id AS value_domain_id,
    vd.code AS value_domain_code,
    vd.name AS value_domain_name,
    vd.domain_type,
    vd.data_type,
    vd.unit_of_measure,
    cd.code AS concept_domain_code,
    cd.name AS concept_domain_name,
    pv.id AS permissible_value_id,
    pv.value,
    pv.value_meaning_name,
    pv.sort_order,
    pv.is_active
FROM value_domain vd
LEFT JOIN concept_domain cd ON vd.concept_domain_id = cd.id
LEFT JOIN permissible_value pv ON vd.id = pv.value_domain_id
ORDER BY vd.code, pv.sort_order;

COMMENT ON VIEW v_data_element_full IS '数据元完整视图-包含数据元、值域、概念域、数据元概念信息';
COMMENT ON VIEW v_value_domain_with_values IS '值域完整视图-包含值域和允许值信息';
