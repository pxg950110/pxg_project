# 03: 临床规则 (ReferenceRange / DrugInteraction)

> **前置:** 01-ddl-core 完成
> **产出:** 参考范围 CRUD + 动态匹配 API，药物相互作用 CRUD + 处方审核 API

---

### Task 1: 追加 DDL (2张表)

**Files:**
- Modify: `docker/init-db/17-masterdata.sql`

- [ ] **Step 1: 在种子数据之前追加**

```sql
-- 6. 参考范围
CREATE TABLE masterdata.m_reference_range (
    id              BIGSERIAL    PRIMARY KEY,
    concept_id      BIGINT       NOT NULL,
    gender          VARCHAR(8)   NOT NULL DEFAULT 'ALL',
    age_min         DECIMAL,
    age_max         DECIMAL,
    range_low       DECIMAL,
    range_high      DECIMAL,
    unit            VARCHAR(32),
    critical_low    DECIMAL,
    critical_high   DECIMAL,
    source          VARCHAR(128),
    created_by      VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id          BIGINT       NOT NULL DEFAULT 0
);
COMMENT ON TABLE masterdata.m_reference_range IS '参考范围';

-- 7. 药物相互作用
CREATE TABLE masterdata.m_drug_interaction (
    id                    BIGSERIAL    PRIMARY KEY,
    drug_concept_id_1     BIGINT       NOT NULL,
    drug_concept_id_2     BIGINT       NOT NULL,
    severity              VARCHAR(16)  NOT NULL,
    interaction_type      VARCHAR(32),
    description           TEXT,
    evidence_level        VARCHAR(16),
    clinical_action       TEXT,
    created_by            VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by            VARCHAR(64),
    updated_at            TIMESTAMP,
    is_deleted            BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id                BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_drug_pair UNIQUE (drug_concept_id_1, drug_concept_id_2)
);
COMMENT ON TABLE masterdata.m_drug_interaction IS '药物相互作用';

-- 索引
CREATE INDEX idx_refrange_concept ON masterdata.m_reference_range(concept_id, gender);
CREATE INDEX idx_drug_int_pair ON masterdata.m_drug_interaction(drug_concept_id_1, drug_concept_id_2);
CREATE INDEX idx_drug_int_reverse ON masterdata.m_drug_interaction(drug_concept_id_2, drug_concept_id_1);
```

- [ ] **Step 2: Commit**

```bash
git add docker/init-db/17-masterdata.sql
git commit -m "feat(masterdata): add DDL for reference_range and drug_interaction tables"
```

---

### Task 2: ReferenceRange Entity + Repository + Service + Controller

**Files:**
- Create: `entity/ReferenceRangeEntity.java`
- Create: `repository/ReferenceRangeRepository.java`
- Create: `service/ReferenceRangeService.java`
- Create: `controller/ReferenceRangeController.java`

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

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "m_reference_range", schema = "masterdata")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE masterdata.m_reference_range SET is_deleted = true WHERE id = ?")
public class ReferenceRangeEntity extends BaseEntity {

    @Column(name = "concept_id", nullable = false)
    private Long conceptId;

    @Column(name = "gender", nullable = false, length = 8)
    private String gender = "ALL";

    @Column(name = "age_min", precision = 10, scale = 2)
    private BigDecimal ageMin;

    @Column(name = "age_max", precision = 10, scale = 2)
    private BigDecimal ageMax;

    @Column(name = "range_low", precision = 12, scale = 4)
    private BigDecimal rangeLow;

    @Column(name = "range_high", precision = 12, scale = 4)
    private BigDecimal rangeHigh;

    @Column(name = "unit", length = 32)
    private String unit;

    @Column(name = "critical_low", precision = 12, scale = 4)
    private BigDecimal criticalLow;

    @Column(name = "critical_high", precision = 12, scale = 4)
    private BigDecimal criticalHigh;

    @Column(name = "source", length = 128)
    private String source;
}
```

- [ ] **Step 2: Repository**

```java
package com.maidc.data.repository;

