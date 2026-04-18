package com.maidc.data.etl;

import com.maidc.data.config.EtlProperties;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * CSV bulk importer using PostgreSQL native COPY protocol via JDBC.
 * Replaces Embulk for high-performance CSV loading into ODS tables.
 *
 * <p>Design notes:
 * <ul>
 *   <li>ODS tables have metadata columns at the end: _batch_id, _source_file,
 *       _loaded_at, _row_hash, _is_valid</li>
 *   <li>Before COPY, temporary DEFAULT values are set on _batch_id and _source_file
 *       so that COPY only needs to provide the actual data columns</li>
 *   <li>Supports both plain .csv and .gz (gzip compressed) files</li>
 *   <li>Uses a raw JDBC connection (autoCommit=true) instead of Spring's DataSource
 *       pool, because the COPY protocol requires direct PGConnection access</li>
 * </ul>
 */
@Component
public class CsvCopyImporter {

    private static final Logger log = LoggerFactory.getLogger(CsvCopyImporter.class);

    /** Metadata columns that exist in ODS tables but are NOT present in CSV files. */
    private static final Set<String> METADATA_COLUMNS = Set.of(
            "id", "_batch_id", "_source_file", "_loaded_at", "_row_hash", "_is_valid"
    );

    private final EtlProperties props;

    public CsvCopyImporter(EtlProperties props) {
        this.props = props;
    }

    // ------------------------------------------------------------------ core

    /**
     * Import a CSV file into an ODS table using PostgreSQL COPY protocol.
     *
     * @param tableName   the ODS table name (without schema prefix)
     * @param csvFilePath absolute path to the CSV or .gz file
     * @param csvColumns  column names that match the CSV header, in order
     * @param batchId     the batch identifier to tag every row
     * @param sourceFile  the original source file name for provenance
     * @return number of rows imported
     */
    public long importCsv(String tableName, String csvFilePath,
                          List<String> csvColumns, String batchId,
                          String sourceFile) {
        String schema = props.getDbSchema();
        String qualifiedTable = schema + "." + tableName;
        String columnList = String.join(", ", csvColumns);

        String copySql = String.format(
                "COPY %s(%s) FROM STDIN WITH (FORMAT csv, HEADER true, DELIMITER ',', QUOTE '\"', NULL '')",
                qualifiedTable, columnList
        );

        log.info("COPY import: {} columns -> {}, file={}", csvColumns.size(), qualifiedTable, csvFilePath);

        try (Connection conn = DriverManager.getConnection(
                props.getDbUrl(), props.getDbUser(), props.getDbPassword())) {

            conn.setAutoCommit(true);

            PGConnection pgConn = conn.unwrap(org.postgresql.PGConnection.class);

            // --- set temporary defaults for metadata columns ---
            setColumnDefault(conn, qualifiedTable, "_batch_id", batchId);
            setColumnDefault(conn, qualifiedTable, "_source_file", sourceFile);

            // --- execute COPY ---
            CopyManager copyManager = pgConn.getCopyAPI();
            long rowCount;

            try (Reader reader = createReader(csvFilePath)) {
                rowCount = copyManager.copyIn(copySql, reader);
            }

            log.info("COPY complete: {} rows imported into {}", rowCount, qualifiedTable);

            // --- drop temporary defaults ---
            dropColumnDefault(conn, qualifiedTable, "_batch_id");
            dropColumnDefault(conn, qualifiedTable, "_source_file");

            return rowCount;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to import CSV into " + qualifiedTable + " from " + csvFilePath, e);
        }
    }

    // --------------------------------------------------------- column helpers

    /**
     * Query the database for the non-metadata columns of an ODS table.
     * Excludes: id, _batch_id, _source_file, _loaded_at, _row_hash, _is_valid.
     *
     * @param tableName the ODS table name (without schema)
     * @return list of column names in ordinal order
     */
    public List<String> getTableColumns(String tableName) {
        String sql = "SELECT column_name FROM information_schema.columns "
                + "WHERE table_schema = ? AND table_name = ? "
                + "ORDER BY ordinal_position";

        try (Connection conn = DriverManager.getConnection(
                props.getDbUrl(), props.getDbUser(), props.getDbPassword());
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, props.getDbSchema());
            ps.setString(2, tableName);

            List<String> columns = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String col = rs.getString("column_name");
                    if (!METADATA_COLUMNS.contains(col)) {
                        columns.add(col);
                    }
                }
            }
            return columns;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to query columns for table " + tableName, e);
        }
    }

    // -------------------------------------------------------------- counters

    /**
     * Count rows in an ODS table filtered by batch ID.
     *
     * @param tableName the ODS table name (without schema)
     * @param batchId   the batch identifier; if null, counts all rows
     * @return row count
     */
    public long countTableRows(String tableName, String batchId) {
        String schema = props.getDbSchema();
        String sql;
        if (batchId != null) {
            sql = String.format("SELECT COUNT(*) FROM %s.%s WHERE _batch_id = ?", schema, tableName);
        } else {
            sql = String.format("SELECT COUNT(*) FROM %s.%s", schema, tableName);
        }

        try (Connection conn = DriverManager.getConnection(
                props.getDbUrl(), props.getDbUser(), props.getDbPassword());
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (batchId != null) {
                ps.setString(1, batchId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
            return 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to count rows in " + schema + "." + tableName, e);
        }
    }

    /**
     * Count the number of data rows in a CSV file (total lines minus 1 header).
     * Supports both plain .csv and .gz files.
     *
     * @param csvFilePath absolute path to the CSV or .gz file
     * @return estimated number of data rows
     */
    public long countCsvRows(String csvFilePath) {
        long lines = 0;
        try (Reader r = createReader(csvFilePath);
             BufferedReader br = new BufferedReader(r)) {
            while (br.readLine() != null) {
                lines++;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to count rows in " + csvFilePath, e);
        }
        // subtract header line
        return Math.max(0, lines - 1);
    }

    // ---------------------------------------------------------- internal

    /**
     * Create a Reader appropriate for the file type.
     * .gz files are decompressed; plain files are read as UTF-8.
     */
    private Reader createReader(String filePath) throws IOException {
        if (filePath.toLowerCase().endsWith(".gz")) {
            return new InputStreamReader(
                    new GZIPInputStream(new FileInputStream(filePath)),
                    StandardCharsets.UTF_8);
        } else {
            return new FileReader(filePath, StandardCharsets.UTF_8);
        }
    }

    /** Set a column default value via ALTER TABLE. */
    private void setColumnDefault(Connection conn, String qualifiedTable,
                                  String column, String value) throws SQLException {
        // Use literal quoting to avoid injection — value is internal (batchId/sourceFile)
        String escaped = value.replace("'", "''");
        String sql = String.format("ALTER TABLE %s ALTER COLUMN %s SET DEFAULT '%s'",
                qualifiedTable, column, escaped);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
        log.debug("Set default on {}.{} = '{}'", qualifiedTable, column, value);
    }

    /** Drop a column default via ALTER TABLE. */
    private void dropColumnDefault(Connection conn, String qualifiedTable,
                                   String column) throws SQLException {
        String sql = String.format("ALTER TABLE %s ALTER COLUMN %s DROP DEFAULT",
                qualifiedTable, column);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
        log.debug("Dropped default on {}.{}", qualifiedTable, column);
    }
}
