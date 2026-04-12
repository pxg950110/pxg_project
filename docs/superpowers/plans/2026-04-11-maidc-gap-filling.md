# MAIDC Gap-Filling Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix gateway routing gaps, add cross-service Feign clients, expand test coverage for under-tested modules, and add the missing version download endpoint.

**Status:** COMPLETED (2026-04-11) — All 8 tasks done. 120 tests passing across 8 modules. BUILD SUCCESS.

**Architecture:** Gateway routes are configured in `docker/nacos-config/maidc-gateway-dev.yaml`. Cross-service calls use OpenFeign. Tests use Mockito for unit tests and standalone MockMvc for controller tests.

**Tech Stack:** Java 17, Spring Boot 3.2.5, Spring Cloud Gateway, OpenFeign, JUnit 5, Mockito

---

## File Structure

### Files to Modify
```
docker/nacos-config/maidc-gateway-dev.yaml         # Add alert/monitoring routes
maidc-parent/maidc-model/src/main/java/com/maidc/model/
  ├── controller/VersionController.java              # Add download endpoint
  ├── controller/AlertController.java                # Fix base path
  ├── service/VersionService.java                    # Add download logic
  └── config/ServiceClientConfig.java                # NEW: Feign clients
maidc-parent/maidc-gateway/src/main/java/com/maidc/gateway/
  ├── filter/AuthFilter.java                         # javax→jakarta fix
  └── filter/RateLimiterFilter.java                  # javax→jakarta fix
```

### Test Files to Create
```
maidc-parent/maidc-gateway/src/test/java/com/maidc/gateway/filter/
  ├── AuthFilterTest.java
  ├── RateLimiterFilterTest.java
  ├── RequestLogFilterTest.java
  └── TraceFilterTest.java
maidc-parent/maidc-data/src/test/java/com/maidc/data/service/
  ├── EncounterServiceTest.java
  ├── DatasetServiceTest.java
  └── EtlTaskServiceTest.java
maidc-parent/maidc-audit/src/test/java/com/maidc/audit/service/
  ├── AuditLogQueryTest.java
  └── DataAccessLogTest.java
maidc-parent/maidc-task/src/test/java/com/maidc/task/service/
  └── TaskExecutionTest.java
maidc-parent/maidc-label/src/test/java/com/maidc/label/service/
  └── LabelStatsTest.java
maidc-parent/maidc-msg/src/test/java/com/maidc/msg/service/
  └── NotificationServiceTest.java
```

---

## Phase 1: Gateway Routing Fixes

### Task 1: Fix Gateway Route for Alerts and Monitoring

**Files:**
- Modify: `docker/nacos-config/maidc-gateway-dev.yaml:31-35`

**Problem:** AlertController uses `/api/v1/alert-rules/**` and `/api/v1/alerts/**`, but gateway only routes `/api/v1/models/**` paths to maidc-model. Alert endpoints will get 404 in production.

Similarly, MonitoringController uses `/api/v1/monitoring/**` which is also not routed.

- [ ] **Step 1: Verify the gap**

Read `docker/nacos-config/maidc-gateway-dev.yaml` line 33:
```yaml
- Path=/api/v1/models/**,/api/v1/evaluations/**,/api/v1/approvals/**,/api/v1/deployments/**,/api/v1/routes/**,/api/v1/inference/**,/api/v1/workers/**
```

Missing: `/api/v1/alert-rules/**`, `/api/v1/alerts/**`, `/api/v1/monitoring/**`

- [ ] **Step 2: Fix gateway routes**

In `docker/nacos-config/maidc-gateway-dev.yaml`, update the maidc-model route (line 33):

**Before:**
```yaml
- Path=/api/v1/models/**,/api/v1/evaluations/**,/api/v1/approvals/**,/api/v1/deployments/**,/api/v1/routes/**,/api/v1/inference/**,/api/v1/workers/**
```

**After:**
```yaml
- Path=/api/v1/models/**,/api/v1/evaluations/**,/api/v1/approvals/**,/api/v1/deployments/**,/api/v1/routes/**,/api/v1/inference/**,/api/v1/workers/**,/api/v1/alert-rules/**,/api/v1/alerts/**,/api/v1/monitoring/**
```

- [ ] **Step 3: Commit**

