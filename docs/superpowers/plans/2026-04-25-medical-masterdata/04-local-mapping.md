# 04: 本地编码映射 (Institution / LocalConcept)

> **前置:** 01-ddl-core 完成
> **产出:** 机构管理 + 本地编码 CRUD + 翻译 API + 自动匹配 API

---

### Task 1: 追加 DDL (2张表)

**Files:**
- Modify: `docker/init-db/17-masterdata.sql`

- [ ] **Step 1: 在种子数据之前追加**

```sql
-- 8. 医疗机构注册
CREATE TABLE masterdata.m_institution (
    id              BIGSERIAL    PRIMARY KEY,
    inst_code       VARCHAR(32)  NOT NULL,
    name            VARCHAR(128) NOT NULL,
    short_name      VARCHAR(64),
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_by      VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_inst_code UNIQUE (inst_code)
);
COMMENT ON TABLE masterdata.m_institution IS '医疗机构注册';

-- 9. 院内本地编码
CREATE TABLE masterdata.m_local_concept (
    id                    BIGSERIAL    PRIMARY KEY,
    institution_id        BIGINT       NOT NULL,
    code_system_id        BIGINT       NOT NULL,
    local_code            VARCHAR(64)  NOT NULL,
    local_name            VARCHAR(512) NOT NULL,
    standard_concept_id   BIGINT,
    mapping_confidence    DECIMAL(3,2),
    mapping_status        VARCHAR(16)  NOT NULL DEFAULT 'UNMAPPED',
    mapped_by             VARCHAR(64),
    mapped_at             TIMESTAMP,
    created_by            VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by            VARCHAR(64),
    updated_at            TIMESTAMP,
    is_deleted            BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id                BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_local_concept UNIQUE (institution_id, code_system_id, local_code)
);
COMMENT ON TABLE masterdata.m_local_concept IS '院内本地编码映射';

-- 索引
CREATE INDEX idx_local_inst ON masterdata.m_local_concept(institution_id, code_system_id);
CREATE INDEX idx_local_translate ON masterdata.m_local_concept(institution_id, code_system_id, local_code);
CREATE INDEX idx_local_status ON masterdata.m_local_concept(mapping_status);
CREATE INDEX idx_local_standard ON masterdata.m_local_concept(standard_concept_id);
```

- [ ] **Step 2: Commit**

```bash
git add docker/init-db/17-masterdata.sql
git commit -m "feat(masterdata): add DDL for institution and local_concept tables"
```

---

### Task 2: Institution 全栈 (Entity → Controller)

**Files:**
- Create: `entity/InstitutionEntity.java`
- Create: `repository/InstitutionRepository.java`
- Create: `service/InstitutionService.java`
- Create: `controller/InstitutionController.java`

- [ ] **Step 1: Entity**

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
@Table(name = "m_institution", schema = "masterdata")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE masterdata.m_institution SET is_deleted = true WHERE id = ?")
public class InstitutionEntity extends BaseEntity {

    @Column(name = "inst_code", nullable = false, length = 32)
    private String instCode;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "short_name", length = 64)
    private String shortName;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "ACTIVE";
}
```

- [ ] **Step 2: Repository**

```java
package com.maidc.data.repository;

import com.maidc.data.entity.InstitutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface InstitutionRepository extends JpaRepository<InstitutionEntity, Long> {
    Optional<InstitutionEntity> findByInstCodeAndIsDeletedFalse(String instCode);
}
```

- [ ] **Step 3: Service**

```java
package com.maidc.data.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.InstitutionEntity;
import com.maidc.data.repository.InstitutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InstitutionService {

    private final InstitutionRepository institutionRepository;

    public List<InstitutionEntity> list() {
        return institutionRepository.findAll();
    }

    public InstitutionEntity getById(Long id) {
        return institutionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND));
    }

    @Transactional
    public InstitutionEntity create(InstitutionEntity entity) {
        return institutionRepository.save(entity);
    }

    @Transactional
    public InstitutionEntity update(Long id, InstitutionEntity entity) {
        InstitutionEntity existing = getById(id);
        existing.setName(entity.getName());
        existing.setShortName(entity.getShortName());
        existing.setStatus(entity.getStatus());
        return institutionRepository.save(existing);
    }
}
```

- [ ] **Step 4: Controller**

```java
package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.InstitutionEntity;
import com.maidc.data.service.InstitutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/masterdata/institutions")
@RequiredArgsConstructor
public class InstitutionController {

    private final InstitutionService institutionService;

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping
    public R<List<InstitutionEntity>> list() {
        return R.ok(institutionService.list());
    }

    @PreAuthorize("hasPermission('masterdata:create')")
    @PostMapping
    public R<InstitutionEntity> create(@RequestBody InstitutionEntity entity) {
        return R.ok(institutionService.create(entity));
    }

