-- ============================================================
-- 11-ods-reduce.sql
-- ODS 数据缩减：保留 1000 个随机患者的数据，其余删除
-- 执行方式：docker exec -i maidc-postgres psql -U maidc -d maidc -f /docker-entrypoint-initdb.d/11-ods-reduce.sql
--
-- 策略：CTAS 重建表（分区表改为非分区），字典表全量保留
-- ============================================================

\set ON_ERROR_STOP on
\timing on

-- ===================== Step 0: 检查是否有导入正在进行 =====================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM ods.ods_import_log WHERE status = 'RUNNING') THEN
        RAISE EXCEPTION '有导入任务正在运行，请先等待完成';
    END IF;
END $$;

-- ===================== Step 1: 随机选取 1000 个患者 =====================
DROP TABLE IF EXISTS ods._sample_mimic3;
DROP TABLE IF EXISTS ods._sample_mimic4;

CREATE TABLE ods._sample_mimic3 AS
SELECT subject_id FROM ods.o3_patients ORDER BY RANDOM() LIMIT 1000;

CREATE TABLE ods._sample_mimic4 AS
SELECT subject_id FROM ods.o4_patients ORDER BY RANDOM() LIMIT 1000;

-- 为临时表创建索引加速后续 JOIN
CREATE INDEX ON ods._sample_mimic3 (subject_id);
CREATE INDEX ON ods._sample_mimic4 (subject_id);

\echo 'Step 1 done: selected patients'
\echo 'MIMIC-III sample:'
SELECT count(*) as mimic3_patients FROM ods._sample_mimic3;
\echo 'MIMIC-IV sample:'
SELECT count(*) as mimic4_patients FROM ods._sample_mimic4;

-- ===================== Step 2: 重建有 subject_id 的临床表 =====================
-- 辅助函数：重建一张表（LIKE 建表 → INSERT SELECT → DROP 旧表 → RENAME）
-- 注意：对于分区表，LIKE 不继承分区属性，自动变为普通表

-- ============================================================
-- MIMIC-III 临床表 (o3_ 前缀，20张有 subject_id)
-- ============================================================

-- --- o3_patients (主表) ---
DROP TABLE IF EXISTS ods.o3_patients_new;
CREATE TABLE ods.o3_patients_new (LIKE ods.o3_patients INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES);
INSERT INTO ods.o3_patients_new SELECT o.* FROM ods.o3_patients o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic3);
\echo 'o3_patients: ' || ROW_COUNT || ' rows';

-- --- o3_admissions ---
DROP TABLE IF EXISTS ods.o3_admissions_new;
CREATE TABLE ods.o3_admissions_new (LIKE ods.o3_admissions INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES);
INSERT INTO ods.o3_admissions_new SELECT o.* FROM ods.o3_admissions o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic3);
\echo 'o3_admissions: ' || ROW_COUNT || ' rows';

-- --- o3_callout ---
DROP TABLE IF EXISTS ods.o3_callout_new;
CREATE TABLE ods.o3_callout_new (LIKE ods.o3_callout INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES);
INSERT INTO ods.o3_callout_new SELECT o.* FROM ods.o3_callout o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic3);
\echo 'o3_callout: ' || ROW_COUNT || ' rows';

-- --- o3_chartevents (分区表 1333 partitions, 最大) ---
DROP TABLE IF EXISTS ods.o3_chartevents_new;
CREATE TABLE ods.o3_chartevents_new (LIKE ods.o3_chartevents INCLUDING DEFAULTS INCLUDING CONSTRAINTS);
INSERT INTO ods.o3_chartevents_new SELECT o.* FROM ods.o3_chartevents o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic3);
\echo 'o3_chartevents: ' || ROW_COUNT || ' rows';

-- --- o3_cptevents ---
DROP TABLE IF EXISTS ods.o3_cptevents_new;
CREATE TABLE ods.o3_cptevents_new (LIKE ods.o3_cptevents INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES);
INSERT INTO ods.o3_cptevents_new SELECT o.* FROM ods.o3_cptevents o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic3);
\echo 'o3_cptevents: ' || ROW_COUNT || ' rows';

