# 01 — DDL + 种子数据

**Goal:** 创建专病管理所需的 3 张表并插入 16 条疾病模板种子数据。

**Files:**
- Create: `docker/init-db/15-disease-cohort.sql`

---

- [ ] **Step 1: 创建 SQL 文件**

```sql
-- ==================== 专病管理: 3 tables ====================

-- c_disease_cohort
CREATE TABLE IF NOT EXISTS cdr.c_disease_cohort (
    id                BIGSERIAL    PRIMARY KEY,
    name              VARCHAR(128) NOT NULL,
    description       TEXT,
    inclusion_rules   JSONB        NOT NULL,
    patient_count     INT          NOT NULL DEFAULT 0,
    auto_sync         BOOLEAN      NOT NULL DEFAULT TRUE,
    status            VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    last_sync_at      TIMESTAMP,
    created_by        VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by        VARCHAR(64),
    updated_at        TIMESTAMP,
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id            BIGINT       NOT NULL DEFAULT 0
);
COMMENT ON TABLE cdr.c_disease_cohort IS '专病库';

-- c_disease_cohort_patient
CREATE TABLE IF NOT EXISTS cdr.c_disease_cohort_patient (
    id                BIGSERIAL    PRIMARY KEY,
    cohort_id         BIGINT       NOT NULL,
    patient_id        BIGINT       NOT NULL,
    match_source      VARCHAR(16)  NOT NULL,
    matched_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_cohort_patient UNIQUE (cohort_id, patient_id)
);
COMMENT ON TABLE cdr.c_disease_cohort_patient IS '专病-患者关联';

CREATE INDEX idx_dcp_cohort ON cdr.c_disease_cohort_patient(cohort_id);
CREATE INDEX idx_dcp_patient ON cdr.c_disease_cohort_patient(patient_id);

-- s_disease_template
CREATE TABLE IF NOT EXISTS system.s_disease_template (
    id                  BIGSERIAL    PRIMARY KEY,
    disease_name        VARCHAR(128) NOT NULL,
    icd_codes          TEXT[],
    inclusion_template  JSONB        NOT NULL,
    description         TEXT,
    is_builtin          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by          VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id              BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_disease_name UNIQUE (org_id, disease_name)
);
COMMENT ON TABLE system.s_disease_template IS '疾病模板';
```

- [ ] **Step 2: 追加种子数据（16 条模板）**

在同一文件末尾追加 INSERT 语句，每条模板的 `inclusion_template` 使用分组 JSON 结构。示例：

