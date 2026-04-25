# 01: DDL + 核心CRUD (CodeSystem / Concept)

> **前置:** 无
> **产出:** masterdata schema + CodeSystem/Concept 全栈 CRUD API

---

### Task 1: 创建 masterdata schema + DDL

**Files:**
- Create: `docker/init-db/17-masterdata.sql`

- [ ] **Step 1: 写 DDL 脚本**

```sql
-- 17-masterdata.sql — 医疗主数据 schema

CREATE SCHEMA IF NOT EXISTS masterdata;
COMMENT ON SCHEMA masterdata IS '医疗主数据（编码体系/标准概念/映射/临床规则）';

GRANT USAGE ON SCHEMA masterdata TO maidc;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA masterdata TO maidc;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA masterdata TO maidc;
ALTER DEFAULT PRIVILEGES IN SCHEMA masterdata GRANT ALL PRIVILEGES ON TABLES TO maidc;
ALTER DEFAULT PRIVILEGES IN SCHEMA masterdata GRANT ALL PRIVILEGES ON SEQUENCES TO maidc;

-- 1. 编码体系注册表
CREATE TABLE masterdata.m_code_system (
    id                BIGSERIAL    PRIMARY KEY,
    code              VARCHAR(32)  NOT NULL,
    name              VARCHAR(128) NOT NULL,
    version           VARCHAR(32),
    description       TEXT,
    hierarchy_support BOOLEAN      NOT NULL DEFAULT FALSE,
    status            VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_by        VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by        VARCHAR(64),
    updated_at        TIMESTAMP,
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_code_system_code UNIQUE (code)
);
COMMENT ON TABLE masterdata.m_code_system IS '编码体系注册表';

-- 2. 统一标准概念表
CREATE TABLE masterdata.m_concept (
    id                BIGSERIAL    PRIMARY KEY,
    concept_code      VARCHAR(64)  NOT NULL,
    code_system_id    BIGINT       NOT NULL,
    name              VARCHAR(512) NOT NULL,
    name_en           VARCHAR(512),
    domain            VARCHAR(64),
    standard_class    VARCHAR(64),
    properties        JSONB,
    status            VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    valid_from        DATE,
    valid_to          DATE,
    created_by        VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by        VARCHAR(64),
    updated_at        TIMESTAMP,
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_concept_code_system UNIQUE (concept_code, code_system_id)
);
COMMENT ON TABLE masterdata.m_concept IS '统一标准概念表';

-- 索引
CREATE INDEX idx_concept_system_code ON masterdata.m_concept(code_system_id, concept_code);
CREATE INDEX idx_concept_domain ON masterdata.m_concept(domain) WHERE status = 'ACTIVE' AND is_deleted = false;
CREATE INDEX idx_concept_properties ON masterdata.m_concept USING gin(properties);
```

- [ ] **Step 2: 在 01-schemas.sql 末尾追加 schema 创建**

在 `docker/init-db/01-schemas.sql` 末尾追加 `CREATE SCHEMA IF NOT EXISTS masterdata;` 和对应 GRANT。

- [ ] **Step 3: 重启数据库验证表已创建**

```bash
docker exec -it maidc-postgres psql -U maidc -d maidc -c "\dt masterdata.*"
```
Expected: 显示 m_code_system, m_concept 两张表。

- [ ] **Step 4: Commit**

```bash
git add docker/init-db/17-masterdata.sql docker/init-db/01-schemas.sql
git commit -m "feat(masterdata): add schema and core DDL for code_system and concept tables"
```

---

### Task 2: CodeSystem Entity + Repository

**Files:**
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/entity/CodeSystemEntity.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/repository/CodeSystemRepository.java`

- [ ] **Step 1: 写 Entity**

```java
package com.maidc.data.entity;

import com.maidc.common.jpa.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "m_code_system", schema = "masterdata")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE masterdata.m_code_system SET is_deleted = true WHERE id = ?")
public class CodeSystemEntity extends BaseEntity {

