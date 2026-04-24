"""Generate pharmacy/order tables: c_pharmacy_order, c_provider_order,
c_med_admin, c_drg_code, c_cpt_event."""

import random
from datetime import timedelta
from .config import MEDS, DEPTS
from .utils import (Ctx, rand_float, fmt_dt, fmt_bool, write_csv)


def generate_pharmacy(ctx: Ctx, output_dir: str):
    _gen_pharmacy_order(ctx, output_dir)
    _gen_provider_order(ctx, output_dir)
    _gen_med_admin(ctx, output_dir)
    _gen_drg_code(ctx, output_dir)
    _gen_cpt_event(ctx, output_dir)


def _gen_pharmacy_order(ctx, output_dir):
    rows = []
    routes = ['口服', '静脉滴注', '静脉注射', '皮下注射', '吸入', '舌下含服', '外用']
    frequencies = ['QD', 'BID', 'TID', 'QID', 'Q8H', 'Q12H', 'PRN', 'QN']
    statuses = ['ACTIVE', 'DISCONTINUED', 'COMPLETED', 'PENDING']

    for enc in ctx.encounters:
        disease_meds = enc['disease'].get('meds', [])
        n_orders = random.randint(1, 4)
        for _ in range(n_orders):
            oid = ctx.seq.next()
            if disease_meds and random.random() < 0.7:
                med_name = random.choice(disease_meds)
                med = next((m for m in MEDS if med_name in m[1]), random.choice(MEDS))
            else:
                med = random.choice(MEDS)

            start = enc['admit'] + timedelta(hours=random.randint(0, 12))
            is_inpatient = enc['encounter_type'] in ('INPATIENT', 'EMERGENCY')
            end = start + timedelta(days=random.randint(1, 14)) if is_inpatient else None
            status = random.choice(statuses)

            rows.append((
                oid, enc['id'], enc['patient_id'],
                random.randint(10000, 99999),  # pharmacy_id
                f'POE{oid:08d}',
                med[1],  # medication
                random.choice(['PROCEDURE', 'MEDICATION']),
                status,
                fmt_dt(start), fmt_dt(end) if end else '',
                med[3],  # route
                random.choice(frequencies),
                random.choice(['', 'BOLUS', 'CONTINUOUS', 'INFUSION']),
                random.choice(['1', '2', '3', '4']),
                'MIMIC4', str(oid + 120000),
                'etl', '2024-01-01 00:00:00', '', '', 'f', enc['org_id']
            ))
    write_csv('c_pharmacy_order.csv',
              ['id', 'encounter_id', 'patient_id', 'pharmacy_id',
               'poe_id', 'medication', 'proc_type', 'status',
               'start_time', 'end_time', 'route', 'frequency',
               'infusion_type', 'doses_per_24_hrs',
               'source_dataset', 'source_id',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'],
              rows, output_dir)


def _gen_provider_order(ctx, output_dir):
    rows = []
    order_types = ['Medication', 'Lab', 'Imaging', 'Nursing', 'Diet', 'Consult', 'Respiratory']
    transaction_types = ['RENEW', 'FIRST', 'DISCONTINUE', 'REWRITE']

    for enc in ctx.encounters:
        n_orders = random.randint(2, 6)
        for _ in range(n_orders):
            oid = ctx.seq.next()
            otype = random.choice(order_types)
            poe_id = f'POE{random.randint(10000000, 99999999)}'
            order_time = enc['admit'] + timedelta(hours=random.randint(0, enc.get('los_days', 1) * 24))

            rows.append((
                oid, enc['id'], enc['patient_id'],
                poe_id, random.randint(1, 5),
                fmt_dt(order_time),
                otype, random.choice(['', f'{otype} - {random.choice(DEPTS)[1]}']),
                random.choice(transaction_types),
                '', '',
                random.choice([f'PROV{i:04d}' for i in range(1, 51)]),
                random.choice(['COMPLETED', 'PENDING', 'ACTIVE']),
                '',  # order_detail JSONB
                'MIMIC4', str(oid + 130000),
                'etl', '2024-01-01 00:00:00', '', '', 'f', enc['org_id']
            ))
    write_csv('c_provider_order.csv',
              ['id', 'encounter_id', 'patient_id',
               'poe_id', 'poe_seq', 'order_time',
               'order_type', 'order_subtype',
               'transaction_type', 'discontinue_of_poe_id', 'discontinued_by_poe_id',
               'order_provider_id', 'order_status', 'order_detail',
               'source_dataset', 'source_id',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'],
              rows, output_dir)


def _gen_med_admin(ctx, output_dir):
    rows = []
    events = ['Administered', 'Applied', 'Started', 'Stopped', 'Delayed', ' Held']

    for enc in ctx.encounters:
        disease_meds = enc['disease'].get('meds', [])
        n_admins = random.randint(1, 5)
        for _ in range(n_admins):
            oid = ctx.seq.next()
            if disease_meds and random.random() < 0.7:
                med_name = random.choice(disease_meds)
                med = next((m for m in MEDS if med_name in m[1]), random.choice(MEDS))
            else:
                med = random.choice(MEDS)

            chart_time = enc['admit'] + timedelta(hours=random.randint(0, enc.get('los_days', 1) * 24))
            dose = med[2]
            unit = ''
            for suffix in ['mg', 'μg', 'IU', 'g']:
                if med[2].endswith(suffix):
                    unit = suffix
                    break

            rows.append((
                oid, enc['id'], enc['patient_id'],
                f'EMAR{oid:08d}',
                f'POE{random.randint(10000000, 99999999)}',
                random.randint(10000, 99999),
                fmt_dt(chart_time),
                med[1],  # medication
                random.choice(events),
                fmt_dt(chart_time - timedelta(minutes=random.randint(0, 60))),
                dose, unit,
                med[3],  # route
                '', '',  # infusion_rate, site
                '',  # detail JSONB
                'MIMIC4', str(oid + 140000),
                'etl', '2024-01-01 00:00:00', '', '', 'f', enc['org_id']
            ))
    write_csv('c_med_admin.csv',
              ['id', 'encounter_id', 'patient_id',
               'emar_id', 'poe_id', 'pharmacy_id',
               'chart_time', 'medication', 'event_txt',
               'schedule_time', 'dose_given', 'dose_given_unit',
               'route', 'infusion_rate', 'site', 'detail',
               'source_dataset', 'source_id',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'],
              rows, output_dir)


def _gen_drg_code(ctx, output_dir):
    rows = []
    drg_types = ['APR', 'HCFA', 'MS']
    drg_codes = [
        ('190', 'COPD W CC/MCC', 2, 2), ('194', 'SIMPLE PNEUMONIA W CC', 2, 1),
        ('291', 'HEART FAILURE W CC/MCC', 2, 2), ('312', 'SYNCOPE W CC/MCC', 2, 1),
        ('392', 'ESOPHAGITIS GASTROENTERITIS W CC', 1, 1),
        ('470', 'MAJOR JOINT REPLACEMENT W/O MCC', 1, 0),
        ('690', 'KIDNEY & URINARY TRACT INFECTIONS W CC', 2, 1),
        ('698', 'OTHER KIDNEY & URINARY TRACT DIAGNOSES W CC', 2, 1),
        ('766', 'OTHER DIGESTIVE SYSTEM O.R. PROCEDURES W CC', 2, 1),
        ('885', 'OTHER O.R. PROCEDURES FOR INJURIES W CC', 2, 1),
    ]

    for enc in ctx.encounters:
        if enc['encounter_type'] in ('INPATIENT', 'EMERGENCY'):
            n_drgs = random.choices([1, 2], weights=[70, 30])[0]
            for _ in range(n_drgs):
                oid = ctx.seq.next()
                drg = random.choice(drg_codes)
                rows.append((
                    oid, enc['id'], enc['patient_id'],
                    random.choice(drg_types), drg[0], drg[1],
                    drg[2], drg[3],
                    'MIMIC4', str(oid + 150000),
                    'etl', '2024-01-01 00:00:00', '', '', 'f', enc['org_id']
                ))
    write_csv('c_drg_code.csv',
              ['id', 'encounter_id', 'patient_id',
               'drg_type', 'drg_code', 'description',
               'drg_severity', 'drg_mortality',
               'source_dataset', 'source_id',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'],
              rows, output_dir)


def _gen_cpt_event(ctx, output_dir):
    rows = []
    cpt_codes = [
        ('99213', 'Office Visit', 'Evaluation & Management', 'Established Patient Office Visit'),
        ('99223', 'Initial Hospital Care', 'Evaluation & Management', 'Initial Hospital Care High Complexity'),
        ('99232', 'Subsequent Hospital Care', 'Evaluation & Management', 'Subsequent Hospital Care Moderate'),
        ('99238', 'Hospital Discharge Day', 'Evaluation & Management', 'Hospital Discharge Day Management'),
        ('71046', 'Chest X-ray 2 Views', 'Radiology', 'Chest X-ray Frontal & Lateral'),
        ('71250', 'Chest CT W/O Contrast', 'Radiology', 'Chest CT Without Contrast'),
        ('70553', 'Brain MRI W & W/O Contrast', 'Radiology', 'Brain MRI'),
        ('36415', 'Venipuncture', 'Pathology & Laboratory', 'Routine Venipuncture'),
        ('85025', 'CBC', 'Pathology & Laboratory', 'Complete Blood Count Automated'),
        ('80053', 'Comprehensive Metabolic Panel', 'Pathology & Laboratory', 'CMP'),
        ('93000', 'ECG', 'Cardiology', 'Electrocardiogram'),
        ('43239', 'EGD With Biopsy', 'Gastroenterology', 'Upper GI Endoscopy With Biopsy'),
        ('45378', 'Colonoscopy', 'Gastroenterology', 'Diagnostic Colonoscopy'),
        ('93306', 'Echocardiogram', 'Cardiology', 'Transthoracic Echocardiography'),
    ]
    cost_centers = ['I', 'O', 'E', 'P']

    for enc in ctx.encounters:
        if enc['encounter_type'] in ('INPATIENT', 'EMERGENCY'):
            n_cpts = random.randint(1, 4)
        elif random.random() < 0.3:
            n_cpts = random.randint(1, 2)
        else:
            continue

        for _ in range(n_cpts):
            oid = ctx.seq.next()
            cpt = random.choice(cpt_codes)
            chart_date = enc['admit'] + timedelta(hours=random.randint(0, enc.get('los_days', 1) * 24))

            rows.append((
                oid, enc['id'], enc['patient_id'],
                fmt_dt(chart_date), cpt[0],
                cpt[2], cpt[2], cpt[3],
                random.choice(cost_centers),
                random.randint(1, 5),
                'MIMIC4', str(oid + 160000),
                'etl', '2024-01-01 00:00:00', '', '', 'f', enc['org_id']
            ))
    write_csv('c_cpt_event.csv',
              ['id', 'encounter_id', 'patient_id',
               'chart_date', 'cpt_code',
               'section_header', 'subsection_header', 'description',
               'cost_center', 'seq_num',
               'source_dataset', 'source_id',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'],
              rows, output_dir)
