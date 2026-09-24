-- ============================================================
-- 23-masterdata-standard.sql
-- 主数据/数据标准（WS/T 303-2023）表收敛至 masterdata schema：
-- concept_domain / value_meaning / data_element_concept / value_domain /
-- permissible_value / object_class / property
-- 幂等可重跑；此前由 Hibernate 隐式建在 cdr schema 的存量表需另行迁移：
--   ALTER TABLE cdr.<table> SET SCHEMA masterdata;  （含对应 <table>_id_seq）
-- 同时注册 masterdata:* 与 system:dict:manage 权限码并授权
-- ============================================================

SET search_path TO masterdata, public;

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

-- ============================================================
-- 样例数据（幂等）
-- ============================================================


INSERT INTO concept_domain (code, name, name_en, definition, domain_type, dimension, status) VALUES
('CD_GENDER', '性别', 'Gender', '人类生物学性别的分类', 'ENUMERABLE', NULL, 'APPROVED') ON CONFLICT DO NOTHING;

INSERT INTO concept_domain (code, name, name_en, definition, domain_type, dimension, status) VALUES
('CD_BLOOD_TYPE', '血型', 'Blood Type', '人类血液类型的分类', 'ENUMERABLE', NULL, 'APPROVED') ON CONFLICT DO NOTHING;

INSERT INTO concept_domain (code, name, name_en, definition, domain_type, description_rule, dimension, status) VALUES
('CD_WEIGHT', '体重', 'Weight', '身体所有器官重量的总和', 'NON_ENUMERABLE', '用非负实数表示', '重量', 'APPROVED') ON CONFLICT DO NOTHING;

INSERT INTO concept_domain (code, name, name_en, definition, domain_type, dimension, status) VALUES
('CD_COUNTRY', '国别', 'Country', '世界各国名称的表示', 'ENUMERABLE', NULL, 'APPROVED') ON CONFLICT DO NOTHING;

INSERT INTO concept_domain (code, name, name_en, definition, domain_type, description_rule, dimension, status) VALUES
('CD_BODY_TEMPERATURE', '体温', 'Body Temperature', '人体内部的温度', 'NON_ENUMERABLE', '用实数表示，正常范围36.0-37.0℃', '温度', 'APPROVED') ON CONFLICT DO NOTHING;


INSERT INTO value_meaning (concept_domain_id, code, name, name_en, definition, sort_order) VALUES
((SELECT id FROM concept_domain WHERE code = 'CD_GENDER'), '1', '男性', 'Male', '男性性别', 1),
((SELECT id FROM concept_domain WHERE code = 'CD_GENDER'), '2', '女性', 'Female', '女性性别', 2),
((SELECT id FROM concept_domain WHERE code = 'CD_GENDER'), '9', '未知的性别', 'Unknown', '性别未知', 9) ON CONFLICT DO NOTHING;

INSERT INTO value_meaning (concept_domain_id, code, name, name_en, definition, sort_order) VALUES
((SELECT id FROM concept_domain WHERE code = 'CD_BLOOD_TYPE'), '1', 'A型', 'Type A', 'A型血', 1),
((SELECT id FROM concept_domain WHERE code = 'CD_BLOOD_TYPE'), '2', 'B型', 'Type B', 'B型血', 2),
((SELECT id FROM concept_domain WHERE code = 'CD_BLOOD_TYPE'), '3', 'O型', 'Type O', 'O型血', 3),
((SELECT id FROM concept_domain WHERE code = 'CD_BLOOD_TYPE'), '4', 'AB型', 'Type AB', 'AB型血', 4),
((SELECT id FROM concept_domain WHERE code = 'CD_BLOOD_TYPE'), '9', '不详', 'Unknown', '血型不详', 9) ON CONFLICT DO NOTHING;


INSERT INTO value_domain (code, name, name_en, definition, domain_type, data_type, max_length, min_length, representation_class, concept_domain_id, status) VALUES
('VD_GENDER_CODE_1', '性别代码-1位数字', 'Gender Code 1 Digit', '性别代码，用1位数字表示', 'ENUMERABLE', 'STRING', 1, 1, 'CODE', (SELECT id FROM concept_domain WHERE code = 'CD_GENDER'), 'APPROVED') ON CONFLICT DO NOTHING;

