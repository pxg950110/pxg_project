/**
 * 医学五字典（药品/诊断/检验/检查/收费）配置驱动单一来源。
 * DictionaryListView 引擎按此 schema 渲染筛选项、表格、详情抽屉与新建/编辑表单。
 * API 适配器直接复用 @/api/medical-dictionary（端点契约唯一真源）。
 */
import {
  getDrugs, getDrug, createDrug, updateDrug, deleteDrug,
  getDiagnoses, getDiagnosis, createDiagnosis, updateDiagnosis, deleteDiagnosis,
  getLabItems, getLabItem, createLabItem, updateLabItem, deleteLabItem, getLabCategories,
  getExamItems, getExamItem, createExamItem, updateExamItem, deleteExamItem, getExamTypes,
  getFeeItems, getFeeItem, createFeeItem, updateFeeItem, deleteFeeItem, getFeeCategories,
  getDrugCategories,
} from '@/api/medical-dictionary'

export type DictionaryTypeKey = 'drug' | 'diagnosis' | 'lab' | 'exam' | 'fee'

export interface DictionaryRecord {
  id: number
  [key: string]: any
}

export interface SelectOption {
  label: string
  value: any
}

/** 动态选项集：由引擎在挂载时加载（drugCategoryTree 为树形数据，仅用于表单 tree-select） */
export type OptionsKey = 'labCategories' | 'examTypes' | 'feeCategories' | 'drugCategoryTree'

export interface FormFieldDef {
  name: string
  label: string
  type: 'input' | 'number' | 'select' | 'textarea' | 'tree-select'
  placeholder?: string
  options?: SelectOption[]
  optionsKey?: OptionsKey
  required?: boolean
  requiredMessage?: string
  disabledOnEdit?: boolean
  default?: any
  /** 占整行（备注等） */
  full?: boolean
}

export interface FilterDef {
  name: string
  placeholder: string
  options?: SelectOption[]
  optionsKey?: OptionsKey
}

export interface ColumnDef {
  title: string
  key?: string
  dataIndex?: string
  width?: number
  ellipsis?: boolean
  fixed?: string
}

export interface DetailFieldDef {
  label: string
  name: string
  render?: 'tag' | 'price' | 'refRange' | 'rx'
  span?: 1 | 2
}

export type SidePanelKey = 'drugCategoryTree' | 'icdChapterMenu'

export interface DictionaryApi {
  list: (params?: any) => Promise<any>
  get: (id: number) => Promise<any>
  create: (data: any) => Promise<any>
  update: (id: number, data: any) => Promise<any>
  remove: (id: number) => Promise<any>
}

export interface DictionarySchema {
  key: DictionaryTypeKey
  title: string
  newLabel: string
  editLabel: string
  detailTitle: string
  modalWidth: number
  scrollX: number
  keywordPlaceholder: string
  filters: FilterDef[]
  columns: ColumnDef[]
  fields: FormFieldDef[]
  detailFields: DetailFieldDef[]
  sidePanel?: SidePanelKey
  /** 侧栏选中值注入列表查询的参数名 */
  sideParam?: string
  showImport?: boolean
}

// ==================== 公共选项与色值 ====================

export const STATUS_OPTIONS: SelectOption[] = [
  { label: '启用', value: 'ACTIVE' },
  { label: '停用', value: 'INACTIVE' },
]

export const INSURANCE_TYPE_OPTIONS: SelectOption[] = [
  { label: '甲类', value: '甲类' },
  { label: '乙类', value: '乙类' },
  { label: '丙类', value: '丙类' },
  { label: '自费', value: '自费' },
]

export const FEE_TYPE_OPTIONS = INSURANCE_TYPE_OPTIONS

export const FEE_CATEGORY_OPTIONS: SelectOption[] = [
  { label: '药品', value: '药品' },
  { label: '诊疗', value: '诊疗' },
  { label: '检验', value: '检验' },
  { label: '检查', value: '检查' },
  { label: '材料', value: '材料' },
  { label: '服务', value: '服务' },
]

export const RESULT_TYPE_OPTIONS: SelectOption[] = [
  { label: '数值', value: '数值' },
  { label: '文本', value: '文本' },
  { label: '定性', value: '定性' },
]