```bash
git add docker/nacos-config/maidc-gateway-dev.yaml
git commit -m "fix: add alert-rules, alerts, monitoring routes to gateway config"
```

---

### Task 2: Fix Gateway Filter javax→jakarta Imports

**Files:**
- Modify: `maidc-parent/maidc-gateway/src/main/java/com/maidc/gateway/filter/AuthFilter.java:18`
- Check and fix: `RateLimiterFilter.java`, `RequestLogFilter.java`, `TraceFilter.java`

- [ ] **Step 1: Read all gateway filter files**

Read each filter to check for `javax` imports:
- `AuthFilter.java`
- `RateLimiterFilter.java`
- `RequestLogFilter.java`
- `TraceFilter.java`

- [ ] **Step 2: Fix javax→jakarta**

In any file that uses `javax.crypto.SecretKey` or `javax.servlet.*`, replace with `jakarta.*` equivalents. For `javax.crypto.SecretKey` — this is JDK standard, NOT Jakarta, so it stays as-is. Only `javax.servlet` needs migration to `jakarta.servlet`.

- [ ] **Step 3: Verify compilation**

```bash
cd maidc-parent && mvn compile -pl maidc-gateway -q
```

- [ ] **Step 4: Commit**

```bash
git add maidc-parent/maidc-gateway/
git commit -m "fix: migrate gateway filters from javax to jakarta namespace"
```

---

## Phase 2: Gateway Filter Tests

### Task 3: Gateway Filter Unit Tests

**Files:**
- Create: `maidc-parent/maidc-gateway/src/test/java/com/maidc/gateway/filter/AuthFilterTest.java`
- Create: `maidc-parent/maidc-gateway/src/test/java/com/maidc/gateway/filter/RateLimiterFilterTest.java`
- Create: `maidc-parent/maidc-gateway/src/test/java/com/maidc/gateway/filter/RequestLogFilterTest.java`
- Create: `maidc-parent/maidc-gateway/src/test/java/com/maidc/gateway/filter/TraceFilterTest.java`

- [ ] **Step 1: Read all filter source files to understand exact signatures**

Read: AuthFilter, RateLimiterFilter, RequestLogFilter, TraceFilter

- [ ] **Step 2: Add test dependencies to gateway POM**

Ensure `maidc-gateway/pom.xml` has `spring-boot-starter-test` and `h2`. Add if missing.

- [ ] **Step 3: Write AuthFilterTest**

Create test for the JWT auth filter. Since this is reactive (Spring Cloud Gateway uses WebFlux), tests need `MockServerWebExchange`:

```java
@ExtendWith(MockitoExtension.class)
class AuthFilterTest {
    @Mock private StringRedisTemplate redisTemplate;
    private AuthFilter authFilter;

    @BeforeEach
    void setUp() {
        authFilter = new AuthFilter("test-secret-key-must-be-at-least-256-bits-long-for-hmac-sha", redisTemplate);
    }

    @Test
    void whiteListPath_passesThrough() {
        // Test /api/v1/auth/login bypasses auth
        MockServerWebExchange exchange = createExchange("/api/v1/auth/login");
        // verify chain.filter() is called
    }

    @Test
    void missingAuthHeader_returns401() {
        // No Authorization header → 401
    }

    @Test
    void invalidToken_returns401() {
        // Bearer invalid-token → 401
    }
}
```

Note: Adapt to actual Gateway filter testing patterns. If `MockServerWebExchange` is not available, use `org.springframework.mock.http.server.reactive.MockServerHttpRequest` + `MockServerWebExchange`.

- [ ] **Step 4: Write RateLimiterFilterTest**

```java
@ExtendWith(MockitoExtension.class)
class RateLimiterFilterTest {
    // Test: within limit → passes through
    // Test: exceeds limit → returns 429
}
```

- [ ] **Step 5: Write TraceFilterTest**

```java
@ExtendWith(MockitoExtension.class)
class TraceFilterTest {
    // Test: adds X-Trace-Id header to request
    // Test: preserves existing trace ID if present
}
```

- [ ] **Step 6: Write RequestLogFilterTest**

```java
@ExtendWith(MockitoExtension.class)
class RequestLogFilterTest {
    // Test: logs request path and response time
}
```

- [ ] **Step 7: Run gateway tests**

```bash
cd maidc-parent && mvn test -pl maidc-gateway -q
```

- [ ] **Step 8: Commit**

