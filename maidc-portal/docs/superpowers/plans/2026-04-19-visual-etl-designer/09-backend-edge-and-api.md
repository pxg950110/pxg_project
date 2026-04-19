# Task 09: Backend - r_etl_edge DDL + Graph API

**Files:**
- Create: `docker/init-db/12-etl-edge.sql`
- Modify: `maidc-parent/maidc-data/src/main/java/com/maidc/data/controller/EtlPipelineController.java`

- [ ] **Step 1: Create DDL for r_etl_edge table**

Create `docker/init-db/12-etl-edge.sql`:

```sql
-- ETL Edge table for visual designer graph connections
CREATE TABLE IF NOT EXISTS cdr.r_etl_edge (
    id              BIGSERIAL PRIMARY KEY,
    pipeline_id     BIGINT NOT NULL,
    source_step_id  BIGINT NOT NULL,
    source_port     VARCHAR(32) NOT NULL DEFAULT 'out_1',
    target_step_id  BIGINT NOT NULL,
    target_port     VARCHAR(32) NOT NULL DEFAULT 'in_1',
    field_mappings  JSONB,
    sort_order      INT NOT NULL DEFAULT 0,
    created_by      VARCHAR(64) NOT NULL DEFAULT 'system',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    org_id          BIGINT NOT NULL
);

CREATE INDEX idx_etl_edge_pipeline ON cdr.r_etl_edge(pipeline_id) WHERE NOT is_deleted;
CREATE INDEX idx_etl_edge_source ON cdr.r_etl_edge(source_step_id) WHERE NOT is_deleted;
CREATE INDEX idx_etl_edge_target ON cdr.r_etl_edge(target_step_id) WHERE NOT is_deleted;

-- Add edge_id to field mapping table
ALTER TABLE cdr.r_etl_field_mapping ADD COLUMN IF NOT EXISTS edge_id BIGINT;
```

- [ ] **Step 2: Run DDL in Docker**

```bash
docker exec -i maidc-postgres psql -U maidc -d maidc < docker/init-db/12-etl-edge.sql
```

Expected: `CREATE TABLE`, `CREATE INDEX`, `ALTER TABLE` success messages

- [ ] **Step 3: Add graph API endpoints to controller**

Add these endpoints to `EtlPipelineController.java`:

```java
// ===== Graph API =====

@GetMapping("/{id}/graph")
public R<Map<String, Object>> getPipelineGraph(@PathVariable Long id) {
    return R.ok(pipelineService.getPipelineGraph(id));
}

@PutMapping("/{id}/graph")
public R<Void> savePipelineGraph(
        @PathVariable Long id,
        @RequestBody Map<String, Object> graphData,
        HttpServletRequest request) {
    String orgIdHeader = request.getHeader("X-Org-Id");
    Long orgId = orgIdHeader != null ? Long.valueOf(orgIdHeader) : 0L;
    pipelineService.savePipelineGraph(id, graphData, orgId);
    return R.ok(null);
}

@GetMapping("/{id}/preview")
public R<String> previewEmbulkYaml(@PathVariable Long id) {
    return R.ok(pipelineService.previewEmbulkYaml(id));
}
```

- [ ] **Step 4: Add service methods**

In `EtlPipelineService.java`, add:

```java
@Transactional(readOnly = true)
public Map<String, Object> getPipelineGraph(Long pipelineId) {
    List<EtlStep> steps = stepRepository.findByPipelineIdAndIsDeletedFalse(pipelineId);

    List<Map<String, Object>> nodes = steps.stream().map(step -> {
        Map<String, Object> node = new HashMap<>();
        node.put("id", "step_" + step.getId());
        node.put("type", "etlNode");

        Map<String, Object> position = new HashMap<>();
        try {
            String tc = step.getTransformConfig();
            if (tc != null && !tc.isEmpty()) {
                Map<String, Object> config = objectMapper.readValue(tc, Map.class);
                Map<String, Object> pos = (Map<String, Object>) config.get("position");
                if (pos != null) {
                    position = pos;
                }
            }
        } catch (Exception ignored) {}
        node.put("position", position);

        Map<String, Object> data = new HashMap<>();
        data.put("label", step.getStepName());
        data.put("nodeType", extractNodeType(step));
        data.put("category", deriveCategory(extractNodeType(step)));
        data.put("config", buildNodeConfig(step));
        data.put("status", step.getStatus() != null ? step.getStatus().toLowerCase() : "draft");
        node.put("data", data);

        return node;
    }).collect(Collectors.toList());

    // Get edges from r_etl_edge table using native query
    List<Map<String, Object>> edges = getEdgesForPipeline(pipelineId);

    Map<String, Object> result = new HashMap<>();
    result.put("nodes", nodes);
    result.put("edges", edges);
    return result;
}

@Transactional
public void savePipelineGraph(Long pipelineId, Map<String, Object> graphData, Long orgId) {
    List<Map<String, Object>> nodes = (List<Map<String, Object>>) graphData.get("nodes");
    List<Map<String, Object>> edges = (List<Map<String, Object>>) graphData.get("edges");

    // Upsert steps from nodes
    for (Map<String, Object> nodeData : nodes) {
        String nodeIdStr = (String) nodeData.get("id");
        Map<String, Object> data = (Map<String, Object>) nodeData.get("data");
        Map<String, Object> position = (Map<String, Object>) nodeData.get("position");

        String stepName = (String) data.get("label");
        String nodeType = (String) data.get("nodeType");
        Map<String, Object> config = (Map<String, Object>) data.get("config");

        Long stepId = nodeIdStr.startsWith("step_")
            ? Long.valueOf(nodeIdStr.substring(5))
            : null;

        if (stepId != null) {
            Optional<EtlStep> existing = stepRepository.findById(stepId);
            if (existing.isPresent()) {
                EtlStep step = existing.get();
                step.setStepName(stepName);
                step.setTransformConfig(buildTransformConfig(nodeType, position, config));
                if (config != null) {
                    step.setSourceSchema((String) config.get("schema"));
                    step.setSourceTable((String) config.get("table"));
                    step.setTargetSchema((String) config.get("schema"));
                    step.setTargetTable((String) config.get("table"));
                }
                stepRepository.save(step);
            }
        } else {
            EtlStep step = new EtlStep();
            step.setPipelineId(pipelineId);
            step.setStepName(stepName);
            step.setStepType("ONE_TO_ONE");
            step.setOnError("ABORT");
            step.setTransformConfig(buildTransformConfig(nodeType, position, config));
            step.setOrgId(orgId);
            step.setCreatedBy("system");
            step.setCreatedAt(java.time.LocalDateTime.now());
            step.setIsDeleted(false);
            if (config != null) {
                step.setSourceSchema((String) config.get("schema"));
                step.setSourceTable((String) config.get("table"));
            }
            stepRepository.save(step);
        }
    }

    // Delete old edges and insert new ones
    jdbcTemplate.update("DELETE FROM cdr.r_etl_edge WHERE pipeline_id = ? AND NOT is_deleted", pipelineId);
    for (Map<String, Object> edgeData : edges) {
        String sourceId = (String) edgeData.get("source");
        String targetId = (String) edgeData.get("target");
        Long sourceStepId = Long.valueOf(sourceId.replace("step_", ""));
        Long targetStepId = Long.valueOf(targetId.replace("step_", ""));

        String fieldMappingsJson = edgeData.get("data") != null
            ? toJson(((Map<String, Object>) edgeData.get("data")).get("fieldMappings"))
            : null;

        jdbcTemplate.update(
            "INSERT INTO cdr.r_etl_edge (pipeline_id, source_step_id, source_port, target_step_id, target_port, field_mappings, sort_order, created_by, created_at, is_deleted, org_id) VALUES (?, ?, ?, ?, ?, ?::jsonb, 0, 'system', NOW(), false, ?)",
            pipelineId, sourceStepId,
            edgeData.getOrDefault("sourceHandle", "out_1"),
            targetStepId,
            edgeData.getOrDefault("targetHandle", "in_1"),
            fieldMappingsJson, orgId
        );
    }
}

private String extractNodeType(EtlStep step) {
    try {
        String tc = step.getTransformConfig();
        if (tc != null && !tc.isEmpty()) {
            Map<String, Object> config = objectMapper.readValue(tc, Map.class);
            return (String) config.getOrDefault("nodeType", "TABLE_INPUT");
        }
    } catch (Exception ignored) {}
    // Infer from step fields
    if (step.getTargetTable() != null && !step.getTargetTable().isEmpty()) return "TABLE_OUTPUT";
    return "TABLE_INPUT";
}

private String deriveCategory(String nodeType) {
    if (nodeType.startsWith("TABLE_") || nodeType.startsWith("CSV_")) {
        if (nodeType.endsWith("_INPUT")) return "INPUT";
        return "OUTPUT";
    }
    if (nodeType.equals("FILTER") || nodeType.equals("JOIN") || nodeType.equals("AGGREGATE")) return "PROCESSOR";
    return "TRANSFORM";
}

private Map<String, Object> buildNodeConfig(EtlStep step) {
    Map<String, Object> config = new HashMap<>();
    if (step.getSourceSchema() != null) config.put("schema", step.getSourceSchema());
    if (step.getSourceTable() != null) config.put("table", step.getSourceTable());
    if (step.getFilterCondition() != null) config.put("where", step.getFilterCondition());
    config.put("writeMode", "insert");
    return config;
}

private String buildTransformConfig(String nodeType, Map<String, Object> position, Map<String, Object> config) {
    Map<String, Object> tc = new HashMap<>();
    tc.put("nodeType", nodeType);
    tc.put("position", position != null ? position : Map.of("x", 100, "y", 100));
    tc.put("config", config != null ? config : Map.of());
    return toJson(tc);
}

private List<Map<String, Object>> getEdgesForPipeline(Long pipelineId) {
    return jdbcTemplate.queryForList(
        "SELECT id, source_step_id, source_port, target_step_id, target_port, field_mappings FROM cdr.r_etl_edge WHERE pipeline_id = ? AND NOT is_deleted ORDER BY sort_order",
        pipelineId
    ).stream().map(row -> {
        Map<String, Object> edge = new HashMap<>();
        edge.put("id", "edge_" + row.get("id"));
        edge.put("source", "step_" + row.get("source_step_id"));
        edge.put("target", "step_" + row.get("target_step_id"));
        edge.put("sourceHandle", row.getOrDefault("source_port", "out_1"));
        edge.put("targetHandle", row.getOrDefault("target_port", "in_1"));
        if (row.get("field_mappings") != null) {
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("fieldMappings", objectMapper.readValue(row.get("field_mappings").toString(), List.class));
                edge.put("data", data);
            } catch (Exception ignored) {}
        }
        return edge;
    }).collect(Collectors.toList());
}

public String previewEmbulkYaml(Long pipelineId) {
    Map<String, Object> graph = getPipelineGraph(pipelineId);
    // Delegate to EtlConfigGenerator
    return etlConfigGenerator.generateFromGraph(graph);
}

private String toJson(Object obj) {
    try { return objectMapper.writeValueAsString(obj); }
    catch (Exception e) { return "{}"; }
}
```

- [ ] **Step 5: Add required imports**

Ensure the controller and service have these imports:
- `import javax.servlet.http.HttpServletRequest;` (or `jakarta.servlet...` for Spring Boot 3)
- `import java.util.HashMap;`
- `import java.util.Map;`
- `import com.fasterxml.jackson.databind.ObjectMapper;`

- [ ] **Step 6: Verify backend compiles**

```bash
cd E:/pxg_project/maidc-portal/../maidc-parent
mvn compile -pl maidc-data -q
```

Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add docker/init-db/12-etl-edge.sql
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/controller/EtlPipelineController.java
git add maidc-parent/maidc-data/src/main/java/com/maidc/data/service/etl/EtlPipelineService.java
git commit -m "feat(etl): add r_etl_edge DDL + graph API endpoints for visual designer"
```