INSERT INTO value_domain (code, name, name_en, definition, domain_type, data_type, max_length, min_length, representation_class, concept_domain_id, status) VALUES
('VD_GENDER_CODE_1L', '性别代码-1位字母', 'Gender Code 1 Letter', '性别代码，用1位字母表示', 'ENUMERABLE', 'STRING', 1, 1, 'CODE', (SELECT id FROM concept_domain WHERE code = 'CD_GENDER'), 'APPROVED') ON CONFLICT DO NOTHING;

INSERT INTO value_domain (code, name, name_en, definition, domain_type, data_type, max_length, min_length, representation_class, concept_domain_id, status) VALUES
('VD_BLOOD_TYPE_CODE', '血型类别代码', 'Blood Type Code', '血型类别代码，用1位数字表示', 'ENUMERABLE', 'STRING', 1, 1, 'CODE', (SELECT id FROM concept_domain WHERE code = 'CD_BLOOD_TYPE'), 'APPROVED') ON CONFLICT DO NOTHING;

INSERT INTO value_domain (code, name, name_en, definition, domain_type, description, data_type, max_length, min_length, format, unit_of_measure, unit_name, representation_class, concept_domain_id, status) VALUES
('VD_WEIGHT_KG', '体重-N5,2(千克)', 'Weight N5,2(kg)', '身体所有器官重量的总和，最大长度5位的非负实数，小数点后保留2位数字', 'NON_ENUMERABLE', '非负实数，小数点后保留2位', 'DECIMAL', 5, 1, 'N5,2', 'kg', '千克', 'MEASUREMENT', (SELECT id FROM concept_domain WHERE code = 'CD_WEIGHT'), 'APPROVED') ON CONFLICT DO NOTHING;

INSERT INTO value_domain (code, name, name_en, definition, domain_type, description, data_type, max_length, min_length, unit_of_measure, unit_name, representation_class, concept_domain_id, status) VALUES
('VD_WEIGHT_G', '体重-N4(克)', 'Weight N4(g)', '身体所有器官重量的总和，最大长度4位的非负整数', 'NON_ENUMERABLE', '非负整数', 'INTEGER', 4, 1, 'g', '克', 'MEASUREMENT', (SELECT id FROM concept_domain WHERE code = 'CD_WEIGHT'), 'APPROVED') ON CONFLICT DO NOTHING;

INSERT INTO value_domain (code, name, name_en, definition, domain_type, data_type, max_length, min_length, representation_class, concept_domain_id, status) VALUES
('VD_COUNTRY_CODE_3L', '国家代码-3位字母', 'Country Code 3 Letters', '国家代码，用3位字母表示(ISO 3166-1)', 'ENUMERABLE', 'STRING', 3, 3, 'CODE', (SELECT id FROM concept_domain WHERE code = 'CD_COUNTRY'), 'APPROVED') ON CONFLICT DO NOTHING;

INSERT INTO value_domain (code, name, name_en, definition, domain_type, data_type, max_length, min_length, representation_class, concept_domain_id, status) VALUES
('VD_COUNTRY_CODE_2L', '国家代码-2位字母', 'Country Code 2 Letters', '国家代码，用2位字母表示(ISO 3166-1)', 'ENUMERABLE', 'STRING', 2, 2, 'CODE', (SELECT id FROM concept_domain WHERE code = 'CD_COUNTRY'), 'APPROVED') ON CONFLICT DO NOTHING;


INSERT INTO permissible_value (value_domain_id, value, value_meaning_id, value_meaning_name, sort_order) VALUES
((SELECT id FROM value_domain WHERE code = 'VD_GENDER_CODE_1'), '1', (SELECT id FROM value_meaning WHERE concept_domain_id = (SELECT id FROM concept_domain WHERE code = 'CD_GENDER') AND code = '1'), '男性', 1),
((SELECT id FROM value_domain WHERE code = 'VD_GENDER_CODE_1'), '2', (SELECT id FROM value_meaning WHERE concept_domain_id = (SELECT id FROM concept_domain WHERE code = 'CD_GENDER') AND code = '2'), '女性', 2),
((SELECT id FROM value_domain WHERE code = 'VD_GENDER_CODE_1'), '9', (SELECT id FROM value_meaning WHERE concept_domain_id = (SELECT id FROM concept_domain WHERE code = 'CD_GENDER') AND code = '9'), '未知的性别', 9) ON CONFLICT DO NOTHING;

