-- ============================================================
-- 卫生健康信息数据元标准化示例数据
-- 基于 WS/T 303-2023 《卫生健康信息数据元标准化规则》
-- ============================================================

-- ============================================================
-- 1. 概念域示例
-- ============================================================

-- 1.1 性别概念域(可枚举)
INSERT INTO concept_domain (code, name, name_en, definition, domain_type, dimension, status) VALUES
('CD_GENDER', '性别', 'Gender', '人类生物学性别的分类', 'ENUMERABLE', NULL, 'APPROVED');

-- 1.2 血型概念域(可枚举)
INSERT INTO concept_domain (code, name, name_en, definition, domain_type, dimension, status) VALUES
('CD_BLOOD_TYPE', '血型', 'Blood Type', '人类血液类型的分类', 'ENUMERABLE', NULL, 'APPROVED');

-- 1.3 体重概念域(不可枚举)
INSERT INTO concept_domain (code, name, name_en, definition, domain_type, description_rule, dimension, status) VALUES
('CD_WEIGHT', '体重', 'Weight', '身体所有器官重量的总和', 'NON_ENUMERABLE', '用非负实数表示', '重量', 'APPROVED');

-- 1.4 国别概念域(可枚举)
INSERT INTO concept_domain (code, name, name_en, definition, domain_type, dimension, status) VALUES
('CD_COUNTRY', '国别', 'Country', '世界各国名称的表示', 'ENUMERABLE', NULL, 'APPROVED');

-- 1.5 体温概念域(不可枚举)
INSERT INTO concept_domain (code, name, name_en, definition, domain_type, description_rule, dimension, status) VALUES
('CD_BODY_TEMPERATURE', '体温', 'Body Temperature', '人体内部的温度', 'NON_ENUMERABLE', '用实数表示，正常范围36.0-37.0℃', '温度', 'APPROVED');

-- ============================================================
-- 2. 值含义示例
-- ============================================================

-- 2.1 性别值含义
INSERT INTO value_meaning (concept_domain_id, code, name, name_en, definition, sort_order) VALUES
((SELECT id FROM concept_domain WHERE code = 'CD_GENDER'), '1', '男性', 'Male', '男性性别', 1),
((SELECT id FROM concept_domain WHERE code = 'CD_GENDER'), '2', '女性', 'Female', '女性性别', 2),
((SELECT id FROM concept_domain WHERE code = 'CD_GENDER'), '9', '未知的性别', 'Unknown', '性别未知', 9);

-- 2.2 血型值含义
INSERT INTO value_meaning (concept_domain_id, code, name, name_en, definition, sort_order) VALUES
((SELECT id FROM concept_domain WHERE code = 'CD_BLOOD_TYPE'), '1', 'A型', 'Type A', 'A型血', 1),
((SELECT id FROM concept_domain WHERE code = 'CD_BLOOD_TYPE'), '2', 'B型', 'Type B', 'B型血', 2),
((SELECT id FROM concept_domain WHERE code = 'CD_BLOOD_TYPE'), '3', 'O型', 'Type O', 'O型血', 3),
((SELECT id FROM concept_domain WHERE code = 'CD_BLOOD_TYPE'), '4', 'AB型', 'Type AB', 'AB型血', 4),
((SELECT id FROM concept_domain WHERE code = 'CD_BLOOD_TYPE'), '9', '不详', 'Unknown', '血型不详', 9);

-- ============================================================
-- 3. 值域示例
-- ============================================================

-- 3.1 性别代码值域(1位数字)
INSERT INTO value_domain (code, name, name_en, definition, domain_type, data_type, max_length, min_length, representation_class, concept_domain_id, status) VALUES
('VD_GENDER_CODE_1', '性别代码-1位数字', 'Gender Code 1 Digit', '性别代码，用1位数字表示', 'ENUMERABLE', 'STRING', 1, 1, 'CODE', (SELECT id FROM concept_domain WHERE code = 'CD_GENDER'), 'APPROVED');

