import type { RouteRecordRaw } from 'vue-router'

// 菜单层级说明（2026-09-11 专病库聚焦重组）：
// 「专病库」为一级菜单，聚合专病管理/知识库/患者/随访/量表/科研；
// 子路由使用绝对 path（以 / 开头），保持既有 URL 与页面跳转引用零变更。
export const asyncRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        meta: { title: '工作台', icon: 'DashboardOutlined', sort: 1 },
        redirect: '/dashboard/workspace',
        children: [
          { path: 'workspace', name: 'DashboardWorkspace', meta: { title: '个人工作台' }, component: () => import('@/views/dashboard/workspace/WorkspaceView.vue') },
          { path: 'overview', name: 'DashboardOverview', meta: { title: '系统总览' }, component: () => import('@/views/dashboard/Overview.vue') },
          { path: 'data', name: 'DataDashboard', meta: { title: '数据看板' }, component: () => import('@/views/dashboard/DataDashboard.vue') },
        ],
      },
      {
        path: 'disease',
        name: 'Disease',
        meta: { title: '专病库', icon: 'MedicineBoxOutlined', sort: 2 },
        redirect: '/data/cdr/disease',
        children: [
          { path: '/data/cdr/disease', name: 'DiseaseList', meta: { title: '专病管理' }, component: () => import('@/views/data-cdr/DiseaseList.vue') },
          { path: '/data/cdr/disease/:id', name: 'DiseaseDetail', meta: { title: '专病详情', hidden: true }, component: () => import('@/views/data-cdr/DiseaseDetail.vue') },
          { path: '/data/cdr/disease-kb', name: 'DiseaseKnowledgeList', meta: { title: '专病知识库' }, component: () => import('@/views/data-cdr/DiseaseKnowledgeList.vue') },
          { path: '/data/cdr/disease-kb/:id', name: 'DiseaseKnowledgeDetail', meta: { title: '专病知识库详情', hidden: true }, component: () => import('@/views/data-cdr/DiseaseKnowledgeDetail.vue') },
          { path: '/data/cdr/patients', name: 'PatientList', meta: { title: '患者管理' }, component: () => import('@/views/data-cdr/PatientList.vue') },
          { path: '/data/cdr/patients/:id', name: 'PatientDetail', meta: { title: '患者详情', hidden: true }, component: () => import('@/views/data-cdr/PatientDetail.vue') },
          { path: '/data/cdr/patient/:patientId', name: 'PatientEncounter360', meta: { title: '患者就诊360视图', hidden: true }, component: () => import('@/views/data-cdr/patient/Patient360Overview.vue') },
          { path: '/data/cdr/patients/:id/encounters/:encounterId', name: 'EncounterDetail', meta: { title: '就诊详情', hidden: true }, component: () => import('@/views/data-cdr/EncounterDetail.vue') },
          { path: '/followup/workbench', name: 'FollowupWorkbench', meta: { title: '随访工作台', permission: 'disease:followup:work' }, component: () => import('@/views/followup/FollowupWorkbench.vue') },
          { path: '/data/cdr/followup/:fid', name: 'FollowupArchiveDetail', meta: { title: '随访档案详情', hidden: true, permission: 'disease:followup:work' }, component: () => import('@/views/data-cdr/followup/FollowupArchiveDetail.vue') },
          { path: '/data/cdr/scales', name: 'ScaleManagement', meta: { title: '量表管理', permission: 'disease:followup:work' }, component: () => import('@/views/data-cdr/followup/ScaleManagement.vue') },
          { path: '/data/cdr/scales/design', name: 'ScaleDesigner', meta: { title: '量表设计器', hidden: true, permission: 'disease:scale:manage' }, component: () => import('@/views/data-cdr/followup/ScaleDesigner.vue') },
          { path: '/data/rdr/projects', name: 'ProjectList', meta: { title: '研究项目' }, component: () => import('@/views/data-rdr/ProjectList.vue') },
          { path: '/data/rdr/datasets', name: 'DatasetList', meta: { title: '数据集' }, component: () => import('@/views/data-rdr/DatasetList.vue') },
        ],
      },
      {
        path: 'data',
        name: 'Data',
        meta: { title: '数据管理', icon: 'DatabaseOutlined', sort: 3, permission: 'data' },
        redirect: '/data/cdr/search',
        children: [
          { path: 'cdr/search', name: 'ClinicalSearch', meta: { title: '临床检索' }, component: () => import('@/views/data-cdr/ClinicalSearch.vue') },
          { path: 'cdr/quality-rules', name: 'QualityRuleList', meta: { title: '质量规则' }, component: () => import('@/views/data-cdr/QualityRuleList.vue') },
          { path: 'cdr/quality-results', name: 'QualityResultList', meta: { title: '质量检测' }, component: () => import('@/views/data-cdr/QualityResultList.vue') },
        ],
      },
      {
        path: 'etl',
        name: 'Etl',
        meta: { title: '数据转换', icon: 'SwapOutlined', sort: 4, permission: 'data' },
        redirect: '/etl/pipelines',
        children: [
          { path: 'datasources', name: 'DataSourceList', meta: { title: '数据源管理' }, component: () => import('@/views/data-cdr/DataSourceList.vue') },
          { path: 'datasources/:id', name: 'DataSourceDetail', meta: { title: '数据源详情', hidden: true }, component: () => import('@/views/data-cdr/DataSourceDetail.vue') },
          { path: 'pipelines', name: 'EtlPipelineList', meta: { title: '管道管理' }, component: () => import('@/views/data-etl/EtlPipelineList.vue') },
          { path: 'pipelines/:id', name: 'EtlPipelineConfig', meta: { title: '管道配置', hidden: true }, component: () => import('@/views/data-etl/EtlPipelineConfig.vue') },
          { path: 'executions', name: 'EtlExecutionList', meta: { title: '执行监控' }, component: () => import('@/views/data-etl/EtlExecutionList.vue') },
          { path: 'sync', name: 'SyncTaskList', meta: { title: '数据同步' }, component: () => import('@/views/data-cdr/SyncTaskList.vue') },
          { path: 'schedule', name: 'ScheduleTaskList', meta: { title: '定时任务' }, component: () => import('@/views/schedule/TaskList.vue') },
        ],
      },
      {
        path: 'system',
        name: 'System',
        meta: { title: '系统设置', icon: 'SettingOutlined', sort: 5 },
        redirect: '/system/masterdata',
        children: [
          {
            path: 'masterdata',
            name: 'SystemMasterData',
            meta: { title: '主数据管理', permission: 'masterdata:read' },
            redirect: '/system/masterdata',
            children: [
              { path: '', name: 'MasterDataManagement', meta: { title: '主数据管理', permission: 'masterdata:read' }, component: () => import('@/views/masterdata/MasterDataManagement.vue') },
              { path: 'concept-domains', name: 'ConceptDomains', meta: { title: '概念域管理', hidden: true, permission: 'masterdata:read' }, component: () => import('@/views/masterdata/ConceptDomainList.vue') },
              { path: 'value-domains', name: 'ValueDomains', meta: { title: '值域管理', hidden: true, permission: 'masterdata:read' }, component: () => import('@/views/masterdata/ValueDomainList.vue') },
              { path: 'data-element-concepts', name: 'DataElementConcepts', meta: { title: '数据元概念', hidden: true, permission: 'masterdata:read' }, component: () => import('@/views/masterdata/DataElementConceptList.vue') },
              { path: 'data-elements', name: 'DataElementList', meta: { title: '数据元管理', hidden: true, permission: 'masterdata:read' }, component: () => import('@/views/masterdata/DataElementList.vue') },
              { path: 'code-systems', name: 'CodeSystems', meta: { title: '编码体系', hidden: true, permission: 'masterdata:read' }, component: () => import('@/views/masterdata/CodeSystems.vue') },
              { path: 'mappings', name: 'MappingManager', meta: { title: '编码映射', hidden: true, permission: 'masterdata:read' }, component: () => import('@/views/masterdata/MappingManager.vue') },
              { path: 'clinical-rules', name: 'ClinicalRules', meta: { title: '临床规则', hidden: true, permission: 'masterdata:read' }, component: () => import('@/views/masterdata/ClinicalRules.vue') },
              { path: 'domains', name: 'DomainManager', meta: { title: '领域管理', hidden: true, permission: 'masterdata:read' }, component: () => import('@/views/masterdata/DomainManager.vue') },
              { path: 'knowledge', name: 'KnowledgeList', meta: { title: '知识体系', hidden: true, permission: 'masterdata:read' }, component: () => import('@/views/masterdata/KnowledgeList.vue') },
            ],
          },
          {
            path: 'dict-manage',
            name: 'DictManage',
            meta: { title: '数据字典', permission: 'system:dict:manage' },
            component: () => import('@/views/data-cdr/DictManage.vue'),
          },
          {
            path: 'dictionaries',
            name: 'MedicalDictionaries',
            meta: { title: '常用字典', permission: 'masterdata:read' },
            redirect: '/system/dictionaries/drugs',
            children: [
              { path: 'drugs', name: 'DrugList', meta: { title: '药品字典', permission: 'masterdata:read' }, component: () => import('@/views/masterdata/dictionary/DrugList.vue') },
              { path: 'diagnoses', name: 'DiagnosisList', meta: { title: '诊断字典', permission: 'masterdata:read' }, component: () => import('@/views/masterdata/dictionary/DiagnosisList.vue') },
              { path: 'fee-items', name: 'FeeItemList', meta: { title: '收费项目', permission: 'masterdata:read' }, component: () => import('@/views/masterdata/dictionary/FeeItemList.vue') },
              { path: 'lab-items', name: 'LabItemList', meta: { title: '检验项目', permission: 'masterdata:read' }, component: () => import('@/views/masterdata/dictionary/LabItemList.vue') },
              { path: 'exam-items', name: 'ExamItemList', meta: { title: '检查项目', permission: 'masterdata:read' }, component: () => import('@/views/masterdata/dictionary/ExamItemList.vue') },
            ],
          },
          {
            path: 'sys',
            name: 'SystemAdmin',
            meta: { title: '系统管理' },
            redirect: '/system/users',
            children: [
              { path: 'users', name: 'UserList', meta: { title: '用户管理' }, component: () => import('@/views/system/UserList.vue') },
              { path: 'users/:id', name: 'UserDetail', meta: { title: '用户详情', hidden: true }, component: () => import('@/views/system/UserDetail.vue') },
              { path: 'roles', name: 'RoleList', meta: { title: '角色管理' }, component: () => import('@/views/system/RoleList.vue') },
              { path: 'roles/:id', name: 'RoleDetail', meta: { title: '角色详情', hidden: true }, component: () => import('@/views/system/RoleDetail.vue') },
              { path: 'permissions', name: 'PermissionManagement', meta: { title: '权限管理' }, component: () => import('@/views/system/PermissionManagement.vue') },
              { path: 'institutions', name: 'InstitutionList', meta: { title: '机构管理' }, component: () => import('@/views/masterdata/InstitutionList.vue') },
              { path: 'config', name: 'SystemConfig', meta: { title: '系统参数' }, component: () => import('@/views/system/SystemConfig.vue') },
              { path: 'desensitize', name: 'DesensitizeRule', meta: { title: '脱敏规则' }, component: () => import('@/views/data-cdr/DesensitizeRule.vue') },
            ],
          },
        ],
      },
      {
        path: 'alert',
        name: 'Alert',
        meta: { title: '告警中心', icon: 'AlertOutlined', sort: 8 },
        children: [
          { path: 'active', name: 'AlertList', meta: { title: '活跃告警' }, component: () => import('@/views/alert/AlertList.vue') },
          { path: 'rules', name: 'AlertRuleList', meta: { title: '告警规则' }, component: () => import('@/views/alert/AlertRuleList.vue') },
          { path: 'detail/:id', name: 'AlertDetail', meta: { title: '告警详情', hidden: true }, component: () => import('@/views/alert/AlertDetail.vue') },
        ],
      },
      {
        path: 'audit',
        name: 'Audit',
        meta: { title: '审计日志', icon: 'FileSearchOutlined', sort: 9, permission: 'audit' },
        children: [
          { path: 'operations', name: 'OperationLog', meta: { title: '操作审计' }, component: () => import('@/views/audit/OperationLog.vue') },
          { path: 'data-access', name: 'DataAccessLog', meta: { title: '数据访问' }, component: () => import('@/views/audit/DataAccessLog.vue') },
          { path: 'system-events', name: 'SystemEventLog', meta: { title: '系统事件' }, component: () => import('@/views/audit/SystemEventLog.vue') },
          { path: 'compliance', name: 'ComplianceReport', meta: { title: '合规报表' }, component: () => import('@/views/audit/ComplianceReport.vue') },
        ],
      },
      {
        path: 'message',
        name: 'Message',
        meta: { title: '消息中心', icon: 'BellOutlined', sort: 10 },
        children: [
          { path: 'list', name: 'MessageList', meta: { title: '我的消息' }, component: () => import('@/views/message/MessageList.vue') },
          { path: 'detail/:id', name: 'MessageDetail', meta: { title: '消息详情', hidden: true }, component: () => import('@/views/message/MessageDetail.vue') },
          { path: 'settings', name: 'NotificationSettings', meta: { title: '通知设置' }, component: () => import('@/views/message/NotificationSettings.vue') },
          { path: 'templates', name: 'TemplateManagement', meta: { title: '模板管理' }, component: () => import('@/views/message/TemplateManagement.vue') },
        ],
      },
    ],
  },
]
