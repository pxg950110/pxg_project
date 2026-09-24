# Data Element Import Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Excel-based batch import for data elements with multi-sheet support (Sheet1=data elements, Sheet2=allowed values), async processing, and frontend import modal.

**Architecture:** Upload Excel → create import task → @Async POI parse → JDBC batch upsert → frontend polls status. Reuse existing `ImportTaskEntity` / `ImportTaskRepository` from the MasterDataImport module.

**Tech Stack:** Apache POI (Excel parsing), Spring @Async, JDBC batch, Ant Design Vue (frontend modal)

---

## File Map

### Backend (maidc-parent/maidc-data)
| Action | File | Responsibility |
|--------|------|----------------|
| Modify | `pom.xml` | Add poi-ooxml dependency |
| Create | `dto/DataElementImportDTO.java` | Excel row → DTO mapping |
| Create | `service/DataElementImportService.java` | Parse Excel, validate, batch write |
| Modify | `controller/DataElementController.java` | Add import/template/task endpoints |
| Modify | `entity/ImportTaskEntity.java` | Add `taskType` field to distinguish import types |
| Modify | `repository/ImportTaskRepository.java` | (no change needed — existing is sufficient) |

### DDL
| Action | File | Responsibility |
|--------|------|----------------|
| Modify | `docker/init-db/17-masterdata.sql` | Add `task_type` column to m_import_task |

### Frontend (maidc-portal)
| Action | File | Responsibility |
|--------|------|----------------|
| Create | `src/views/masterdata/DataElementImportModal.vue` | Import modal with upload, progress, result |
| Modify | `src/api/masterdata.ts` | Add import API functions |
| Modify | `src/views/masterdata/DataElementList.vue` | Add import button + modal integration |

---

## Task 1: Add POI dependency + DDL migration

**Files:**
- Modify: `maidc-parent/maidc-data/pom.xml`
- Modify: `docker/init-db/17-masterdata.sql`

- [ ] **Step 1: Add poi-ooxml to maidc-data/pom.xml**

Add before the `<!-- Test Dependencies -->` block:

```xml
        <!-- Apache POI (Excel import) -->
        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml</artifactId>
            <version>5.2.5</version>
        </dependency>
```

- [ ] **Step 2: Add task_type column to m_import_task DDL**

In `docker/init-db/17-masterdata.sql`, after the `file_name` column definition (line ~199), add:

```sql
    task_type       VARCHAR(32)  NOT NULL DEFAULT 'CONCEPT',
```

And after the COMMENT line for m_import_task (line ~214), add:

```sql
COMMENT ON COLUMN masterdata.m_import_task.task_type IS '导入类型: CONCEPT, DATA_ELEMENT';
```

- [ ] **Step 3: Run DDL on running database**

```bash
docker exec maidc-postgres psql -U maidc -d maidc -c "ALTER TABLE masterdata.m_import_task ADD COLUMN IF NOT EXISTS task_type VARCHAR(32) NOT NULL DEFAULT 'CONCEPT';"
```

Expected: `ALTER TABLE`

- [ ] **Step 4: Commit**

```bash
git add maidc-parent/maidc-data/pom.xml docker/init-db/17-masterdata.sql
git commit -m "feat(data-element): add POI dependency and task_type DDL for import"
```

---

## Task 2: Add taskType field to ImportTaskEntity

**Files:**
- Modify: `maidc-parent/maidc-data/src/main/java/com/maidc/data/entity/ImportTaskEntity.java`

- [ ] **Step 1: Add taskType field**

Add after the `codeSystemId` field:

```java
    @Column(name = "task_type", nullable = false, length = 32)
    private String taskType = "CONCEPT";
```

- [ ] **Step 2: Commit**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/entity/ImportTaskEntity.java
git commit -m "feat(data-element): add taskType field to ImportTaskEntity"
```

---

## Task 3: Create DataElementImportDTO

**Files:**
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/dto/DataElementImportDTO.java`

- [ ] **Step 1: Create the DTO**

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
public class DataElementImportDTO {
    private String elementCode;
    private String name;
    private String nameEn;
    private String definition;
    private String objectClassName;
    private String objectClassId;
    private String propertyName;
    private String propertyId;
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
    /** Row number in the Excel file (1-based) for error reporting */
    private int rowNumber;
}
```

- [ ] **Step 2: Commit**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/dto/DataElementImportDTO.java
git commit -m "feat(data-element): add DataElementImportDTO"
```

---

## Task 4: Create DataElementImportService

