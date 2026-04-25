# 医疗主数据系统设计

> 日期: 2026-04-25
> 状态: APPROVED
> 架构: 方案 A 统一术语服务 + 思路 B 标准与本地编码分离

## 1. 概述

### 1.1 目标

为 MAIDC CDR 构建全面医疗主数据管理平台，提供：

- **标准编码管理**：ICD-10、LOINC、SNOMED CT、ATC 四大编码体系的统一存储、查询和版本管理
- **多院区本地编码映射**：各家医院 HIS 系统的本地编码映射到标准概念，支持 ETL 标准化
- **临床规则**：参考范围、药物相互作用等临床决策支持数据
- **数据质量治理**：映射置信度、映射状态、自动匹配推荐

### 1.2 架构决策

- **统一术语模型**：所有标准编码统一为 `m_concept` 概念表，通过 `code_system_id` 区分编码体系
- **标准与本地分离**：标准概念放 `m_concept`，院内本地编码放 `m_local_concept`
- **混合数据来源**：内置基础数据集 + 批量导入工具 + 外部术语服务 API
- **前端范围**：后端 REST API + 管理后台页面（术语查询、映射管理、参考值配置）
- **无外键约束**：遵循项目既有原则，业务层保证数据完整性

## 2. 数据模型

### 2.1 `m_code_system` — 编码体系注册表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 主键 |
| code | VARCHAR(32) | UNIQUE NOT NULL | 体系代码（ICD10, LOINC, SNOMEDCT, ATC） |
| name | VARCHAR(128) | NOT NULL | 体系名称（ICD-10 国际疾病分类） |
| version | VARCHAR(32) | | 当前版本号 |
| description | TEXT | | 说明 |
| hierarchy_support | BOOLEAN | DEFAULT false | 是否支持层次结构 |
| status | VARCHAR(16) | DEFAULT 'ACTIVE' | ACTIVE/DRAFT/RETIRED |
| created_at | TIMESTAMP | DEFAULT NOW() | |
| updated_at | TIMESTAMP | DEFAULT NOW() | |

### 2.2 `m_concept` — 统一标准概念表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 主键 |
| concept_code | VARCHAR(64) | NOT NULL | 概念编码（如 J18.9, 2345-7） |
| code_system_id | BIGINT | NOT NULL | 所属编码体系 → m_code_system.id |
| name | VARCHAR(512) | NOT NULL | 概念名称（中文） |
| name_en | VARCHAR(512) | | 英文名称 |
| domain | VARCHAR(64) | | 领域（Diagnosis/Procedure/Lab/Drug/Observation） |
| standard_class | VARCHAR(64) | | 标准分类（如 ICD chapter、ATC level） |
| properties | JSONB | | 体系特有属性 |
| status | VARCHAR(16) | DEFAULT 'ACTIVE' | ACTIVE/DRAFT/RETIRED |
| valid_from | DATE | | 有效期起始 |
| valid_to | DATE | | 有效期截止 |
| created_at | TIMESTAMP | DEFAULT NOW() | |
| updated_at | TIMESTAMP | DEFAULT NOW() | |

UNIQUE 约束：`(concept_code, code_system_id)`

### 2.3 `m_concept_relationship` — 概念关系

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 主键 |
| concept_id_1 | BIGINT | NOT NULL | 源概念 → m_concept.id |
| concept_id_2 | BIGINT | NOT NULL | 目标概念 → m_concept.id |
| relationship_type | VARCHAR(64) | NOT NULL | 关系类型 |
| is_hierarchical | BOOLEAN | DEFAULT false | 是否层次关系 |
| created_at | TIMESTAMP | DEFAULT NOW() | |

UNIQUE 约束：`(concept_id_1, concept_id_2, relationship_type)`

关系类型枚举：

- `MAPS_TO` — 编码映射（ICD → SNOMED）
- `IS_A` — 层次结构（SNOMED 父子关系）
- `BROADER_THAN` — 分类层级（ATC）
- `EQUIVALENT` — 等价关系
- `REPLACES` — 版本替换

