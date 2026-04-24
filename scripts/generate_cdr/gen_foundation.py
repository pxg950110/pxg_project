"""Generate foundation tables: c_org, c_caregiver, c_patient, c_patient_contact,
c_patient_insurance, c_allergy, c_family_history."""

import random
from datetime import date, timedelta
from .config import (fake, NUM_PATIENTS, ORGS, DEPTS, ETHNICITIES, BLOOD_TYPES,
                     MARITAL_STATUS, OCCUPATIONS, INSURANCE_TYPES, RELATIONSHIPS,
                     ALLERGENS, ALLERGY_REACTIONS, ALLERGY_SEVERITIES,
                     ICD10)
from .utils import (Ctx, rand_date, rand_datetime, fmt_dt, fmt_date, fmt_bool,
                    write_csv)


def generate_foundation(ctx: Ctx, output_dir: str):
    _gen_org(ctx, output_dir)
    _gen_caregiver(ctx, output_dir)
    _gen_patient(ctx, output_dir)
    _gen_patient_contact(ctx, output_dir)
    _gen_patient_insurance(ctx, output_dir)
    _gen_allergy(ctx, output_dir)
    _gen_family_history(ctx, output_dir)


def _gen_org(ctx, output_dir):
    rows = []
    for org_code, org_name, org_type, addr, phone in ORGS:
        oid = ctx.seq.next()
        org_id = random.randint(1, 100)
        rows.append((
            oid, org_code, org_name, org_type, '',
            addr, phone, 'ACTIVE',
            'system', '2024-01-01 00:00:00', '', '', 'f', org_id
        ))
        ctx.orgs.append({'id': oid, 'org_code': org_code, 'org_id': org_id})
    write_csv('c_org.csv',
              ['id', 'org_code', 'org_name', 'org_type', 'parent_id',
               'address', 'contact_phone', 'status',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'], rows, output_dir)


def _gen_caregiver(ctx, output_dir):
    rows = []
    labels = ['主治医师', '副主任医师', '住院医师', '主管护师', '护师', '护士', '主任护师', '主任医师']
    for i in range(200):
        oid = ctx.seq.next()
        code = f'CG{i+1:04d}'
        label = random.choice(labels)
        rows.append((
            oid, code, label, f'{label}-{code}',
            'MIMIC4', str(i + 100), 'etl', '2024-01-01 00:00:00', '', '', 'f', 1
        ))
        ctx.caregivers.append({'id': oid, 'code': code})
    write_csv('c_caregiver.csv',
              ['id', 'caregiver_code', 'label', 'description',
               'source_dataset', 'source_id', 'created_by', 'created_at',
               'updated_by', 'updated_at', 'is_deleted', 'org_id'], rows, output_dir)


def _gen_patient(ctx, output_dir):
    rows = []
    for i in range(NUM_PATIENTS):
        oid = ctx.seq.next()
        org = random.choice(ctx.orgs)
        gender = random.choices(['M', 'F', 'O'], weights=[48, 50, 2])[0]
        birth = fake.date_of_birth(minimum_age=0, maximum_age=95)
        id_card_hash = f'hash_{fake.sha256()[:32]}'
        name = fake.name()
        address = fake.address().replace('\n', ' ')
        phone_hash = f'hash_{fake.sha256()[:16]}'

        expire = random.random() < 0.08
        max_dod = max(birth + timedelta(days=365*18), date(2025, 12, 31))
        dod = rand_date(birth, max_dod) if expire else None
        anchor_age = random.randint(18, 90)
        anchor_year = random.choice([2020, 2021, 2022, 2023, 2024])

        # Columns matching actual DB: id, created_at, created_by, is_deleted, org_id,
        # updated_at, updated_by, address, birth_date, gender, id_card_hash, name,
        # phone_hash, dod, expire_flag, anchor_age, anchor_year, anchor_year_group
        rows.append((
            oid, '2024-01-01 00:00:00', 'system', 'f', org['org_id'],
            '', '',
            address, fmt_date(birth), gender, id_card_hash, name, phone_hash,
            fmt_date(dod), fmt_bool(expire), anchor_age, anchor_year, str(anchor_year)
        ))
        ctx.patients.append({
            'id': oid, 'org_id': org['org_id'], 'gender': gender,
            'birth_date': birth, 'expire': expire,
        })
    write_csv('c_patient.csv',
              ['id', 'created_at', 'created_by', 'is_deleted', 'org_id',
               'updated_at', 'updated_by',
               'address', 'birth_date', 'gender', 'id_card_hash', 'name', 'phone_hash',
               'dod', 'expire_flag', 'anchor_age', 'anchor_year', 'anchor_year_group'],
              rows, output_dir)


def _gen_patient_contact(ctx, output_dir):
    rows = []
    for pat in ctx.patients:
        n_contacts = random.choices([1, 2, 3], weights=[50, 40, 10])[0]
        for _ in range(n_contacts):
            oid = ctx.seq.next()
            rows.append((
                oid, pat['id'], fake.name(),
                random.choice(RELATIONSHIPS), fake.phone_number(),
                fake.address().replace('\n', ' '),
                fmt_bool(random.random() < 0.4),
                'system', '2024-01-01 00:00:00', '', '', 'f', pat['org_id']
            ))
    write_csv('c_patient_contact.csv',
              ['id', 'patient_id', 'contact_name', 'relationship',
               'phone', 'address', 'is_emergency',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'], rows, output_dir)


def _gen_patient_insurance(ctx, output_dir):
    rows = []
    for pat in ctx.patients:
        if random.random() < 0.9:
            oid = ctx.seq.next()
            ins_type = random.choice(INSURANCE_TYPES)
            valid_from = rand_date()
            valid_to = valid_from + timedelta(days=random.randint(365, 3650))
            rows.append((
                oid, pat['id'], ins_type,
                f'INS{random.randint(10000000, 99999999)}',
                fake.name(), fmt_date(valid_from), fmt_date(valid_to),
                'system', '2024-01-01 00:00:00', '', '', 'f', pat['org_id']
            ))
    write_csv('c_patient_insurance.csv',
              ['id', 'patient_id', 'insurance_type', 'insurance_no',
               'holder_name', 'valid_from', 'valid_to',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'], rows, output_dir)


def _gen_allergy(ctx, output_dir):
    rows = []
    for pat in ctx.patients:
        if random.random() < 0.35:
            n = random.choices([1, 2], weights=[80, 20])[0]
            for _ in range(n):
                oid = ctx.seq.next()
                atype = random.choice(list(ALLERGENS.keys()))
                allergen = random.choice(ALLERGENS[atype])
                rows.append((
                    oid, pat['id'], allergen, atype,
                    random.choice(ALLERGY_REACTIONS),
                    random.choice(ALLERGY_SEVERITIES),
                    fmt_dt(rand_datetime()),
                    'system', '2024-01-01 00:00:00', '', '', 'f', pat['org_id']
                ))
    write_csv('c_allergy.csv',
              ['id', 'patient_id', 'allergen', 'allergen_type',
               'reaction', 'severity', 'confirmed_at',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'], rows, output_dir)


def _gen_family_history(ctx, output_dir):
    rows = []
    for pat in ctx.patients:
        if random.random() < 0.5:
            n = random.choices([1, 2, 3], weights=[60, 30, 10])[0]
            for _ in range(n):
                oid = ctx.seq.next()
                icd = random.choice(ICD10)
                rel = random.choice(['父亲', '母亲', '兄弟', '姐妹', '祖父', '祖母'])
                rows.append((
                    oid, pat['id'], rel, icd[1], icd[0],
                    random.randint(30, 75),
                    'system', '2024-01-01 00:00:00', '', '', 'f', pat['org_id']
                ))
    write_csv('c_family_history.csv',
              ['id', 'patient_id', 'relationship', 'disease_name', 'icd_code',
               'onset_age', 'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'], rows, output_dir)
