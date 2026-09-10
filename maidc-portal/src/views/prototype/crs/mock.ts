/**
 * 专病管理（CRS 试点）原型 mock 数据
 * 依据 docs/superpowers/specs/2026-09-08-disease-management-design.md
 * 全部为静态数据，不调后端；量表条目由 definition JSONB 驱动（前端不硬编码）
 */

export const TODAY = '2026-09-09'

export const cohort = {
  id: 1,
  name: '慢性鼻窦炎',
  icdCode: 'J32',
  status: 'ACTIVE',
  autoSync: true,
  lastSyncAt: '2026-09-09 06:00',
  createdAt: '2026-08-01',
  description: '慢性鼻窦炎（CRS）专病队列，纳入确诊 J32 且近 2 年有就诊记录的患者',
  inclusionRules: {
    groupLogic: 'AND',
    groups: [
      { domain: 'DIAGNOSIS', logic: 'AND', conditions: [{ field: 'icd_code', operator: 'LIKE', value: 'J32%' }] },
      { domain: 'DIAGNOSIS', logic: 'AND', conditions: [{ field: 'icd_name', operator: 'CONTAINS', value: '鼻窦炎' }] },
    ],
  },
  stats: { totalPatients: 86, maleRatio: 58.1, avgAge: 44.6, recentCount: 5 },
}

/* ---------------- 量表定义（c_scale_definition） ---------------- */

const likert05 = [
  { value: 0, label: '没有' }, { value: 1, label: '轻微' }, { value: 2, label: '轻度' },
  { value: 3, label: '中度' }, { value: 4, label: '重度' }, { value: 5, label: '非常严重' },
]
const likert02 = [
  { value: 0, label: '无' }, { value: 1, label: '轻' }, { value: 2, label: '重' },
]
const likert03 = [
  { value: 0, label: '正常' }, { value: 1, label: '轻度减退' }, { value: 2, label: '中度减退' }, { value: 3, label: '完全丧失' },
]

const SNOT22_ITEMS = [
  '需要擤鼻涕', '鼻塞', '流涕', '打喷嚏', '咳嗽', '鼻涕倒流',
  '耳闷', '耳痛', '头晕/不稳', '头痛', '面部疼痛或压迫感', '嗅觉减退',
  '味觉减退', '睡眠困难', '夜间反复醒来', '睡眠质量差', '晨起疲乏',
  '疲劳', '效率下降', '注意力不集中', '沮丧/烦躁', '悲伤或尴尬',
]