export const EXAM_TYPE_OPTIONS: SelectOption[] = [
  { label: 'CT', value: 'CT' },
  { label: 'MRI', value: 'MRI' },
  { label: 'X线', value: 'X线' },
  { label: '超声', value: '超声' },
  { label: '心电', value: '心电' },
  { label: '内镜', value: '内镜' },
  { label: '病理', value: '病理' },
]

export const CONTRAST_TYPE_OPTIONS: SelectOption[] = [
  { label: '无', value: '无' },
  { label: '增强', value: '增强' },
  { label: '造影', value: '造影' },
]

/** 状态标签 */
export const STATUS_TAG: Record<string, { color: string; label: string }> = {
  ACTIVE: { color: 'green', label: '启用' },
  INACTIVE: { color: 'default', label: '停用' },
}

/** 医保类别 / 费别标签色 */
export const TYPE_TAG_COLORS: Record<string, string> = {
  '甲类': 'green',
  '乙类': 'blue',
  '丙类': 'orange',
  '自费': 'default',
}

export const dynamicOptionLoaders: Record<OptionsKey, () => Promise<any>> = {
  labCategories: getLabCategories,
  examTypes: getExamTypes,
  feeCategories: getFeeCategories,
  drugCategoryTree: getDrugCategories,
}

export const dictionaryApis: Record<DictionaryTypeKey, DictionaryApi> = {
  drug: { list: getDrugs, get: getDrug, create: createDrug, update: updateDrug, remove: deleteDrug },
  diagnosis: { list: getDiagnoses, get: getDiagnosis, create: createDiagnosis, update: updateDiagnosis, remove: deleteDiagnosis },
  lab: { list: getLabItems, get: getLabItem, create: createLabItem, update: updateLabItem, remove: deleteLabItem },
  exam: { list: getExamItems, get: getExamItem, create: createExamItem, update: updateExamItem, remove: deleteExamItem },
  fee: { list: getFeeItems, get: getFeeItem, create: createFeeItem, update: updateFeeItem, remove: deleteFeeItem },
}

const statusField = (full = true): FormFieldDef => ({
  name: 'status',
  label: '状态',
  type: 'select',
  options: STATUS_OPTIONS,
  default: 'ACTIVE',
  full,
})

const remarkField = (): FormFieldDef => ({
  name: 'remark',
  label: '备注',
  type: 'textarea',
  placeholder: '备注信息',
  full: true,
})

// ==================== 五类字典 Schema ====================