-- --- o3_datetimeevents (分区表) ---
DROP TABLE IF EXISTS ods.o3_datetimeevents_new;
CREATE TABLE ods.o3_datetimeevents_new (LIKE ods.o3_datetimeevents INCLUDING DEFAULTS INCLUDING CONSTRAINTS);
INSERT INTO ods.o3_datetimeevents_new SELECT o.* FROM ods.o3_datetimeevents o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic3);
\echo 'o3_datetimeevents: ' || ROW_COUNT || ' rows';

-- --- o3_diagnoses_icd ---
DROP TABLE IF EXISTS ods.o3_diagnoses_icd_new;
CREATE TABLE ods.o3_diagnoses_icd_new (LIKE ods.o3_diagnoses_icd INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES);
INSERT INTO ods.o3_diagnoses_icd_new SELECT o.* FROM ods.o3_diagnoses_icd o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic3);
\echo 'o3_diagnoses_icd: ' || ROW_COUNT || ' rows';

-- --- o3_drgcodes ---
DROP TABLE IF EXISTS ods.o3_drgcodes_new;
CREATE TABLE ods.o3_drgcodes_new (LIKE ods.o3_drgcodes INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES);
INSERT INTO ods.o3_drgcodes_new SELECT o.* FROM ods.o3_drgcodes o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic3);
\echo 'o3_drgcodes: ' || ROW_COUNT || ' rows';

-- --- o3_icustays ---
DROP TABLE IF EXISTS ods.o3_icustays_new;
CREATE TABLE ods.o3_icustays_new (LIKE ods.o3_icustays INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES);
INSERT INTO ods.o3_icustays_new SELECT o.* FROM ods.o3_icustays o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic3);
\echo 'o3_icustays: ' || ROW_COUNT || ' rows';

-- --- o3_inputevents_cv (分区表) ---
DROP TABLE IF EXISTS ods.o3_inputevents_cv_new;
CREATE TABLE ods.o3_inputevents_cv_new (LIKE ods.o3_inputevents_cv INCLUDING DEFAULTS INCLUDING CONSTRAINTS);
INSERT INTO ods.o3_inputevents_cv_new SELECT o.* FROM ods.o3_inputevents_cv o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic3);
\echo 'o3_inputevents_cv: ' || ROW_COUNT || ' rows';

-- --- o3_inputevents_mv (分区表) ---
DROP TABLE IF EXISTS ods.o3_inputevents_mv_new;
CREATE TABLE ods.o3_inputevents_mv_new (LIKE ods.o3_inputevents_mv INCLUDING DEFAULTS INCLUDING CONSTRAINTS);
INSERT INTO ods.o3_inputevents_mv_new SELECT o.* FROM ods.o3_inputevents_mv o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic3);
\echo 'o3_inputevents_mv: ' || ROW_COUNT || ' rows';

-- --- o3_labevents (分区表) ---
DROP TABLE IF EXISTS ods.o3_labevents_new;
CREATE TABLE ods.o3_labevents_new (LIKE ods.o3_labevents INCLUDING DEFAULTS INCLUDING CONSTRAINTS);
INSERT INTO ods.o3_labevents_new SELECT o.* FROM ods.o3_labevents o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic3);
\echo 'o3_labevents: ' || ROW_COUNT || ' rows';

-- --- o3_microbiologyevents ---
DROP TABLE IF EXISTS ods.o3_microbiologyevents_new;
CREATE TABLE ods.o3_microbiologyevents_new (LIKE ods.o3_microbiologyevents INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES);
INSERT INTO ods.o3_microbiologyevents_new SELECT o.* FROM ods.o3_microbiologyevents o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic3);
\echo 'o3_microbiologyevents: ' || ROW_COUNT || ' rows';

-- --- o3_noteevents (分区表, 第二大) ---
DROP TABLE IF EXISTS ods.o3_noteevents_new;
CREATE TABLE ods.o3_noteevents_new (LIKE ods.o3_noteevents INCLUDING DEFAULTS INCLUDING CONSTRAINTS);
INSERT INTO ods.o3_noteevents_new SELECT o.* FROM ods.o3_noteevents o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic3);
\echo 'o3_noteevents: ' || ROW_COUNT || ' rows';

-- --- o3_outputevents ---
DROP TABLE IF EXISTS ods.o3_outputevents_new;
CREATE TABLE ods.o3_outputevents_new (LIKE ods.o3_outputevents INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES);
INSERT INTO ods.o3_outputevents_new SELECT o.* FROM ods.o3_outputevents o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic3);
\echo 'o3_outputevents: ' || ROW_COUNT || ' rows';