**Files:**
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/service/DataElementImportService.java`

- [ ] **Step 1: Write the service**

```java
package com.maidc.data.service;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.dto.DataElementImportDTO;
import com.maidc.data.entity.DataElementEntity;
import com.maidc.data.entity.DataElementValueEntity;
import com.maidc.data.entity.ImportTaskEntity;
import com.maidc.data.repository.DataElementRepository;
import com.maidc.data.repository.DataElementValueRepository;
import com.maidc.data.repository.ImportTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataElementImportService {

    private final ImportTaskRepository importTaskRepository;
    private final DataElementRepository dataElementRepository;
    private final DataElementValueRepository valueRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final int BATCH_SIZE = 500;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    // ── Column header mapping for Sheet1 (data elements) ──
    private static final Map<String, String> HEADER_MAP = new LinkedHashMap<>();
    static {
        HEADER_MAP.put("标识符", "elementCode");
        HEADER_MAP.put("规范名称", "name");
        HEADER_MAP.put("英文名称", "nameEn");
        HEADER_MAP.put("定义", "definition");
        HEADER_MAP.put("对象类", "objectClassName");
        HEADER_MAP.put("对象类ID", "objectClassId");
        HEADER_MAP.put("特性", "propertyName");
        HEADER_MAP.put("特性ID", "propertyId");
        HEADER_MAP.put("数据类型", "dataType");
        HEADER_MAP.put("表示类", "representationClass");
        HEADER_MAP.put("值域名称", "valueDomainName");
        HEADER_MAP.put("值域ID", "valueDomainId");
        HEADER_MAP.put("最小长度", "minLength");
        HEADER_MAP.put("最大长度", "maxLength");
        HEADER_MAP.put("格式", "format");
        HEADER_MAP.put("计量单位", "unitOfMeasure");
        HEADER_MAP.put("分类", "category");
        HEADER_MAP.put("标准来源", "standardSource");
        HEADER_MAP.put("注册状态", "registrationStatus");
        HEADER_MAP.put("版本", "version");
    }

    // ── Upload and start async import ──

    @Transactional
    public ImportTaskEntity uploadAndStart(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(400, "上传文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(400, "文件大小不能超过10MB");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".xlsx")) {
            throw new BusinessException(400, "仅支持 .xlsx 格式文件");
        }

        ImportTaskEntity task = new ImportTaskEntity();
        task.setTaskType("DATA_ELEMENT");
        task.setCodeSystemId(0L);
        task.setFileName(fileName);
        task.setStatus("PENDING");
        task.setTotalRows(0);
        task.setProcessedRows(0);
        task.setFailedRows(0);
        task.setOrgId(0L);
        task = importTaskRepository.save(task);

        processAsync(task.getId(), file);

        return task;
    }

    public ImportTaskEntity getTaskStatus(Long taskId) {
        return importTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(404, "导入任务不存在: " + taskId));
    }

    // ── Async processing ──

    @Async
    public void processAsync(Long taskId, MultipartFile file) {
        ImportTaskEntity task = null;
        try {
            task = importTaskRepository.findById(taskId)
                    .orElseThrow(() -> new BusinessException(404, "导入任务不存在"));
            task.setStatus("PROCESSING");
            importTaskRepository.save(task);

            Workbook workbook = WorkbookFactory.create(file.getInputStream());

            // Phase 1: Parse and import data elements from Sheet1
            int[] elementStats = processSheet1(workbook, task);
            int totalRows = elementStats[0];
            int successCount = elementStats[1];
            int failCount = elementStats[2];

            // Phase 2: Parse and import allowed values from Sheet2
            int valueCount = 0;
            if (workbook.getNumberOfSheets() >= 2) {
                valueCount = processSheet2(workbook);
            }

            task.setTotalRows(totalRows);
            task.setProcessedRows(successCount);
            task.setFailedRows(failCount);
            task.setStatus("COMPLETED");
            importTaskRepository.save(task);

            log.info("数据元导入完成: taskId={}, 成功={}, 失败={}, 允许值={}",
                    taskId, successCount, failCount, valueCount);

            workbook.close();
        } catch (Exception e) {
            log.error("数据元导入失败: taskId={}", taskId, e);
            if (task != null) {
                task.setStatus("FAILED");
                String msg = e.getMessage();
                task.setErrorMessage(msg != null && msg.length() > 2000
                        ? msg.substring(0, 2000) : msg);
                importTaskRepository.save(task);
            }
        }
    }

    // ── Sheet1: Data Elements ──

    private int[] processSheet1(Workbook workbook, ImportTaskEntity task) {
        Sheet sheet = workbook.getSheetAt(0);
        if (sheet.getPhysicalNumberOfRows() < 2) {
            return new int[]{0, 0, 0};
        }

        // Parse header row to build column index mapping
        Row headerRow = sheet.getRow(0);
        Map<String, Integer> colIndex = buildColumnIndex(headerRow);

        // Validate required columns exist
        if (!colIndex.containsKey("elementCode") || !colIndex.containsKey("name")
                || !colIndex.containsKey("definition") || !colIndex.containsKey("dataType")) {
            throw new BusinessException(400,
                    "Excel缺少必需列: 标识符(elementCode), 规范名称(name), 定义(definition), 数据类型(dataType)");
        }

        int totalRows = 0;
        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();

        List<DataElementEntity> batch = new ArrayList<>(BATCH_SIZE);

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isEmptyRow(row)) continue;
            totalRows++;

            try {
                DataElementEntity entity = mapRowToEntity(row, colIndex, i + 1);
                batch.add(entity);
                successCount++;

                if (batch.size() >= BATCH_SIZE) {
                    batchUpsertElements(batch);
                    batch.clear();
                    updateTaskProgress(task, totalRows, successCount, failCount);
                }
            } catch (Exception e) {
                failCount++;
                if (errors.size() < 100) {
                    errors.add("行" + (i + 1) + ": " + e.getMessage());
                }
                log.warn("数据元行解析失败, 行号={}: {}", i + 1, e.getMessage());
            }
        }

        if (!batch.isEmpty()) {
            batchUpsertElements(batch);
        }

        if (!errors.isEmpty()) {
            task.setErrorMessage(String.join("\n", errors));
        }

        return new int[]{totalRows, successCount, failCount};
    }

    // ── Sheet2: Allowed Values ──

    private int processSheet2(Workbook workbook) {
        Sheet sheet = workbook.getSheetAt(1);
        if (sheet.getPhysicalNumberOfRows() < 2) return 0;

        Row headerRow = sheet.getRow(0);
        int colElementCode = findColumn(headerRow, "标识符");
        int colValueCode = findColumn(headerRow, "值编码");
        int colValueMeaning = findColumn(headerRow, "值含义");
        int colSortOrder = findColumn(headerRow, "排序");

        if (colElementCode < 0 || colValueCode < 0 || colValueMeaning < 0) {
            log.warn("Sheet2(允许值)缺少必需列: 标识符, 值编码, 值含义");
            return 0;
        }

        // Group values by element_code
        Map<String, List<DataElementValueEntity>> valueMap = new LinkedHashMap<>();
        int count = 0;

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isEmptyRow(row)) continue;

            String elementCode = getStringValue(row, colElementCode);
            String valueCode = getStringValue(row, colValueCode);
            String valueMeaning = getStringValue(row, colValueMeaning);

            if (elementCode == null || valueCode == null || valueMeaning == null) continue;

            DataElementValueEntity value = new DataElementValueEntity();
            value.setValueCode(valueCode);
            value.setValueMeaning(valueMeaning);
            value.setSortOrder(colSortOrder >= 0 ? getIntValue(row, colSortOrder, count) : count);
            value.setOrgId(0L);

            valueMap.computeIfAbsent(elementCode, k -> new ArrayList<>()).add(value);
            count++;
        }

        // Batch write: resolve element_code → data_element_id, then insert values
        batchUpsertValues(valueMap);

        return count;
    }

    // ── JDBC Batch Upsert: Data Elements ──

    private void batchUpsertElements(List<DataElementEntity> batch) {
        String sql = """
            INSERT INTO masterdata.m_data_element
                (element_code, name, name_en, definition, object_class_name, object_class_id,
                 property_name, property_id, data_type, representation_class,
                 value_domain_name, value_domain_id, min_length, max_length, format,
                 unit_of_measure, category, standard_source, registration_status, version,
                 status, created_by, created_at, is_deleted, org_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', 'system', NOW(), false, 0)
            ON CONFLICT (element_code) DO UPDATE SET
                name = EXCLUDED.name,
                name_en = EXCLUDED.name_en,
                definition = EXCLUDED.definition,
                object_class_name = EXCLUDED.object_class_name,
                object_class_id = EXCLUDED.object_class_id,
                property_name = EXCLUDED.property_name,
                property_id = EXCLUDED.property_id,
                data_type = EXCLUDED.data_type,
                representation_class = EXCLUDED.representation_class,
                value_domain_name = EXCLUDED.value_domain_name,
                value_domain_id = EXCLUDED.value_domain_id,
                min_length = EXCLUDED.min_length,
                max_length = EXCLUDED.max_length,
                format = EXCLUDED.format,
                unit_of_measure = EXCLUDED.unit_of_measure,
                category = EXCLUDED.category,
                standard_source = EXCLUDED.standard_source,
                registration_status = EXCLUDED.registration_status,
                version = EXCLUDED.version,
                updated_by = 'system',
                updated_at = NOW()
            """;

        jdbcTemplate.batchUpdate(sql, batch, BATCH_SIZE, (ps, e) -> {
            ps.setString(1, e.getElementCode());
            ps.setString(2, e.getName());
            ps.setString(3, e.getNameEn());
            ps.setString(4, e.getDefinition());
            ps.setString(5, e.getObjectClassName());
            ps.setString(6, e.getObjectClassId());
            ps.setString(7, e.getPropertyName());
            ps.setString(8, e.getPropertyId());
            ps.setString(9, e.getDataType());
            ps.setString(10, e.getRepresentationClass());
            ps.setString(11, e.getValueDomainName());
            ps.setString(12, e.getValueDomainId());
            setNullableInt(ps, 13, e.getMaxLength() != null ? e.getMaxLength() : e.getMinLength());
            setNullableInt(ps, 14, e.getMaxLength());
            ps.setString(15, e.getFormat());
            ps.setString(16, e.getUnitOfMeasure());
            ps.setString(17, e.getCategory());
            ps.setString(18, e.getStandardSource());
            ps.setString(19, e.getRegistrationStatus() != null ? e.getRegistrationStatus() : "DRAFT");
            ps.setString(20, e.getVersion() != null ? e.getVersion() : "1.0");
        });
    }

    // min_length maps to column index 13 in the SQL
    // Fix: index 13 = min_length, 14 = max_length
    // Redo the lambda properly:
    // Actually the SQL has min_length at position 13, max_length at 14.
    // The setNullableInt call above is wrong — fixing below in the complete service.

    // ── JDBC Batch Upsert: Allowed Values ──

    private void batchUpsertValues(Map<String, List<DataElementValueEntity>> valueMap) {
        // Resolve element_code → id
        Map<String, Long> codeToId = new HashMap<>();
        List<String> codes = new ArrayList<>(valueMap.keySet());
        for (int i = 0; i < codes.size(); i += BATCH_SIZE) {
            List<String> sub = codes.subList(i, Math.min(i + BATCH_SIZE, codes.size()));
            String placeholders = String.join(",", Collections.nCopies(sub.size(), "?"));
            String sql = "SELECT element_code, id FROM masterdata.m_data_element WHERE element_code IN ("
                    + placeholders + ") AND is_deleted = false";
            jdbcTemplate.query(sql, rs -> {
                codeToId.put(rs.getString("element_code"), rs.getLong("id"));
            }, sub.toArray());
        }

        // Delete existing values and batch insert new ones
        String insertSql = """
            INSERT INTO masterdata.m_data_element_value
                (data_element_id, value_code, value_meaning, sort_order, created_by, created_at, is_deleted, org_id)
            VALUES (?, ?, ?, ?, 'system', NOW(), false, 0)
            """;

        List<Object[]> batchArgs = new ArrayList<>();
        for (Map.Entry<String, List<DataElementValueEntity>> entry : valueMap.entrySet()) {
            Long elementId = codeToId.get(entry.getKey());
            if (elementId == null) {
                log.warn("允许值引用的数据元不存在: {}", entry.getKey());
                continue;
            }

            // Soft-delete existing values for this element
            jdbcTemplate.update(
                    "UPDATE masterdata.m_data_element_value SET is_deleted = true WHERE data_element_id = ?",
                    elementId);

            for (DataElementValueEntity v : entry.getValue()) {
                batchArgs.add(new Object[]{elementId, v.getValueCode(), v.getValueMeaning(), v.getSortOrder()});
            }

            if (batchArgs.size() >= BATCH_SIZE) {
                jdbcTemplate.batchUpdate(insertSql, batchArgs);
                batchArgs.clear();
            }
        }

        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(insertSql, batchArgs);
        }
    }

    // ── Template Download ──

    public byte[] generateTemplate() {
        try (Workbook wb = new XSSFWorkbook()) {
            // Sheet1: Data Elements
            Sheet sheet1 = wb.createSheet("数据元");
            Row header1 = sheet1.createRow(0);
            String[] headers1 = HEADER_MAP.keySet().toArray(new String[0]);
            for (int i = 0; i < headers1.length; i++) {
                Cell cell = header1.createCell(i);
                cell.setCellValue(headers1[i]);
            }

            // Add a sample row
            Row sample = sheet1.createRow(1);
            sample.createCell(0).setCellValue("DE04.50.001");
            sample.createCell(1).setCellValue("患者姓名");
            sample.createCell(2).setCellValue("Patient Name");
            sample.createCell(3).setCellValue("患者在户籍登记时所用的姓名");
            sample.createCell(4).setCellValue("患者");
            sample.createCell(8).setCellValue("ST");
            sample.createCell(12).setCellValue(1);
            sample.createCell(13).setCellValue(50);
            sample.createCell(16).setCellValue("人口学信息");
            sample.createCell(17).setCellValue("WS363");
            sample.createCell(18).setCellValue("PUBLISHED");
            sample.createCell(19).setCellValue("1.0");

            // Auto-size columns
            for (int i = 0; i < headers1.length; i++) {
                sheet1.autoSizeColumn(i);
            }

            // Sheet2: Allowed Values
            Sheet sheet2 = wb.createSheet("允许值");
            Row header2 = sheet2.createRow(0);
            header2.createCell(0).setCellValue("标识符");
            header2.createCell(1).setCellValue("值编码");
            header2.createCell(2).setCellValue("值含义");
            header2.createCell(3).setCellValue("排序");

            Row sampleVal = sheet2.createRow(1);
            sampleVal.createCell(0).setCellValue("DE04.50.005");
            sampleVal.createCell(1).setCellValue("1");
            sampleVal.createCell(2).setCellValue("男");
            sampleVal.createCell(3).setCellValue("0");
            Row sampleVal2 = sheet2.createRow(2);
            sampleVal2.createCell(0).setCellValue("DE04.50.005");
            sampleVal2.createCell(1).setCellValue("2");
            sampleVal2.createCell(2).setCellValue("女");
            sampleVal2.createCell(3).setCellValue("1");

            for (int i = 0; i < 4; i++) {
                sheet2.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(500, "生成模板失败: " + e.getMessage());
        }
    }

    // ── Helpers ──

    private Map<String, Integer> buildColumnIndex(Row headerRow) {
        Map<String, Integer> colIndex = new HashMap<>();
        for (Cell cell : headerRow) {
            String header = cell.getStringCellValue().trim();
            String field = HEADER_MAP.get(header);
            if (field != null) {
                colIndex.put(field, cell.getColumnIndex());
            }
        }
        return colIndex;
    }

    private DataElementEntity mapRowToEntity(Row row, Map<String, Integer> colIndex, int rowNumber) {
        String elementCode = getColumnString(row, colIndex, "elementCode");
        String name = getColumnString(row, colIndex, "name");
        String definition = getColumnString(row, colIndex, "definition");
        String dataType = getColumnString(row, colIndex, "dataType");

        if (elementCode == null || elementCode.isBlank()) {
            throw new IllegalArgumentException("标识符不能为空");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("规范名称不能为空");
        }
        if (dataType == null || dataType.isBlank()) {
            throw new IllegalArgumentException("数据类型不能为空");
        }

        DataElementEntity entity = new DataElementEntity();
        entity.setElementCode(elementCode.trim());
        entity.setName(name.trim());
        entity.setNameEn(getColumnString(row, colIndex, "nameEn"));
        entity.setDefinition(definition != null ? definition.trim() : "");
        entity.setObjectClassName(getColumnString(row, colIndex, "objectClassName"));
        entity.setObjectClassId(getColumnString(row, colIndex, "objectClassId"));
        entity.setPropertyName(getColumnString(row, colIndex, "propertyName"));
        entity.setPropertyId(getColumnString(row, colIndex, "propertyId"));
        entity.setDataType(dataType.trim());
        entity.setRepresentationClass(getColumnString(row, colIndex, "representationClass"));
        entity.setValueDomainName(getColumnString(row, colIndex, "valueDomainName"));
        entity.setValueDomainId(getColumnString(row, colIndex, "valueDomainId"));
        entity.setMinLength(getColumnInt(row, colIndex, "minLength"));
        entity.setMaxLength(getColumnInt(row, colIndex, "maxLength"));
        entity.setFormat(getColumnString(row, colIndex, "format"));
        entity.setUnitOfMeasure(getColumnString(row, colIndex, "unitOfMeasure"));
        entity.setCategory(getColumnString(row, colIndex, "category"));
        entity.setStandardSource(getColumnString(row, colIndex, "standardSource"));
        entity.setRegistrationStatus(
                getOrDefault(getColumnString(row, colIndex, "registrationStatus"), "DRAFT"));
        entity.setVersion(getOrDefault(getColumnString(row, colIndex, "version"), "1.0"));
        entity.setStatus("ACTIVE");
        entity.setOrgId(0L);
        return entity;
    }

    private int findColumn(Row headerRow, String headerName) {
        for (Cell cell : headerRow) {
            if (headerName.equals(cell.getStringCellValue().trim())) {
                return cell.getColumnIndex();
            }
        }
        return -1;
    }

    private String getStringValue(Row row, int colIndex) {
        if (colIndex < 0) return null;
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private int getIntValue(Row row, int colIndex, int defaultValue) {
        if (colIndex < 0) return defaultValue;
        Cell cell = row.getCell(colIndex);
        if (cell == null) return defaultValue;
        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING -> {
                try { yield Integer.parseInt(cell.getStringCellValue().trim()); }
                catch (NumberFormatException e) { yield defaultValue; }
            }
            default -> defaultValue;
        };
    }

    private String getColumnString(Row row, Map<String, Integer> colIndex, String field) {
        Integer idx = colIndex.get(field);
        return idx != null ? getStringValue(row, idx) : null;
    }

    private Integer getColumnInt(Row row, Map<String, Integer> colIndex, String field) {
        Integer idx = colIndex.get(field);
        if (idx == null) return null;
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        }
        return null;
    }

    private boolean isEmptyRow(Row row) {
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                if (cell.getCellType() == CellType.STRING && !cell.getStringCellValue().isBlank()) {
                    return false;
                }
                if (cell.getCellType() == CellType.NUMERIC) {
                    return false;
                }
            }
        }
        return true;
    }

    private void setNullableInt(java.sql.PreparedStatement ps, int paramIndex, Integer value)
            throws java.sql.SQLException {
        if (value != null) {
            ps.setInt(paramIndex, value);
        } else {
            ps.setNull(paramIndex, java.sql.Types.INTEGER);
        }
    }

    private String getOrDefault(String value, String defaultValue) {
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

    private void updateTaskProgress(ImportTaskEntity task, int total, int success, int fail) {
        task.setTotalRows(total);
        task.setProcessedRows(success);
        task.setFailedRows(fail);
        importTaskRepository.save(task);
    }
}
```

Note: the `batchUpsertElements` method's PreparedStatement setter has a bug where `minLength` is set incorrectly. The corrected setter lambda for indices 13/14 should be:

```java
    setNullableInt(ps, 13, e.getMinLength());
    setNullableInt(ps, 14, e.getMaxLength());
