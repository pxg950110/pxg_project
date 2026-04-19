package com.maidc.data.service.etl;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.dto.etl.EtlFieldMappingDTO;
import com.maidc.data.entity.EtlFieldMappingEntity;
import com.maidc.data.entity.EtlStepEntity;
import com.maidc.data.mapper.DataMapper;
import com.maidc.data.repository.EtlFieldMappingRepository;
import com.maidc.data.repository.EtlStepRepository;
import com.maidc.data.vo.EtlFieldMappingVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EtlFieldMappingService {

    private final EtlFieldMappingRepository fieldMappingRepository;
    private final EtlStepRepository stepRepository;
    private final DataMapper dataMapper;
    private final JdbcTemplate jdbcTemplate;

    public List<EtlFieldMappingVO> listMappings(Long stepId) {
        List<EtlFieldMappingEntity> mappings =
                fieldMappingRepository.findByStepIdAndIsDeletedFalseOrderBySortOrder(stepId);
        return mappings.stream().map(dataMapper::toEtlFieldMappingVO).toList();
    }

    @Transactional
    public List<EtlFieldMappingVO> batchUpdateMappings(Long stepId, List<EtlFieldMappingDTO> dtos) {
        EtlStepEntity step = stepRepository.findById(stepId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        fieldMappingRepository.deleteByStepId(stepId);

        List<EtlFieldMappingEntity> entities = new ArrayList<>();
        for (int i = 0; i < dtos.size(); i++) {
            EtlFieldMappingDTO dto = dtos.get(i);
            EtlFieldMappingEntity entity = new EtlFieldMappingEntity();
            entity.setOrgId(step.getOrgId());
            entity.setStepId(stepId);
            entity.setSourceColumn(dto.getSourceColumn());
            entity.setSourceTableAlias(dto.getSourceTableAlias());
            entity.setTargetColumn(dto.getTargetColumn());
            entity.setTransformType(dto.getTransformType() != null ? dto.getTransformType() : "DIRECT");
            entity.setTransformExpr(dto.getTransformExpr());
            entity.setDefaultValue(dto.getDefaultValue());
            entity.setIsRequired(dto.getIsRequired() != null ? dto.getIsRequired() : false);
            entity.setSortOrder(i + 1);
            entities.add(entity);
        }

        entities = fieldMappingRepository.saveAll(entities);
        log.info("Batch updated field mappings: stepId={}, count={}", stepId, entities.size());
        return entities.stream().map(dataMapper::toEtlFieldMappingVO).toList();
    }

    @Transactional
    public List<EtlFieldMappingVO> autoMap(Long stepId) {
        EtlStepEntity step = stepRepository.findById(stepId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        List<Map<String, Object>> sourceColumns = getColumns(step.getSourceSchema(), step.getSourceTable());
        List<Map<String, Object>> targetColumns = getColumns(step.getTargetSchema(), step.getTargetTable());

        List<EtlFieldMappingDTO> mappings = new ArrayList<>();
        Set<String> matchedSource = new HashSet<>();
        Set<String> matchedTarget = new HashSet<>();

        // Phase 1: Exact match (case-insensitive)
        for (Map<String, Object> src : sourceColumns) {
            String srcName = (String) src.get("column_name");
            for (Map<String, Object> tgt : targetColumns) {
                String tgtName = (String) tgt.get("column_name");
                if (!matchedTarget.contains(tgtName) && srcName.equalsIgnoreCase(tgtName)) {
                    mappings.add(buildMappingDTO(srcName, tgtName));
                    matchedSource.add(srcName);
                    matchedTarget.add(tgtName);
                    break;
                }
            }
        }

        // Phase 2: Fuzzy match (edit distance <= 2)
        for (Map<String, Object> src : sourceColumns) {
            String srcName = (String) src.get("column_name");
            if (matchedSource.contains(srcName)) continue;
            String bestMatch = null;
            int bestDist = Integer.MAX_VALUE;
            for (Map<String, Object> tgt : targetColumns) {
                String tgtName = (String) tgt.get("column_name");
                if (matchedTarget.contains(tgtName)) continue;
                int dist = editDistance(srcName.toLowerCase(), tgtName.toLowerCase());
                if (dist <= 2 && dist < bestDist) {
                    bestDist = dist;
                    bestMatch = tgtName;
                }
            }
            if (bestMatch != null) {
                mappings.add(buildMappingDTO(srcName, bestMatch));
                matchedSource.add(srcName);
                matchedTarget.add(bestMatch);
            }
        }

        // Phase 3: Type match (same data type, not yet matched)
        for (Map<String, Object> src : sourceColumns) {
            String srcName = (String) src.get("column_name");
            if (matchedSource.contains(srcName)) continue;
            String srcType = (String) src.get("data_type");
            for (Map<String, Object> tgt : targetColumns) {
                String tgtName = (String) tgt.get("column_name");
                if (matchedTarget.contains(tgtName)) continue;
                String tgtType = (String) tgt.get("data_type");
                if (srcType != null && srcType.equalsIgnoreCase(tgtType)) {
                    mappings.add(buildMappingDTO(srcName, tgtName));
                    matchedSource.add(srcName);
                    matchedTarget.add(tgtName);
                    break;
                }
            }
        }

        log.info("Auto-mapped fields for step {}: {} mappings (exact+fuzzy+type)", stepId, mappings.size());
        return batchUpdateMappings(stepId, mappings);
    }

    private List<Map<String, Object>> getColumns(String schema, String table) {
        return jdbcTemplate.queryForList(
                "SELECT column_name, data_type FROM information_schema.columns " +
                        "WHERE table_schema = ? AND table_name = ? ORDER BY ordinal_position",
                schema, table);
    }

    private EtlFieldMappingDTO buildMappingDTO(String sourceColumn, String targetColumn) {
        return EtlFieldMappingDTO.builder()
                .sourceColumn(sourceColumn)
                .targetColumn(targetColumn)
                .transformType("DIRECT")
                .isRequired(false)
                .build();
    }

    private int editDistance(String a, String b) {
        int m = a.length(), n = b.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                dp[i][j] = a.charAt(i - 1) == b.charAt(j - 1)
                        ? dp[i - 1][j - 1]
                        : 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
            }
        }
        return dp[m][n];
    }
}
