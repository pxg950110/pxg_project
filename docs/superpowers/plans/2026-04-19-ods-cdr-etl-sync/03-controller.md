# Phase 3: Controller 层

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** 实现 5 个 Controller，暴露所有 ETL 管理 REST API，包含权限控制和操作日志。

**Architecture:** 遵循现有 Controller 模式：`@RestController` + `@RequiredArgsConstructor` + `R<T>` 响应包装 + `@PreAuthorize` + `@OperLog`。

**Tech Stack:** Spring Web / Spring Security / Validation

---

## File Structure

```
maidc-parent/maidc-data/src/main/java/com/maidc/data/controller/
  EtlPipelineController.java          (新建)
  EtlStepController.java              (新建)
  EtlFieldMappingController.java      (新建)
  EtlExecutionController.java         (新建)
  EtlMetadataController.java          (新建)
```

---

### Task 3.1: EtlPipelineController

**Files:**
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/controller/EtlPipelineController.java`

- [ ] **Step 1: 创建 Controller**

```java
package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.common.core.result.PageResult;
import com.maidc.data.dto.etl.EtlPipelineCreateDTO;
import com.maidc.data.dto.etl.EtlPipelineQueryDTO;
import com.maidc.data.service.etl.EtlExecutionService;
import com.maidc.data.service.etl.EtlPipelineService;
import com.maidc.data.vo.EtlExecutionVO;
import com.maidc.data.vo.EtlPipelineDetailVO;
import com.maidc.data.vo.EtlPipelineVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cdr/etl/pipelines")
@RequiredArgsConstructor
public class EtlPipelineController {

    private final EtlPipelineService pipelineService;
    private final EtlExecutionService executionService;

    @PreAuthorize("hasPermission('data:read')")
    @GetMapping
    public R<PageResult<EtlPipelineVO>> listPipelines(EtlPipelineQueryDTO query) {
        var page = pipelineService.listPipelines(query);
        return R.ok(new PageResult<>(page.getContent(), page.getTotalElements()));
    }

    @PreAuthorize("hasPermission('data:create')")
    @PostMapping
    public R<EtlPipelineVO> createPipeline(@RequestBody @Valid EtlPipelineCreateDTO dto) {
        return R.ok(pipelineService.createPipeline(dto));
    }

    @PreAuthorize("hasPermission('data:read')")
    @GetMapping("/{id}")
    public R<EtlPipelineDetailVO> getPipelineDetail(@PathVariable Long id) {
        return R.ok(pipelineService.getPipelineDetail(id));
    }

    @PreAuthorize("hasPermission('data:update')")
    @PutMapping("/{id}")
    public R<EtlPipelineVO> updatePipeline(@PathVariable Long id,
                                            @RequestBody @Valid EtlPipelineCreateDTO dto) {
        return R.ok(pipelineService.updatePipeline(id, dto));
    }

    @PreAuthorize("hasPermission('data:delete')")
    @DeleteMapping("/{id}")
    public R<Void> deletePipeline(@PathVariable Long id) {
        pipelineService.deletePipeline(id);
        return R.ok();
    }

    @PreAuthorize("hasPermission('data:update')")
    @PostMapping("/{id}/run")
    public R<EtlExecutionVO> runPipeline(@PathVariable Long id) {
        return R.ok(executionService.triggerExecution(id, "MANUAL"));
    }

    @PreAuthorize("hasPermission('data:read')")
    @PostMapping("/{id}/validate")
    public R<List<String>> validatePipeline(@PathVariable Long id) {
        return R.ok(pipelineService.validatePipeline(id));
    }

    @PreAuthorize("hasPermission('data:read')")
    @GetMapping("/{id}/executions")
    public R<PageResult<EtlExecutionVO>> getExecutions(@PathVariable Long id,
                                                        EtlExecutionQueryDTO query) {
        query.setPipelineId(id);
        var page = executionService.listExecutions(query);
        return R.ok(new PageResult<>(page.getContent(), page.getTotalElements()));
    }

    @PreAuthorize("hasPermission('data:update')")
    @PostMapping("/{id}/copy")
    public R<EtlPipelineVO> copyPipeline(@PathVariable Long id) {
        return R.ok(pipelineService.copyPipeline(id));
    }

