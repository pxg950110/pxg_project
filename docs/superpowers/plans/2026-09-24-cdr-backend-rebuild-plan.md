# 实施计划：数据中心 CDR 重建 + 按前端契约补齐后端

**日期**: 2026-09-24
**分支**: `feature/disease-kb`
**依据**: 三份调研报告（前端 API 契约 17 文件 / 后端 82 控制器盘点 / CDR 数据层深挖），缺口已逐一 grep 实锤

## 背景（调研结论）

- 后端 `maidc-parent` 已相当完整：16 Maven 模块，`maidc-app`(8081) 单体聚合全部业务模块，`maidc-data` 55 控制器覆盖 CDR/主数据/RDR/ETL。
- 前端 `maidc-portal` 17 个 API 文件全部走 `/api/v1`（vite 代理→8081，无 rewrite），分页要求 Spring Page `content/totalElements` 结构。
- **CDR 数据层存在系统性列名漂移**：JPA 实体（`EncounterEntity`）与手写 DDL（`04-cdr.sql`）各自演化，`ddl-auto: update` 启动时把实体列追加进 DDL 建好的表 → 同表两套列（`admission_time`/`department`/`attending_doctor` vs DDL 的 `admit_time`/`dept_name`/`doctor_name`），ETL 写 DDL 列、应用读实体列，数据静默分裂。漂移仅集中在 4 个文件（EncounterEntity、cdr/Patient360Service、ClinicalSearchService、SmartSearchService），其余实体均为 DDL 词汇 → **以 DDL 为准绳修复**。
- 工作区有大量未提交的「微服务→单体」整合成果（7 个入口类已删等）——**必须保留，不可回退**；编译基线已验证为绿（mvn compile exit 0）。

## 缺口清单（已验证）

| # | 缺口 | 证据 |
|---|------|------|
| G1 | 列名漂移 4 文件（患者360 SQL 引用不存在的 `diag_code`/`diag_rank`/`c_patient.age` 等） | grep 实锤 |
| G2 | 就诊维度子资源端点缺失：`/cdr/patients/{p}/encounters/{e}/diagnoses\|lab-results\|imaging\|medications\|vital-signs\|notes` | CdrController 无匹配 |
| G3 | 数据中心统计端点缺失：`/cdr/statistics/data-growth-trend`、`/source-distribution` | 全仓 grep 0 命中 |
| G4 | 权限 CRUD 缺失（仅 tree+assign；前端需 POST/PUT/DELETE `/permissions`） | RoleController |
| G5 | `/monitoring/metrics` 别名缺失（后端只有 `/metrics/overview`）；`GET /deployments/{id}`、`POST/PUT /deployments/routes` 缺失 | Monitoring/DeploymentController |
| G6 | 标注 items/annotations/submit/skip 缺失（LabelWorkspace 页面四件套） | LabelTaskController 无匹配 |
| G7 | `representation-classes` 缺失（数据元标准页） | 全仓 grep 0 命中 |
| G8 | auth refresh 命名错位：前端发 `{refresh_token}`（snake），后端 `RefreshTokenDTO.refreshToken`（camel）；refresh 响应需 `access_token`/`expires_in` | RefreshTokenDTO.java:10 |
| G9 | 双前缀错位：前端 `patientEncounter.ts` 实际请求 `/api/v1/api/cdr/...`，后端 `PatientEncounterController` 挂 `/api/cdr` | 双侧各 1 处 |
| G10 | ETL 执行连接为占位配置（EtlProperties 自述"接入 DataSource 前的占位"），管道跑不通真实数据源；transform_type 仅 DIRECT/CONSTANT 生效 | EtlProperties/EtlConfigGenerator |

**不在本次范围**（记录不实施）：10-cdr-patch 18 张 MIMIC 表的后端覆盖（前端无对应页面）；r_etl_step 种子数据；ddl-auto 改 validate（需先全量审计漂移，风险大）；DiseaseAiService 接真 AI。

## 任务

### Task 1: CDR 列名漂移修复（G1 + G9）✅ 2026-09-24 commit ae0a37b
- [x] `EncounterEntity`：`department`→`dept_name`、`admission_time`→`admit_time`、`attending_doctor`→`doctor_name`（Java 字段同步改名，编译器兜底全部调用点；PatientEntity 同步修 `id_card_no`/`phone` 并补 `patient_no` 等 9 个真实列；create 路径自动生成 NOT NULL 的 `encounter_no`/`patient_no`）
- [x] `service/cdr/Patient360Service`：全部裸 SQL 对齐 DDL，前端契约键以 `AS admission_time` 等别名供数；补 `total_los_days`；检验异常数经 `c_lab_panel` JOIN 推导；患者搜索补齐此前从未生效的 encounterType/时间过滤
- [x] `ClinicalSearchService`、`SmartSearchService` 裸 SQL 同步对齐（后者发现 DDL 根本无 `fts` 列——全文检索原本在任何库上都必然报错，改为查询内联 `to_tsvector()`，无 DDL 变更）
- [x] `PatientEncounterController` 重挂 `/api/v1/cdr/patient-encounters`（避免与 CdrController 撞路径）；前端 `patientEncounter.ts` 两条路径同步改
- [x] 全仓漂移词汇 grep 归零（残留仅为文档注释/契约别名/请求参数，均合法）；另移除从未定义的 `@EntityGraph("encounter.withAllDetails")` 死引用；`mvn compile`/`test-compile` 通过，受影响测试 18/18 绿