export const scales = [
  {
    id: 1, scaleCode: 'SNOT22', name: 'SNOT-22 鼻窦炎结局测试（中文版）', version: 2,
    status: 'ACTIVE', refCount: 1,
    definition: {
      type: 'LIKERT_ITEMS',
      items: SNOT22_ITEMS.map((text, i) => ({ no: String(i + 1), text, options: likert05 })),
      maxScore: 110,
      mcid: 9,
      interpretation: [
        { min: 0, max: 20, label: '轻微' },
        { min: 21, max: 50, label: '中度' },
        { min: 51, max: 110, label: '重度' },
      ],
    },
  },
  {
    id: 2, scaleCode: 'LUND_KENNEDY', name: 'Lund-Kennedy 内镜评分', version: 1,
    status: 'ACTIVE', refCount: 1,
    definition: {
      type: 'LIKERT_ITEMS',
      items: [
        { no: 'L1', side: 'LEFT', text: '鼻息肉（左）', options: likert02 },
        { no: 'L2', side: 'LEFT', text: '鼻甲/黏膜水肿（左）', options: likert02 },
        { no: 'L3', side: 'LEFT', text: '分泌物（左）', options: likert02 },
        { no: 'R1', side: 'RIGHT', text: '鼻息肉（右）', options: likert02 },
        { no: 'R2', side: 'RIGHT', text: '鼻甲/黏膜水肿（右）', options: likert02 },
        { no: 'R3', side: 'RIGHT', text: '分泌物（右）', options: likert02 },
      ],
      maxScore: 12,
      mcid: null,
      interpretation: [{ min: 0, max: 12, label: '分值越低越好' }],
    },
  },
  {
    id: 3, scaleCode: 'LUND_MACKAY', name: 'Lund-Mackay CT 评分', version: 1,
    status: 'ACTIVE', refCount: 1,
    definition: {
      type: 'LIKERT_ITEMS',
      items: ['上颌窦', '前组筛窦', '后组筛窦', '蝶窦', '额窦', '窦口鼻道复合体'].flatMap((s, i) => [
        { no: `L${i + 1}`, side: 'LEFT', text: `${s}（左）`, options: likert02 },
        { no: `R${i + 1}`, side: 'RIGHT', text: `${s}（右）`, options: likert02 },
      ]),
      maxScore: 24,
      mcid: null,
      interpretation: [{ min: 0, max: 24, label: '分值越低越好' }],
    },
  },
  {
    id: 4, scaleCode: 'OLFACTION', name: '嗅觉主观分级', version: 1,
    status: 'ACTIVE', refCount: 1,
    definition: {
      type: 'LIKERT_ITEMS',
      items: [{ no: '1', text: '当前嗅觉状况', options: likert03 }],
      maxScore: 3,
      mcid: null,
      interpretation: [{ min: 0, max: 0, label: '正常' }, { min: 1, max: 3, label: '减退' }],
    },
  },
  {
    id: 5, scaleCode: 'CRS_AUX', name: 'CRS 随访辅助采集（业务数据带入）', version: 1,
    status: 'ACTIVE', refCount: 1,
    definition: {
      type: 'MIXED',
      items: [
        { no: '1', type: 'NUMBER', text: '血清总IgE（自动带入最近一次）', max: 5000,
          binding: { domain: 'LAB', field: 'TIGE', mode: 'AUTO_READONLY', windowDays: 180 } },
        { no: '2', type: 'NUMBER', text: '嗜酸性粒细胞比例（自动带入，可复核修正）', max: 100,
          binding: { domain: 'LAB', field: 'EOS_PCT', mode: 'AUTO_EDITABLE', windowDays: 180 } },
        { no: '3', type: 'NUMBER', text: '体重（自动带入，现场复测后可改）', max: 300,
          binding: { domain: 'VITAL', field: 'WEIGHT', mode: 'AUTO_EDITABLE', windowDays: 30 } },
        { no: '4', type: 'CHECKBOX', text: '当前在用药物（从用药记录枚举，请核对勾选）', options: [],
          binding: { domain: 'MEDICATION', field: 'ACTIVE_MEDS', mode: 'OPTIONS' } },
        { no: '5', type: 'INPUT', text: '补充说明', required: false },
      ],
      maxScore: 0,
      mcid: null,
      interpretation: [],
    },
  },
]

export const scaleByCode = (code: string) => scales.find(s => s.scaleCode === code)

/* ---------------- 量表数据绑定（items[].binding） ---------------- */
/**
 * 绑定语义：
 * - AUTO_READONLY  评估时自动带入 CDR 值，只读快照展示（检验值等客观指标）
 * - AUTO_EDITABLE  自动带入预填，允许执行人修改（体重等可复核项）
 * - OPTIONS        选择类控件的选项在评估时按患者动态枚举（如"当前在用药物"）
 * 值随评估落 answers 快照并记录来源；windowDays 为取值时间窗（空 = 不限，取最近一次）
 */
export type BindingDomain = 'LAB' | 'VITAL' | 'MEDICATION' | 'DIAGNOSIS' | 'IMAGING'
export type BindingMode = 'AUTO_READONLY' | 'AUTO_EDITABLE' | 'OPTIONS'
export interface ScaleBinding {
  domain: BindingDomain
  field: string
  mode: BindingMode
  windowDays?: number | null
}
export interface BindingValue { value: number | string; at?: string; unit?: string; label?: string }