-- --- o3_prescriptions ---
DROP TABLE IF EXISTS ods.o3_prescriptions_new;
CREATE TABLE ods.o3_prescriptions_new (LIKE ods.o3_prescriptions INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES);
INSERT INTO ods.o3_prescriptions_new SELECT o.* FROM ods.o3_prescriptions o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic3);
\echo 'o3_prescriptions: ' || ROW_COUNT || ' rows';

-- --- o3_procedureevents_mv (分区表) ---
DROP TABLE IF EXISTS ods.o3_procedureevents_mv_new;
CREATE TABLE ods.o3_procedureevents_mv_new (LIKE ods.o3_procedureevents_mv INCLUDING DEFAULTS INCLUDING CONSTRAINTS);
INSERT INTO ods.o3_procedureevents_mv_new SELECT o.* FROM ods.o3_procedureevents_mv o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic3);
\echo 'o3_procedureevents_mv: ' || ROW_COUNT || ' rows';

-- --- o3_procedures_icd ---
DROP TABLE IF EXISTS ods.o3_procedures_icd_new;
CREATE TABLE ods.o3_procedures_icd_new (LIKE ods.o3_procedures_icd INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES);
INSERT INTO ods.o3_procedures_icd_new SELECT o.* FROM ods.o3_procedures_icd o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic3);
\echo 'o3_procedures_icd: ' || ROW_COUNT || ' rows';

-- --- o3_services ---
DROP TABLE IF EXISTS ods.o3_services_new;
CREATE TABLE ods.o3_services_new (LIKE ods.o3_services INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES);
INSERT INTO ods.o3_services_new SELECT o.* FROM ods.o3_services o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic3);
\echo 'o3_services: ' || ROW_COUNT || ' rows';

-- --- o3_transfers ---
DROP TABLE IF EXISTS ods.o3_transfers_new;
CREATE TABLE ods.o3_transfers_new (LIKE ods.o3_transfers INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES);
INSERT INTO ods.o3_transfers_new SELECT o.* FROM ods.o3_transfers o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic3);
\echo 'o3_transfers: ' || ROW_COUNT || ' rows';

\echo '=== MIMIC-III clinical tables rebuilt ==='

-- ============================================================
-- MIMIC-IV 临床表 (o4_ 前缀，24张有 subject_id)
-- ============================================================

-- --- o4_patients (主表) ---
DROP TABLE IF EXISTS ods.o4_patients_new;
CREATE TABLE ods.o4_patients_new (LIKE ods.o4_patients INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES);
INSERT INTO ods.o4_patients_new SELECT o.* FROM ods.o4_patients o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic4);
\echo 'o4_patients: ' || ROW_COUNT || ' rows';

-- --- o4_admissions ---
DROP TABLE IF EXISTS ods.o4_admissions_new;
CREATE TABLE ods.o4_admissions_new (LIKE ods.o4_admissions INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES);
INSERT INTO ods.o4_admissions_new SELECT o.* FROM ods.o4_admissions o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic4);
\echo 'o4_admissions: ' || ROW_COUNT || ' rows';

-- --- o4_chartevents (分区表) ---
DROP TABLE IF EXISTS ods.o4_chartevents_new;
CREATE TABLE ods.o4_chartevents_new (LIKE ods.o4_chartevents INCLUDING DEFAULTS INCLUDING CONSTRAINTS);
INSERT INTO ods.o4_chartevents_new SELECT o.* FROM ods.o4_chartevents o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic4);
\echo 'o4_chartevents: ' || ROW_COUNT || ' rows';

-- --- o4_datetimeevents (分区表) ---
DROP TABLE IF EXISTS ods.o4_datetimeevents_new;
CREATE TABLE ods.o4_datetimeevents_new (LIKE ods.o4_datetimeevents INCLUDING DEFAULTS INCLUDING CONSTRAINTS);
INSERT INTO ods.o4_datetimeevents_new SELECT o.* FROM ods.o4_datetimeevents o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic4);
\echo 'o4_datetimeevents: ' || ROW_COUNT || ' rows';

-- --- o4_diagnoses_icd ---
DROP TABLE IF EXISTS ods.o4_diagnoses_icd_new;
CREATE TABLE ods.o4_diagnoses_icd_new (LIKE ods.o4_diagnoses_icd INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES);
INSERT INTO ods.o4_diagnoses_icd_new SELECT o.* FROM ods.o4_diagnoses_icd o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic4);
\echo 'o4_diagnoses_icd: ' || ROW_COUNT || ' rows';

