# 重构记录：医学五字典模板化合并 + init-db DDL 补齐

## 基本信息

| 项目 | 内容 |
|------|------|
| 日期 | 2026-09-18 |
| 分支 | feature/disease-kb |
| 范围 | maidc-data dictionary 模块（后端）+ maidc-portal dictionary 页面（前端）+ docker/init-db |
| 背景 | 2026-09-15 masterdata 重构遗留 P2：dictionary 五套 CRUD 模板化合并、d_* 五字典表纳入 init-db 正式 DDL |

## 调研结论（重构依据）

- **后端 ~2000 行、五套 90% 相同样板**：Diagnosis/Drug/LabItem/ExamItem/FeeItem 五套 Controller/Service/Repository/Entity 结构完全平行，仅过滤字段、编码字段、排序与特有查询不同。
- **存量缺陷**：① delete 语义为「置 RETIRED 后物理删行」，与 BaseEntity 软删设计（is_deleted + @Where）及 DictService 逻辑删除不一致，且日志谎称"逻辑删除"；② update 的 if 链只合并了部分字段（如诊断的 icd9cmCode/parentId/level 等 API 无法更新）；③ `DrugService.buildTree` 死代码导致 `GET /drugs/categories` 只返回根分类扁平列表，而前端按树递归消费（children 永远为空）；④ 列表带 keyword 时丢弃全部过滤条件；⑤ 五个仓储各有多达 5 个从未被调用的查询方法。
- **前端 ~2000 行、五页 70–80% 复制粘贴**：列/表单/筛选/CRUD 逻辑五份拷贝；ExamItemList/FeeItemList 缺 `embedded` prop（DictionaryHub 内嵌降级为整页）；表格未套 `.tech-table`，Exam/Fee 按钮未用 tech-btn；5 条路由无 permission meta。
- **DDL 断裂**：完整 DDL 早已写好（`maidc-parent/docker/init-db/17-medical-dictionary.sql`，400 行，与实体逐列匹配），但该目录不被 compose 挂载，生效的 `docker/init-db/` 中缺失，存量库全靠 Hibernate ddl-auto:update 隐式建表。

## 改动清单

### 数据库
| 文件 | 说明 |
|------|------|
| docker/init-db/17-medical-dictionary.sql（新增） | 从 maidc-parent/docker/init-db 迁入生效目录并幂等化：CREATE TABLE/INDEX IF NOT EXISTS、种子数据 ON CONFLICT DO NOTHING。含 6 表（d_drug_category/d_drug/d_diagnosis/d_fee_item/d_lab_item/d_exam_item）+ d_lab_reference + 局部索引/GIN FTS + ICD 章节/ATC 分类种子。**业务编码唯一约束改为局部唯一索引（UNIQUE ... WHERE is_deleted = false）**，支撑逻辑删除后编码复用；存量库已同步迁移（删除 Hibernate 隐式全量唯一约束、建局部索引）。权限码无需新增（23-masterdata-standard.sql 已种 masterdata:read/create/update/delete） |

### 后端（maidc-data）
| 文件 | 说明 |
|------|------|
| repository/dictionary/DictionaryRepository.java（新增） | 泛型基接口：JpaRepository + JpaSpecificationExecutor + `findByIdAndIsDeletedFalse`，五套仓储继承 |
| service/dictionary/AbstractDictionaryService.java（新增） | 模板方法基类：固化分页列表（keyword 与过滤条件按 Specification 交集）、/search（空关键词 400）、getById（404 文案）、create（编码非空校验 + 重复编码 400 + orgId/status/isDeleted 兜底）、update（BeanUtils 非空合并，始终忽略 id/审计/isDeleted/orgId/业务编码）、delete（逻辑删除 is_deleted=true）；关键词检索统一 LOWER(col) LIKE（Specification 实现） |
| 五个 Service（重写瘦身） | 继承基类，各自仅保留：列表过滤 spec、编码/检索属性声明、默认排序、特有查询（诊断 tree/chapter/children、药品 by-code/by-atc/分类管理、检验 by-loinc/categories/children、检查 exam-types、收费 categories）。DrugService.buildTree 修复为真实挂 children 的递归树；deleteCategory 同步改逻辑删除 |
| 五个 Repository（精简） | 继承 DictionaryRepository，删除从未被调用的方法（findActiveByCode/findByCategory/findByIds/findByInsuranceCode/findBySpecimenType/findByBodySite/findByExamType/findByXxxCodeAndIsDeletedFalse/searchByKeyword 等）；关键词搜索由基类 Specification 取代 |
| DrugCategoryEntity | 加 `@Transient children`（树形响应字段，不落库） |
| Controller ×5 | 零改动（路径/参数/响应结构/权限注解不变） |
| 测试（新增） | service/dictionary/DiagnosisServiceTest（10 用例，经具体子类覆盖模板基类行为：查重/默认值/非空合并/编码不可变/逻辑删除/空关键词 400）+ DrugServiceTest（4 用例：树构建挂 children/层级继承/删除守卫/分类逻辑删除） |