### 2.4 `m_concept_ancestor` — 祖先关系（闭包表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 主键 |
| ancestor_concept_id | BIGINT | NOT NULL | 祖先概念 → m_concept.id |
| descendant_concept_id | BIGINT | NOT NULL | 后代概念 → m_concept.id |
| min_levels_of_separation | INT | DEFAULT 0 | 最小层级间隔 |
| max_levels_of_separation | INT | DEFAULT 0 | 最大层级间隔 |

### 2.5 `m_concept_synonym` — 概念同义词

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 主键 |
| concept_id | BIGINT | NOT NULL | 关联概念 → m_concept.id |
| synonym | VARCHAR(512) | NOT NULL | 同义词 |
| language_code | VARCHAR(8) | DEFAULT 'zh' | 语言（zh/en） |
| is_preferred | BOOLEAN | DEFAULT false | 是否首选名称 |

### 2.6 `m_reference_range` — 参考范围

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 主键 |
| concept_id | BIGINT | NOT NULL | 关联概念 → m_concept.id |
| gender | VARCHAR(8) | DEFAULT 'ALL' | 性别（M/F/ALL） |
| age_min | DECIMAL | | 年龄下限（岁） |
| age_max | DECIMAL | | 年龄上限（岁） |
| range_low | DECIMAL | | 正常下限 |
| range_high | DECIMAL | | 正常上限 |
| unit | VARCHAR(32) | | 单位 |
| critical_low | DECIMAL | | 危急值下限 |
| critical_high | DECIMAL | | 危急值上限 |
| source | VARCHAR(128) | | 数据来源 |
| created_at | TIMESTAMP | DEFAULT NOW() | |
| updated_at | TIMESTAMP | DEFAULT NOW() | |

### 2.7 `m_drug_interaction` — 药物相互作用

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 主键 |
| drug_concept_id_1 | BIGINT | NOT NULL | 药物1 → m_concept.id |
| drug_concept_id_2 | BIGINT | NOT NULL | 药物2 → m_concept.id |
| severity | VARCHAR(16) | NOT NULL | 严重程度（MINOR/MODERATE/MAJOR/CONTRAINDICATED） |
| interaction_type | VARCHAR(32) | | 类型 |
| description | TEXT | | 交互描述 |
| evidence_level | VARCHAR(16) | | 证据等级 |
| clinical_action | TEXT | | 临床建议 |
| created_at | TIMESTAMP | DEFAULT NOW() | |
| updated_at | TIMESTAMP | DEFAULT NOW() | |

UNIQUE 约束：`(drug_concept_id_1, drug_concept_id_2)`（约定 id_1 < id_2）

### 2.8 `m_institution` — 医疗机构注册

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 主键 |
| inst_code | VARCHAR(32) | UNIQUE NOT NULL | 机构代码 |
| name | VARCHAR(128) | NOT NULL | 机构名称 |
| short_name | VARCHAR(64) | | 简称 |
| status | VARCHAR(16) | DEFAULT 'ACTIVE' | ACTIVE/DISABLED |
| created_at | TIMESTAMP | DEFAULT NOW() | |
| updated_at | TIMESTAMP | DEFAULT NOW() | |

### 2.9 `m_local_concept` — 院内本地编码

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 主键 |
| institution_id | BIGINT | NOT NULL | 所属机构 → m_institution.id |
| code_system_id | BIGINT | NOT NULL | 目标标准体系 → m_code_system.id |
| local_code | VARCHAR(64) | NOT NULL | 院内编码 |
| local_name | VARCHAR(512) | NOT NULL | 院内名称 |
| standard_concept_id | BIGINT | | 映射到的标准概念 → m_concept.id |
| mapping_confidence | DECIMAL(3,2) | | 映射置信度（0.00~1.00） |
| mapping_status | VARCHAR(16) | DEFAULT 'UNMAPPED' | CONFIRMED/AUTO/SUSPECTED/UNMAPPED |
| mapped_by | VARCHAR(64) | | 映射人/系统 |
| mapped_at | TIMESTAMP | | 映射时间 |
| created_at | TIMESTAMP | DEFAULT NOW() | |
| updated_at | TIMESTAMP | DEFAULT NOW() | |

