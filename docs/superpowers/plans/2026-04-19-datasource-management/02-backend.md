# Plan 02: 后端实现

**Goal:** 实现 Entity/Repository/Service/Controller + ConnectionTester 策略模式

**依赖:** Plan 01 DDL 完成

**架构:** Controller → Service → Repository（JPA）+ ConnectionTester 策略模式

---

## Task 1: DataSourceType Entity + Repository

**Files:**
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/entity/DataSourceTypeEntity.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/repository/DataSourceTypeRepository.java`

- [ ] **Step 1: 创建 DataSourceTypeEntity**

```java
package com.maidc.data.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.maidc.common.jpa.base.BaseEntity;
import com.maidc.common.jpa.converter.JsonNodeConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "r_data_source_type", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.r_data_source_type SET is_deleted = true WHERE id = ?")
public class DataSourceTypeEntity extends BaseEntity {

    @Column(name = "type_code", nullable = false, unique = true, length = 64)
    private String typeCode;

    @Column(name = "type_name", nullable = false, length = 128)
    private String typeName;

    @Column(name = "category", nullable = false, length = 32)
    private String category;

    @Column(name = "icon", length = 64)
    private String icon;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "param_schema", nullable = false, columnDefinition = "jsonb")
    private JsonNode paramSchema;

    @Column(name = "test_command", length = 32)
    private String testCommand;

    @Column(name = "is_builtin", nullable = false)
    private Boolean isBuiltin = false;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
```

- [ ] **Step 2: 创建 DataSourceTypeRepository**

```java
package com.maidc.data.repository;

import com.maidc.data.entity.DataSourceTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DataSourceTypeRepository extends JpaRepository<DataSourceTypeEntity, Long> {
    Optional<DataSourceTypeEntity> findByTypeCode(String typeCode);
}
```

- [ ] **Step 3: 提交**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/entity/DataSourceTypeEntity.java maidc-parent/maidc-data/src/main/java/com/maidc/data/repository/DataSourceTypeRepository.java
git commit -m "feat(datasource): add DataSourceType entity and repository"
```

---

## Task 2: DataSourceHealth Entity + Repository

