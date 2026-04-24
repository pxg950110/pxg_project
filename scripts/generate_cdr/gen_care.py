"""Generate care & admin tables: c_nursing_record, c_blood_transfusion,
c_transfer, c_fee_record, c_discharge_summary."""

import random
from datetime import timedelta
from .config import (DEPTS, FEE_TYPES, BLOOD_TYPES)
from .utils import (Ctx, rand_datetime, rand_float, fmt_dt, fmt_bool,
                    write_csv)


def generate_care(ctx: Ctx, output_dir: str):
    _gen_nursing_record(ctx, output_dir)
    _gen_blood_transfusion(ctx, output_dir)
    _gen_transfer(ctx, output_dir)
    _gen_fee_record(ctx, output_dir)
    _gen_discharge_summary(ctx, output_dir)


def _gen_nursing_record(ctx, output_dir):
    rows = []
    record_types = ['一般护理记录', '重症护理记录', '专科护理记录', '手术护理记录', '交接班记录']
    nursing_templates = [
        '患者神志{conscious}，精神{spirit}。T {temp}℃，P {pulse}次/分，R {resp}次/分，BP {bp}mmHg，SpO2 {spo2}%。{extra}',
        '患者今日{condition}。饮食{diet}，睡眠{sleep}。{extra}',
        '遵医嘱给予{treatment}，患者{response}。{extra}',
        '伤口敷料{wound}，引流管{drainage}。{extra}',
        '患者情绪{emotion}，配合治疗。{extra}',
    ]

    for enc in ctx.encounters:
        if enc['encounter_type'] in ('INPATIENT', 'EMERGENCY'):
            n_records = random.randint(2, 8)
            for _ in range(n_records):
                oid = ctx.seq.next()
                record_time = enc['admit'] + timedelta(hours=random.randint(0, enc.get('los_days', 3) * 24))
                template = random.choice(nursing_templates)
                content = template.format(
                    conscious=random.choice(['清楚', '清楚', '清楚', '模糊', '嗜睡']),
                    spirit=random.choice(['可', '一般', '好', '差']),
                    temp=rand_float(36.5, 0.5, 35.5, 39.0),
                    pulse=rand_float(80, 10, 60, 120, 0),
                    resp=rand_float(18, 3, 12, 30, 0),
                    bp=f'{rand_float(120, 15, 90, 160, 0):.0f}/{rand_float(80, 10, 60, 100, 0):.0f}',
                    spo2=rand_float(97, 2, 90, 100, 0),
                    extra=random.choice(['继续观察。', '报告医生。', '嘱患者卧床休息。', '']),
                    condition=random.choice(['平稳', '一般情况可', '较前好转', '无明显变化']),
                    diet=random.choice(['正常', '半流质', '流质', '禁食']),
                    sleep=random.choice(['可', '尚可', '欠佳', '好']),
                    treatment=random.choice(['抗感染治疗', '补液治疗', '降压治疗', '降糖治疗']),
                    response=random.choice(['无不适', '耐受良好', '未见明显不良反应']),
                    wound=random.choice(['干燥', '清洁', '渗出少许']),
                    drainage=random.choice(['通畅', '引流通畅', '固定良好']),
                    emotion=random.choice(['稳定', '平稳', '焦虑', '配合']),
                )
                rows.append((
                    oid, enc['id'], enc['patient_id'],
                    random.choice(record_types), content,
                    random.choice(['NURSE001', 'NURSE002', 'NURSE003', 'NURSE004']),
                    fmt_dt(record_time),
                    'system', '2024-01-01 00:00:00', '', '', 'f', enc['org_id']
                ))
    write_csv('c_nursing_record.csv',
              ['id', 'encounter_id', 'patient_id',
               'record_type', 'content', 'nurse_code', 'record_time',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'],
              rows, output_dir)


def _gen_blood_transfusion(ctx, output_dir):
    rows = []
    for enc in ctx.encounters:
        if enc['encounter_type'] in ('INPATIENT', 'EMERGENCY') and random.random() < 0.05:
            oid = ctx.seq.next()
            btype = random.choice(BLOOD_TYPES)
            volume = random.choice([200, 400, 400, 600])
            transfused = rand_datetime()
            has_reaction = random.random() < 0.05
            rows.append((
                oid, enc['id'], enc['patient_id'],
                btype, volume, fmt_dt(transfused),
                random.choice(['NURSE001', 'NURSE002']),
                random.choice(['无反应', '无反应', '无反应', '出现轻微过敏反应']) if has_reaction else '无反应',
                'system', '2024-01-01 00:00:00', '', '', 'f', enc['org_id']
            ))
    write_csv('c_blood_transfusion.csv',
              ['id', 'encounter_id', 'patient_id',
               'blood_type', 'volume_ml', 'transfused_at', 'operator',
               'reaction_desc',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'],
              rows, output_dir)


def _gen_transfer(ctx, output_dir):
    rows = []
    for enc in ctx.encounters:
        if enc['encounter_type'] in ('INPATIENT', 'EMERGENCY') and random.random() < 0.35:
            n_transfers = random.choices([1, 2], weights=[80, 20])[0]
            for _ in range(n_transfers):
                oid = ctx.seq.next()
                dept = random.choice(DEPTS)
                to_dept = random.choice([d for d in DEPTS if d != dept])
                transfer_time = enc['admit'] + timedelta(hours=random.randint(4, enc.get('los_days', 3) * 24))
                rows.append((
                    oid, enc['id'], enc['patient_id'],
                    dept[1], to_dept[1],
                    random.choice(['DEPT', 'WARD', 'ICU']),
                    fmt_dt(transfer_time),
                    random.choice(['病情需要', '患者要求', '专科治疗', '术后恢复']),
                    'system', '2024-01-01 00:00:00', '', '', 'f', enc['org_id']
                ))
    write_csv('c_transfer.csv',
              ['id', 'encounter_id', 'patient_id',
               'from_dept', 'to_dept', 'transfer_type', 'transfer_time', 'reason',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'],
              rows, output_dir)


def _gen_fee_record(ctx, output_dir):
    rows = []
    for enc in ctx.encounters:
        is_inpatient = enc['encounter_type'] in ('INPATIENT', 'EMERGENCY')
        n_fees = random.randint(3, 10) if is_inpatient else random.randint(1, 4)
        admit = enc['admit']

        for _ in range(n_fees):
            oid = ctx.seq.next()
            fee = random.choice(FEE_TYPES)
            if enc.get('has_surgery') and fee[0] == '手术费' and random.random() < 0.5:
                pass  # include surgery fee
            elif not enc.get('has_surgery') and fee[0] == '手术费':
                if random.random() < 0.7:
                    continue
            amount = round(random.uniform(fee[2][0], fee[2][1]), 2)
            quantity = round(random.uniform(1, 5), 2) if fee[1] in ('MEDICATION', 'MATERIAL') else 1

            rows.append((
                oid, enc['id'], enc['patient_id'],
                fee[1], fee[0], amount, quantity,
                fmt_dt(rand_datetime()),
                'system', '2024-01-01 00:00:00', '', '', 'f', enc['org_id']
            ))
    write_csv('c_fee_record.csv',
              ['id', 'encounter_id', 'patient_id',
               'fee_type', 'fee_name', 'amount', 'quantity', 'billing_time',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'],
              rows, output_dir)


def _gen_discharge_summary(ctx, output_dir):
    rows = []
    for enc in ctx.encounters:
        if enc['encounter_type'] in ('INPATIENT', 'EMERGENCY') and enc.get('discharge'):
            oid = ctx.seq.next()
            disease = enc['disease']
            outcome = random.choice(['好转', '明显好转', '痊愈', '好转', '稳定', '恶化'])
            rows.append((
                oid, enc['id'], enc['patient_id'],
                disease['name'],
                disease['name'] + (f'；{random.choice(["高血压", "糖尿病", "冠心病"])}' if random.random() < 0.3 else ''),
                f'入院后完善相关检查，明确诊断，给予{random.choice(["抗感染", "对症支持", "规范降糖降压", "手术"])}治疗。',
                f'1.{random.choice(["注意休息", "避免劳累", "适当活动"])} '
                f'2.{random.choice(["规律服药", "定期复查", "门诊随访"])} '
                f'3.{random.choice(["低盐低脂饮食", "清淡饮食", "营养均衡"])}',
                f'{random.randint(1, 4)}周后门诊复查。',
                fmt_dt(enc['discharge']),
                random.choice(['DOC001', 'DOC002', 'DOC003']),
                'system', '2024-01-01 00:00:00', '', '', 'f', enc['org_id']
            ))
    write_csv('c_discharge_summary.csv',
              ['id', 'encounter_id', 'patient_id',
               'admission_diagnosis', 'discharge_diagnosis',
               'treatment_summary', 'discharge_instruction', 'follow_up_plan',
               'discharged_at', 'discharge_doctor',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'],
              rows, output_dir)