UNIQUE 约束：`(institution_id, code_system_id, local_code)`

## 3. API 设计

### 3.1 编码体系管理 `/api/masterdata/code-systems`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 列出所有编码体系 |
| GET | `/{id}` | 获取体系详情 |
| GET | `/{id}/stats` | 获取体系统计（概念数、关系数等） |
| POST | `/` | 新增编码体系 |
| PUT | `/{id}` | 更新编码体系 |

### 3.2 概念查询 `/api/masterdata/concepts`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 分页查询（code_system, domain, keyword 过滤） |
| GET | `/{id}` | 概念详情（含 properties + synonyms） |
| GET | `/search` | 全文搜索（关键词 + 编码模糊匹配） |
| GET | `/{id}/children` | 直接子概念（IS_A 关系） |
| GET | `/{id}/descendants` | 所有后代（闭包表） |
| GET | `/{id}/ancestors` | 所有祖先 |
| GET | `/{id}/mappings` | 跨编码映射 |
| GET | `/{id}/synonyms` | 同义词列表 |
| POST | `/` | 新增概念 |
| PUT | `/{id}` | 更新概念 |
| DELETE | `/{id}` | 逻辑删除（status→RETIRED） |

### 3.3 概念映射 `/api/masterdata/mappings`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 查询映射（source_system, target_system, concept_code 过滤） |
| POST | `/` | 创建映射关系 |
| POST | `/batch` | 批量创建映射 |
| DELETE | `/{id}` | 删除映射 |

### 3.4 参考范围 `/api/masterdata/reference-ranges`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 查询（concept_id, gender, age 过滤） |
| GET | `/{id}` | 获取详情 |
| POST | `/` | 新增 |
| PUT | `/{id}` | 更新 |
| GET | `/evaluate` | 动态匹配（概念+性别+年龄） |

参考范围匹配优先级：精确匹配(性别+年龄) > 性别匹配 > 通用范围(ALL)。

### 3.5 药物相互作用 `/api/masterdata/drug-interactions`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 查询（drug1, drug2, severity 过滤） |
| POST | `/` | 新增 |
| GET | `/check` | 检查两个药物相互作用 |
| POST | `/check-list` | 检查一组药物的所有相互作用（处方审核） |

### 3.6 数据导入 `/api/masterdata/import`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/upload` | 上传术语文件（CSV/Excel/RF2），返回导入任务 ID |
| GET | `/tasks/{taskId}` | 查询导入任务状态 |
| GET | `/templates/{codeSystem}` | 下载导入模板 |

### 3.7 机构管理 `/api/masterdata/institutions`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 机构列表 |
| POST | `/` | 新增机构 |
| PUT | `/{id}` | 更新机构 |

### 3.8 本地编码映射 `/api/masterdata/local-concepts`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 查询本地编码（institution_id, code_system_id, mapping_status 过滤） |
| GET | `/unmapped` | 未映射的本地编码（映射质检用） |
| POST | `/` | 新增本地编码映射 |
| POST | `/batch` | 批量映射 |
| PUT | `/{id}` | 更新映射（确认/修改标准概念） |
| POST | `/auto-match` | 自动匹配（名称相似度推荐标准概念） |
| GET | `/translate` | 翻译本地编码→标准编码（ETL 调用） |
| GET | `/stats` | 映射统计（各机构映射覆盖率） |

translate 接口示例：