    @Column(name = "code", nullable = false, length = 32)
    private String code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "version", length = 32)
    private String version;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "hierarchy_support", nullable = false)
    private Boolean hierarchySupport = false;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "ACTIVE";
}
```

- [ ] **Step 2: 写 Repository**

```java
package com.maidc.data.repository;

import com.maidc.data.entity.CodeSystemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CodeSystemRepository extends JpaRepository<CodeSystemEntity, Long>, JpaSpecificationExecutor<CodeSystemEntity> {

    Optional<CodeSystemEntity> findByCodeAndIsDeletedFalse(String code);

    boolean existsByCodeAndIsDeletedFalse(String code);
}
```

- [ ] **Step 3: Commit**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/entity/CodeSystemEntity.java \
        maidc-parent/maidc-data/src/main/java/com/maidc/data/repository/CodeSystemRepository.java
git commit -m "feat(masterdata): add CodeSystem entity and repository"
```

---

### Task 3: Concept Entity + Repository

**Files:**
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/entity/ConceptEntity.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/repository/ConceptRepository.java`

- [ ] **Step 1: 写 Entity**

```java
package com.maidc.data.entity;

import com.maidc.common.jpa.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "m_concept", schema = "masterdata")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE masterdata.m_concept SET is_deleted = true WHERE id = ?")
public class ConceptEntity extends BaseEntity {

    @Column(name = "concept_code", nullable = false, length = 64)
    private String conceptCode;

    @Column(name = "code_system_id", nullable = false)
    private Long codeSystemId;

    @Column(name = "name", nullable = false, length = 512)
    private String name;

    @Column(name = "name_en", length = 512)
    private String nameEn;

    @Column(name = "domain", length = 64)
    private String domain;

    @Column(name = "standard_class", length = 64)
    private String standardClass;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "properties", columnDefinition = "jsonb")
    private Map<String, Object> properties;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "ACTIVE";

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;
}
```

- [ ] **Step 2: 写 Repository**

```java
package com.maidc.data.repository;

import com.maidc.data.entity.ConceptEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConceptRepository extends JpaRepository<ConceptEntity, Long>, JpaSpecificationExecutor<ConceptEntity> {

    Optional<ConceptEntity> findByIdAndIsDeletedFalse(Long id);

    Page<ConceptEntity> findByCodeSystemIdAndIsDeletedFalse(Long codeSystemId, Pageable pageable);

    @Query("SELECT c FROM ConceptEntity c WHERE c.isDeleted = false AND c.status = 'ACTIVE' " +
           "AND c.codeSystemId = :systemId AND c.conceptCode = :code")
    Optional<ConceptEntity> findBySystemAndCode(@Param("systemId") Long systemId, @Param("code") String code);

    @Query("SELECT c FROM ConceptEntity c WHERE c.isDeleted = false " +
           "AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(c.nameEn) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR c.conceptCode LIKE CONCAT('%', :keyword, '%'))")
    Page<ConceptEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
```

- [ ] **Step 3: Commit**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/entity/ConceptEntity.java \
        maidc-parent/maidc-data/src/main/java/com/maidc/data/repository/ConceptRepository.java
git commit -m "feat(masterdata): add Concept entity and repository with search"
```

---

### Task 4: CodeSystemService + Controller

**Files:**
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/service/CodeSystemService.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/controller/CodeSystemController.java`

- [ ] **Step 1: 写 Service**

```java
package com.maidc.data.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.CodeSystemEntity;
import com.maidc.data.repository.CodeSystemRepository;
import com.maidc.data.repository.ConceptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeSystemService {

    private final CodeSystemRepository codeSystemRepository;
    private final ConceptRepository conceptRepository;

    public List<CodeSystemEntity> listAll() {
        return codeSystemRepository.findAll();
    }

    public CodeSystemEntity getById(Long id) {
        return codeSystemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND));
    }

    public Map<String, Object> getStats(Long id) {
        CodeSystemEntity cs = getById(id);
        long conceptCount = conceptRepository.findByCodeSystemIdAndIsDeletedFalse(id, org.springframework.data.domain.Pageable.unpaged()).getTotalElements();
        Map<String, Object> stats = new HashMap<>();
        stats.put("codeSystem", cs);
        stats.put("conceptCount", conceptCount);
        return stats;
    }

    @Transactional
    public CodeSystemEntity create(CodeSystemEntity entity) {
        if (codeSystemRepository.existsByCodeAndIsDeletedFalse(entity.getCode())) {
            throw new BusinessException(ErrorCode.MODEL_CODE_DUPLICATE);
        }
        return codeSystemRepository.save(entity);
    }

    @Transactional
    public CodeSystemEntity update(Long id, CodeSystemEntity entity) {
        CodeSystemEntity existing = getById(id);
        existing.setName(entity.getName());
        existing.setVersion(entity.getVersion());
        existing.setDescription(entity.getDescription());
        existing.setHierarchySupport(entity.getHierarchySupport());
        existing.setStatus(entity.getStatus());
        return codeSystemRepository.save(existing);
    }
}
```

- [ ] **Step 2: 写 Controller**

```java
package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.CodeSystemEntity;
import com.maidc.data.service.CodeSystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/masterdata/code-systems")
@RequiredArgsConstructor
public class CodeSystemController {

