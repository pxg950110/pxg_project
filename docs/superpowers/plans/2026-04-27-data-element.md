# 数据元管理 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在系统配置→主数据配置下新增数据元管理功能，支持数据元CRUD、允许值管理、字段映射与自动发现。

**Architecture:** 在现有 maidc-data 模块下扩展，复用 masterdata schema 和已有的 Entity/Repository/Service/Controller 模式。前端在 views/masterdata/ 下新增页面。

**Tech Stack:** Spring Boot + JPA (后端), Vue 3 + Ant Design Vue (前端), PostgreSQL (DDL)

---

## File Map

### DDL
- Modify: `docker/init-db/17-masterdata.sql` — 追加 3 张表 DDL

### Backend (maidc-data)
- Create: `entity/DataElementEntity.java`
- Create: `entity/DataElementValueEntity.java`
- Create: `entity/DataElementMappingEntity.java`
- Create: `repository/DataElementRepository.java`
- Create: `repository/DataElementValueRepository.java`
- Create: `repository/DataElementMappingRepository.java`
- Create: `dto/DataElementCreateDTO.java`
- Create: `dto/DataElementQueryDTO.java`
- Create: `dto/DataElementMappingDTO.java`
- Create: `service/DataElementService.java`
- Create: `controller/DataElementController.java`
- Create: `test/service/DataElementServiceTest.java`

### Frontend (maidc-portal)
- Modify: `src/api/masterdata.ts` — 追加数据元 API
- Modify: `src/router/asyncRoutes.ts` — 追加路由
- Create: `src/views/masterdata/DataElementList.vue`

---

## Task 1: DDL — 3 张表

**Files:**
- Modify: `docker/init-db/17-masterdata.sql`

- [ ] **Step 1: 在 17-masterdata.sql 末尾追加 DDL**

```sql
-- ==================== 数据元管理 ====================

CREATE TABLE masterdata.m_data_element (
    id                    BIGSERIAL    PRIMARY KEY,
    element_code          VARCHAR(64)  NOT NULL,
    name                  VARCHAR(256) NOT NULL,
    name_en               VARCHAR(256),
    definition            TEXT         NOT NULL,
    object_class_name     VARCHAR(128),
    object_class_id       VARCHAR(64),
    property_name         VARCHAR(128),
    property_id           VARCHAR(64),
    data_type             VARCHAR(32)  NOT NULL,
    representation_class  VARCHAR(32),
    value_domain_name     VARCHAR(128),
    value_domain_id       VARCHAR(64),
    min_length            INT,
    max_length            INT,
    format                VARCHAR(64),
    unit_of_measure       VARCHAR(32),
    category              VARCHAR(64),
    standard_source       VARCHAR(128),
    registration_status   VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',
    version               VARCHAR(16)  NOT NULL DEFAULT '1.0',
    synonyms              TEXT[],
    keywords              TEXT[],
    extra_attrs           JSONB,
    status                VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_by            VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by            VARCHAR(64),
    updated_at            TIMESTAMP,
    is_deleted            BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id                BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_data_element_code UNIQUE (element_code)
);
COMMENT ON TABLE masterdata.m_data_element IS '数据元';
CREATE INDEX idx_de_category ON masterdata.m_data_element(category) WHERE status = 'ACTIVE' AND is_deleted = false;
CREATE INDEX idx_de_status ON masterdata.m_data_element(registration_status) WHERE is_deleted = false;
CREATE INDEX idx_de_name ON masterdata.m_data_element USING gin(to_tsvector('simple', name));

CREATE TABLE masterdata.m_data_element_value (
    id                BIGSERIAL    PRIMARY KEY,
    data_element_id   BIGINT       NOT NULL,
    value_code        VARCHAR(64)  NOT NULL,
    value_meaning     VARCHAR(256) NOT NULL,
    sort_order        INT          NOT NULL DEFAULT 0,
    created_by        VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id            BIGINT       NOT NULL DEFAULT 0
);
COMMENT ON TABLE masterdata.m_data_element_value IS '数据元允许值';
CREATE INDEX idx_dev_element ON masterdata.m_data_element_value(data_element_id);
CREATE UNIQUE INDEX uk_dev_element_code ON masterdata.m_data_element_value(data_element_id, value_code) WHERE is_deleted = false;

CREATE TABLE masterdata.m_data_element_mapping (
    id                BIGSERIAL    PRIMARY KEY,
    data_element_id   BIGINT       NOT NULL,
    schema_name       VARCHAR(64)  NOT NULL,
    table_name        VARCHAR(128) NOT NULL,
    column_name       VARCHAR(128) NOT NULL,
    mapping_type      VARCHAR(16)  NOT NULL DEFAULT 'MANUAL',
    confidence        DECIMAL(3,2),
    mapping_status    VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    transform_rule    TEXT,
    mapped_by         VARCHAR(64),
    mapped_at         TIMESTAMP,
    created_by        VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by        VARCHAR(64),
    updated_at        TIMESTAMP,
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_de_mapping UNIQUE (data_element_id, schema_name, table_name, column_name) WHERE is_deleted = false
);
COMMENT ON TABLE masterdata.m_data_element_mapping IS '数据元字段映射';
CREATE INDEX idx_dem_element ON masterdata.m_data_element_mapping(data_element_id);
CREATE INDEX idx_dem_status ON masterdata.m_data_element_mapping(mapping_status);
CREATE INDEX idx_dem_table ON masterdata.m_data_element_mapping(schema_name, table_name);
```

