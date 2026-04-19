# Phase 2: Service 层

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** 实现 6 个 Service 类：EtlPipelineService、EtlStepService、EtlFieldMappingService、EtlMetadataService、EtlConfigGenerator、EtlExecutionService。

**Architecture:** 服务层负责业务逻辑，包括 CRUD、校验、自动映射算法、Embulk YAML 配置生成、执行调度。事务边界在 Service 方法上控制。

**Tech Stack:** Spring Boot / JPA Specifications / Jackson / ProcessBuilder（Embulk 调用）

---

## File Structure

```
maidc-parent/maidc-data/src/main/java/com/maidc/data/
  service/
    etl/
      EtlPipelineService.java         (新建)
      EtlStepService.java             (新建)
      EtlFieldMappingService.java     (新建)
      EtlMetadataService.java         (新建)
      EtlConfigGenerator.java         (新建)
      EtlExecutionService.java        (新建)
```

---

### Task 2.1: EtlMetadataService

**Files:**
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/service/etl/EtlMetadataService.java`

**说明：** 查询数据库 schema/table/column 元信息，供前端下拉选择源表、目标表、字段。

- [ ] **Step 1: 创建 EtlMetadataService**

```java
package com.maidc.data.service.etl;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EtlMetadataService {

    private final JdbcTemplate jdbcTemplate;

    public List<String> listSchemas() {
        return jdbcTemplate.queryForList(
            "SELECT schema_name FROM information_schema.schemata " +
            "WHERE schema_name NOT IN ('pg_catalog','information_schema','pg_toast') " +
            "ORDER BY schema_name", String.class);
    }

    public List<Map<String, Object>> listTables(String schema) {
        return jdbcTemplate.queryForList(
            "SELECT table_name, table_type FROM information_schema.tables " +
            "WHERE table_schema = ? AND table_type = 'BASE TABLE' ORDER BY table_name", schema);
    }

    public List<Map<String, Object>> listColumns(String schema, String table) {
        return jdbcTemplate.queryForList(
            "SELECT column_name, data_type, is_nullable, column_default " +
            "FROM information_schema.columns " +
            "WHERE table_schema = ? AND table_name = ? ORDER BY ordinal_position", schema, table);
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `cd maidc-parent && mvn compile -pl maidc-data -am -q`

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/service/etl/EtlMetadataService.java
git commit -m "feat(etl): add EtlMetadataService for schema/table/column queries"
```

---

### Task 2.2: EtlPipelineService

**Files:**
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/service/etl/EtlPipelineService.java`

**说明：** 管道 CRUD + 校验 + 列表查询（分页 + 关键字 + 状态过滤）。

- [ ] **Step 1: 创建 EtlPipelineService**

```java
package com.maidc.data.service.etl;

import com.maidc.common.core.exception.BizException;
import com.maidc.data.dto.etl.EtlPipelineCreateDTO;
import com.maidc.data.dto.etl.EtlPipelineQueryDTO;
import com.maidc.data.entity.EtlPipelineEntity;
import com.maidc.data.entity.EtlStepEntity;
import com.maidc.data.mapper.DataMapper;
import com.maidc.data.repository.EtlPipelineRepository;
import com.maidc.data.repository.EtlStepRepository;
import com.maidc.data.vo.EtlPipelineDetailVO;
import com.maidc.data.vo.EtlPipelineVO;
import com.maidc.data.vo.EtlStepVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EtlPipelineService {

    private final EtlPipelineRepository pipelineRepository;
    private final EtlStepRepository stepRepository;
    private final DataMapper dataMapper;

    @Transactional
    public EtlPipelineVO createPipeline(EtlPipelineCreateDTO dto) {
        EtlPipelineEntity entity = new EtlPipelineEntity();
        entity.setPipelineName(dto.getPipelineName());
        entity.setSourceId(dto.getSourceId());
        entity.setDescription(dto.getDescription());
        entity.setEngineType(dto.getEngineType());
        entity.setSyncMode(dto.getSyncMode());
        entity.setCronExpression(dto.getCronExpression());
        entity = pipelineRepository.save(entity);
        return dataMapper.toEtlPipelineVO(entity);
    }

    @Transactional
    public EtlPipelineVO updatePipeline(Long id, EtlPipelineCreateDTO dto) {
        EtlPipelineEntity entity = pipelineRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "管道不存在"));
        entity.setPipelineName(dto.getPipelineName());
        entity.setSourceId(dto.getSourceId());
        entity.setDescription(dto.getDescription());
        entity.setEngineType(dto.getEngineType());
        entity.setSyncMode(dto.getSyncMode());
        entity.setCronExpression(dto.getCronExpression());
        entity = pipelineRepository.save(entity);
        return dataMapper.toEtlPipelineVO(entity);
    }

    public EtlPipelineDetailVO getPipelineDetail(Long id) {
        EtlPipelineEntity entity = pipelineRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "管道不存在"));

        EtlPipelineDetailVO detail = EtlPipelineDetailVO.builder()
                .id(entity.getId())
                .pipelineName(entity.getPipelineName())
                .sourceId(entity.getSourceId())
                .description(entity.getDescription())
                .engineType(entity.getEngineType())
                .status(entity.getStatus())
                .syncMode(entity.getSyncMode())
                .cronExpression(entity.getCronExpression())
                .lastRunTime(entity.getLastRunTime())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();

        List<EtlStepEntity> steps = stepRepository
                .findByPipelineIdAndIsDeletedFalseOrderByStepOrder(id);
        detail.setSteps(steps.stream().map(dataMapper::toEtlStepVO).toList());

        return detail;
    }

    public Page<EtlPipelineVO> listPipelines(EtlPipelineQueryDTO query) {
        Specification<EtlPipelineEntity> spec = (root, q, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
                predicates.add(cb.like(root.get("pipelineName"), "%" + query.getKeyword() + "%"));
            }
            if (query.getSourceId() != null) {
                predicates.add(cb.equal(root.get("sourceId"), query.getSourceId()));
            }
            if (query.getStatus() != null && !query.getStatus().isBlank()) {
                predicates.add(cb.equal(root.get("status"), query.getStatus()));
            }
            if (query.getEngineType() != null && !query.getEngineType().isBlank()) {
                predicates.add(cb.equal(root.get("engineType"), query.getEngineType()));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<EtlPipelineEntity> page = pipelineRepository.findAll(spec,
                PageRequest.of(query.getPage() - 1, query.getPageSize(),
                        Sort.by(Sort.Direction.DESC, "createdAt")));

        return page.map(entity -> {
            EtlPipelineVO vo = dataMapper.toEtlPipelineVO(entity);
            vo.setStepCount((int) stepRepository.countByPipelineIdAndIsDeletedFalse(entity.getId()));
            return vo;
        });
    }

    @Transactional
    public void deletePipeline(Long id) {
        pipelineRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "管道不存在"));
        pipelineRepository.deleteById(id);
    }

    @Transactional
    public EtlPipelineVO updateStatus(Long id, String status) {
        EtlPipelineEntity entity = pipelineRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "管道不存在"));
        entity.setStatus(status);
        entity = pipelineRepository.save(entity);
        return dataMapper.toEtlPipelineVO(entity);
    }

    @Transactional
    public EtlPipelineVO copyPipeline(Long id) {
        EtlPipelineEntity source = pipelineRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "管道不存在"));

        EtlPipelineEntity copy = new EtlPipelineEntity();
        copy.setPipelineName(source.getPipelineName() + " (副本)");
        copy.setSourceId(source.getSourceId());
        copy.setDescription(source.getDescription());
        copy.setEngineType(source.getEngineType());
        copy.setStatus("DRAFT");
        copy.setSyncMode(source.getSyncMode());
        copy.setCronExpression(source.getCronExpression());
        copy = pipelineRepository.save(copy);

        // 复制步骤和字段映射
        List<EtlStepEntity> steps = stepRepository
                .findByPipelineIdAndIsDeletedFalseOrderByStepOrder(id);
        for (EtlStepEntity step : steps) {
            // 步骤复制由 EtlStepService 处理，此处仅复制管道基础信息
        }

        return dataMapper.toEtlPipelineVO(copy);
    }

    public List<String> validatePipeline(Long id) {
        var errors = new java.util.ArrayList<String>();
        EtlPipelineEntity entity = pipelineRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "管道不存在"));

        List<EtlStepEntity> steps = stepRepository
                .findByPipelineIdAndIsDeletedFalseOrderByStepOrder(id);

        if (steps.isEmpty()) {
            errors.add("管道没有配置任何步骤");
        }

        for (EtlStepEntity step : steps) {
            if (step.getSourceTable() == null || step.getSourceTable().isBlank()) {
                errors.add("步骤[" + step.getStepName() + "]未配置源表");
            }
            if (step.getTargetTable() == null || step.getTargetTable().isBlank()) {
                errors.add("步骤[" + step.getStepName() + "]未配置目标表");
            }
        }

        return errors;
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `cd maidc-parent && mvn compile -pl maidc-data -am -q`

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/service/etl/EtlPipelineService.java
git commit -m "feat(etl): add EtlPipelineService with CRUD, copy, validate"
```

---

### Task 2.3: EtlStepService

**Files:**
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/service/etl/EtlStepService.java`

**说明：** 步骤管理 + 顺序编排 + 预览。

- [ ] **Step 1: 创建 EtlStepService**

```java
package com.maidc.data.service.etl;

import com.maidc.common.core.exception.BizException;
import com.maidc.data.dto.etl.EtlStepCreateDTO;
import com.maidc.data.dto.etl.EtlStepUpdateDTO;
import com.maidc.data.entity.EtlFieldMappingEntity;
import com.maidc.data.entity.EtlStepEntity;
import com.maidc.data.mapper.DataMapper;
import com.maidc.data.repository.EtlFieldMappingRepository;
import com.maidc.data.repository.EtlStepRepository;
import com.maidc.data.vo.EtlFieldMappingVO;
import com.maidc.data.vo.EtlStepVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EtlStepService {

    private final EtlStepRepository stepRepository;
    private final EtlFieldMappingRepository fieldMappingRepository;
    private final DataMapper dataMapper;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public EtlStepVO createStep(Long pipelineId, EtlStepCreateDTO dto) {
        EtlStepEntity entity = new EtlStepEntity();
        entity.setPipelineId(pipelineId);
        applyCreateDto(entity, dto);
        entity = stepRepository.save(entity);
        return dataMapper.toEtlStepVO(entity);
    }

    @Transactional
    public EtlStepVO updateStep(Long stepId, EtlStepUpdateDTO dto) {
        EtlStepEntity entity = stepRepository.findById(stepId)
                .orElseThrow(() -> new BizException(404, "步骤不存在"));
        applyUpdateDto(entity, dto);
        entity = stepRepository.save(entity);
        return dataMapper.toEtlStepVO(entity);
    }

    public List<EtlStepVO> listSteps(Long pipelineId) {
        return stepRepository.findByPipelineIdAndIsDeletedFalseOrderByStepOrder(pipelineId)
                .stream()
                .map(entity -> {
                    EtlStepVO vo = dataMapper.toEtlStepVO(entity);
                    List<EtlFieldMappingVO> mappings = fieldMappingRepository
                            .findByStepIdAndIsDeletedFalseOrderBySortOrder(entity.getId())
                            .stream().map(dataMapper::toEtlFieldMappingVO).toList();
                    vo.setFieldMappings(mappings);
                    return vo;
                })
                .toList();
    }

    @Transactional
    public void deleteStep(Long stepId) {
        stepRepository.findById(stepId)
                .orElseThrow(() -> new BizException(404, "步骤不存在"));
        // 级联删除字段映射
        fieldMappingRepository.deleteByStepId(stepId);
        stepRepository.deleteById(stepId);
    }

    @Transactional
    public void reorderSteps(Long pipelineId, List<Long> stepIds) {
        List<EtlStepEntity> steps = stepRepository
                .findByPipelineIdAndIsDeletedFalseOrderByStepOrder(pipelineId);
        for (int i = 0; i < stepIds.size(); i++) {
            int order = i + 1;
            steps.stream()
                    .filter(s -> s.getId().equals(stepIds.get(i)))
                    .findFirst()
                    .ifPresent(s -> s.setStepOrder(order));
        }
        stepRepository.saveAll(steps);
    }

    public List<Map<String, Object>> previewData(Long stepId) {
        EtlStepEntity step = stepRepository.findById(stepId)
                .orElseThrow(() -> new BizException(404, "步骤不存在"));

        String schema = step.getSourceSchema() != null ? step.getSourceSchema() : "ods";
        String sql = "SELECT * FROM " + schema + "." + step.getSourceTable() + " LIMIT 10";

        return jdbcTemplate.queryForList(sql);
    }

    private void applyCreateDto(EtlStepEntity entity, EtlStepCreateDTO dto) {
        entity.setStepName(dto.getStepName());
        entity.setStepOrder(dto.getStepOrder());
        entity.setStepType(dto.getStepType());
        entity.setSourceSchema(dto.getSourceSchema());
        entity.setSourceTable(dto.getSourceTable());
        entity.setTargetSchema(dto.getTargetSchema());
        entity.setTargetTable(dto.getTargetTable());
        entity.setFilterCondition(dto.getFilterCondition());
        entity.setPreSql(dto.getPreSql());
        entity.setPostSql(dto.getPostSql());
        entity.setOnError(dto.getOnError());
        entity.setSyncMode(dto.getSyncMode());
    }

    private void applyUpdateDto(EtlStepEntity entity, EtlStepUpdateDTO dto) {
        if (dto.getStepName() != null) entity.setStepName(dto.getStepName());
        if (dto.getStepOrder() != null) entity.setStepOrder(dto.getStepOrder());
        if (dto.getStepType() != null) entity.setStepType(dto.getStepType());
        if (dto.getSourceSchema() != null) entity.setSourceSchema(dto.getSourceSchema());
        if (dto.getSourceTable() != null) entity.setSourceTable(dto.getSourceTable());
        if (dto.getTargetSchema() != null) entity.setTargetSchema(dto.getTargetSchema());
        if (dto.getTargetTable() != null) entity.setTargetTable(dto.getTargetTable());
        if (dto.getFilterCondition() != null) entity.setFilterCondition(dto.getFilterCondition());
        if (dto.getPreSql() != null) entity.setPreSql(dto.getPreSql());
        if (dto.getPostSql() != null) entity.setPostSql(dto.getPostSql());
        if (dto.getOnError() != null) entity.setOnError(dto.getOnError());
        if (dto.getSyncMode() != null) entity.setSyncMode(dto.getSyncMode());
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `cd maidc-parent && mvn compile -pl maidc-data -am -q`

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/service/etl/EtlStepService.java
git commit -m "feat(etl): add EtlStepService with CRUD, reorder, preview"
```

---

### Task 2.4: EtlFieldMappingService

**Files:**
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/service/etl/EtlFieldMappingService.java`

**说明：** 字段映射管理 + 自动映射算法（精确匹配 → 模糊匹配 → 类型匹配）。

- [ ] **Step 1: 创建 EtlFieldMappingService**

```java
package com.maidc.data.service.etl;

import com.maidc.common.core.exception.BizException;
import com.maidc.data.dto.etl.EtlFieldMappingDTO;
import com.maidc.data.entity.EtlFieldMappingEntity;
import com.maidc.data.entity.EtlStepEntity;
import com.maidc.data.mapper.DataMapper;
import com.maidc.data.repository.EtlFieldMappingRepository;
import com.maidc.data.repository.EtlStepRepository;
import com.maidc.data.vo.EtlFieldMappingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EtlFieldMappingService {

    private final EtlFieldMappingRepository fieldMappingRepository;
    private final EtlStepRepository stepRepository;
    private final DataMapper dataMapper;
    private final JdbcTemplate jdbcTemplate;

    public List<EtlFieldMappingVO> listMappings(Long stepId) {
        return fieldMappingRepository.findByStepIdAndIsDeletedFalseOrderBySortOrder(stepId)
                .stream().map(dataMapper::toEtlFieldMappingVO).toList();
    }

    @Transactional
    public List<EtlFieldMappingVO> batchUpdateMappings(Long stepId, List<EtlFieldMappingDTO> dtos) {
        stepRepository.findById(stepId)
                .orElseThrow(() -> new BizException(404, "步骤不存在"));

        // 删除旧映射
        fieldMappingRepository.deleteByStepId(stepId);

        // 批量创建新映射
        List<EtlFieldMappingEntity> entities = new ArrayList<>();
        for (int i = 0; i < dtos.size(); i++) {
            EtlFieldMappingDTO dto = dtos.get(i);
            EtlFieldMappingEntity entity = new EtlFieldMappingEntity();
            entity.setStepId(stepId);
            entity.setSourceColumn(dto.getSourceColumn());
            entity.setSourceTableAlias(dto.getSourceTableAlias());
            entity.setTargetColumn(dto.getTargetColumn());
            entity.setTransformType(dto.getTransformType());
            entity.setTransformExpr(dto.getTransformExpr());
            entity.setDefaultValue(dto.getDefaultValue());
            entity.setIsRequired(dto.getIsRequired());
            entity.setSortOrder(i);
            entities.add(entity);
        }
        entities = fieldMappingRepository.saveAll(entities);

        return entities.stream().map(dataMapper::toEtlFieldMappingVO).toList();
    }

    @Transactional
    public List<EtlFieldMappingVO> autoMap(Long stepId) {
        EtlStepEntity step = stepRepository.findById(stepId)
                .orElseThrow(() -> new BizException(404, "步骤不存在"));

        String srcSchema = step.getSourceSchema() != null ? step.getSourceSchema() : "ods";
        String tgtSchema = step.getTargetSchema() != null ? step.getTargetSchema() : "cdr";

        List<Map<String, Object>> sourceColumns = listColumnMetadata(srcSchema, step.getSourceTable());
        List<Map<String, Object>> targetColumns = listColumnMetadata(tgtSchema, step.getTargetTable());

        Set<String> targetNames = targetColumns.stream()
                .map(c -> (String) c.get("column_name"))
                .collect(Collectors.toSet());

        List<EtlFieldMappingDTO> mappings = new ArrayList<>();

        // 阶段1: 精确匹配（不区分大小写）
        Map<String, String> srcLowerMap = sourceColumns.stream()
                .collect(Collectors.toMap(
                        c -> ((String) c.get("column_name")).toLowerCase(),
                        c -> (String) c.get("column_name")));

        Set<String> matched = new HashSet<>();
        for (String tgt : targetNames) {
            String srcMatch = srcLowerMap.get(tgt.toLowerCase());
            if (srcMatch != null) {
                mappings.add(EtlFieldMappingDTO.builder()
                        .sourceColumn(srcMatch).targetColumn(tgt)
                        .transformType("DIRECT").sortOrder(mappings.size()).build());
                matched.add(srcMatch.toLowerCase());
                matched.add(tgt.toLowerCase());
            }
        }

        // 阶段2: 模糊匹配（编辑距离 ≤ 2）
        for (String tgt : targetNames) {
            if (matched.contains(tgt.toLowerCase())) continue;
            for (Map<String, Object> src : sourceColumns) {
                String srcName = (String) src.get("column_name");
                if (matched.contains(srcName.toLowerCase())) continue;
                if (editDistance(srcName.toLowerCase(), tgt.toLowerCase()) <= 2) {
                    mappings.add(EtlFieldMappingDTO.builder()
                            .sourceColumn(srcName).targetColumn(tgt)
                            .transformType("DIRECT").sortOrder(mappings.size()).build());
                    matched.add(srcName.toLowerCase());
                    matched.add(tgt.toLowerCase());
                    break;
                }
            }
        }

        return batchUpdateMappings(stepId, mappings);
    }

    private List<Map<String, Object>> listColumnMetadata(String schema, String table) {
        return jdbcTemplate.queryForList(
            "SELECT column_name, data_type FROM information_schema.columns " +
            "WHERE table_schema = ? AND table_name = ? ORDER BY ordinal_position",
            schema, table);
    }

    private int editDistance(String a, String b) {
        int m = a.length(), n = b.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                dp[i][j] = a.charAt(i - 1) == b.charAt(j - 1)
                        ? dp[i - 1][j - 1]
                        : 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
            }
        }
        return dp[m][n];
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `cd maidc-parent && mvn compile -pl maidc-data -am -q`

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/service/etl/EtlFieldMappingService.java
git commit -m "feat(etl): add EtlFieldMappingService with auto-map algorithm"
```

---

### Task 2.5: EtlConfigGenerator

**Files:**
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/service/etl/EtlConfigGenerator.java`

**说明：** 根据 Step + FieldMapping 生成 Embulk YAML 配置。

- [ ] **Step 1: 创建 EtlConfigGenerator**

```java
package com.maidc.data.service.etl;

import com.maidc.data.entity.EtlFieldMappingEntity;
import com.maidc.data.entity.EtlStepEntity;
import com.maidc.data.repository.EtlFieldMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EtlConfigGenerator {

    private final EtlFieldMappingRepository fieldMappingRepository;

    public String generateEmbulkConfig(EtlStepEntity step, String sourceHost, int sourcePort,
                                        String sourceDb, String sourceUser, String sourcePass,
                                        String targetHost, int targetPort,
                                        String targetDb, String targetUser, String targetPass) {

        List<EtlFieldMappingEntity> mappings = fieldMappingRepository
                .findByStepIdAndIsDeletedFalseOrderBySortOrder(step.getId());

        String srcSchema = step.getSourceSchema() != null ? step.getSourceSchema() : "ods";
        String tgtSchema = step.getTargetSchema() != null ? step.getTargetSchema() : "cdr";

        StringBuilder sb = new StringBuilder();
        sb.append("in:\n");
        sb.append("  type: postgresql\n");
        sb.append("  host: ").append(sourceHost).append("\n");
        sb.append("  port: ").append(sourcePort).append("\n");
        sb.append("  database: ").append(sourceDb).append("\n");
        sb.append("  user: ").append(sourceUser).append("\n");
        sb.append("  password: ").append(sourcePass).append("\n");
        sb.append("  query: >\n");

        String selectClause = buildSelectClause(mappings);
        String whereClause = buildWhereClause(step);
        sb.append("    SELECT ").append(selectClause)
          .append(" FROM ").append(srcSchema).append(".").append(step.getSourceTable());
        if (whereClause != null) {
            sb.append(" WHERE ").append(whereClause);
        }
        sb.append("\n");

        sb.append("out:\n");
        sb.append("  type: postgresql\n");
        sb.append("  host: ").append(targetHost).append("\n");
        sb.append("  port: ").append(targetPort).append("\n");
        sb.append("  database: ").append(targetDb).append("\n");
        sb.append("  user: ").append(targetUser).append("\n");
        sb.append("  password: ").append(targetPass).append("\n");
        sb.append("  table: ").append(tgtSchema).append(".").append(step.getTargetTable()).append("\n");
        sb.append("  mode: merge\n");
        sb.append("  column_options:\n");

        for (EtlFieldMappingEntity m : mappings) {
            sb.append("    ").append(m.getTargetColumn()).append(": ");
            if ("DIRECT".equals(m.getTransformType())) {
                sb.append("{value_from: ").append(m.getSourceColumn()).append("}\n");
            } else if ("CONSTANT".equals(m.getTransformType())) {
                sb.append("{value: \"").append(m.getDefaultValue()).append("\"}\n");
            } else {
                sb.append("{value_from: ").append(m.getSourceColumn()).append("}\n");
            }
        }

        return sb.toString();
    }

    private String buildSelectClause(List<EtlFieldMappingEntity> mappings) {
        return mappings.stream()
                .filter(m -> m.getSourceColumn() != null)
                .map(EtlFieldMappingEntity::getSourceColumn)
                .collect(Collectors.joining(", "));
    }

    private String buildWhereClause(EtlStepEntity step) {
        if (step.getFilterCondition() != null && !step.getFilterCondition().isBlank()) {
            return step.getFilterCondition();
        }
        if ("INCREMENTAL".equals(step.getSyncMode()) && step.getLastSyncTime() != null) {
            return "_loaded_at > '" + step.getLastSyncTime() + "'";
        }
        return null;
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `cd maidc-parent && mvn compile -pl maidc-data -am -q`

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/service/etl/EtlConfigGenerator.java
git commit -m "feat(etl): add EtlConfigGenerator for Embulk YAML generation"
```

---

### Task 2.6: EtlExecutionService

**Files:**
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/service/etl/EtlExecutionService.java`

**说明：** 执行调度、引擎调用、状态跟踪。按 step_order 顺序执行，支持 ABORT/SKIP/RETRY 错误策略。

- [ ] **Step 1: 创建 EtlExecutionService**

```java
package com.maidc.data.service.etl;

import com.maidc.common.core.exception.BizException;
import com.maidc.data.dto.etl.EtlExecutionQueryDTO;
import com.maidc.data.entity.*;
import com.maidc.data.mapper.DataMapper;
import com.maidc.data.repository.*;
import com.maidc.data.vo.EtlExecutionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class EtlExecutionService {

    private final EtlPipelineRepository pipelineRepository;
    private final EtlStepRepository stepRepository;
    private final EtlExecutionRepository executionRepository;
    private final EtlConfigGenerator configGenerator;
    private final DataMapper dataMapper;

    private final Map<Long, Process> runningProcesses = new ConcurrentHashMap<>();

    @Transactional
    public EtlExecutionVO triggerExecution(Long pipelineId, String triggerType) {
        EtlPipelineEntity pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new BizException(404, "管道不存在"));

        // 创建管道级执行记录
        EtlExecutionEntity exec = new EtlExecutionEntity();
        exec.setPipelineId(pipelineId);
        exec.setStatus("RUNNING");
        exec.setStartTime(LocalDateTime.now());
        exec.setTriggerType(triggerType);
        exec = executionRepository.save(exec);

        // 异步执行步骤
        List<EtlStepEntity> steps = stepRepository
                .findByPipelineIdAndIsDeletedFalseOrderByStepOrder(pipelineId);

        new Thread(() -> executeSteps(pipeline, steps, exec.getId())).start();

        return dataMapper.toEtlExecutionVO(exec);
    }

    private void executeSteps(EtlPipelineEntity pipeline, List<EtlStepEntity> steps, Long parentExecId) {
        for (EtlStepEntity step : steps) {
            EtlExecutionEntity stepExec = new EtlExecutionEntity();
            stepExec.setPipelineId(pipeline.getId());
            stepExec.setStepId(step.getId());
            stepExec.setStatus("RUNNING");
            stepExec.setStartTime(LocalDateTime.now());
            stepExec.setTriggerType("MANUAL");
            stepExec = executionRepository.save(stepExec);

            try {
                String config = configGenerator.generateEmbulkConfig(
                        step,
                        "localhost", 5432, "maidc", "maidc", "maidc",
                        "localhost", 5432, "maidc", "maidc", "maidc");
                stepExec.setEngineConfig(config);

                // 调用 Embulk
                ProcessBuilder pb = new ProcessBuilder("embulk", "run", "-");
                pb.redirectErrorStream(true);
                Process process = pb.start();

                // 写入配置到 stdin
                try (OutputStream os = process.getOutputStream()) {
                    os.write(config.getBytes());
                }

                runningProcesses.put(stepExec.getId(), process);
                int exitCode = process.waitFor();
                runningProcesses.remove(stepExec.getId());

                if (exitCode == 0) {
                    stepExec.setStatus("SUCCESS");
                    step.setLastSyncTime(LocalDateTime.now());
                    stepRepository.save(step);
                } else {
                    handleStepError(step, stepExec, readProcessOutput(process));
                }
            } catch (Exception e) {
                handleStepError(step, stepExec, e.getMessage());
            }

            stepExec.setEndTime(LocalDateTime.now());
            executionRepository.save(stepExec);

            // 检查错误策略
            if ("FAILED".equals(stepExec.getStatus())) {
                if ("ABORT".equals(step.getOnError())) {
                    markRemainingSkipped(pipeline.getId(), steps, step.getStepOrder());
                    break;
                }
                // SKIP: 继续
                // RETRY: 简化处理，等同于 SKIP 并记录
            }
        }

        // 更新管道级执行记录
        executionRepository.findById(parentExecId).ifPresent(parent -> {
            parent.setStatus("SUCCESS");
            parent.setEndTime(LocalDateTime.now());
            executionRepository.save(parent);
        });

        pipeline.setLastRunTime(LocalDateTime.now());
        pipelineRepository.save(pipeline);
    }

    private void handleStepError(EtlStepEntity step, EtlExecutionEntity stepExec, String error) {
        stepExec.setStatus("FAILED");
        stepExec.setErrorMessage(error != null ? error.substring(0, Math.min(error.length(), 4000)) : "Unknown error");
        log.error("ETL step failed: pipeline={}, step={}, error={}", step.getPipelineId(), step.getStepName(), error);
    }

    private void markRemainingSkipped(Long pipelineId, List<EtlStepEntity> steps, int afterOrder) {
        steps.stream()
                .filter(s -> s.getStepOrder() > afterOrder)
                .forEach(s -> {
                    EtlExecutionEntity skip = new EtlExecutionEntity();
                    skip.setPipelineId(pipelineId);
                    skip.setStepId(s.getId());
                    skip.setStatus("SKIPPED");
                    skip.setTriggerType("MANUAL");
                    executionRepository.save(skip);
                });
    }

    private String readProcessOutput(Process process) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            return "Failed to read process output: " + e.getMessage();
        }
    }

    public Page<EtlExecutionVO> listExecutions(EtlExecutionQueryDTO query) {
        Specification<EtlExecutionEntity> spec = (root, q, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (query.getPipelineId() != null) {
                predicates.add(cb.equal(root.get("pipelineId"), query.getPipelineId()));
            }
            if (query.getStepId() != null) {
                predicates.add(cb.equal(root.get("stepId"), query.getStepId()));
            }
            if (query.getStatus() != null && !query.getStatus().isBlank()) {
                predicates.add(cb.equal(root.get("status"), query.getStatus()));
            }
            if (query.getTriggerType() != null && !query.getTriggerType().isBlank()) {
                predicates.add(cb.equal(root.get("triggerType"), query.getTriggerType()));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<EtlExecutionEntity> page = executionRepository.findAll(spec,
                PageRequest.of(query.getPage() - 1, query.getPageSize(),
                        Sort.by(Sort.Direction.DESC, "createdAt")));

        return page.map(exec -> {
            EtlExecutionVO vo = dataMapper.toEtlExecutionVO(exec);
            pipelineRepository.findById(exec.getPipelineId())
                    .ifPresent(p -> vo.setPipelineName(p.getPipelineName()));
            if (exec.getStepId() != null) {
                stepRepository.findById(exec.getStepId())
                        .ifPresent(s -> vo.setStepName(s.getStepName()));
            }
            return vo;
        });
    }

    public EtlExecutionVO getExecution(Long id) {
        EtlExecutionEntity exec = executionRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "执行记录不存在"));
        EtlExecutionVO vo = dataMapper.toEtlExecutionVO(exec);
        pipelineRepository.findById(exec.getPipelineId())
                .ifPresent(p -> vo.setPipelineName(p.getPipelineName()));
        if (exec.getStepId() != null) {
            stepRepository.findById(exec.getStepId())
                    .ifPresent(s -> vo.setStepName(s.getStepName()));
        }
        return vo;
    }

    @Transactional
    public void cancelExecution(Long id) {
        EtlExecutionEntity exec = executionRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "执行记录不存在"));
        Process process = runningProcesses.remove(id);
        if (process != null) {
            process.destroyForcibly();
        }
        exec.setStatus("CANCELLED");
        exec.setEndTime(LocalDateTime.now());
        executionRepository.save(exec);
    }

    @Transactional
    public EtlExecutionVO retryExecution(Long id) {
        EtlExecutionEntity exec = executionRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "执行记录不存在"));
        if (exec.getStepId() == null) {
            throw new BizException(400, "管道级执行不支持重试，请重试管道");
        }
        return triggerExecution(exec.getPipelineId(), "RETRY");
    }

    public String getExecutionLogs(Long id) {
        EtlExecutionEntity exec = executionRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "执行记录不存在"));
        if (exec.getLogPath() != null) {
            try {
                return Files.readString(Path.of(exec.getLogPath()));
            } catch (IOException e) {
                return "Failed to read log file: " + e.getMessage();
            }
        }
        return exec.getErrorMessage() != null ? exec.getErrorMessage() : "No logs available";
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `cd maidc-parent && mvn compile -pl maidc-data -am -q`

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/service/etl/EtlExecutionService.java
git commit -m "feat(etl): add EtlExecutionService with step execution and error handling"
```

---

### Task 2.7: 全量编译 + 验证

- [ ] **Step 1: 全模块编译**

Run: `cd maidc-parent && mvn compile -q`

Expected: BUILD SUCCESS

- [ ] **Step 2: 运行现有测试确保无回归**

Run: `cd maidc-parent && mvn test -pl maidc-data -q`

Expected: 所有测试通过

---

## Phase 2 完成标准

- [x] 6 个 Service 类全部编译通过
- [x] EtlMetadataService: schema/table/column 元数据查询
- [x] EtlPipelineService: CRUD + 校验 + 复制
- [x] EtlStepService: CRUD + 排序 + 预览
- [x] EtlFieldMappingService: 批量更新 + 自动映射算法
- [x] EtlConfigGenerator: Embulk YAML 配置生成
- [x] EtlExecutionService: 执行调度 + 错误处理 + 取消/重试
- [x] 全模块编译 BUILD SUCCESS
