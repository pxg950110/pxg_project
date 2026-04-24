"""Generate health checkup tables: c_health_checkup, c_checkup_package,
c_checkup_item_result, c_checkup_summary, c_checkup_comparison."""

import random
import json
from datetime import timedelta
from .config import (fake, LAB_PANELS, ORGS)
from .utils import (Ctx, rand_date, rand_float, fmt_date, fmt_bool,
                    write_csv)


def generate_checkup(ctx: Ctx, output_dir: str):
    _gen_checkup(ctx, output_dir)
    _gen_checkup_package(ctx, output_dir)
    _gen_checkup_item_result(ctx, output_dir)
    _gen_checkup_summary(ctx, output_dir)
    _gen_checkup_comparison(ctx, output_dir)


CHECKUP_PACKAGES = [
    ('基础体检套餐', 'BASIC', [
        ('CI001', '身高', 'cm', 150, 190, 168, 8),
        ('CI002', '体重', 'kg', 40, 120, 68, 12),
        ('CI003', '血压(收缩压)', 'mmHg', 90, 140, 120, 15),
        ('CI004', '血压(舒张压)', 'mmHg', 60, 90, 80, 10),
        ('CI005', '心率', '次/分', 60, 100, 75, 10),
        ('CI006', '体温', '℃', 36.0, 37.3, 36.5, 0.3),
    ]),
    ('血常规', 'BLOOD', [
        ('WBC', '白细胞计数', '×10⁹/L', 4.0, 10.0, 6.5, 1.5),
        ('RBC', '红细胞计数', '×10¹²/L', 4.0, 5.5, 4.8, 0.4),
        ('HGB', '血红蛋白', 'g/L', 120, 160, 140, 10),
        ('PLT', '血小板计数', '×10⁹/L', 100, 300, 200, 40),
    ]),
    ('生化检查', 'BIOCHEM', [
        ('ALT', '谷丙转氨酶', 'U/L', 0, 40, 25, 10),
        ('AST', '谷草转氨酶', 'U/L', 0, 40, 22, 8),
        ('GLU', '空腹血糖', 'mmol/L', 3.9, 6.1, 5.2, 0.5),
        ('TC', '总胆固醇', 'mmol/L', 2.8, 5.7, 4.5, 0.7),
        ('TG', '甘油三酯', 'mmol/L', 0.56, 1.7, 1.2, 0.3),
        ('CREA', '肌酐', 'μmol/L', 44, 133, 80, 20),
        ('BUN', '尿素氮', 'mmol/L', 2.5, 7.1, 5.0, 1.0),
        ('UA', '尿酸', 'μmol/L', 150, 420, 320, 60),
    ]),
    ('尿常规', 'URINE', [
        ('UPRO', '尿蛋白', '', 0, 0, 0, 0),
        ('UGLU', '尿糖', '', 0, 0, 0, 0),
        ('URBC', '尿红细胞', '', 0, 0, 0, 0),
    ]),
    ('心电图', 'ECG', []),
    ('胸部X光', 'CHEST_XRAY', []),
    ('腹部彩超', 'ABDOMEN_US', []),
    ('甲状腺功能', 'THYROID', [
        ('TSH', '促甲状腺激素', 'mIU/L', 0.27, 4.2, 2.0, 1.0),
        ('FT3', '游离T3', 'pmol/L', 3.1, 6.8, 4.5, 0.8),
        ('FT4', '游离T4', 'pmol/L', 12.0, 22.0, 16.0, 2.0),
    ]),
    ('肿瘤标志物', 'TUMOR', [
        ('AFP', '甲胎蛋白', 'ng/mL', 0, 7, 3, 2),
        ('CEA', '癌胚抗原', 'ng/mL', 0, 5, 2, 1.5),
    ]),
]

CONCLUSIONS = [
    '体检各项指标基本正常。',
    '血常规、生化检查未见明显异常。',
    '部分指标轻度异常，建议定期复查。',
    '血糖偏高，建议内分泌科进一步检查。',
    '血脂偏高，建议控制饮食，定期复查。',
    '肝功能轻度异常，建议戒酒，定期复查。',
    '甲状腺功能异常，建议内分泌科随访。',
    '心电图未见明显异常。',
    '胸部X线未见明显异常。',
    '血压偏高，建议心内科随访。',
]

SUGGESTIONS = [
    '建议保持良好生活习惯，定期体检。',
    '建议低盐低脂饮食，适当运动。',
    '建议戒烟限酒，保持心情舒畅。',
    '建议规律作息，避免熬夜。',
    '异常指标建议专科就诊进一步检查。',
    '建议每年进行一次全面体检。',
    '建议控制体重，BMI保持在正常范围。',
]