export const BINDING_DOMAIN_META: Record<BindingDomain, { label: string; color: string }> = {
  LAB: { label: '检验', color: 'purple' },
  VITAL: { label: '体征', color: 'green' },
  MEDICATION: { label: '用药', color: 'orange' },
  DIAGNOSIS: { label: '诊断', color: 'blue' },
  IMAGING: { label: '影像', color: 'cyan' },
}

export const BINDING_CATALOG: { domain: BindingDomain; fields: { code: string; label: string; unit?: string }[] }[] = [
  {
    domain: 'LAB', fields: [
      { code: 'TIGE', label: '血清总IgE', unit: 'kU/L' },
      { code: 'EOS_PCT', label: '嗜酸性粒细胞比例', unit: '%' },
      { code: 'EOS_ABS', label: '嗜酸性粒细胞绝对值', unit: '×10⁹/L' },
      { code: 'CRP', label: 'C反应蛋白', unit: 'mg/L' },
    ],
  },
  {
    domain: 'VITAL', fields: [
      { code: 'WEIGHT', label: '体重', unit: 'kg' },
      { code: 'HEIGHT', label: '身高', unit: 'cm' },
      { code: 'BMI', label: 'BMI', unit: 'kg/m²' },
      { code: 'BP_SYSTOLIC', label: '收缩压', unit: 'mmHg' },
    ],
  },
  {
    domain: 'MEDICATION', fields: [
      { code: 'ACTIVE_MEDS', label: '当前在用药物（枚举）' },
      { code: 'NASAL_STEROID', label: '在用鼻用激素（枚举）' },
    ],
  },
  {
    domain: 'DIAGNOSIS', fields: [
      { code: 'ACTIVE_DX', label: '现有诊断（枚举）' },
      { code: 'ALLERGY_DX', label: '过敏相关诊断（枚举）' },
    ],
  },
  {
    domain: 'IMAGING', fields: [
      { code: 'RECENT_CT', label: '近一次鼻窦CT报告' },
      { code: 'RECENT_ENDOSCOPY', label: '近一次鼻内镜报告' },
    ],
  },
]

export function bindingFieldLabel(domain: string, field: string): string {
  return BINDING_CATALOG.find(d => d.domain === domain)?.fields.find(f => f.code === field)?.label || field
}

/* 按患者模拟的可解析业务值（真实实现 = CDR 查询服务） */
const BINDING_VALUES: Record<number, Record<string, BindingValue | BindingValue[]>> = {
  9001: {  // 王建国
    'LAB|TIGE': { value: 182, at: '2026-08-30', unit: 'kU/L' },
    'LAB|EOS_PCT': { value: 7.8, at: '2026-08-30', unit: '%' },
    'VITAL|WEIGHT': { value: 74, at: '2026-09-02', unit: 'kg' },
    'VITAL|BMI': { value: 24.2, at: '2026-09-02', unit: 'kg/m²' },
    'MEDICATION|ACTIVE_MEDS': [
      { value: '糠酸莫米松鼻喷剂' }, { value: '克拉霉素缓释片' }, { value: '生理盐水冲洗' },
    ],
  },
  9005: {  // 孙国庆
    'LAB|TIGE': { value: 96, at: '2026-09-01', unit: 'kU/L' },
    'LAB|EOS_PCT': { value: 4.2, at: '2026-09-01', unit: '%' },
    'VITAL|WEIGHT': { value: 68, at: '2026-09-05', unit: 'kg' },
    'VITAL|BMI': { value: 23.1, at: '2026-09-05', unit: 'kg/m²' },
    'MEDICATION|ACTIVE_MEDS': [
      { value: '布地奈德鼻喷剂' }, { value: '孟鲁司特钠片' },
    ],
  },
}

