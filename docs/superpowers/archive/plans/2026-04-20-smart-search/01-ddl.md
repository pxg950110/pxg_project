# Plan 01: 数据库全文检索基础设施

**Goal:** 安装 zhparser + 创建 13 个表的 tsvector 生成列 + GIN 索引

**依赖:** 无

---

## Task 1: zhparser 安装脚本

**Files:**
- Create: `docker/postgres-zhparser/Dockerfile`

- [ ] **Step 1: 创建自定义 PostgreSQL Dockerfile**

```dockerfile
FROM postgres:15

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    postgresql-server-dev-15 \
    git \
    wget \
    && rm -rf /var/lib/apt/lists/*

# 安装 scws (Simple Chinese Word Segmentation)
RUN wget -q -O /tmp/scws-1.2.3.tar.bz2 https://github.com/hightman/scws/releases/download/v1.2.3/scws-1.2.3.tar.bz2 \
    && tar xjf /tmp/scws-1.2.3.tar.bz2 -C /tmp \
    && cd /tmp/scws-1.2.3 \
    && ./configure \
    && make \
    && make install \
    && rm -rf /tmp/scws-1.2.3*

# 安装 zhparser
RUN git clone https://github.com/amutu/zhparser.git /tmp/zhparser \
    && cd /tmp/zhparser \
    && SCWS_HOME=/usr/local make \
    && SCWS_HOME=/usr/local make install \
    && rm -rf /tmp/zhparser
```

- [ ] **Step 2: 修改 docker-compose-infra.yml 中 PostgreSQL 使用自定义镜像**

在 `maidc-postgres` service 中添加 `build` 指向 Dockerfile：

```yaml
  maidc-postgres:
    build:
      context: ./postgres-zhparser
      dockerfile: Dockerfile
    # 保留原有的 image、environment、volumes 等不变
```

- [ ] **Step 3: 重建 PostgreSQL 容器**

```bash
cd docker
docker compose -f docker-compose-infra.yml build maidc-postgres
docker compose -f docker-compose-infra.yml up -d maidc-postgres
```

- [ ] **Step 4: 验证 zhparser 可用**

```bash
docker exec maidc-postgres psql -U maidc -d maidc -c "CREATE EXTENSION IF NOT EXISTS zhparser; SELECT * FROM pg_extension WHERE extname='zhparser';"
```

Expected: 输出包含 zhparser 行

- [ ] **Step 5: 提交**

```bash
git add docker/postgres-zhparser/Dockerfile docker/docker-compose-infra.yml
git commit -m "feat(search): add zhparser-enabled PostgreSQL Dockerfile"
```

---

## Task 2: FTS 初始化 SQL 脚本

**Files:**
- Create: `docker/init-db/14-smart-search-fts.sql`

- [ ] **Step 1: 创建全文检索初始化脚本**

