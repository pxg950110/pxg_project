/**
 * 首页工作台 v2 原型 mock 数据
 * 结构对齐 PRD 中 GET /api/v1/workspace/dashboard 的 v2 响应扩展：
 * welcome + cards + todos(FOLLOWUP 扩展) + todoStats + notifications + cohortDigest + quickActions
 * 实现后整目录删除，勿在正式代码中引用。
 */

export type RoleGroup = 'CLINICAL' | 'RESEARCH' | 'DATA' | 'GOVERNANCE'

export interface MetricCardVO {
  key: string
  label: string
  value: number
  suffix: string
  icon: string
  route?: string
  tone?: 'primary' | 'danger' | 'success' | 'warning' | 'default'
}

export interface TodoItemVO {
  id: number
  taskType: 'FOLLOWUP' | 'APPROVAL' | 'LABELING' | 'DATA_QUERY' | 'OTHER'
  title: string
  priority: 'HIGH' | 'MEDIUM' | 'LOW'
  dueDate: string
  /** 随访待办扩展字段 */
  patientName?: string
  stageName?: string
  scales?: string[]
  overdueDays?: number
}

export interface NotificationVO {
  id: number
  type: 'SYSTEM' | 'ALERT' | 'APPROVAL' | 'COHORT'
  title: string
  isRead: boolean
  createdAt: string
}

export interface CohortDigestVO {
  type: 'SYNC_DONE' | 'AI_SUGGEST' | 'KB_UPDATE'
  title: string
  time: string
}

export interface QuickActionVO {
  key: string
  label: string
  icon: string
  route: string
}

export interface WorkspaceV2Mock {
  roleKey: string
  roleName: string
  userName: string
  welcome: { greeting: string; date: string; orgName: string }
  cards: MetricCardVO[]
  todoStats: { today: number; overdue: number; total: number }
  todos: TodoItemVO[]
  notifications: NotificationVO[]
  cohortDigest?: CohortDigestVO[]
  quickActions: QuickActionVO[]
}

/** 今日日期锚点：与后端 welcome.date 同源的展示口径 */
const TODAY = '2026年09月09日 星期三'

