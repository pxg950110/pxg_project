# CDR/RDR 智能全文检索引擎设计

## 目标

在现有单域 LIKE 搜索基础上，构建基于 PostgreSQL 全文检索的跨域统一搜索引擎，覆盖 CDR 11 个临床域 + RDR 2 个科研域，实现：

1. **单搜索框跨域搜索** — 输入关键词同时搜索 13 个域，按相关性排序返回统一结果
2. **中文全文检索** — 基于 zhparser 中文分词 + GIN 索引，毫秒级响应
3. **相关性排序** — `ts_rank_cd()` 计算分数，结果按匹配度排序
4. **匹配词高亮** — 前端展示匹配片段，关键词高亮

暂不接入 LLM/向量搜索，预留后续升级空间。

## 技术栈

| 层 | 技术 |
|---|---|
| 全文检索引擎 | PostgreSQL 15 + zhparser 中文分词 |
| 索引 | GIN (Generalized Inverted Index) |
| 后端 | Spring Boot + JPA Native Query |
| 前端 | Vue 3 + Ant Design Vue |

## 架构

```
用户输入搜索词
       ↓
SmartSearchController (POST /api/v1/cdr/smart-search)
       ↓
SmartSearchService.search()
       ├── 构建 UNION ALL SQL（13 个子查询 + ts_rank 排序）
       │     ├── cdr.c_patient        → name, phone_hash
       │     ├── cdr.c_encounter      → attending_doctor, diagnosis_summary, department
       │     ├── cdr.c_diagnosis      → diagnosis_name, diagnosis_code
       │     ├── cdr.c_lab_test       → test_name, test_code, ordering_doctor
       │     ├── cdr.c_medication     → med_name, med_code, prescriber
       │     ├── cdr.c_imaging_exam   → exam_type, body_part, report_text
       │     ├── cdr.c_operation      → operation_name, operation_code, surgeon
       │     ├── cdr.c_pathology      → diagnosis_desc, specimen_type
       │     ├── cdr.c_vital_sign     → sign_type
       │     ├── cdr.c_allergy        → allergen, reaction
       │     ├── cdr.c_clinical_note  → title, content, author
       │     ├── rdr.r_project        → name, description
       │     └── rdr.r_dataset        → name, description
       └── resolvePatientNames()（复用现有）
       ↓
SmartSearchResult → 前端统一列表
```

## 数据库层

### zhparser 安装

在 PostgreSQL 容器中安装 zhparser 扩展，配置中文分词：

```sql
CREATE EXTENSION zhparser;
CREATE TEXT SEARCH CONFIGURATION zh (PARSER = zhparser);
ALTER TEXT SEARCH CONFIGURATION zh ADD MAPPING FOR n,v,a,i,e,l WITH simple;
```

### tsvector 生成列 + GIN 索引

对每个需要搜索的表添加 tsvector 生成列和 GIN 索引。示例：

```sql
-- 患者
ALTER TABLE cdr.c_patient ADD COLUMN fts tsvector
  GENERATED ALWAYS AS (to_tsvector('zh', coalesce(name,'') || ' ' || coalesce(phone_hash,''))) STORED;
CREATE INDEX idx_patient_fts ON cdr.c_patient USING gin(fts);

-- 就诊记录
ALTER TABLE cdr.c_encounter ADD COLUMN fts tsvector
  GENERATED ALWAYS AS (to_tsvector('zh',
    coalesce(attending_doctor,'') || ' ' || coalesce(diagnosis_summary,'') || ' ' || coalesce(department,''))) STORED;
CREATE INDEX idx_encounter_fts ON cdr.c_encounter USING gin(fts);

-- ... 其他 11 个表同理
```

### 同义词词典（可选增强）

在 `share/postgresql/tsearch_data/` 下配置 `zhparser.syn` 医学同义词：

```
糖尿病 DM 糖尿
高血压 HTN 血压高
肺部感染 肺炎
...
```

## 后端 API

### 新增 DTO

```java
// SmartSearchRequest.java
public class SmartSearchRequest {
    private String keyword;          // 搜索关键词
    private List<String> domains;    // 可选：限定搜索域（空=全部）
    private LocalDate dateFrom;      // 可选：日期范围起
    private LocalDate dateTo;        // 可选：日期范围止
    private int page = 1;
    private int pageSize = 20;
}

// SmartSearchItem.java
public class SmartSearchItem {
    private String domain;           // 域名：PATIENT, ENCOUNTER, ...
    private Long id;                 // 记录 ID
    private Long patientId;          // 患者 ID（CDR 域有值）
    private String patientName;      // 患者姓名
    private String title;            // 标题（主要匹配字段）
    private String subtitle;         // 副标题（辅助信息）
    private double score;            // 相关性分数
    private String headline;         // 匹配片段（ts_headline 生成）
}

// SmartSearchResult.java
public class SmartSearchResult {
    private List<SmartSearchItem> items;
    private long total;
    private int page;
    private int pageSize;
    private Map<String, Long> aggregations;  // 各域命中数
}
```

### SmartSearchService 核心查询

通过原生 SQL 构建 UNION ALL 查询：

