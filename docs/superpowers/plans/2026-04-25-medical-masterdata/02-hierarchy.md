# 02: 层级与映射 (Relationship / Ancestor / Synonym)

> **前置:** 01-ddl-core 完成
> **产出:** 概念关系、祖先闭包、同义词、跨编码映射 API

---

### Task 1: 追加 DDL (3张表)

**Files:**
- Modify: `docker/init-db/17-masterdata.sql`

- [ ] **Step 1: 追加 DDL**

在 17-masterdata.sql 的种子数据之前追加：

```sql
-- 3. 概念关系
CREATE TABLE masterdata.m_concept_relationship (
    id                  BIGSERIAL    PRIMARY KEY,
    concept_id_1        BIGINT       NOT NULL,
    concept_id_2        BIGINT       NOT NULL,
    relationship_type   VARCHAR(64)  NOT NULL,
    is_hierarchical     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by          VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    is_deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id              BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_concept_rel UNIQUE (concept_id_1, concept_id_2, relationship_type)
);
COMMENT ON TABLE masterdata.m_concept_relationship IS '概念关系';

-- 4. 祖先闭包
CREATE TABLE masterdata.m_concept_ancestor (
    id                          BIGSERIAL PRIMARY KEY,
    ancestor_concept_id         BIGINT    NOT NULL,
    descendant_concept_id       BIGINT    NOT NULL,
    min_levels_of_separation    INT       NOT NULL DEFAULT 0,
    max_levels_of_separation    INT       NOT NULL DEFAULT 0
);
COMMENT ON TABLE masterdata.m_concept_ancestor IS '概念祖先闭包表';

-- 5. 概念同义词
CREATE TABLE masterdata.m_concept_synonym (
    id              BIGSERIAL    PRIMARY KEY,
    concept_id      BIGINT       NOT NULL,
    synonym         VARCHAR(512) NOT NULL,
    language_code   VARCHAR(8)   NOT NULL DEFAULT 'zh',
    is_preferred    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by      VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id          BIGINT       NOT NULL DEFAULT 0
);
COMMENT ON TABLE masterdata.m_concept_synonym IS '概念同义词';

-- 索引
CREATE INDEX idx_rel_type ON masterdata.m_concept_relationship(concept_id_1, relationship_type);
CREATE INDEX idx_rel_reverse ON masterdata.m_concept_relationship(concept_id_2, relationship_type);
CREATE INDEX idx_ancestor_desc ON masterdata.m_concept_ancestor(descendant_concept_id);
CREATE INDEX idx_ancestor_asc ON masterdata.m_concept_ancestor(ancestor_concept_id);
CREATE INDEX idx_synonym_concept ON masterdata.m_concept_synonym(concept_id);
```

- [ ] **Step 2: Commit**

```bash
git add docker/init-db/17-masterdata.sql
git commit -m "feat(masterdata): add DDL for relationship, ancestor, synonym tables"
```

---

### Task 2: Entity + Repository (3组)

**Files:**
- Create: `entity/ConceptRelationshipEntity.java`
- Create: `entity/ConceptAncestorEntity.java`
- Create: `entity/ConceptSynonymEntity.java`
- Create: `repository/ConceptRelationshipRepository.java`
- Create: `repository/ConceptAncestorRepository.java`
- Create: `repository/ConceptSynonymRepository.java`

- [ ] **Step 1: ConceptRelationshipEntity**

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
@Table(name = "m_concept_relationship", schema = "masterdata")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE masterdata.m_concept_relationship SET is_deleted = true WHERE id = ?")
public class ConceptRelationshipEntity extends BaseEntity {

    @Column(name = "concept_id_1", nullable = false)
    private Long conceptId1;

    @Column(name = "concept_id_2", nullable = false)
    private Long conceptId2;

    @Column(name = "relationship_type", nullable = false, length = 64)
    private String relationshipType;

    @Column(name = "is_hierarchical", nullable = false)
    private Boolean isHierarchical = false;
}
```

- [ ] **Step 2: ConceptAncestorEntity (不继承 BaseEntity，无审计字段)**

```java
package com.maidc.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "m_concept_ancestor", schema = "masterdata")
public class ConceptAncestorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ancestor_concept_id", nullable = false)
    private Long ancestorConceptId;

    @Column(name = "descendant_concept_id", nullable = false)
    private Long descendantConceptId;

    @Column(name = "min_levels_of_separation", nullable = false)
    private Integer minLevelsOfSeparation = 0;

    @Column(name = "max_levels_of_separation", nullable = false)
    private Integer maxLevelsOfSeparation = 0;
}
```

- [ ] **Step 3: ConceptSynonymEntity**

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
@Table(name = "m_concept_synonym", schema = "masterdata")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE masterdata.m_concept_synonym SET is_deleted = true WHERE id = ?")
public class ConceptSynonymEntity extends BaseEntity {

    @Column(name = "concept_id", nullable = false)
    private Long conceptId;

    @Column(name = "synonym", nullable = false, length = 512)
    private String synonym;

    @Column(name = "language_code", nullable = false, length = 8)
    private String languageCode = "zh";

    @Column(name = "is_preferred", nullable = false)
    private Boolean isPreferred = false;
}
```

