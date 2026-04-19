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
