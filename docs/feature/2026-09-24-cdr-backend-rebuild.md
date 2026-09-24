# 功能记录：数据中心 CDR 重建 + 按前端契约补齐后端

**日期**: 2026-09-24
**分支**: `feature/disease-kb`
**计划**: `docs/superpowers/plans/2026-09-24-cdr-backend-rebuild-plan.md`
**执行方式**: superpowers executing-plans 内联执行（TDD，逐任务提交）

## 背景与目标

前端 `maidc-portal` 17 个 API 文件全部走 `/api/v1`（分页要求 Spring Page 结构），调研比对后端 82 个控制器后确认 10 项缺口（G1–G10）。本批工作在保留工作区既有「微服务→单体」整合成果的前提下，以 DDL 为准绳修复 CDR 列名漂移，并按前端契约补齐缺失端点。

## 交付内容（8 任务 / 8 提交）

| 提交 | 任务 | 内容 |
|---|---|---|
| `ae0a37b` | T1 (G1+G9) | CDR 列名漂移修复：EncounterEntity/PatientEntity 对齐 DDL 词汇；Patient360Service、ClinicalSearchService、SmartSearchService 裸 SQL 对齐（SmartSearch 改内联 to_tsvector，DDL 无 fts 列）；PatientEncounterController 重挂 `/api/v1/cdr/patient-encounters`，前端 patientEncounter.ts 双前缀同步 |
| `6056136` | T2 (G2) | 就诊维度子资源 7 端点（diagnoses/lab-results/imaging/medications/vital-signs/notes/详情）+ EncounterSubresourceService 归属校验（fail-closed，不暴露存在性） |
| `5d9c62e` | T3 (G3) | `/cdr/statistics/data-growth-trend`（generate_series 空月补零）与 `/source-distribution`（空值归并 UNKNOWN），DataStatisticsService（JdbcTemplate，DDL 词汇） |
| `de3bcd9` | T4 (G4+G8) | 权限 CRUD（POST/PUT/DELETE `/permissions`，含 role_permission 关联清理）；RefreshTokenDTO 加 `@JsonAlias("refresh_token")`，refresh 响应独立 RefreshVO（snake_case 输出） |
| `4b9f871` | T5 (G5+G6) | `/monitoring/metrics` 别名；DeploymentController 详情 + routes CRUD（RouteUpsertDTO）；标注四件套（items/annotations/submit/skip）落 maidc-label 生效副本，/summary 统一前端契约键 |
| `ce225f2` | T6 (G7) | `GET /masterdata/representation-classes`：独立字典表 `masterdata.representation_class`（建表即含 is_deleted/org_id 审计列，杜绝 ddl-auto 追列漂移）+ WS/T 303-2023 八类种子 |
| `a14aa21` | T7 (G10) | ETL 数据源打通：`pipeline.sourceId` → DataSourceEntity 连接解析（逐字段回落 EtlProperties 兜底；target 走 EtlProperties 由部署侧指向 CDR 本体）；transform_type 补齐 MAP/DATE_FMT/LOOKUP/EXPRESSION（SELECT 生成层实现，受控片段拒绝 `;`/`--`/`/*`） |
| （本提交） | T8 | 全模块 compile 通过；data 134 / model 36 / auth 36 测试全绿；计划勾选与本记录 |

## 关键决策（Rulings）

1. **DDL 为准绳**：列名漂移集中在 4 文件，其余实体均为 DDL 词汇，故修代码而非改 DDL；新表 `representation_class` 从建表起就带 BaseEntity 全部审计列，避免重蹈「ddl-auto 启动追列」漂移。
2. **表示类独立字典表**：`23-masterdata-standard.sql` 无表示类表，`object_class` 是 WS/303 对象类语义，复用会污染语义，故新建表 + 标准种子。
3. **ETL target 连接**：管道表无 target_id，target 连接取 `EtlProperties.target*`（部署侧在 nacos 指向 CDR 本体）；source 按实体优先、逐字段回落。
4. **transform_expr 防护**：管理端受控录入的 SQL 片段仍拒绝语句终止符与注释符，配置生成期快速失败，不让坏片段流入 Embulk 查询。
5. **提交切分**：只 add 本次触碰文件；`23-masterdata-standard.sql`、`EtlProperties`、`EmbulkProcessRunner`（整合期未跟踪）按依赖闭包随相关任务入库并在提交信息注明。

## 不在本次范围（记录不实施）

10-cdr-patch 18 张 MIMIC 表的后端覆盖（前端无对应页面）；r_etl_step 种子数据；ddl-auto 改 validate（需先全量审计漂移，风险大）；DiseaseAiService 接真 AI。

## 最终评审与修复轮（同日）

fresh reviewer 整体评审结论 FAIL（2 Critical + 4 Important），已完成单一修复轮（每项 RED→GREEN）：

1. **[Critical] 统计 join 扇出**：data-growth-trend 月轴同时 LEFT JOIN 四张一对多表，月内跨表相乘导致各系列计数为交叉积 → 改 `COUNT(DISTINCT <pk>)`（DataStatisticsServiceTest 锚定 SQL 语义）
2. **[Critical] createPermission 缺 orgId**：`s_permission.org_id` NOT NULL，插入必 500 → 落系统级 `orgId=0L`（RoleServiceTest）
3. **[Important] 患者列表/详情 PII 明文**：PatientService 出口 idCardNo/phone 未脱敏 → 三个 VO 出口统一脱敏，对齐就诊流（PatientServiceTest 2）
4. **[Important] SmartSearch 误弃 fts 列**：Task 1 前提"DDL 无 fts 列"有误——`14-smart-search-fts.sql` 已建 zhparser 生成列+GIN；内联 to_tsvector('simple') 丢中文分词且全表扫 → 13 域全部切回存储 `fts` 列 + `plainto_tsquery('zh')`；RDR 标题列同步 coalesce DDL 词汇（project_name/dataset_name）
5. **[Important] 文档虚报测试交付**：计划 Task 8 曾称统计/refresh 单测"已随 Task 3/4 交付"不实 → 已更正并补齐测试（DataStatisticsServiceTest 3、RefreshContractNamingTest 2）
6. **[Important] 患者创建缺 400 校验**：gender/id_card_no 为 NOT NULL 列但无 @NotBlank → 补注解（CdrController 已有 @Valid），缺失返回 400 而非库约束 500（PatientCreateDTOValidationTest 2）

评审 Minor 项（延后不修）：实体长度与 DDL 漂移（encounter_no 64 vs 32 等 7 处）、RDR 实体 name 幽灵列、标注 items page_size 绑定（前端发 page_size 后端只绑 pageSize）、全 CONSTANT 映射产生空 SELECT、RoleController 裸 Map 强转、标注控制器双副本同 FQCN 依赖类路径顺序、Embulk YAML query 引号未转义、RepresentationClassServiceTest 仅验证委托。

## 验证

- `mvn -q compile` 全模块 exit 0
- `mvn test`（maidc-data, maidc-model, maidc-auth）：206/206 通过
- 新增单测：RepresentationClassServiceTest 1、EtlConfigGeneratorTest 8、EtlExecutionServiceTest 5、DataStatisticsServiceTest 3、RefreshContractNamingTest 2、PatientServiceTest 脱敏 2、PatientCreateDTOValidationTest 2、RoleServiceTest orgId 1（合计 24，均 RED→GREEN 或契约锚定）
- `mvn test`（maidc-data, maidc-auth）修复轮回归：全绿
- 部署验证入口：`http://localhost:3000`（docker-compose-full）