```sql
-- ============================================================
-- 14-smart-search-fts.sql
-- CDR/RDR 智能全文检索：zhparser + tsvector 生成列 + GIN 索引
-- ============================================================

-- 1. 中文分词配置
CREATE EXTENSION IF NOT EXISTS zhparser;
CREATE TEXT SEARCH CONFIGURATION IF NOT EXISTS zh (PARSER = zhparser);
ALTER TEXT SEARCH CONFIGURATION zh ADD MAPPING FOR n,v,a,i,e,l WITH simple;

-- 2. CDR 表 tsvector 生成列 + GIN 索引

-- c_patient: 搜索 name
ALTER TABLE cdr.c_patient ADD COLUMN IF NOT EXISTS fts tsvector
  GENERATED ALWAYS AS (to_tsvector('zh', coalesce(name,''))) STORED;
CREATE INDEX IF NOT EXISTS idx_patient_fts ON cdr.c_patient USING gin(fts);

-- c_encounter: 搜索 doctor_name, diagnosis_name, dept_name
ALTER TABLE cdr.c_encounter ADD COLUMN IF NOT EXISTS fts tsvector
  GENERATED ALWAYS AS (to_tsvector('zh',
    coalesce(doctor_name,'') || ' ' || coalesce(diagnosis_name,'') || ' ' || coalesce(dept_name,''))) STORED;
CREATE INDEX IF NOT EXISTS idx_encounter_fts ON cdr.c_encounter USING gin(fts);

-- c_diagnosis: 搜索 icd_name, icd_code
ALTER TABLE cdr.c_diagnosis ADD COLUMN IF NOT EXISTS fts tsvector
  GENERATED ALWAYS AS (to_tsvector('zh',
    coalesce(icd_name,'') || ' ' || coalesce(icd_code,''))) STORED;
CREATE INDEX IF NOT EXISTS idx_diagnosis_fts ON cdr.c_diagnosis USING gin(fts);

-- c_lab_test: 搜索 test_name, test_code, ordering_doctor
ALTER TABLE cdr.c_lab_test ADD COLUMN IF NOT EXISTS fts tsvector
  GENERATED ALWAYS AS (to_tsvector('zh',
    coalesce(test_name,'') || ' ' || coalesce(test_code,'') || ' ' || coalesce(ordering_doctor,''))) STORED;
CREATE INDEX IF NOT EXISTS idx_lab_test_fts ON cdr.c_lab_test USING gin(fts);

-- c_medication: 搜索 med_name, med_code, prescriber
ALTER TABLE cdr.c_medication ADD COLUMN IF NOT EXISTS fts tsvector
  GENERATED ALWAYS AS (to_tsvector('zh',
    coalesce(med_name,'') || ' ' || coalesce(med_code,'') || ' ' || coalesce(prescriber,''))) STORED;
CREATE INDEX IF NOT EXISTS idx_medication_fts ON cdr.c_medication USING gin(fts);

-- c_imaging_exam: 搜索 exam_type, body_part, report_text
ALTER TABLE cdr.c_imaging_exam ADD COLUMN IF NOT EXISTS fts tsvector
  GENERATED ALWAYS AS (to_tsvector('zh',
    coalesce(exam_type,'') || ' ' || coalesce(body_part,'') || ' ' || coalesce(report_text,''))) STORED;
CREATE INDEX IF NOT EXISTS idx_imaging_fts ON cdr.c_imaging_exam USING gin(fts);

-- c_operation: 搜索 operation_name, operation_code, surgeon
ALTER TABLE cdr.c_operation ADD COLUMN IF NOT EXISTS fts tsvector
  GENERATED ALWAYS AS (to_tsvector('zh',
    coalesce(operation_name,'') || ' ' || coalesce(operation_code,'') || ' ' || coalesce(surgeon,''))) STORED;
CREATE INDEX IF NOT EXISTS idx_operation_fts ON cdr.c_operation USING gin(fts);

-- c_pathology: 搜索 diagnosis_desc, specimen_type
ALTER TABLE cdr.c_pathology ADD COLUMN IF NOT EXISTS fts tsvector
  GENERATED ALWAYS AS (to_tsvector('zh',
    coalesce(diagnosis_desc,'') || ' ' || coalesce(specimen_type,''))) STORED;
CREATE INDEX IF NOT EXISTS idx_pathology_fts ON cdr.c_pathology USING gin(fts);

-- c_vital_sign: 搜索 sign_type
ALTER TABLE cdr.c_vital_sign ADD COLUMN IF NOT EXISTS fts tsvector
  GENERATED ALWAYS AS (to_tsvector('zh', coalesce(sign_type,''))) STORED;
CREATE INDEX IF NOT EXISTS idx_vital_sign_fts ON cdr.c_vital_sign USING gin(fts);

-- c_allergy: 搜索 allergen, reaction
ALTER TABLE cdr.c_allergy ADD COLUMN IF NOT EXISTS fts tsvector
  GENERATED ALWAYS AS (to_tsvector('zh',
    coalesce(allergen,'') || ' ' || coalesce(reaction,''))) STORED;
CREATE INDEX IF NOT EXISTS idx_allergy_fts ON cdr.c_allergy USING gin(fts);

-- c_clinical_note: 搜索 title, content, author
ALTER TABLE cdr.c_clinical_note ADD COLUMN IF NOT EXISTS fts tsvector
  GENERATED ALWAYS AS (to_tsvector('zh',
    coalesce(title,'') || ' ' || coalesce(content,'') || ' ' || coalesce(author,''))) STORED;
CREATE INDEX IF NOT EXISTS idx_clinical_note_fts ON cdr.c_clinical_note USING gin(fts);

-- 3. RDR 表 tsvector 生成列 + GIN 索引

-- r_study_project: 搜索 project_name, description
ALTER TABLE rdr.r_study_project ADD COLUMN IF NOT EXISTS fts tsvector
  GENERATED ALWAYS AS (to_tsvector('zh',
    coalesce(project_name,'') || ' ' || coalesce(description,''))) STORED;
CREATE INDEX IF NOT EXISTS idx_project_fts ON rdr.r_study_project USING gin(fts);

-- r_dataset: 搜索 dataset_name, description
ALTER TABLE rdr.r_dataset ADD COLUMN IF NOT EXISTS fts tsvector
  GENERATED ALWAYS AS (to_tsvector('zh',
    coalesce(dataset_name,'') || ' ' || coalesce(description,''))) STORED;
CREATE INDEX IF NOT EXISTS idx_dataset_fts ON rdr.r_dataset USING gin(fts);
```

- [ ] **Step 2: 执行脚本**

```bash
docker exec -i maidc-postgres psql -U maidc -d maidc < docker/init-db/14-smart-search-fts.sql
```

Expected: 所有 ALTER TABLE 和 CREATE INDEX 成功，无 ERROR

- [ ] **Step 3: 验证 FTS 索引**

```bash
docker exec maidc-postgres psql -U maidc -d maidc -c "SELECT tablename, indexname FROM pg_indexes WHERE indexname LIKE '%_fts' ORDER BY tablename;"
```

Expected: 13 行结果，每行一个 GIN 索引

- [ ] **Step 4: 功能验证 — 测试全文检索**

```bash
docker exec maidc-postgres psql -U maidc -d maidc -c "SELECT name, ts_rank_cd(fts, query) AS score FROM cdr.c_patient, plainto_tsquery('zh', 'Zhang') query WHERE fts @@ query AND is_deleted = false ORDER BY score DESC;"
```

Expected: 返回 Zhang San 的记录

- [ ] **Step 5: 提交**

```bash
git add docker/init-db/14-smart-search-fts.sql
git commit -m "feat(search): add FTS tsvector columns and GIN indexes for 13 tables"
```