```
GET /api/masterdata/local-concepts/translate?
    institutionCode=HOSP_A&
    codeSystem=LOINC&
    localCode=GLU001

Response:
{
  "localCode": "GLU001",
  "localName": "血糖测定",
  "standardConcept": {
    "id": 23456,
    "conceptCode": "2345-7",
    "name": "葡萄糖",
    "codeSystem": "LOINC"
  },
  "confidence": 1.0,
  "mappingStatus": "CONFIRMED"
}
```

auto-match 接口示例：

```
POST /api/masterdata/local-concepts/auto-match
Body: { "institutionId": 1, "codeSystemId": 3 }

Response:
[
  {
    "localCode": "GLU002",
    "localName": "餐后血糖",
    "recommendedConcept": { "code": "41651-1", "name": "Glucose 2h post meal" },
    "confidence": 0.92,
    "matchMethod": "NAME_SIMILARITY"
  }
]
```

## 4. 后端架构

### 4.1 包结构

```
com.maidc.data.domain.masterdata/
├── controller/
│   ├── CodeSystemController.java
│   ├── ConceptController.java
│   ├── ConceptMappingController.java
│   ├── ReferenceRangeController.java
│   ├── DrugInteractionController.java
│   ├── MasterDataImportController.java
│   ├── InstitutionController.java
│   └── LocalConceptController.java
├── entity/
│   ├── CodeSystem.java
│   ├── Concept.java
│   ├── ConceptRelationship.java
│   ├── ConceptAncestor.java
│   ├── ConceptSynonym.java
│   ├── ReferenceRange.java
│   ├── DrugInteraction.java
│   ├── Institution.java
│   └── LocalConcept.java
├── repository/
│   ├── CodeSystemRepository.java
│   ├── ConceptRepository.java
│   ├── ConceptRelationshipRepository.java
│   ├── ConceptAncestorRepository.java
│   ├── ConceptSynonymRepository.java
│   ├── ReferenceRangeRepository.java
│   ├── DrugInteractionRepository.java
│   ├── InstitutionRepository.java
│   └── LocalConceptRepository.java
├── service/
│   ├── CodeSystemService.java
│   ├── ConceptService.java
│   ├── ConceptMappingService.java
│   ├── ReferenceRangeService.java
│   ├── DrugInteractionService.java
│   ├── MasterDataImportService.java
│   ├── InstitutionService.java
│   └── LocalConceptService.java
├── dto/
│   ├── ConceptSearchRequest.java
│   ├── ConceptDetailVO.java
│   ├── MappingQueryRequest.java
│   ├── ReferenceRangeEvaluateRequest.java
│   ├── DrugInteractionCheckRequest.java
│   ├── ImportTaskVO.java
│   └── LocalConceptTranslateRequest.java
└── enums/
    ├── DomainType.java          (DIAGNOSIS, PROCEDURE, LAB, DRUG, OBSERVATION)
    ├── RelationshipType.java    (MAPS_TO, IS_A, BROADER_THAN, EQUIVALENT, REPLACES)
    ├── SeverityLevel.java       (MINOR, MODERATE, MAJOR, CONTRAINDICATED)
    ├── ConceptStatus.java       (ACTIVE, DRAFT, RETIRED)
    └── MappingStatus.java       (CONFIRMED, AUTO, SUSPECTED, UNMAPPED)
```

### 4.2 关键服务逻辑

**ConceptService — 全文搜索**

使用 PostgreSQL tsvector + GIN 索引，搜索优先级：

1. 编码精确匹配（权重最高）
2. 名称前缀匹配
3. 全文模糊匹配
4. 同义词匹配

**ConceptMappingService — 跨编码映射**

映射路径：ICD10 → m_concept(id=100) → m_concept_relationship(MAPS_TO) → m_concept(id=200) → SNOMEDCT。支持多跳映射链。

**ReferenceRangeService — 动态匹配**

匹配优先级：精确匹配(性别+年龄) > 性别匹配 > 通用范围(ALL)。

**DrugInteractionService — 处方审核**

传入 N 个药物，两两组合查询 m_drug_interaction，按 severity 排序返回。

