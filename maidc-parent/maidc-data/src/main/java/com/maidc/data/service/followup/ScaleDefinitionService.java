package com.maidc.data.service.followup;

import com.fasterxml.jackson.databind.JsonNode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.ScaleDefinitionEntity;
import com.maidc.data.repository.ScaleDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScaleDefinitionService {

    private final ScaleDefinitionRepository scaleRepository;
    private final ScaleEngine scaleEngine;

    /** 列表：每个 scaleCode 只取最新版本（含 DISABLED） */
    public List<ScaleDefinitionEntity> list(String keyword) {
        List<ScaleDefinitionEntity> all = keyword == null || keyword.isBlank()
                ? scaleRepository.findAll(Sort.by(Sort.Direction.DESC, "version"))
                : scaleRepository.search(keyword, PageRequest.of(0, 500, Sort.by(Sort.Direction.DESC, "version"))).getContent();
        return all.stream()
                .collect(Collectors.toMap(ScaleDefinitionEntity::getScaleCode, s -> s,
                        (a, b) -> a.getVersion() >= b.getVersion() ? a : b))
                .values().stream()
                .sorted((a, b) -> a.getScaleCode().compareTo(b.getScaleCode()))
                .collect(Collectors.toList());
    }

    public List<ScaleDefinitionEntity> versions(String scaleCode) {
        return scaleRepository.findByScaleCodeAndIsDeletedFalseOrderByVersionDesc(scaleCode);
    }

    public ScaleDefinitionEntity latestActive(String scaleCode) {
        return scaleRepository.findFirstByScaleCodeAndStatusAndIsDeletedFalseOrderByVersionDesc(scaleCode, "ACTIVE")
                .orElseThrow(() -> new BusinessException(404, "量表不存在或已停用: " + scaleCode));
    }

    @Transactional
    public ScaleDefinitionEntity create(Map<String, Object> req) {
        String code = str(req.get("scaleCode"));
        String name = str(req.get("name"));
        if (code.isBlank() || name.isBlank()) throw new BusinessException(400, "量表编码与名称必填");
        String definition = json(req.get("definition"));
        scaleEngine.parse(definition); // 结构校验

        Integer nextVersion = scaleRepository.findByScaleCodeAndIsDeletedFalseOrderByVersionDesc(code).stream()
                .findFirst().map(s -> s.getVersion() + 1).orElse(1);
        ScaleDefinitionEntity entity = new ScaleDefinitionEntity();
        entity.setScaleCode(code);
        entity.setName(name);
        entity.setVersion(nextVersion);
        entity.setStatus("ACTIVE");
        entity.setDefinition(definition);
        if (entity.getOrgId() == null) entity.setOrgId(0L);
        ScaleDefinitionEntity saved = scaleRepository.save(entity);
        log.info("量表发布: {} v{} 满分={}", code, nextVersion, maxScore(saved));
        return saved;
    }

    /** 发布新版本 = version+1 新行（UNIQUE(scale_code, version)），历史评估快照不受影响 */
    @Transactional
    public ScaleDefinitionEntity publishNewVersion(Map<String, Object> req) {
        return create(req);
    }

    /** 停用后不可被新方案引用，历史快照照常展示 */
    @Transactional
    public ScaleDefinitionEntity updateStatus(Long id, String status) {
        if (!"ACTIVE".equals(status) && !"DISABLED".equals(status)) throw new BusinessException(400, "非法状态: " + status);
        ScaleDefinitionEntity entity = scaleRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(404, "量表不存在: " + id));
        if ("DISABLED".equals(status)) {
            long refs = scaleRepository.countProtocolsReferencing(entity.getScaleCode());
            if (refs > 0) throw new BusinessException(400, "量表仍被 " + refs + " 个随访方案引用，不可停用");
        }
        entity.setStatus(status);
        return scaleRepository.save(entity);
    }

    private int maxScore(ScaleDefinitionEntity entity) {
        try {
            JsonNode def = scaleEngine.parse(entity.getDefinition());
            return def.path("maxScore").asInt(0);
        } catch (Exception e) { return 0; }
    }

    private String str(Object o) { return o == null ? "" : String.valueOf(o).trim(); }

    private String json(Object o) {
        if (o == null) throw new BusinessException(400, "definition 不能为空");
        try { return o instanceof String s ? s : new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(o); }
        catch (Exception e) { throw new BusinessException(400, "definition 非法 JSON"); }
    }
}
