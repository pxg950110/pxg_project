# 05: 异步导入管道

> **前置:** 01-ddl-core 完成
> **产出:** CSV/Excel 文件上传 → 异步解析 → 批量导入 → 进度查询

---

### Task 1: 导入任务 DDL

**Files:**
- Modify: `docker/init-db/17-masterdata.sql`

- [ ] **Step 1: 追加导入任务表**

```sql
-- 导入任务表（复用 rdr schema 或建在 masterdata）
CREATE TABLE masterdata.m_import_task (
    id              BIGSERIAL    PRIMARY KEY,
    code_system_id  BIGINT       NOT NULL,
    file_name       VARCHAR(256) NOT NULL,
    file_path       VARCHAR(512),
    total_rows      INT          DEFAULT 0,
    processed_rows  INT          DEFAULT 0,
    failed_rows     INT          DEFAULT 0,
    status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    error_message   TEXT,
    created_by      VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id          BIGINT       NOT NULL DEFAULT 0
);
COMMENT ON TABLE masterdata.m_import_task IS '主数据导入任务';
```

- [ ] **Step 2: Commit**

```bash
git add docker/init-db/17-masterdata.sql
git commit -m "feat(masterdata): add import task DDL"
```

---

### Task 2: ImportTask Entity + Repository

**Files:**
- Create: `entity/ImportTaskEntity.java`
- Create: `repository/ImportTaskRepository.java`

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
import org.hibernate.annotations.Where;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "m_import_task", schema = "masterdata")
@Where(clause = "is_deleted = false")
public class ImportTaskEntity extends BaseEntity {

    @Column(name = "code_system_id", nullable = false)
    private Long codeSystemId;

    @Column(name = "file_name", nullable = false, length = 256)
    private String fileName;

    @Column(name = "file_path", length = 512)
    private String filePath;

    @Column(name = "total_rows")
    private Integer totalRows = 0;

    @Column(name = "processed_rows")
    private Integer processedRows = 0;

    @Column(name = "failed_rows")
    private Integer failedRows = 0;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "PENDING";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
```

- [ ] **Step 2: Repository**

```java
package com.maidc.data.repository;

import com.maidc.data.entity.ImportTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportTaskRepository extends JpaRepository<ImportTaskEntity, Long> {
}
```

- [ ] **Step 3: Commit**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/{entity/ImportTaskEntity.java,repository/ImportTaskRepository.java}
git commit -m "feat(masterdata): add import task entity and repository"
```

---

### Task 3: MasterDataImportService + Controller

**Files:**
- Create: `service/MasterDataImportService.java`
- Create: `controller/MasterDataImportController.java`

- [ ] **Step 1: Service**

```java
package com.maidc.data.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.ConceptEntity;
import com.maidc.data.entity.ImportTaskEntity;
import com.maidc.data.repository.ConceptRepository;
import com.maidc.data.repository.ImportTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MasterDataImportService {

    private final ImportTaskRepository importTaskRepository;
    private final ConceptRepository conceptRepository;

    @Transactional
    public ImportTaskEntity uploadAndCreateTask(MultipartFile file, Long codeSystemId) {
        String fileName = file.getOriginalFilename();

        ImportTaskEntity task = new ImportTaskEntity();
        task.setCodeSystemId(codeSystemId);
        task.setFileName(fileName);
        task.setStatus("PENDING");
        task = importTaskRepository.save(task);

        processAsync(task.getId(), file, codeSystemId);

        return task;
    }

    public ImportTaskEntity getTaskStatus(Long taskId) {
        return importTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND));
    }

    @Async
    @Transactional
    public void processAsync(Long taskId, MultipartFile file, Long codeSystemId) {
        ImportTaskEntity task = importTaskRepository.findById(taskId).orElse(null);
        if (task == null) return;

        task.setStatus("PROCESSING");
        importTaskRepository.save(task);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String header = reader.readLine();
            if (header == null) {
                task.setStatus("FAILED");
                task.setErrorMessage("空文件");
                importTaskRepository.save(task);
                return;
            }

            String[] columns = header.split(",");
            String line;
            int total = 0, processed = 0, failed = 0;
            List<ConceptEntity> batch = new ArrayList<>();
            int batchSize = 500;

            while ((line = reader.readLine()) != null) {
                total++;
                try {
                    String[] values = line.split(",", -1);
                    Map<String, String> row = new HashMap<>();
                    for (int i = 0; i < columns.length && i < values.length; i++) {
                        row.put(columns[i].trim(), values[i].trim());
                    }

                    ConceptEntity concept = new ConceptEntity();
                    concept.setCodeSystemId(codeSystemId);
                    concept.setConceptCode(row.getOrDefault("concept_code", ""));
                    concept.setName(row.getOrDefault("name", ""));
                    concept.setNameEn(row.getOrDefault("name_en", null));
                    concept.setDomain(row.getOrDefault("domain", null));
                    concept.setStatus("ACTIVE");

                    batch.add(concept);
                    processed++;

                    if (batch.size() >= batchSize) {
                        conceptRepository.saveAll(batch);
                        batch.clear();
                        task.setProcessedRows(processed);
                        task.setTotalRows(total);
                        importTaskRepository.save(task);
                    }
                } catch (Exception e) {
                    failed++;
                    log.warn("导入第{}行失败: {}", total, e.getMessage());
                }
            }

            if (!batch.isEmpty()) {
                conceptRepository.saveAll(batch);
            }

            task.setTotalRows(total);
            task.setProcessedRows(processed);
            task.setFailedRows(failed);
            task.setStatus("COMPLETED");
            importTaskRepository.save(task);

            log.info("导入完成: taskId={}, total={}, processed={}, failed={}", taskId, total, processed, failed);
        } catch (Exception e) {
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            importTaskRepository.save(task);
            log.error("导入失败: taskId={}", taskId, e);
        }
    }
}
```

- [ ] **Step 2: Controller**

```java
package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.ImportTaskEntity;
import com.maidc.data.service.MasterDataImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/masterdata/import")
@RequiredArgsConstructor
public class MasterDataImportController {

    private final MasterDataImportService importService;

    @PreAuthorize("hasPermission('masterdata:create')")
    @PostMapping("/upload")
    public R<ImportTaskEntity> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("codeSystemId") Long codeSystemId) {
        return R.ok(importService.uploadAndCreateTask(file, codeSystemId));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/tasks/{taskId}")
    public R<ImportTaskEntity> getTaskStatus(@PathVariable Long taskId) {
        return R.ok(importService.getTaskStatus(taskId));
    }
}
```

- [ ] **Step 3: 确保 Spring @Async 启用**

在 `DataApplication.java` 或配置类上添加 `@EnableAsync`。

- [ ] **Step 4: 编译 + Commit**

```bash
cd maidc-parent && mvn compile -pl maidc-data -am -q
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/{service/MasterDataImportService.java,controller/MasterDataImportController.java}
git commit -m "feat(masterdata): add async import pipeline with CSV support"
```
