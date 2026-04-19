# Phase 1: DDL + Entity + Repository + DTO/VO + Mapper

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** 创建 4 张 ETL 管道相关表的 DDL，以及对应的 JPA Entity、Repository、DTO、VO 和 MapStruct Mapper。

**Architecture:** 4 张表在 `cdr` schema 下（`r_etl_pipeline`, `r_etl_step`, `r_etl_field_mapping`, `r_etl_execution`），遵循现有 BaseEntity + 软删除 + JsonNodeConverter 模式。

**Tech Stack:** PostgreSQL / JPA / Lombok / MapStruct

---

## File Structure

```
docker/init-db/
  12-cdr-etl.sql                        (新建) 4 张 ETL 表 DDL

maidc-parent/maidc-data/src/main/java/com/maidc/data/
  entity/
    EtlPipelineEntity.java              (新建)
    EtlStepEntity.java                  (新建)
    EtlFieldMappingEntity.java          (新建)
    EtlExecutionEntity.java             (新建)
  repository/
    EtlPipelineRepository.java          (新建)
    EtlStepRepository.java              (新建)
    EtlFieldMappingRepository.java      (新建)
    EtlExecutionRepository.java         (新建)
  dto/
    etl/
      EtlPipelineCreateDTO.java         (新建)
      EtlPipelineQueryDTO.java          (新建)
      EtlStepCreateDTO.java             (新建)
      EtlStepUpdateDTO.java             (新建)
      EtlFieldMappingDTO.java           (新建)
      EtlExecutionQueryDTO.java         (新建)
  vo/
    EtlPipelineVO.java                  (新建)
    EtlPipelineDetailVO.java            (新建)
    EtlStepVO.java                      (新建)
    EtlFieldMappingVO.java              (新建)
    EtlExecutionVO.java                 (新建)
  mapper/
    DataMapper.java                     (修改) 添加 ETL 映射方法
```

---

### Task 1.1: 创建 DDL 脚本

**Files:**
- Create: `docker/init-db/12-cdr-etl.sql`

- [ ] **Step 1: 编写 DDL**

创建 `docker/init-db/12-cdr-etl.sql`：

