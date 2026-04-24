"""Generate extended clinical tables: c_imaging_exam, c_imaging_finding,
c_pathology, c_operation, c_clinical_note."""

import random
from datetime import timedelta
from .config import (BODY_PARTS, SPECIMEN_TYPES, PATH_GRADES, PATH_STAGES,
                     NOTE_TYPES, NOTE_CATEGORIES, DEPTS)
from .utils import (Ctx, rand_datetime, rand_float, fmt_dt, fmt_bool,
                    write_csv, choice_weighted)


def generate_extended(ctx: Ctx, output_dir: str):
    _gen_imaging(ctx, output_dir)
    _gen_pathology(ctx, output_dir)
    _gen_operation(ctx, output_dir)
    _gen_clinical_note(ctx, output_dir)


def _gen_imaging(ctx, output_dir):
    exam_rows = []
    finding_rows = []

    report_templates = {
        'CT': {
            '头部': '颅脑CT平扫示：脑实质密度{status}，中线结构{midline}，脑室系统{ventricle}。',
            '胸部': '胸部CT示：双肺纹理{texture}，{finding}。心影{heart}。',
            '腹部': '腹部CT示：肝脏{organ}，胆囊{gallbladder}，胰腺{pancreas}。',
            '盆腔': '盆腔CT示：膀胱{bladder}，{extra}。',
            '脊柱': '脊柱CT示：{vertebra}椎体{v_status}。',
        },
        'MRI': {
            '头部': '头颅MRI示：脑实质信号{signal}，{finding}。FLAIR序列{flair}。',
            '脊柱': '脊柱MRI示：{vertebra}椎间盘{disc_status}，{cord_status}。',
            '膝关节': '膝关节MRI示：{meniscus}，{ligament}。',
            '肩关节': '肩关节MRI示：{rotator_cuff}，{labrum}。',
            '腰椎': '腰椎MRI示：L{level}椎间盘{disc_status}，{cord_status}。',
        },
        'XRAY': {
            '胸部': '胸部X线示：双肺野{lung_field}，心影{heart}，膈面{diaphragm}。',
            '腹部': '腹部平片示：肠管{bowel}，{extra}。',
            '骨盆': '骨盆X线示：{bone_status}。',
            '四肢': '四肢X线示：{bone_status}，软组织{soft_tissue}。',
            '脊柱': '脊柱X线示：{vertebra}曲度{curve}，椎体{v_status}。',
        },
        'ULTRASOUND': {
            '腹部': '腹部彩超示：肝脏{organ}，胆囊{gallbladder}，胰腺{pancreas}，双肾{kidney}。',
            '甲状腺': '甲状腺彩超示：甲状腺{thyroid}，{nodule}。',
            '心脏': '心脏彩超示：各房室内径{chamber}，射血分数EF {ef}%，{valve}。',
            '泌尿系统': '泌尿系彩超示：双肾{kidney}，膀胱{bladder}，{prostate}。',
            '妇科': '妇科彩超示：子宫{uterus}，{adnexa}。',
        },
        'PET': {
            '全身': 'PET-CT示：{metabolism}，{finding}。',
            '胸部': 'PET-CT胸部示：{mass}，代谢{metabolism}。',
            '腹部': 'PET-CT腹部示：{finding}，代谢{metabolism}。',
        },
    }

    for enc in ctx.encounters:
        imaging_types = enc['disease'].get('imaging', [])
        if not imaging_types:
            if random.random() < 0.15:
                imaging_types = [random.choice(list(BODY_PARTS.keys()))]

        for exam_type in imaging_types:
            oid = ctx.seq.next()
            body_parts = BODY_PARTS.get(exam_type, ['胸部'])
            body_part = random.choice(body_parts)
            study_date = rand_datetime()
            accession = f'IMG{oid:08d}'
            modality = {'CT': 'CT', 'MRI': 'MR', 'XRAY': 'DR', 'ULTRASOUND': 'US', 'PET': 'PET'}[exam_type]

            template = report_templates.get(exam_type, {}).get(body_part, '{finding}。')
            report_text = template.format(
                status=random.choice(['正常', '稍减低', '未见明显异常']),
                midline=random.choice(['居中', '略偏移']),
                ventricle=random.choice(['无扩张', '轻度扩张', '未见明显扩张']),
                texture=random.choice(['清晰', '增粗', '正常']),
                finding=random.choice(['未见明显占位', '可见小片状高密度影', '未见明显异常']),
                heart=random.choice(['不大', '稍大', '正常大小']),
                organ=random.choice(['大小形态正常', '增大', '未见明显异常']),
                gallbladder=random.choice(['正常', '壁毛糙', '未见明显异常']),
                pancreas=random.choice(['正常', '未见明显异常']),
                bladder=random.choice(['充盈良好', '正常']),
                extra=random.choice(['未见明显异常', '未见明显游离液体']),
                vertebra=random.choice(['颈', '胸', '腰', 'L4/5']),
                v_status=random.choice(['未见明显异常', '退行性改变', '骨质增生']),
                signal=random.choice(['正常', '未见明显异常']),
                flair=random.choice(['未见明显高信号', '可见点状高信号']),
                disc_status=random.choice(['向后突出', '膨出', '正常']),
                cord_status=random.choice(['硬膜囊受压', '神经根受压', '未见明显受压']),
                meniscus=random.choice(['半月板信号正常', '内侧半月板后角损伤'],
                                       ),
                ligament=random.choice(['前后交叉韧带正常', '韧带信号正常']),
                rotator_cuff=random.choice(['肩袖完整', '冈上肌腱信号异常']),
                labrum=random.choice(['盂唇完整', '未见明显异常']),
                level=random.choice(['4/5', '5/S1', '3/4']),
                lung_field=random.choice(['清晰', '纹理增粗', '可见片状密度增高影']),
                diaphragm=random.choice(['光滑', '正常', '右侧膈面抬高']),
                bowel=random.choice(['未见明显扩张', '可见气液平', '正常']),
                bone_status=random.choice(['未见明显骨折征象', '骨质密度正常', '可见骨折线']),
                soft_tissue=random.choice(['正常', '稍肿胀']),
                curve=random.choice(['正常', '变直']),
                thyroid=random.choice(['大小正常', '弥漫性肿大', '体积正常']),
                nodule=random.choice(['未见明显结节', '可见低回声结节', '未见明显异常回声']),
                chamber=random.choice(['正常', '未见明显扩大']),
                ef=random.randint(50, 70),
                valve=random.choice(['各瓣膜未见明显反流', '二尖瓣轻度反流', '瓣膜功能正常']),
                kidney=random.choice(['大小形态正常', '未见明显积水', '可见强回声']),
                prostate=random.choice(['前列腺未见明显异常', '']),
                uterus=random.choice(['大小正常', '未见明显异常']),
                adnexa=random.choice(['双侧附件区未见明显异常', '']),
                metabolism=random.choice(['未见明显异常代谢增高灶', '可见代谢增高灶']),
                mass=random.choice(['未见明显占位', '可见软组织密度影']),
            )

            exam_rows.append((
                oid, enc['id'], enc['patient_id'],
                accession, exam_type, body_part,
                fmt_dt(study_date), modality, 'REPORTED',
                report_text, f'dicom/{exam_type.lower()}/{oid}',
                'system', '2024-01-01 00:00:00', '', '', 'f', enc['org_id']
            ))
            ctx.imaging_exams.append({'id': oid, 'encounter_id': enc['id'],
                                      'patient_id': enc['patient_id'], 'org_id': enc['org_id']})

            # Generate 1-3 findings
            n_findings = random.choices([1, 2, 3], weights=[40, 40, 20])[0]
            finding_types = ['结节', '炎症', '积液', '钙化', '肿块', '骨折', '增生', '囊肿']
            for _ in range(n_findings):
                fid = ctx.seq.next()
                laterality = random.choice(['', 'LEFT', 'RIGHT', 'BILATERAL'])
                severity = random.choice(['', 'MILD', 'MODERATE'])
                rows_f = (
                    fid, oid,
                    random.choice(finding_types),
                    random.choice(['未见明显异常', f'可见{random.choice(["小片状", "点状", "结节状"])}{random.choice(["高密度", "低密度", "等密度"])}影',
                                   '形态正常', '信号异常']),
                    laterality, severity, body_part,
                    '', 'system', '2024-01-01 00:00:00', '', '', 'f', enc['org_id']
                )
                finding_rows.append(rows_f)

    write_csv('c_imaging_exam.csv',
              ['id', 'encounter_id', 'patient_id', 'accession_no',
               'exam_type', 'body_part', 'study_date', 'modality', 'status',
               'report_text', 'dicom_bucket_path',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'],
              exam_rows, output_dir)

    write_csv('c_imaging_finding.csv',
              ['id', 'exam_id', 'finding_type', 'finding_desc',
               'laterality', 'severity', 'region', 'annotation_data',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'],
              finding_rows, output_dir)


def _gen_pathology(ctx, output_dir):
    rows = []
    for enc in ctx.encounters:
        if random.random() < 0.12:
            oid = ctx.seq.next()
            specimen = random.choice(SPECIMEN_TYPES)
            grade = random.choice(PATH_GRADES) if random.random() < 0.5 else ''
            stage = random.choice(PATH_STAGES) if random.random() < 0.5 else ''
            diag_desc = random.choice([
                '慢性炎症改变', '未见异型细胞', '炎性肉芽组织',
                '鳞状上皮增生', '腺体增生', '纤维组织增生',
                '符合{disease}改变'.format(disease=enc['disease']['name']),
                '未见恶性细胞', '轻度非典型增生', '组织细胞浸润',
            ])
            rows.append((
                oid, enc['id'], enc['patient_id'],
                f'PATH{oid:08d}', specimen, diag_desc,
                grade, stage,
                fmt_dt(rand_datetime()),
                'system', '2024-01-01 00:00:00', '', '', 'f', enc['org_id']
            ))
    write_csv('c_pathology.csv',
              ['id', 'encounter_id', 'patient_id',
               'specimen_no', 'specimen_type', 'diagnosis_desc',
               'grade', 'stage', 'report_date',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'],
              rows, output_dir)


def _gen_operation(ctx, output_dir):
    rows = []
    operations = [
        ('OP001', '阑尾切除术', 60), ('OP002', '腹腔镜胆囊切除术', 90),
        ('OP003', '骨折内固定术', 120), ('OP004', '全髋关节置换术', 180),
        ('OP005', '冠脉支架置入术', 60), ('OP006', '胃镜检查术', 30),
        ('OP007', '结肠镜检查术', 40), ('OP008', '甲状腺次全切除术', 120),
        ('OP009', '肾镜碎石术', 90), ('OP010', '全子宫切除术', 150),
    ]
    anesthesia_types = ['全身麻醉', '硬膜外麻醉', '局部麻醉', '腰麻', '臂丛神经阻滞']

    for enc in ctx.encounters:
        if enc.get('has_surgery', False):
            oid = ctx.seq.next()
            op = random.choice(operations)
            surgeon = random.choice(ctx.caregivers)
            assistant = random.choice(ctx.caregivers)
            operated = enc['admit'] + timedelta(hours=random.randint(6, 48))
            rows.append((
                oid, enc['id'], enc['patient_id'],
                op[0], op[1], fmt_dt(operated),
                op[2] + random.randint(-20, 30),
                surgeon['code'], f'{random.choice(ctx.caregivers)["code"]},{random.choice(ctx.caregivers)["code"]}',
                random.choice(anesthesia_types),
                'system', '2024-01-01 00:00:00', '', '', 'f', enc['org_id']
            ))
    write_csv('c_operation.csv',
              ['id', 'encounter_id', 'patient_id',
               'operation_code', 'operation_name', 'operated_at', 'duration_min',
               'surgeon', 'assistant', 'anesthesia_type',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'],
              rows, output_dir)


def _gen_clinical_note(ctx, output_dir):
    rows = []
    note_templates = {
        'ADMISSION': {
            'title': '入院记录',
            'content': '患者{gender}，{age}岁，因"{complaint}"入院。既往{history}。查体：T {temp}℃，P {pulse}次/分，R {resp}次/分，BP {bp}mmHg。初步诊断：{diagnosis}。',
        },
        'PROGRESS': {
            'title': '病程记录',
            'content': '患者今日一般情况{condition}。诉{symptom}。查体同前。目前治疗方案{treatment}，{plan}。',
        },
        'DISCHARGE': {
            'title': '出院小结',
            'content': '患者因"{diagnosis}"住院治疗{los}天，经{treatment}后病情{outcome}。出院诊断：{diagnosis}。出院医嘱：{instruction}。',
        },
        'CONSULTATION': {
            'title': '会诊记录',
            'content': '应{dept}邀请会诊。患者{condition}，{finding}。建议{recommendation}。',
        },
    }

    for enc in ctx.encounters:
        is_inpatient = enc['encounter_type'] in ('INPATIENT', 'EMERGENCY')
        n_notes = random.randint(2, 5) if is_inpatient else random.randint(1, 2)

        for _ in range(n_notes):
            oid = ctx.seq.next()
            if is_inpatient:
                ntype = random.choice(NOTE_TYPES)
            else:
                ntype = random.choice(['PROGRESS', 'CONSULTATION'])
            template = note_templates[ntype]
            pat = next(p for p in ctx.patients if p['id'] == enc['patient_id'])
            age = random.randint(18, 85)

            content = template['content'].format(
                gender='男' if pat['gender'] == 'M' else '女',
                age=age,
                complaint=enc['disease']['name'],
                history=random.choice(['体健', '高血压病史', '糖尿病史', '否认特殊病史']),
                temp=rand_float(36.5, 0.5, 35.5, 39.0),
                pulse=rand_float(80, 10, 60, 120, 0),
                resp=rand_float(18, 3, 12, 30, 0),
                bp=f'{rand_float(120, 15, 90, 160, 0):.0f}/{rand_float(80, 10, 60, 100, 0):.0f}',
                diagnosis=enc['disease']['name'],
                condition=random.choice(['良好', '平稳', '尚可', '一般']),
                symptom=random.choice(['无明显不适', '仍有轻微不适', '症状有所缓解', '偶有疼痛']),
                treatment=random.choice(['抗感染', '对症支持', '降糖降压', '规范治疗']),
                plan=random.choice(['继续目前方案', '密切观察病情变化', '择期复查']),
                los=enc.get('los_days', 1),
                outcome=random.choice(['好转', '明显好转', '稳定', '痊愈']),
                instruction=random.choice(['注意休息，定期复查', '规律服药，低盐低脂饮食', '出院后门诊随访']),
                dept=random.choice([d[1] for d in DEPTS]),
                finding=random.choice(['查体未见明显异常', '局部压痛阳性', '双肺呼吸音清']),
                recommendation=random.choice(['继续目前治疗', '加用相关药物', '完善相关检查']),
            )
            note_date = enc['admit'] + timedelta(hours=random.randint(0, enc.get('los_days', 1) * 24))

            rows.append((
                oid, enc['id'], enc['patient_id'],
                ntype, template['title'], content,
                random.choice(['DOC001', 'DOC002', 'DOC003']),
                fmt_dt(note_date),
                fmt_bool(random.random() < 0.02),
                random.choice(NOTE_CATEGORIES),
                'system', '2024-01-01 00:00:00', '', '', 'f', enc['org_id']
            ))
    write_csv('c_clinical_note.csv',
              ['id', 'encounter_id', 'patient_id',
               'note_type', 'title', 'content', 'author', 'note_date',
               'is_error', 'note_category',
               'created_by', 'created_at', 'updated_by', 'updated_at',
               'is_deleted', 'org_id'],
              rows, output_dir)