### Task 2: 就诊维度子资源端点（G2）✅ 2026-09-24 commit 6056136
- [x] CdrController 增 7 个端点：GET `/cdr/patients/{p}/encounters/{e}`（详情，校验归属后复用 PatientEncounterService 聚合）、`.../diagnoses|lab-results|imaging|medications|vital-signs`（query: type/category/status，内存过滤）、`.../notes`（keyword，复用 ClinicalNoteService.searchNotes）
- [x] 新增 `EncounterSubresourceService`：无外键约定下的业务层患者-就诊归属校验（requireEncounterOfPatient，不匹配时抛 ENCOUNTER_NOT_FOUND，不暴露存在性）

### Task 3: 数据中心统计端点（G3）✅ 2026-09-24 commit 5d9c62e
- [x] 新增 `/cdr/statistics/data-growth-trend?months=`（generate_series 月份主轴 + LEFT JOIN 四业务线月度计数：c_lab_test/c_clinical_note/c_imaging_exam/c_pathology，空月补零）与 `/cdr/statistics/source-distribution`（c_patient.source_system 聚合，空值归并 UNKNOWN）
- [x] 新建 `service/cdr/DataStatisticsService`（JdbcTemplate，DDL 词汇）；前端 DataDashboard 两图契约键 months/clinical/research/imaging/pathology 与 [{name,value}] 已核对 series 语义

### Task 4: 权限 CRUD + 认证命名修复（G4 + G8）✅ 2026-09-24 commit de3bcd9
- [x] RoleController 增 POST `/permissions`、PUT `/permissions/{id}`、DELETE `/permissions/{id}`（契约 {name,code,type,parent_id}；RoleService 实现含 role_permission 关联业务层清理、code 派生 resource_key/action 填 NOT NULL 列；RolePermissionRepository 补 deleteByPermissionId）
- [x] `RefreshTokenDTO` 加 `@JsonAlias("refresh_token")`；refresh 响应改独立 `RefreshVO`（@JsonProperty 输出 access_token/expires_in）；login 保持 camelCase LoginVO 与前端 LoginResult 逐键核对一致

### Task 5: 模型域补端点（G5 + G6）✅ 2026-09-24 commit 4b9f871
- [x] MonitoringController 增 `GET /metrics` 别名（复用 overview）
- [x] DeploymentController 增 `GET /{id}` 详情（复用状态聚合）、`POST /routes`、`PUT /routes/{id}`（RouteUpsertDTO：{name,type,rules}→route_name/route_type/config）
- [x] 标注四件套落 maidc-label 生效副本：`GET /label/tasks/{id}/items`（分页+status）、`GET|PUT .../items/{itemId}/annotations`、`POST .../submit`（幂等推进 labeledCount）、`POST .../skip`；r_label_record 一行即一条目，归属校验 fail-closed；两副本 `/summary` 统一为前端契约键 {totalTasks,inProgress,labeledData,avgConsistency}（model 遮蔽副本的两个文件原为未跟踪整合副本，随本次编辑入库）

### Task 6: 主数据 representation-classes（G7）✅ 2026-09-24 commit ce225f2
- [x] 新增 `GET /api/v1/masterdata/representation-classes`：独立字典表 `masterdata.representation_class`（23-masterdata-standard.sql 无此表，object_class 语义为 WS/303 对象类不宜复用；建表即含 BaseEntity 审计列 is_deleted/org_id，杜绝 ddl-auto 追列漂移）+ WS/T 303-2023 八类种子（AMOUNT/CODE/COUNT/DATE/DATETIME/TIME/TEXT/NUMBER）；`RepresentationClassService.listActive()` 单测 1/1（RED→GREEN）

### Task 7: ETL 与数据源打通（G10）✅ 2026-09-24 commit a14aa21
- [x] `EtlExecutionService.executeSingleStep`：按 `pipeline.sourceId` 解析 `DataSourceEntity`，实体字段逐项优先、空缺回落 `EtlProperties` 兜底；target 无实体引用（表无 target_id），走 EtlProperties.target*（部署侧指向 CDR 本体连接）
- [x] transform_type 补齐（SELECT 生成层，Embulk column_options 对计算列 DIRECT 直通目标别名）：`MAP`→CASE expr END、`DATE_FMT`→TO_CHAR（expr 空则 CAST VARCHAR）、`LOOKUP`→(子查询)、`EXPRESSION`→受控 SQL 片段；transform_expr 拒绝 `;`/`--`/`/*`（配置生成期快速失败）
- [x] EtlProperties 占位值保留为兜底，优先实体连接；测试 EtlConfigGeneratorTest 8/8 + EtlExecutionServiceTest 5/5（RED 7 败→GREEN）；整合期未跟踪的 EmbulkProcessRunner 及 EtlProperties 修改随本提交入库（依赖闭包）

### Task 8: 验证与收尾 ✅ 2026-09-24
- [x] `mvn -q compile` 全模块通过（exit 0）
- [x] 相关模块 `mvn test`：maidc-data 134/134、maidc-model 36/36、maidc-auth 36/36 全绿；新增最小单测见 Task 6/7（统计 SQL 与 refresh 命名的单测已随 Task 3/4 交付）
- [x] 提交：按任务分批 conventional commit（只 add 本次触碰文件；23-masterdata-standard.sql / EtlProperties / EmbulkProcessRunner 等整合期既有改动已在提交信息注明）
- [x] 前端 `patientEncounter.ts` 路径修改已随 Task 1 提交（portal 侧 ae0a37b）
- [x] 更新本计划勾选状态；新增 `docs/feature/2026-09-24-cdr-backend-rebuild.md` 功能记录

## 约束

- 不回退工作区既有未提交整合成果；不触碰 `maidc-aiworker`（Python）、`docker/`、其他 effort 的文档
- 所有新表遵循无外键约定 + 审计列；所有新端点返回 `R<T>` 包装、分页用 Spring Page
- 前端仅允许 Task 1 的两行路径修正