**Files:**
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/entity/DataSourceHealthEntity.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/repository/DataSourceHealthRepository.java`

- [ ] **Step 1: 创建 DataSourceHealthEntity**

```java
package com.maidc.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "r_data_source_health", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.r_data_source_health SET is_deleted = true WHERE id = ?")
public class DataSourceHealthEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "check_type", nullable = false, length = 32)
    private String checkType;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;

    @Column(name = "org_id", nullable = false)
    private Long orgId = 0L;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @PrePersist
    protected void onCreate() {
        if (checkedAt == null) checkedAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 2: 创建 DataSourceHealthRepository**

```java
package com.maidc.data.repository;

import com.maidc.data.entity.DataSourceHealthEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DataSourceHealthRepository extends JpaRepository<DataSourceHealthEntity, Long> {

    List<DataSourceHealthEntity> findTop50BySourceIdAndIsDeletedFalseOrderByCheckedAtDesc(Long sourceId);

    Page<DataSourceHealthEntity> findBySourceIdAndIsDeletedFalseOrderByCheckedAtDesc(Long sourceId, Pageable pageable);

    @Query("SELECT h FROM DataSourceHealthEntity h WHERE h.sourceId = :sourceId " +
           "AND h.isDeleted = false AND h.checkedAt >= :since ORDER BY h.checkedAt DESC")
    List<DataSourceHealthEntity> findRecentHealth(@Param("sourceId") Long sourceId,
                                                   @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(h) FROM DataSourceHealthEntity h WHERE h.sourceId = :sourceId " +
           "AND h.isDeleted = false AND h.checkedAt >= :since")
    long countSince(@Param("sourceId") Long sourceId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(h) FROM DataSourceHealthEntity h WHERE h.sourceId = :sourceId " +
           "AND h.isDeleted = false AND h.status = 'SUCCESS' AND h.checkedAt >= :since")
    long countSuccessSince(@Param("sourceId") Long sourceId, @Param("since") LocalDateTime since);

    @Query("SELECT AVG(CAST(h.latencyMs AS double)) FROM DataSourceHealthEntity h " +
           "WHERE h.sourceId = :sourceId AND h.isDeleted = false AND h.status = 'SUCCESS' AND h.checkedAt >= :since")
    Double avgLatencySince(@Param("sourceId") Long sourceId, @Param("since") LocalDateTime since);
}
```

- [ ] **Step 3: 提交**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/entity/DataSourceHealthEntity.java maidc-parent/maidc-data/src/main/java/com/maidc/data/repository/DataSourceHealthRepository.java
git commit -m "feat(datasource): add DataSourceHealth entity and repository"
```

---

## Task 3: 修改 DataSourceEntity

**Files:**
- Modify: `maidc-parent/maidc-data/src/main/java/com/maidc/data/entity/DataSourceEntity.java`

- [ ] **Step 1: 新增 sourceTypeCode + connectionParams 字段**

在现有 DataSourceEntity 中新增两个字段（保留旧字段，不破坏现有功能）：

```java
@Column(name = "source_type_code", length = 64)
private String sourceTypeCode;

@Convert(converter = JsonNodeConverter.class)
@Column(name = "connection_params", columnDefinition = "jsonb")
private JsonNode connectionParams;
```

需要在文件顶部增加 import：
```java
import com.fasterxml.jackson.databind.JsonNode;
import com.maidc.common.jpa.converter.JsonNodeConverter;
import jakarta.persistence.Convert;
```

- [ ] **Step 2: 提交**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/entity/DataSourceEntity.java
git commit -m "feat(datasource): add sourceTypeCode and connectionParams fields"
```

---

## Task 4: ConnectionTester 策略模式

**Files:**
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/service/connection/ConnectionTester.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/service/connection/ConnectionTestResult.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/service/connection/JdbcConnectionTester.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/service/connection/HttpConnectionTester.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/service/connection/FileConnectionTester.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/service/connection/ConnectionTesterFactory.java`

- [ ] **Step 1: 创建接口和结果类**

`ConnectionTestResult.java`:
```java
package com.maidc.data.service.connection;

import java.util.Map;

public record ConnectionTestResult(
    boolean success,
    String message,
    int latencyMs,
    Map<String, Object> details
) {
    public static ConnectionTestResult ok(int latencyMs) {
        return new ConnectionTestResult(true, "连接成功", latencyMs, Map.of());
    }
    public static ConnectionTestResult ok(int latencyMs, Map<String, Object> details) {
        return new ConnectionTestResult(true, "连接成功", latencyMs, details);
    }
    public static ConnectionTestResult fail(String message) {
        return new ConnectionTestResult(false, message, -1, Map.of());
    }
}
```

`ConnectionTester.java`:
```java
package com.maidc.data.service.connection;

import java.util.Map;

public interface ConnectionTester {
    String getType();
    ConnectionTestResult test(Map<String, Object> params);
}
```

- [ ] **Step 2: 创建 JdbcConnectionTester**

```java
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
```

- [ ] **Step 3: 创建 HttpConnectionTester**

```java
package com.maidc.data.service.connection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
public class HttpConnectionTester implements ConnectionTester {

    @Override
    public String getType() { return "HTTP"; }

    @Override
    public ConnectionTestResult test(Map<String, Object> params) {
        String url = (String) params.get("url");
        String method = (String) params.getOrDefault("method", "GET");
        String authType = (String) params.getOrDefault("auth_type", "NONE");
        String authToken = (String) params.get("auth_token");

        if (url == null || url.isBlank()) {
            return ConnectionTestResult.fail("接口地址不能为空");
        }

        long start = System.currentTimeMillis();
        try {
            var client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            var requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10));

            if ("BEARER".equals(authType) && authToken != null) {
                requestBuilder.header("Authorization", "Bearer " + authToken);
            } else if ("BASIC".equals(authType) && authToken != null) {
                requestBuilder.header("Authorization", "Basic " + authToken);
            }

            if ("POST".equals(method)) {
                requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
            } else {
                requestBuilder.GET();
            }

            HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            int latency = (int) (System.currentTimeMillis() - start);
            int statusCode = response.statusCode();

            if (statusCode >= 200 && statusCode < 400) {
                return ConnectionTestResult.ok(latency, Map.of("statusCode", statusCode));
            } else {
                return ConnectionTestResult.fail("HTTP " + statusCode);
            }
        } catch (Exception e) {
            log.warn("HTTP连接测试失败: {}", e.getMessage());
            return ConnectionTestResult.fail(e.getMessage());
        }
    }
}
```

- [ ] **Step 4: 创建 FileConnectionTester**

```java
package com.maidc.data.service.connection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.File;
import java.util.Map;

@Slf4j
@Component
public class FileConnectionTester implements ConnectionTester {

    @Override
    public String getType() { return "FILE_CHECK"; }