import com.maidc.data.entity.ReferenceRangeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReferenceRangeRepository extends JpaRepository<ReferenceRangeEntity, Long> {

    List<ReferenceRangeEntity> findByConceptIdAndIsDeletedFalse(Long conceptId);

    @Query("SELECT r FROM ReferenceRangeEntity r WHERE r.isDeleted = false " +
           "AND r.conceptId = :conceptId " +
           "AND (r.gender = :gender OR r.gender = 'ALL') " +
           "AND (r.ageMin IS NULL OR r.ageMin <= :age) " +
           "AND (r.ageMax IS NULL OR r.ageMax >= :age) " +
           "ORDER BY CASE WHEN r.gender = :gender AND r.ageMin IS NOT NULL THEN 0 " +
           "              WHEN r.gender = :gender THEN 1 " +
           "              ELSE 2 END")
    List<ReferenceRangeEntity> findBestMatch(@Param("conceptId") Long conceptId,
                                              @Param("gender") String gender,
                                              @Param("age") BigDecimal age);
}
```

- [ ] **Step 3: Service**

```java
package com.maidc.data.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.ReferenceRangeEntity;
import com.maidc.data.repository.ReferenceRangeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferenceRangeService {

    private final ReferenceRangeRepository referenceRangeRepository;

    public List<ReferenceRangeEntity> list(Long conceptId, String gender) {
        if (conceptId != null) {
            return referenceRangeRepository.findByConceptIdAndIsDeletedFalse(conceptId);
        }
        return referenceRangeRepository.findAll();
    }

    public ReferenceRangeEntity getById(Long id) {
        return referenceRangeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND));
    }

    public ReferenceRangeEntity evaluate(Long conceptId, String gender, BigDecimal age) {
        List<ReferenceRangeEntity> matches = referenceRangeRepository.findBestMatch(conceptId, gender, age);
        return matches.isEmpty() ? null : matches.get(0);
    }

    @Transactional
    public ReferenceRangeEntity create(ReferenceRangeEntity entity) {
        return referenceRangeRepository.save(entity);
    }

    @Transactional
    public ReferenceRangeEntity update(Long id, ReferenceRangeEntity entity) {
        ReferenceRangeEntity existing = getById(id);
        existing.setGender(entity.getGender());
        existing.setAgeMin(entity.getAgeMin());
        existing.setAgeMax(entity.getAgeMax());
        existing.setRangeLow(entity.getRangeLow());
        existing.setRangeHigh(entity.getRangeHigh());
        existing.setUnit(entity.getUnit());
        existing.setCriticalLow(entity.getCriticalLow());
        existing.setCriticalHigh(entity.getCriticalHigh());
        existing.setSource(entity.getSource());
        return referenceRangeRepository.save(existing);
    }
}
```

- [ ] **Step 4: Controller**

```java
package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.ReferenceRangeEntity;
import com.maidc.data.service.ReferenceRangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/masterdata/reference-ranges")
@RequiredArgsConstructor
public class ReferenceRangeController {