```

When creating the file, use this corrected version for those two lines.

- [ ] **Step 2: Commit**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/service/DataElementImportService.java
git commit -m "feat(data-element): add DataElementImportService with Excel parsing and batch upsert"
```

---

## Task 5: Add import endpoints to DataElementController

**Files:**
- Modify: `maidc-parent/maidc-data/src/main/java/com/maidc/data/controller/DataElementController.java`

- [ ] **Step 1: Add imports and inject service**

Add to the existing imports:

```java
import com.maidc.data.service.DataElementImportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
```

Add field injection (after existing `private final DataElementService service;`):

```java
    private final DataElementImportService importService;
```

- [ ] **Step 2: Add three endpoints at the end of the controller class (before the closing brace)**

```java
    // ── 导入 ──

    @PreAuthorize("hasPermission('masterdata:create')")
    @PostMapping(value = "/import", consumes = "multipart/form-data")
    public R<ImportTaskEntity> importExcel(@RequestParam("file") MultipartFile file) {
        return R.ok(importService.uploadAndStart(file));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/import/tasks/{taskId}")
    public R<ImportTaskEntity> getImportTaskStatus(@PathVariable Long taskId) {
        return R.ok(importService.getTaskStatus(taskId));
    }

    @PreAuthorize("hasPermission('masterdata:read')")
    @GetMapping("/import/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] data = importService.generateTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=data-element-template.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
```

