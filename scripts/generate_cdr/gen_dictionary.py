"""Generate CDR dictionary/reference tables (5 tables)."""

import random
from .config import ICD10, LAB_PANELS
from .utils import Ctx, write_csv


def generate_dict_tables(ctx: Ctx, output_dir: str):
    """Generate all 5 dictionary tables."""
    _gen_dict_icd_diagnosis(ctx, output_dir)
    _gen_dict_icd_procedure(ctx, output_dir)
    _gen_dict_lab_item(ctx, output_dir)
    _gen_dict_item(ctx, output_dir)
    _gen_dict_procedure_code(ctx, output_dir)


def _gen_dict_icd_diagnosis(ctx, output_dir):
    rows = []
    for code, name in ICD10:
        rows.append((
            ctx.seq.next(), code, 10,
            name[:20], name,
            'MIMIC4', 'etl', '2024-01-01 00:00:00', '', '', 'f', 1
        ))
    write_csv('c_dict_icd_diagnosis.csv',
              ['id', 'icd_code', 'icd_version', 'short_title', 'long_title',
               'source_dataset', 'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'], rows, output_dir)


def _gen_dict_icd_procedure(ctx, output_dir):
    procs = [
        ('00.60', '冠状动脉支架置入术'), ('39.61', '体外循环心脏直视手术'),
        ('45.13', '胃镜检查'), ('51.22', '腹腔镜胆囊切除术'),
        ('79.30', '骨折开放复位术'), ('81.51', '全髋关节置换术'),
        ('84.10', '截肢术'), ('86.22', '皮肤病变切除术'),
        ('87.03', '腹部CT检查'), ('88.01', '头颅CT检查'),
        ('88.91', '头颅MRI检查'), ('93.53', '呼吸机治疗'),
        ('96.04', '气管内插管'), ('96.72', '持续机械通气'),
        ('99.60', '心肺复苏'), ('37.21', '右心导管检查'),
        ('35.20', '二尖瓣修复术'), ('36.07', '冠状动脉腔内血管成形术'),
        ('40.11', '淋巴结活检'), ('55.02', '肾穿刺活检'),
        ('57.94', '膀胱镜检查'), ('68.40', '全子宫切除术'),
        ('80.51', '关节镜下半月板修整术'), ('85.43', '单侧乳腺切除术'),
    ]
    rows = []
    for code, name in procs:
        rows.append((
            ctx.seq.next(), code, 10,
            name[:20], name,
            'MIMIC4', 'etl', '2024-01-01 00:00:00', '', '', 'f', 1
        ))
    write_csv('c_dict_icd_procedure.csv',
              ['id', 'icd_code', 'icd_version', 'short_title', 'long_title',
               'source_dataset', 'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'], rows, output_dir)


def _gen_dict_lab_item(ctx, output_dir):
    rows = []
    for panel in LAB_PANELS:
        for item in panel['items']:
            rows.append((
                ctx.seq.next(), len(rows) + 1000,
                item[1], panel['specimen'], 'LAB',
                '', 'MIMIC4',
                'etl', '2024-01-01 00:00:00', '', '', 'f', 1
            ))
    write_csv('c_dict_lab_item.csv',
              ['id', 'itemid', 'label', 'fluid', 'category', 'loinc_code',
               'source_dataset', 'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'], rows, output_dir)