export function resolveBinding(patientId: number | undefined, binding?: ScaleBinding | null): BindingValue | BindingValue[] | null {
  if (!patientId || !binding?.domain || !binding?.field) return null
  return BINDING_VALUES[patientId]?.[`${binding.domain}|${binding.field}`] ?? null
}

/* ---------------- 随访方案（c_followup_protocol） ---------------- */

export const protocol = {
  id: 10, cohortId: 1, name: 'CRS 慢性鼻窦炎随访方案', version: 2, status: 'PUBLISHED',
  updatedAt: '2026-09-05',
  stages: [
    { stageCode: 'BASELINE', name: '基线', offsetDays: 0, requiredScales: ['SNOT22', 'LUND_KENNEDY', 'OLFACTION'], optionalScales: ['LUND_MACKAY'], note: '建档当日完成' },
    { stageCode: 'M3', name: '3个月随访', offsetDays: 90, requiredScales: ['SNOT22'], optionalScales: [], note: '' },
    { stageCode: 'M6', name: '6个月随访', offsetDays: 180, requiredScales: ['SNOT22', 'LUND_KENNEDY'], optionalScales: [], note: '' },
    { stageCode: 'M12', name: '12个月随访', offsetDays: 365, requiredScales: ['SNOT22'], optionalScales: ['LUND_MACKAY'], note: '复查CT可选' },
  ],
}

/* ---------------- 患者随访档案（c_patient_followup） ---------------- */

export interface Task {
  id: number
  stageCode: string
  stageName: string
  dueDate: string
  status: 'PENDING' | 'DONE' | 'SKIPPED'
  requiredScales: string[]
  optionalScales?: string[]
  completedAt?: string
  completedByName?: string
  skipReason?: string
}
export interface Assessment {
  taskId?: number
  scaleCode: string
  stageName: string
  assessedAt: string
  totalScore: number
  interpretation: string
}
export interface Treatment {
  id: number
  category: 'MEDICATION' | 'SURGERY' | 'OTHER'
  name: string
  occurredDate: string
  source: 'MANUAL' | 'CDR'
  detail: Record<string, any>
}
export interface Followup {
  id: number
  patientId: number
  patientName: string
  gender: string
  age: number
  doctorName: string
  nurseName: string
  status: 'ACTIVE' | 'SUSPENDED' | 'CLOSED' | 'OUT_OF_COHORT'
  enrollDate: string
  closeReason?: string
  protocolVersion: number
  tasks: Task[]
  assessments: Assessment[]
  treatments: Treatment[]
}