INSERT INTO permissible_value (value_domain_id, value, value_meaning_name, sort_order) VALUES
((SELECT id FROM value_domain WHERE code = 'VD_GENDER_CODE_1L'), 'M', '男性', 1),
((SELECT id FROM value_domain WHERE code = 'VD_GENDER_CODE_1L'), 'F', '女性', 2),
((SELECT id FROM value_domain WHERE code = 'VD_GENDER_CODE_1L'), 'U', '未知的性别', 9) ON CONFLICT DO NOTHING;

INSERT INTO permissible_value (value_domain_id, value, value_meaning_id, value_meaning_name, sort_order) VALUES
((SELECT id FROM value_domain WHERE code = 'VD_BLOOD_TYPE_CODE'), '1', (SELECT id FROM value_meaning WHERE concept_domain_id = (SELECT id FROM concept_domain WHERE code = 'CD_BLOOD_TYPE') AND code = '1'), 'A型', 1),
((SELECT id FROM value_domain WHERE code = 'VD_BLOOD_TYPE_CODE'), '2', (SELECT id FROM value_meaning WHERE concept_domain_id = (SELECT id FROM concept_domain WHERE code = 'CD_BLOOD_TYPE') AND code = '2'), 'B型', 2),
((SELECT id FROM value_domain WHERE code = 'VD_BLOOD_TYPE_CODE'), '3', (SELECT id FROM value_meaning WHERE concept_domain_id = (SELECT id FROM concept_domain WHERE code = 'CD_BLOOD_TYPE') AND code = '3'), 'O型', 3),
((SELECT id FROM value_domain WHERE code = 'VD_BLOOD_TYPE_CODE'), '4', (SELECT id FROM value_meaning WHERE concept_domain_id = (SELECT id FROM concept_domain WHERE code = 'CD_BLOOD_TYPE') AND code = '4'), 'AB型', 4),
((SELECT id FROM value_domain WHERE code = 'VD_BLOOD_TYPE_CODE'), '9', (SELECT id FROM value_meaning WHERE concept_domain_id = (SELECT id FROM concept_domain WHERE code = 'CD_BLOOD_TYPE') AND code = '9'), '不详', 9) ON CONFLICT DO NOTHING;

INSERT INTO permissible_value (value_domain_id, value, value_meaning_name, sort_order) VALUES
((SELECT id FROM value_domain WHERE code = 'VD_COUNTRY_CODE_3L'), 'CHN', '中国', 1),
((SELECT id FROM value_domain WHERE code = 'VD_COUNTRY_CODE_3L'), 'USA', '美国', 2),
((SELECT id FROM value_domain WHERE code = 'VD_COUNTRY_CODE_3L'), 'GBR', '英国', 3),
((SELECT id FROM value_domain WHERE code = 'VD_COUNTRY_CODE_3L'), 'JPN', '日本', 4),
((SELECT id FROM value_domain WHERE code = 'VD_COUNTRY_CODE_3L'), 'KOR', '韩国', 5) ON CONFLICT DO NOTHING;

INSERT INTO permissible_value (value_domain_id, value, value_meaning_name, sort_order) VALUES
((SELECT id FROM value_domain WHERE code = 'VD_COUNTRY_CODE_2L'), 'CN', '中国', 1),
((SELECT id FROM value_domain WHERE code = 'VD_COUNTRY_CODE_2L'), 'US', '美国', 2),
((SELECT id FROM value_domain WHERE code = 'VD_COUNTRY_CODE_2L'), 'GB', '英国', 3),
((SELECT id FROM value_domain WHERE code = 'VD_COUNTRY_CODE_2L'), 'JP', '日本', 4),
((SELECT id FROM value_domain WHERE code = 'VD_COUNTRY_CODE_2L'), 'KR', '韩国', 5) ON CONFLICT DO NOTHING;


INSERT INTO object_class (code, name, name_en, definition, concept_type, status) VALUES
('OC_PATIENT', '患者', 'Patient', '接受医疗服务的个体', 'GENERAL', 'APPROVED'),
('OC_DOCTOR', '医生', 'Doctor', '提供医疗服务的专业人员', 'GENERAL', 'APPROVED'),
('OC_NURSE', '护士', 'Nurse', '提供护理服务的专业人员', 'GENERAL', 'APPROVED'),
('OC_HOSPITAL', '医疗机构', 'Hospital', '提供医疗服务的机构', 'GENERAL', 'APPROVED'),
('OC_NEWBORN', '新生儿', 'Newborn', '出生28天内的婴儿', 'GENERAL', 'APPROVED') ON CONFLICT DO NOTHING;


