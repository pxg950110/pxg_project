"""Generate clinical tables: c_diagnosis, c_lab_test, c_lab_panel, c_medication, c_vital_sign."""

import random
from datetime import timedelta
from .config import (ICD10, LAB_PANELS, MEDS, VITAL_TYPES, DISEASES)
from .utils import (Ctx, rand_datetime, rand_float, fmt_dt, fmt_bool,
                    write_csv)


def generate_clinical(ctx: Ctx, output_dir: str):
    _gen_diagnosis(ctx, output_dir)
    _gen_lab_test_panel(ctx, output_dir)
    _gen_medication(ctx, output_dir)
    _gen_vital_sign(ctx, output_dir)


def _gen_diagnosis(ctx, output_dir):
    rows = []
    for enc in ctx.encounters:
        disease = enc['disease']
        # Main diagnosis
        oid = ctx.seq.next()
        rows.append((
            oid, '2024-01-01 00:00:00', 'system', 'f', enc['org_id'],
            '', '',
            disease['icd'], disease['name'], 'MAIN',
            enc['id'], enc['patient_id'],
            10
        ))
        # Secondary diagnoses (1-3)
        n_sec = random.choices([0, 1, 2, 3], weights=[30, 35, 25, 10])[0]
        other_icds = [c for c in ICD10 if c[0] != disease['icd']]
        for j in range(n_sec):
            oid = ctx.seq.next()
            icd = random.choice(other_icds)
            rows.append((
                oid, '2024-01-01 00:00:00', 'system', 'f', enc['org_id'],
                '', '',
                icd[0], icd[1], 'SECONDARY',
                enc['id'], enc['patient_id'],
                10
            ))
        # Admission/discharge diagnosis for inpatients
        if enc['encounter_type'] in ('INPATIENT', 'EMERGENCY'):
            for dtype in ['ADMISSION', 'DISCHARGE']:
                oid = ctx.seq.next()
                rows.append((
                    oid, '2024-01-01 00:00:00', 'system', 'f', enc['org_id'],
                    '', '',
                    disease['icd'], disease['name'], dtype,
                    enc['id'], enc['patient_id'],
                    10
                ))
    write_csv('c_diagnosis.csv',
              ['id', 'created_at', 'created_by', 'is_deleted', 'org_id',
               'updated_at', 'updated_by',
               'diagnosis_code', 'diagnosis_name', 'diagnosis_type',
               'encounter_id', 'patient_id', 'icd_version'],
              rows, output_dir)