    @PreAuthorize("hasPermission('data:update')")
    @PutMapping("/{id}/status")
    public R<EtlPipelineVO> updateStatus(@PathVariable Long id,
                                          @RequestBody java.util.Map<String, String> body) {
        return R.ok(pipelineService.updateStatus(id, body.get("status")));
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `cd maidc-parent && mvn compile -pl maidc-data -am -q`

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/controller/EtlPipelineController.java
git commit -m "feat(etl): add EtlPipelineController with CRUD, run, validate, copy"
```

---

### Task 3.2: EtlStepController

**Files:**
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/controller/EtlStepController.java`

- [ ] **Step 1: 创建 Controller**

```java
package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.dto.etl.EtlStepCreateDTO;
import com.maidc.data.dto.etl.EtlStepUpdateDTO;
import com.maidc.data.service.etl.EtlStepService;
import com.maidc.data.vo.EtlStepVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cdr/etl/pipelines/{pipelineId}/steps")
@RequiredArgsConstructor
public class EtlStepController {

    private final EtlStepService stepService;

    @PreAuthorize("hasPermission('data:read')")
    @GetMapping
    public R<List<EtlStepVO>> listSteps(@PathVariable Long pipelineId) {
        return R.ok(stepService.listSteps(pipelineId));
    }

    @PreAuthorize("hasPermission('data:create')")
    @PostMapping
    public R<EtlStepVO> createStep(@PathVariable Long pipelineId,
                                    @RequestBody @Valid EtlStepCreateDTO dto) {
        return R.ok(stepService.createStep(pipelineId, dto));
    }

    @PreAuthorize("hasPermission('data:update')")
    @PutMapping("/{stepId}")
    public R<EtlStepVO> updateStep(@PathVariable Long pipelineId,
                                    @PathVariable Long stepId,
                                    @RequestBody EtlStepUpdateDTO dto) {
        return R.ok(stepService.updateStep(stepId, dto));
    }

    @PreAuthorize("hasPermission('data:delete')")
    @DeleteMapping("/{stepId}")
    public R<Void> deleteStep(@PathVariable Long pipelineId, @PathVariable Long stepId) {
        stepService.deleteStep(stepId);
        return R.ok();
    }

    @PreAuthorize("hasPermission('data:update')")
    @PutMapping("/reorder")
    public R<Void> reorderSteps(@PathVariable Long pipelineId,
                                 @RequestBody Map<String, List<Long>> body) {
        stepService.reorderSteps(pipelineId, body.get("stepIds"));
        return R.ok();
    }

    @PreAuthorize("hasPermission('data:read')")
    @PostMapping("/{stepId}/preview")
    public R<List<Map<String, Object>>> previewData(@PathVariable Long pipelineId,
                                                      @PathVariable Long stepId) {
        return R.ok(stepService.previewData(stepId));
    }
}
```

- [ ] **Step 2: 编译 + 提交**

```bash
cd maidc-parent && mvn compile -pl maidc-data -am -q
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/controller/EtlStepController.java
git commit -m "feat(etl): add EtlStepController with CRUD, reorder, preview"
```

---

### Task 3.3: EtlFieldMappingController

**Files:**
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/controller/EtlFieldMappingController.java`

- [ ] **Step 1: 创建 Controller**

```java
package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.dto.etl.EtlFieldMappingDTO;
import com.maidc.data.service.etl.EtlFieldMappingService;
import com.maidc.data.vo.EtlFieldMappingVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cdr/etl/steps/{stepId}/field-mappings")
@RequiredArgsConstructor
public class EtlFieldMappingController {

    private final EtlFieldMappingService fieldMappingService;

    @PreAuthorize("hasPermission('data:read')")
    @GetMapping
    public R<List<EtlFieldMappingVO>> listMappings(@PathVariable Long stepId) {
        return R.ok(fieldMappingService.listMappings(stepId));
    }

    @PreAuthorize("hasPermission('data:update')")
    @PutMapping
    public R<List<EtlFieldMappingVO>> batchUpdateMappings(@PathVariable Long stepId,
                                                           @RequestBody @Valid List<EtlFieldMappingDTO> dtos) {
        return R.ok(fieldMappingService.batchUpdateMappings(stepId, dtos));
    }

    @PreAuthorize("hasPermission('data:update')")
    @PostMapping("/auto-map")
    public R<List<EtlFieldMappingVO>> autoMap(@PathVariable Long stepId) {
        return R.ok(fieldMappingService.autoMap(stepId));
    }
}
```

- [ ] **Step 2: 编译 + 提交**

```bash
cd maidc-parent && mvn compile -pl maidc-data -am -q
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/controller/EtlFieldMappingController.java
git commit -m "feat(etl): add EtlFieldMappingController with batch update and auto-map"
```

---

### Task 3.4: EtlExecutionController

**Files:**
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/controller/EtlExecutionController.java`

- [ ] **Step 1: 创建 Controller**

```java
package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.common.core.result.PageResult;
import com.maidc.data.dto.etl.EtlExecutionQueryDTO;
import com.maidc.data.service.etl.EtlExecutionService;
import com.maidc.data.vo.EtlExecutionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cdr/etl/executions")
@RequiredArgsConstructor
public class EtlExecutionController {

    private final EtlExecutionService executionService;

    @PreAuthorize("hasPermission('data:read')")
    @GetMapping
    public R<PageResult<EtlExecutionVO>> listExecutions(EtlExecutionQueryDTO query) {
        var page = executionService.listExecutions(query);
        return R.ok(new PageResult<>(page.getContent(), page.getTotalElements()));
    }

    @PreAuthorize("hasPermission('data:read')")
    @GetMapping("/{id}")
    public R<EtlExecutionVO> getExecution(@PathVariable Long id) {
        return R.ok(executionService.getExecution(id));
    }

    @PreAuthorize("hasPermission('data:read')")
    @GetMapping("/{id}/logs")
    public R<String> getLogs(@PathVariable Long id) {
        return R.ok(executionService.getExecutionLogs(id));
    }

    @PreAuthorize("hasPermission('data:update')")
    @PostMapping("/{id}/cancel")
    public R<Void> cancelExecution(@PathVariable Long id) {
        executionService.cancelExecution(id);
        return R.ok();
    }

    @PreAuthorize("hasPermission('data:update')")
    @PostMapping("/{id}/retry")
    public R<EtlExecutionVO> retryExecution(@PathVariable Long id) {
        return R.ok(executionService.retryExecution(id));
    }
}
```

- [ ] **Step 2: 编译 + 提交**

```bash
cd maidc-parent && mvn compile -pl maidc-data -am -q
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/controller/EtlExecutionController.java
git commit -m "feat(etl): add EtlExecutionController with list, detail, logs, cancel, retry"
```

---

### Task 3.5: EtlMetadataController

**Files:**
- Create: `maidc-parent/maidc-data/src/main/java/com/maidc/data/controller/EtlMetadataController.java`

- [ ] **Step 1: 创建 Controller**

```java
package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.data.service.etl.EtlMetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cdr/etl/metadata")
@RequiredArgsConstructor
public class EtlMetadataController {

    private final EtlMetadataService metadataService;

    @PreAuthorize("hasPermission('data:read')")
    @GetMapping("/schemas")
    public R<List<String>> listSchemas() {
        return R.ok(metadataService.listSchemas());
    }

    @PreAuthorize("hasPermission('data:read')")
    @GetMapping("/schemas/{schema}/tables")
    public R<List<Map<String, Object>>> listTables(@PathVariable String schema) {
        return R.ok(metadataService.listTables(schema));
    }

    @PreAuthorize("hasPermission('data:read')")
    @GetMapping("/tables/{schema}.{table}/columns")
    public R<List<Map<String, Object>>> listColumns(@PathVariable String schema,
                                                      @PathVariable String table) {
        return R.ok(metadataService.listColumns(schema, table));
    }
}
```

- [ ] **Step 2: 编译 + 提交**

```bash
cd maidc-parent && mvn compile -pl maidc-data -am -q
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/controller/EtlMetadataController.java
git commit -m "feat(etl): add EtlMetadataController for schema/table/column metadata"
```

---

### Task 3.6: 全量编译 + 启动验证

- [ ] **Step 1: 全模块编译**

Run: `cd maidc-parent && mvn compile -q`

Expected: BUILD SUCCESS

- [ ] **Step 2: 运行现有测试**

Run: `cd maidc-parent && mvn test -pl maidc-data -q`

Expected: 所有测试通过

- [ ] **Step 3: 启动服务验证 API 注册**

Run: `cd maidc-parent/maidc-data && mvn spring-boot:run -q`

检查日志中是否注册了所有 ETL 相关端点：
- `/api/v1/cdr/etl/pipelines/**`
- `/api/v1/cdr/etl/executions/**`
- `/api/v1/cdr/etl/metadata/**`

---

## Phase 3 完成标准

- [x] 5 个 Controller 全部编译通过
- [x] EtlPipelineController: 管道 CRUD + 执行 + 校验 + 复制 + 状态更新
- [x] EtlStepController: 步骤 CRUD + 排序 + 预览
- [x] EtlFieldMappingController: 字段映射列表 + 批量更新 + 自动映射
- [x] EtlExecutionController: 执行列表 + 详情 + 日志 + 取消 + 重试
- [x] EtlMetadataController: schema/table/column 元数据查询
- [x] 全模块编译 BUILD SUCCESS