-- --- o4_drgcodes ---
DROP TABLE IF EXISTS ods.o4_drgcodes_new;
CREATE TABLE ods.o4_drgcodes_new (LIKE ods.o4_drgcodes INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES);
INSERT INTO ods.o4_drgcodes_new SELECT o.* FROM ods.o4_drgcodes o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic4);
\echo 'o4_drgcodes: ' || ROW_COUNT || ' rows';

-- --- o4_emar (分区表) ---
DROP TABLE IF EXISTS ods.o4_emar_new;
CREATE TABLE ods.o4_emar_new (LIKE ods.o4_emar INCLUDING DEFAULTS INCLUDING CONSTRAINTS);
INSERT INTO ods.o4_emar_new SELECT o.* FROM ods.o4_emar o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic4);
\echo 'o4_emar: ' || ROW_COUNT || ' rows';

-- --- o4_emar_detail ---
DROP TABLE IF EXISTS ods.o4_emar_detail_new;
CREATE TABLE ods.o4_emar_detail_new (LIKE ods.o4_emar_detail INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES);
INSERT INTO ods.o4_emar_detail_new SELECT o.* FROM ods.o4_emar_detail o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic4);
\echo 'o4_emar_detail: ' || ROW_COUNT || ' rows';

-- --- o4_hcpcsevents ---
DROP TABLE IF EXISTS ods.o4_hcpcsevents_new;
CREATE TABLE ods.o4_hcpcsevents_new (LIKE ods.o4_hcpcsevents INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES);
INSERT INTO ods.o4_hcpcsevents_new SELECT o.* FROM ods.o4_hcpcsevents o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic4);
\echo 'o4_hcpcsevents: ' || ROW_COUNT || ' rows';

-- --- o4_icustays ---
DROP TABLE IF EXISTS ods.o4_icustays_new;
CREATE TABLE ods.o4_icustays_new (LIKE ods.o4_icustays INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES);
INSERT INTO ods.o4_icustays_new SELECT o.* FROM ods.o4_icustays o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic4);
\echo 'o4_icustays: ' || ROW_COUNT || ' rows';

-- --- o4_ingredientevents (分区表) ---
DROP TABLE IF EXISTS ods.o4_ingredientevents_new;
CREATE TABLE ods.o4_ingredientevents_new (LIKE ods.o4_ingredientevents INCLUDING DEFAULTS INCLUDING CONSTRAINTS);
INSERT INTO ods.o4_ingredientevents_new SELECT o.* FROM ods.o4_ingredientevents o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic4);
\echo 'o4_ingredientevents: ' || ROW_COUNT || ' rows';

-- --- o4_inputevents (分区表) ---
DROP TABLE IF EXISTS ods.o4_inputevents_new;
CREATE TABLE ods.o4_inputevents_new (LIKE ods.o4_inputevents INCLUDING DEFAULTS INCLUDING CONSTRAINTS);
INSERT INTO ods.o4_inputevents_new SELECT o.* FROM ods.o4_inputevents o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic4);
\echo 'o4_inputevents: ' || ROW_COUNT || ' rows';

-- --- o4_labevents (分区表) ---
DROP TABLE IF EXISTS ods.o4_labevents_new;
CREATE TABLE ods.o4_labevents_new (LIKE ods.o4_labevents INCLUDING DEFAULTS INCLUDING CONSTRAINTS);
INSERT INTO ods.o4_labevents_new SELECT o.* FROM ods.o4_labevents o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic4);
\echo 'o4_labevents: ' || ROW_COUNT || ' rows';

-- --- o4_microbiologyevents ---
DROP TABLE IF EXISTS ods.o4_microbiologyevents_new;
CREATE TABLE ods.o4_microbiologyevents_new (LIKE ods.o4_microbiologyevents INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES);
INSERT INTO ods.o4_microbiologyevents_new SELECT o.* FROM ods.o4_microbiologyevents o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic4);
\echo 'o4_microbiologyevents: ' || ROW_COUNT || ' rows';

