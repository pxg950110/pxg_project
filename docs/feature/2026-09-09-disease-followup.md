# 功能实现记录

## 基本信息

| 项目 | 内容 |
|------|------|
| **日期** | 2026-09-09 ~ 2026-09-10 |
| **分支** | feature/disease-kb |
| **需求标题** | CRS 专病管理·随访管理后端（慢性鼻窦炎试点） |
| **设计依据** | docs/superpowers/specs/2026-09-08-disease-management-design.md（§1-§8 已确认） |
| **实施计划** | docs/plans/2026-09-09-disease-followup.md |

## 修改文件清单

### 新增（后端 maidc-data，包 com.maidc.data）

| 层 | 文件 | 说明 |
|----|------|------|
| DDL | docker/init-db/20-disease-followup.sql | 6 表 + 5 量表种子 + J32 模板/队列/方案种子 + 3 权限码（幂等可重放） |
| Entity | entity/FollowupProtocolEntity / PatientFollowupEntity / FollowupTaskEntity / ScaleDefinitionEntity / ScaleAssessmentEntity / TreatmentRecordEntity | 全部 @Where(is_deleted)+@SQLDelete 软删 |
| Repository | repository/ 同名 ×6 | 含 uq_active_followup 部分唯一、UNIQUE(task_id,scale_code) 条件索引 |
| 引擎 | service/followup/ScaleEngine.java | definition 解析/answers 逐条校验/服务端计分/归一化快照；AUTO_READONLY 与 OPTIONS 绑定项豁免必答 |
| Service | service/followup/ScaleDefinitionService | 列表按 scaleCode 取最新版/发布=version+1 新行/停用前置校验(被方案引用阻止) |
| Service | service/followup/FollowupProtocolService | 方案 CRUD/发布新版本/ARCHIVE 旧版/stages 校验(量表 ACTIVE) |
| Service | service/followup/PatientFollowupService | 建档(在队列校验+版本快照+全量生成任务 due=enroll+offset)/状态机(ACTIVE⇄SUSPENDED/CLOSED 原因必填/OUT_OF_COHORT)/升级方案(**只重建被删任务对应阶段**) |
| Service | service/followup/FollowupTaskService | 工作台分组(今日/超期/未来7天, OVERDUE 派生)/complete 事务(assessments 批量+可选 treatments+task DONE)/必评覆盖校验/skip/计划外评估(task_id=NULL) |
| Service | service/followup/TreatmentRecordService | 手工 CRUD/CDR 候选(180 天窗口+exists 标注)/import-cdr(同 name+date 跳过并列出) |
| Service | service/followup/OutcomeStatsService | 依从率/SNOT-22 改善率(MCID9, 基线-末次≥3月)/手术率/再手术(≥2 ESS)/四量表阶段趋势/药物分布 |
| Controller | controller/followup/ ×5 | FollowupProtocolController / PatientFollowupController / FollowupTaskController / ScaleDefinitionController / TreatmentRecordController，@PreAuthorize disease:followup:manage/work + disease:scale:manage |
| 测试 | test/…/followup/ScaleEngineTest (9) + PatientFollowupServiceTest (7) | 引擎计分/校验/归一化/绑定豁免 + 建档任务生成边界/状态机守卫/升级语义 |

### 修改（前置缺陷修复）

| 文件 | 修改 |
|------|------|
| entity/DiagnosisEntity.java | 列映射 `diagnosis_code/name` → `icd_code/icd_name`（对齐 04-cdr.sql，原映射 SELECT 直接 SQL 报错） |
| service/DiseaseCohortService.java | DOMAIN_FIELD_MAP DIAGNOSIS 域同步改 icd_code/icd_name |
| service/SmartSearchService.java | DIAGNOSIS 分支原生 SQL 同步改列名 |
| scripts/generate_cdr/gen_clinical.py | CSV 列头 diagnosis_code/name → icd_code/icd_name（入库全空的根因） |
| scripts/generate_cdr/config.py | 新增病种「慢性鼻窦炎 J32.9」(D018 耳鼻喉科, 手术/CT/鼻喷剂等) + ICD10 追加 J32.9 |

## 数据修复

