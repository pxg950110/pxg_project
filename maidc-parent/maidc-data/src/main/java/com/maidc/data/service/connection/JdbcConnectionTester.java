package com.maidc.data.service.connection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.sql.DriverManager;
import java.util.Map;

@Slf4j
@Component
public class JdbcConnectionTester implements ConnectionTester {

    @Override
    public String getType() { return "JDBC"; }

    @Override
    public ConnectionTestResult test(Map<String, Object> params) {
        String typeCode = (String) params.get("_typeCode");
        String host = (String) params.get("host");
        Number portNum = (Number) params.get("port");
        int port = portNum != null ? portNum.intValue() : 0;
        String database = (String) params.get("database");
        String username = (String) params.get("username");
        String password = (String) params.get("password");

        String jdbcUrl = buildJdbcUrl(typeCode, host, port, database);
        if (jdbcUrl == null) {
            return ConnectionTestResult.fail("不支持的数据库类型: " + typeCode);
        }

        long start = System.currentTimeMillis();
        try {
            Class.forName(getDriverClass(typeCode));
        } catch (ClassNotFoundException e) {
            return ConnectionTestResult.fail("数据库驱动未安装: " + typeCode);
        }

        try (var conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            int latency = (int) (System.currentTimeMillis() - start);
            String version = conn.getMetaData().getDatabaseProductName() + " " + conn.getMetaData().getDatabaseProductVersion();
            return ConnectionTestResult.ok(latency, Map.of("version", version));
        } catch (Exception e) {
            log.warn("JDBC连接测试失败: {}", e.getMessage());
            return ConnectionTestResult.fail(e.getMessage());
        }
    }

    private String buildJdbcUrl(String typeCode, String host, int port, String database) {
        return switch (typeCode) {
            case "MYSQL" -> "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&connectTimeout=5000";
            case "POSTGRESQL" -> "jdbc:postgresql://" + host + ":" + port + "/" + database + "?connectTimeout=5";
            case "ORACLE" -> "jdbc:oracle:thin:@" + host + ":" + port + ":" + database;
            default -> null;
        };
    }

    private String getDriverClass(String typeCode) {
        return switch (typeCode) {
            case "MYSQL" -> "com.mysql.cj.jdbc.Driver";
            case "POSTGRESQL" -> "org.postgresql.Driver";
            case "ORACLE" -> "oracle.jdbc.OracleDriver";
            default -> throw new IllegalArgumentException("Unknown: " + typeCode);
        };
    }
}