-- 3.2 性别代码值域(1位字母)
INSERT INTO value_domain (code, name, name_en, definition, domain_type, data_type, max_length, min_length, representation_class, concept_domain_id, status) VALUES
('VD_GENDER_CODE_1L', '性别代码-1位字母', 'Gender Code 1 Letter', '性别代码，用1位字母表示', 'ENUMERABLE', 'STRING', 1, 1, 'CODE', (SELECT id FROM concept_domain WHERE code = 'CD_GENDER'), 'APPROVED');

-- 3.3 血型代码值域
INSERT INTO value_domain (code, name, name_en, definition, domain_type, data_type, max_length, min_length, representation_class, concept_domain_id, status) VALUES
('VD_BLOOD_TYPE_CODE', '血型类别代码', 'Blood Type Code', '血型类别代码，用1位数字表示', 'ENUMERABLE', 'STRING', 1, 1, 'CODE', (SELECT id FROM concept_domain WHERE code = 'CD_BLOOD_TYPE'), 'APPROVED');

-- 3.4 体重值域(千克)
INSERT INTO value_domain (code, name, name_en, definition, domain_type, description, data_type, max_length, min_length, format, unit_of_measure, unit_name, representation_class, concept_domain_id, status) VALUES
('VD_WEIGHT_KG', '体重-N5,2(千克)', 'Weight N5,2(kg)', '身体所有器官重量的总和，最大长度5位的非负实数，小数点后保留2位数字', 'NON_ENUMERABLE', '非负实数，小数点后保留2位', 'DECIMAL', 5, 1, 'N5,2', 'kg', '千克', 'MEASUREMENT', (SELECT id FROM concept_domain WHERE code = 'CD_WEIGHT'), 'APPROVED');

-- 3.5 体重值域(克)
INSERT INTO value_domain (code, name, name_en, definition, domain_type, description, data_type, max_length, min_length, unit_of_measure, unit_name, representation_class, concept_domain_id, status) VALUES
('VD_WEIGHT_G', '体重-N4(克)', 'Weight N4(g)', '身体所有器官重量的总和，最大长度4位的非负整数', 'NON_ENUMERABLE', '非负整数', 'INTEGER', 4, 1, 'g', '克', 'MEASUREMENT', (SELECT id FROM concept_domain WHERE code = 'CD_WEIGHT'), 'APPROVED');

-- 3.6 国别代码值域(3位字母)
INSERT INTO value_domain (code, name, name_en, definition, domain_type, data_type, max_length, min_length, representation_class, concept_domain_id, status) VALUES
('VD_COUNTRY_CODE_3L', '国家代码-3位字母', 'Country Code 3 Letters', '国家代码，用3位字母表示(ISO 3166-1)', 'ENUMERABLE', 'STRING', 3, 3, 'CODE', (SELECT id FROM concept_domain WHERE code = 'CD_COUNTRY'), 'APPROVED');

-- 3.7 国别代码值域(2位字母)
INSERT INTO value_domain (code, name, name_en, definition, domain_type, data_type, max_length, min_length, representation_class, concept_domain_id, status) VALUES
('VD_COUNTRY_CODE_2L', '国家代码-2位字母', 'Country Code 2 Letters', '国家代码，用2位字母表示(ISO 3166-1)', 'ENUMERABLE', 'STRING', 2, 2, 'CODE', (SELECT id FROM concept_domain WHERE code = 'CD_COUNTRY'), 'APPROVED');

-- ============================================================
-- 4. 允许值/值集示例
-- ============================================================

-- 4.1 性别代码-1位数字 允许值
INSERT INTO permissible_value (value_domain_id, value, value_meaning_id, value_meaning_name, sort_order) VALUES
((SELECT id FROM value_domain WHERE code = 'VD_GENDER_CODE_1'), '1', (SELECT id FROM value_meaning WHERE concept_domain_id = (SELECT id FROM concept_domain WHERE code = 'CD_GENDER') AND code = '1'), '男性', 1),
((SELECT id FROM value_domain WHERE code = 'VD_GENDER_CODE_1'), '2', (SELECT id FROM value_meaning WHERE concept_domain_id = (SELECT id FROM concept_domain WHERE code = 'CD_GENDER') AND code = '2'), '女性', 2),
((SELECT id FROM value_domain WHERE code = 'VD_GENDER_CODE_1'), '9', (SELECT id FROM value_meaning WHERE concept_domain_id = (SELECT id FROM concept_domain WHERE code = 'CD_GENDER') AND code = '9'), '未知的性别', 9);