def _gen_lab_test_panel(ctx, output_dir):
    test_rows = []
    panel_rows = []
    for enc in ctx.encounters:
        disease = enc['disease']
        panel_names = disease.get('labs', ['血常规'])
        # Maybe add extra labs
        if random.random() < 0.3:
            extra = random.choice(LAB_PANELS)
            if extra['name'] not in panel_names:
                panel_names.append(extra['name'])

        for pname in panel_names:
            panel_def = next((p for p in LAB_PANELS if p['name'] == pname), None)
            if not panel_def:
                continue

            tid = ctx.seq.next()
            admit = enc['admit']
            ordered = rand_datetime()
            collected = random.choice([None, ordered]) if enc['encounter_type'] == 'OUTPATIENT' else ordered
            reported = collected or ordered
            if collected:
                reported = collected + timedelta(hours=random.randint(1, 24))

            test_rows.append((
                tid, enc['id'], enc['patient_id'],
                panel_def['code'], panel_def['name'], panel_def['specimen'],
                fmt_dt(ordered), fmt_dt(collected), fmt_dt(reported),
                'REPORTED',
                random.choice(['DOC001', 'DOC002', 'DOC003']),
                'system', '2024-01-01 00:00:00', '', '', 'f', enc['org_id']
            ))
            ctx.lab_tests.append({'id': tid, 'encounter_id': enc['id'],
                                  'patient_id': enc['patient_id'], 'org_id': enc['org_id']})

            # Generate lab panel items
            is_sick = random.random() < 0.3  # 30% chance of abnormal results
            for item in panel_def['items']:
                pid = ctx.seq.next()
                icode, iname, ref_str, unit, lo, hi = item[:6]
                if lo == 0 and hi == 0:
                    # Text result (e.g. "阴性")
                    val = random.choice(['阴性', '阴性', '阴性', '弱阳性', '阳性'])
                    abnormal = val != '阴性'
                    ref_lo, ref_hi = '', ''
                else:
                    if is_sick:
                        mean = (lo + hi) / 2
                        val = rand_float(mean, (hi - lo) * 0.5, lo * 0.5, hi * 1.8, 2)
                    else:
                        mean = (lo + hi) / 2
                        val = rand_float(mean, (hi - lo) * 0.15, lo, hi, 2)
                    abnormal = val < lo or val > hi
                    ref_lo, ref_hi = lo, hi

                panel_rows.append((
                    pid, tid, icode, iname,
                    str(val), unit, ref_str,
                    fmt_bool(abnormal),
                    ref_lo if isinstance(ref_lo, (int, float)) and ref_lo != '' else '',
                    ref_hi if isinstance(ref_hi, (int, float)) and ref_hi != '' else '',
                    random.choice(['', 'ROUTINE', 'STAT']),
                    '', '',
                    'system', '2024-01-01 00:00:00', '', '', 'f', enc['org_id']
                ))

    write_csv('c_lab_test.csv',
              ['id', 'encounter_id', 'patient_id', 'test_code', 'test_name',
               'specimen_type', 'ordered_at', 'collected_at', 'reported_at',
               'status', 'ordering_doctor',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'],
              test_rows, output_dir)

    write_csv('c_lab_panel.csv',
              ['id', 'test_id', 'item_code', 'item_name',
               'result_value', 'result_unit', 'reference_range',
               'abnormal_flag', 'ref_range_lower', 'ref_range_upper',
               'priority', 'comments', 'specimen_id',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'],
              panel_rows, output_dir)


def _gen_medication(ctx, output_dir):
    rows = []
    disease_meds = {m[1]: m for m in MEDS}

    for enc in ctx.encounters:
        disease = enc['disease']
        med_names = disease.get('meds', [])
        # Pick 1 to all of the disease's meds, plus maybe 1-2 random
        selected = random.sample(med_names, min(len(med_names), random.randint(1, max(len(med_names), 1))))
        if random.random() < 0.3:
            extra = random.choice(MEDS)
            if extra[1] not in selected:
                selected.append(extra[1])

        admit = enc['admit']
        for mname in selected:
            med_def = disease_meds.get(mname)
            if not med_def:
                med_def = next((m for m in MEDS if mname in m[1]), random.choice(MEDS))

            oid = ctx.seq.next()
            start = admit + timedelta(hours=random.randint(0, 12))
            dur = random.randint(1, 14)
            end = start + timedelta(days=dur) if enc['encounter_type'] != 'OUTPATIENT' else None

            rows.append((
                oid, enc['id'], enc['patient_id'],
                med_def[0], med_def[1], med_def[2], med_def[3], med_def[4],
                fmt_dt(start), fmt_dt(end) if end else '',
                random.choice(['DOC001', 'DOC002', 'DOC003']),
                random.choice(['ACTIVE', 'COMPLETED', 'COMPLETED', 'DISCONTINUED']),
                'system', '2024-01-01 00:00:00', '', '', 'f', enc['org_id']
            ))
    write_csv('c_medication.csv',
              ['id', 'encounter_id', 'patient_id', 'med_code', 'med_name',
               'dosage', 'route', 'frequency', 'start_time', 'end_time',
               'prescriber', 'status',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'],
              rows, output_dir)


def _gen_vital_sign(ctx, output_dir):
    rows = []
    for enc in ctx.encounters:
        admit = enc['admit']
        is_inpatient = enc['encounter_type'] in ('INPATIENT', 'EMERGENCY')
        n_measurements = random.randint(4, 15) if is_inpatient else random.randint(1, 3)

        for _ in range(n_measurements):
            measured = admit + timedelta(hours=random.randint(0, enc.get('los_days', 1) * 24))

            # Height & weight only once per encounter
            vitals = VITAL_TYPES[:]
            if _ > 0:
                vitals = [v for v in vitals if v[0] not in ('HEIGHT', 'WEIGHT')]

            for stype, unit, mean, std, lo, hi in vitals:
                oid = ctx.seq.next()
                val = rand_float(mean, std, lo, hi, 2)
                rows.append((
                    oid, enc['id'], enc['patient_id'],
                    stype, val, unit,
                    fmt_dt(measured),
                    random.choice(['NURSE001', 'NURSE002', 'NURSE003']),
                    'system', '2024-01-01 00:00:00', '', '', 'f', enc['org_id'],
                    random.randint(220000, 220020)
                ))
    write_csv('c_vital_sign.csv',
              ['id', 'encounter_id', 'patient_id',
               'sign_type', 'sign_value', 'unit', 'measured_at', 'measured_by',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id', 'itemid'],
              rows, output_dir)
