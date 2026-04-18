package com.maidc.data.repository;

import com.maidc.data.config.EtlProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ODS 导入元数据表 (ods_import_log / ods_import_check) 的 JDBC 操作。
 * 使用独立的 JDBC 连接，不经过 JPA/Hibernate。
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class OdsImportRepository {

    private final EtlProperties etlProperties;

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                etlProperties.getDbUrl(),
                etlProperties.getDbUser(),
                etlProperties.getDbPassword()
        );
    }

    // ====================== DDL ======================

    /**
     * 确保 ODS schema 和 57 张数据表存在。
     * 读取 docker/init-db 下的 07/08/09 SQL 文件执行建表。
     */
    public void ensureOdsSchema() {
        // 1. 先检查 ODS schema 是否已存在
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM information_schema.schemata WHERE schema_name = ?")) {
            ps.setString(1, etlProperties.getDbSchema());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                log.info("ODS schema already exists, skipping DDL");
                return;
            }
        } catch (SQLException e) {
            log.warn("Failed to check schema existence: {}", e.getMessage());
        }

        // 2. Schema 不存在，执行 DDL 文件
        String ddlPath = etlProperties.getDdlPath();
        if (ddlPath == null || ddlPath.isBlank()) {
            // 只创建 schema
            try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE SCHEMA IF NOT EXISTS " + etlProperties.getDbSchema());
                log.info("Created ODS schema");
            } catch (SQLException e) {
                throw new RuntimeException("Failed to create ODS schema", e);
            }
            return;
        }

        String[] ddlFiles = {"07-ods-schema.sql", "08-ods-mimic3.sql", "09-ods-mimic4.sql"};
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // 先创建 schema
            stmt.execute("CREATE SCHEMA IF NOT EXISTS " + etlProperties.getDbSchema());

            for (String ddlFile : ddlFiles) {
                java.io.File file = new java.io.File(ddlPath, ddlFile);
                if (!file.exists()) {
                    log.warn("DDL file not found: {}", file.getAbsolutePath());
                    continue;
                }
                String sql = new String(java.nio.file.Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);
                // 移除注释行后按分号分割执行
                for (String single : sql.split(";")) {
                    String trimmed = single.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                        try {
                            stmt.execute(trimmed);
                        } catch (SQLException e) {
                            log.warn("DDL statement warning: {}", e.getMessage());
                        }
                    }
                }
                log.info("Executed DDL file: {}", ddlFile);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute ODS DDL", e);
        }
    }

    public void createMetadataTables() {
        String schema = etlProperties.getDbSchema();
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS %s.ods_import_log (
                    id              BIGSERIAL   PRIMARY KEY,
                    table_name      VARCHAR(64) NOT NULL,
                    source_file     VARCHAR(256) NOT NULL,
                    status          VARCHAR(16) NOT NULL DEFAULT 'PENDING',
                    csv_rows        BIGINT,
                    db_rows         BIGINT,
                    row_match       BOOLEAN,
                    started_at      TIMESTAMP,
                    finished_at     TIMESTAMP,
                    duration_sec    INT,
                    error_msg       TEXT,
                    batch_id        VARCHAR(32) NOT NULL
                )
                """.formatted(schema));

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS %s.ods_import_check (
                    id              BIGSERIAL   PRIMARY KEY,
                    batch_id        VARCHAR(32) NOT NULL,
                    table_name      VARCHAR(64) NOT NULL,
                    check_type      VARCHAR(16) NOT NULL,
                    check_result    VARCHAR(8)  NOT NULL,
                    expected        BIGINT,
                    actual          BIGINT,
                    diff            BIGINT,
                    checked_at      TIMESTAMP   NOT NULL DEFAULT NOW()
                )
                """.formatted(schema));

            log.info("ODS metadata tables created in schema {}", schema);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create ODS metadata tables", e);
        }
    }

    // ====================== Import Log ======================

    public void insertImportLog(String batchId, String tableName, String sourceFile) {
        String sql = "INSERT INTO %s.ods_import_log (table_name, source_file, status, batch_id) VALUES (?, ?, 'PENDING', ?)"
                .formatted(etlProperties.getDbSchema());
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setString(2, sourceFile);
            ps.setString(3, batchId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert import log for " + tableName, e);
        }
    }

    public void updateStatus(String batchId, String tableName, String status,
                             Long csvRows, Long dbRows, Boolean rowMatch,
                             Integer durationSec, String errorMsg) {
        String sql = """
            UPDATE %s.ods_import_log SET
                status = ?, csv_rows = ?, db_rows = ?, row_match = ?,
                finished_at = NOW(), duration_sec = ?, error_msg = ?
            WHERE batch_id = ? AND table_name = ?
            """.formatted(etlProperties.getDbSchema());

        if ("RUNNING".equals(status)) {
            sql = """
                UPDATE %s.ods_import_log SET status = ?, started_at = NOW()
                WHERE batch_id = ? AND table_name = ?
                """.formatted(etlProperties.getDbSchema());
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, status);
                ps.setString(2, batchId);
                ps.setString(3, tableName);
                ps.executeUpdate();
            } catch (SQLException e) {
                log.error("Failed to update import log status", e);
            }
            return;
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            if (csvRows != null) ps.setLong(2, csvRows); else ps.setNull(2, Types.BIGINT);
            if (dbRows != null) ps.setLong(3, dbRows); else ps.setNull(3, Types.BIGINT);
            if (rowMatch != null) ps.setBoolean(4, rowMatch); else ps.setNull(4, Types.BOOLEAN);
            if (durationSec != null) ps.setInt(5, durationSec); else ps.setNull(5, Types.INTEGER);
            ps.setString(6, errorMsg);
            ps.setString(7, batchId);
            ps.setString(8, tableName);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to update import log", e);
        }
    }

    public void markRunning(String batchId, String tableName) {
        updateStatus(batchId, tableName, "RUNNING", null, null, null, null, null);
    }

    public void markSuccess(String batchId, String tableName,
                            Long csvRows, Long dbRows, boolean rowMatch, int durationSec) {
        updateStatus(batchId, tableName, "SUCCESS", csvRows, dbRows, rowMatch, durationSec, null);
    }

    public void markFailed(String batchId, String tableName, int durationSec, String errorMsg) {
        updateStatus(batchId, tableName, "FAILED", null, null, null, durationSec, errorMsg);
    }

    // ====================== Import Check ======================

    public void insertCheck(String batchId, String tableName, String checkType,
                            String checkResult, long expected, long actual, long diff) {
        String sql = """
            INSERT INTO %s.ods_import_check (batch_id, table_name, check_type, check_result, expected, actual, diff)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """.formatted(etlProperties.getDbSchema());
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, batchId);
            ps.setString(2, tableName);
            ps.setString(3, checkType);
            ps.setString(4, checkResult);
            ps.setLong(5, expected);
            ps.setLong(6, actual);
            ps.setLong(7, diff);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to insert import check", e);
        }
    }

    // ====================== Query ======================

    public List<ImportLogEntry> getImportLogs(String batchId) {
        String sql = """
            SELECT table_name, status, csv_rows, db_rows, row_match,
                   started_at, finished_at, duration_sec, error_msg
            FROM %s.ods_import_log WHERE batch_id = ? ORDER BY id
            """.formatted(etlProperties.getDbSchema());
        List<ImportLogEntry> entries = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, batchId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                entries.add(ImportLogEntry.builder()
                        .tableName(rs.getString("table_name"))
                        .status(rs.getString("status"))
                        .csvRows(rs.getObject("csv_rows") != null ? rs.getLong("csv_rows") : null)
                        .dbRows(rs.getObject("db_rows") != null ? rs.getLong("db_rows") : null)
                        .rowMatch(rs.getObject("row_match") != null ? rs.getBoolean("row_match") : null)
                        .startedAt(rs.getTimestamp("started_at") != null ? rs.getTimestamp("started_at").toLocalDateTime() : null)
                        .finishedAt(rs.getTimestamp("finished_at") != null ? rs.getTimestamp("finished_at").toLocalDateTime() : null)
                        .durationSec(rs.getObject("duration_sec") != null ? rs.getInt("duration_sec") : null)
                        .errorMsg(rs.getString("error_msg"))
                        .build());
            }
        } catch (SQLException e) {
            log.error("Failed to query import logs", e);
        }
        return entries;
    }

    public String getLatestBatchId() {
        String sql = "SELECT MAX(batch_id) FROM %s.ods_import_log".formatted(etlProperties.getDbSchema());
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getString(1);
        } catch (SQLException e) {
            log.error("Failed to get latest batch ID", e);
        }
        return null;
    }

    // ====================== Inner DTO ======================

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ImportLogEntry {
        private String tableName;
        private String status;
        private Long csvRows;
        private Long dbRows;
        private Boolean rowMatch;
        private LocalDateTime startedAt;
        private LocalDateTime finishedAt;
        private Integer durationSec;
        private String errorMsg;
    }
}
