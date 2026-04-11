# MAIDC 全栈对齐设计：前端 + 数据库 + Docker

> 日期：2026-04-11
> 状态：已确认
> 执行方案：方案 C — 先核心后边缘

## 背景

MAIDC（医疗AI数据中心）平台当前存在三个方面的差距：
1. **前端 vs 设计稿**：57 个 Vue 页面与 .pen 设计稿（50+ 页面 + 20 弹窗）存在视觉和功能差异
2. **数据库 Entity 不全**：68 张表中约 38 张缺少 JPA Entity + CRUD
3. **Docker 部署**：基础设施已就绪但未经全栈验证

## 目标

- 前端与设计稿**视觉精确对齐**（像素级）
- 数据库所有 68 张表都有完整的 Entity + Repository + Service + Controller
- Docker 全栈部署可通过 `docker-compose up` 一键启动

## 拆分策略

采用方案 C（先核心后边缘），分为 7 个子项目，每个子项目独立执行 spec → plan → implementation 周期。

---

## 子项目 1：Dashboard + 登录页

### 前端页面
| 设计稿 | 前端文件 | 状态 |
|--------|---------|------|
| 01-登录页 | LoginPage.vue | 需对比 |
| 02-Dashboard工作台 | Overview.vue | 需对比 |
| — | DataDashboard.vue | 需对比 |
| — | ModelDashboard.vue | 需对比 |

### 数据库
无新增。system schema 已有 UserEntity、RoleEntity 等。

### 交付物
- 登录页和仪表盘视觉对齐设计稿
- 仪表盘数据接口联调

---

## 子项目 2：模型管理（全链路）

### 前端页面
| 设计稿 | 前端文件 |
|--------|---------|
| 03-模型管理-模型列表 | ModelList.vue |
| 04-模型管理-模型详情 | ModelDetail.vue |
| 05-模型管理-部署监控 | DeploymentList.vue |
| 05a-模型管理-部署详情 | DeploymentDetail.vue |
| 06-模型管理-版本管理 | VersionList.vue |
| 06a-模型管理-版本对比 | (需确认) |
| 07-模型管理-模型评估 | EvalList.vue |
| 07a-模型管理-评估详情 | EvalDetail.vue |
| 08-模型管理-审批流程 | ApprovalList.vue |
| 08a-模型管理-审批详情 | (需确认) |
| 09-模型管理-流量路由 | RouteConfig.vue |
| 推理日志 | InferenceLog.vue |
| Modal-注册模型 | ModelList.vue 内 |
| Modal-编辑模型 | ModelDetail.vue 内 |
| Modal-上传版本 | VersionList.vue 内 |
| Modal-新建评估 | EvalList.vue 内 |
| Modal-审批操作 | ApprovalList.vue 内 |
| Modal-新建部署 | DeploymentList.vue 内 |
| Modal-创建路由 | RouteConfig.vue 内 |
| AI Worker 集群管理 | (需确认是否已有) |

### 数据库
model schema 已有 10 个 Entity，仅缺：
- `m_model_tag` 表的 Entity

### 交付物
- 模型管理全链路页面视觉对齐
- m_model_tag Entity + CRUD
- AI Worker 集群管理页面（如设计稿有但前端无）

---

## 子项目 3：数据管理 CDR + RDR

这是工作量最大的子项目。

### 前端页面（CDR）
| 设计稿 | 前端文件 |
|--------|---------|
| 10-数据管理-患者列表 | PatientList.vue |
| 11a-数据管理-就诊详情 | EncounterDetail.vue |
| 11a2-就诊详情-诊断记录 | DiagnosisView.vue |
| 11a3-就诊详情-检验结果 | LabResultView.vue |
| 11a4-就诊详情-影像检查 | ImagingView.vue |
| 11a5-就诊详情-用药记录 | MedicationView.vue |
| 11a6-就诊详情-生命体征 | VitalSignView.vue |
| 11a7-就诊详情-临床文书 | ClinicalNoteView.vue |
| 患者360视图 | PatientDetail.vue |
| 数据字典映射 | DictManage.vue |
| 数据脱敏管理 | DesensitizeRule.vue |
| Modal-脱敏预览 | DesensitizePreview 组件 |
| Modal-导出审批 | (需确认) |
| Modal-新建映射规则 | (需确认) |