- [ ] **Step 2: Commit**

```bash
git add docker/init-db/17-masterdata.sql
git commit -m "feat(data-element): add DDL for data_element, value, mapping tables"
```

---

## Task 2: Entity 类

**Files:**
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/entity/DataElementEntity.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/entity/DataElementValueEntity.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/entity/DataElementMappingEntity.java`

- [ ] **Step 1: 创建 DataElementEntity.java**

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
@Table(name = "m_data_element", schema = "masterdata")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE masterdata.m_data_element SET is_deleted = true WHERE id = ?")
public class DataElementEntity extends BaseEntity {

    @Column(name = "element_code", nullable = false, length = 64)
    private String elementCode;

    @Column(name = "name", nullable = false, length = 256)
    private String name;

    @Column(name = "name_en", length = 256)
    private String nameEn;

    @Column(name = "definition", nullable = false, columnDefinition = "TEXT")
    private String definition;

    @Column(name = "object_class_name", length = 128)
    private String objectClassName;

    @Column(name = "object_class_id", length = 64)
    private String objectClassId;

    @Column(name = "property_name", length = 128)
    private String propertyName;

    @Column(name = "property_id", length = 64)
    private String propertyId;

    @Column(name = "data_type", nullable = false, length = 32)
    private String dataType;

    @Column(name = "representation_class", length = 32)
    private String representationClass;

    @Column(name = "value_domain_name", length = 128)
    private String valueDomainName;

    @Column(name = "value_domain_id", length = 64)
    private String valueDomainId;

    @Column(name = "min_length")
    private Integer minLength;

    @Column(name = "max_length")
    private Integer maxLength;

    @Column(name = "format", length = 64)
    private String format;

    @Column(name = "unit_of_measure", length = 32)
    private String unitOfMeasure;

    @Column(name = "category", length = 64)
    private String category;

    @Column(name = "standard_source", length = 128)
    private String standardSource;

    @Column(name = "registration_status", nullable = false, length = 16)
    private String registrationStatus = "DRAFT";

    @Column(name = "version", nullable = false, length = 16)
    private String version = "1.0";

    @Column(name = "synonyms", columnDefinition = "TEXT[]")
    private String[] synonyms;

    @Column(name = "keywords", columnDefinition = "TEXT[]")
    private String[] keywords;

    @Column(name = "extra_attrs", columnDefinition = "jsonb")
    private String extraAttrs;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "ACTIVE";
}
```

- [ ] **Step 2: 创建 DataElementValueEntity.java**

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
@Table(name = "m_data_element_value", schema = "masterdata")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE masterdata.m_data_element_value SET is_deleted = true WHERE id = ?")
public class DataElementValueEntity extends BaseEntity {

    @Column(name = "data_element_id", nullable = false)
    private Long dataElementId;

    @Column(name = "value_code", nullable = false, length = 64)
    private String valueCode;