- [ ] **Step 4: 三个 Repository**

```java
// ConceptRelationshipRepository.java
package com.maidc.data.repository;

import com.maidc.data.entity.ConceptRelationshipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ConceptRelationshipRepository extends JpaRepository<ConceptRelationshipEntity, Long>, JpaSpecificationExecutor<ConceptRelationshipEntity> {
    List<ConceptRelationshipEntity> findByConceptId1AndRelationshipTypeAndIsDeletedFalse(Long conceptId, String type);
    List<ConceptRelationshipEntity> findByConceptId2AndRelationshipTypeAndIsDeletedFalse(Long conceptId, String type);
}
```

```java
// ConceptAncestorRepository.java
package com.maidc.data.repository;

import com.maidc.data.entity.ConceptAncestorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ConceptAncestorRepository extends JpaRepository<ConceptAncestorEntity, Long> {
    List<ConceptAncestorEntity> findByDescendantConceptId(Long descendantId);
    List<ConceptAncestorEntity> findByAncestorConceptId(Long ancestorId);
    List<ConceptAncestorEntity> findByAncestorConceptIdAndMinLevelsOfSeparation(Long ancestorId, int levels);
}
```

```java
// ConceptSynonymRepository.java
package com.maidc.data.repository;

import com.maidc.data.entity.ConceptSynonymEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ConceptSynonymRepository extends JpaRepository<ConceptSynonymEntity, Long> {
    List<ConceptSynonymEntity> findByConceptIdAndIsDeletedFalse(Long conceptId);
}
```

- [ ] **Step 5: Commit**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/entity/Concept{Relationship,Ancestor,Synonym}Entity.java \
        maidc-parent/maidc-data/src/main/java/com/maidc/data/repository/Concept{Relationship,Ancestor,Synonym}Repository.java
git commit -m "feat(masterdata): add relationship, ancestor, synonym entities and repositories"
```

---

### Task 3: ConceptMappingService + Controller

**Files:**
- Create: `service/ConceptMappingService.java`
- Create: `controller/ConceptMappingController.java`

- [ ] **Step 1: 写 Service**

```java
package com.maidc.data.service;

import com.maidc.data.entity.ConceptAncestorEntity;
import com.maidc.data.entity.ConceptEntity;
import com.maidc.data.entity.ConceptRelationshipEntity;
import com.maidc.data.entity.ConceptSynonymEntity;
import com.maidc.data.repository.ConceptAncestorRepository;
import com.maidc.data.repository.ConceptRelationshipRepository;
import com.maidc.data.repository.ConceptRepository;
import com.maidc.data.repository.ConceptSynonymRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConceptMappingService {

    private final ConceptRepository conceptRepository;
    private final ConceptRelationshipRepository relationshipRepository;
    private final ConceptAncestorRepository ancestorRepository;
    private final ConceptSynonymRepository synonymRepository;

    public List<ConceptEntity> getChildren(Long conceptId) {
        List<ConceptRelationshipEntity> rels = relationshipRepository
                .findByConceptId2AndRelationshipTypeAndIsDeletedFalse(conceptId, "IS_A");
        List<ConceptEntity> children = new ArrayList<>();
        for (ConceptRelationshipEntity rel : rels) {
            conceptRepository.findByIdAndIsDeletedFalse(rel.getConceptId1()).ifPresent(children::add);
        }
        return children;
    }

    public List<ConceptEntity> getDescendants(Long conceptId) {
        List<ConceptAncestorEntity> ancestors = ancestorRepository.findByAncestorConceptId(conceptId);
        List<ConceptEntity> descendants = new ArrayList<>();
        for (ConceptAncestorEntity a : ancestors) {
            conceptRepository.findByIdAndIsDeletedFalse(a.getDescendantConceptId()).ifPresent(descendants::add);
        }
        return descendants;
    }

    public List<ConceptEntity> getAncestors(Long conceptId) {
        List<ConceptAncestorEntity> ancestors = ancestorRepository.findByDescendantConceptId(conceptId);
        List<ConceptEntity> result = new ArrayList<>();
        for (ConceptAncestorEntity a : ancestors) {
            conceptRepository.findByIdAndIsDeletedFalse(a.getAncestorConceptId()).ifPresent(result::add);
        }
        return result;
    }

    public List<ConceptEntity> getMappings(Long conceptId, String targetSystem) {
        List<ConceptRelationshipEntity> rels = relationshipRepository
                .findByConceptId1AndRelationshipTypeAndIsDeletedFalse(conceptId, "MAPS_TO");
        List<ConceptEntity> mappings = new ArrayList<>();
        for (ConceptRelationshipEntity rel : rels) {
            conceptRepository.findByIdAndIsDeletedFalse(rel.getConceptId2()).ifPresent(c -> {
                if (targetSystem == null || targetSystem.equals(c.getCodeSystemId().toString())) {
                    mappings.add(c);
                }
            });
        }
        return mappings;
    }

    public List<ConceptSynonymEntity> getSynonyms(Long conceptId) {
        return synonymRepository.findByConceptIdAndIsDeletedFalse(conceptId);
    }

    @Transactional
    public ConceptRelationshipEntity createMapping(Long sourceId, Long targetId, String type) {
        ConceptRelationshipEntity rel = new ConceptRelationshipEntity();
        rel.setConceptId1(sourceId);
        rel.setConceptId2(targetId);
        rel.setRelationshipType(type);
        rel.setIsHierarchical("IS_A".equals(type) || "BROADER_THAN".equals(type));
        return relationshipRepository.save(rel);
    }

    @Transactional
    public List<ConceptRelationshipEntity> batchCreateMappings(List<ConceptRelationshipEntity> mappings) {
        return relationshipRepository.saveAll(mappings);
    }

    @Transactional
    public void deleteMapping(Long id) {
        relationshipRepository.deleteById(id);
    }
}
```

- [ ] **Step 2: 写 Controller**

```java
package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.ConceptEntity;
import com.maidc.data.entity.ConceptRelationshipEntity;
import com.maidc.data.entity.ConceptSynonymEntity;
import com.maidc.data.service.ConceptMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/masterdata/concepts")
@RequiredArgsConstructor
public class ConceptMappingController {