- 重生成合成数据（97 万行）+ `import_cdr_data.py --truncate` 重导：c_diagnosis 21,666 行 icd_code 100% 填充，**J32.9 = 653 条** → J32 队列匹配 **454 人**
- 20 号 SQL 幂等执行：5 量表/方案 v1/J32 队列/3 权限码落库

## 技术要点

- OVERDUE 不落库：工作台/时间轴按 `due_date < today && PENDING` 实时派生
- 版本快照：建档快照 protocol_version；方案修改=新版本行(PUBLISHED→旧 ARCHIVED)；升级只重建被删任务对应阶段（SKIPPED/DONE 阶段不复活——冒烟中发现全量重建会复活基线，已修复+单测覆盖）
- 服务端计分不信任前端；answers 归一化剔除未知条目；binding_sources 预留绑定来源快照列
- 软删实体统一 @Where+@SQLDelete；无外键，业务层校验（404 不暴露存在性）
- JPQL 不支持 `\"` 转义 → 引用计数查询改 native SQL（启动期才暴露，已记入教训）

## 测试验证

- [x] 单测 101/101 通过（maidc-data 全量，含新增 16）
- [x] API 冒烟全链路（见下）

### API 冒烟记录（网关 8080，admin token）

| 场景 | 结果 |
|------|------|
| GET /cdr/scales | 5 量表 ✓ |
| POST cohorts/5/sync | J32 匹配 454 人 ✓ |
| POST followups 建档 | 4 阶段任务 due 正确 ✓ |
| POST tasks/1/complete | 服务端计分 SNOT22=22/LK=5 ✓；缺必评 400；重复提交 409 ✓ |
| GET followup-tasks/my | 今日/超期分组正确 ✓ |
| POST tasks/5/skip | 原因必填 400；中文原因落库 ✓ |
| 方案 PUT 发布 v2/v3 + upgrade-protocol | DONE 基线不复活/旧任务软删/新 offset 生效 ✓ |
| suspend/resume/close | 状态机守卫 409 + 冻结提交 409 + 中文结案原因 ✓ |
| 计划外评估 | task_id=NULL, 计分 ✓ |
| import-cdr ×2 | 首次 2 条导入/重复全跳过列出 ✓ |
| GET outcome-stats | 依从率 100/药物分布聚合 ✓ |

### 已知数据侧限制（非代码缺陷）

- 合成数据用药 start_time 最晚 2025-12 → CDR 候选 180 天窗口内为空；import 端点已用直连请求验证
- admin 密码由 auth DataInitializer 每次启动重置为 Admin@123（既有 dev 机制）

## 待处理事项