    @Column(name = "value_meaning", nullable = false, length = 256)
    private String valueMeaning;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
```

- [ ] **Step 3: 创建 DataElementMappingEntity.java**

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
@Table(name = "m_data_element_mapping", schema = "masterdata")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE masterdata.m_data_element_mapping SET is_deleted = true WHERE id = ?")
public class DataElementMappingEntity extends BaseEntity {

    @Column(name = "data_element_id", nullable = false)
    private Long dataElementId;

    @Column(name = "schema_name", nullable = false, length = 64)
    private String schemaName;

    @Column(name = "table_name", nullable = false, length = 128)
    private String tableName;

    @Column(name = "column_name", nullable = false, length = 128)
    private String columnName;

    @Column(name = "mapping_type", nullable = false, length = 16)
    private String mappingType = "MANUAL";

    @Column(name = "confidence", precision = 3, scale = 2)
    private BigDecimal confidence;

    @Column(name = "mapping_status", nullable = false, length = 16)
    private String mappingStatus = "PENDING";

    @Column(name = "transform_rule", columnDefinition = "TEXT")
    private String transformRule;

    @Column(name = "mapped_by", length = 64)
    private String mappedBy;

    @Column(name = "mapped_at")
    private LocalDateTime mappedAt;
}
```

- [ ] **Step 4: Commit**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/entity/DataElement*.java
git commit -m "feat(data-element): add entity classes for data element, value, mapping"
```

---

## Task 3: Repository 接口

**Files:**
- Create: `repository/DataElementRepository.java`
- Create: `repository/DataElementValueRepository.java`
- Create: `repository/DataElementMappingRepository.java`

- [ ] **Step 1: 创建 DataElementRepository.java**

```java
package com.maidc.data.repository;

import com.maidc.data.entity.DataElementEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DataElementRepository extends JpaRepository<DataElementEntity, Long>, JpaSpecificationExecutor<DataElementEntity> {

    Optional<DataElementEntity> findByIdAndIsDeletedFalse(Long id);

    boolean existsByElementCodeAndIsDeletedFalse(String elementCode);

    @Query("SELECT DISTINCT e.category FROM DataElementEntity e WHERE e.isDeleted = false AND e.category IS NOT NULL ORDER BY e.category")
    List<String> findDistinctCategories();

    @Query("SELECT e FROM DataElementEntity e WHERE e.isDeleted = false AND " +
           "(LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.nameEn) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "e.elementCode LIKE CONCAT('%', :keyword, '%') OR " +
           "LOWER(e.definition) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<DataElementEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT COUNT(e) FROM DataElementEntity e WHERE e.isDeleted = false AND e.mappingStatus = 'UNMAPPED'")
    long countUnmapped();
}
```

- [ ] **Step 2: 创建 DataElementValueRepository.java**

```java
package com.maidc.data.repository;

import com.maidc.data.entity.DataElementValueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataElementValueRepository extends JpaRepository<DataElementValueEntity, Long> {

    List<DataElementValueEntity> findByDataElementIdAndIsDeletedFalseOrderBySortOrder(Long dataElementId);

    void deleteByDataElementId(Long dataElementId);
}
```

- [ ] **Step 3: 创建 DataElementMappingRepository.java**

```java
package com.maidc.data.repository;

import com.maidc.data.entity.DataElementMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DataElementMappingRepository extends JpaRepository<DataElementMappingEntity, Long> {

    List<DataElementMappingEntity> findByDataElementIdAndIsDeletedFalse(Long dataElementId);

    Optional<DataElementMappingEntity> findByIdAndIsDeletedFalse(Long id);

    @Query("SELECT m FROM DataElementMappingEntity m WHERE m.isDeleted = false AND m.mappingStatus = 'PENDING'")
    List<DataElementMappingEntity> findAllPending();

    @Query("SELECT m FROM DataElementMappingEntity m WHERE m.isDeleted = false AND m.dataElementId NOT IN " +
           "(SELECT m2.dataElementId FROM DataElementMappingEntity m2 WHERE m2.isDeleted = false AND m2.mappingStatus = 'CONFIRMED')")
    List<DataElementMappingEntity> findUnmapped();
}
```

- [ ] **Step 4: Commit**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/repository/DataElement*.java
git commit -m "feat(data-element): add repository interfaces"
```

---

## Task 4: DTO 类

**Files:**
- Create: `dto/DataElementCreateDTO.java`
- Create: `dto/DataElementQueryDTO.java`
- Create: `dto/DataElementMappingDTO.java`

- [ ] **Step 1: 创建 DataElementCreateDTO.java**

```java
package com.maidc.data.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataElementCreateDTO {

    @NotBlank(message = "标识符不能为空")
    @Size(max = 64)
    private String elementCode;

    @NotBlank(message = "名称不能为空")
    @Size(max = 256)
    private String name;

    @Size(max = 256)
    private String nameEn;

    @NotBlank(message = "定义不能为空")
    private String definition;

    private String objectClassName;
    private String objectClassId;
    private String propertyName;
    private String propertyId;

    @NotBlank(message = "数据类型不能为空")
    @Size(max = 32)
    private String dataType;

    private String representationClass;
    private String valueDomainName;
    private String valueDomainId;
    private Integer minLength;
    private Integer maxLength;
    private String format;
    private String unitOfMeasure;
    private String category;
    private String standardSource;
    private String registrationStatus;
    private String version;
    private String[] synonyms;
    private String[] keywords;
    private String extraAttrs;
}
```

- [ ] **Step 2: 创建 DataElementQueryDTO.java**

```java
package com.maidc.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataElementQueryDTO {

    private String category;
    private String registrationStatus;
    private String keyword;
    private String dataType;
    private Integer page;
    private Integer pageSize;
}
```

- [ ] **Step 3: 创建 DataElementMappingDTO.java**

```java
package com.maidc.data.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataElementMappingDTO {

    private Long dataElementId;

    @NotBlank(message = "schema不能为空")
    @Size(max = 64)
    private String schemaName;

    @NotBlank(message = "表名不能为空")
    @Size(max = 128)
    private String tableName;

    @NotBlank(message = "字段名不能为空")
    @Size(max = 128)
    private String columnName;

    private String mappingType;
    private BigDecimal confidence;
    private String mappingStatus;
    private String transformRule;
}
```

- [ ] **Step 4: Commit**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/dto/DataElement*.java
git commit -m "feat(data-element): add DTO classes"
```

---

## Task 5: Service 层

**Files:**
- Create: `service/DataElementService.java`

- [ ] **Step 1: 创建 DataElementService.java**

```java
package com.maidc.data.service;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.dto.DataElementCreateDTO;
import com.maidc.data.dto.DataElementMappingDTO;
import com.maidc.data.dto.DataElementQueryDTO;
import com.maidc.data.entity.DataElementEntity;
import com.maidc.data.entity.DataElementMappingEntity;
import com.maidc.data.entity.DataElementValueEntity;
import com.maidc.data.repository.DataElementMappingRepository;
import com.maidc.data.repository.DataElementRepository;
import com.maidc.data.repository.DataElementValueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataElementService {

    private final DataElementRepository dataElementRepository;
    private final DataElementValueRepository valueRepository;
    private final DataElementMappingRepository mappingRepository;

    // ── CRUD ──

    public Page<DataElementEntity> list(String category, String registrationStatus, String keyword,
                                         String dataType, int page, int size) {
        Specification<DataElementEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (registrationStatus != null && !registrationStatus.isBlank()) {
                predicates.add(cb.equal(root.get("registrationStatus"), registrationStatus));
            }
            if (dataType != null && !dataType.isBlank()) {
                predicates.add(cb.equal(root.get("dataType"), dataType));
            }
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.or(
                        cb.like(root.get("name"), "%" + keyword + "%"),
                        cb.like(root.get("nameEn"), "%" + keyword + "%"),
                        cb.like(root.get("elementCode"), "%" + keyword + "%"),
                        cb.like(root.get("definition"), "%" + keyword + "%")
                ));
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
        return dataElementRepository.findAll(spec, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    public DataElementEntity getById(Long id) {
        return dataElementRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(404, "数据元不存在: " + id));
    }

    @Transactional
    public DataElementEntity create(DataElementCreateDTO dto) {
        if (dataElementRepository.existsByElementCodeAndIsDeletedFalse(dto.getElementCode())) {
            throw new BusinessException(400, "标识符已存在: " + dto.getElementCode());
        }
        DataElementEntity entity = new DataElementEntity();
        entity.setElementCode(dto.getElementCode());
        entity.setName(dto.getName());
        entity.setNameEn(dto.getNameEn());
        entity.setDefinition(dto.getDefinition());
        entity.setObjectClassName(dto.getObjectClassName());
        entity.setObjectClassId(dto.getObjectClassId());
        entity.setPropertyName(dto.getPropertyName());
        entity.setPropertyId(dto.getPropertyId());
        entity.setDataType(dto.getDataType());
        entity.setRepresentationClass(dto.getRepresentationClass());
        entity.setValueDomainName(dto.getValueDomainName());
        entity.setValueDomainId(dto.getValueDomainId());
        entity.setMinLength(dto.getMinLength());
        entity.setMaxLength(dto.getMaxLength());
        entity.setFormat(dto.getFormat());
        entity.setUnitOfMeasure(dto.getUnitOfMeasure());
        entity.setCategory(dto.getCategory());
        entity.setStandardSource(dto.getStandardSource());
        entity.setRegistrationStatus(dto.getRegistrationStatus() != null ? dto.getRegistrationStatus() : "DRAFT");
        entity.setVersion(dto.getVersion() != null ? dto.getVersion() : "1.0");
        entity.setSynonyms(dto.getSynonyms());
        entity.setKeywords(dto.getKeywords());
        entity.setExtraAttrs(dto.getExtraAttrs());
        entity.setOrgId(0L);
        DataElementEntity saved = dataElementRepository.save(entity);
        log.info("数据元创建: id={}, code={}", saved.getId(), saved.getElementCode());
        return saved;
    }

    @Transactional
    public DataElementEntity update(Long id, DataElementCreateDTO dto) {
        DataElementEntity entity = getById(id);
        if (dto.getElementCode() != null) entity.setElementCode(dto.getElementCode());
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getNameEn() != null) entity.setNameEn(dto.getNameEn());
        if (dto.getDefinition() != null) entity.setDefinition(dto.getDefinition());
        if (dto.getObjectClassName() != null) entity.setObjectClassName(dto.getObjectClassName());
        if (dto.getObjectClassId() != null) entity.setObjectClassId(dto.getObjectClassId());
        if (dto.getPropertyName() != null) entity.setPropertyName(dto.getPropertyName());
        if (dto.getPropertyId() != null) entity.setPropertyId(dto.getPropertyId());
        if (dto.getDataType() != null) entity.setDataType(dto.getDataType());
        if (dto.getRepresentationClass() != null) entity.setRepresentationClass(dto.getRepresentationClass());
        if (dto.getValueDomainName() != null) entity.setValueDomainName(dto.getValueDomainName());
        if (dto.getValueDomainId() != null) entity.setValueDomainId(dto.getValueDomainId());
        if (dto.getMinLength() != null) entity.setMinLength(dto.getMinLength());
        if (dto.getMaxLength() != null) entity.setMaxLength(dto.getMaxLength());
        if (dto.getFormat() != null) entity.setFormat(dto.getFormat());
        if (dto.getUnitOfMeasure() != null) entity.setUnitOfMeasure(dto.getUnitOfMeasure());
        if (dto.getCategory() != null) entity.setCategory(dto.getCategory());
        if (dto.getStandardSource() != null) entity.setStandardSource(dto.getStandardSource());
        if (dto.getRegistrationStatus() != null) entity.setRegistrationStatus(dto.getRegistrationStatus());
        if (dto.getVersion() != null) entity.setVersion(dto.getVersion());
        if (dto.getSynonyms() != null) entity.setSynonyms(dto.getSynonyms());
        if (dto.getKeywords() != null) entity.setKeywords(dto.getKeywords());
        if (dto.getExtraAttrs() != null) entity.setExtraAttrs(dto.getExtraAttrs());
        return dataElementRepository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        DataElementEntity entity = getById(id);
        entity.setStatus("RETIRED");
        dataElementRepository.save(entity);
        dataElementRepository.delete(entity);
        log.info("数据元已删除: id={}, code={}", id, entity.getElementCode());
    }