-- 4.2 性别代码-1位字母 允许值
INSERT INTO permissible_value (value_domain_id, value, value_meaning_name, sort_order) VALUES
((SELECT id FROM value_domain WHERE code = 'VD_GENDER_CODE_1L'), 'M', '男性', 1),
((SELECT id FROM value_domain WHERE code = 'VD_GENDER_CODE_1L'), 'F', '女性', 2),
((SELECT id FROM value_domain WHERE code = 'VD_GENDER_CODE_1L'), 'U', '未知的性别', 9);

-- 4.3 血型类别代码 允许值
INSERT INTO permissible_value (value_domain_id, value, value_meaning_id, value_meaning_name, sort_order) VALUES
((SELECT id FROM value_domain WHERE code = 'VD_BLOOD_TYPE_CODE'), '1', (SELECT id FROM value_meaning WHERE concept_domain_id = (SELECT id FROM concept_domain WHERE code = 'CD_BLOOD_TYPE') AND code = '1'), 'A型', 1),
((SELECT id FROM value_domain WHERE code = 'VD_BLOOD_TYPE_CODE'), '2', (SELECT id FROM value_meaning WHERE concept_domain_id = (SELECT id FROM concept_domain WHERE code = 'CD_BLOOD_TYPE') AND code = '2'), 'B型', 2),
((SELECT id FROM value_domain WHERE code = 'VD_BLOOD_TYPE_CODE'), '3', (SELECT id FROM value_meaning WHERE concept_domain_id = (SELECT id FROM concept_domain WHERE code = 'CD_BLOOD_TYPE') AND code = '3'), 'O型', 3),
((SELECT id FROM value_domain WHERE code = 'VD_BLOOD_TYPE_CODE'), '4', (SELECT id FROM value_meaning WHERE concept_domain_id = (SELECT id FROM concept_domain WHERE code = 'CD_BLOOD_TYPE') AND code = '4'), 'AB型', 4),
((SELECT id FROM value_domain WHERE code = 'VD_BLOOD_TYPE_CODE'), '9', (SELECT id FROM value_meaning WHERE concept_domain_id = (SELECT id FROM concept_domain WHERE code = 'CD_BLOOD_TYPE') AND code = '9'), '不详', 9);

-- 4.4 国别代码-3位字母 允许值(部分示例)
INSERT INTO permissible_value (value_domain_id, value, value_meaning_name, sort_order) VALUES
((SELECT id FROM value_domain WHERE code = 'VD_COUNTRY_CODE_3L'), 'CHN', '中国', 1),
((SELECT id FROM value_domain WHERE code = 'VD_COUNTRY_CODE_3L'), 'USA', '美国', 2),
((SELECT id FROM value_domain WHERE code = 'VD_COUNTRY_CODE_3L'), 'GBR', '英国', 3),
((SELECT id FROM value_domain WHERE code = 'VD_COUNTRY_CODE_3L'), 'JPN', '日本', 4),
((SELECT id FROM value_domain WHERE code = 'VD_COUNTRY_CODE_3L'), 'KOR', '韩国', 5);

-- 4.5 国别代码-2位字母 允许值(部分示例)
INSERT INTO permissible_value (value_domain_id, value, value_meaning_name, sort_order) VALUES
((SELECT id FROM value_domain WHERE code = 'VD_COUNTRY_CODE_2L'), 'CN', '中国', 1),
((SELECT id FROM value_domain WHERE code = 'VD_COUNTRY_CODE_2L'), 'US', '美国', 2),
((SELECT id FROM value_domain WHERE code = 'VD_COUNTRY_CODE_2L'), 'GB', '英国', 3),
((SELECT id FROM value_domain WHERE code = 'VD_COUNTRY_CODE_2L'), 'JP', '日本', 4),
((SELECT id FROM value_domain WHERE code = 'VD_COUNTRY_CODE_2L'), 'KR', '韩国', 5);

-- ============================================================
-- 5. 对象类示例
-- ============================================================