-- --- o4_omr ---
DROP TABLE IF EXISTS ods.o4_omr_new;
CREATE TABLE ods.o4_omr_new (LIKE ods.o4_omr INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES);
INSERT INTO ods.o4_omr_new SELECT o.* FROM ods.o4_omr o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic4);
\echo 'o4_omr: ' || ROW_COUNT || ' rows';

-- --- o4_outputevents ---
DROP TABLE IF EXISTS ods.o4_outputevents_new;
CREATE TABLE ods.o4_outputevents_new (LIKE ods.o4_outputevents INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES);
INSERT INTO ods.o4_outputevents_new SELECT o.* FROM ods.o4_outputevents o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic4);
\echo 'o4_outputevents: ' || ROW_COUNT || ' rows';

-- --- o4_pharmacy ---
DROP TABLE IF EXISTS ods.o4_pharmacy_new;
CREATE TABLE ods.o4_pharmacy_new (LIKE ods.o4_pharmacy INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES);
INSERT INTO ods.o4_pharmacy_new SELECT o.* FROM ods.o4_pharmacy o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic4);
\echo 'o4_pharmacy: ' || ROW_COUNT || ' rows';

-- --- o4_poe (分区表) ---
DROP TABLE IF EXISTS ods.o4_poe_new;
CREATE TABLE ods.o4_poe_new (LIKE ods.o4_poe INCLUDING DEFAULTS INCLUDING CONSTRAINTS);
INSERT INTO ods.o4_poe_new SELECT o.* FROM ods.o4_poe o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic4);
\echo 'o4_poe: ' || ROW_COUNT || ' rows';

-- --- o4_poe_detail ---
DROP TABLE IF EXISTS ods.o4_poe_detail_new;
CREATE TABLE ods.o4_poe_detail_new (LIKE ods.o4_poe_detail INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES);
INSERT INTO ods.o4_poe_detail_new SELECT o.* FROM ods.o4_poe_detail o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic4);
\echo 'o4_poe_detail: ' || ROW_COUNT || ' rows';

-- --- o4_prescriptions ---
DROP TABLE IF EXISTS ods.o4_prescriptions_new;
CREATE TABLE ods.o4_prescriptions_new (LIKE ods.o4_prescriptions INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES);
INSERT INTO ods.o4_prescriptions_new SELECT o.* FROM ods.o4_prescriptions o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic4);
\echo 'o4_prescriptions: ' || ROW_COUNT || ' rows';

-- --- o4_procedureevents ---
DROP TABLE IF EXISTS ods.o4_procedureevents_new;
CREATE TABLE ods.o4_procedureevents_new (LIKE ods.o4_procedureevents INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES);
INSERT INTO ods.o4_procedureevents_new SELECT o.* FROM ods.o4_procedureevents o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic4);
\echo 'o4_procedureevents: ' || ROW_COUNT || ' rows';

-- --- o4_procedures_icd ---
DROP TABLE IF EXISTS ods.o4_procedures_icd_new;
CREATE TABLE ods.o4_procedures_icd_new (LIKE ods.o4_procedures_icd INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES);
INSERT INTO ods.o4_procedures_icd_new SELECT o.* FROM ods.o4_procedures_icd o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic4);
\echo 'o4_procedures_icd: ' || ROW_COUNT || ' rows';

-- --- o4_services ---
DROP TABLE IF EXISTS ods.o4_services_new;
CREATE TABLE ods.o4_services_new (LIKE ods.o4_services INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES);
INSERT INTO ods.o4_services_new SELECT o.* FROM ods.o4_services o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic4);
\echo 'o4_services: ' || ROW_COUNT || ' rows';

-- --- o4_transfers ---
DROP TABLE IF EXISTS ods.o4_transfers_new;
CREATE TABLE ods.o4_transfers_new (LIKE ods.o4_transfers INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES);
INSERT INTO ods.o4_transfers_new SELECT o.* FROM ods.o4_transfers o WHERE o.subject_id IN (SELECT subject_id FROM ods._sample_mimic4);
\echo 'o4_transfers: ' || ROW_COUNT || ' rows';

\echo '=== MIMIC-IV clinical tables rebuilt ==='

-- ===================== Step 3: 交换表名 =====================
-- 对于分区表：先 DROP 分区父表（会级联删除所有子分区），再 RENAME
-- 对于非分区表：直接 RENAME

\echo '=== Swapping tables ==='