**MasterDataImportService — 异步导入**

上传文件 → MinIO 存储 → 异步逐行解析 → 批量 INSERT（每500条一批）。支持 CSV/Excel 通用格式和 SNOMED RF2 专用格式。

**LocalConceptService — 自动匹配**

基于 PostgreSQL pg_trgm 三元组扩展做名称模糊匹配，`similarity(local_name, standard_name) > 0.3` 推荐候选映射。

### 4.3 数据库索引

```sql
-- 概念查询核心索引
CREATE INDEX idx_concept_system_code ON m_concept(code_system_id, concept_code);
CREATE INDEX idx_concept_domain ON m_concept(domain) WHERE status = 'ACTIVE';
CREATE INDEX idx_concept_name_fts ON m_concept USING gin(to_tsvector('chinese_zh', name));
CREATE INDEX idx_concept_properties ON m_concept USING gin(properties);

-- 关系查询索引
CREATE INDEX idx_relationship_type ON m_concept_relationship(concept_id_1, relationship_type);
CREATE INDEX idx_relationship_reverse ON m_concept_relationship(concept_id_2, relationship_type);

-- 闭包表索引
CREATE INDEX idx_ancestor_desc ON m_concept_ancestor(descendant_concept_id);
CREATE INDEX idx_ancestor_asc ON m_concept_ancestor(ancestor_concept_id);

-- 参考范围匹配索引
CREATE INDEX idx_refrange_concept ON m_reference_range(concept_id, gender);

-- 药物相互作用索引（双向）
CREATE INDEX idx_drug_int_pair ON m_drug_interaction(drug_concept_id_1, drug_concept_id_2);
CREATE INDEX idx_drug_int_reverse ON m_drug_interaction(drug_concept_id_2, drug_concept_id_1);

-- 同义词搜索索引
CREATE INDEX idx_synonym_fts ON m_concept_synonym USING gin(to_tsvector('chinese_zh', synonym));

-- 本地编码索引
CREATE INDEX idx_local_concept_inst ON m_local_concept(institution_id, code_system_id);
CREATE INDEX idx_local_concept_translate ON m_local_concept(institution_id, code_system_id, local_code);
CREATE INDEX idx_local_concept_status ON m_local_concept(mapping_status);
CREATE INDEX idx_local_concept_standard ON m_local_concept(standard_concept_id);
```

## 5. 前端页面

### 5.1 路由与菜单

```typescript
{ path: '/masterdata', component: Layout, children: [
  { path: 'code-systems',    component: CodeSystems },
  { path: 'concepts',        component: ConceptBrowser },
  { path: 'mappings',        component: MappingManager },
  { path: 'clinical-rules',  component: ClinicalRules },
  { path: 'local-concepts',  component: LocalConceptMapping },
]}

// 侧边菜单
医疗主数据管理 (icon: DatabaseOutlined)
  ├── 编码体系
  ├── 概念浏览
  ├── 编码映射
  ├── 临床规则
  └── 本地编码映射
```

### 5.2 页面布局

**编码体系管理 `/masterdata/code-systems`**

卡片布局展示各编码体系（名称、版本号、概念数量、状态），点击进入概念列表。

**概念浏览与搜索 `/masterdata/concepts`**

左树右详情布局：左侧树形层级导航（基于 ancestor 闭包表）、右侧概念详情面板（编码、名称、属性、映射关系、同义词、参考范围 Tab）、底部概念列表表格。

**编码映射 `/masterdata/mappings`**

双向选择器（源体系 ↔ 目标体系），表格展示映射关系。

**临床规则 `/masterdata/clinical-rules`**

双 Tab 页：参考范围表格 + 药物相互作用表格（含处方审核功能）。

**本地编码映射 `/masterdata/local-concepts`**

机构+编码体系筛选，映射统计卡片（已确认/自动/待确认/未映射），本地编码列表表格，支持自动匹配和批量导入。

## 6. 数据初始化

### 6.1 Flyway 迁移脚本

