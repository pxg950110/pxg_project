package com.maidc.data.service;

import com.maidc.data.config.EtlProperties;
import com.maidc.data.etl.CsvCopyImporter;
import com.maidc.data.etl.TableMapping;
import com.maidc.data.repository.OdsImportRepository;
import com.maidc.data.vo.ImportStatusVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.*;

/**
 * ODS 数据导入编排服务。
 * 扫描 CSV 清单 → 生成任务 → 线程池调度 COPY 导入 → 校验行数。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OdsImportService {

    private final TableMapping tableMapping;
    private final CsvCopyImporter csvCopyImporter;
    private final OdsImportRepository importRepository;
    private final EtlProperties etlProperties;

    private volatile String currentBatchId;
    private volatile boolean importing = false;
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    /**
     * 启动全量导入。
     */
    public synchronized String startImport() {
        if (importing) {
            throw new IllegalStateException("导入正在进行中，batch: " + currentBatchId);
        }

        currentBatchId = "batch_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        importing = true;

        // 1. 确保 ODS schema + 数据表存在
        importRepository.ensureOdsSchema();
        importRepository.createMetadataTables();

        // 2. 扫描 CSV → 写入 import_log
        List<TableMapping.TableEntry> entries = tableMapping.getAll();
        int taskCount = 0;
        for (TableMapping.TableEntry entry : entries) {
            String csvPath = resolveCsvPath(entry);
            if (csvPath != null) {
                importRepository.insertImportLog(currentBatchId, entry.tableName(), entry.csvFileName());
                taskCount++;
            } else {
                log.warn("CSV not found for table {}, skipping: {}", entry.tableName(), entry.csvFileName());
            }
        }

        log.info("Import batch {} started: {} tables", currentBatchId, taskCount);

        // 3. 异步执行导入
        CompletableFuture.runAsync(() -> {
            try {
                executeImport(entries);
            } catch (Exception e) {
                log.error("Import batch {} failed", currentBatchId, e);
            } finally {
                importing = false;
            }
        }, executor);

        return currentBatchId;
    }

    private void executeImport(List<TableMapping.TableEntry> entries) {
        String batchId = currentBatchId;
        Semaphore semaphore = new Semaphore(etlProperties.getParallel());
        List<CompletableFuture<Void>> futures = new CopyOnWriteArrayList<>();

        for (TableMapping.TableEntry entry : entries) {
            String csvPath = resolveCsvPath(entry);
            if (csvPath == null) continue;

            // 获取信号量（控制并发数）
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    importTable(batchId, entry, csvPath);
                } finally {
                    semaphore.release();
                }
            }, executor);

            futures.add(future);
        }

        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info("Import batch {} completed", batchId);
    }

    private void importTable(String batchId, TableMapping.TableEntry entry, String csvPath) {
        String tableName = entry.tableName();
        long startMs = System.currentTimeMillis();

        importRepository.markRunning(batchId, tableName);
        log.info("Importing {} from {}", tableName, csvPath);

        try {
            // 0. 清空目标表（确保幂等）
            csvCopyImporter.truncateTable(tableName);

            // 1. 获取 CSV 列名（从数据库元数据）
            List<String> columns = csvCopyImporter.getTableColumns(tableName);
            if (columns.isEmpty()) {
                throw new RuntimeException("No columns found for table " + tableName + ". Table may not exist in ODS schema.");
            }

            // 2. 执行 COPY 导入
            long importedRows = csvCopyImporter.importCsv(
                    tableName, csvPath, columns, batchId, entry.csvFileName()
            );

            // 3. 校验行数
            long csvRows = csvCopyImporter.countCsvRows(csvPath);
            long dbRows = csvCopyImporter.countTableRows(tableName, batchId);
            boolean match = (csvRows == dbRows);
            int durationSec = (int) ((System.currentTimeMillis() - startMs) / 1000);

            importRepository.markSuccess(batchId, tableName, csvRows, dbRows, match, durationSec);
            importRepository.insertCheck(batchId, tableName, "ROW_COUNT",
                    match ? "PASS" : "FAIL", csvRows, dbRows, dbRows - csvRows);

            log.info("Imported {} : {} CSV rows, {} DB rows, match={}, {}s",
                    tableName, csvRows, dbRows, match, durationSec);

        } catch (Exception e) {
            int durationSec = (int) ((System.currentTimeMillis() - startMs) / 1000);
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > 500) {
                errorMsg = errorMsg.substring(0, 500);
            }
            importRepository.markFailed(batchId, tableName, durationSec, errorMsg);
            log.error("Failed to import {}", tableName, e);
        }
    }

    /**
     * 解析 CSV 文件路径。支持 .csv / .csv.gz / 目录/文件两种模式。
     */
    private String resolveCsvPath(TableMapping.TableEntry entry) {
        String baseDir = etlProperties.getCsvBaseDir();
        String subDir = entry.source() == TableMapping.DataSource.MIMIC_III ? "iii" : "iv";
        String csvFileName = entry.csvFileName();

        // 尝试多种路径模式
        String[] candidates = {
                // MIMIC-III: base/iii/PATIENTS.csv/PATIENTS.csv (directory with file inside)
                baseDir + "/" + subDir + "/" + csvFileName + "/" + csvFileName,
                // MIMIC-III: base/iii/PATIENTS.csv (plain file)
                baseDir + "/" + subDir + "/" + csvFileName,
                // MIMIC-III: compressed
                baseDir + "/" + subDir + "/" + csvFileName + ".gz",
                // MIMIC-IV: base/iv/content/mimic-iv-3.1/hosp/patients.csv
                baseDir + "/" + subDir + "/content/mimic-iv-3.1/hosp/" + csvFileName,
                baseDir + "/" + subDir + "/content/mimic-iv-3.1/icu/" + csvFileName,
                // MIMIC-IV: compressed
                baseDir + "/" + subDir + "/content/mimic-iv-3.1/hosp/" + csvFileName + ".gz",
                baseDir + "/" + subDir + "/content/mimic-iv-3.1/icu/" + csvFileName + ".gz",
                // MIMIC-IV: direct hosp/icu (no content prefix)
                baseDir + "/" + subDir + "/hosp/" + csvFileName,
                baseDir + "/" + subDir + "/icu/" + csvFileName,
                baseDir + "/" + subDir + "/hosp/" + csvFileName + ".gz",
                baseDir + "/" + subDir + "/icu/" + csvFileName + ".gz",
        };

        for (String candidate : candidates) {
            File f = new File(candidate);
            if (f.exists()) {
                return f.getAbsolutePath();
            }
        }

        log.debug("CSV not found for {}, tried: {}", entry.tableName(), String.join(", ", candidates));
        return null;
    }

    /**
     * 查询导入状态。
     */
    public ImportStatusVO getImportStatus() {
        String batchId = currentBatchId != null ? currentBatchId : importRepository.getLatestBatchId();
        if (batchId == null) {
            return ImportStatusVO.builder().batchId("N/A").total(0).build();
        }

        List<OdsImportRepository.ImportLogEntry> logs = importRepository.getImportLogs(batchId);

        int success = 0, failed = 0, running = 0, pending = 0;
        for (OdsImportRepository.ImportLogEntry log : logs) {
            switch (log.getStatus()) {
                case "SUCCESS" -> success++;
                case "FAILED" -> failed++;
                case "RUNNING" -> running++;
                default -> pending++;
            }
        }

        List<ImportStatusVO.TableStatus> tables = logs.stream().map(l ->
                ImportStatusVO.TableStatus.builder()
                        .table(l.getTableName())
                        .status(l.getStatus())
                        .csvRows(l.getCsvRows())
                        .dbRows(l.getDbRows())
                        .match(l.getRowMatch())
                        .duration(l.getDurationSec())
                        .error(l.getErrorMsg())
                        .startedAt(l.getStartedAt() != null ? l.getStartedAt().toString() : null)
                        .finishedAt(l.getFinishedAt() != null ? l.getFinishedAt().toString() : null)
                        .build()
        ).toList();

        return ImportStatusVO.builder()
                .batchId(batchId)
                .total(logs.size())
                .success(success)
                .failed(failed)
                .running(running)
                .pending(pending)
                .tables(tables)
                .build();
    }

    /**
     * 重试失败的表。
     */
    public void retryTable(String tableName) {
        String bid = currentBatchId != null ? currentBatchId : importRepository.getLatestBatchId();
        if (bid == null) {
            throw new IllegalStateException("No import batch found");
        }
        final String batchId = bid;

        // Find the table entry
        TableMapping.TableEntry entry = tableMapping.getByTableName(tableName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown table: " + tableName));

        String csvPath = resolveCsvPath(entry);
        if (csvPath == null) {
            throw new IllegalStateException("CSV file not found for " + tableName);
        }

        // Reset status
        importRepository.insertImportLog(batchId, tableName, entry.csvFileName());

        CompletableFuture.runAsync(() -> importTable(batchId, entry, csvPath), executor);
    }

    public boolean isImporting() {
        return importing;
    }
}