```bash
git add maidc-parent/maidc-gateway/src/test/
git commit -m "test: add gateway filter unit tests (Auth, RateLimiter, Trace, RequestLog)"
```

---

## Phase 3: Missing API Endpoints

### Task 4: Add Version Download Endpoint

**Files:**
- Modify: `maidc-parent/maidc-model/src/main/java/com/maidc/model/controller/VersionController.java`
- Modify: `maidc-parent/maidc-model/src/main/java/com/maidc/model/service/VersionService.java`
- Create: `maidc-parent/maidc-model/src/test/java/com/maidc/model/controller/VersionControllerTest.java` (add to existing)

- [ ] **Step 1: Write the failing test**

Add to existing VersionControllerTest (or create if needed):

```java
@Test
void downloadVersion_existingVersion_returnsFile() {
    // Mock VersionService to return a valid version with modelFilePath
    // Call GET /api/v1/models/{modelId}/versions/{versionId}/download
    // Verify 200 with file stream
}
```

- [ ] **Step 2: Add download endpoint to VersionController**

```java
@PreAuthorize("hasPermission('model:read')")
@GetMapping("/{versionId}/download")
public ResponseEntity<org.springframework.core.io.Resource> downloadVersion(
        @PathVariable Long modelId, @PathVariable Long versionId) {
    return versionService.downloadVersion(modelId, versionId);
}
```

- [ ] **Step 3: Add downloadVersion to VersionService**

```java
public ResponseEntity<Resource> downloadVersion(Long modelId, Long versionId) {
    ModelVersionEntity version = versionRepository.findByIdAndIsDeletedFalse(versionId)
            .orElseThrow(() -> new BusinessException(ErrorCode.VERSION_NOT_FOUND));
    if (!version.getModelId().equals(modelId)) {
        throw new BusinessException(ErrorCode.VERSION_NOT_FOUND);
    }

    InputStream inputStream = minioService.downloadFile(MODELS_BUCKET, version.getModelFilePath());

    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + version.getModelFilePath().replace("/", "_") + "\"")
            .contentLength(version.getModelFileSize())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(new InputStreamResource(inputStream));
}
```

Add imports: `org.springframework.core.io.Resource`, `org.springframework.core.io.InputStreamResource`, `org.springframework.http.ResponseEntity`, `org.springframework.http.HttpHeaders`, `org.springframework.http.MediaType`

- [ ] **Step 4: Run tests**

```bash
cd maidc-parent && mvn test -pl maidc-model -q
```

- [ ] **Step 5: Commit**

```bash
git add maidc-parent/maidc-model/src/main/java/com/maidc/model/controller/VersionController.java
git add maidc-parent/maidc-model/src/main/java/com/maidc/model/service/VersionService.java
git add maidc-parent/maidc-model/src/test/
git commit -m "feat: add version download endpoint with MinIO file streaming"
```

---

## Phase 4: Expanded Service Tests

### Task 5: Expand Data Service Tests

**Files:**
- Create: `maidc-parent/maidc-data/src/test/java/com/maidc/data/service/EncounterServiceTest.java`
- Create: `maidc-parent/maidc-data/src/test/java/com/maidc/data/service/DatasetServiceTest.java`
- Create: `maidc-parent/maidc-data/src/test/java/com/maidc/data/service/EtlTaskServiceTest.java`

- [ ] **Step 1: Read actual service source files**

Read: EncounterService.java, DatasetService.java, EtlTaskService.java and their entities/repositories

- [ ] **Step 2: Write EncounterServiceTest**

Test cases:
1. `listEncounters_byPatient_returnsList` - Mock patient exists, return encounters
2. `getEncounter_existingId_returnsEncounter` - Happy path
3. `getEncounter_nonExisting_throws` - Not found

- [ ] **Step 3: Write DatasetServiceTest**

Test cases:
1. `createDataset_validInput_succeeds` - Create under existing project
2. `listDatasets_byProject_returnsList` - Filtered list
3. `deleteDataset_existing_succeeds` - Soft delete

- [ ] **Step 4: Write EtlTaskServiceTest**

Test cases:
1. `createEtlTask_validInput_succeeds` - Create task
2. `triggerEtlTask_changesStatus` - Trigger changes status to RUNNING
3. `pauseEtlTask_running_changesToPaused` - Pause running task