```
maidc-data/src/main/resources/db/migration/
├── V50__masterdata_ddl.sql            -- 9张表DDL + 索引
├── V51__masterdata_codesystem.sql     -- 编码体系注册
├── V52__masterdata_icd10.sql          -- ICD-10 概念 + 层级
├── V53__masterdata_icd9cm.sql         -- ICD-9-CM-3 手术
├── V54__masterdata_loinc.sql          -- LOINC 常用检验项
├── V55__masterdata_snomed.sql         -- SNOMED CT 核心概念
├── V56__masterdata_atc.sql            -- ATC 药品分类
├── V57__masterdata_mappings.sql       -- 跨编码映射
├── V58__masterdata_refrange.sql       -- 参考范围
├── V59__masterdata_interactions.sql   -- 药物相互作用（常见300对）
└── V60__masterdata_institution.sql    -- 示例机构数据
```

### 6.2 内置数据集规模

| 体系 | 版本 | 预计概念数 | 初始化方式 |
|------|------|-----------|-----------|
| ICD-10 (中文版) | 2024版 | ~72,000 | 内置 SQL |
| ICD-9-CM-3 (手术) | 2011版 | ~13,000 | 内置 SQL |
| LOINC (中文子集) | v2.78 | ~1,500 | 内置 SQL（常用检验项） |
| SNOMED CT (中文子集) | 2024-01 | ~30,000 | 内置 SQL（核心临床概念） |
| ATC (中文版) | 2024 | ~6,000 | 内置 SQL |

### 6.3 properties JSONB 结构

ICD-10 示例：
```json
{ "chapter": "X", "chapter_name": "呼吸系统疾病", "block": "J09-J18", "block_name": "流行性感冒和肺炎", "category": "J18", "category_name": "肺炎(病原体未特指)" }
```

LOINC 示例：
```json
{ "component": "Glucose", "property": "MCnc", "time_aspect": "Pt", "system": "Ser/Plas", "scale": "Qn", "method_type": null, "order_obs": "Both" }
```

SNOMED CT 示例：
```json
{ "concept_id": "233604007", "fully_specified_name": "Pneumonia (disorder)", "semantic_tag": "disorder", "module_id": "900000000000207008" }
```

ATC 示例：
```json
{ "atc_level": 4, "atc_code": "J01CA04", "level1": "J", "level1_name": "抗感染药物", "level3": "J01C", "level3_name": "β-内酰胺类抗菌药,青霉素类", "ddd": "2.0", "ddd_unit": "g", "ddd_route": "O", "admin_route": "口服" }
```

### 6.4 与 CDR 表的逻辑关联

不修改现有 CDR 表结构，通过编码值逻辑关联：

| CDR 表 | 关联字段 | 目标 |
|--------|---------|------|
| c_diagnosis | icd_code → m_concept(ICD10) | 诊断标准化 |
| c_lab_test | test_code → m_concept(LOINC) | 检验项标准化 |
| c_medication | atc_code → m_concept(ATC) | 药品分类标准化 |
| c_operation | op_code → m_concept(ICD9CM) | 手术编码标准化 |

ETL 流程中的角色：

```
ODS (源系统) → m_local_concept (翻译) → m_concept (标准) → CDR (标准化存储)
```

## 7. 表总览

共 9 张表：

| 表名 | 用途 | 预计数据量 |
|------|------|-----------|
| m_code_system | 编码体系注册 | 4-10 |
| m_concept | 标准概念 | 120,000+ |
| m_concept_relationship | 概念关系 | 500,000+ |
| m_concept_ancestor | 祖先闭包 | 2,000,000+ |
| m_concept_synonym | 同义词 | 200,000+ |
| m_reference_range | 参考范围 | 5,000+ |
| m_drug_interaction | 药物相互作用 | 10,000+ |
| m_institution | 医疗机构 | 10-100 |
| m_local_concept | 院内本地编码 | 100,000+ (per institution) |