export const followups: Followup[] = [
  {
    id: 101, patientId: 9001, patientName: '王建国', gender: '男', age: 52,
    doctorName: '陈志远', nurseName: '刘敏', status: 'ACTIVE', enrollDate: '2026-06-09', protocolVersion: 2,
    tasks: [
      { id: 1, stageCode: 'BASELINE', stageName: '基线', dueDate: '2026-06-09', status: 'DONE', requiredScales: ['SNOT22', 'LUND_KENNEDY', 'OLFACTION'], completedAt: '2026-06-09', completedByName: '刘敏' },
      { id: 2, stageCode: 'M3', stageName: '3个月随访', dueDate: '2026-09-06', status: 'PENDING', requiredScales: ['SNOT22'] },
      { id: 3, stageCode: 'M6', stageName: '6个月随访', dueDate: '2026-12-06', status: 'PENDING', requiredScales: ['SNOT22', 'LUND_KENNEDY'] },
      { id: 4, stageCode: 'M12', stageName: '12个月随访', dueDate: '2027-06-09', status: 'PENDING', requiredScales: ['SNOT22'] },
    ],
    assessments: [
      { taskId: 1, scaleCode: 'SNOT22', stageName: '基线', assessedAt: '2026-06-09', totalScore: 45, interpretation: '中度' },
      { taskId: 1, scaleCode: 'LUND_KENNEDY', stageName: '基线', assessedAt: '2026-06-09', totalScore: 8, interpretation: '分值越低越好' },
      { taskId: 1, scaleCode: 'OLFACTION', stageName: '基线', assessedAt: '2026-06-09', totalScore: 2, interpretation: '减退' },
    ],
    treatments: [
      { id: 1, category: 'MEDICATION', name: '糠酸莫米松鼻喷剂', occurredDate: '2026-06-10', source: 'CDR', detail: { dosage: '每侧2喷 qd', route: '鼻用', startDate: '2026-06-10', endDate: null } },
      { id: 2, category: 'SURGERY', name: 'ESS 功能性内镜鼻窦手术', occurredDate: '2026-07-15', source: 'MANUAL', detail: { surgeon: '陈志远', hospitalDays: 5, side: '双侧' } },
    ],
  },
  {
    id: 102, patientId: 9002, patientName: '李淑芳', gender: '女', age: 47,
    doctorName: '陈志远', nurseName: '刘敏', status: 'ACTIVE', enrollDate: '2026-09-01', protocolVersion: 2,
    tasks: [
      { id: 5, stageCode: 'BASELINE', stageName: '基线', dueDate: '2026-09-01', status: 'DONE', requiredScales: ['SNOT22', 'LUND_KENNEDY', 'OLFACTION'], completedAt: '2026-09-01', completedByName: '刘敏' },
      { id: 6, stageCode: 'M3', stageName: '3个月随访', dueDate: '2026-11-30', status: 'PENDING', requiredScales: ['SNOT22'] },
      { id: 7, stageCode: 'M6', stageName: '6个月随访', dueDate: '2027-03-01', status: 'PENDING', requiredScales: ['SNOT22', 'LUND_KENNEDY'] },
      { id: 8, stageCode: 'M12', stageName: '12个月随访', dueDate: '2027-09-01', status: 'PENDING', requiredScales: ['SNOT22'] },
    ],
    assessments: [
      { taskId: 5, scaleCode: 'SNOT22', stageName: '基线', assessedAt: '2026-09-01', totalScore: 58, interpretation: '重度' },
      { taskId: 5, scaleCode: 'LUND_KENNEDY', stageName: '基线', assessedAt: '2026-09-01', totalScore: 10, interpretation: '分值越低越好' },
    ],
    treatments: [
      { id: 3, category: 'MEDICATION', name: '克拉霉素缓释片', occurredDate: '2026-09-02', source: 'CDR', detail: { dosage: '0.25g qd', route: '口服', startDate: '2026-09-02', endDate: '2026-12-02' } },
    ],
  },
  {
    id: 103, patientId: 9003, patientName: '赵德柱', gender: '男', age: 61,
    doctorName: '吴静怡', nurseName: '孙萍', status: 'SUSPENDED', enrollDate: '2026-03-15', protocolVersion: 1,
    tasks: [
      { id: 9, stageCode: 'BASELINE', stageName: '基线', dueDate: '2026-03-15', status: 'DONE', requiredScales: ['SNOT22', 'LUND_KENNEDY', 'OLFACTION'], completedAt: '2026-03-16', completedByName: '孙萍' },
      { id: 10, stageCode: 'M3', stageName: '3个月随访', dueDate: '2026-06-13', status: 'DONE', requiredScales: ['SNOT22'], completedAt: '2026-06-13', completedByName: '孙萍' },
      { id: 11, stageCode: 'M6', stageName: '6个月随访', dueDate: '2026-09-11', status: 'PENDING', requiredScales: ['SNOT22', 'LUND_KENNEDY'] },
      { id: 12, stageCode: 'M12', stageName: '12个月随访', dueDate: '2027-03-15', status: 'PENDING', requiredScales: ['SNOT22'] },
    ],
    assessments: [
      { taskId: 9, scaleCode: 'SNOT22', stageName: '基线', assessedAt: '2026-03-15', totalScore: 62, interpretation: '重度' },
      { taskId: 10, scaleCode: 'SNOT22', stageName: '3个月随访', assessedAt: '2026-06-13', totalScore: 41, interpretation: '中度' },
    ],
    treatments: [],
  },
  {
    id: 104, patientId: 9004, patientName: '周丽华', gender: '女', age: 38,
    doctorName: '陈志远', nurseName: '刘敏', status: 'ACTIVE', enrollDate: '2026-08-20', protocolVersion: 2,
    tasks: [
      { id: 13, stageCode: 'BASELINE', stageName: '基线', dueDate: '2026-08-20', status: 'DONE', requiredScales: ['SNOT22', 'LUND_KENNEDY', 'OLFACTION'], completedAt: '2026-08-20', completedByName: '刘敏' },
      { id: 14, stageCode: 'M3', stageName: '3个月随访', dueDate: '2026-09-08', status: 'PENDING', requiredScales: ['SNOT22'] },
      { id: 15, stageCode: 'M6', stageName: '6个月随访', dueDate: '2027-02-16', status: 'PENDING', requiredScales: ['SNOT22', 'LUND_KENNEDY'] },
      { id: 16, stageCode: 'M12', stageName: '12个月随访', dueDate: '2027-08-20', status: 'PENDING', requiredScales: ['SNOT22'] },
    ],
    assessments: [
      { taskId: 13, scaleCode: 'SNOT22', stageName: '基线', assessedAt: '2026-08-20', totalScore: 33, interpretation: '中度' },
    ],
    treatments: [],
  },
  {
    id: 105, patientId: 9005, patientName: '孙国庆', gender: '男', age: 55,
    doctorName: '吴静怡', nurseName: '刘敏', status: 'ACTIVE', enrollDate: '2026-09-09', protocolVersion: 2,
    tasks: [
      { id: 17, stageCode: 'BASELINE', stageName: '基线', dueDate: '2026-09-09', status: 'PENDING', requiredScales: ['SNOT22', 'LUND_KENNEDY', 'OLFACTION'], optionalScales: ['CRS_AUX'] },
      { id: 18, stageCode: 'M3', stageName: '3个月随访', dueDate: '2026-12-08', status: 'PENDING', requiredScales: ['SNOT22'] },
      { id: 19, stageCode: 'M6', stageName: '6个月随访', dueDate: '2027-03-09', status: 'PENDING', requiredScales: ['SNOT22', 'LUND_KENNEDY'] },
      { id: 20, stageCode: 'M12', stageName: '12个月随访', dueDate: '2027-09-09', status: 'PENDING', requiredScales: ['SNOT22'] },
    ],
    assessments: [],
    treatments: [],
  },
  {
    id: 106, patientId: 9006, patientName: '钱伟东', gender: '男', age: 44,
    doctorName: '陈志远', nurseName: '刘敏', status: 'CLOSED', enrollDate: '2025-11-10', protocolVersion: 1,
    closeReason: '患者转外院治疗，主动退出随访',
    tasks: [
      { id: 21, stageCode: 'BASELINE', stageName: '基线', dueDate: '2025-11-10', status: 'DONE', requiredScales: ['SNOT22', 'LUND_KENNEDY', 'OLFACTION'], completedAt: '2025-11-10', completedByName: '刘敏' },
      { id: 22, stageCode: 'M3', stageName: '3个月随访', dueDate: '2026-02-08', status: 'SKIPPED', requiredScales: ['SNOT22'], skipReason: '患者外地出差，电话随访替代' },
      { id: 23, stageCode: 'M6', stageName: '6个月随访', dueDate: '2026-05-09', status: 'DONE', requiredScales: ['SNOT22', 'LUND_KENNEDY'], completedAt: '2026-05-09', completedByName: '刘敏' },
      { id: 24, stageCode: 'M12', stageName: '12个月随访', dueDate: '2026-11-10', status: 'PENDING', requiredScales: ['SNOT22'] },
    ],
    assessments: [
      { taskId: 21, scaleCode: 'SNOT22', stageName: '基线', assessedAt: '2025-11-10', totalScore: 51, interpretation: '重度' },
      { taskId: 23, scaleCode: 'SNOT22', stageName: '6个月随访', assessedAt: '2026-05-09', totalScore: 29, interpretation: '中度' },
    ],
    treatments: [
      { id: 4, category: 'MEDICATION', name: '布地奈德鼻喷剂', occurredDate: '2025-11-12', source: 'CDR', detail: { dosage: '每侧2喷 qd', route: '鼻用', startDate: '2025-11-12', endDate: '2026-05-12' } },
    ],
  } as unknown as Followup,
  {
    id: 107, patientId: 9008, patientName: '郑桂香', gender: '女', age: 66,
    doctorName: '吴静怡', nurseName: '刘敏', status: 'ACTIVE', enrollDate: '2026-06-14', protocolVersion: 2,
    tasks: [
      { id: 25, stageCode: 'BASELINE', stageName: '基线', dueDate: '2026-06-14', status: 'DONE', requiredScales: ['SNOT22', 'LUND_KENNEDY', 'OLFACTION'], completedAt: '2026-06-14', completedByName: '刘敏' },
      { id: 26, stageCode: 'M3', stageName: '3个月随访', dueDate: '2026-09-12', status: 'PENDING', requiredScales: ['SNOT22'] },
      { id: 27, stageCode: 'M6', stageName: '6个月随访', dueDate: '2026-12-11', status: 'PENDING', requiredScales: ['SNOT22', 'LUND_KENNEDY'] },
      { id: 28, stageCode: 'M12', stageName: '12个月随访', dueDate: '2027-06-14', status: 'PENDING', requiredScales: ['SNOT22'] },
    ],
    assessments: [
      { taskId: 25, scaleCode: 'SNOT22', stageName: '基线', assessedAt: '2026-06-14', totalScore: 51, interpretation: '重度' },
    ],
    treatments: [],
  },
]