    @Override
    public ConnectionTestResult test(Map<String, Object> params) {
        String filePath = (String) params.get("file_path");
        if (filePath == null || filePath.isBlank()) {
            return ConnectionTestResult.fail("文件路径不能为空");
        }

        long start = System.currentTimeMillis();
        File file = new File(filePath);
        int latency = (int) (System.currentTimeMillis() - start);

        if (!file.exists()) {
            return ConnectionTestResult.fail("文件不存在: " + filePath);
        }
        if (!file.canRead()) {
            return ConnectionTestResult.fail("文件不可读: " + filePath);
        }

        long sizeKB = file.length() / 1024;
        return ConnectionTestResult.ok(latency, Map.of("sizeKB", sizeKB, "fileName", file.getName()));
    }
}
```

- [ ] **Step 5: 创建 ConnectionTesterFactory**

```java
package com.maidc.data.service.connection;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ConnectionTesterFactory {

    private final Map<String, ConnectionTester> testerMap;

    public ConnectionTesterFactory(List<ConnectionTester> testers) {
        this.testerMap = testers.stream()
                .collect(Collectors.toMap(ConnectionTester::getType, Function.identity()));
    }

    public ConnectionTester getTester(String testCommand) {
        return testerMap.get(testCommand);
    }
}
```

- [ ] **Step 6: 提交**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/service/connection/
git commit -m "feat(datasource): add ConnectionTester strategy pattern with JDBC/HTTP/File implementations"
```

---

## Task 5: DataSourceTypeService + Controller

**Files:**
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/service/DataSourceTypeService.java`
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/controller/DataSourceTypeController.java`

- [ ] **Step 1: 创建 DataSourceTypeService**

```java
package com.maidc.data.service;

import com.maidc.data.entity.DataSourceTypeEntity;
import com.maidc.data.repository.DataSourceTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceTypeService {

    private final DataSourceTypeRepository dataSourceTypeRepository;

    public List<DataSourceTypeEntity> listAll() {
        return dataSourceTypeRepository.findAll();
    }

    public DataSourceTypeEntity getByTypeCode(String typeCode) {
        return dataSourceTypeRepository.findByTypeCode(typeCode).orElse(null);
    }

    @Transactional
    public DataSourceTypeEntity create(DataSourceTypeEntity entity) {
        return dataSourceTypeRepository.save(entity);
    }

    @Transactional
    public DataSourceTypeEntity update(String typeCode, DataSourceTypeEntity entity) {
        DataSourceTypeEntity existing = dataSourceTypeRepository.findByTypeCode(typeCode).orElse(null);
        if (existing == null) return null;
        entity.setId(existing.getId());
        entity.setCreatedBy(existing.getCreatedBy());
        entity.setCreatedAt(existing.getCreatedAt());
        return dataSourceTypeRepository.save(entity);
    }

    @Transactional
    public boolean delete(String typeCode) {
        DataSourceTypeEntity existing = dataSourceTypeRepository.findByTypeCode(typeCode).orElse(null);
        if (existing == null) return false;
        if (Boolean.TRUE.equals(existing.getIsBuiltin())) {
            throw new IllegalStateException("内置类型不可删除: " + typeCode);
        }
        dataSourceTypeRepository.deleteById(existing.getId());
        return true;
    }
}
```

- [ ] **Step 2: 创建 DataSourceTypeController**

```java
package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.entity.DataSourceTypeEntity;
import com.maidc.data.service.DataSourceTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cdr/datasource-types")
@RequiredArgsConstructor
public class DataSourceTypeController {

    private final DataSourceTypeService dataSourceTypeService;

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping
    public R<List<DataSourceTypeEntity>> listTypes() {
        return R.ok(dataSourceTypeService.listAll());
    }

    @PreAuthorize("hasPermission('cdr:read')")
    @GetMapping("/{code}")
    public R<DataSourceTypeEntity> getType(@PathVariable String code) {
        DataSourceTypeEntity entity = dataSourceTypeService.getByTypeCode(code);
        if (entity == null) return R.fail(404, "类型不存在");
        return R.ok(entity);
    }

    @PreAuthorize("hasPermission('cdr:create')")
    @PostMapping
    public R<DataSourceTypeEntity> createType(@RequestBody DataSourceTypeEntity entity) {
        return R.ok(dataSourceTypeService.create(entity));
    }

    @PreAuthorize("hasPermission('cdr:create')")
    @PutMapping("/{code}")
    public R<DataSourceTypeEntity> updateType(@PathVariable String code,
                                               @RequestBody DataSourceTypeEntity entity) {
        DataSourceTypeEntity result = dataSourceTypeService.update(code, entity);
        if (result == null) return R.fail(404, "类型不存在");
        return R.ok(result);
    }

    @PreAuthorize("hasPermission('cdr:create')")
    @DeleteMapping("/{code}")
    public R<Void> deleteType(@PathVariable String code) {
        try {
            dataSourceTypeService.delete(code);
            return R.ok();
        } catch (IllegalStateException e) {
            return R.fail(400, e.getMessage());
        }
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/service/DataSourceTypeService.java maidc-parent/maidc-data/src/main/java/com/maidc/data/controller/DataSourceTypeController.java
git commit -m "feat(datasource): add DataSourceType service and controller"
```