INSERT INTO property (code, name, name_en, definition, concept_type, status) VALUES
('PR_NAME', '姓名', 'Name', '个体的正式称呼', 'GENERAL', 'APPROVED'),
('PR_GENDER', '性别', 'Gender', '个体的生物学性别', 'GENERAL', 'APPROVED'),
('PR_AGE', '年龄', 'Age', '个体从出生到现在的时长', 'GENERAL', 'APPROVED'),
('PR_WEIGHT', '体重', 'Weight', '身体所有器官重量的总和', 'GENERAL', 'APPROVED'),
('PR_HEIGHT', '身高', 'Height', '个体站立时从脚底到头顶的垂直距离', 'GENERAL', 'APPROVED'),
('PR_BLOOD_TYPE', '血型', 'Blood Type', '个体血液的类型', 'GENERAL', 'APPROVED'),
('PR_BIRTH_DATE', '出生日期', 'Birth Date', '个体出生的日期', 'GENERAL', 'APPROVED'),
('PR_NATIONALITY', '国籍', 'Nationality', '个体所属的国家', 'GENERAL', 'APPROVED'),
('PR_BODY_TEMPERATURE', '体温', 'Body Temperature', '人体内部的温度', 'GENERAL', 'APPROVED') ON CONFLICT DO NOTHING;


INSERT INTO data_element_concept (code, name, name_en, definition, object_class_code, object_class_name, property_code, property_name, concept_domain_id, status) VALUES
('DEC_PATIENT_GENDER', '患者性别', 'Patient Gender', '患者的生物学性别分类', 'OC_PATIENT', '患者', 'PR_GENDER', '性别', (SELECT id FROM concept_domain WHERE code = 'CD_GENDER'), 'APPROVED'),
('DEC_PATIENT_BLOOD_TYPE', '患者血型', 'Patient Blood Type', '患者的血液类型', 'OC_PATIENT', '患者', 'PR_BLOOD_TYPE', '血型', (SELECT id FROM concept_domain WHERE code = 'CD_BLOOD_TYPE'), 'APPROVED'),
('DEC_PATIENT_WEIGHT', '患者体重', 'Patient Weight', '患者身体所有器官重量的总和', 'OC_PATIENT', '患者', 'PR_WEIGHT', '体重', (SELECT id FROM concept_domain WHERE code = 'CD_WEIGHT'), 'APPROVED'),
('DEC_PATIENT_NATIONALITY', '患者国籍', 'Patient Nationality', '患者所属的国家', 'OC_PATIENT', '患者', 'PR_NATIONALITY', '国籍', (SELECT id FROM concept_domain WHERE code = 'CD_COUNTRY'), 'APPROVED'),
('DEC_NEWBORN_WEIGHT', '新生儿体重', 'Newborn Weight', '新生儿身体所有器官重量的总和', 'OC_NEWBORN', '新生儿', 'PR_WEIGHT', '体重', (SELECT id FROM concept_domain WHERE code = 'CD_WEIGHT'), 'APPROVED'),
('DEC_PATIENT_BODY_TEMPERATURE', '患者体温', 'Patient Body Temperature', '患者人体内部的温度', 'OC_PATIENT', '患者', 'PR_BODY_TEMPERATURE', '体温', (SELECT id FROM concept_domain WHERE code = 'CD_BODY_TEMPERATURE'), 'APPROVED') ON CONFLICT DO NOTHING;







SELECT
    vd.code AS value_domain_code,
    vd.name AS value_domain_name,
    pv.value,
    pv.value_meaning_name,
    pv.sort_order
FROM value_domain vd
LEFT JOIN permissible_value pv ON vd.id = pv.value_domain_id
WHERE vd.code = 'VD_GENDER_CODE_1'
ORDER BY pv.sort_order;

SELECT
    cd.name AS concept_domain_name,
    vd.code AS value_domain_code,
    vd.name AS value_domain_name,
    vd.data_type,
    vd.unit_of_measure
