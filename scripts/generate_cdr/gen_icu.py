"""Generate ICU tables: c_icu_stay, c_input_event, c_output_event,
c_microbiology, c_procedure_event, c_datetime_event, c_ingredient_event."""

import random
import json
from datetime import timedelta
from .config import (ICU_CAREUNITS, ICD10, LAB_PANELS, MEDS)
from .utils import (Ctx, rand_float, fmt_dt, fmt_bool, write_csv)


def generate_icu(ctx: Ctx, output_dir: str):
    _gen_icu_stay(ctx, output_dir)
    _gen_input_event(ctx, output_dir)
    _gen_output_event(ctx, output_dir)
    _gen_microbiology(ctx, output_dir)
    _gen_procedure_event(ctx, output_dir)
    _gen_datetime_event(ctx, output_dir)
    _gen_ingredient_event(ctx, output_dir)


def _gen_icu_stay(ctx, output_dir):
    rows = []
    icu_encounters = [e for e in ctx.encounters if e.get('has_icu')]
    if not icu_encounters:
        # Pick some inpatient encounters to have ICU stays
        icu_encounters = [e for e in ctx.encounters
                          if e['encounter_type'] in ('INPATIENT', 'EMERGENCY')][:200]

    for enc in icu_encounters:
        oid = ctx.seq.next()
        admit = enc['admit']
        icu_in = admit + timedelta(hours=random.randint(0, 24))
        los = random.choices([1, 2, 3, 5, 7, 10, 14],
                             weights=[15, 20, 20, 15, 15, 10, 5])[0]
        icu_out = icu_in + timedelta(days=los)
        first_cu = random.choice(ICU_CAREUNITS)
        last_cu = random.choice(ICU_CAREUNITS)

        rows.append((
            oid, enc['id'], enc['patient_id'],
            f'ICU{oid:08d}', first_cu, last_cu,
            random.randint(1, 20), random.randint(1, 20),
            fmt_dt(icu_in), fmt_dt(icu_out),
            round(los + random.random(), 4),
            random.choice(['carevue', 'metavision']),
            'MIMIC4', str(oid + 50000),
            'etl', '2024-01-01 00:00:00', '', '', 'f', enc['org_id']
        ))
        ctx.icu_stays.append({
            'id': oid, 'encounter_id': enc['id'],
            'patient_id': enc['patient_id'], 'org_id': enc['org_id'],
            'in_time': icu_in, 'out_time': icu_out, 'los_days': los,
        })
    write_csv('c_icu_stay.csv',
              ['id', 'encounter_id', 'patient_id', 'stay_no',
               'first_careunit', 'last_careunit', 'first_ward_id', 'last_ward_id',
               'in_time', 'out_time', 'los_days', 'dbsource',
               'source_dataset', 'source_id',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'],
              rows, output_dir)


def _gen_input_event(ctx, output_dir):
    rows = []
    input_items = [
        ('0.9% 氯化钠注射液', 220338, 'Fluids', 'mL', 100, 1000),
        ('5% 葡萄糖注射液', 220336, 'Fluids', 'mL', 100, 500),
        ('乳酸林格液', 220339, 'Fluids', 'mL', 100, 500),
        ('去甲肾上腺素', 221906, 'Vasoactive', 'mcg/kg/min', 0.01, 0.5),
        ('多巴胺', 221662, 'Vasoactive', 'mcg/kg/min', 2, 20),
        ('芬太尼', 221744, 'Sedation', 'mcg/hr', 25, 200),
        ('丙泊酚', 221743, 'Sedation', 'mg/hr', 10, 80),
        ('万古霉素', 222168, 'Antibiotic', 'mg', 500, 1500),
        ('哌拉西林他唑巴坦', 222162, 'Antibiotic', 'g', 3.375, 4.5),
        ('钾离子补充液', 228389, 'Electrolyte', 'mEq', 10, 40),
    ]

    for stay in ctx.icu_stays:
        n_inputs = random.randint(3, 15)
        for _ in range(n_inputs):
            oid = ctx.seq.next()
            item = random.choice(input_items)
            start = stay['in_time'] + timedelta(hours=random.randint(0, int(stay['los_days'] * 24)))
            end = start + timedelta(hours=random.randint(1, 24))
            amount = rand_float(item[4], item[5] * 0.3, item[4], item[5])

            rows.append((
                oid, stay['encounter_id'], stay['patient_id'], stay['id'],
                item[1], fmt_dt(start), fmt_dt(end),
                amount, item[3],
                rand_float(amount * 0.5, amount * 0.2, 0, amount * 2),
                f'{item[3]}/hr',
                rand_float(70, 15, 40, 120),  # patient_weight
                rand_float(amount, 10, 0, amount * 3),
                item[3],
                random.randint(10000, 99999),  # order_id
                random.randint(10000, 99999),  # link_order_id
                item[2], item[0],
                random.choice(['RUNNING', 'FINISHED']),
                random.randint(1000, 1200),
                'MIMIC4', str(oid + 60000),
                'etl', '2024-01-01 00:00:00', '', '', 'f', stay['org_id']
            ))
    write_csv('c_input_event.csv',
              ['id', 'encounter_id', 'patient_id', 'icu_stay_id',
               'itemid', 'start_time', 'end_time',
               'amount', 'amount_uom', 'rate', 'rate_uom',
               'patient_weight', 'total_amount', 'total_amount_uom',
               'order_id', 'link_order_id',
               'order_category', 'order_component_type',
               'status', 'caregiver_id',
               'source_dataset', 'source_id',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'],
              rows, output_dir)


def _gen_output_event(ctx, output_dir):
    rows = []
    output_items = [
        ('尿量', 40055, 'mL', 50, 500),
        ('胸腔引流', 40085, 'mL', 10, 200),
        ('胃肠减压', 40086, 'mL', 10, 300),
        ('伤口引流', 40095, 'mL', 5, 100),
    ]

    for stay in ctx.icu_stays:
        n_outputs = random.randint(2, 8)
        for _ in range(n_outputs):
            oid = ctx.seq.next()
            item = random.choice(output_items)
            chart_time = stay['in_time'] + timedelta(hours=random.randint(0, int(stay['los_days'] * 24)))

            rows.append((
                oid, stay['encounter_id'], stay['patient_id'], stay['id'],
                item[1], fmt_dt(chart_time),
                rand_float(item[3], item[4] * 0.3, item[3], item[4]),
                item[2],
                random.randint(1000, 1200),
                'MIMIC4', str(oid + 70000),
                'etl', '2024-01-01 00:00:00', '', '', 'f', stay['org_id']
            ))
    write_csv('c_output_event.csv',
              ['id', 'encounter_id', 'patient_id', 'icu_stay_id',
               'itemid', 'chart_time', 'value_num', 'value_uom',
               'caregiver_id', 'source_dataset', 'source_id',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'],
              rows, output_dir)


def _gen_microbiology(ctx, output_dir):
    rows = []
    from .config import (MICRO_ORGANISMS, MICRO_AB_GRAM_NEG, MICRO_AB_GRAM_POS,
                         MICRO_AB_FUNGAL, MICRO_SPECIMENS)

    def _pick_interpretation(s_w, r_w, i_w):
        return random.choices(['S', 'R', 'I'], weights=[s_w, r_w, i_w])[0]

    def _parse_dilution(dilution_text):
        comp = '='
        val_str = dilution_text
        if dilution_text.startswith('<='):
            comp, val_str = '<=', dilution_text[2:]
        elif dilution_text.startswith('>='):
            comp, val_str = '>=', dilution_text[2:]
        try:
            val = float(val_str)
        except ValueError:
            val = 0.0
        return dilution_text, comp, val

    def _get_ab_panel(gram_type):
        if gram_type == 'GN':
            return MICRO_AB_GRAM_NEG
        elif gram_type == 'GP':
            return MICRO_AB_GRAM_POS
        else:
            return MICRO_AB_FUNGAL

    def _pick_organisms_for_specimen(spec_type, n_orgs):
        if spec_type in ('痰培养',):
            weights = {'GN': 50, 'GP': 35, 'FUNGUS': 15}
        elif spec_type in ('尿培养', '腹水培养', '引流液培养'):
            weights = {'GN': 70, 'GP': 20, 'FUNGUS': 10}
        elif spec_type in ('血培养',):
            weights = {'GN': 50, 'GP': 35, 'FUNGUS': 15}
        elif spec_type in ('伤口分泌物培养',):
            weights = {'GN': 40, 'GP': 50, 'FUNGUS': 10}
        elif spec_type in ('脑脊液培养',):
            weights = {'GN': 30, 'GP': 60, 'FUNGUS': 10}
        else:
            weights = {'GN': 50, 'GP': 35, 'FUNGUS': 15}
        gram_types = list(weights.keys())
        gram_weights = list(weights.values())

        picked = []
        for i in range(n_orgs):
            gt = random.choices(gram_types, weights=gram_weights)[0]
            candidates = [o for o in MICRO_ORGANISMS if o[2] == gt]
            org = random.choice(candidates)
            picked.append(org)
        return picked

    # Collect all inpatient + emergency encounters
    target_encounters = [e for e in ctx.encounters
                         if e['encounter_type'] in ('INPATIENT', 'EMERGENCY')]

    for enc in target_encounters:
        icd = enc['disease']['icd']
        admit = enc['admit']
        los = enc.get('los_days', 3)
        if los < 1:
            los = 1
        org_id = enc['org_id']

        for spec_cn, related_icds, org_hint, base_prob in MICRO_SPECIMENS:
            if icd not in related_icds:
                continue

            prob = base_prob
            if enc.get('has_icu'):
                prob = min(prob * 1.8, 0.7)
            if enc['severity'] == 'SEVERE':
                prob = min(prob * 1.5, 0.8)

            if random.random() > prob:
                continue

            # 1-3 cultures of this specimen type per encounter
            n_cultures = random.choices([1, 2, 3], weights=[60, 30, 10])[0]
            for ci in range(n_cultures):
                chart_offset = random.randint(0, int(los * 24))
                chart_time = admit + timedelta(hours=chart_offset)

                # 1-3 organisms per culture
                n_orgs = random.choices([1, 2, 3], weights=[82, 15, 3])[0]
                organisms = _pick_organisms_for_specimen(spec_cn, n_orgs)

                for oi, org in enumerate(organisms):
                    cn_name, en_name, gram_type, is_mdr = org
                    isolate_num = oi + 1
                    quantity = random.choice(['+', '++', '+++'])

                    ab_panel = _get_ab_panel(gram_type)
                    max_ab = min(len(ab_panel), 12)
                    min_ab = min(5, max_ab)
                    n_ab = random.randint(min_ab, max_ab)
                    selected_abs = random.sample(ab_panel, n_ab)

                    for ab_cn, ab_en, dilution_options, s_w, r_w, i_w in selected_abs:
                        # MDR organisms shift toward resistant
                        if is_mdr:
                            s_w, r_w, i_w = s_w * 0.5, r_w * 1.6, i_w * 1.2

                        interp = _pick_interpretation(s_w, r_w, i_w)
                        dilution_text = random.choice(dilution_options)
                        dilution_text, dilution_comp, dilution_val = _parse_dilution(dilution_text)

                        oid = ctx.seq.next()
                        rows.append((
                            oid, enc['id'], enc['patient_id'],
                            fmt_dt(chart_time), fmt_dt(chart_time),
                            700 + random.randint(0, 20), spec_cn,
                            800 + random.randint(0, 20), '细菌培养' if gram_type != 'FUNGUS' else '真菌培养',
                            900 + random.randint(0, 20), en_name,
                            isolate_num,
                            1000 + random.randint(0, 20), ab_en,
                            dilution_text, dilution_comp, dilution_val,
                            interp, quantity,
                            'MIMIC4', str(oid + 80000),
                            'etl', '2024-01-01 00:00:00', '', '', 'f', org_id
                        ))

    write_csv('c_microbiology.csv',
              ['id', 'encounter_id', 'patient_id',
               'chart_date', 'chart_time',
               'spec_itemid', 'spec_type_desc',
               'test_itemid', 'test_name',
               'org_itemid', 'org_name', 'isolate_num',
               'ab_itemid', 'ab_name',
               'dilution_text', 'dilution_comparison', 'dilution_value',
               'interpretation', 'quantity',
               'source_dataset', 'source_id',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'],
              rows, output_dir)


def _gen_procedure_event(ctx, output_dir):
    rows = []
    procedures = [
        ('气管内插管', 224385, 15, 120),
        ('中心静脉置管', 224239, 20, 60),
        ('动脉穿刺置管', 225792, 10, 30),
        ('胸腔闭式引流', 225312, 30, 90),
        ('胃管置入', 224088, 10, 30),
        ('尿管置入', 224090, 5, 15),
    ]

    for stay in ctx.icu_stays:
        n_procs = random.randint(1, 4)
        for _ in range(n_procs):
            oid = ctx.seq.next()
            proc = random.choice(procedures)
            start = stay['in_time'] + timedelta(hours=random.randint(0, int(stay['los_days'] * 12)))
            dur = random.randint(proc[2], proc[3])
            end = start + timedelta(minutes=dur)

            rows.append((
                oid, stay['encounter_id'], stay['patient_id'], stay['id'],
                proc[1], fmt_dt(start), fmt_dt(end),
                '', '', '', '',
                random.randint(10000, 99999), random.randint(10000, 99999),
                proc[0], rand_float(70, 15, 40, 120),
                random.choice(['COMPLETED', 'COMPLETED']),
                random.randint(1000, 1200),
                'MIMIC4', str(oid + 90000),
                'etl', '2024-01-01 00:00:00', '', '', 'f', stay['org_id']
            ))
    write_csv('c_procedure_event.csv',
              ['id', 'encounter_id', 'patient_id', 'icu_stay_id',
               'itemid', 'start_time', 'end_time',
               'value_text', 'value_uom', 'location', 'location_category',
               'order_id', 'link_order_id',
               'order_category', 'patient_weight',
               'status', 'caregiver_id',
               'source_dataset', 'source_id',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'],
              rows, output_dir)


def _gen_datetime_event(ctx, output_dir):
    rows = []
    dt_items = [
        ('气管插管时间', 225312, ' Respiratory'),
        ('拔管时间', 225313, 'Respiratory'),
        ('手术开始时间', 224001, 'Surgery'),
        ('手术结束时间', 224002, 'Surgery'),
        ('透析开始', 225436, 'Renal'),
    ]

    for stay in ctx.icu_stays:
        if random.random() < 0.4:
            n_events = random.randint(1, 3)
            for _ in range(n_events):
                oid = ctx.seq.next()
                item = random.choice(dt_items)
                chart_time = stay['in_time'] + timedelta(hours=random.randint(0, int(stay['los_days'] * 24)))
                value_text = fmt_dt(chart_time + timedelta(hours=random.randint(1, 48)))

                rows.append((
                    oid, stay['encounter_id'], stay['patient_id'], stay['id'],
                    item[1], fmt_dt(chart_time),
                    value_text, '',
                    fmt_bool(random.random() < 0.02),
                    random.randint(1000, 1200),
                    'MIMIC4', str(oid + 100000),
                    'etl', '2024-01-01 00:00:00', '', '', 'f', stay['org_id']
                ))
    write_csv('c_datetime_event.csv',
              ['id', 'encounter_id', 'patient_id', 'icu_stay_id',
               'itemid', 'chart_time', 'value_text', 'value_uom',
               'warning', 'caregiver_id',
               'source_dataset', 'source_id',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'],
              rows, output_dir)


def _gen_ingredient_event(ctx, output_dir):
    rows = []
    ingredients = [
        ('氯化钠 0.9%', 220863, 'mL', 50, 250),
        ('葡萄糖 5%', 220864, 'mL', 50, 250),
        ('氯化钾', 220991, 'mEq', 10, 40),
        ('胰岛素', 223257, 'Units', 1, 10),
        ('肝素', 225153, 'Units', 100, 1000),
    ]

    for stay in ctx.icu_stays:
        if random.random() < 0.5:
            n_events = random.randint(1, 5)
            for _ in range(n_events):
                oid = ctx.seq.next()
                item = random.choice(ingredients)
                start = stay['in_time'] + timedelta(hours=random.randint(0, int(stay['los_days'] * 24)))
                end = start + timedelta(hours=random.randint(1, 12))

                rows.append((
                    oid, stay['encounter_id'], stay['patient_id'], stay['id'],
                    item[1], fmt_dt(start), fmt_dt(end),
                    rand_float(item[3], item[4] * 0.3, item[3], item[4]),
                    item[2],
                    rand_float(item[3] * 0.5, item[4] * 0.2, 0, item[4]),
                    f'{item[2]}/hr',
                    random.randint(10000, 99999),
                    random.randint(10000, 99999),
                    random.choice(['RUNNING', 'FINISHED']),
                    random.randint(1000, 1200),
                    'MIMIC4', str(oid + 110000),
                    'etl', '2024-01-01 00:00:00', '', '', 'f', stay['org_id']
                ))
    write_csv('c_ingredient_event.csv',
              ['id', 'encounter_id', 'patient_id', 'icu_stay_id',
               'itemid', 'start_time', 'end_time',
               'amount', 'amount_uom', 'rate', 'rate_uom',
               'order_id', 'link_order_id',
               'status', 'caregiver_id',
               'source_dataset', 'source_id',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'],
              rows, output_dir)