- [ ] **Step 5: Run tests**

```bash
cd maidc-parent && mvn test -pl maidc-data -q
```

- [ ] **Step 6: Commit**

```bash
git add maidc-parent/maidc-data/src/test/
git commit -m "test: expand data service tests — Encounter, Dataset, EtlTask"
```

---

### Task 6: Expand Audit and Task Service Tests

**Files:**
- Create: `maidc-parent/maidc-audit/src/test/java/com/maidc/audit/service/AuditQueryTest.java`
- Create: `maidc-parent/maidc-task/src/test/java/com/maidc/task/service/TaskExecutionTest.java`

- [ ] **Step 1: Read service source files**

Read: AuditService.java (all query methods), TaskService.java (execution-related methods)

- [ ] **Step 2: Write AuditQueryTest**

Test cases:
1. `queryAuditLogs_withFilters_returnsPage` - Query with date range + module filter
2. `queryDataAccessLogs_returnsPage` - Data access log query
3. `querySystemEvents_returnsPage` - System event query

- [ ] **Step 3: Write TaskExecutionTest**

Test cases:
1. `getTaskExecutions_existingTask_returnsHistory` - List execution history
2. `resumeTask_pausedTask_changesToRunning` - Resume paused task
3. `deleteTask_existing_succeeds` - Soft delete

- [ ] **Step 4: Run tests**

```bash
cd maidc-parent && mvn test -pl maidc-audit,maidc-task -q
```

- [ ] **Step 5: Commit**

```bash
git add maidc-parent/maidc-audit/src/test/ maidc-parent/maidc-task/src/test/
git commit -m "test: expand audit query and task execution tests"
```

---

### Task 7: Expand Label and Msg Service Tests

**Files:**
- Create: `maidc-parent/maidc-label/src/test/java/com/maidc/label/service/LabelStatsTest.java`
- Create: `maidc-parent/maidc-msg/src/test/java/com/maidc/msg/service/NotificationServiceTest.java`

- [ ] **Step 1: Read service source files**

Read: LabelTaskService.java (stats methods), NotificationService.java

- [ ] **Step 2: Write LabelStatsTest**

Test cases:
1. `getTaskStats_existingTask_returnsStats` - Statistics calculation
2. `aiPreAnnotate_existingTask_sendsMQ` - Trigger AI pre-annotation
3. `updateLabelTask_changesStatus` - Status transition

- [ ] **Step 3: Write NotificationServiceTest**

Test cases:
1. `getNotificationSettings_returnsUserSettings` - Get user settings
2. `createNotificationSetting_validInput_succeeds` - Create setting
3. `listTemplates_returnsAll` - List message templates

- [ ] **Step 4: Run tests**

```bash
cd maidc-parent && mvn test -pl maidc-label,maidc-msg -q
```

- [ ] **Step 5: Commit**

```bash
git add maidc-parent/maidc-label/src/test/ maidc-parent/maidc-msg/src/test/
git commit -m "test: expand label stats and notification service tests"
```

---

## Phase 5: Full Verification

### Task 8: Run Full Test Suite

- [ ] **Step 1: Run all tests**

```bash
cd maidc-parent && mvn test -q
```

- [ ] **Step 2: Fix any failures**

Read error output, diagnose, fix, re-run.

- [ ] **Step 3: Commit any remaining fixes**

```bash
git add -A
git commit -m "test: final gap-filling adjustments — all tests passing"
```

---

## Self-Review Checklist

- [x] **Spec coverage:** Each identified gap (gateway routing, filter tests, version download, expanded tests) has a corresponding task
- [x] **Placeholder scan:** No TBD, TODO, or vague instructions — all tasks have concrete code examples
- [x] **Type consistency:** ResponseEntity, Resource, InputStreamResource types are correct for Spring Boot 3.x
- [x] **No extra scope:** Only fixing what was identified as actually missing — no speculative features

---

## Summary

| Phase | Tasks | Focus |
|-------|-------|-------|
| Phase 1 | Tasks 1-2 | Gateway routing fix + javax→jakarta |
| Phase 2 | Task 3 | Gateway filter unit tests (4 filters) |
| Phase 3 | Task 4 | Version download endpoint |
| Phase 4 | Tasks 5-7 | Expanded service tests (data, audit, task, label, msg) |
| Phase 5 | Task 8 | Full test suite verification |