---

## Task 6: DataSourceHealthService

**Files:**
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/service/DataSourceHealthService.java`

- [ ] **Step 1: 创建 DataSourceHealthService**

```java
package com.maidc.data.service;

import com.maidc.data.entity.DataSourceHealthEntity;
import com.maidc.data.repository.DataSourceHealthRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceHealthService {

    private final DataSourceHealthRepository healthRepository;

    @Transactional
    public DataSourceHealthEntity recordHealth(Long sourceId, String checkType,
                                                String status, Integer latencyMs, String errorMessage) {
        DataSourceHealthEntity entity = new DataSourceHealthEntity();
        entity.setSourceId(sourceId);
        entity.setCheckType(checkType);
        entity.setStatus(status);
        entity.setLatencyMs(latencyMs);
        entity.setErrorMessage(errorMessage);
        entity.setCheckedAt(LocalDateTime.now());
        return healthRepository.save(entity);
    }

    public List<DataSourceHealthEntity> getRecentHealth(Long sourceId, int limit) {
        return healthRepository.findTop50BySourceIdAndIsDeletedFalseOrderByCheckedAtDesc(sourceId)
                .stream().limit(limit).toList();
    }

    public Map<String, Object> getHealthStats(Long sourceId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        long total = healthRepository.countSince(sourceId, since);
        long success = healthRepository.countSuccessSince(sourceId, since);
        Double avgLatency = healthRepository.avgLatencySince(sourceId, since);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalChecks", total);
        stats.put("successCount", success);
        stats.put("failCount", total - success);
        stats.put("availabilityRate", total > 0 ? (double) success / total : 0);
        stats.put("avgLatencyMs", avgLatency != null ? avgLatency : 0);
        stats.put("since", since);
        return stats;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/service/DataSourceHealthService.java
git commit -m "feat(datasource): add DataSourceHealth service for health monitoring"
```

---

## Task 7: 增强 DataSourceService + Controller

**Files:**
- Modify: `maidc-parent/maidc-data/src/main/java/com/maidc/data/service/DataSourceService.java`
- Modify: `maidc-parent/maidc-data/src/main/java/com/maidc/data/controller/DataSourceController.java`

- [ ] **Step 1: 增强 DataSourceService**

在现有 DataSourceService 中注入新依赖并添加方法：

```java
// 新增注入
private final DataSourceTypeService dataSourceTypeService;
private final DataSourceHealthService dataSourceHealthService;
private final ConnectionTesterFactory connectionTesterFactory;

// 新增方法：创建前测试连接（不依赖已保存ID）
public ConnectionTestResult testConnection(String typeCode, Map<String, Object> connectionParams) {
    DataSourceTypeEntity type = dataSourceTypeService.getByTypeCode(typeCode);
    if (type == null) {
        return ConnectionTestResult.fail("数据源类型不存在: " + typeCode);
    }
    ConnectionTester tester = connectionTesterFactory.getTester(type.getTestCommand());
    if (tester == null) {
        return ConnectionTestResult.fail("无可用测试器: " + type.getTestCommand());
    }
    Map<String, Object> params = new HashMap<>(connectionParams);
    params.put("_typeCode", typeCode);
    return tester.test(params);
}

// 新增方法：已保存数据源测试连接 + 记录健康
public ConnectionTestResult testSavedConnection(Long id) {
    DataSourceEntity ds = getDataSource(id);
    if (ds == null) return ConnectionTestResult.fail("数据源不存在");

    DataSourceTypeEntity type = dataSourceTypeService.getByTypeCode(ds.getSourceTypeCode());
    if (type == null) return ConnectionTestResult.fail("数据源类型未配置");

    ConnectionTester tester = connectionTesterFactory.getTester(type.getTestCommand());
    if (tester == null) return ConnectionTestResult.fail("无可用测试器");

    Map<String, Object> params = new HashMap<>();
    if (ds.getConnectionParams() != null) {
        ds.getConnectionParams().fields().forEachRemaining(e ->
            params.put(e.getKey(), e.getValue().isTextual() ? e.getValue().asText() : e.getValue()));
    }
    params.put("_typeCode", ds.getSourceTypeCode());

    ConnectionTestResult result = tester.test(params);

    dataSourceHealthService.recordHealth(id, "MANUAL",
        result.success() ? "SUCCESS" : "FAIL",
        result.success() ? result.latencyMs() : null,
        result.success() ? null : result.message());

    return result;
}
```

需要在 DataSourceService 顶部增加 import：
```java
import com.maidc.data.entity.DataSourceTypeEntity;
import com.maidc.data.service.connection.*;
import java.util.HashMap;
import java.util.Map;
```

- [ ] **Step 2: 修改 DataSourceController**

替换现有的 mock testConnection 并新增端点：

```java
// 替换现有的 testConnection 方法
@PreAuthorize("hasPermission('cdr:create')")
@PostMapping("/{id}/test-connection")
public R<Map<String, Object>> testConnection(@PathVariable Long id) {
    ConnectionTestResult result = dataSourceService.testSavedConnection(id);
    Map<String, Object> resp = new HashMap<>();
    resp.put("success", result.success());
    resp.put("message", result.message());
    resp.put("latencyMs", result.latencyMs());
    resp.put("details", result.details());
    return R.ok(resp);
}

// 新增：创建前测试（不依赖已保存ID）
@PreAuthorize("hasPermission('cdr:create')")
@PostMapping("/test-connection")
public R<Map<String, Object>> testConnectionPreSave(@RequestBody Map<String, Object> body) {
    String typeCode = (String) body.get("type_code");
    @SuppressWarnings("unchecked")
    Map<String, Object> params = (Map<String, Object>) body.get("connection_params");
    ConnectionTestResult result = dataSourceService.testConnection(typeCode, params);
    Map<String, Object> resp = new HashMap<>();
    resp.put("success", result.success());
    resp.put("message", result.message());
    resp.put("latencyMs", result.latencyMs());
    resp.put("details", result.details());
    return R.ok(resp);
}

// 新增：健康检查历史
@PreAuthorize("hasPermission('cdr:read')")
@GetMapping("/{id}/health")
public R<List<DataSourceHealthEntity>> getHealthHistory(
        @PathVariable Long id,
        @RequestParam(defaultValue = "50") int limit) {
    return R.ok(dataSourceHealthService.getRecentHealth(id, limit));
}

// 新增：健康统计
@PreAuthorize("hasPermission('cdr:read')")
@GetMapping("/{id}/health/stats")
public R<Map<String, Object>> getHealthStats(
        @PathVariable Long id,
        @RequestParam(defaultValue = "30") int days) {
    return R.ok(dataSourceHealthService.getHealthStats(id, days));
}
```

新增 import：
```java
import com.maidc.data.entity.DataSourceHealthEntity;
import com.maidc.data.service.DataSourceHealthService;
import com.maidc.data.service.connection.ConnectionTestResult;
import java.util.HashMap;
```

Controller 构造器中注入 DataSourceHealthService（或使用 @RequiredArgsConstructor 自动注入）。

- [ ] **Step 3: 构建验证**

```bash
cd maidc-parent && mvn compile -pl maidc-data -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/service/DataSourceService.java maidc-parent/maidc-data/src/main/java/com/maidc/data/controller/DataSourceController.java
git commit -m "feat(datasource): enhance service/controller with real connection testing and health endpoints"
```

---

## Task 8: 添加 MySQL JDBC 驱动依赖

**Files:**
- Modify: `maidc-parent/maidc-data/pom.xml`

- [ ] **Step 1: 在 pom.xml 中添加 MySQL 驱动**

在 `<dependencies>` 中 PostgreSQL 依赖之后添加：

```xml
<!-- MySQL (for data source connection testing) -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency>
```

不需要指定 version，由 Spring Boot parent BOM 管理。

- [ ] **Step 2: 构建验证**

```bash
cd maidc-parent && mvn compile -pl maidc-data -q
```

- [ ] **Step 3: 提交**

```bash
git add maidc-parent/maidc-data/pom.xml
git commit -m "feat(datasource): add MySQL JDBC driver for connection testing"
```