FROM concept_domain cd
JOIN value_domain vd ON cd.id = vd.concept_domain_id
WHERE cd.code = 'CD_WEIGHT'
ORDER BY vd.code;


RESET search_path;

-- ============================================================
-- 权限码注册与授权（masterdata 粒度化 + 系统字典管理）
-- ============================================================
INSERT INTO system.s_permission (permission_code, permission_name, resource_type, resource_key, action, sort_order, created_by, org_id) VALUES
('masterdata:read',  '主数据查看',     'MENU',   '/system/masterdata',                 'READ',   60, 'system', 0),
('masterdata:create','主数据新增',     'BUTTON', '/system/masterdata/create',          'CREATE', 61, 'system', 0),
('masterdata:update','主数据修改',     'BUTTON', '/system/masterdata/update',          'UPDATE', 62, 'system', 0),
('masterdata:delete','主数据删除',     'BUTTON', '/system/masterdata/delete',          'DELETE', 63, 'system', 0),
('system:dict:manage','数据字典管理',  'BUTTON', '/system/dict-manage',                'MANAGE', 64, 'system', 0)
ON CONFLICT DO NOTHING;

-- admin / data_admin：全部主数据与字典权限
INSERT INTO system.s_role_permission (role_id, permission_id, org_id)
SELECT r.id, p.id, 0 FROM system.s_role r, system.s_permission p
WHERE r.org_id = 0 AND p.org_id = 0
  AND r.role_code IN ('admin','data_admin')
  AND p.permission_code IN ('masterdata:read','masterdata:create','masterdata:update','masterdata:delete','system:dict:manage')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- researcher / doctor / auditor：只读
INSERT INTO system.s_role_permission (role_id, permission_id, org_id)
SELECT r.id, p.id, 0 FROM system.s_role r, system.s_permission p
WHERE r.org_id = 0 AND p.org_id = 0
  AND r.role_code IN ('researcher','doctor','auditor')
  AND p.permission_code = 'masterdata:read'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ============================================================
-- 数据字典类型表（字典项仍存于 system.s_dict，dict_type = type_code）
-- ============================================================
CREATE TABLE IF NOT EXISTS system.s_dict_type (
    id          BIGSERIAL    PRIMARY KEY,
    type_code   VARCHAR(64)  NOT NULL,
    type_name   VARCHAR(128) NOT NULL,
    remark      VARCHAR(256),
    created_by  VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by  VARCHAR(64),
    updated_at  TIMESTAMP,
    is_deleted  BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id      BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_dict_type UNIQUE (org_id, type_code)
);
COMMENT ON TABLE system.s_dict_type IS '数据字典类型表';

-- ============================================================
-- 表示类字典（WS/T 303-2023 值域"表示类"分类目录）
-- 与 RepresentationClassEntity 对齐：BaseEntity 审计列(is_deleted/org_id)一并建列，避免 ddl-auto 追列漂移
-- ============================================================
CREATE TABLE IF NOT EXISTS masterdata.representation_class (
    id          BIGSERIAL    PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(200) NOT NULL,
    name_en     VARCHAR(200),
    definition  TEXT,
    sort_order  INT          NOT NULL DEFAULT 0,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by  VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by  VARCHAR(64),
    updated_at  TIMESTAMP,
    is_deleted  BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id      BIGINT       NOT NULL DEFAULT 0
);
COMMENT ON TABLE masterdata.representation_class IS '值域表示类字典（WS/T 303-2023）';

INSERT INTO masterdata.representation_class (code, name, name_en, definition, sort_order) VALUES
('AMOUNT',   '金额',     'Amount',   '以货币形式表示的量', 1),
('CODE',     '代码',     'Code',     '以代码形式表示的值', 2),
('COUNT',    '计数',     'Count',    '以整数形式表示的数量', 3),
('DATE',     '日期',     'Date',     '以日期形式表示的值', 4),
('DATETIME', '日期时间', 'DateTime', '以日期时间形式表示的值', 5),
('TIME',     '时间',     'Time',     '以时间形式表示的值', 6),
('TEXT',     '文本',     'Text',     '以自由文本形式表示的值', 7),
('NUMBER',   '数值',     'Number',   '以数值形式表示的量', 8)
ON CONFLICT (code) DO NOTHING;