-- === MIMIC-III ===
-- 分区表需要 CASCADE 删除子分区
DROP TABLE IF EXISTS ods.o3_chartevents CASCADE;
DROP TABLE IF EXISTS ods.o3_datetimeevents CASCADE;
DROP TABLE IF EXISTS ods.o3_inputevents_cv CASCADE;
DROP TABLE IF EXISTS ods.o3_inputevents_mv CASCADE;
DROP TABLE IF EXISTS ods.o3_labevents CASCADE;
DROP TABLE IF EXISTS ods.o3_noteevents CASCADE;
DROP TABLE IF EXISTS ods.o3_procedureevents_mv CASCADE;

-- 非分区表
DROP TABLE IF EXISTS ods.o3_patients CASCADE;
DROP TABLE IF EXISTS ods.o3_admissions CASCADE;
DROP TABLE IF EXISTS ods.o3_callout CASCADE;
DROP TABLE IF EXISTS ods.o3_cptevents CASCADE;
DROP TABLE IF EXISTS ods.o3_diagnoses_icd CASCADE;
DROP TABLE IF EXISTS ods.o3_drgcodes CASCADE;
DROP TABLE IF EXISTS ods.o3_icustays CASCADE;
DROP TABLE IF EXISTS ods.o3_microbiologyevents CASCADE;
DROP TABLE IF EXISTS ods.o3_outputevents CASCADE;
DROP TABLE IF EXISTS ods.o3_prescriptions CASCADE;
DROP TABLE IF EXISTS ods.o3_procedures_icd CASCADE;
DROP TABLE IF EXISTS ods.o3_services CASCADE;
DROP TABLE IF EXISTS ods.o3_transfers CASCADE;

-- RENAME all _new to final name
ALTER TABLE ods.o3_patients_new RENAME TO o3_patients;
ALTER TABLE ods.o3_admissions_new RENAME TO o3_admissions;
ALTER TABLE ods.o3_callout_new RENAME TO o3_callout;
ALTER TABLE ods.o3_chartevents_new RENAME TO o3_chartevents;
ALTER TABLE ods.o3_cptevents_new RENAME TO o3_cptevents;
ALTER TABLE ods.o3_datetimeevents_new RENAME TO o3_datetimeevents;
ALTER TABLE ods.o3_diagnoses_icd_new RENAME TO o3_diagnoses_icd;
ALTER TABLE ods.o3_drgcodes_new RENAME TO o3_drgcodes;
ALTER TABLE ods.o3_icustays_new RENAME TO o3_icustays;
ALTER TABLE ods.o3_inputevents_cv_new RENAME TO o3_inputevents_cv;
ALTER TABLE ods.o3_inputevents_mv_new RENAME TO o3_inputevents_mv;
ALTER TABLE ods.o3_labevents_new RENAME TO o3_labevents;
ALTER TABLE ods.o3_microbiologyevents_new RENAME TO o3_microbiologyevents;
ALTER TABLE ods.o3_noteevents_new RENAME TO o3_noteevents;
ALTER TABLE ods.o3_outputevents_new RENAME TO o3_outputevents;
ALTER TABLE ods.o3_prescriptions_new RENAME TO o3_prescriptions;
ALTER TABLE ods.o3_procedureevents_mv_new RENAME TO o3_procedureevents_mv;
ALTER TABLE ods.o3_procedures_icd_new RENAME TO o3_procedures_icd;
ALTER TABLE ods.o3_services_new RENAME TO o3_services;
ALTER TABLE ods.o3_transfers_new RENAME TO o3_transfers;

\echo '=== MIMIC-III tables swapped ==='

-- === MIMIC-IV ===
-- 分区表 CASCADE
DROP TABLE IF EXISTS ods.o4_chartevents CASCADE;
DROP TABLE IF EXISTS ods.o4_datetimeevents CASCADE;
DROP TABLE IF EXISTS ods.o4_emar CASCADE;
DROP TABLE IF EXISTS ods.o4_ingredientevents CASCADE;
DROP TABLE IF EXISTS ods.o4_inputevents CASCADE;
DROP TABLE IF EXISTS ods.o4_labevents CASCADE;
DROP TABLE IF EXISTS ods.o4_poe CASCADE;