### 前端（maidc-portal）
| 文件 | 说明 |
|------|------|
| dictionary/config.ts（新增） | 五类字典 schema 唯一来源：列定义、动态表单字段（含必填/编码不可变/静态与动态选项）、筛选项、详情抽屉字段、标签色映射、API 适配器（复用 medical-dictionary.ts，端点零变更） |
| dictionary/DictionaryListView.vue（新增） | 配置驱动引擎：深色科技风容器（独立路由挂 dark-tech-container，内嵌模式复用父容器）+ 自绘标题头（弃用白底 PageContainer）+ 筛选项 + `.tech-table` 表格（scoped :deep 深色表头/行/悬停/分页/固定列，空单元格 `-` 占位）+ 详情抽屉 + 双列动态表单弹窗 + 分页（顺带修复 pageSize 变更不生效）+ CRUD；统一支持 `embedded`（修复 Exam/Fee 在 DictionaryHub 内嵌降级）；增删改按钮按 masterdata:create/update/delete 权限显隐（usePermission） |
| dictionary/DrugCategoryTree.vue（新增） | 药品分类树侧栏（原 DrugList 内嵌面板拆出，深色化），emit select/loaded（树数据供表单 tree-select 复用），expose clearSelection |
| dictionary/IcdChapterMenu.vue（新增） | ICD-10 章节菜单侧栏（原 DiagnosisList 内嵌面板拆出，深色化） |
| 五个列表页（重写为薄壳） | DrugList/DiagnosisList/LabItemList/ExamItemList/FeeItemList 各 ~12 行：传 type + embedded 给引擎，组件名/路由契约不变；约 2000 行 → 约 60 行 |
| router/asyncRoutes.ts | dictionaries 组及 5 子路由补 `meta.permission: 'masterdata:read'`（对齐主数据路由组） |

## 行为变化（有意为之）

1. **删除统一为逻辑删除**（is_deleted=true）：行数据保留、编码可复用（DDL 的局部唯一索引支持）；诊断/药品/检验/检查/收费及药品分类一致。
2. **update 合并面变宽**：非空合并覆盖实体全部业务字段（旧 if 链漏掉的字段现在可更新）；业务编码创建后不可变。
3. **列表 keyword 与过滤条件取交集**：旧行为是有关键词时丢弃全部过滤条件（含左侧树/章节选中）；现传给列表接口的关键词与 categoryId/chapterCode/状态等同时生效（前端也因此统一只调列表接口，不再切换 /search）。
4. **关键词检索编码列改大小写不敏感**：原编码列 LIKE 区分大小写，统一 LOWER LIKE。
5. **`GET /drugs/categories` 返回真实树**：children 实际挂载（前端本就按树递归消费）。
6. **路由权限收敛**：5 条字典路由需 masterdata:read 才可见（与主数据组一致）；页面增删改按钮按 create/update/delete 权限显隐。
7. **检验/检查/收费筛选栏补状态过滤**：与药品/诊断对齐（后端本就支持 status 参数）。

## 实施过程中发现并修复的问题

- **泛型仓储缺 @NoRepositoryBean**：Spring Data 将中间接口本身按 BaseEntity 建查询导致应用启动失败——补注解后正常（单测为纯 Mockito 无法暴露此类启动期问题）。
- **Specification 组合 List.of NPE**：eqIfPresent 返回 null 时 List.of 抛 NPE——allOf 改为可含 null 的 varargs（@SafeVarargs），并补无过滤条件的回归用例。
- **唯一约束与逻辑删除冲突**：原 DDL 的 uk_*_code 为全量唯一约束，逻辑删除后编码无法复用（插入 500）——DDL 与存量库均改为 `WHERE is_deleted = false` 的局部唯一索引。
- **存量库 Hibernate 表无列默认值**：直接 SQL 灌种子需显式提供 created_by/created_at/is_deleted/org_id/status（JPA 审计平时负责填充）；新 DDL 文件自带默认值，全新部署无此问题。
- **前端深色化**：全局 .tech-table 的 :deep 规则在全局样式表不编译，表格仍白底——改为引擎 scoped :deep 深色表格；PageContainer 白底盖住 dark-tech-container 渐变——非内嵌模式弃用 PageContainer 改自绘标题头。

## 验证结果

- [x] mvn -pl maidc-data test：123/123 通过（含新增 15 个字典单测），BUILD SUCCESS
- [x] maidc-parent `mvn clean install -DskipTests` 全量打包通过，jar 复制至 build/maidc-app.jar
- [x] maidc-portal `npx vite build` 通过
- [x] docker compose 部署 portal + app + 全部依赖，容器健康
- [x] 接口自检：五字典列表 200；创建（默认值 ACTIVE/orgId=0）；重复编码 400「编码已存在」；空编码 400「编码不能为空」；keyword∩过滤条件交集（正确过滤 + 错误类别 0 命中）；非空合并更新（改价保名）；逻辑删除（is_deleted=true 落库）；删除后同编码可重建；药品分类树 children 正确挂载
- [x] 页面验证：5 条路由页 + DictionaryHub 内嵌 tab 全部截图核验深色科技风与功能
- [x] judge 视觉验收：6/6 通过（遗留非阻塞小项：分页「共 N 条」被表格横向滚动区轻微裁切，属原表格滚动配置既有表现）

## 遗留事项

- [ ] 代码提交（git-merge）与 PR
- [ ] 存量库若曾用旧接口物理删除过数据，无迁移需求（物理删行本就无痕）；此前误标 RETIRED 的行如需恢复另行处理
