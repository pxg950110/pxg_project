"""Generate encounter-level tables: c_encounter, c_patient_bed."""

import random
from datetime import timedelta
from .config import (fake, DEPTS, DISEASES, BUILDINGS, FLOORS,
                     INSURANCE_TYPES, ETHNICITIES)
from .utils import (Ctx, rand_datetime, rand_date, fmt_dt, fmt_bool,
                    write_csv, choice_weighted)


def generate_encounters(ctx: Ctx, output_dir: str):
    _gen_encounter(ctx, output_dir)
    _gen_patient_bed(ctx, output_dir)


def _pick_encounter_type():
    return choice_weighted([
        ('OUTPATIENT', 55), ('INPATIENT', 30), ('EMERGENCY', 15)
    ])


def _pick_disease():
    return random.choice(DISEASES)


def _gen_encounter(ctx, output_dir):
    rows = []
    for pat in ctx.patients:
        n_enc = random.choices([1, 2, 3, 4, 5, 6, 7, 8],
                               weights=[10, 20, 25, 20, 10, 8, 5, 2])[0]
        for _ in range(n_enc):
            oid = ctx.seq.next()
            disease = _pick_disease()
            enc_type = random.choice(disease['encounter_types'])
            dept = random.choice(disease['depts'])
            dept_info = next((d for d in DEPTS if d[0] == dept), random.choice(DEPTS))

            cg = random.choice(ctx.caregivers)
            admit = rand_datetime()
            enc_no = f'ENC{oid:08d}'

            is_inpatient = enc_type in ('INPATIENT', 'EMERGENCY')
            los_days = 0
            discharge = None
            bed_no = ''
            ward_code = ''
            if is_inpatient:
                los_days = random.choices([1, 2, 3, 5, 7, 10, 14, 21, 30],
                                          weights=[5, 10, 15, 20, 15, 15, 10, 5, 5])[0]
                discharge = admit + timedelta(days=los_days)
                bed_no = f'{random.randint(1,50)}床'
                ward_code = f'W{random.randint(1,20):02d}'

            status = 'DISCHARGED' if discharge else 'COMPLETED'
            if enc_type == 'OUTPATIENT':
                status = 'COMPLETED'

            severity = disease['severity']
            death_time = None
            hosp_expire = False
            if pat['expire'] and random.random() < 0.15 and is_inpatient:
                death_time = discharge or admit + timedelta(days=los_days)
                hosp_expire = True
                status = 'EXPIRED'

            adm_type = random.choice(['EMERGENCY', 'URGENT', 'ELECTIVE', 'NEWBORN', 'EU OBSERVATION']) if is_inpatient else ''
            adm_loc = random.choice(['CLINIC', 'EMERGENCY', 'PHYSICIAN_REFERRAL', 'TRANSFER']) if is_inpatient else ''
            dis_loc = random.choice(['HOME', 'SNF', 'REHAB', 'AGAINST_ADVICE', 'DIED']) if discharge else ''

            rows.append((
                oid, '2024-01-01 00:00:00', 'system', 'f', pat['org_id'],
                '', '',
                fmt_dt(admit), f'医生{cg["code"]}',
                dept_info[1],
                disease['name'],
                fmt_dt(discharge),
                enc_type,
                pat['id'],
                adm_type, adm_loc, dis_loc,
                fmt_dt(death_time), fmt_bool(hosp_expire),
                random.choice(INSURANCE_TYPES) if is_inpatient else '',
                '', '',
                '', ''
            ))
            ctx.encounters.append({
                'id': oid, 'patient_id': pat['id'], 'org_id': pat['org_id'],
                'encounter_type': enc_type, 'dept_code': dept_info[0],
                'dept_name': dept_info[1], 'admit': admit, 'discharge': discharge,
                'severity': severity, 'status': status,
                'los_days': los_days, 'has_icu': disease['has_icu'],
                'has_surgery': disease['has_surgery'],
                'disease': disease, 'bed_no': bed_no, 'ward_code': ward_code,
            })

    write_csv('c_encounter.csv',
              ['id', 'created_at', 'created_by', 'is_deleted', 'org_id',
               'updated_at', 'updated_by',
               'admission_time', 'attending_doctor', 'department',
               'diagnosis_summary',
               'discharge_time', 'encounter_type', 'patient_id',
               'admission_type', 'admission_location', 'discharge_location',
               'death_time', 'hospital_expire_flag',
               'insurance_type', 'language', 'ethnicity',
               'ed_reg_time', 'ed_out_time'],
              rows, output_dir)


def _gen_patient_bed(ctx, output_dir):
    rows = []
    for enc in ctx.encounters:
        if enc['encounter_type'] in ('INPATIENT', 'EMERGENCY') and enc['bed_no']:
            oid = ctx.seq.next()
            admit = enc['admit']
            discharge = enc['discharge'] or admit + timedelta(days=7)
            building = random.choice(BUILDINGS)
            floor = random.choice(FLOORS)
            rows.append((
                oid, enc['patient_id'], enc['id'],
                enc['bed_no'], enc['ward_code'],
                building, floor,
                fmt_dt(admit), fmt_dt(discharge),
                'system', '2024-01-01 00:00:00', '', '', 'f', enc['org_id']
            ))
    write_csv('c_patient_bed.csv',
              ['id', 'patient_id', 'encounter_id',
               'bed_no', 'ward_code', 'building', 'floor',
               'assigned_at', 'vacated_at',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'], rows, output_dir)