INSERT INTO object_class (code, name, name_en, definition, concept_type, status) VALUES
('OC_PATIENT', '患者', 'Patient', '接受医疗服务的个体', 'GENERAL', 'APPROVED'),
('OC_DOCTOR', '医生', 'Doctor', '提供医疗服务的专业人员', 'GENERAL', 'APPROVED'),
('OC_NURSE', '护士', 'Nurse', '提供护理服务的专业人员', 'GENERAL', 'APPROVED'),
('OC_HOSPITAL', '医疗机构', 'Hospital', '提供医疗服务的机构', 'GENERAL', 'APPROVED'),
('OC_NEWBORN', '新生儿', 'Newborn', '出生28天内的婴儿', 'GENERAL', 'APPROVED');

-- ============================================================
-- 6. 特性示例
-- ============================================================

INSERT INTO property (code, name, name_en, definition, concept_type, status) VALUES
('PR_NAME', '姓名', 'Name', '个体的正式称呼', 'GENERAL', 'APPROVED'),
('PR_GENDER', '性别', 'Gender', '个体的生物学性别', 'GENERAL', 'APPROVED'),
('PR_AGE', '年龄', 'Age', '个体从出生到现在的时长', 'GENERAL', 'APPROVED'),
('PR_WEIGHT', '体重', 'Weight', '身体所有器官重量的总和', 'GENERAL', 'APPROVED'),
('PR_HEIGHT', '身高', 'Height', '个体站立时从脚底到头顶的垂直距离', 'GENERAL', 'APPROVED'),
('PR_BLOOD_TYPE', '血型', 'Blood Type', '个体血液的类型', 'GENERAL', 'APPROVED'),
('PR_BIRTH_DATE', '出生日期', 'Birth Date', '个体出生的日期', 'GENERAL', 'APPROVED'),
('PR_NATIONALITY', '国籍', 'Nationality', '个体所属的国家', 'GENERAL', 'APPROVED'),
('PR_BODY_TEMPERATURE', '体温', 'Body Temperature', '人体内部的温度', 'GENERAL', 'APPROVED');

-- ============================================================
-- 7. 数据元概念示例
-- ============================================================

INSERT INTO data_element_concept (code, name, name_en, definition, object_class_code, object_class_name, property_code, property_name, concept_domain_id, status) VALUES
('DEC_PATIENT_GENDER', '患者性别', 'Patient Gender', '患者的生物学性别分类', 'OC_PATIENT', '患者', 'PR_GENDER', '性别', (SELECT id FROM concept_domain WHERE code = 'CD_GENDER'), 'APPROVED'),
('DEC_PATIENT_BLOOD_TYPE', '患者血型', 'Patient Blood Type', '患者的血液类型', 'OC_PATIENT', '患者', 'PR_BLOOD_TYPE', '血型', (SELECT id FROM concept_domain WHERE code = 'CD_BLOOD_TYPE'), 'APPROVED'),
('DEC_PATIENT_WEIGHT', '患者体重', 'Patient Weight', '患者身体所有器官重量的总和', 'OC_PATIENT', '患者', 'PR_WEIGHT', '体重', (SELECT id FROM concept_domain WHERE code = 'CD_WEIGHT'), 'APPROVED'),
('DEC_PATIENT_NATIONALITY', '患者国籍', 'Patient Nationality', '患者所属的国家', 'OC_PATIENT', '患者', 'PR_NATIONALITY', '国籍', (SELECT id FROM concept_domain WHERE code = 'CD_COUNTRY'), 'APPROVED'),
('DEC_NEWBORN_WEIGHT', '新生儿体重', 'Newborn Weight', '新生儿身体所有器官重量的总和', 'OC_NEWBORN', '新生儿', 'PR_WEIGHT', '体重', (SELECT id FROM concept_domain WHERE code = 'CD_WEIGHT'), 'APPROVED'),
('DEC_PATIENT_BODY_TEMPERATURE', '患者体温', 'Patient Body Temperature', '患者人体内部的温度', 'OC_PATIENT', '患者', 'PR_BODY_TEMPERATURE', '体温', (SELECT id FROM concept_domain WHERE code = 'CD_BODY_TEMPERATURE'), 'APPROVED');

-- ============================================================
-- 8. 数据元示例
-- ============================================================

