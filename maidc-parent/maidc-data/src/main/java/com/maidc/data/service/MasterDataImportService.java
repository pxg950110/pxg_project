package com.maidc.data.service;

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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MasterDataImportService {

    private final ImportTaskRepository importTaskRepository;
    private final ConceptRepository conceptRepository;

    private static final int BATCH_SIZE = 500;

    @Transactional
    public ImportTaskEntity uploadAndCreateTask(MultipartFile file, Long codeSystemId) {
        if (file.isEmpty()) {
            throw new BusinessException(400, "上传文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".csv")) {
            throw new BusinessException(400, "仅支持CSV格式文件");
        }

        ImportTaskEntity task = new ImportTaskEntity();
        task.setCodeSystemId(codeSystemId);
        task.setFileName(originalFilename);
        task.setStatus("PENDING");
        task.setTotalRows(0);
        task.setProcessedRows(0);
        task.setFailedRows(0);
        task.setOrgId(0L);
        task = importTaskRepository.save(task);

        processAsync(task.getId(), file, codeSystemId);

        return task;
    }

    public ImportTaskEntity getTaskStatus(Long taskId) {
        return importTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(404, "导入任务不存在: " + taskId));
    }

    @Async
    public void processAsync(Long taskId, MultipartFile file, Long codeSystemId) {
        ImportTaskEntity task = null;
        try {
            task = importTaskRepository.findById(taskId)
                    .orElseThrow(() -> new BusinessException(404, "导入任务不存在: " + taskId));

            task.setStatus("PROCESSING");
            importTaskRepository.save(task);

            List<ConceptEntity> batch = new ArrayList<>(BATCH_SIZE);
            int totalRows = 0;
            int processedRows = 0;
            int failedRows = 0;

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

                String headerLine = reader.readLine();
                if (headerLine == null || headerLine.isBlank()) {
                    throw new BusinessException(400, "CSV文件为空或缺少表头");
                }

                String[] headers = parseCsvLine(headerLine);
                int colConceptCode = findIndex(headers, "concept_code");
                int colName = findIndex(headers, "name");
                int colNameEn = findIndex(headers, "name_en");
                int colDomain = findIndex(headers, "domain");
                int colProperties = findIndex(headers, "properties");
                int colStatus = findIndex(headers, "status");

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    totalRows++;

                    try {
                        String[] cols = parseCsvLine(line);
                        ConceptEntity concept = new ConceptEntity();
                        concept.setCodeSystemId(codeSystemId);
                        concept.setConceptCode(getValue(cols, colConceptCode));
                        concept.setName(getValue(cols, colName));
                        concept.setNameEn(getValue(cols, colNameEn));
                        concept.setDomain(getValue(cols, colDomain));
                        concept.setProperties(getValue(cols, colProperties));
                        concept.setStatus(colStatus >= 0 && cols.length > colStatus && !cols[colStatus].isBlank()
                                ? cols[colStatus] : "ACTIVE");
                        concept.setOrgId(0L);

                        batch.add(concept);
                        processedRows++;

                        if (batch.size() >= BATCH_SIZE) {
                            conceptRepository.saveAll(batch);
                            batch.clear();
                            task.setTotalRows(totalRows);
                            task.setProcessedRows(processedRows);
                            task.setFailedRows(failedRows);
                            importTaskRepository.save(task);
                        }
                    } catch (Exception e) {
                        failedRows++;
                        log.warn("CSV行解析失败, 行号={}, 错误={}", totalRows + 1, e.getMessage());
                    }
                }

                if (!batch.isEmpty()) {
                    conceptRepository.saveAll(batch);
                    batch.clear();
                }
            }

            task.setTotalRows(totalRows);
            task.setProcessedRows(processedRows);
            task.setFailedRows(failedRows);
            task.setStatus("COMPLETED");
            importTaskRepository.save(task);
            log.info("导入任务完成: id={}, 成功={}, 失败={}", taskId, processedRows, failedRows);

        } catch (Exception e) {
            log.error("导入任务失败: id={}", taskId, e);
            if (task != null) {
                task.setStatus("FAILED");
                task.setErrorMessage(e.getMessage());
                importTaskRepository.save(task);
            }
        }
    }

    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString().trim());
        return result.toArray(new String[0]);
    }

    private int findIndex(String[] headers, String target) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].equalsIgnoreCase(target)) {
                return i;
            }
        }
        return -1;
    }

    private String getValue(String[] cols, int index) {
        if (index < 0 || index >= cols.length) return null;
        String val = cols[index];
        return (val == null || val.isBlank()) ? null : val;
    }
}