def _gen_checkup(ctx, output_dir):
    rows = []
    checkup_patients = random.sample(ctx.patients, min(1200, len(ctx.patients)))
    for pat in checkup_patients:
        n_checkups = random.choices([1, 2, 3], weights=[60, 30, 10])[0]
        for _ in range(n_checkups):
            oid = ctx.seq.next()
            checkup_date = rand_date()
            org = random.choice(ORGS)
            rows.append((
                oid, pat['id'], f'CU{oid:08d}',
                fmt_date(checkup_date), org[1],
                random.choice(CONCLUSIONS), random.choice(SUGGESTIONS),
                'system', '2024-01-01 00:00:00', '', '', 'f', pat['org_id']
            ))
            ctx.checkups.append({'id': oid, 'patient_id': pat['id'],
                                 'org_id': pat['org_id'], 'checkup_date': checkup_date})
    write_csv('c_health_checkup.csv',
              ['id', 'patient_id', 'checkup_no', 'checkup_date', 'org_name',
               'conclusion', 'suggestion',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'],
              rows, output_dir)


def _gen_checkup_package(ctx, output_dir):
    rows = []
    for checkup in ctx.checkups:
        n_pkgs = random.choices([2, 3, 4, 5], weights=[20, 40, 30, 10])[0]
        selected = random.sample(CHECKUP_PACKAGES, min(n_pkgs, len(CHECKUP_PACKAGES)))
        for pkg_name, category, _ in selected:
            oid = ctx.seq.next()
            rows.append((
                oid, checkup['id'], pkg_name, category,
                'system', '2024-01-01 00:00:00', '', '', 'f', checkup['org_id']
            ))
            ctx.checkup_pkgs.append({'id': oid, 'checkup_id': checkup['id'],
                                     'org_id': checkup['org_id'],
                                     'category': category, 'items': next(p[2] for p in CHECKUP_PACKAGES if p[1] == category)})
    write_csv('c_checkup_package.csv',
              ['id', 'checkup_id', 'package_name', 'category',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'],
              rows, output_dir)


def _gen_checkup_item_result(ctx, output_dir):
    rows = []
    for pkg in ctx.checkup_pkgs:
        for item in pkg['items']:
            oid = ctx.seq.next()
            code, name, unit, lo, hi, mean, std = item
            if lo == 0 and hi == 0:
                val = random.choice(['阴性', '阴性', '阴性', '弱阳性'])
                abnormal = val != '阴性'
                ref_range = '阴性'
            else:
                val = rand_float(mean, std, lo * 0.8, hi * 1.3, 2)
                abnormal = val < lo or val > hi
                ref_range = f'{lo}-{hi}'
            rows.append((
                oid, pkg['id'], code, name,
                str(val), unit, ref_range,
                fmt_bool(abnormal),
                'system', '2024-01-01 00:00:00', '', '', 'f', pkg['org_id']
            ))
        # Add text items for non-lab packages
        if not pkg['items']:
            oid = ctx.seq.next()
            text_results = {
                'ECG': ('CI100', '心电图', '窦性心律，心电图大致正常', '正常心电图'),
                'CHEST_XRAY': ('CI101', '胸部X线', '双肺纹理清晰，心影不大', '未见明显异常'),
                'ABDOMEN_US': ('CI102', '腹部彩超', '肝胆脾胰双肾未见明显异常', '未见明显异常'),
            }
            if pkg['category'] in text_results:
                code, name, val, ref = text_results[pkg['category']]
                rows.append((
                    oid, pkg['id'], code, name, val, '', ref, 'f',
                    'system', '2024-01-01 00:00:00', '', '', 'f', pkg['org_id']
                ))
    write_csv('c_checkup_item_result.csv',
              ['id', 'package_id', 'item_code', 'item_name',
               'result_value', 'unit', 'reference_range', 'abnormal_flag',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'],
              rows, output_dir)


def _gen_checkup_summary(ctx, output_dir):
    rows = []
    summary_types = ['总体评估', '异常项目汇总', '健康建议', '风险提示']
    for checkup in ctx.checkups:
        n_summaries = random.choices([1, 2, 3], weights=[30, 50, 20])[0]
        for stype in random.sample(summary_types, min(n_summaries, len(summary_types))):
            oid = ctx.seq.next()
            content_map = {
                '总体评估': random.choice(CONCLUSIONS),
                '异常项目汇总': '本次体检共{abn}项异常指标。'.format(abn=random.randint(0, 3)),
                '健康建议': random.choice(SUGGESTIONS),
                '风险提示': random.choice(['暂无明显健康风险。', '建议关注心血管健康。', '建议关注血糖水平。']),
            }
            rows.append((
                oid, checkup['id'], stype, content_map.get(stype, ''),
                'system', '2024-01-01 00:00:00', '', '', 'f', checkup['org_id']
            ))
    write_csv('c_checkup_summary.csv',
              ['id', 'checkup_id', 'summary_type', 'summary_content',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'],
              rows, output_dir)


def _gen_checkup_comparison(ctx, output_dir):
    rows = []
    # Group checkups by patient
    patient_checkups = {}
    for c in ctx.checkups:
        pid = c['patient_id']
        if pid not in patient_checkups:
            patient_checkups[pid] = []
        patient_checkups[pid].append(c)

    for pid, checkups in patient_checkups.items():
        if len(checkups) >= 2:
            sorted_cu = sorted(checkups, key=lambda x: x['checkup_date'])
            for i in range(1, len(sorted_cu)):
                if random.random() < 0.5:
                    oid = ctx.seq.next()
                    comparison = json.dumps({
                        'changes': random.choice([
                            '各项指标基本稳定',
                            '血糖较上次有所升高',
                            '血脂水平较上次改善',
                            '肝功能指标较上次无明显变化',
                        ]),
                        'new_abnormal': random.randint(0, 2),
                        'resolved_abnormal': random.randint(0, 1),
                    }, ensure_ascii=False)
                    rows.append((
                        oid, pid, sorted_cu[i]['id'], sorted_cu[i - 1]['id'],
                        comparison,
                        'system', '2024-01-01 00:00:00', '', '', 'f', sorted_cu[i]['org_id']
                    ))
    write_csv('c_checkup_comparison.csv',
              ['id', 'patient_id', 'current_checkup_id', 'previous_checkup_id',
               'comparison_result',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'],
              rows, output_dir)
