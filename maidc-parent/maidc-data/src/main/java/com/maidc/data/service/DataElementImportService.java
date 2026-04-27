package com.maidc.data.service;

import com.maidc.common.core.exception.BusinessException;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataElementImportService {

    private final ImportTaskRepository importTaskRepository;
    private final DataElementRepository dataElementRepository;
    private final DataElementValueRepository dataElementValueRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final int BATCH_SIZE = 500;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_ERRORS = 100;

    private static final LinkedHashMap<String, String> HEADER_MAP = new LinkedHashMap<>();

    static {
        HEADER_MAP.put("\u6807\u8bc6\u7b26", "elementCode");
        HEADER_MAP.put("\u89c4\u8303\u540d\u79f0", "name");
        HEADER_MAP.put("\u82f1\u6587\u540d\u79f0", "nameEn");
        HEADER_MAP.put("\u5b9a\u4e49", "definition");
        HEADER_MAP.put("\u5bf9\u8c61\u7c7b", "objectClassName");
        HEADER_MAP.put("\u5bf9\u8c61\u7c7bID", "objectClassId");
        HEADER_MAP.put("\u7279\u6027", "propertyName");
        HEADER_MAP.put("\u7279\u6027ID", "propertyId");
        HEADER_MAP.put("\u6570\u636e\u7c7b\u578b", "dataType");
        HEADER_MAP.put("\u8868\u793a\u7c7b", "representationClass");
        HEADER_MAP.put("\u503c\u57df\u540d\u79f0", "valueDomainName");
        HEADER_MAP.put("\u503c\u57dfID", "valueDomainId");
        HEADER_MAP.put("\u6700\u5c0f\u957f\u5ea6", "minLength");
        HEADER_MAP.put("\u6700\u5927\u957f\u5ea6", "maxLength");
        HEADER_MAP.put("\u683c\u5f0f", "format");
        HEADER_MAP.put("\u8ba1\u91cf\u5355\u4f4d", "unitOfMeasure");
        HEADER_MAP.put("\u5206\u7c7b", "category");
        HEADER_MAP.put("\u6807\u51c6\u6765\u6e90", "standardSource");
        HEADER_MAP.put("\u6ce8\u518c\u72b6\u6001", "registrationStatus");
        HEADER_MAP.put("\u7248\u672c", "version");
    }

    private static final String UPSERT_ELEMENT_SQL =
            "INSERT INTO masterdata.m_data_element " +
            "(element_code, name, name_en, definition, object_class_name, object_class_id, " +
            " property_name, property_id, data_type, representation_class, " +
            " value_domain_name, value_domain_id, min_length, max_length, format, " +
            " unit_of_measure, category, standard_source, registration_status, version, " +
            " status, created_by, created_at, is_deleted, org_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
            " 'ACTIVE', 'system', NOW(), false, 0) " +
            "ON CONFLICT (element_code) DO UPDATE SET " +
            " name = EXCLUDED.name, name_en = EXCLUDED.name_en, definition = EXCLUDED.definition, " +
            " object_class_name = EXCLUDED.object_class_name, object_class_id = EXCLUDED.object_class_id, " +
            " property_name = EXCLUDED.property_name, property_id = EXCLUDED.property_id, " +
            " data_type = EXCLUDED.data_type, representation_class = EXCLUDED.representation_class, " +
            " value_domain_name = EXCLUDED.value_domain_name, value_domain_id = EXCLUDED.value_domain_id, " +
            " min_length = EXCLUDED.min_length, max_length = EXCLUDED.max_length, format = EXCLUDED.format, " +
            " unit_of_measure = EXCLUDED.unit_of_measure, category = EXCLUDED.category, " +
            " standard_source = EXCLUDED.standard_source, registration_status = EXCLUDED.registration_status, " +
            " version = EXCLUDED.version, updated_by = 'system', updated_at = NOW()";

    // ── Public API ──

    public ImportTaskEntity uploadAndStart(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(400, "\u4e0a\u4f20\u6587\u4ef6\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(400, "\u6587\u4ef6\u5927\u5c0f\u4e0d\u80fd\u8d85\u8fc710MB");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".xlsx")) {
            throw new BusinessException(400, "\u4ec5\u652f\u6301.xlsx\u683c\u5f0f\u6587\u4ef6");
        }

        ImportTaskEntity task = new ImportTaskEntity();
        task.setTaskType("DATA_ELEMENT");
        task.setCodeSystemId(0L);
        task.setFileName(originalFilename);
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
                .orElseThrow(() -> new BusinessException(404, "\u5bfc\u5165\u4efb\u52a1\u4e0d\u5b58\u5728: " + taskId));
    }

    // ── Async Processing ──

    @Async
    public void processAsync(Long taskId, MultipartFile file) {
        ImportTaskEntity task = null;
        try {
            task = importTaskRepository.findById(taskId)
                    .orElseThrow(() -> new BusinessException(404, "\u5bfc\u5165\u4efb\u52a1\u4e0d\u5b58\u5728: " + taskId));

            task.setStatus("PROCESSING");
            importTaskRepository.save(task);

            try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
                int[] sheet1Result = processSheet1(workbook, task);
                int valueCount = processSheet2(workbook);

                task.setTotalRows(sheet1Result[0]);
                task.setProcessedRows(sheet1Result[1]);
                task.setFailedRows(sheet1Result[2]);
                task.setStatus("COMPLETED");
                importTaskRepository.save(task);
                log.info("\u6570\u636e\u5143\u5bfc\u5165\u5b8c\u6210: taskId={}, \u603b\u884c={}, \u6210\u529f={}, \u5931\u8d25={}, \u5141\u8bb8\u503c={}",
                        taskId, sheet1Result[0], sheet1Result[1], sheet1Result[2], valueCount);
            }
        } catch (Exception e) {
            log.error("\u6570\u636e\u5143\u5bfc\u5165\u5931\u8d25: taskId={}", taskId, e);
            if (task != null) {
                task.setStatus("FAILED");
                String msg = e.getMessage();
                if (msg != null && msg.length() > 2000) {
                    msg = msg.substring(0, 2000);
                }
                task.setErrorMessage(msg);
                importTaskRepository.save(task);
            }
        }
    }

    // ── Sheet1: Data Elements ──

    private int[] processSheet1(Workbook workbook, ImportTaskEntity task) {
        Sheet sheet = workbook.getSheetAt(0);
        if (sheet == null || sheet.getLastRowNum() < 1) {
            return new int[]{0, 0, 0};
        }

        Row headerRow = sheet.getRow(0);
        Map<String, Integer> colIndex = buildColumnIndex(headerRow);

        List<DataElementEntity> batch = new ArrayList<>(BATCH_SIZE);
        List<String> errors = new ArrayList<>();
        int totalRows = 0;
        int successCount = 0;
        int failCount = 0;

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isEmptyRow(row)) continue;
            totalRows++;

            try {
                DataElementEntity entity = mapRowToEntity(row, colIndex, i + 1);
                batch.add(entity);
                successCount++;
            } catch (Exception e) {
                failCount++;
                if (errors.size() < MAX_ERRORS) {
                    errors.add("\u7b2c" + (i + 1) + "\u884c: " + e.getMessage());
                }
            }

            if (batch.size() >= BATCH_SIZE) {
                batchUpsertElements(batch);
                batch.clear();
                task.setTotalRows(totalRows);
                task.setProcessedRows(successCount);
                task.setFailedRows(failCount);
                importTaskRepository.save(task);
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
        if (workbook.getNumberOfSheets() < 2) {
            return 0;
        }

        Sheet sheet = workbook.getSheetAt(1);
        if (sheet == null || sheet.getLastRowNum() < 1) {
            return 0;
        }

        Row headerRow = sheet.getRow(0);
        if (headerRow == null) return 0;

        int colCode = -1, colValueCode = -1, colMeaning = -1, colSort = -1;
        for (int c = 0; c < headerRow.getLastCellNum(); c++) {
            String header = getStringValue(headerRow, c);
            if (header == null) continue;
            switch (header.trim()) {
                case "\u6807\u8bc6\u7b26" -> colCode = c;
                case "\u503c\u7f16\u7801" -> colValueCode = c;
                case "\u503c\u542b\u4e49" -> colMeaning = c;
                case "\u6392\u5e8f" -> colSort = c;
            }
        }

        if (colCode < 0 || colValueCode < 0 || colMeaning < 0) {
            log.warn("Sheet2 \u8868\u5934\u4e0d\u5339\u914d\u5141\u8bb8\u503c\u683c\u5f0f\uff0c\u8df3\u8fc7");
            return 0;
        }

        Map<String, List<DataElementValueEntity>> valuesByCode = new LinkedHashMap<>();
        int totalValues = 0;

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isEmptyRow(row)) continue;

            String elementCode = getStringValue(row, colCode);
            String valueCode = getStringValue(row, colValueCode);
            String valueMeaning = getStringValue(row, colMeaning);
            int sortOrder = getIntValue(row, colSort, totalValues);

            if (elementCode == null || elementCode.isBlank() || valueCode == null || valueCode.isBlank()) {
                continue;
            }

            DataElementValueEntity val = new DataElementValueEntity();
            val.setValueCode(valueCode);
            val.setValueMeaning(valueMeaning != null ? valueMeaning : "");
            val.setSortOrder(sortOrder);

            valuesByCode.computeIfAbsent(elementCode.trim(), k -> new ArrayList<>()).add(val);
            totalValues++;
        }

        if (!valuesByCode.isEmpty()) {
            batchUpsertValues(valuesByCode);
        }

        return totalValues;
    }

    // ── Batch Upsert: Data Elements ──

    private void batchUpsertElements(List<DataElementEntity> elements) {
        jdbcTemplate.batchUpdate(UPSERT_ELEMENT_SQL, elements, BATCH_SIZE,
                (ps, e) -> {
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
                    setNullableInt(ps, 13, e.getMinLength());
                    setNullableInt(ps, 14, e.getMaxLength());
                    ps.setString(15, e.getFormat());
                    ps.setString(16, e.getUnitOfMeasure());
                    ps.setString(17, e.getCategory());
                    ps.setString(18, e.getStandardSource());
                    ps.setString(19, e.getRegistrationStatus());
                    ps.setString(20, e.getVersion());
                });
        log.debug("\u6279\u91cf\u5199\u5165\u6570\u636e\u5143: {} \u6761", elements.size());
    }

    // ── Batch Upsert: Values ──

    private void batchUpsertValues(Map<String, List<DataElementValueEntity>> valuesByCode) {
        List<String> codes = new ArrayList<>(valuesByCode.keySet());

        // Resolve elementCode -> id in batches
        Map<String, Long> codeToId = new HashMap<>();
        for (int i = 0; i < codes.size(); i += BATCH_SIZE) {
            List<String> batch = codes.subList(i, Math.min(i + BATCH_SIZE, codes.size()));
            String placeholders = String.join(",", Collections.nCopies(batch.size(), "?"));
            String sql = "SELECT id, element_code FROM masterdata.m_data_element " +
                         "WHERE element_code IN (" + placeholders + ") AND is_deleted = false";

            jdbcTemplate.query(sql, rs -> {
                codeToId.put(rs.getString("element_code"), rs.getLong("id"));
            }, batch.toArray());
        }

        // Soft-delete old values, then insert new ones
        List<DataElementValueEntity> allValues = new ArrayList<>();

        for (Map.Entry<String, List<DataElementValueEntity>> entry : valuesByCode.entrySet()) {
            Long elementId = codeToId.get(entry.getKey());
            if (elementId == null) {
                log.warn("\u5141\u8bb8\u503c\u5f15\u7528\u7684\u6570\u636e\u5143\u4e0d\u5b58\u5728: {}", entry.getKey());
                continue;
            }

            // Soft-delete existing values
            jdbcTemplate.update(
                    "UPDATE masterdata.m_data_element_value SET is_deleted = true WHERE data_element_id = ?",
                    elementId);

            for (DataElementValueEntity val : entry.getValue()) {
                val.setDataElementId(elementId);
                allValues.add(val);
            }
        }

        if (!allValues.isEmpty()) {
            String insertSql = "INSERT INTO masterdata.m_data_element_value " +
                    "(data_element_id, value_code, value_meaning, sort_order, " +
                    " created_by, created_at, is_deleted, org_id) " +
                    "VALUES (?, ?, ?, ?, 'system', NOW(), false, 0)";

            jdbcTemplate.batchUpdate(insertSql, allValues, BATCH_SIZE,
                    (ps, v) -> {
                        ps.setLong(1, v.getDataElementId());
                        ps.setString(2, v.getValueCode());
                        ps.setString(3, v.getValueMeaning());
                        ps.setInt(4, v.getSortOrder() != null ? v.getSortOrder() : 0);
                    });
        }

        log.info("\u6279\u91cf\u5199\u5165\u5141\u8bb8\u503c: {} \u6761", allValues.size());
    }

    // ── Template Generation ──

    public byte[] generateTemplate() {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Sheet1: 数据元
            Sheet sheet1 = workbook.createSheet("\u6570\u636e\u5143");
            Row header1 = sheet1.createRow(0);
            List<String> headers = new ArrayList<>(HEADER_MAP.keySet());
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = header1.createCell(i);
                cell.setCellValue(headers.get(i));
            }

            // Sample row
            Row sample = sheet1.createRow(1);
            sample.createCell(0).setCellValue("DE04.50.001");
            sample.createCell(1).setCellValue("\u60a3\u8005\u59d3\u540d");
            sample.createCell(2).setCellValue("Patient Name");
            sample.createCell(3).setCellValue("\u60a3\u8005\u7684\u59d3\u540d");
            sample.createCell(4).setCellValue("\u60a3\u8005");
            sample.createCell(5).setCellValue("");
            sample.createCell(6).setCellValue("\u59d3\u540d");
            sample.createCell(7).setCellValue("");
            sample.createCell(8).setCellValue("S");
            sample.createCell(9).setCellValue("\u53ef\u53d8\u957f\u5ea6\u5b57\u7b26\u4e32");
            sample.createCell(10).setCellValue("");
            sample.createCell(11).setCellValue("");
            sample.createCell(12).setCellValue("1");
            sample.createCell(13).setCellValue("100");
            sample.createCell(14).setCellValue("");
            sample.createCell(15).setCellValue("");
            sample.createCell(16).setCellValue("\u4eba\u53e3\u5b66");
            sample.createCell(17).setCellValue("\u5065\u5eb7\u4fe1\u606f\u6570\u636e\u5143\u76ee\u5f55");
            sample.createCell(18).setCellValue("DRAFT");
            sample.createCell(19).setCellValue("1.0");

            // Sheet2: 允许值
            Sheet sheet2 = workbook.createSheet("\u5141\u8bb8\u503c");
            Row header2 = sheet2.createRow(0);
            header2.createCell(0).setCellValue("\u6807\u8bc6\u7b26");
            header2.createCell(1).setCellValue("\u503c\u7f16\u7801");
            header2.createCell(2).setCellValue("\u503c\u542b\u4e49");
            header2.createCell(3).setCellValue("\u6392\u5e8f");

            Row sampleVal1 = sheet2.createRow(1);
            sampleVal1.createCell(0).setCellValue("DE04.50.001");
            sampleVal1.createCell(1).setCellValue("1");
            sampleVal1.createCell(2).setCellValue("\u793a\u4f8b\u503c");
            sampleVal1.createCell(3).setCellValue("0");

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(500, "\u751f\u6210\u6a21\u677f\u5931\u8d25: " + e.getMessage());
        }
    }

    // ── Helper Methods ──

    private Map<String, Integer> buildColumnIndex(Row headerRow) {
        Map<String, Integer> colIndex = new HashMap<>();
        for (int c = 0; c < headerRow.getLastCellNum(); c++) {
            String header = getStringValue(headerRow, c);
            if (header != null && HEADER_MAP.containsKey(header.trim())) {
                colIndex.put(HEADER_MAP.get(header.trim()), c);
            }
        }
        return colIndex;
    }

    private DataElementEntity mapRowToEntity(Row row, Map<String, Integer> colIndex, int rowNumber) {
        String elementCode = getStringByField(row, colIndex, "elementCode");
        String name = getStringByField(row, colIndex, "name");
        String dataType = getStringByField(row, colIndex, "dataType");

        if (elementCode == null || elementCode.isBlank()) {
            throw new IllegalArgumentException("\u6807\u8bc6\u7b26\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("\u89c4\u8303\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (dataType == null || dataType.isBlank()) {
            throw new IllegalArgumentException("\u6570\u636e\u7c7b\u578b\u4e0d\u80fd\u4e3a\u7a7a");
        }

        DataElementEntity entity = new DataElementEntity();
        entity.setElementCode(elementCode.trim());
        entity.setName(name.trim());
        entity.setNameEn(getStringByField(row, colIndex, "nameEn"));
        entity.setDefinition(getStringByField(row, colIndex, "definition"));
        entity.setObjectClassName(getStringByField(row, colIndex, "objectClassName"));
        entity.setObjectClassId(getStringByField(row, colIndex, "objectClassId"));
        entity.setPropertyName(getStringByField(row, colIndex, "propertyName"));
        entity.setPropertyId(getStringByField(row, colIndex, "propertyId"));
        entity.setDataType(dataType.trim());
        entity.setRepresentationClass(getStringByField(row, colIndex, "representationClass"));
        entity.setValueDomainName(getStringByField(row, colIndex, "valueDomainName"));
        entity.setValueDomainId(getStringByField(row, colIndex, "valueDomainId"));

        Integer minLength = colIndex.containsKey("minLength")
                ? getIntValue(row, colIndex.get("minLength"), null) : null;
        Integer maxLength = colIndex.containsKey("maxLength")
                ? getIntValue(row, colIndex.get("maxLength"), null) : null;
        entity.setMinLength(minLength);
        entity.setMaxLength(maxLength);

        entity.setFormat(getStringByField(row, colIndex, "format"));
        entity.setUnitOfMeasure(getStringByField(row, colIndex, "unitOfMeasure"));
        entity.setCategory(getStringByField(row, colIndex, "category"));
        entity.setStandardSource(getStringByField(row, colIndex, "standardSource"));

        String regStatus = getStringByField(row, colIndex, "registrationStatus");
        entity.setRegistrationStatus((regStatus != null && !regStatus.isBlank()) ? regStatus.trim() : "DRAFT");

        String version = getStringByField(row, colIndex, "version");
        entity.setVersion((version != null && !version.isBlank()) ? version.trim() : "1.0");

        entity.setOrgId(0L);
        return entity;
    }

    private String getStringByField(Row row, Map<String, Integer> colIndex, String field) {
        Integer idx = colIndex.get(field);
        if (idx == null) return null;
        return getStringValue(row, idx);
    }

    private String getStringValue(Row row, int colIndex) {
        if (colIndex < 0) return null;
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d) && !Double.isInfinite(d)) {
                    yield String.valueOf((long) d);
                }
                yield String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue();
                } catch (Exception e) {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
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
                try {
                    yield Integer.parseInt(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    yield defaultValue;
                }
            }
            default -> defaultValue;
        };
    }

    private Integer getIntValue(Row row, int colIndex, Integer defaultValue) {
        if (colIndex < 0) return defaultValue;
        Cell cell = row.getCell(colIndex);
        if (cell == null) return defaultValue;

        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Integer.parseInt(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    yield defaultValue;
                }
            }
            default -> defaultValue;
        };
    }

    private boolean isEmptyRow(Row row) {
        if (row == null) return true;
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = getStringValue(row, c);
                if (val != null && !val.isBlank()) return false;
            }
        }
        return true;
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws java.sql.SQLException {
        if (value != null) {
            ps.setInt(index, value);
        } else {
            ps.setNull(index, java.sql.Types.INTEGER);
        }
    }
}