/* ---------------- 工作台（今日/超期/未来 视图） ---------------- */

export interface WorkbenchRow {
  taskId: number
  followupId: number
  patientId: number
  patientName: string
  gender: string
  age: number
  cohortName: string
  stageName: string
  requiredScales: string[]
  optionalScales?: string[]
  dueDate: string
  overdueDays: number   // 0 = 未超期
  doctorName: string
  nurseName: string
}

export const workbenchRows: WorkbenchRow[] = followups
  .filter(f => f.status === 'ACTIVE')
  .flatMap(f => f.tasks
    .filter(t => t.status === 'PENDING')
    .map(t => ({
      taskId: t.id,
      followupId: f.id,
      patientId: f.patientId,
      patientName: f.patientName,
      gender: f.gender,
      age: f.age,
      cohortName: '慢性鼻窦炎',
      stageName: t.stageName,
      requiredScales: t.requiredScales,
      optionalScales: t.optionalScales || [],
      dueDate: t.dueDate,
      overdueDays: Math.max(0, Math.round((new Date(TODAY).getTime() - new Date(t.dueDate).getTime()) / 86400000)),
      doctorName: f.doctorName,
      nurseName: f.nurseName,
    })))

/* ---------------- 结局统计（outcome-stats 聚合） ---------------- */