```sql
-- ===========================================================================
-- ODS→CDR ETL 管道管理表
-- ===========================================================================

-- 1. ETL 管道
CREATE TABLE cdr.r_etl_pipeline (
    id                  BIGSERIAL       PRIMARY KEY,
    pipeline_name       VARCHAR(128)    NOT NULL,
    source_id           BIGINT          NOT NULL,
    description         TEXT,
    engine_type         VARCHAR(16)     NOT NULL DEFAULT 'EMBULK'
                            CHECK (engine_type IN ('EMBULK','SPARK','PYTHON')),
    status              VARCHAR(16)     NOT NULL DEFAULT 'DRAFT'
                            CHECK (status IN ('DRAFT','ACTIVE','DISABLED')),
    sync_mode           VARCHAR(16)     NOT NULL DEFAULT 'MANUAL'
                            CHECK (sync_mode IN ('MANUAL','INCREMENTAL','FULL')),
    cron_expression     VARCHAR(64),
    last_run_time       TIMESTAMP,
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);
COMMENT ON TABLE cdr.r_etl_pipeline IS 'ETL管道';

-- 2. 管道步骤
CREATE TABLE cdr.r_etl_step (
    id                  BIGSERIAL       PRIMARY KEY,
    pipeline_id         BIGINT          NOT NULL,
    step_name           VARCHAR(128)    NOT NULL,
    step_order          INT             NOT NULL,
    step_type           VARCHAR(16)     NOT NULL DEFAULT 'ONE_TO_ONE'
                            CHECK (step_type IN ('ONE_TO_ONE','ONE_TO_MANY','MANY_TO_ONE')),
    source_schema       VARCHAR(32),
    source_table        VARCHAR(128)    NOT NULL,
    target_schema       VARCHAR(32),
    target_table        VARCHAR(128)    NOT NULL,
    join_config         JSONB,
    filter_condition    TEXT,
    transform_config    JSONB,
    pre_sql             TEXT,
    post_sql            TEXT,
    on_error            VARCHAR(16)     NOT NULL DEFAULT 'ABORT'
                            CHECK (on_error IN ('SKIP','RETRY','ABORT')),
    sync_mode           VARCHAR(16)     NOT NULL DEFAULT 'INCREMENTAL'
                            CHECK (sync_mode IN ('FULL','INCREMENTAL')),
    last_sync_time      TIMESTAMP,
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);
COMMENT ON TABLE cdr.r_etl_step IS 'ETL管道步骤';

-- 3. 字段映射
CREATE TABLE cdr.r_etl_field_mapping (
    id                  BIGSERIAL       PRIMARY KEY,
    step_id             BIGINT          NOT NULL,
    source_column       VARCHAR(64),
    source_table_alias  VARCHAR(32),
    target_column       VARCHAR(64)     NOT NULL,
    transform_type      VARCHAR(16)     NOT NULL DEFAULT 'DIRECT'
                            CHECK (transform_type IN ('DIRECT','MAP','EXPRESSION','CONSTANT','DATE_FMT','LOOKUP')),
    transform_expr      TEXT,
    default_value       TEXT,
    is_required         BOOLEAN         NOT NULL DEFAULT FALSE,
    sort_order          INT,
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);
COMMENT ON TABLE cdr.r_etl_field_mapping IS 'ETL字段映射';

-- 4. 执行记录
CREATE TABLE cdr.r_etl_execution (
    id                  BIGSERIAL       PRIMARY KEY,
    pipeline_id         BIGINT          NOT NULL,
    step_id             BIGINT,
    status              VARCHAR(16)     NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN ('PENDING','RUNNING','SUCCESS','FAILED','CANCELLED','SKIPPED')),
    engine_config       TEXT,
    start_time          TIMESTAMP,
    end_time            TIMESTAMP,
    rows_read           BIGINT          DEFAULT 0,
    rows_written        BIGINT          DEFAULT 0,
    rows_skipped        BIGINT          DEFAULT 0,
    rows_error          BIGINT          DEFAULT 0,
    error_message       TEXT,
    log_path            VARCHAR(256),
    trigger_type        VARCHAR(16)     NOT NULL DEFAULT 'MANUAL'
                            CHECK (trigger_type IN ('MANUAL','SCHEDULE','RETRY')),
    execution_snapshot  JSONB,
    created_by          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    org_id              BIGINT          NOT NULL
);
COMMENT ON TABLE cdr.r_etl_execution IS 'ETL执行记录';

-- 索引
CREATE INDEX idx_etl_pipeline_source ON cdr.r_etl_pipeline(source_id) WHERE NOT is_deleted;
CREATE INDEX idx_etl_step_pipeline    ON cdr.r_etl_step(pipeline_id)   WHERE NOT is_deleted;
CREATE INDEX idx_etl_mapping_step     ON cdr.r_etl_field_mapping(step_id) WHERE NOT is_deleted;
CREATE INDEX idx_etl_exec_pipeline    ON cdr.r_etl_execution(pipeline_id) WHERE NOT is_deleted;
CREATE INDEX idx_etl_exec_status      ON cdr.r_etl_execution(status)   WHERE NOT is_deleted;
CREATE INDEX idx_etl_exec_time        ON cdr.r_etl_execution(created_at DESC) WHERE NOT is_deleted;
```

- [ ] **Step 2: 验证 SQL 语法**

Run: `docker exec -i maidc-postgres psql -U maidc -d maidc -f /docker-entrypoint-initdb.d/12-cdr-etl.sql`

Expected: 无报错，4 张表创建成功

- [ ] **Step 3: 提交**

```bash
git add docker/init-db/12-cdr-etl.sql
git commit -m "feat(etl): add DDL for ETL pipeline management tables"
```

---

### Task 1.2: 创建 Entity 类