### 前端页面（RDR）
| 设计稿 | 前端文件 |
|--------|---------|
| 11-数据管理-研究项目 | ProjectList.vue |
| 11b-数据管理-研究项目详情 | ProjectDetail.vue |
| 12-数据管理-数据集管理 | DatasetList.vue |
| 12b-数据管理-数据集详情 | DatasetDetail.vue |
| 13-数据管理-数据质量 | QualityRuleList.vue, QualityResultList.vue |
| 14-数据管理-ETL任务 | EtlTaskList.vue, EtlTaskDetail.vue |
| 17-数据管理-数据源管理 | DataSourceList.vue |
| 17a-数据管理-数据源详情 | DataSourceDetail.vue |
| 18-数据管理-特征字典 | FeatureDictionary.vue |
| 同步任务列表 | SyncTaskList.vue |
| Modal-创建ETL任务 | EtlTaskList.vue 内 |
| Modal-新增数据源 | DataSourceList.vue 内 |
| Modal-创建项目 | ProjectList.vue 内 |
| Modal-新建数据集 | DatasetList.vue 内 |
| Modal-创建质量规则 | QualityRuleList.vue 内 |
| Modal-邀请成员 | (需确认) |

### 数据库（需新增 Entity）

**CDR 缺失的 25 张表：**
| 表名 | 用途 |
|------|------|
| c_encounter | 就诊记录 |
| c_diagnosis | 诊断 |
| c_lab_test | 检验项目 |
| c_lab_result | 检验结果 |
| c_medication | 药物 |
| c_medication_order | 用药医嘱 |
| c_vital_sign | 生命体征 |
| c_imaging_study | 影像检查 |
| c_imaging_series | 影像序列 |
| c_clinical_note | 临床文书 |
| c_operation | 手术记录 |
| c_allergy | 过敏信息 |
| c_pathology | 病理报告 |
| c_discharge_summary | 出院小结 |
| c_referral | 转诊记录 |
| c_follow_up | 随访记录 |
| c_lab_reference_range | 检验参考范围 |
| c_medication_admin | 用药执行 |
| c_fluid_balance | 出入量记录 |
| c_nursing_record | 护理记录 |
| c_consultation | 会诊记录 |
| c_transfer | 转科记录 |
| c_icd_mapping | ICD编码映射 |
| c_data_source | 数据来源 |
| c_sync_task | 同步任务 |

**RDR 缺失的 16 张表：**
| 表名 | 用途 |
|------|------|
| r_project_member | 项目成员 |
| r_cohort | 研究队列 |
| r_cohort_criteria | 队列筛选条件 |
| r_dataset_field | 数据集字段 |
| r_dataset_record | 数据集记录 |
| r_etl_task | ETL任务 |
| r_etl_step | ETL步骤 |
| r_etl_log | ETL日志 |
| r_quality_rule | 质量规则 |
| r_quality_result | 质量检查结果 |
| r_desensitize_rule | 脱敏规则 |
| r_desensitize_log | 脱敏日志 |
| r_feature | 特征定义 |
| r_feature_value | 特征值 |
| r_data_export | 数据导出 |
| r_export_approval | 导出审批 |

每个 Entity 需完整 CRUD：Entity → Repository → Service → Controller。

### 交付物
- CDR + RDR 所有页面视觉对齐
- 41 个新 Entity + 完整 CRUD
- 联调验证

---

## 子项目 4：标注管理

### 前端页面
| 设计稿 | 前端文件 |
|--------|---------|
| 12-标注管理-任务列表 | LabelTaskList.vue |
| 12a-标注管理-任务详情 | LabelTaskDetail.vue |
| 25-标注工作台-影像 | LabelWorkspace.vue |
| 25a-标注工作台-文本 | LabelWorkspaceText.vue |
| Modal-创建标注任务 | LabelTaskList.vue 内 |
| Modal-AI预标注 | (需确认) |
| Modal-标注审核 | (需确认) |