    private final CodeSystemService codeSystemService;

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping
    public R<List<CodeSystemEntity>> list() {
        return R.ok(codeSystemService.listAll());
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/{id}")
    public R<CodeSystemEntity> get(@PathVariable Long id) {
        return R.ok(codeSystemService.getById(id));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/{id}/stats")
    public R<Map<String, Object>> stats(@PathVariable Long id) {
        return R.ok(codeSystemService.getStats(id));
    }

    @PreAuthorize("hasPermission('masterdata:create')")
    @PostMapping
    public R<CodeSystemEntity> create(@RequestBody CodeSystemEntity entity) {
        return R.ok(codeSystemService.create(entity));
    }

    @PreAuthorize("hasPermission('masterdata:create')")
    @PutMapping("/{id}")
    public R<CodeSystemEntity> update(@PathVariable Long id, @RequestBody CodeSystemEntity entity) {
        return R.ok(codeSystemService.update(id, entity));
    }
}
```

- [ ] **Step 3: 启动验证**

```bash
cd maidc-parent && mvn compile -pl maidc-data -am -q
```
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/service/CodeSystemService.java \
        maidc-parent/maidc-data/src/main/java/com/maidc/data/controller/CodeSystemController.java
git commit -m "feat(masterdata): add CodeSystem service and CRUD controller"
```

---

### Task 5: ConceptService + Controller

**Files:**
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/service/ConceptService.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/controller/ConceptController.java`

- [ ] **Step 1: 写 Service**

```java
package com.maidc.data.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.ConceptEntity;
import com.maidc.data.repository.ConceptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConceptService {

    private final ConceptRepository conceptRepository;

    public Page<ConceptEntity> list(Long codeSystemId, String domain, String keyword, int page, int size) {
        Specification<ConceptEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));
            if (codeSystemId != null) {
                predicates.add(cb.equal(root.get("codeSystemId"), codeSystemId));
            }
            if (domain != null && !domain.isBlank()) {
                predicates.add(cb.equal(root.get("domain"), domain));
            }
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.or(
                        cb.like(root.get("conceptCode"), "%" + keyword + "%"),
                        cb.like(root.get("name"), "%" + keyword + "%"),
                        cb.like(root.get("nameEn"), "%" + keyword + "%")
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return conceptRepository.findAll(spec, PageRequest.of(page - 1, size));
    }

    public ConceptEntity getById(Long id) {
        return conceptRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND));
    }

    public Page<ConceptEntity> search(String keyword, Long codeSystemId, int page, int size) {
        if (keyword == null || keyword.isBlank()) {
            return list(codeSystemId, null, null, page, size);
        }
        return conceptRepository.searchByKeyword(keyword, PageRequest.of(page - 1, size));
    }

    @Transactional
    public ConceptEntity create(ConceptEntity entity) {
        return conceptRepository.save(entity);
    }

    @Transactional
    public ConceptEntity update(Long id, ConceptEntity entity) {
        ConceptEntity existing = getById(id);
        existing.setConceptCode(entity.getConceptCode());
        existing.setName(entity.getName());
        existing.setNameEn(entity.getNameEn());
        existing.setDomain(entity.getDomain());
        existing.setStandardClass(entity.getStandardClass());
        existing.setProperties(entity.getProperties());
        existing.setValidFrom(entity.getValidFrom());
        existing.setValidTo(entity.getValidTo());
        return conceptRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        ConceptEntity existing = getById(id);
        existing.setStatus("RETIRED");
        conceptRepository.save(existing);
    }
}
```

- [ ] **Step 2: 写 Controller**

```java
package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.ConceptEntity;
import com.maidc.data.service.ConceptService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/masterdata/concepts")
@RequiredArgsConstructor
public class ConceptController {

