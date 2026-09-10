package com.maidc.data.service.followup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.FollowupProtocolEntity;
import com.maidc.data.repository.FollowupProtocolRepository;
import com.maidc.data.repository.ScaleDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowupProtocolService {

    private final FollowupProtocolRepository protocolRepository;
    private final ScaleDefinitionRepository scaleRepository;
    private final ObjectMapper objectMapper;

    public List<FollowupProtocolEntity> list(Long cohortId) {
        return protocolRepository.findByCohortIdAndIsDeletedFalseOrderByVersionDesc(cohortId);
    }

    public FollowupProtocolEntity latestPublished(Long cohortId) {
        return protocolRepository.findFirstByCohortIdAndStatusAndIsDeletedFalseOrderByVersionDesc(cohortId, "PUBLISHED")
                .orElseThrow(() -> new BusinessException(404, "该专病暂无已发布随访方案"));
    }

    /** 创建草稿（或直接发布） */
    @Transactional
    public FollowupProtocolEntity create(Long cohortId, Map<String, Object> req, boolean publish) {
        String name = req.get("name") == null ? "" : String.valueOf(req.get("name")).trim();
        if (name.isBlank()) throw new BusinessException(400, "方案名称必填");
        String stages = toJson(req.get("stages"));
        validateStages(stages);

        Integer nextVersion = protocolRepository.findByCohortIdAndIsDeletedFalseOrderByVersionDesc(cohortId).stream()
                .findFirst().map(p -> p.getVersion() + 1).orElse(1);

        FollowupProtocolEntity entity = new FollowupProtocolEntity();
        entity.setCohortId(cohortId);
        entity.setName(name);
        entity.setVersion(nextVersion);
        entity.setStatus(publish ? "PUBLISHED" : "DRAFT");
        entity.setStages(stages);
        if (entity.getOrgId() == null) entity.setOrgId(0L);
        FollowupProtocolEntity saved = protocolRepository.save(entity);
        if (publish) archiveOldVersions(cohortId, saved.getId());
        log.info("随访方案保存: cohort={} v{} status={}", cohortId, nextVersion, saved.getStatus());
        return saved;
    }

    /** 修改已发布方案 = 发布新版本；旧版本 ARCHIVED */
    @Transactional
    public FollowupProtocolEntity publishNewVersion(Long cohortId, Map<String, Object> req) {
        return create(cohortId, req, true);
    }

    private void archiveOldVersions(Long cohortId, Long keepId) {
        protocolRepository.findByCohortIdAndIsDeletedFalseOrderByVersionDesc(cohortId).stream()
                .filter(p -> !p.getId().equals(keepId) && "PUBLISHED".equals(p.getStatus()))
                .forEach(p -> p.setStatus("ARCHIVED"));
    }

    /** 阶段结构校验：stageCode/offsetDays/requiredScales 必填；量表必须存在且 ACTIVE */
    private void validateStages(String stagesJson) {
        JsonNode stages;
        try {
            stages = objectMapper.readTree(stagesJson);
        } catch (Exception e) {
            throw new BusinessException(400, "stages 非法 JSON");
        }
        if (!stages.isArray() || stages.isEmpty()) throw new BusinessException(400, "stages 不能为空");
        for (JsonNode stage : stages) {
            if (stage.path("stageCode").asText("").isBlank()) throw new BusinessException(400, "阶段 stageCode 必填");
            if (!stage.has("offsetDays")) throw new BusinessException(400, "阶段 offsetDays 必填");
            for (JsonNode code : stage.path("requiredScales")) {
                String sc = code.asText();
                if (scaleRepository.findFirstByScaleCodeAndStatusAndIsDeletedFalseOrderByVersionDesc(sc, "ACTIVE").isEmpty()) {
                    throw new BusinessException(400, "方案引用了不存在或已停用的量表: " + sc);
                }
            }
        }
    }

    private String toJson(Object o) {
        if (o == null) throw new BusinessException(400, "stages 不能为空");
        try { return o instanceof String s ? s : objectMapper.writeValueAsString(o); }
        catch (Exception e) { throw new BusinessException(400, "stages 序列化失败"); }
    }
}