INSERT INTO data_element (code, name, name_en, definition, data_type, max_length, min_length, representation_form, data_element_concept_id, value_domain_id, registration_status, status) VALUES
('DE_PATIENT_GENDER_CODE_1', '患者性别代码-1位数字', 'Patient Gender Code 1 Digit', '患者的生物学性别代码，用1位数字表示', 'STRING', 1, 1, '代码', (SELECT id FROM data_element_concept WHERE code = 'DEC_PATIENT_GENDER'), (SELECT id FROM value_domain WHERE code = 'VD_GENDER_CODE_1'), 'APPROVED', 'APPROVED'),
('DE_PATIENT_GENDER_CODE_1L', '患者性别代码-1位字母', 'Patient Gender Code 1 Letter', '患者的生物学性别代码，用1位字母表示', 'STRING', 1, 1, '代码', (SELECT id FROM data_element_concept WHERE code = 'DEC_PATIENT_GENDER'), (SELECT id FROM value_domain WHERE code = 'VD_GENDER_CODE_1L'), 'APPROVED', 'APPROVED'),
('DE_PATIENT_BLOOD_TYPE_CODE', '患者血型类别代码', 'Patient Blood Type Code', '患者的血液类型代码，用1位数字表示', 'STRING', 1, 1, '代码', (SELECT id FROM data_element_concept WHERE code = 'DEC_PATIENT_BLOOD_TYPE'), (SELECT id FROM value_domain WHERE code = 'VD_BLOOD_TYPE_CODE'), 'APPROVED', 'APPROVED'),
('DE_PATIENT_WEIGHT_KG', '患者体重-N5,2(千克)', 'Patient Weight N5,2(kg)', '患者身体所有器官重量的总和，以千克为单位', 'DECIMAL', 5, 1, '数值', (SELECT id FROM data_element_concept WHERE code = 'DEC_PATIENT_WEIGHT'), (SELECT id FROM value_domain WHERE code = 'VD_WEIGHT_KG'), 'APPROVED', 'APPROVED'),
('DE_PATIENT_NATIONALITY_3L', '患者国籍代码-3位字母', 'Patient Nationality Code 3 Letters', '患者所属国家的代码，用3位字母表示', 'STRING', 3, 3, '代码', (SELECT id FROM data_element_concept WHERE code = 'DEC_PATIENT_NATIONALITY'), (SELECT id FROM value_domain WHERE code = 'VD_COUNTRY_CODE_3L'), 'APPROVED', 'APPROVED'),
('DE_NEWBORN_WEIGHT_KG', '新生儿体重-N5,2(千克)', 'Newborn Weight N5,2(kg)', '新生儿身体所有器官重量的总和，以千克为单位', 'DECIMAL', 5, 1, '数值', (SELECT id FROM data_element_concept WHERE code = 'DEC_NEWBORN_WEIGHT'), (SELECT id FROM value_domain WHERE code = 'VD_WEIGHT_KG'), 'APPROVED', 'APPROVED'),
('DE_NEWBORN_WEIGHT_G', '新生儿体重-N4(克)', 'Newborn Weight N4(g)', '新生儿身体所有器官重量的总和，以克为单位', 'INTEGER', 4, 1, '数值', (SELECT id FROM data_element_concept WHERE code = 'DEC_NEWBORN_WEIGHT'), (SELECT id FROM value_domain WHERE code = 'VD_WEIGHT_G'), 'APPROVED', 'APPROVED');

-- ============================================================
-- 9. 验证查询
-- ============================================================

-- 查询数据元完整信息
SELECT
    de.code AS data_element_code,
    de.name AS data_element_name,
    de.definition,
    dec.name AS data_element_concept_name,
    dec.object_class_name,
    dec.property_name,
    vd.code AS value_domain_code,
    vd.name AS value_domain_name,
    cd.code AS concept_domain_code,
    cd.name AS concept_domain_name
FROM data_element de
JOIN data_element_concept dec ON de.data_element_concept_id = dec.id
JOIN value_domain vd ON de.value_domain_id = vd.id
JOIN concept_domain cd ON vd.concept_domain_id = cd.id
ORDER BY de.code;

-- 查询值域及其允许值
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

-- 查询同一概念域对应的不同值域
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