-- 非分区表
DROP TABLE IF EXISTS ods.o4_patients CASCADE;
DROP TABLE IF EXISTS ods.o4_admissions CASCADE;
DROP TABLE IF EXISTS ods.o4_diagnoses_icd CASCADE;
DROP TABLE IF EXISTS ods.o4_drgcodes CASCADE;
DROP TABLE IF EXISTS ods.o4_emar_detail CASCADE;
DROP TABLE IF EXISTS ods.o4_hcpcsevents CASCADE;
DROP TABLE IF EXISTS ods.o4_icustays CASCADE;
DROP TABLE IF EXISTS ods.o4_microbiologyevents CASCADE;
DROP TABLE IF EXISTS ods.o4_omr CASCADE;
DROP TABLE IF EXISTS ods.o4_outputevents CASCADE;
DROP TABLE IF EXISTS ods.o4_pharmacy CASCADE;
DROP TABLE IF EXISTS ods.o4_poe_detail CASCADE;
DROP TABLE IF EXISTS ods.o4_prescriptions CASCADE;
DROP TABLE IF EXISTS ods.o4_procedureevents CASCADE;
DROP TABLE IF EXISTS ods.o4_procedures_icd CASCADE;
DROP TABLE IF EXISTS ods.o4_services CASCADE;
DROP TABLE IF EXISTS ods.o4_transfers CASCADE;

-- RENAME all _new to final name
ALTER TABLE ods.o4_patients_new RENAME TO o4_patients;
ALTER TABLE ods.o4_admissions_new RENAME TO o4_admissions;
ALTER TABLE ods.o4_chartevents_new RENAME TO o4_chartevents;
ALTER TABLE ods.o4_datetimeevents_new RENAME TO o4_datetimeevents;
ALTER TABLE ods.o4_diagnoses_icd_new RENAME TO o4_diagnoses_icd;
ALTER TABLE ods.o4_drgcodes_new RENAME TO o4_drgcodes;
ALTER TABLE ods.o4_emar_new RENAME TO o4_emar;
ALTER TABLE ods.o4_emar_detail_new RENAME TO o4_emar_detail;
ALTER TABLE ods.o4_hcpcsevents_new RENAME TO o4_hcpcsevents;
ALTER TABLE ods.o4_icustays_new RENAME TO o4_icustays;
ALTER TABLE ods.o4_ingredientevents_new RENAME TO o4_ingredientevents;
ALTER TABLE ods.o4_inputevents_new RENAME TO o4_inputevents;
ALTER TABLE ods.o4_labevents_new RENAME TO o4_labevents;
ALTER TABLE ods.o4_microbiologyevents_new RENAME TO o4_microbiologyevents;
ALTER TABLE ods.o4_omr_new RENAME TO o4_omr;
ALTER TABLE ods.o4_outputevents_new RENAME TO o4_outputevents;
ALTER TABLE ods.o4_pharmacy_new RENAME TO o4_pharmacy;
ALTER TABLE ods.o4_poe_new RENAME TO o4_poe;
ALTER TABLE ods.o4_poe_detail_new RENAME TO o4_poe_detail;
ALTER TABLE ods.o4_prescriptions_new RENAME TO o4_prescriptions;
ALTER TABLE ods.o4_procedureevents_new RENAME TO o4_procedureevents;
ALTER TABLE ods.o4_procedures_icd_new RENAME TO o4_procedures_icd;
ALTER TABLE ods.o4_services_new RENAME TO o4_services;
ALTER TABLE ods.o4_transfers_new RENAME TO o4_transfers;

\echo '=== MIMIC-IV tables swapped ==='

-- ===================== Step 4: 恢复默认值和元数据列 =====================
-- 所有表的元数据列需要恢复 DEFAULT（如果有）
-- 由于 CTAS 重建后 id 列没有 serial/identity，需要处理

-- o3_patients: id 列需要 serial
ALTER TABLE ods.o3_patients ALTER COLUMN id SET DEFAULT nextval('ods.o3_patients_id_seq');
-- 如果序列不存在则创建
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_sequences WHERE schemaname='ods' AND sequencename='o3_patients_id_seq') THEN
        CREATE SEQUENCE ods.o3_patients_id_seq OWNED BY ods.o3_patients.id;
        SELECT setval('ods.o3_patients_id_seq', (SELECT COALESCE(MAX(id),0) FROM ods.o3_patients));
    END IF;
END $$;