    private final ConceptMappingService mappingService;

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/{id}/children")
    public R<List<ConceptEntity>> children(@PathVariable Long id) {
        return R.ok(mappingService.getChildren(id));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/{id}/descendants")
    public R<List<ConceptEntity>> descendants(@PathVariable Long id) {
        return R.ok(mappingService.getDescendants(id));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/{id}/ancestors")
    public R<List<ConceptEntity>> ancestors(@PathVariable Long id) {
        return R.ok(mappingService.getAncestors(id));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/{id}/mappings")
    public R<List<ConceptEntity>> mappings(@PathVariable Long id,
                                            @RequestParam(required = false) String targetSystem) {
        return R.ok(mappingService.getMappings(id, targetSystem));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/{id}/synonyms")
    public R<List<ConceptSynonymEntity>> synonyms(@PathVariable Long id) {
        return R.ok(mappingService.getSynonyms(id));
    }
}
```

- [ ] **Step 3: 独立映射管理 Controller**

```java
package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.ConceptRelationshipEntity;
import com.maidc.data.service.ConceptMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/masterdata/mappings")
@RequiredArgsConstructor
public class MappingController {

    private final ConceptMappingService mappingService;

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping
    public R<List<ConceptRelationshipEntity>> list(
            @RequestParam(required = false) Long sourceSystem,
            @RequestParam(required = false) Long targetSystem) {
        // 简单查询：后续可扩展 Specification
        return R.ok(mappingService.listMappings(sourceSystem, targetSystem));
    }

    @PreAuthorize("hasPermission('masterdata:create')")
    @PostMapping
    public R<ConceptRelationshipEntity> create(@RequestBody ConceptRelationshipEntity rel) {
        return R.ok(mappingService.createMapping(rel.getConceptId1(), rel.getConceptId2(), rel.getRelationshipType()));
    }

    @PreAuthorize("hasPermission('masterdata:create')")
    @PostMapping("/batch")
    public R<List<ConceptRelationshipEntity>> batch(@RequestBody List<ConceptRelationshipEntity> rels) {
        return R.ok(mappingService.batchCreateMappings(rels));
    }

    @PreAuthorize("hasPermission('masterdata:create')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        mappingService.deleteMapping(id);
        return R.ok();
    }
}
```

注意: ConceptMappingService 需追加 `listMappings` 方法（用 Specification 查询）。

- [ ] **Step 4: 编译验证**

```bash
cd maidc-parent && mvn compile -pl maidc-data -am -q
```

- [ ] **Step 5: Commit**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/service/ConceptMappingService.java \
        maidc-parent/maidc-data/src/main/java/com/maidc/data/controller/ConceptMappingController.java \
        maidc-parent/maidc-data/src/main/java/com/maidc/data/controller/MappingController.java
git commit -m "feat(masterdata): add concept hierarchy, ancestor, synonym and mapping APIs"
```