    private final ReferenceRangeService referenceRangeService;

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping
    public R<List<ReferenceRangeEntity>> list(
            @RequestParam(required = false) Long conceptId,
            @RequestParam(required = false) String gender) {
        return R.ok(referenceRangeService.list(conceptId, gender));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/{id}")
    public R<ReferenceRangeEntity> get(@PathVariable Long id) {
        return R.ok(referenceRangeService.getById(id));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/evaluate")
    public R<ReferenceRangeEntity> evaluate(
            @RequestParam Long conceptId,
            @RequestParam String gender,
            @RequestParam BigDecimal age) {
        return R.ok(referenceRangeService.evaluate(conceptId, gender, age));
    }

    @PreAuthorize("hasPermission('masterdata:create')")
    @PostMapping
    public R<ReferenceRangeEntity> create(@RequestBody ReferenceRangeEntity entity) {
        return R.ok(referenceRangeService.create(entity));
    }

    @PreAuthorize("hasPermission('masterdata:create')")
    @PutMapping("/{id}")
    public R<ReferenceRangeEntity> update(@PathVariable Long id, @RequestBody ReferenceRangeEntity entity) {
        return R.ok(referenceRangeService.update(id, entity));
    }
}
```

- [ ] **Step 5: 编译 + Commit**

```bash
cd maidc-parent && mvn compile -pl maidc-data -am -q
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/{entity/ReferenceRangeEntity.java,repository/ReferenceRangeRepository.java,service/ReferenceRangeService.java,controller/ReferenceRangeController.java}
git commit -m "feat(masterdata): add reference range with evaluate API"
```

---

### Task 3: DrugInteraction Entity + Repository + Service + Controller

**Files:**
- Create: `entity/DrugInteractionEntity.java`
- Create: `repository/DrugInteractionRepository.java`
- Create: `service/DrugInteractionService.java`
- Create: `controller/DrugInteractionController.java`

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
@Table(name = "m_drug_interaction", schema = "masterdata")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE masterdata.m_drug_interaction SET is_deleted = true WHERE id = ?")
public class DrugInteractionEntity extends BaseEntity {

    @Column(name = "drug_concept_id_1", nullable = false)
    private Long drugConceptId1;

    @Column(name = "drug_concept_id_2", nullable = false)
    private Long drugConceptId2;

    @Column(name = "severity", nullable = false, length = 16)
    private String severity;

    @Column(name = "interaction_type", length = 32)
    private String interactionType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "evidence_level", length = 16)
    private String evidenceLevel;

    @Column(name = "clinical_action", columnDefinition = "TEXT")
    private String clinicalAction;
}
```

- [ ] **Step 2: Repository**

```java
package com.maidc.data.repository;

import com.maidc.data.entity.DrugInteractionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DrugInteractionRepository extends JpaRepository<DrugInteractionEntity, Long> {

    @Query("SELECT d FROM DrugInteractionEntity d WHERE d.isDeleted = false " +
           "AND (d.drugConceptId1 = :drug1 AND d.drugConceptId2 = :drug2 " +
           "     OR d.drugConceptId1 = :drug2 AND d.drugConceptId2 = :drug1)")
    List<DrugInteractionEntity> findBetween(@Param("drug1") Long drug1, @Param("drug2") Long drug2);

    @Query("SELECT d FROM DrugInteractionEntity d WHERE d.isDeleted = false " +
           "AND (d.drugConceptId1 IN :ids AND d.drugConceptId2 IN :ids) " +
           "ORDER BY CASE d.severity WHEN 'CONTRAINDICATED' THEN 0 " +
           "                          WHEN 'MAJOR' THEN 1 " +
           "                          WHEN 'MODERATE' THEN 2 " +
           "                          ELSE 3 END")
    List<DrugInteractionEntity> findInPairSet(@Param("ids") List<Long> ids);
}
```

- [ ] **Step 3: Service**

```java
package com.maidc.data.service;

import com.maidc.data.entity.DrugInteractionEntity;
import com.maidc.data.repository.DrugInteractionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrugInteractionService {

    private final DrugInteractionRepository drugInteractionRepository;

    public List<DrugInteractionEntity> list(Long drug1, Long drug2, String severity) {
        if (drug1 != null && drug2 != null) {
            return drugInteractionRepository.findBetween(drug1, drug2);
        }
        return drugInteractionRepository.findAll();
    }

    public List<DrugInteractionEntity> checkPair(Long drug1, Long drug2) {
        return drugInteractionRepository.findBetween(drug1, drug2);
    }

    public List<DrugInteractionEntity> checkList(List<Long> drugIds) {
        if (drugIds == null || drugIds.size() < 2) {
            return List.of();
        }
        return drugInteractionRepository.findInPairSet(drugIds);
    }

    @Transactional
    public DrugInteractionEntity create(DrugInteractionEntity entity) {
        Long id1 = entity.getDrugConceptId1();
        Long id2 = entity.getDrugConceptId2();
        if (id1 > id2) {
            entity.setDrugConceptId1(id2);
            entity.setDrugConceptId2(id1);
        }
        return drugInteractionRepository.save(entity);
    }
}
```

- [ ] **Step 4: Controller**

```java
package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.DrugInteractionEntity;
import com.maidc.data.service.DrugInteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/masterdata/drug-interactions")
@RequiredArgsConstructor
public class DrugInteractionController {

    private final DrugInteractionService drugInteractionService;

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping
    public R<List<DrugInteractionEntity>> list(
            @RequestParam(required = false) Long drug1,
            @RequestParam(required = false) Long drug2,
            @RequestParam(required = false) String severity) {
        return R.ok(drugInteractionService.list(drug1, drug2, severity));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/check")
    public R<List<DrugInteractionEntity>> check(
            @RequestParam Long drug1, @RequestParam Long drug2) {
        return R.ok(drugInteractionService.checkPair(drug1, drug2));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @PostMapping("/check-list")
    public R<List<DrugInteractionEntity>> checkList(@RequestBody List<Long> drugIds) {
        return R.ok(drugInteractionService.checkList(drugIds));
    }

    @PreAuthorize("hasPermission('masterdata:create')")
    @PostMapping
    public R<DrugInteractionEntity> create(@RequestBody DrugInteractionEntity entity) {
        return R.ok(drugInteractionService.create(entity));
    }
}
```

- [ ] **Step 5: 编译 + Commit**

```bash
cd maidc-parent && mvn compile -pl maidc-data -am -q
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/{entity/DrugInteractionEntity.java,repository/DrugInteractionRepository.java,service/DrugInteractionService.java,controller/DrugInteractionController.java}
git commit -m "feat(masterdata): add drug interaction with check and check-list APIs"
```