- [x] 提醒链路：@Scheduled 每日扫描 + MQ → maidc-msg 站内信（2026-09-10 第二轮完成）
- [x] 前端平移：原型 /proto/crs/* → 正式页面接真实 API（2026-09-10 第二轮完成）
- [x] DEPT 数据范围：doctor/nurse 仅本科室患者（2026-09-10 第二轮完成）
- [ ] Git 提交（说"提交代码"触发 git-merge）

---

# 第二轮实现记录（2026-09-10）

## R1 提醒链路（设计 §4 提醒分级）

| 文件 | 内容 |
|------|------|
| maidc-data `mq/FollowupNotifyProducer` | 发 maidc.msg exchange / system.notify 队列（常量与 msg 端约定一致，不跨模块依赖） |
| maidc-data `service/followup/FollowupReminderScheduler` | @Scheduled 每日 08:00（Asia/Shanghai）：今日到期/超期 PENDING → 站内信护士（无护士→医生）；超 7 天 → 追加升级医生；按 (taskId,日期,升级) 内存去重（多实例需换 Redis）；发送失败回滚去重位重试；单任务失败不中断扫描 |
| maidc-data FollowupTaskController | `POST /followup-tasks/reminders/run`（manage）手动触发联调 |
| maidc-msg `mq/AlertNotifyConsumer` | resolveMessageType 增加 followup.due/overdue/escalate → FOLLOWUP |
| maidc-msg `service/MessageService` | sendMessage 补 orgId 默认 0（m_message 非空约束） |

**端到端冒烟通过**：超期 42 天档案 → m_message 落库 2 条（护士 user2「随访任务已超期」+ 医生 user1「随访超期升级」，type=FOLLOWUP，biz_id=任务 id）。

**环境根因修复**（与代码无关但必须记录）：RabbitMQ 数据卷曾重建导致 vhost `/` 与 `maidc.model` exchange 丢失 → msg 的 RabbitAdmin 绑定声明链中断（channel 404）→ 消息发到 exchange 后无绑定被静默丢弃。已重建 vhost/exchange/绑定；生产部署用 init 容器声明。

## R2 前端平移（原型 → 正式代码）

| 文件 | 说明 |
|------|------|
| `api/followup.ts` | 全部后端端点封装（方案/档案/任务/评估/量表/治疗/结局/绑定/提醒） |
| `views/followup/FollowupWorkbench.vue` | 一级菜单（`/followup/workbench`，permission disease:followup:work）：三组统计卡+任务表+执行抽屉（必评+选评+治疗+跳过） |
| `views/data-cdr/followup/ScaleManagement.vue` | 量表列表 + 预览（复用填写组件）/设计/停用（disease:scale:manage） |
| `views/data-cdr/followup/ScaleDesigner.vue` | form-design 三栏设计器正式版：`?scaleCode=` 经 API 加载 definition、发布调 version+1 |
| `views/data-cdr/followup/FollowupArchiveDetail.vue` | 聚合详情（档案头+时间轴+评估曲线+治疗+CDR 带入+暂停/恢复/结案/升级/计划外评估），按钮按 manage 权限显隐 |
| `views/data-cdr/followup/components/ScaleFillPanel.vue` | 唯一量表渲染入口：getScaleLatest 拉 definition；patientId 传入时调绑定解析端点自动带入（只读快照/预填/选项枚举） |
| `views/data-cdr/followup/components/EnrollModal.vue` | 队列患者下拉 + 系统用户（按角色过滤医生/护士）+ 真实建档 |
| `views/data-cdr/DiseaseDetail.vue` | 改造为 4 Tab：概览（基础信息+混合指标）/患者队列（建档状态列+行内建档）/随访管理（方案卡 steps+档案表+发布新版本）/结局看板（4 指标+趋势折线+药物分布） |
| `router/asyncRoutes.ts` | data 下新增 scales / scales/design(hidden) / followup/:fid(hidden)；新一级菜单 随访工作台（sort 3，permission 码过滤） |

**配套后端增强**：FollowupDisplayNameResolver（批量填充患者/医护姓名，@Transient 展示字段）、myWorkbench 行附患者信息、`GET /followups/{id}/detail` 聚合端点、`GET /scales/{code}/bindings?patientId=` 绑定解析（LAB/VITAL 最近值、MEDICATION/DIAGNOSIS 枚举）、ScaleEngine maxScore=0 采集类不计分守卫。

**前端适配要点**：后端 definition JSONB 以字符串输出 → 前端统一 `typeof d==='string' ? JSON.parse(d)`；`definition.items[].type` 在种子中未显式声明时默认 SCORE_RADIO（渲染回退链 itemOptions）。

**浏览器验证通过**：工作台（今日 1/超期 7 真实分组）、专病详情 4 Tab（10 档案/结局图表渲染）、量表管理 5 条、设计器 CRS_AUX 5 题+绑定 tag 回显、档案详情（真实患者名+时间轴 4 节点+空评估 empty state）。

## R3 DEPT 数据范围（设计 §5，∃ 语义）

| 文件 | 内容 |
|------|------|
| `DeptScopeService` | 复用 PermissionStore/DataScopeHelper/机构表，解析用户科室名（与 PatientEncounterService 一致）；DEPT 下不可得 → fail-closed 抛 notFound |
| PatientFollowupRepository | +3 个 native 查询：cohort 档案分页 ∃ 就诊科室、工作台 ACTIVE+PENDING∪dept |
| PatientFollowupService | list/get(DEPT 404 兜底) 接入 |
| FollowupTaskService | 工作台 mine/all 视图接入 |

语义：随访实例可见性 = 患者可见性（患者任一就诊科室 == 用户科室）；范围外 404 不暴露存在性。

## 第二轮测试

- [x] 单测 101/101（新增 DeptScope mock 装配）
- [x] 绑定解析冒烟：体重 65.47kg 带入 / 用药 9 条枚举 / 无检验数据显示空态
- [x] 提醒链路：MQ → 站内信落库（2 条，见上）
- [x] 前端 5 页浏览器验证（console 仅剩既有 router 初始 warn）
