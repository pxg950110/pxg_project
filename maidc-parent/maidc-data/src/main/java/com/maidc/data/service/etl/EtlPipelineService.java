package com.maidc.data.service.etl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.core.result.PageResult;
import com.maidc.data.dto.etl.EtlPipelineCreateDTO;
import com.maidc.data.dto.etl.EtlPipelineQueryDTO;
import com.maidc.data.entity.EtlPipelineEntity;
import com.maidc.data.entity.EtlStepEntity;
import com.maidc.data.mapper.DataMapper;
import com.maidc.data.repository.EtlPipelineRepository;
import com.maidc.data.repository.EtlStepRepository;
import com.maidc.data.vo.EtlPipelineDetailVO;
import com.maidc.data.vo.EtlPipelineVO;
import com.maidc.data.vo.EtlStepVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EtlPipelineService {

    private final EtlPipelineRepository pipelineRepository;
    private final EtlStepRepository stepRepository;
    private final DataMapper dataMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public EtlPipelineVO createPipeline(EtlPipelineCreateDTO dto, Long orgId) {
        EtlPipelineEntity entity = new EtlPipelineEntity();
        entity.setPipelineName(dto.getPipelineName());
        entity.setSourceId(dto.getSourceId());
        entity.setDescription(dto.getDescription());
        entity.setEngineType(dto.getEngineType() != null ? dto.getEngineType() : "EMBULK");
        entity.setSyncMode(dto.getSyncMode() != null ? dto.getSyncMode() : "MANUAL");
        entity.setCronExpression(dto.getCronExpression());
        entity.setStatus("DRAFT");
        entity.setOrgId(orgId != null ? orgId : 0L);

        entity = pipelineRepository.save(entity);
        log.info("ETL pipeline created: id={}, name={}", entity.getId(), entity.getPipelineName());
        return enrichPipelineVO(entity);
    }

    @Transactional
    public EtlPipelineVO updatePipeline(Long id, EtlPipelineCreateDTO dto) {
        EtlPipelineEntity entity = pipelineRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        entity.setPipelineName(dto.getPipelineName());
        entity.setSourceId(dto.getSourceId());
        entity.setDescription(dto.getDescription());
        if (dto.getEngineType() != null) {
            entity.setEngineType(dto.getEngineType());
        }
        if (dto.getSyncMode() != null) {
            entity.setSyncMode(dto.getSyncMode());
        }
        entity.setCronExpression(dto.getCronExpression());

        entity = pipelineRepository.save(entity);
        log.info("ETL pipeline updated: id={}", id);
        return enrichPipelineVO(entity);
    }

    public EtlPipelineDetailVO getPipelineDetail(Long id) {
        EtlPipelineEntity entity = pipelineRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        List<EtlStepEntity> steps = stepRepository.findByPipelineIdAndIsDeletedFalseOrderByStepOrder(id);
        List<EtlStepVO> stepVOs = steps.stream()
                .map(dataMapper::toEtlStepVO)
                .toList();

        return EtlPipelineDetailVO.builder()
                .id(entity.getId())
                .pipelineName(entity.getPipelineName())
                .sourceId(entity.getSourceId())
                .description(entity.getDescription())
                .engineType(entity.getEngineType())
                .status(entity.getStatus())
                .syncMode(entity.getSyncMode())
                .cronExpression(entity.getCronExpression())
                .lastRunTime(entity.getLastRunTime())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .steps(stepVOs)
                .build();
    }

    public PageResult<EtlPipelineVO> listPipelines(EtlPipelineQueryDTO query) {
        Specification<EtlPipelineEntity> spec = (root, q, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();

            if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("pipelineName")),
                        "%" + query.getKeyword().toLowerCase() + "%"));
            }
            if (query.getSourceId() != null) {
                predicates.add(cb.equal(root.get("sourceId"), query.getSourceId()));
            }
            if (query.getStatus() != null && !query.getStatus().isBlank()) {
                predicates.add(cb.equal(root.get("status"), query.getStatus()));
            }
            if (query.getEngineType() != null && !query.getEngineType().isBlank()) {
                predicates.add(cb.equal(root.get("engineType"), query.getEngineType()));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        PageRequest pageRequest = PageRequest.of(
                query.getPage() - 1, query.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<EtlPipelineEntity> page = pipelineRepository.findAll(spec, pageRequest);
        Page<EtlPipelineVO> voPage = page.map(this::enrichPipelineVO);
        return PageResult.of(voPage);
    }

    @Transactional
    public void deletePipeline(Long id) {
        EtlPipelineEntity entity = pipelineRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        pipelineRepository.delete(entity);
        log.info("ETL pipeline soft-deleted: id={}", id);
    }

    @Transactional
    public EtlPipelineVO updateStatus(Long id, String status) {
        EtlPipelineEntity entity = pipelineRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        entity.setStatus(status);
        entity = pipelineRepository.save(entity);
        log.info("ETL pipeline status updated: id={}, status={}", id, status);
        return enrichPipelineVO(entity);
    }

    @Transactional
    public EtlPipelineVO copyPipeline(Long id) {
        EtlPipelineEntity source = pipelineRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        EtlPipelineEntity copy = new EtlPipelineEntity();
        copy.setPipelineName(source.getPipelineName() + " (Copy)");
        copy.setSourceId(source.getSourceId());
        copy.setDescription(source.getDescription());
        copy.setEngineType(source.getEngineType());
        copy.setStatus("DRAFT");
        copy.setSyncMode(source.getSyncMode());
        copy.setCronExpression(source.getCronExpression());
        copy.setOrgId(source.getOrgId());

        copy = pipelineRepository.save(copy);
        log.info("ETL pipeline copied: fromId={}, toId={}", id, copy.getId());
        return enrichPipelineVO(copy);
    }

    public List<String> validatePipeline(Long id) {
        List<String> errors = new ArrayList<>();

        EtlPipelineEntity entity = pipelineRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        List<EtlStepEntity> steps = stepRepository.findByPipelineIdAndIsDeletedFalseOrderByStepOrder(id);
        if (steps.isEmpty()) {
            errors.add("Pipeline has no steps configured");
        }

        for (EtlStepEntity step : steps) {
            if (step.getSourceTable() == null || step.getSourceTable().isBlank()) {
                errors.add("Step '" + step.getStepName() + "' is missing sourceTable");
            }
            if (step.getTargetTable() == null || step.getTargetTable().isBlank()) {
                errors.add("Step '" + step.getStepName() + "' is missing targetTable");
            }
        }

        return errors;
    }

    private EtlPipelineVO enrichPipelineVO(EtlPipelineEntity entity) {
        EtlPipelineVO vo = dataMapper.toEtlPipelineVO(entity);
        long stepCount = stepRepository.countByPipelineIdAndIsDeletedFalse(entity.getId());
        vo.setStepCount((int) stepCount);
        return vo;
    }

    // ===== Visual Designer Graph API =====

    @Transactional(readOnly = true)
    public Map<String, Object> getPipelineGraph(Long pipelineId) {
        pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        List<EtlStepEntity> steps = stepRepository.findByPipelineIdAndIsDeletedFalseOrderByStepOrder(pipelineId);

        List<Map<String, Object>> nodes = steps.stream().map(step -> {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", "step_" + step.getId());
            node.put("type", "etlNode");

            Map<String, Object> position = new HashMap<>();
            position.put("x", 100);
            position.put("y", 100);
            try {
                JsonNode tc = step.getTransformConfig();
                if (tc != null && tc.has("position")) {
                    JsonNode pos = tc.get("position");
                    if (pos.has("x")) position.put("x", pos.get("x").asInt());
                    if (pos.has("y")) position.put("y", pos.get("y").asInt());
                }
            } catch (Exception ignored) {}
            node.put("position", position);

            String nodeType = extractNodeType(step);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("label", step.getStepName());
            data.put("nodeType", nodeType);
            data.put("category", deriveCategory(nodeType));
            data.put("config", buildNodeConfig(step, nodeType));
            data.put("status", "draft");
            node.put("data", data);

            return node;
        }).collect(Collectors.toList());

        List<Map<String, Object>> edges = getEdgesForPipeline(pipelineId);

        Map<String, Object> result = new HashMap<>();
        result.put("nodes", nodes);
        result.put("edges", edges);
        return result;
    }

    @Transactional
    public void savePipelineGraph(Long pipelineId, Map<String, Object> graphData, Long orgId) {
        pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) graphData.getOrDefault("nodes", List.of());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> edges = (List<Map<String, Object>>) graphData.getOrDefault("edges", List.of());

        // Upsert steps from nodes
        for (Map<String, Object> nodeData : nodes) {
            String nodeIdStr = (String) nodeData.get("id");
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) nodeData.get("data");
            @SuppressWarnings("unchecked")
            Map<String, Object> position = (Map<String, Object>) nodeData.get("position");

            String stepName = data != null ? (String) data.getOrDefault("label", "Unnamed") : "Unnamed";
            String nodeType = data != null ? (String) data.getOrDefault("nodeType", "TABLE_INPUT") : "TABLE_INPUT";
            @SuppressWarnings("unchecked")
            Map<String, Object> config = data != null ? (Map<String, Object>) data.get("config") : null;

            Long stepId = null;
            if (nodeIdStr != null && nodeIdStr.startsWith("step_")) {
                try { stepId = Long.valueOf(nodeIdStr.substring(5)); } catch (NumberFormatException ignored) {}
            }

            String transformConfigJson = buildTransformConfigJson(nodeType, position, config);

            if (stepId != null) {
                Optional<EtlStepEntity> existing = stepRepository.findById(stepId);
                if (existing.isPresent()) {
                    EtlStepEntity step = existing.get();
                    step.setStepName(stepName);
                    step.setTransformConfig(toJsonNode(transformConfigJson));
                    applyConfigToStep(step, config);
                    stepRepository.save(step);
                }
            } else {
                EtlStepEntity step = new EtlStepEntity();
                step.setPipelineId(pipelineId);
                step.setStepName(stepName);
                step.setStepOrder(0);
                step.setStepType("ONE_TO_ONE");
                step.setOnError("ABORT");
                step.setSyncMode("FULL");
                step.setSourceTable("");
                step.setTargetTable("");
                step.setTransformConfig(toJsonNode(transformConfigJson));
                step.setOrgId(orgId);
                step.setCreatedBy("system");
                step.setCreatedAt(java.time.LocalDateTime.now());
                step.setIsDeleted(false);
                applyConfigToStep(step, config);
                stepRepository.save(step);
            }
        }

        // Delete old edges and insert new ones
        jdbcTemplate.update("UPDATE cdr.r_etl_edge SET is_deleted = true WHERE pipeline_id = ? AND NOT is_deleted", pipelineId);
        for (Map<String, Object> edgeData : edges) {
            String sourceId = (String) edgeData.get("source");
            String targetId = (String) edgeData.get("target");
            if (sourceId == null || targetId == null) continue;

            Long sourceStepId = extractStepId(sourceId);
            Long targetStepId = extractStepId(targetId);
            if (sourceStepId == null || targetStepId == null) continue;

            String sourcePort = (String) edgeData.getOrDefault("sourceHandle", "out_1");
            String targetPort = (String) edgeData.getOrDefault("targetHandle", "in_1");

            String fieldMappingsJson = null;
            Object edgeDataObj = edgeData.get("data");
            if (edgeDataObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> d = (Map<String, Object>) edgeDataObj;
                Object fm = d.get("fieldMappings");
                if (fm != null) fieldMappingsJson = toJsonStr(fm);
            }

            jdbcTemplate.update(
                    "INSERT INTO cdr.r_etl_edge (pipeline_id, source_step_id, source_port, target_step_id, target_port, field_mappings, sort_order, created_by, created_at, is_deleted, org_id) VALUES (?, ?, ?, ?, ?, ?::jsonb, 0, 'system', NOW(), false, ?)",
                    pipelineId, sourceStepId, sourcePort, targetStepId, targetPort, fieldMappingsJson, orgId
            );
        }

        log.info("ETL pipeline graph saved: pipelineId={}, nodes={}, edges={}", pipelineId, nodes.size(), edges.size());
    }

    public String previewEmbulkYaml(Long pipelineId) {
        Map<String, Object> graph = getPipelineGraph(pipelineId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) graph.get("nodes");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> edges = (List<Map<String, Object>>) graph.get("edges");

        if (nodes == null || nodes.isEmpty()) return "# No steps configured\n";

        Set<String> nodesWithIncoming = new HashSet<>();
        for (Map<String, Object> edge : edges) {
            String target = (String) edge.get("target");
            if (target != null) nodesWithIncoming.add(target);
        }

        StringBuilder yaml = new StringBuilder();
        for (Map<String, Object> node : nodes) {
            if (!nodesWithIncoming.contains(node.get("id"))) {
                generateChainYaml(node, nodes, edges, yaml);
            }
        }
        return yaml.toString();
    }

    private void generateChainYaml(Map<String, Object> node, List<Map<String, Object>> allNodes,
                                    List<Map<String, Object>> edges, StringBuilder yaml) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) node.get("data");
        if (data == null) return;

        String nodeType = (String) data.getOrDefault("nodeType", "TABLE_INPUT");
        @SuppressWarnings("unchecked")
        Map<String, Object> config = (Map<String, Object>) data.get("config");
        if (config == null) config = Map.of();

        switch (nodeType) {
            case "TABLE_INPUT" -> {
                yaml.append("in:\n  type: postgresql\n");
                yaml.append("  host: ${env:DB_HOST:localhost}\n  user: ${env:DB_USER:maidc}\n");
                yaml.append("  password: ${env:DB_PASS:maidc123}\n  database: maidc\n");
                yaml.append("  schema: ").append(config.getOrDefault("schema", "public")).append("\n");
                yaml.append("  table: ").append(config.getOrDefault("table", "")).append("\n");
                if (config.get("where") != null && !config.get("where").toString().isEmpty())
                    yaml.append("  where: ").append(config.get("where")).append("\n");
                yaml.append("\n");
            }
            case "CSV_INPUT" -> {
                yaml.append("in:\n  type: file\n");
                yaml.append("  path_prefix: ").append(config.getOrDefault("filePath", "")).append("\n");
                yaml.append("  parser:\n    type: csv\n");
                yaml.append("    delimiter: '").append(config.getOrDefault("delimiter", ",")).append("'\n\n");
            }
            case "FILTER" -> {
                yaml.append("filters:\n  - type: row\n");
                yaml.append("    where: ").append(config.getOrDefault("condition", "1=1")).append("\n\n");
            }
            case "TABLE_OUTPUT" -> {
                yaml.append("out:\n  type: postgresql\n");
                yaml.append("  host: ${env:DB_HOST:localhost}\n  user: ${env:DB_USER:maidc}\n");
                yaml.append("  password: ${env:DB_PASS:maidc123}\n  database: maidc\n");
                yaml.append("  schema: ").append(config.getOrDefault("schema", "public")).append("\n");
                yaml.append("  table: ").append(config.getOrDefault("table", "")).append("\n");
                String mode = (String) config.getOrDefault("writeMode", "insert");
                yaml.append("  mode: ").append(switch (mode) {
                    case "truncate" -> "truncate_insert";
                    case "upsert" -> "merge";
                    default -> "insert";
                }).append("\n\n");
            }
            case "CSV_OUTPUT" -> {
                yaml.append("out:\n  type: file\n");
                yaml.append("  path_prefix: ").append(config.getOrDefault("filePath", "")).append("\n");
                yaml.append("  formatter:\n    type: csv\n\n");
            }
            default -> yaml.append("# ").append(nodeType).append(" - requires SQL preprocessing\n\n");
        }

        String nodeId = (String) node.get("id");
        for (Map<String, Object> edge : edges) {
            if (nodeId != null && nodeId.equals(edge.get("source"))) {
                String targetId = (String) edge.get("target");
                allNodes.stream()
                        .filter(n -> targetId != null && targetId.equals(n.get("id")))
                        .findFirst()
                        .ifPresent(child -> generateChainYaml(child, allNodes, edges, yaml));
            }
        }
    }

    private String extractNodeType(EtlStepEntity step) {
        try {
            JsonNode tc = step.getTransformConfig();
            if (tc != null && tc.has("nodeType")) return tc.get("nodeType").asText();
        } catch (Exception ignored) {}
        if (step.getTargetTable() != null && !step.getTargetTable().isEmpty()) return "TABLE_OUTPUT";
        return "TABLE_INPUT";
    }

    private String deriveCategory(String nodeType) {
        if (nodeType == null) return "INPUT";
        if (nodeType.endsWith("_INPUT")) return "INPUT";
        if (nodeType.endsWith("_OUTPUT")) return "OUTPUT";
        if ("FILTER".equals(nodeType) || "JOIN".equals(nodeType) || "AGGREGATE".equals(nodeType)) return "PROCESSOR";
        return "TRANSFORM";
    }

    private Map<String, Object> buildNodeConfig(EtlStepEntity step, String nodeType) {
        Map<String, Object> config = new HashMap<>();
        if ("TABLE_INPUT".equals(nodeType) || "TABLE_OUTPUT".equals(nodeType)) {
            if (step.getSourceSchema() != null) config.put("schema", step.getSourceSchema());
            if (step.getSourceTable() != null) config.put("table", step.getSourceTable());
            if (step.getFilterCondition() != null) config.put("where", step.getFilterCondition());
            config.put("writeMode", "insert");
        }
        try {
            JsonNode tc = step.getTransformConfig();
            if (tc != null && tc.has("config")) {
                JsonNode cfgNode = tc.get("config");
                cfgNode.fields().forEachRemaining(e -> config.put(e.getKey(), convertJsonValue(e.getValue())));
            }
        } catch (Exception ignored) {}
        return config;
    }

    private Object convertJsonValue(JsonNode node) {
        if (node.isTextual()) return node.asText();
        if (node.isInt()) return node.asInt();
        if (node.isLong()) return node.asLong();
        if (node.isBoolean()) return node.asBoolean();
        if (node.isDouble()) return node.asDouble();
        return node.toString();
    }

    private List<Map<String, Object>> getEdgesForPipeline(Long pipelineId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, source_step_id, source_port, target_step_id, target_port, field_mappings " +
                        "FROM cdr.r_etl_edge WHERE pipeline_id = ? AND NOT is_deleted ORDER BY sort_order", pipelineId);

        List<Map<String, Object>> edges = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> edge = new LinkedHashMap<>();
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
            edges.add(edge);
        }
        return edges;
    }

    private String buildTransformConfigJson(String nodeType, Map<String, Object> position, Map<String, Object> config) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("nodeType", nodeType);
            ObjectNode posNode = root.putObject("position");
            if (position != null) {
                posNode.put("x", ((Number) position.getOrDefault("x", 100)).intValue());
                posNode.put("y", ((Number) position.getOrDefault("y", 100)).intValue());
            } else {
                posNode.put("x", 100).put("y", 100);
            }
            root.put("config", objectMapper.valueToTree(config != null ? config : Map.of()));
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            return "{\"nodeType\":\"" + nodeType + "\"}";
        }
    }

    private void applyConfigToStep(EtlStepEntity step, Map<String, Object> config) {
        if (config == null) return;
        if (config.get("schema") != null) {
            step.setSourceSchema((String) config.get("schema"));
            step.setTargetSchema((String) config.get("schema"));
        }
        if (config.get("table") != null) {
            String nodeType = extractNodeType(step);
            if ("TABLE_OUTPUT".equals(nodeType)) {
                step.setTargetTable((String) config.get("table"));
            } else {
                step.setSourceTable((String) config.get("table"));
                step.setTargetTable((String) config.get("table"));
            }
        }
        if (config.get("where") != null) step.setFilterCondition((String) config.get("where"));
    }

    private Long extractStepId(String nodeId) {
        if (nodeId != null && nodeId.startsWith("step_")) {
            try { return Long.valueOf(nodeId.substring(5)); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private JsonNode toJsonNode(String json) {
        try { return objectMapper.readTree(json); } catch (Exception e) { return objectMapper.createObjectNode(); }
    }

    private String toJsonStr(Object obj) {
        try { return objectMapper.writeValueAsString(obj); } catch (Exception e) { return "{}"; }
    }
}