### 数据库
标注相关 Entity（LabelTaskEntity, LabelRecordEntity）已存在，需确认是否完整。

### 交付物
- 标注管理页面视觉对齐
- 缺失 Entity 补全（如有）

---

## 子项目 5：审计 + 告警

### 前端页面
| 设计稿 | 前端文件 |
|--------|---------|
| 15-审计日志-操作审计 | OperationLog.vue |
| 15a-审计日志-操作详情 | (AuditDetailDrawer 组件) |
| 15b-审计日志-数据访问审计 | DataAccessLog.vue |
| 15c-审计日志-系统事件 | (需确认) |
| 15d-审计日志-合规报表 | (需确认) |
| 16-告警中心 | AlertList.vue |
| 16a-告警中心-告警详情 | (需确认) |
| Modal-创建告警规则 | AlertRuleList.vue 内 |

### 数据库
audit schema 已完整（3 张表）。model schema 的 AlertRule 和 AlertRecord 也已有。

### 交付物
- 审计和告警页面视觉对齐
- 系统事件页面和合规报表页面（如前端缺失则新建）

---

## 子项目 6：系统设置

### 前端页面
| 设计稿 | 前端文件 |
|--------|---------|
| 20-系统设置-用户管理 | UserList.vue |
| 20a-系统设置-用户详情 | (需确认) |
| 21-系统设置-角色管理 | RoleList.vue |
| 21a-系统设置-角色详情 | (需确认) |
| 22-系统设置-系统配置 | SystemConfig.vue |
| 23-系统设置-组织管理 | (需确认) |
| 23a-系统设置-组织详情 | (需确认) |
| 24-系统设置-权限管理 | RolePermission.vue |
| Modal-新建用户 | UserList.vue 内 |
| Modal-编辑用户 | UserList.vue 内 |
| Modal-重置密码 | UserList.vue 内 |
| Modal-新建角色 | RoleList.vue 内 |
| Modal-新增组织 | (需确认) |
| Modal-新增配置 | (需确认) |

### 数据库
system schema 已基本完整。可能需要补充组织相关表。

### 交付物
- 系统设置页面视觉对齐
- 组织管理页面（如前端缺失则新建）

---

## 子项目 7：消息中心 + Docker 部署

### 前端页面
| 设计稿 | 前端文件 |
|--------|---------|
| 30-消息中心 | MessageList.vue |
| 30a-消息中心-消息详情 | (需确认) |
| 31-消息中心-通知设置 | NotificationSettings.vue |
| 32-消息中心-模板管理 | (需确认) |

### 数据库
消息相关 3 张表需创建 Entity：
- r_message → MessageEntity（可能已有）
- r_notification_setting → NotificationSettingEntity（可能已有）
- r_message_template → MessageTemplateEntity（可能已有）

### Docker 部署验证
- `docker-compose-infra.yml` 启动基础设施
- `docker-compose-full.yml` 启动全栈
- 各服务健康检查通过
- 前端可通过 Gateway 访问所有 API

### 交付物
- 消息中心页面视觉对齐
- 消息 Entity 确认/补全
- Docker 全栈部署通过验证

---

## 技术栈参考

- **前端**：Vue 3 + TypeScript + Vite + Naive UI
- **后端**：Spring Boot + Spring Data JPA + PostgreSQL
- **设计**：.pen 文件（Pencil MCP 工具访问）
- **部署**：Docker Compose（9 服务 + 5 基础设施）
- **注册中心**：Nacos
- **消息队列**：RabbitMQ
- **对象存储**：MinIO

## 执行流程

每个子项目遵循：
1. **对比分析**：截图对比 .pen vs Vue，列出差异清单
2. **前端修复**：逐页对齐
3. **数据库补全**：创建 Entity + Repository + Service + Controller
4. **联调验证**：确保前后端数据流通
5. **Git 提交**：每个子项目完成后提交

## 风险

1. **设计稿变更**：.pen 文件可能还在迭代，需要锁定版本
2. **Entity 依赖**：CDR 表之间有关联（外键），创建顺序需注意
3. **前端组件复用**：对齐过程中可能需要新建或修改共享组件
4. **Docker 网络**：微服务间通信需要 Nacos 注册正确