```sql
INSERT INTO system.s_disease_template (disease_name, icd_codes, inclusion_template, description, is_builtin, created_by, org_id) VALUES
('2型糖尿病', ARRAY['E11%','E10%'],
 '{"groupLogic":"AND","groups":[{"domain":"DIAGNOSIS","logic":"OR","conditions":[{"field":"diagnosis_code","operator":"LIKE","value":"E11%"},{"field":"diagnosis_name","operator":"CONTAINS","value":"糖尿病"}]},{"domain":"LAB","logic":"AND","conditions":[{"field":"test_code","operator":"IN","value":["HBA1C","FBG"]}]}]}',
 '2型糖尿病专病库', true, 'system', 0),
('冠心病', ARRAY['I25%','I20%'],
 '{"groupLogic":"AND","groups":[{"domain":"DIAGNOSIS","logic":"OR","conditions":[{"field":"diagnosis_code","operator":"LIKE","value":"I25%"},{"field":"diagnosis_code","operator":"LIKE","value":"I20%"}]},{"domain":"SURGERY","logic":"OR","conditions":[{"field":"operation_name","operator":"CONTAINS","value":"PCI"},{"field":"operation_name","operator":"CONTAINS","value":"CABG"}]}]}',
 '冠心病专病库', true, 'system', 0),
('脑卒中', ARRAY['I63%','I61%','I60%'],
 '{"groupLogic":"AND","groups":[{"domain":"DIAGNOSIS","logic":"OR","conditions":[{"field":"diagnosis_code","operator":"LIKE","value":"I63%"},{"field":"diagnosis_code","operator":"LIKE","value":"I61%"},{"field":"diagnosis_code","operator":"LIKE","value":"I60%"}]}]}',
 '脑卒中专病库', true, 'system', 0),
('高血压', ARRAY['I10%','I11%'],
 '{"groupLogic":"AND","groups":[{"domain":"DIAGNOSIS","logic":"OR","conditions":[{"field":"diagnosis_code","operator":"LIKE","value":"I10%"},{"field":"diagnosis_code","operator":"LIKE","value":"I11%"}]}]}',
 '高血压专病库', true, 'system', 0),
('肺癌', ARRAY['C34%'],
 '{"groupLogic":"AND","groups":[{"domain":"DIAGNOSIS","logic":"OR","conditions":[{"field":"diagnosis_code","operator":"LIKE","value":"C34%"}]},{"domain":"PATHOLOGY","logic":"OR","conditions":[{"field":"diagnosis_desc","operator":"CONTAINS","value":"肺腺癌"},{"field":"diagnosis_desc","operator":"CONTAINS","value":"肺鳞癌"}]}]}',
 '肺癌专病库', true, 'system', 0),
('胃癌', ARRAY['C16%'],
 '{"groupLogic":"AND","groups":[{"domain":"DIAGNOSIS","logic":"OR","conditions":[{"field":"diagnosis_code","operator":"LIKE","value":"C16%"}]},{"domain":"PATHOLOGY","logic":"OR","conditions":[{"field":"diagnosis_desc","operator":"CONTAINS","value":"胃"}]}]}',
 '胃癌专病库', true, 'system', 0),
('结直肠癌', ARRAY['C18%','C20%'],
 '{"groupLogic":"AND","groups":[{"domain":"DIAGNOSIS","logic":"OR","conditions":[{"field":"diagnosis_code","operator":"LIKE","value":"C18%"},{"field":"diagnosis_code","operator":"LIKE","value":"C20%"}]},{"domain":"LAB","logic":"AND","conditions":[{"field":"test_code","operator":"IN","value":["CEA"]}]}]}',
 '结直肠癌专病库', true, 'system', 0),
('乳腺癌', ARRAY['C50%'],
 '{"groupLogic":"AND","groups":[{"domain":"DIAGNOSIS","logic":"OR","conditions":[{"field":"diagnosis_code","operator":"LIKE","value":"C50%"}]},{"domain":"PATHOLOGY","logic":"OR","conditions":[{"field":"diagnosis_desc","operator":"CONTAINS","value":"乳腺"}]}]}',
 '乳腺癌专病库', true, 'system', 0),
('慢性肾病', ARRAY['N18%'],
 '{"groupLogic":"AND","groups":[{"domain":"DIAGNOSIS","logic":"OR","conditions":[{"field":"diagnosis_code","operator":"LIKE","value":"N18%"}]},{"domain":"LAB","logic":"AND","conditions":[{"field":"test_code","operator":"IN","value":["CREA","eGFR"]}]}]}',
 '慢性肾病专病库', true, 'system', 0),
('肝硬化', ARRAY['K74%'],
 '{"groupLogic":"AND","groups":[{"domain":"DIAGNOSIS","logic":"OR","conditions":[{"field":"diagnosis_code","operator":"LIKE","value":"K74%"}]},{"domain":"LAB","logic":"AND","conditions":[{"field":"test_code","operator":"IN","value":["ALT","AST","TBIL"]}]}]}',
 '肝硬化专病库', true, 'system', 0),
('慢阻肺', ARRAY['J44%'],
 '{"groupLogic":"AND","groups":[{"domain":"DIAGNOSIS","logic":"OR","conditions":[{"field":"diagnosis_code","operator":"LIKE","value":"J44%"}]}]}',
 '慢阻肺专病库', true, 'system', 0),
('房颤', ARRAY['I48%'],
 '{"groupLogic":"AND","groups":[{"domain":"DIAGNOSIS","logic":"OR","conditions":[{"field":"diagnosis_code","operator":"LIKE","value":"I48%"}]}]}',
 '房颤专病库', true, 'system', 0),
('心力衰竭', ARRAY['I50%'],
 '{"groupLogic":"AND","groups":[{"domain":"DIAGNOSIS","logic":"OR","conditions":[{"field":"diagnosis_code","operator":"LIKE","value":"I50%"}]},{"domain":"LAB","logic":"AND","conditions":[{"field":"test_code","operator":"IN","value":["BNP","NT-proBNP"]}]}]}',
 '心力衰竭专病库', true, 'system', 0),
('肺炎', ARRAY['J18%'],
 '{"groupLogic":"AND","groups":[{"domain":"DIAGNOSIS","logic":"OR","conditions":[{"field":"diagnosis_code","operator":"LIKE","value":"J18%"}]},{"domain":"IMAGING","logic":"OR","conditions":[{"field":"exam_type","operator":"CONTAINS","value":"胸片"},{"field":"exam_type","operator":"CONTAINS","value":"CT"}]}]}',
 '肺炎专病库', true, 'system', 0),
('乙肝', ARRAY['B18%'],
 '{"groupLogic":"AND","groups":[{"domain":"DIAGNOSIS","logic":"OR","conditions":[{"field":"diagnosis_code","operator":"LIKE","value":"B18%"}]},{"domain":"LAB","logic":"AND","conditions":[{"field":"test_code","operator":"IN","value":["HBsAg","HBV-DNA"]}]}]}',
 '乙肝专病库', true, 'system', 0),
('1型糖尿病', ARRAY['E10%'],
 '{"groupLogic":"AND","groups":[{"domain":"DIAGNOSIS","logic":"OR","conditions":[{"field":"diagnosis_code","operator":"LIKE","value":"E10%"}]},{"domain":"LAB","logic":"AND","conditions":[{"field":"test_code","operator":"IN","value":["C肽","INS"]}]}]}',
 '1型糖尿病专病库', true, 'system', 0);
```

- [ ] **Step 3: 执行 SQL 验证**

```bash
docker exec -i maidc-postgres psql -U maidc -d maidc -f /docker-entrypoint-initdb.d/15-disease-cohort.sql
```

预期：3 张表创建成功，16 条模板插入成功。

- [ ] **Step 4: Commit**

```bash
git add docker/init-db/15-disease-cohort.sql
git commit -m "feat(disease): add DDL and seed data for disease cohort management"
```