- [ ] **Step 3: Commit**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/controller/DataElementController.java
git commit -m "feat(data-element): add import, task status, and template download endpoints"
```

---

## Task 6: Build and verify backend

**Files:** No new files — verification only.

- [ ] **Step 1: Build**

```bash
cd maidc-parent && mvn clean package -DskipTests -pl maidc-data -am
```

Expected: `BUILD SUCCESS`

- [ ] **Step 2: Restart data service**

```bash
# Find and kill existing process
netstat -ano | grep ":8082 " | grep LISTENING | awk '{print $5}' | head -1 | xargs -I{} taskkill //F //PID {}
# Start new one
cd maidc-parent && java -jar maidc-data/target/maidc-data-1.0.0-SNAPSHOT.jar > /tmp/maidc-data.log 2>&1 &
```

- [ ] **Step 3: Verify endpoints**

```bash
# Check template download
curl -s -o /dev/null -w "%{http_code}" http://localhost:8082/api/v1/masterdata/data-elements/import/template
```

Expected: `401` (auth required — confirms endpoint exists)

- [ ] **Step 4: Commit (if any fixes were needed)**

```bash
git add -A && git commit -m "fix(data-element): build fixes for import service"
```

---

## Task 7: Add frontend API functions

**Files:**
- Modify: `maidc-portal/src/api/masterdata.ts`

- [ ] **Step 1: Add three API functions at the end of the file**

```typescript
// Data Element Import
export const importDataElements = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<ApiResponse<any>>('/masterdata/data-elements/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export const getDataElementImportStatus = (taskId: number) =>
  request.get<ApiResponse<any>>(`/masterdata/data-elements/import/tasks/${taskId}`)

export const downloadDataElementTemplate = () =>
  request.get('/masterdata/data-elements/import/template', { responseType: 'blob' })
```

- [ ] **Step 2: Commit**

```bash
git add maidc-portal/src/api/masterdata.ts
git commit -m "feat(data-element): add import API functions"
```

---

## Task 8: Create DataElementImportModal component

**Files:**
- Create: `maidc-portal/src/views/masterdata/DataElementImportModal.vue`

- [ ] **Step 1: Create the modal component**

```vue
<template>
  <a-modal
    v-model:open="visible"
    title="导入数据元"
    :footer="null"
    width="600"
    destroy-on-close
    @cancel="handleClose"
  >
    <!-- Step 1: Upload -->
    <div v-if="step === 'upload'">
      <div style="margin-bottom: 12px; display: flex; justify-content: flex-end">
        <a-button size="small" @click="handleDownloadTemplate">
          <template #icon><DownloadOutlined /></template>
          下载模板
        </a-button>
      </div>
      <a-upload-dragger
        :before-upload="handleBeforeUpload"
        :show-upload-list="false"
        accept=".xlsx"
      >
        <p class="ant-upload-drag-icon"><InboxOutlined /></p>
        <p class="ant-upload-text">点击或拖拽 Excel 文件上传</p>
        <p class="ant-upload-hint">仅支持 .xlsx 格式，文件大小不超过 10MB</p>
      </a-upload-dragger>
      <div v-if="file" style="margin-top: 12px; display: flex; align-items: center; gap: 8px">
        <FileExcelOutlined style="color: #52c41a; font-size: 20px" />
        <span>{{ file.name }}</span>
        <span style="color: #999">({{ (file.size / 1024).toFixed(1) }} KB)</span>
      </div>
      <div style="margin-top: 16px; text-align: right">
        <a-space>
          <a-button @click="handleClose">取消</a-button>
          <a-button type="primary" :disabled="!file" :loading="uploading" @click="handleUpload">
            开始导入
          </a-button>
        </a-space>
      </div>
    </div>

    <!-- Step 2: Progress -->
    <div v-else-if="step === 'progress'">
      <a-result status="info" title="正在导入...">
        <template #extra>
          <div style="width: 100%">
            <a-progress :percent="progressPercent" :status="progressStatus" />
            <div style="margin-top: 12px; color: #666; font-size: 13px">
              <div>总行数: {{ taskInfo.totalRows || '-' }}</div>
              <div>已处理: {{ taskInfo.processedRows || 0 }}</div>
              <div>失败: {{ taskInfo.failedRows || 0 }}</div>
            </div>
          </div>
        </template>
      </a-result>
    </div>

    <!-- Step 3: Result -->
    <div v-else-if="step === 'result'">
      <a-result
        :status="taskInfo.status === 'COMPLETED' ? 'success' : 'error'"
        :title="taskInfo.status === 'COMPLETED' ? '导入完成' : '导入失败'"
      >
        <template #extra>
          <div v-if="taskInfo.status === 'COMPLETED'" style="font-size: 14px">
            <a-row :gutter="16">
              <a-col :span="6"><a-statistic title="总行数" :value="taskInfo.totalRows" /></a-col>
              <a-col :span="6"><a-statistic title="成功" :value="taskInfo.processedRows" value-style="color: #52c41a" /></a-col>
              <a-col :span="6"><a-statistic title="失败" :value="taskInfo.failedRows" value-style="color: #ff4d4f" /></a-col>
              <a-col :span="6">
                <a-statistic
                  title="跳过"
                  :value="Math.max(0, (taskInfo.totalRows || 0) - (taskInfo.processedRows || 0) - (taskInfo.failedRows || 0))"
                />
              </a-col>
            </a-row>
          </div>
          <div v-else style="color: #ff4d4f; font-size: 13px; max-height: 200px; overflow-y: auto; text-align: left">
            {{ taskInfo.errorMessage || '未知错误' }}
          </div>
          <div style="margin-top: 16px">
            <a-button type="primary" @click="handleClose">关闭</a-button>
          </div>
        </template>
      </a-result>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import {
  InboxOutlined,
  DownloadOutlined,
  FileExcelOutlined,
} from '@ant-design/icons-vue'
import {
  importDataElements,
  getDataElementImportStatus,
  downloadDataElementTemplate,
} from '@/api/masterdata'

const emit = defineEmits<{
  (e: 'success'): void
}>()

const visible = ref(false)
const step = ref<'upload' | 'progress' | 'result'>('upload')
const file = ref<File | null>(null)
const uploading = ref(false)
const taskInfo = ref<any>({})
let pollTimer: ReturnType<typeof setInterval> | null = null

const progressPercent = computed(() => {
  if (!taskInfo.value.totalRows) return 0
  if (taskInfo.value.status === 'COMPLETED') return 100
  return Math.round(((taskInfo.value.processedRows || 0) / taskInfo.value.totalRows) * 100)
})

const progressStatus = computed(() => {
  if (taskInfo.value.status === 'FAILED') return 'exception' as const
  if (taskInfo.value.status === 'COMPLETED') return 'success' as const
  return 'active' as const
})

function open() {
  step.value = 'upload'
  file.value = null
  taskInfo.value = {}
  visible.value = true
}

function handleClose() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  visible.value = false
  if (taskInfo.value.status === 'COMPLETED') {
    emit('success')
  }
}