-- o4_patients: id 列需要 serial
ALTER TABLE ods.o4_patients ALTER COLUMN id SET DEFAULT nextval('ods.o4_patients_id_seq');
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_sequences WHERE schemaname='ods' AND sequencename='o4_patients_id_seq') THEN
        CREATE SEQUENCE ods.o4_patients_id_seq OWNED BY ods.o4_patients.id;
        SELECT setval('ods.o4_patients_id_seq', (SELECT COALESCE(MAX(id),0) FROM ods.o4_patients));
    END IF;
END $$;

-- ===================== Step 5: 创建索引 =====================
-- 为新表的关键列创建索引
CREATE INDEX IF NOT EXISTS idx_o3_admissions_subject ON ods.o3_admissions (subject_id);
CREATE INDEX IF NOT EXISTS idx_o3_icustays_subject ON ods.o3_icustays (subject_id);
CREATE INDEX IF NOT EXISTS idx_o3_chartevents_subject ON ods.o3_chartevents (subject_id);
CREATE INDEX IF NOT EXISTS idx_o3_labevents_subject ON ods.o3_labevents (subject_id);
CREATE INDEX IF NOT EXISTS idx_o3_noteevents_subject ON ods.o3_noteevents (subject_id);

CREATE INDEX IF NOT EXISTS idx_o4_admissions_subject ON ods.o4_admissions (subject_id);
CREATE INDEX IF NOT EXISTS idx_o4_icustays_subject ON ods.o4_icustays (subject_id);
CREATE INDEX IF NOT EXISTS idx_o4_chartevents_subject ON ods.o4_chartevents (subject_id);
CREATE INDEX IF NOT EXISTS idx_o4_labevents_subject ON ods.o4_labevents (subject_id);
CREATE INDEX IF NOT EXISTS idx_o4_emar_subject ON ods.o4_emar (subject_id);
CREATE INDEX IF NOT EXISTS idx_o4_emar_detail_subject ON ods.o4_emar_detail (subject_id);

-- ===================== Step 6: VACUUM 和统计 =====================
VACUUM ANALYZE ods;

-- ===================== Step 7: 清理临时表 =====================
DROP TABLE IF EXISTS ods._sample_mimic3;
DROP TABLE IF EXISTS ods._sample_mimic4;

-- ===================== Step 8: 结果验证 =====================
\echo '========== Data Reduction Results =========='
\echo 'MIMIC-III tables:'
SELECT 'o3_patients' as tbl, count(*) as rows FROM ods.o3_patients
UNION ALL SELECT 'o3_admissions', count(*) FROM ods.o3_admissions
UNION ALL SELECT 'o3_icustays', count(*) FROM ods.o3_icustays
UNION ALL SELECT 'o3_chartevents', count(*) FROM ods.o3_chartevents
UNION ALL SELECT 'o3_labevents', count(*) FROM ods.o3_labevents
UNION ALL SELECT 'o3_noteevents', count(*) FROM ods.o3_noteevents
UNION ALL SELECT 'o3_prescriptions', count(*) FROM ods.o3_prescriptions
UNION ALL SELECT 'o3_inputevents_cv', count(*) FROM ods.o3_inputevents_cv
UNION ALL SELECT 'o3_outputevents', count(*) FROM ods.o3_outputevents
UNION ALL SELECT 'o3_diagnoses_icd', count(*) FROM ods.o3_diagnoses_icd
ORDER BY rows DESC;

\echo 'MIMIC-IV tables:'
SELECT 'o4_patients' as tbl, count(*) as rows FROM ods.o4_patients
UNION ALL SELECT 'o4_admissions', count(*) FROM ods.o4_admissions
UNION ALL SELECT 'o4_icustays', count(*) FROM ods.o4_icustays
UNION ALL SELECT 'o4_chartevents', count(*) FROM ods.o4_chartevents
UNION ALL SELECT 'o4_labevents', count(*) FROM ods.o4_labevents
UNION ALL SELECT 'o4_emar', count(*) FROM ods.o4_emar
UNION ALL SELECT 'o4_emar_detail', count(*) FROM ods.o4_emar_detail
UNION ALL SELECT 'o4_poe', count(*) FROM ods.o4_poe
UNION ALL SELECT 'o4_prescriptions', count(*) FROM ods.o4_prescriptions
UNION ALL SELECT 'o4_pharmacy', count(*) FROM ods.o4_pharmacy
UNION ALL SELECT 'o4_diagnoses_icd', count(*) FROM ods.o4_diagnoses_icd
ORDER BY rows DESC;

\echo '========== Reduction Complete =========='