**Files:**
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/entity/EtlPipelineEntity.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/entity/EtlStepEntity.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/entity/EtlFieldMappingEntity.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/entity/EtlExecutionEntity.java`

- [ ] **Step 1: 创建 EtlPipelineEntity**

```java
package com.maidc.data.entity;

import com.maidc.common.jpa.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@DynamicUpdate
@Table(name = "r_etl_pipeline", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.r_etl_pipeline SET is_deleted = true WHERE id = ?")
@Getter
@Setter
public class EtlPipelineEntity extends BaseEntity {

    @Column(name = "pipeline_name", nullable = false, length = 128)
    private String pipelineName;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "engine_type", nullable = false, length = 16)
    private String engineType = "EMBULK";

    @Column(name = "status", nullable = false, length = 16)
    private String status = "DRAFT";

    @Column(name = "sync_mode", nullable = false, length = 16)
    private String syncMode = "MANUAL";

    @Column(name = "cron_expression", length = 64)
    private String cronExpression;

    @Column(name = "last_run_time")
    private LocalDateTime lastRunTime;
}
```

- [ ] **Step 2: 创建 EtlStepEntity**

```java
package com.maidc.data.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.maidc.common.jpa.base.BaseEntity;
import com.maidc.common.jpa.converter.JsonNodeConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@DynamicUpdate
@Table(name = "r_etl_step", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.r_etl_step SET is_deleted = true WHERE id = ?")
@Getter
@Setter
public class EtlStepEntity extends BaseEntity {

    @Column(name = "pipeline_id", nullable = false)
    private Long pipelineId;

    @Column(name = "step_name", nullable = false, length = 128)
    private String stepName;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "step_type", nullable = false, length = 16)
    private String stepType = "ONE_TO_ONE";

    @Column(name = "source_schema", length = 32)
    private String sourceSchema;

    @Column(name = "source_table", nullable = false, length = 128)
    private String sourceTable;

    @Column(name = "target_schema", length = 32)
    private String targetSchema;

    @Column(name = "target_table", nullable = false, length = 128)
    private String targetTable;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "join_config", columnDefinition = "jsonb")
    private JsonNode joinConfig;

    @Column(name = "filter_condition", columnDefinition = "TEXT")
    private String filterCondition;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "transform_config", columnDefinition = "jsonb")
    private JsonNode transformConfig;

    @Column(name = "pre_sql", columnDefinition = "TEXT")
    private String preSql;

    @Column(name = "post_sql", columnDefinition = "TEXT")
    private String postSql;

    @Column(name = "on_error", nullable = false, length = 16)
    private String onError = "ABORT";

    @Column(name = "sync_mode", nullable = false, length = 16)
    private String syncMode = "INCREMENTAL";

    @Column(name = "last_sync_time")
    private LocalDateTime lastSyncTime;
}
```

- [ ] **Step 3: 创建 EtlFieldMappingEntity**

```java
package com.maidc.data.entity;

import com.maidc.common.jpa.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@DynamicUpdate
@Table(name = "r_etl_field_mapping", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.r_etl_field_mapping SET is_deleted = true WHERE id = ?")
@Getter
@Setter
public class EtlFieldMappingEntity extends BaseEntity {

    @Column(name = "step_id", nullable = false)
    private Long stepId;

    @Column(name = "source_column", length = 64)
    private String sourceColumn;

    @Column(name = "source_table_alias", length = 32)
    private String sourceTableAlias;

    @Column(name = "target_column", nullable = false, length = 64)
    private String targetColumn;

    @Column(name = "transform_type", nullable = false, length = 16)
    private String transformType = "DIRECT";

    @Column(name = "transform_expr", columnDefinition = "TEXT")
    private String transformExpr;

    @Column(name = "default_value", columnDefinition = "TEXT")
    private String defaultValue;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
```

- [ ] **Step 4: 创建 EtlExecutionEntity**

```java
package com.maidc.data.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.maidc.common.jpa.base.BaseEntity;
import com.maidc.common.jpa.converter.JsonNodeConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@DynamicUpdate
@Table(name = "r_etl_execution", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.r_etl_execution SET is_deleted = true WHERE id = ?")
@Getter
@Setter
public class EtlExecutionEntity extends BaseEntity {

    @Column(name = "pipeline_id", nullable = false)
    private Long pipelineId;

    @Column(name = "step_id")
    private Long stepId;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "PENDING";

    @Column(name = "engine_config", columnDefinition = "TEXT")
    private String engineConfig;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "rows_read")
    private Long rowsRead = 0L;

    @Column(name = "rows_written")
    private Long rowsWritten = 0L;

    @Column(name = "rows_skipped")
    private Long rowsSkipped = 0L;

    @Column(name = "rows_error")
    private Long rowsError = 0L;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "log_path", length = 256)
    private String logPath;

    @Column(name = "trigger_type", nullable = false, length = 16)
    private String triggerType = "MANUAL";

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "execution_snapshot", columnDefinition = "jsonb")
    private JsonNode executionSnapshot;
}
```

- [ ] **Step 5: 编译验证**

Run: `cd maidc-parent && mvn compile -pl maidc-data -am -q`

Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/entity/Etl*.java
git commit -m "feat(etl): add ETL pipeline entity classes"
```