def _gen_dict_item(ctx, output_dir):
    items = [
        ('Heart Rate', 'HR', 'Routine Vital Signs', 'bpm', 'Numeric'),
        ('Respiratory Rate', 'RR', 'Routine Vital Signs', 'insp/min', 'Numeric'),
        ('SpO2', 'SpO2', 'Routine Vital Signs', '%', 'Numeric'),
        ('Temperature', 'Temp', 'Routine Vital Signs', 'Deg C', 'Numeric'),
        ('Systolic BP', 'SBP', 'Routine Vital Signs', 'mmHg', 'Numeric'),
        ('Diastolic BP', 'DBP', 'Routine Vital Signs', 'mmHg', 'Numeric'),
        ('GCS - Eye Opening', 'GCS Eye', 'Neurological', 'none', 'Numeric'),
        ('GCS - Verbal', 'GCS Verbal', 'Neurological', 'none', 'Numeric'),
        ('GCS - Motor', 'GCS Motor', 'Neurological', 'none', 'Numeric'),
        ('FiO2', 'FiO2', 'Respiratory', '%', 'Numeric'),
        ('Tidal Volume', 'TV', 'Respiratory', 'mL', 'Numeric'),
        ('PEEP', 'PEEP', 'Respiratory', 'cmH2O', 'Numeric'),
        ('Urine Output', 'UrineOut', 'Output', 'mL', 'Numeric'),
        ('Pain Level', 'Pain', 'Routine Vital Signs', 'none', 'Numeric'),
        ('Blood Glucose', 'BG', 'Point of Care', 'mg/dL', 'Numeric'),
        ('Central Venous Pressure', 'CVP', 'Hemodynamics', 'mmHg', 'Numeric'),
        ('Cardiac Output', 'CO', 'Hemodynamics', 'L/min', 'Numeric'),
        ('Height', 'Ht', 'Routine Vital Signs', 'cm', 'Numeric'),
        ('Weight', 'Wt', 'Routine Vital Signs', 'kg', 'Numeric'),
        ('O2 Flow', 'O2Flow', 'Respiratory', 'L/min', 'Numeric'),
    ]
    rows = []
    for i, (label, abbr, cat, unit, ptype) in enumerate(items):
        rows.append((
            ctx.seq.next(), 220000 + i, label, abbr, cat, unit, ptype,
            'metavision', 'chartevents',
            0.0, 0.0,
            'MIMIC4', 'etl', '2024-01-01 00:00:00', '', '', 'f', 1
        ))
    write_csv('c_dict_item.csv',
              ['id', 'itemid', 'label', 'abbreviation', 'category', 'unit_name', 'param_type',
               'dbsource', 'linksto', 'low_normal_value', 'high_normal_value',
               'source_dataset', 'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'], rows, output_dir)


def _gen_dict_procedure_code(ctx, output_dir):
    codes = [
        ('99213', 'CPT', 'Evaluation & Management', '99200-99215',
         'Office Visit', '99211-99215', 'Established Patient Office Visit',
         'Office or other outpatient visit for established patient'),
        ('99223', 'CPT', 'Evaluation & Management', '99221-99223',
         'Initial Hospital Care', '99221-99223', 'Initial Hospital Care, High Complexity',
         'Initial hospital care, per day, for the evaluation and management of a patient'),
        ('99232', 'CPT', 'Evaluation & Management', '99231-99233',
         'Subsequent Hospital Care', '99231-99233', 'Subsequent Hospital Care, Moderate Complexity',
         'Subsequent hospital care, per day, for established patient'),
        ('99238', 'CPT', 'Evaluation & Management', '99238-99239',
         'Hospital Discharge Day', '99238-99239', 'Hospital Discharge Day Management',
         'Hospital discharge day management'),
        ('71046', 'CPT', 'Radiology', '71000-71049',
         'Chest X-ray', '71045-71048', 'Chest X-ray, 2 Views',
         'Radiologic examination, chest, 2 views'),
        ('71250', 'CPT', 'Radiology', '71250-71275',
         'Chest CT', '71250-71260', 'Chest CT without Contrast',
         'Computed tomography, thorax without contrast'),
        ('70553', 'CPT', 'Radiology', '70551-70553',
         'Brain MRI', '70551-70553', 'Brain MRI with and without Contrast',
         'Magnetic resonance imaging, brain, without contrast material'),
        ('36415', 'CPT', 'Pathology & Laboratory', '36415-36416',
         'Venipuncture', '36415-36416', 'Venipuncture',
         'Collection of venous blood by venipuncture'),
        ('85025', 'CPT', 'Pathology & Laboratory', '85002-85999',
         'CBC', '85025-85027', 'Complete Blood Count',
         'Blood count; complete (CBC), automated'),
        ('80053', 'CPT', 'Pathology & Laboratory', '80000-80099',
         'Comprehensive Metabolic Panel', '80053', 'Comprehensive Metabolic Panel',
         'Comprehensive metabolic panel'),
    ]
    rows = []
    for code, ctype, cat, sec_range, sec_hdr, sub_range, sub_hdr, desc in codes:
        rows.append((
            ctx.seq.next(), code, ctype, cat, sec_range, sec_hdr,
            sub_range, sub_hdr, desc[:80], desc,
            'MIMIC4', 'etl', '2024-01-01 00:00:00', '', '', 'f', 1
        ))
    write_csv('c_dict_procedure_code.csv',
              ['id', 'code', 'code_type', 'category', 'section_range', 'section_header',
               'subsection_range', 'subsection_header', 'short_description', 'long_description',
               'source_dataset', 'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'], rows, output_dir)