export const outcomeStats = {
  complianceRate: 78.6,        // 随访依从率
  improvementRate: 62.5,       // SNOT-22 改善率（基线-末次 ≥ MCID 9）
  surgeryRate: 33.3,           // 手术率
  reoperationCount: 0,         // 再手术患者数
  scaleTrends: {
    SNOT22: { name: 'SNOT-22（↓好）', points: [['基线', 49.8], ['3个月', 38.2], ['6个月', 25.0], ['12个月', 20.6]] },
    LUND_KENNEDY: { name: 'Lund-Kennedy（↓好）', points: [['基线', 9.0], ['3个月', 6.4], ['6个月', 4.1], ['12个月', 3.0]] },
    LUND_MACKAY: { name: 'Lund-Mackay（↓好）', points: [['基线', 14.0], ['3个月', 10.2], ['6个月', 7.5], ['12个月', 6.0]] },
    OLFACTION: { name: '嗅觉分级（↓好）', points: [['基线', 1.9], ['3个月', 1.4], ['6个月', 0.8], ['12个月', 0.6]] },
  },
  medicationDistribution: [
    { name: '鼻用糖皮质激素', value: 38 },
    { name: '大环内酯类', value: 18 },
    { name: '盐水冲洗', value: 25 },
    { name: '抗白三烯', value: 8 },
    { name: '生物制剂', value: 4 },
    { name: '口服激素', value: 7 },
  ],
}