---

### Task 1.3: 创建 Repository 接口

**Files:**
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/repository/EtlPipelineRepository.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/repository/EtlStepRepository.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/repository/EtlFieldMappingRepository.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/repository/EtlExecutionRepository.java`

- [ ] **Step 1: 创建全部 Repository**

`EtlPipelineRepository.java`:
```java
package com.maidc.data.repository;

import com.maidc.data.entity.EtlPipelineEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface EtlPipelineRepository extends JpaRepository<EtlPipelineEntity, Long>,
        JpaSpecificationExecutor<EtlPipelineEntity> {

    Page<EtlPipelineEntity> findBySourceIdAndIsDeletedFalse(Long sourceId, Pageable pageable);

    List<EtlPipelineEntity> findByStatusAndIsDeletedFalse(String status);
}
```

`EtlStepRepository.java`:
```java
package com.maidc.data.repository;

import com.maidc.data.entity.EtlStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EtlStepRepository extends JpaRepository<EtlStepEntity, Long> {

    List<EtlStepEntity> findByPipelineIdAndIsDeletedFalseOrderByStepOrder(Long pipelineId);

    long countByPipelineIdAndIsDeletedFalse(Long pipelineId);

    void deleteByPipelineId(Long pipelineId);
}
```

`EtlFieldMappingRepository.java`:
```java
package com.maidc.data.repository;

import com.maidc.data.entity.EtlFieldMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EtlFieldMappingRepository extends JpaRepository<EtlFieldMappingEntity, Long> {

    List<EtlFieldMappingEntity> findByStepIdAndIsDeletedFalseOrderBySortOrder(Long stepId);

    void deleteByStepId(Long stepId);
}
```

`EtlExecutionRepository.java`:
```java
package com.maidc.data.repository;

import com.maidc.data.entity.EtlExecutionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface EtlExecutionRepository extends JpaRepository<EtlExecutionEntity, Long>,
        JpaSpecificationExecutor<EtlExecutionEntity> {

    Page<EtlExecutionEntity> findByPipelineIdAndIsDeletedFalse(Long pipelineId, Pageable pageable);

    List<EtlExecutionEntity> findByPipelineIdAndStatusAndIsDeletedFalse(Long pipelineId, String status);
}
```

- [ ] **Step 2: 编译验证**

Run: `cd maidc-parent && mvn compile -pl maidc-data -am -q`

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/repository/Etl*.java
git commit -m "feat(etl): add ETL repository interfaces"
```

---

### Task 1.4: 创建 DTO 和 VO