    @PreAuthorize("hasPermission('masterdata:create')")
    @PutMapping("/{id}")
    public R<InstitutionEntity> update(@PathVariable Long id, @RequestBody InstitutionEntity entity) {
        return R.ok(institutionService.update(id, entity));
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/{entity/InstitutionEntity.java,repository/InstitutionRepository.java,service/InstitutionService.java,controller/InstitutionController.java}
git commit -m "feat(masterdata): add institution CRUD"
```

---

### Task 3: LocalConcept 全栈 (Entity → Controller)

**Files:**
- Create: `entity/LocalConceptEntity.java`
- Create: `repository/LocalConceptRepository.java`
- Create: `service/LocalConceptService.java`
- Create: `controller/LocalConceptController.java`

- [ ] **Step 1: Entity**

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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "m_local_concept", schema = "masterdata")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE masterdata.m_local_concept SET is_deleted = true WHERE id = ?")
public class LocalConceptEntity extends BaseEntity {

    @Column(name = "institution_id", nullable = false)
    private Long institutionId;

    @Column(name = "code_system_id", nullable = false)
    private Long codeSystemId;

    @Column(name = "local_code", nullable = false, length = 64)
    private String localCode;

    @Column(name = "local_name", nullable = false, length = 512)
    private String localName;

    @Column(name = "standard_concept_id")
    private Long standardConceptId;

    @Column(name = "mapping_confidence", precision = 3, scale = 2)
    private BigDecimal mappingConfidence;

    @Column(name = "mapping_status", nullable = false, length = 16)
    private String mappingStatus = "UNMAPPED";

    @Column(name = "mapped_by", length = 64)
    private String mappedBy;

    @Column(name = "mapped_at")
    private LocalDateTime mappedAt;
}
```

- [ ] **Step 2: Repository**

```java
package com.maidc.data.repository;

import com.maidc.data.entity.LocalConceptEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocalConceptRepository extends JpaRepository<LocalConceptEntity, Long> {

    Optional<LocalConceptEntity> findByInstitutionIdAndCodeSystemIdAndLocalCodeAndIsDeletedFalse(
            Long institutionId, Long codeSystemId, String localCode);

    Page<LocalConceptEntity> findByInstitutionIdAndCodeSystemIdAndIsDeletedFalse(
            Long institutionId, Long codeSystemId, Pageable pageable);

    Page<LocalConceptEntity> findByInstitutionIdAndCodeSystemIdAndMappingStatusAndIsDeletedFalse(
            Long institutionId, Long codeSystemId, String status, Pageable pageable);

    @Query("SELECT lc FROM LocalConceptEntity lc WHERE lc.isDeleted = false " +
           "AND lc.mappingStatus = 'UNMAPPED' " +
           "AND (:institutionId IS NULL OR lc.institutionId = :institutionId) " +
           "AND (:codeSystemId IS NULL OR lc.codeSystemId = :codeSystemId)")
    Page<LocalConceptEntity> findUnmapped(@Param("institutionId") Long institutionId,
                                            @Param("codeSystemId") Long codeSystemId,
                                            Pageable pageable);

    @Query("SELECT COUNT(lc) FROM LocalConceptEntity lc WHERE lc.isDeleted = false " +
           "AND lc.institutionId = :instId AND lc.codeSystemId = :sysId " +
           "AND lc.mappingStatus = :status")
    long countByStatus(@Param("instId") Long instId, @Param("sysId") Long sysId, @Param("status") String status);
}
```

- [ ] **Step 3: Service**

```java
package com.maidc.data.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.ConceptEntity;
import com.maidc.data.entity.LocalConceptEntity;
import com.maidc.data.repository.ConceptRepository;
import com.maidc.data.repository.LocalConceptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalConceptService {

    private final LocalConceptRepository localConceptRepository;
    private final ConceptRepository conceptRepository;

    public Page<LocalConceptEntity> list(Long institutionId, Long codeSystemId, String mappingStatus, int page, int size) {
        if (mappingStatus != null && !mappingStatus.isBlank()) {
            return localConceptRepository.findByInstitutionIdAndCodeSystemIdAndMappingStatusAndIsDeletedFalse(
                    institutionId, codeSystemId, mappingStatus, PageRequest.of(page - 1, size));
        }
        return localConceptRepository.findByInstitutionIdAndCodeSystemIdAndIsDeletedFalse(
                institutionId, codeSystemId, PageRequest.of(page - 1, size));
    }

    public Page<LocalConceptEntity> getUnmapped(Long institutionId, Long codeSystemId, int page, int size) {
        return localConceptRepository.findUnmapped(institutionId, codeSystemId, PageRequest.of(page - 1, size));
    }

    public Map<String, Object> translate(String instCode, String codeSystem, String localCode) {
        // 需要先查 institution id 和 code_system id，再查 local_concept
        // 简化实现：由 Controller 传入 ID
        throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
    }

    public Map<String, Long> getStats(Long institutionId, Long codeSystemId) {
        Map<String, Long> stats = new HashMap<>();
        stats.put("CONFIRMED", localConceptRepository.countByStatus(institutionId, codeSystemId, "CONFIRMED"));
        stats.put("AUTO", localConceptRepository.countByStatus(institutionId, codeSystemId, "AUTO"));
        stats.put("SUSPECTED", localConceptRepository.countByStatus(institutionId, codeSystemId, "SUSPECTED"));
        stats.put("UNMAPPED", localConceptRepository.countByStatus(institutionId, codeSystemId, "UNMAPPED"));
        return stats;
    }

    @Transactional
    public LocalConceptEntity create(LocalConceptEntity entity) {
        return localConceptRepository.save(entity);
    }

    @Transactional
    public List<LocalConceptEntity> batchCreate(List<LocalConceptEntity> entities) {
        return localConceptRepository.saveAll(entities);
    }

    @Transactional
    public LocalConceptEntity update(Long id, LocalConceptEntity entity) {
        LocalConceptEntity existing = localConceptRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND));
        existing.setLocalName(entity.getLocalName());
        existing.setStandardConceptId(entity.getStandardConceptId());
        existing.setMappingConfidence(entity.getMappingConfidence());
        existing.setMappingStatus(entity.getMappingStatus());
        existing.setMappedBy(entity.getMappedBy());
        existing.setMappedAt(LocalDateTime.now());
        return localConceptRepository.save(existing);
    }

    /**
     * 基于 translate 接口：institutionId + codeSystemId + localCode → 标准概念
     */
    public Map<String, Object> translateById(Long institutionId, Long codeSystemId, String localCode) {
        LocalConceptEntity lc = localConceptRepository
                .findByInstitutionIdAndCodeSystemIdAndLocalCodeAndIsDeletedFalse(institutionId, codeSystemId, localCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND));

        Map<String, Object> result = new HashMap<>();
        result.put("localCode", lc.getLocalCode());
        result.put("localName", lc.getLocalName());
        result.put("confidence", lc.getMappingConfidence());
        result.put("mappingStatus", lc.getMappingStatus());

        if (lc.getStandardConceptId() != null) {
            ConceptEntity concept = conceptRepository.findByIdAndIsDeletedFalse(lc.getStandardConceptId()).orElse(null);
            if (concept != null) {
                Map<String, Object> standard = new HashMap<>();
                standard.put("id", concept.getId());
                standard.put("conceptCode", concept.getConceptCode());
                standard.put("name", concept.getName());
                result.put("standardConcept", standard);
            }
        }
        return result;
    }
}
```

- [ ] **Step 4: Controller**

```java
package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.LocalConceptEntity;
import com.maidc.data.service.LocalConceptService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/masterdata/local-concepts")
@RequiredArgsConstructor
public class LocalConceptController {

    private final LocalConceptService localConceptService;

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping
    public R<Page<LocalConceptEntity>> list(
            @RequestParam Long institutionId,
            @RequestParam Long codeSystemId,
            @RequestParam(required = false) String mappingStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "20") int size) {
        return R.ok(localConceptService.list(institutionId, codeSystemId, mappingStatus, page, size));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/unmapped")
    public R<Page<LocalConceptEntity>> unmapped(
            @RequestParam(required = false) Long institutionId,
            @RequestParam(required = false) Long codeSystemId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "20") int size) {
        return R.ok(localConceptService.getUnmapped(institutionId, codeSystemId, page, size));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/translate")
    public R<Map<String, Object>> translate(
            @RequestParam Long institutionId,
            @RequestParam Long codeSystemId,
            @RequestParam String localCode) {
        return R.ok(localConceptService.translateById(institutionId, codeSystemId, localCode));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/stats")
    public R<Map<String, Long>> stats(
            @RequestParam Long institutionId,
            @RequestParam Long codeSystemId) {
        return R.ok(localConceptService.getStats(institutionId, codeSystemId));
    }

    @PreAuthorize("hasPermission('masterdata:create')")
    @PostMapping
    public R<LocalConceptEntity> create(@RequestBody LocalConceptEntity entity) {
        return R.ok(localConceptService.create(entity));
    }

    @PreAuthorize("hasPermission('masterdata:create')")
    @PostMapping("/batch")
    public R<List<LocalConceptEntity>> batch(@RequestBody List<LocalConceptEntity> entities) {
        return R.ok(localConceptService.batchCreate(entities));
    }

    @PreAuthorize("hasPermission('masterdata:create')")
    @PutMapping("/{id}")
    public R<LocalConceptEntity> update(@PathVariable Long id, @RequestBody LocalConceptEntity entity) {
        return R.ok(localConceptService.update(id, entity));
    }
}
```

- [ ] **Step 5: 编译 + Commit**

```bash
cd maidc-parent && mvn compile -pl maidc-data -am -q
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/{entity/LocalConceptEntity.java,repository/LocalConceptRepository.java,service/LocalConceptService.java,controller/LocalConceptController.java}
git commit -m "feat(masterdata): add local concept mapping with translate and stats APIs"
```