    private final ConceptService conceptService;

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping
    public R<Page<ConceptEntity>> list(
            @RequestParam(required = false) Long codeSystemId,
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "20") int size) {
        return R.ok(conceptService.list(codeSystemId, domain, keyword, page, size));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/{id}")
    public R<ConceptEntity> get(@PathVariable Long id) {
        return R.ok(conceptService.getById(id));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/search")
    public R<Page<ConceptEntity>> search(
            @RequestParam String keyword,
            @RequestParam(required = false) Long codeSystemId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "20") int size) {
        return R.ok(conceptService.search(keyword, codeSystemId, page, size));
    }

    @PreAuthorize("hasPermission('masterdata:create')")
    @PostMapping
    public R<ConceptEntity> create(@RequestBody ConceptEntity entity) {
        return R.ok(conceptService.create(entity));
    }

    @PreAuthorize("hasPermission('masterdata:create')")
    @PutMapping("/{id}")
    public R<ConceptEntity> update(@PathVariable Long id, @RequestBody ConceptEntity entity) {
        return R.ok(conceptService.update(id, entity));
    }

    @PreAuthorize("hasPermission('masterdata:create')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        conceptService.delete(id);
        return R.ok();
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
cd maidc-parent && mvn compile -pl maidc-data -am -q
```
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/service/ConceptService.java \
        maidc-parent/maidc-data/src/main/java/com/maidc/data/controller/ConceptController.java
git commit -m "feat(masterdata): add Concept service, search and CRUD controller"
```

---

### Task 6: 编码体系初始数据

**Files:**
- Modify: `docker/init-db/17-masterdata.sql` (追加)

- [ ] **Step 1: 在 17-masterdata.sql 末尾追加种子数据**

```sql
-- 初始编码体系
INSERT INTO masterdata.m_code_system (code, name, version, description, hierarchy_support, status) VALUES
('ICD10', 'ICD-10 国际疾病分类', '2024', 'WHO国际疾病分类第10次修订本中文版', true, 'ACTIVE'),
('ICD9CM', 'ICD-9-CM-3 手术编码', '2011', '国际疾病分类临床修订本第3卷手术与操作', true, 'ACTIVE'),
('LOINC', 'LOINC 检验观察编码', '2.78', 'Logical Observation Identifiers Names and Codes', false, 'ACTIVE'),
('SNOMEDCT', 'SNOMED CT 临床术语', '2024-01', 'Systematized Nomenclature of Medicine Clinical Terms', true, 'ACTIVE'),
('ATC', 'ATC 药品分类', '2024', 'Anatomical Therapeutic Chemical classification', true, 'ACTIVE');
```

- [ ] **Step 2: Commit**

```bash
git add docker/init-db/17-masterdata.sql
git commit -m "feat(masterdata): seed initial code systems (ICD10/ICD9CM/LOINC/SNOMEDCT/ATC)"
```