**Files:**
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/dto/etl/EtlPipelineCreateDTO.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/dto/etl/EtlPipelineQueryDTO.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/dto/etl/EtlStepCreateDTO.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/dto/etl/EtlStepUpdateDTO.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/dto/etl/EtlFieldMappingDTO.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/dto/etl/EtlExecutionQueryDTO.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/vo/EtlPipelineVO.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/vo/EtlPipelineDetailVO.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/vo/EtlStepVO.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/vo/EtlFieldMappingVO.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/vo/EtlExecutionVO.java`

- [ ] **Step 1: 创建 DTO 类**

`EtlPipelineCreateDTO.java`:
```java
package com.maidc.data.dto.etl;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtlPipelineCreateDTO {

    @NotBlank(message = "管道名称不能为空")
    @Size(max = 128, message = "管道名称最长128个字符")
    private String pipelineName;

    @NotNull(message = "数据源不能为空")
    private Long sourceId;

    private String description;

    @Builder.Default
    private String engineType = "EMBULK";

    @Builder.Default
    private String syncMode = "MANUAL";

    private String cronExpression;
}
```

`EtlPipelineQueryDTO.java`:
```java
package com.maidc.data.dto.etl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtlPipelineQueryDTO {

    private String keyword;

    private Long sourceId;

    private String status;

    private String engineType;

    @Builder.Default
    private int page = 1;

    @Builder.Default
    private int pageSize = 20;
}
```

`EtlStepCreateDTO.java`:
```java
package com.maidc.data.dto.etl;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtlStepCreateDTO {

    @NotBlank(message = "步骤名称不能为空")
    @Size(max = 128)
    private String stepName;

    @NotNull(message = "步骤顺序不能为空")
    private Integer stepOrder;

    @Builder.Default
    private String stepType = "ONE_TO_ONE";

    private String sourceSchema;

    @NotBlank(message = "源表名不能为空")
    private String sourceTable;

    private String targetSchema;

    @NotBlank(message = "目标表名不能为空")
    private String targetTable;

    private String joinConfig;
    private String filterCondition;
    private String transformConfig;
    private String preSql;
    private String postSql;

    @Builder.Default
    private String onError = "ABORT";

    @Builder.Default
    private String syncMode = "INCREMENTAL";
}
```

`EtlStepUpdateDTO.java`:
```java
package com.maidc.data.dto.etl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtlStepUpdateDTO {

    private String stepName;
    private Integer stepOrder;
    private String stepType;
    private String sourceSchema;
    private String sourceTable;
    private String targetSchema;
    private String targetTable;
    private String joinConfig;
    private String filterCondition;
    private String transformConfig;
    private String preSql;
    private String postSql;
    private String onError;
    private String syncMode;
}
```

`EtlFieldMappingDTO.java`:
```java
package com.maidc.data.dto.etl;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtlFieldMappingDTO {

    private Long id;

    private String sourceColumn;

    private String sourceTableAlias;

    @NotBlank(message = "目标字段名不能为空")
    private String targetColumn;

    @Builder.Default
    private String transformType = "DIRECT";

    private String transformExpr;
    private String defaultValue;

    @Builder.Default
    private Boolean isRequired = false;

    private Integer sortOrder;
}
```

`EtlExecutionQueryDTO.java`:
```java
package com.maidc.data.dto.etl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtlExecutionQueryDTO {

    private Long pipelineId;
    private Long stepId;
    private String status;
    private String triggerType;

    @Builder.Default
    private int page = 1;

    @Builder.Default
    private int pageSize = 20;
}
```

- [ ] **Step 2: 创建 VO 类**

`EtlPipelineVO.java`:
```java
package com.maidc.data.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtlPipelineVO {

    private Long id;
    private String pipelineName;
    private Long sourceId;
    private String sourceName;
    private String description;
    private String engineType;
    private String status;
    private String syncMode;
    private String cronExpression;
    private LocalDateTime lastRunTime;
    private Integer stepCount;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

`EtlPipelineDetailVO.java`:
```java
package com.maidc.data.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtlPipelineDetailVO {

    private Long id;
    private String pipelineName;
    private Long sourceId;
    private String sourceName;
    private String description;
    private String engineType;
    private String status;
    private String syncMode;
    private String cronExpression;
    private LocalDateTime lastRunTime;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<EtlStepVO> steps;
}
```

`EtlStepVO.java`:
```java
package com.maidc.data.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtlStepVO {

    private Long id;
    private Long pipelineId;
    private String stepName;
    private Integer stepOrder;
    private String stepType;
    private String sourceSchema;
    private String sourceTable;
    private String targetSchema;
    private String targetTable;
    private Object joinConfig;
    private String filterCondition;
    private Object transformConfig;
    private String preSql;
    private String postSql;
    private String onError;
    private String syncMode;
    private LocalDateTime lastSyncTime;
    private String createdBy;
    private LocalDateTime createdAt;
    private List<EtlFieldMappingVO> fieldMappings;
}
```

`EtlFieldMappingVO.java`:
```java
package com.maidc.data.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtlFieldMappingVO {

    private Long id;
    private Long stepId;
    private String sourceColumn;
    private String sourceTableAlias;
    private String targetColumn;
    private String transformType;
    private String transformExpr;
    private String defaultValue;
    private Boolean isRequired;
    private Integer sortOrder;
}
```

`EtlExecutionVO.java`:
```java
package com.maidc.data.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtlExecutionVO {

    private Long id;
    private Long pipelineId;
    private String pipelineName;
    private Long stepId;
    private String stepName;
    private String status;
    private String engineConfig;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long rowsRead;
    private Long rowsWritten;
    private Long rowsSkipped;
    private Long rowsError;
    private String errorMessage;
    private String logPath;
    private String triggerType;
    private Object executionSnapshot;
    private String createdBy;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 3: 编译验证**

Run: `cd maidc-parent && mvn compile -pl maidc-data -am -q`

Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/dto/etl/
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/vo/Etl*.java
git commit -m "feat(etl): add ETL DTOs and VOs"
```

---

### Task 1.5: 更新 DataMapper

**Files:**
- Modify: `maidc-parent/maidc-data/src/main/java/com/maidc/data/mapper/DataMapper.java`

- [ ] **Step 1: 添加 ETL 映射方法**

在 `DataMapper.java` 末尾添加：

```java
    // ==================== ETL Pipeline ====================
    EtlPipelineVO toEtlPipelineVO(EtlPipelineEntity entity);

    EtlStepVO toEtlStepVO(EtlStepEntity entity);

    EtlFieldMappingVO toEtlFieldMappingVO(EtlFieldMappingEntity entity);

    EtlExecutionVO toEtlExecutionVO(EtlExecutionEntity entity);
```

同时添加 import 语句：
```java
import com.maidc.data.entity.*;
import com.maidc.data.vo.*;
```

（注意：import 使用通配符 `*` 即可覆盖新增类）

- [ ] **Step 2: 编译验证**

Run: `cd maidc-parent && mvn compile -pl maidc-data -am -q`

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/mapper/DataMapper.java
git commit -m "feat(etl): add ETL entity-to-VO mappings in DataMapper"
```

---

### Task 1.6: 全量编译 + 验证

- [ ] **Step 1: 全模块编译**

Run: `cd maidc-parent && mvn compile -q`

Expected: BUILD SUCCESS

- [ ] **Step 2: 运行现有测试确保无回归**

Run: `cd maidc-parent && mvn test -pl maidc-data -q`

Expected: 所有测试通过

- [ ] **Step 3: 确认数据库 DDL 可执行**

在本地 PostgreSQL 中执行 `docker/init-db/12-cdr-etl.sql`，确认 4 张表和索引创建成功。

```bash
docker exec -i maidc-postgres psql -U maidc -d maidc -c "\dt cdr.r_etl_*"
```

Expected: 列出 4 张表

---

## Phase 1 完成标准

- [x] 4 张 ETL 表 DDL 创建且可执行
- [x] 4 个 Entity 类编译通过，遵循 BaseEntity + 软删除模式
- [x] 4 个 Repository 接口定义
- [x] 6 个 DTO + 5 个 VO 类
- [x] DataMapper 添加 ETL 映射方法
- [x] 全模块编译 BUILD SUCCESS
- [x] 现有测试无回归