/* ---------------- CDR 可带入治疗记录（import-cdr 勾选） ---------------- */

export const cdrTreatmentCandidates = [
  { resourceType: 'MedicationRequest', recordId: 88101, name: '糠酸莫米松鼻喷剂', occurredDate: '2026-06-10', category: 'MEDICATION', exists: false },
  { resourceType: 'Procedure', recordId: 88102, name: '鼻内镜检查', occurredDate: '2026-06-10', category: 'OTHER', exists: false },
  { resourceType: 'MedicationRequest', recordId: 88045, name: '克拉霉素缓释片', occurredDate: '2026-05-02', category: 'MEDICATION', exists: true },
]

/* ---------------- 队列患者（患者队列 Tab，含建档状态） ---------------- */

export const cohortPatients = [
  { patientId: 9001, patientName: '王建国', gender: '男', age: 52, matchSource: 'AUTO', matchedAt: '2026-08-01 06:00', followed: true },
  { patientId: 9002, patientName: '李淑芳', gender: '女', age: 47, matchSource: 'AUTO', matchedAt: '2026-08-01 06:00', followed: true },
  { patientId: 9003, patientName: '赵德柱', gender: '男', age: 61, matchSource: 'AUTO', matchedAt: '2026-08-01 06:00', followed: true },
  { patientId: 9004, patientName: '周丽华', gender: '女', age: 38, matchSource: 'MANUAL', matchedAt: '2026-08-21 10:12', followed: true },
  { patientId: 9005, patientName: '孙国庆', gender: '男', age: 55, matchSource: 'AUTO', matchedAt: '2026-09-09 06:00', followed: true },
  { patientId: 9007, patientName: '吴文博', gender: '男', age: 29, matchSource: 'AUTO', matchedAt: '2026-08-15 06:00', followed: false },
  { patientId: 9008, patientName: '郑桂香', gender: '女', age: 66, matchSource: 'AUTO', matchedAt: '2026-08-15 06:00', followed: true },
]

/* ---------------- 工具函数 ---------------- */

export function taskDerivedStatus(t: Task): 'DONE' | 'SKIPPED' | 'OVERDUE' | 'PENDING' {
  if (t.status !== 'PENDING') return t.status
  return t.dueDate < TODAY ? 'OVERDUE' : 'PENDING'
}

export function interpretScore(scaleCode: string, score: number): string {
  const def = scaleByCode(scaleCode)?.definition
  if (!def?.interpretation) return ''
  const hit = def.interpretation.find((r: any) => score >= r.min && score <= r.max)
  return hit?.label ?? ''
}

export const STATUS_META: Record<string, { label: string; color: string }> = {
  ACTIVE: { label: '随访中', color: 'processing' },
  SUSPENDED: { label: '已暂停', color: 'warning' },
  CLOSED: { label: '已结案', color: 'default' },
  OUT_OF_COHORT: { label: '已脱组', color: 'error' },
  DONE: { label: '已完成', color: 'success' },
  SKIPPED: { label: '已跳过', color: 'default' },
  OVERDUE: { label: '已超期', color: 'error' },
  PENDING: { label: '待办', color: 'processing' },
}