export const workspaceMock: Record<RoleGroup, WorkspaceV2Mock> = {
  CLINICAL: {
    roleKey: 'doctor',
    roleName: '临床医生',
    userName: '张伟',
    welcome: { greeting: '下午好', date: TODAY, orgName: '市一医院 · 耳鼻喉科' },
    cards: [
      { key: 'followup_today', label: '今日随访到期', value: 3, suffix: '项', icon: 'schedule', route: '/proto/crs/workbench', tone: 'primary' },
      { key: 'followup_overdue', label: '已超期随访', value: 2, suffix: '项', icon: 'alert', route: '/proto/crs/workbench', tone: 'danger' },
      { key: 'my_patients', label: '我的患者（本科室）', value: 86, suffix: '人', icon: 'team', route: '/data/cdr/patients' },
      { key: 'followup_week_done', label: '本周完成随访', value: 9, suffix: '例', icon: 'check-circle', route: '/proto/crs/workbench', tone: 'success' },
    ],
    todoStats: { today: 3, overdue: 2, total: 7 },
    todos: [
      { id: 1, taskType: 'FOLLOWUP', title: '王建国 · 术后3月随访', priority: 'HIGH', dueDate: '2026-09-09', patientName: '王建国', stageName: '术后3月', scales: ['SNOT-22', 'VAS'] },
      { id: 2, taskType: 'FOLLOWUP', title: '陈志强 · 基线评估', priority: 'MEDIUM', dueDate: '2026-09-09', patientName: '陈志强', stageName: '基线', scales: ['SNOT-22', 'Lund-Kennedy'] },
      { id: 3, taskType: 'FOLLOWUP', title: '李淑芬 · 术后12月随访', priority: 'HIGH', dueDate: '2026-09-08', overdueDays: 1, patientName: '李淑芬', stageName: '术后12月', scales: ['SNOT-22'] },
      { id: 4, taskType: 'FOLLOWUP', title: '周玉兰 · 术后1月随访', priority: 'HIGH', dueDate: '2026-09-02', overdueDays: 7, patientName: '周玉兰', stageName: '术后1月', scales: ['SNOT-22', 'VAS'] },
      { id: 5, taskType: 'APPROVAL', title: '数据导出申请 #2043 待审批', priority: 'MEDIUM', dueDate: '2026-09-11' },
      { id: 6, taskType: 'OTHER', title: '《CRS 随访方案》v2 变更确认', priority: 'LOW', dueDate: '2026-09-12' },
      { id: 7, taskType: 'APPROVAL', title: '新患者入组审核：钱多多', priority: 'MEDIUM', dueDate: '2026-09-10' },
    ],
    notifications: [
      { id: 1, type: 'COHORT', title: '队列同步完成：慢性鼻窦炎队列新增 3 名患者', isRead: false, createdAt: '09:12' },
      { id: 2, type: 'SYSTEM', title: '知识库条目《EPOS 2020 解读》已发布', isRead: false, createdAt: '08:47' },
      { id: 3, type: 'ALERT', title: '患者李淑芬随访已超期 1 天，请尽快处理', isRead: true, createdAt: '昨天 17:30' },
    ],
    cohortDigest: [
      { type: 'SYNC_DONE', title: '慢性鼻窦炎队列昨日同步新增 3 人（自动入组）', time: '06:00' },
      { type: 'AI_SUGGEST', title: 'AI 入组建议 5 条待审核（预估符合率 82%）', time: '昨天' },
      { type: 'KB_UPDATE', title: '《CRS 诊疗指南》知识条目更新至 v3', time: '09-08' },
    ],
    quickActions: [
      { key: 'patient_search', label: '患者检索', icon: 'search', route: '/data/cdr/patients' },
      { key: 'clinical_search', label: '临床检索', icon: 'profile', route: '/data/cdr/search' },
      { key: 'disease_kb', label: '专病知识库', icon: 'book', route: '/data/cdr/disease-kb' },
      { key: 'followup_workbench', label: '随访工作台', icon: 'medicine-box', route: '/proto/crs/workbench' },
    ],
  },

  RESEARCH: {
    roleKey: 'researcher_pi',
    roleName: '科研负责人',
    userName: '李瑾',
    welcome: { greeting: '下午好', date: TODAY, orgName: '市一医院 · 科研处' },
    cards: [
      { key: 'cohort_active', label: '在管专病队列', value: 4, suffix: '个', icon: 'database', route: '/data/cdr/disease' },
      { key: 'cohort_patients', label: '队列患者总数', value: 1286, suffix: '人', icon: 'team', route: '/data/cdr/disease' },
      { key: 'pending_exports', label: '待审批导出', value: 2, suffix: '项', icon: 'audit', tone: 'warning' },
      { key: 'datasets', label: '研究数据集', value: 12, suffix: '个', icon: 'appstore', route: '/data/rdr/datasets' },
    ],
    todoStats: { today: 1, overdue: 0, total: 4 },
    todos: [
      { id: 11, taskType: 'APPROVAL', title: '数据集 DS-2026-031 导出审批', priority: 'HIGH', dueDate: '2026-09-10' },
      { id: 12, taskType: 'APPROVAL', title: '数据集 DS-2026-029 导出审批', priority: 'MEDIUM', dueDate: '2026-09-12' },
      { id: 13, taskType: 'DATA_QUERY', title: '病例入组核查：鼻窦炎多中心研究新增 12 例', priority: 'MEDIUM', dueDate: '2026-09-09' },
      { id: 14, taskType: 'LABELING', title: '文本标注任务 #77 分配确认', priority: 'LOW', dueDate: '2026-09-14' },
    ],
    notifications: [
      { id: 1, type: 'SYSTEM', title: '数据集「鼻窦炎多中心研究 v3」已生成', isRead: false, createdAt: '10:02' },
      { id: 2, type: 'COHORT', title: '队列匹配预览完成：符合条件 38 人', isRead: false, createdAt: '09:40' },
      { id: 3, type: 'APPROVAL', title: '研究项目年审通过', isRead: true, createdAt: '09-08' },
    ],
    cohortDigest: [
      { type: 'AI_SUGGEST', title: '「高血压队列」AI 建议 12 名候选患者待确认', time: '10:15' },
      { type: 'SYNC_DONE', title: '「鼻窦炎队列」本月增量同步 41 人', time: '09-08' },
    ],
    quickActions: [
      { key: 'new_cohort', label: '专病队列', icon: 'database', route: '/data/cdr/disease' },
      { key: 'clinical_search', label: '临床检索', icon: 'profile', route: '/data/cdr/search' },
      { key: 'disease_kb', label: '知识库问答', icon: 'book', route: '/data/cdr/disease-kb' },
      { key: 'rdr_projects', label: '研究项目', icon: 'project', route: '/data/rdr/projects' },
    ],
  },

  DATA: {
    roleKey: 'data_admin',
    roleName: '数据管理员',
    userName: '王浩',
    welcome: { greeting: '下午好', date: TODAY, orgName: '市一医院 · 信息科' },
    cards: [
      { key: 'model_count', label: '模型总数', value: 28, suffix: '个', icon: 'experiment', route: '/model/list' },
      { key: 'active_deployments', label: '活跃部署', value: 8, suffix: '个', icon: 'rocket', route: '/model/deployments' },
      { key: 'daily_inferences', label: '今日推理', value: 12456, suffix: '次', icon: 'thunderbolt', route: '/model/inference-logs' },
      { key: 'quality_pending', label: '待处理质控', value: 5, suffix: '项', icon: 'shield', route: '/data/cdr/quality-results', tone: 'warning' },
    ],
    todoStats: { today: 2, overdue: 1, total: 5 },
    todos: [
      { id: 21, taskType: 'APPROVAL', title: '模型 v3.0 上线审批', priority: 'HIGH', dueDate: '2026-09-09' },
      { id: 22, taskType: 'LABELING', title: '影像标注任务 #77（320 张）', priority: 'MEDIUM', dueDate: '2026-09-09' },
      { id: 23, taskType: 'OTHER', title: 'ETL 管道 cdr_daily 告警处理', priority: 'HIGH', dueDate: '2026-09-08' },
      { id: 24, taskType: 'APPROVAL', title: '新数据源接入审批：体检系统', priority: 'MEDIUM', dueDate: '2026-09-11' },
      { id: 25, taskType: 'OTHER', title: '术语映射 DRG→ICD-10 复核', priority: 'LOW', dueDate: '2026-09-15' },
    ],
    notifications: [
      { id: 1, type: 'SYSTEM', title: 'ETL 管道 cdr_daily 昨夜执行成功（12.4 万行）', isRead: false, createdAt: '06:31' },
      { id: 2, type: 'ALERT', title: '质检规则「血压值域」昨日命中 23 条异常', isRead: false, createdAt: '08:00' },
      { id: 3, type: 'SYSTEM', title: '数据源「体检系统」待验证连接', isRead: true, createdAt: '昨天' },
    ],
    quickActions: [
      { key: 'etl_pipelines', label: 'ETL 管道', icon: 'sync', route: '/etl/pipelines' },
      { key: 'datasources', label: '数据源', icon: 'database', route: '/etl/datasources' },
      { key: 'quality', label: '质量检测', icon: 'shield', route: '/data/cdr/quality-results' },
      { key: 'new_model', label: '新建模型', icon: 'plus', route: '/model/list' },
      { key: 'new_eval', label: '新建评估', icon: 'experiment', route: '/model/evaluations' },
    ],
  },

  GOVERNANCE: {
    roleKey: 'auditor',
    roleName: '审计员',
    userName: '赵敏',
    welcome: { greeting: '下午好', date: TODAY, orgName: '市一医院 · 审计办' },
    cards: [
      { key: 'user_total', label: '平台用户', value: 156, suffix: '人', icon: 'user', route: '/system/users' },
      { key: 'audit_events_today', label: '今日审计事件', value: 3204, suffix: '条', icon: 'file-search', route: '/audit/operations' },
      { key: 'permission_denied_today', label: '今日权限拒绝', value: 12, suffix: '次', icon: 'alert', tone: 'danger', route: '/audit/system-events' },
      { key: 'active_alerts', label: '活跃告警', value: 3, suffix: '条', icon: 'alert', tone: 'warning', route: '/alert/active' },
    ],
    todoStats: { today: 1, overdue: 0, total: 3 },
    todos: [
      { id: 31, taskType: 'APPROVAL', title: '账号开通审批：耳鼻喉科 ×2', priority: 'MEDIUM', dueDate: '2026-09-09' },
      { id: 32, taskType: 'OTHER', title: '8 月合规报表待生成确认', priority: 'MEDIUM', dueDate: '2026-09-10' },
      { id: 33, taskType: 'OTHER', title: '脱敏规则季度复核', priority: 'LOW', dueDate: '2026-09-20' },
    ],
    notifications: [
      { id: 1, type: 'SYSTEM', title: '权限拒绝事件周报已生成（共 41 次）', isRead: false, createdAt: '09-08 18:00' },
      { id: 2, type: 'SYSTEM', title: '角色「科研成员」权限矩阵发生变更', isRead: false, createdAt: '09-07' },
      { id: 3, type: 'ALERT', title: '3 条活跃告警超过 24 小时未处理', isRead: true, createdAt: '09-07' },
    ],
    quickActions: [
      { key: 'overview', label: '系统总览', icon: 'dashboard', route: '/dashboard/overview' },
      { key: 'audit_ops', label: '操作审计', icon: 'file-search', route: '/audit/operations' },
      { key: 'audit_data', label: '数据访问审计', icon: 'search', route: '/audit/data-access' },
      { key: 'users', label: '用户管理', icon: 'user', route: '/system/users' },
      { key: 'alerts', label: '告警中心', icon: 'alert', route: '/alert/active' },
    ],
  },
}

export const roleGroupOptions: { value: RoleGroup; label: string }[] = [
  { value: 'CLINICAL', label: '临床 · 医生' },
  { value: 'RESEARCH', label: '科研 · PI' },
  { value: 'DATA', label: '数据 · 管理员' },
  { value: 'GOVERNANCE', label: '治理 · 审计员' },
]