function handleBeforeUpload(f: File) {
  file.value = f
  return false
}

async function handleDownloadTemplate() {
  try {
    const res = await downloadDataElementTemplate()
    const blob = new Blob([res.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'data-element-template.xlsx'
    a.click()
    window.URL.revokeObjectURL(url)
  } catch {
    message.error('下载模板失败')
  }
}

async function handleUpload() {
  if (!file.value) return
  uploading.value = true
  try {
    const res = await importDataElements(file.value)
    const task = res.data.data
    taskInfo.value = task
    step.value = 'progress'
    startPolling(task.id)
  } catch {
    message.error('上传失败')
  } finally {
    uploading.value = false
  }
}

function startPolling(taskId: number) {
  pollTimer = setInterval(async () => {
    try {
      const res = await getDataElementImportStatus(taskId)
      taskInfo.value = res.data.data
      if (['COMPLETED', 'FAILED'].includes(taskInfo.value.status)) {
        if (pollTimer) clearInterval(pollTimer)
        pollTimer = null
        step.value = 'result'
      }
    } catch {
      if (pollTimer) clearInterval(pollTimer)
      pollTimer = null
      step.value = 'result'
      taskInfo.value = { status: 'FAILED', errorMessage: '查询导入状态失败' }
    }
  }, 2000)
}

defineExpose({ open })
</script>
```

- [ ] **Step 2: Commit**

```bash
git add maidc-portal/src/views/masterdata/DataElementImportModal.vue
git commit -m "feat(data-element): add DataElementImportModal component"
```

---

## Task 9: Integrate import modal into DataElementList

**Files:**
- Modify: `maidc-portal/src/views/masterdata/DataElementList.vue`

- [ ] **Step 1: Add import button and modal reference**

In the `<template>` section, modify the `#extra` slot (line ~4) to add the import button:

Replace:
```html
    <template #extra>
      <a-button type="primary" @click="handleCreate">
        <template #icon><PlusOutlined /></template>
        新增数据元
      </a-button>
    </template>
```

With:
```html
    <template #extra>
      <a-space>
        <a-button @click="importModalRef?.open()">
          <template #icon><UploadOutlined /></template>
          导入
        </a-button>
        <a-button type="primary" @click="handleCreate">
          <template #icon><PlusOutlined /></template>
          新增数据元
        </a-button>
      </a-space>
    </template>
```

Add the modal component at the end of the template, before `</PageContainer>`:

```html
    <!-- Import modal -->
    <DataElementImportModal ref="importModalRef" @success="onImportSuccess" />
```

- [ ] **Step 2: Add script imports and ref**

Add to imports:
```typescript
import { UploadOutlined } from '@ant-design/icons-vue'
import DataElementImportModal from './DataElementImportModal.vue'
```

Add ref and handler after the `// ── Init ──` section:
```typescript
const importModalRef = ref<InstanceType<typeof DataElementImportModal>>()

function onImportSuccess() {
  fetchList()
  fetchStats()
  fetchCategories()
}
```

- [ ] **Step 3: Commit**

```bash
git add maidc-portal/src/views/masterdata/DataElementList.vue
git commit -m "feat(data-element): integrate import modal into list page"
```

---

## Task 10: End-to-end verification

**Files:** No changes — verification only.

- [ ] **Step 1: Rebuild backend and restart data service**

```bash
cd maidc-parent && mvn clean package -DskipTests -q
# Restart data service on port 8082
```

- [ ] **Step 2: Verify template download**

```bash
curl -s -o /tmp/template.xlsx http://localhost:8080/api/v1/masterdata/data-elements/import/template
file /tmp/template.xlsx
```

Expected: the file downloads (may be empty due to auth, but endpoint responds)

- [ ] **Step 3: Verify frontend loads**

Open http://localhost:3000, navigate to 数据元管理 page, confirm:
- "导入" button is visible
- Clicking it opens the import modal
- "下载模板" button works

- [ ] **Step 4: Final commit (if any fixes)**

```bash
git add -A && git commit -m "fix(data-element): e2e fixes for import feature"
```