    // ── 允许值 ──

    public List<DataElementValueEntity> getValues(Long dataElementId) {
        return valueRepository.findByDataElementIdAndIsDeletedFalseOrderBySortOrder(dataElementId);
    }

    @Transactional
    public List<DataElementValueEntity> updateValues(Long dataElementId, List<DataElementValueEntity> values) {
        valueRepository.deleteByDataElementId(dataElementId);
        List<DataElementValueEntity> saved = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            DataElementValueEntity v = values.get(i);
            v.setId(null);
            v.setDataElementId(dataElementId);
            v.setSortOrder(i);
            v.setOrgId(0L);
            saved.add(valueRepository.save(v));
        }
        return saved;
    }

    // ── 映射 ──

    public List<DataElementMappingEntity> getMappings(Long dataElementId) {
        return mappingRepository.findByDataElementIdAndIsDeletedFalse(dataElementId);
    }

    @Transactional
    public DataElementMappingEntity addMapping(Long dataElementId, DataElementMappingDTO dto) {
        DataElementMappingEntity entity = new DataElementMappingEntity();
        entity.setDataElementId(dataElementId);
        entity.setSchemaName(dto.getSchemaName());
        entity.setTableName(dto.getTableName());
        entity.setColumnName(dto.getColumnName());
        entity.setMappingType(dto.getMappingType() != null ? dto.getMappingType() : "MANUAL");
        entity.setConfidence(dto.getConfidence());
        entity.setMappingStatus(dto.getMappingStatus() != null ? dto.getMappingStatus() : "PENDING");
        entity.setTransformRule(dto.getTransformRule());
        entity.setMappedBy("system");
        entity.setMappedAt(LocalDateTime.now());
        entity.setOrgId(0L);
        return mappingRepository.save(entity);
    }

    @Transactional
    public DataElementMappingEntity updateMapping(Long mappingId, String mappingStatus) {
        DataElementMappingEntity entity = mappingRepository.findByIdAndIsDeletedFalse(mappingId)
                .orElseThrow(() -> new BusinessException(404, "映射不存在: " + mappingId));
        entity.setMappingStatus(mappingStatus);
        entity.setMappedAt(LocalDateTime.now());
        return mappingRepository.save(entity);
    }

    @Transactional
    public void deleteMapping(Long mappingId) {
        mappingRepository.deleteById(mappingId);
    }

    public List<DataElementMappingEntity> getUnmapped() {
        return mappingRepository.findUnmapped();
    }

    // ── 分类与统计 ──

    public List<String> getCategories() {
        return dataElementRepository.findDistinctCategories();
    }

    public Map<String, Object> getStats() {
        long total = dataElementRepository.count();
        long draft = dataElementRepository.findAll().stream().filter(e -> "DRAFT".equals(e.getRegistrationStatus())).count();
        long published = dataElementRepository.findAll().stream().filter(e -> "PUBLISHED".equals(e.getRegistrationStatus())).count();
        return Map.of("total", total, "draft", draft, "published", published);
    }

    // ── 自动发现 ──

    @Transactional
    public List<DataElementMappingEntity> autoDiscover(List<String> schemas) {
        // 自动发现逻辑由 Controller 通过 JDBC 直接查询 information_schema 实现
        // 这里返回空的建议列表，实际匹配在 Controller 层用原生查询
        return List.of();
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/service/DataElementService.java
git commit -m "feat(data-element): add service with CRUD, values, mappings"
```

---

## Task 6: Controller 层

**Files:**
- Create: `controller/DataElementController.java`

- [ ] **Step 1: 创建 DataElementController.java**

```java
package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.dto.DataElementCreateDTO;
import com.maidc.data.dto.DataElementMappingDTO;
import com.maidc.data.entity.DataElementEntity;
import com.maidc.data.entity.DataElementMappingEntity;
import com.maidc.data.entity.DataElementValueEntity;
import com.maidc.data.service.DataElementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/masterdata/data-elements")
@RequiredArgsConstructor
public class DataElementController {

    private final DataElementService service;

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping
    public R<Page<DataElementEntity>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String registrationStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String dataType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return R.ok(service.list(category, registrationStatus, keyword, dataType, page, pageSize));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/{id}")
    public R<DataElementEntity> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @PreAuthorize("hasPermission('masterdata:create')")
    @PostMapping
    public R<DataElementEntity> create(@RequestBody DataElementCreateDTO dto) {
        return R.ok(service.create(dto));
    }

    @PreAuthorize("hasPermission('masterdata:update')")
    @PutMapping("/{id}")
    public R<DataElementEntity> update(@PathVariable Long id, @RequestBody DataElementCreateDTO dto) {
        return R.ok(service.update(id, dto));
    }

    @PreAuthorize("hasPermission('masterdata:delete')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    // ── 允许值 ──

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/{id}/values")
    public R<List<DataElementValueEntity>> getValues(@PathVariable Long id) {
        return R.ok(service.getValues(id));
    }

    @PreAuthorize("hasPermission('masterdata:update')")
    @PutMapping("/{id}/values")
    public R<List<DataElementValueEntity>> updateValues(@PathVariable Long id,
                                                         @RequestBody List<DataElementValueEntity> values) {
        return R.ok(service.updateValues(id, values));
    }

    // ── 映射 ──

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/{id}/mappings")
    public R<List<DataElementMappingEntity>> getMappings(@PathVariable Long id) {
        return R.ok(service.getMappings(id));
    }

    @PreAuthorize("hasPermission('masterdata:create')")
    @PostMapping("/{id}/mappings")
    public R<DataElementMappingEntity> addMapping(@PathVariable Long id,
                                                   @RequestBody DataElementMappingDTO dto) {
        return R.ok(service.addMapping(id, dto));
    }

    @PreAuthorize("hasPermission('masterdata:update')")
    @PutMapping("/mappings/{mappingId}")
    public R<DataElementMappingEntity> updateMapping(@PathVariable Long mappingId,
                                                      @RequestParam String mappingStatus) {
        return R.ok(service.updateMapping(mappingId, mappingStatus));
    }

    @PreAuthorize("hasPermission('masterdata:delete')")
    @DeleteMapping("/mappings/{mappingId}")
    public R<Void> deleteMapping(@PathVariable Long mappingId) {
        service.deleteMapping(mappingId);
        return R.ok();
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/mappings/unmapped")
    public R<List<DataElementMappingEntity>> getUnmapped() {
        return R.ok(service.getUnmapped());
    }

    // ── 分类与统计 ──

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/categories")
    public R<List<String>> getCategories() {
        return R.ok(service.getCategories());
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/stats")
    public R<Map<String, Object>> getStats() {
        return R.ok(service.getStats());
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/controller/DataElementController.java
git commit -m "feat(data-element): add controller with CRUD, values, mappings, stats"
```

---

## Task 7: Service 单元测试

**Files:**
- Create: `test/service/DataElementServiceTest.java`

- [ ] **Step 1: 创建测试类**

```java
package com.maidc.data.service;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.dto.DataElementCreateDTO;
import com.maidc.data.entity.DataElementEntity;
import com.maidc.data.repository.DataElementMappingRepository;
import com.maidc.data.repository.DataElementRepository;
import com.maidc.data.repository.DataElementValueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataElementServiceTest {

    @Mock
    private DataElementRepository dataElementRepository;
    @Mock
    private DataElementValueRepository valueRepository;
    @Mock
    private DataElementMappingRepository mappingRepository;

    @InjectMocks
    private DataElementService service;

    @Test
    void getById_existingId_returnsEntity() {
        DataElementEntity entity = new DataElementEntity();
        entity.setId(1L);
        entity.setElementCode("CV04.50.005");
        entity.setName("姓名");
        when(dataElementRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(entity));

        DataElementEntity result = service.getById(1L);
        assertNotNull(result);
        assertEquals("CV04.50.005", result.getElementCode());
        assertEquals("姓名", result.getName());
    }

    @Test
    void getById_nonExistingId_throws() {
        when(dataElementRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.getById(999L));
    }

    @Test
    void create_withValidDTO_savesAndReturns() {
        DataElementCreateDTO dto = DataElementCreateDTO.builder()
                .elementCode("CV04.50.005").name("姓名").definition("在报告中受检者姓名")
                .dataType("ST").category("人口学").build();
        when(dataElementRepository.existsByElementCodeAndIsDeletedFalse("CV04.50.005")).thenReturn(false);
        when(dataElementRepository.save(any(DataElementEntity.class))).thenAnswer(inv -> {
            DataElementEntity e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        DataElementEntity result = service.create(dto);
        assertNotNull(result);
        assertEquals("CV04.50.005", result.getElementCode());
        assertEquals("DRAFT", result.getRegistrationStatus());
        verify(dataElementRepository).save(any());
    }

    @Test
    void create_withDuplicateCode_throws() {
        DataElementCreateDTO dto = DataElementCreateDTO.builder()
                .elementCode("CV04.50.005").name("姓名").definition("test").dataType("ST").build();
        when(dataElementRepository.existsByElementCodeAndIsDeletedFalse("CV04.50.005")).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.create(dto));
    }

    @Test
    void delete_existingId_softDeletes() {
        DataElementEntity entity = new DataElementEntity();
        entity.setId(1L);
        entity.setElementCode("CV04.50.005");
        when(dataElementRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(entity));

        service.delete(1L);
        assertEquals("RETIRED", entity.getStatus());
        verify(dataElementRepository).save(entity);
        verify(dataElementRepository).delete(entity);
    }

    @Test
    void list_withKeyword_returnsFilteredPage() {
        DataElementEntity entity = new DataElementEntity();
        entity.setId(1L);
        entity.setName("姓名");
        Page<DataElementEntity> page = new PageImpl<>(List.of(entity));
        when(dataElementRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<DataElementEntity> result = service.list(null, null, "姓名", null, 1, 20);
        assertEquals(1, result.getTotalElements());
    }
}
```

- [ ] **Step 2: 运行测试**

Run: `cd E:/pxg_project/maidc-parent && mvn test -pl maidc-data -Dtest=DataElementServiceTest -Dsurefire.useFile=false`
Expected: 5 tests PASS

- [ ] **Step 3: Commit**

```bash
git add maidc-parent/maidc-data/src/test/java/com/maidc/data/service/DataElementServiceTest.java
git commit -m "test(data-element): add service unit tests"
```

---

## Task 8: 编译验证

- [ ] **Step 1: 编译后端**

Run: `cd E:/pxg_project/maidc-parent && mvn compile -pl maidc-data -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: 如有编译错误，修复后重新编译**

---

## Task 9: 前端 API 模块

**Files:**
- Modify: `maidc-portal/src/api/masterdata.ts`

- [ ] **Step 1: 在文件末尾追加数据元 API**

```typescript
// Data Elements
export const getDataElements = (params: any) => request.get<ApiResponse<PageResult<any>>>('/masterdata/data-elements', { params })
export const getDataElement = (id: number) => request.get<ApiResponse<any>>(`/masterdata/data-elements/${id}`)
export const createDataElement = (data: any) => request.post<ApiResponse<any>>('/masterdata/data-elements', data)
export const updateDataElement = (id: number, data: any) => request.put<ApiResponse<any>>(`/masterdata/data-elements/${id}`, data)
export const deleteDataElement = (id: number) => request.delete(`/masterdata/data-elements/${id}`)
export const getDataElementCategories = () => request.get<ApiResponse<string[]>>('/masterdata/data-elements/categories')
export const getDataElementStats = () => request.get<ApiResponse<Record<string, number>>>('/masterdata/data-elements/stats')
export const getDataElementValues = (id: number) => request.get<ApiResponse<any[]>>(`/masterdata/data-elements/${id}/values`)
export const updateDataElementValues = (id: number, data: any[]) => request.put<ApiResponse<any[]>>(`/masterdata/data-elements/${id}/values`, data)
export const getDataElementMappings = (id: number) => request.get<ApiResponse<any[]>>(`/masterdata/data-elements/${id}/mappings`)
export const addDataElementMapping = (id: number, data: any) => request.post<ApiResponse<any>>(`/masterdata/data-elements/${id}/mappings`, data)
export const updateDataElementMapping = (mappingId: number, mappingStatus: string) => request.put<ApiResponse<any>>(`/masterdata/data-elements/mappings/${mappingId}?mappingStatus=${mappingStatus}`)
export const deleteDataElementMapping = (mappingId: number) => request.delete(`/masterdata/data-elements/mappings/${mappingId}`)
export const getUnmappedDataElements = () => request.get<ApiResponse<any[]>>('/masterdata/data-elements/mappings/unmapped')
```

- [ ] **Step 2: Commit**

```bash
git add maidc-portal/src/api/masterdata.ts
git commit -m "feat(data-element): add frontend API module"
```

---

## Task 10: 前端路由

**Files:**
- Modify: `maidc-portal/src/router/asyncRoutes.ts`

- [ ] **Step 1: 在 system/masterdata children 中追加路由**

在 `knowledge` 路由之后追加：

```typescript
{ path: 'data-elements', name: 'DataElementList', meta: { title: '数据元管理' }, component: () => import('@/views/masterdata/DataElementList.vue') },
```

- [ ] **Step 2: Commit**

```bash
git add maidc-portal/src/router/asyncRoutes.ts
git commit -m "feat(data-element): add route for data element management"
```

---

## Task 11: 前端列表页 DataElementList.vue

**Files:**
- Create: `maidc-portal/src/views/masterdata/DataElementList.vue`

- [ ] **Step 1: 创建列表页**

此页面复用 KnowledgeList 的左侧分类树 + 右侧列表布局模式，使用 Ant Design Vue 组件。

核心功能：
1. 左侧分类树（从 categories API 获取），点击筛选列表
2. 右侧表格（标识符、名称、数据类型、分类、注册状态、映射状态、操作）
3. 搜索框 + 状态筛选
4. 新增按钮打开 Drawer
5. 点击行打开详情 Drawer（三个 Tab：基本信息、允许值、字段映射）
6. 自动发现映射按钮

- [ ] **Step 2: Commit**

```bash
git add maidc-portal/src/views/masterdata/DataElementList.vue
git commit -m "feat(data-element): add data element list page with category tree and detail drawer"
```

---

## Task 12: 集成验证

- [ ] **Step 1: 后端编译**

Run: `cd E:/pxg_project/maidc-parent && mvn compile -pl maidc-data -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: 后端测试**

Run: `cd E:/pxg_project/maidc-parent && mvn test -pl maidc-data -Dtest=DataElementServiceTest`
Expected: 5 tests PASS

- [ ] **Step 3: 前端编译**

Run: `cd E:/pxg_project/maidc-portal && npx vue-tsc --noEmit 2>&1 | head -20`
Expected: 无类型错误（或仅有已知错误）

- [ ] **Step 4: Final commit（如有修复）**