export const dictionarySchemas: Record<DictionaryTypeKey, DictionarySchema> = {
  drug: {
    key: 'drug',
    title: '药品字典',
    newLabel: '新增药品',
    editLabel: '编辑药品',
    detailTitle: '药品详情',
    modalWidth: 800,
    scrollX: 1400,
    keywordPlaceholder: '搜索编码/名称/商品名/拼音',
    showImport: true,
    sidePanel: 'drugCategoryTree',
    sideParam: 'categoryId',
    filters: [
      { name: 'status', placeholder: '状态', options: STATUS_OPTIONS },
      { name: 'insuranceType', placeholder: '医保类别', options: INSURANCE_TYPE_OPTIONS },
    ],
    columns: [
      { title: '药品编码', dataIndex: 'drugCode', width: 120 },
      { title: '通用名', key: 'name', width: 180, ellipsis: true },
      { title: '商品名', dataIndex: 'tradeName', width: 140, ellipsis: true },
      { title: '剂型', dataIndex: 'dosageForm', width: 80 },
      { title: '规格', dataIndex: 'specification', width: 100 },
      { title: '单价', key: 'price', width: 80 },
      { title: '医保', key: 'insuranceType', width: 70 },
      { title: '生产厂家', dataIndex: 'manufacturer', width: 150, ellipsis: true },
      { title: '状态', key: 'status', width: 70 },
      { title: '操作', key: 'actions', width: 100, fixed: 'right' },
    ],
    fields: [
      { name: 'drugCode', label: '药品编码', type: 'input', placeholder: '药品编码', required: true, requiredMessage: '请输入药品编码', disabledOnEdit: true },
      { name: 'name', label: '通用名', type: 'input', placeholder: '药品通用名', required: true, requiredMessage: '请输入药品名称' },
      { name: 'tradeName', label: '商品名', type: 'input', placeholder: '商品名' },
      { name: 'nameEn', label: '英文名', type: 'input', placeholder: '英文名称' },
      { name: 'categoryId', label: '药理分类', type: 'tree-select', optionsKey: 'drugCategoryTree', placeholder: '选择分类' },
      { name: 'atcCode', label: 'ATC编码', type: 'input', placeholder: 'ATC编码' },
      { name: 'dosageForm', label: '剂型', type: 'input', placeholder: '剂型' },
      { name: 'specification', label: '规格', type: 'input', placeholder: '规格' },
      { name: 'unit', label: '单位', type: 'input', placeholder: '计量单位' },
      { name: 'price', label: '单价', type: 'number', placeholder: '单价' },
      { name: 'insuranceCode', label: '医保编码', type: 'input', placeholder: '医保编码' },
      { name: 'insuranceType', label: '医保类别', type: 'select', options: INSURANCE_TYPE_OPTIONS, placeholder: '医保类别' },
      { name: 'manufacturer', label: '生产厂家', type: 'input', placeholder: '生产厂家' },
      statusField(false),
      remarkField(),
    ],
    detailFields: [
      { label: '药品编码', name: 'drugCode' },
      { label: '通用名', name: 'name' },
      { label: '商品名', name: 'tradeName' },
      { label: '英文名', name: 'nameEn' },
      { label: '分类', name: 'categoryName' },
      { label: 'ATC编码', name: 'atcCode' },
      { label: '剂型', name: 'dosageForm' },
      { label: '规格', name: 'specification' },
      { label: '单位', name: 'unit' },
      { label: '单价', name: 'price', render: 'price' },
      { label: '医保编码', name: 'insuranceCode' },
      { label: '医保类别', name: 'insuranceType', render: 'tag' },
      { label: '生产厂家', name: 'manufacturer', span: 2 },
      { label: '状态', name: 'status', render: 'tag' },
      { label: '处方药', name: 'isPrescription', render: 'rx' },
      { label: '备注', name: 'remark', span: 2 },
    ],
  },

  diagnosis: {
    key: 'diagnosis',
    title: '诊断字典',
    newLabel: '新增诊断',
    editLabel: '编辑诊断',
    detailTitle: '诊断详情',
    modalWidth: 800,
    scrollX: 1200,
    keywordPlaceholder: '搜索诊断编码/名称/ICD-10',
    sidePanel: 'icdChapterMenu',
    sideParam: 'chapterCode',
    filters: [
      { name: 'status', placeholder: '状态', options: STATUS_OPTIONS },
    ],
    columns: [
      { title: '诊断编码', dataIndex: 'diagnosisCode', width: 120 },
      { title: '诊断名称', key: 'name', width: 250, ellipsis: true },
      { title: 'ICD-10编码', dataIndex: 'icd10Code', width: 100 },
      { title: '章节', dataIndex: 'chapterName', width: 150, ellipsis: true },
      { title: '状态', key: 'status', width: 80 },
      { title: '操作', key: 'actions', width: 100, fixed: 'right' },
    ],
    fields: [
      { name: 'diagnosisCode', label: '诊断编码', type: 'input', placeholder: '诊断编码', required: true, requiredMessage: '请输入诊断编码', disabledOnEdit: true },
      { name: 'name', label: '诊断名称', type: 'input', placeholder: '诊断名称', required: true, requiredMessage: '请输入诊断名称' },
      { name: 'icd10Code', label: 'ICD-10编码', type: 'input', placeholder: 'ICD-10编码' },
      { name: 'icd10Name', label: 'ICD-10名称', type: 'input', placeholder: 'ICD-10标准名称' },
      { name: 'chapterCode', label: '章节编码', type: 'input', placeholder: '章节编码' },
      { name: 'chapterName', label: '章节名称', type: 'input', placeholder: '章节名称' },
      statusField(false),
      remarkField(),
    ],
    detailFields: [
      { label: '诊断编码', name: 'diagnosisCode' },
      { label: '诊断名称', name: 'name' },
      { label: 'ICD-10编码', name: 'icd10Code' },
      { label: 'ICD-10名称', name: 'icd10Name' },
      { label: '章节', name: 'chapterName' },
      { label: '状态', name: 'status', render: 'tag' },
      { label: '备注', name: 'remark', span: 2 },
    ],
  },

  lab: {
    key: 'lab',
    title: '检验项目字典',
    newLabel: '新增检验项目',
    editLabel: '编辑检验项目',
    detailTitle: '检验项目详情',
    modalWidth: 800,
    scrollX: 1200,
    keywordPlaceholder: '搜索编码/名称/LOINC/拼音',
    filters: [
      { name: 'labCategory', placeholder: '检验分类', optionsKey: 'labCategories' },
      { name: 'status', placeholder: '状态', options: STATUS_OPTIONS },
    ],
    columns: [
      { title: '检验编码', dataIndex: 'labCode', width: 120 },
      { title: '项目名称', key: 'name', width: 200, ellipsis: true },
      { title: 'LOINC编码', dataIndex: 'loincCode', width: 100 },
      { title: '标本类型', dataIndex: 'specimenType', width: 100 },
      { title: '结果类型', dataIndex: 'resultType', width: 80 },
      { title: '单位', dataIndex: 'unit', width: 80 },
      { title: '参考范围', key: 'refRange', width: 150 },
      { title: '状态', key: 'status', width: 80 },
      { title: '操作', key: 'actions', width: 100, fixed: 'right' },
    ],
    fields: [
      { name: 'labCode', label: '检验编码', type: 'input', placeholder: '检验编码', required: true, requiredMessage: '请输入检验编码', disabledOnEdit: true },
      { name: 'name', label: '项目名称', type: 'input', placeholder: '项目名称', required: true, requiredMessage: '请输入项目名称' },
      { name: 'loincCode', label: 'LOINC编码', type: 'input', placeholder: 'LOINC编码' },
      { name: 'labCategory', label: '检验分类', type: 'select', optionsKey: 'labCategories', placeholder: '检验分类' },
      { name: 'specimenType', label: '标本类型', type: 'input', placeholder: '标本类型' },
      { name: 'method', label: '检验方法', type: 'input', placeholder: '检验方法' },
      { name: 'resultType', label: '结果类型', type: 'select', options: RESULT_TYPE_OPTIONS, placeholder: '结果类型' },
      { name: 'unit', label: '结果单位', type: 'input', placeholder: '结果单位' },
      { name: 'refRangeLow', label: '参考下限', type: 'number', placeholder: '参考下限' },
      { name: 'refRangeHigh', label: '参考上限', type: 'number', placeholder: '参考上限' },
      statusField(false),
      remarkField(),
    ],
    detailFields: [
      { label: '检验编码', name: 'labCode' },
      { label: '项目名称', name: 'name' },
      { label: 'LOINC编码', name: 'loincCode' },
      { label: '标本类型', name: 'specimenType' },
      { label: '检验分类', name: 'labCategory' },
      { label: '检验方法', name: 'method' },
      { label: '结果类型', name: 'resultType' },
      { label: '单位', name: 'unit' },
      { label: '参考范围', name: 'refRange', render: 'refRange' },
      { label: '状态', name: 'status', render: 'tag' },
      { label: '备注', name: 'remark', span: 2 },
    ],
  },

  exam: {
    key: 'exam',
    title: '检查项目字典',
    newLabel: '新增检查项目',
    editLabel: '编辑检查项目',
    detailTitle: '检查项目详情',
    modalWidth: 800,
    scrollX: 1200,
    keywordPlaceholder: '搜索编码/名称/拼音',
    filters: [
      { name: 'examType', placeholder: '检查类型', optionsKey: 'examTypes' },
      { name: 'status', placeholder: '状态', options: STATUS_OPTIONS },
    ],
    columns: [
      { title: '检查编码', dataIndex: 'examCode', width: 120 },
      { title: '项目名称', key: 'name', width: 200, ellipsis: true },
      { title: '检查类型', dataIndex: 'examType', width: 100 },
      { title: '检查部位', dataIndex: 'bodySite', width: 120 },
      { title: '检查方法', dataIndex: 'method', width: 120 },
      { title: '造影类型', dataIndex: 'contrastType', width: 100 },
      { title: '设备类型', dataIndex: 'equipmentType', width: 120 },
      { title: '状态', key: 'status', width: 80 },
      { title: '操作', key: 'actions', width: 100, fixed: 'right' },
    ],
    fields: [
      { name: 'examCode', label: '检查编码', type: 'input', placeholder: '检查编码', required: true, requiredMessage: '请输入检查编码', disabledOnEdit: true },
      { name: 'name', label: '项目名称', type: 'input', placeholder: '项目名称', required: true, requiredMessage: '请输入项目名称' },
      { name: 'examType', label: '检查类型', type: 'select', options: EXAM_TYPE_OPTIONS, required: true, requiredMessage: '请选择检查类型', default: 'CT' },
      { name: 'bodySite', label: '检查部位', type: 'input', placeholder: '检查部位' },
      { name: 'method', label: '检查方法', type: 'input', placeholder: '检查方法' },
      { name: 'contrastType', label: '造影类型', type: 'select', options: CONTRAST_TYPE_OPTIONS, placeholder: '造影类型' },
      { name: 'equipmentType', label: '设备类型', type: 'input', placeholder: '设备类型' },
      statusField(false),
      remarkField(),
    ],
    detailFields: [
      { label: '检查编码', name: 'examCode' },
      { label: '项目名称', name: 'name' },
      { label: '检查类型', name: 'examType' },
      { label: '检查部位', name: 'bodySite' },
      { label: '检查方法', name: 'method' },
      { label: '造影类型', name: 'contrastType' },
      { label: '设备类型', name: 'equipmentType' },
      { label: '状态', name: 'status', render: 'tag' },
      { label: '备注', name: 'remark', span: 2 },
    ],
  },

  fee: {
    key: 'fee',
    title: '收费项目字典',
    newLabel: '新增收费项目',
    editLabel: '编辑收费项目',
    detailTitle: '收费项目详情',
    modalWidth: 800,
    scrollX: 1200,
    keywordPlaceholder: '搜索编码/名称/拼音',
    filters: [
      { name: 'feeCategory', placeholder: '收费类别', optionsKey: 'feeCategories' },
      { name: 'feeType', placeholder: '费别', options: FEE_TYPE_OPTIONS },
      { name: 'status', placeholder: '状态', options: STATUS_OPTIONS },
    ],
    columns: [
      { title: '项目编码', dataIndex: 'feeCode', width: 120 },
      { title: '项目名称', key: 'name', width: 200, ellipsis: true },
      { title: '收费类别', dataIndex: 'feeCategory', width: 100 },
      { title: '费别', key: 'feeType', width: 80 },
      { title: '单价', key: 'price', width: 100 },
      { title: '单位', dataIndex: 'unit', width: 80 },
      { title: '执行科室', dataIndex: 'executingDeptName', width: 120 },
      { title: '状态', key: 'status', width: 80 },
      { title: '操作', key: 'actions', width: 100, fixed: 'right' },
    ],
    fields: [
      { name: 'feeCode', label: '项目编码', type: 'input', placeholder: '项目编码', required: true, requiredMessage: '请输入项目编码', disabledOnEdit: true },
      { name: 'name', label: '项目名称', type: 'input', placeholder: '项目名称', required: true, requiredMessage: '请输入项目名称' },
      { name: 'feeCategory', label: '收费类别', type: 'select', options: FEE_CATEGORY_OPTIONS, required: true, requiredMessage: '请选择收费类别', default: '诊疗' },
      { name: 'feeType', label: '费别', type: 'select', options: FEE_TYPE_OPTIONS, placeholder: '费别' },
      { name: 'price', label: '单价', type: 'number', placeholder: '单价' },
      { name: 'unit', label: '单位', type: 'input', placeholder: '单位' },
      statusField(false),
      remarkField(),
    ],
    detailFields: [
      { label: '项目编码', name: 'feeCode' },
      { label: '项目名称', name: 'name' },
      { label: '收费类别', name: 'feeCategory' },
      { label: '费别', name: 'feeType', render: 'tag' },
      { label: '单价', name: 'price', render: 'price' },
      { label: '单位', name: 'unit' },
      { label: '执行科室', name: 'executingDeptName' },
      { label: '状态', name: 'status', render: 'tag' },
      { label: '备注', name: 'remark', span: 2 },
    ],
  },
}