```java
@Service
public class SmartSearchService {

    public SmartSearchResult search(SmartSearchRequest req) {
        // 1. 将关键词转为 tsquery
        // 2. 构建 13 个子查询 UNION ALL
        // 3. 每个 SELECT 返回: domain, id, patient_id, title, subtitle, score, headline
        // 4. 全局 ORDER BY score DESC, LIMIT/OFFSET 分页
        // 5. 单独 COUNT 查询获取各域命中数（aggregations）
        // 6. 解析 patientNames
    }
}
```

每个子查询模板：

```sql
SELECT 'PATIENT' AS domain, p.id, p.id AS patient_id,
       p.name AS title,
       p.gender || ' ' || p.birth_date AS subtitle,
       ts_rank_cd(p.fts, query) AS score,
       ts_headline('zh', p.name || ' ' || p.phone_hash, query) AS headline
FROM cdr.c_patient p, plainto_tsquery('zh', :keyword) query
WHERE p.fts @@ query AND p.is_deleted = false
```

### SmartSearchController

```java
@RestController
@RequestMapping("/api/v1/cdr/smart-search")
public class SmartSearchController {
    @PostMapping
    public R<SmartSearchResult> search(@RequestBody SmartSearchRequest request) {
        if (request.getKeyword() == null || request.getKeyword().isBlank()) {
            return R.fail(400, "搜索关键词不能为空");
        }
        return R.ok(smartSearchService.search(request));
    }
}
```

## 前端

### 页面改造

替换现有 `ClinicalSearch.vue` 为智能搜索界面：

- **搜索区**：大搜索框 + 搜索按钮 + 可展开的高级筛选（域过滤、日期范围）
- **统计栏**：显示总命中数 + 各域命中分布（标签形式）
- **结果列表**：
  - 每条结果：域标签（颜色区分）+ 标题 + 副标题 + 匹配高亮片段
  - 患者名可点击跳转患者详情
  - 分页

### 域标签颜色映射

| 域 | 标签色 | 标签名 |
|---|---|---|
| PATIENT | blue | 患者 |
| ENCOUNTER | green | 就诊 |
| DIAGNOSIS | orange | 诊断 |
| LAB | cyan | 检验 |
| MEDICATION | purple | 用药 |
| IMAGING | geekblue | 影像 |
| SURGERY | red | 手术 |
| PATHOLOGY | magenta | 病理 |
| VITAL | lime | 体征 |
| ALLERGY | volcano | 过敏 |
| NOTE | gold | 文书 |
| PROJECT | teal | 科研项目 |
| DATASET | blue | 数据集 |

## 搜索域详情

| 域 | 表 | 搜索字段（title） | 辅助字段（subtitle） | patientId 列 |
|---|---|---|---|---|
| PATIENT | cdr.c_patient | name | gender + birthDate | id |
| ENCOUNTER | cdr.c_encounter | attending_doctor + diagnosis_summary | department + encounter_type | patient_id |
| DIAGNOSIS | cdr.c_diagnosis | diagnosis_name + diagnosis_code | diagnosis_type | patient_id |
| LAB | cdr.c_lab_test | test_name + test_code | specimen_type + status | patient_id |
| MEDICATION | cdr.c_medication | med_name + med_code | dosage + route + frequency | patient_id |
| IMAGING | cdr.c_imaging_exam | exam_type + body_part + report_text | modality + status | patient_id |
| SURGERY | cdr.c_operation | operation_name + operation_code | surgeon + anesthesia_type | patient_id |
| PATHOLOGY | cdr.c_pathology | diagnosis_desc + specimen_type | grade + stage | patient_id |
| VITAL | cdr.c_vital_sign | sign_type | sign_value + unit | patient_id |
| ALLERGY | cdr.c_allergy | allergen + reaction | allergen_type + severity | patient_id |
| NOTE | cdr.c_clinical_note | title + content | note_type + author | patient_id |
| PROJECT | rdr.r_project | name | description | null |
| DATASET | rdr.r_dataset | name | description | null |

## 文件清单

### 新建文件

| 文件 | 职责 |
|---|---|
| `docker/init-db/14-smart-search-fts.sql` | zhparser 配置 + 13 个表的 tsvector 列 + GIN 索引 |
| `dto/SmartSearchRequest.java` | 搜索请求 DTO |
| `dto/SmartSearchItem.java` | 单条搜索结果 |
| `dto/SmartSearchResult.java` | 搜索结果集 |
| `service/SmartSearchService.java` | 跨域 UNION 全文检索服务 |
| `controller/SmartSearchController.java` | POST /api/v1/cdr/smart-search |
| `views/data-cdr/ClinicalSearch.vue` | 重写为智能搜索页面 |

### 修改文件

| 文件 | 变更 |
|---|---|
| `maidc-portal/src/api/data.ts` | 添加 smartSearch() |
| `maidc-portal/src/router/asyncRoutes.ts` | 更新路由元信息 |

## 性能预估

| 指标 | 预估值 |
|---|---|
| 100 万条数据搜索响应 | < 200ms (GIN 索引) |
| 索引创建时间（100 万条） | < 30s |
| 索引空间开销 | 约原始文本的 30-50% |
| 并发支持 | PostgreSQL 连接池上限 |

## 后续升级路径

1. **接入 LLM** — 自然语言解析为结构化查询条件
2. **pgvector 语义搜索** — 安装 pgvector 扩展，预计算嵌入向量
3. **搜索建议** — 基于用户搜索历史的热词推荐
4. **搜索审计** — 记录搜索行为到审计日志
