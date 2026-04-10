package com.maidc.model.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.core.result.PageResult;
import com.maidc.model.dto.ModelCreateDTO;
import com.maidc.model.dto.ModelUpdateDTO;
import com.maidc.model.entity.ModelEntity;
import com.maidc.model.mapper.ModelMapper;
import com.maidc.model.repository.ModelRepository;
import com.maidc.model.repository.ModelSpecification;
import com.maidc.model.repository.VersionRepository;
import com.maidc.model.vo.ModelDetailVO;
import com.maidc.model.vo.ModelVO;
import com.maidc.model.vo.VersionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelService {

    private final ModelRepository modelRepository;
    private final VersionRepository versionRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public ModelVO createModel(ModelCreateDTO dto) {
        ModelEntity entity = new ModelEntity();
        entity.setModelCode(dto.getModelCode());
        entity.setModelName(dto.getModelName());
        entity.setDescription(dto.getDescription());
        entity.setModelType(dto.getModelType());
        entity.setTaskType(dto.getTaskType());
        entity.setFramework(dto.getFramework());
        entity.setInputSchema(dto.getInputSchema());
        entity.setOutputSchema(dto.getOutputSchema());
        entity.setTags(dto.getTags());
        entity.setLicense(dto.getLicense());
        entity.setProjectId(dto.getProjectId());
        entity.setOrgId(dto.getOrgId() != null ? dto.getOrgId() : 0L);
        entity.setOwnerId(0L);
        entity.setStatus("DRAFT");

        entity = modelRepository.save(entity);
        log.info("模型注册成功: code={}", entity.getModelCode());
        return modelMapper.toModelVO(entity);
    }

    public PageResult<ModelVO> listModels(int page, int pageSize, Long orgId,
                                            String keyword, String modelType, String status) {
        Page<ModelEntity> result = modelRepository.findAll(
                ModelSpecification.buildSearchSpec(orgId, keyword, modelType, status),
                PageRequest.of(page - 1, pageSize));

        Page<ModelVO> voPage = result.map(entity -> {
            ModelVO vo = modelMapper.toModelVO(entity);
            versionRepository.findFirstByModelIdAndIsDeletedFalseOrderByCreatedAtDesc(entity.getId())
                    .ifPresent(v -> vo.setLatestVersion(v.getVersionNo()));
            return vo;
        });

        return PageResult.of(voPage);
    }

    public ModelDetailVO getModelDetail(Long id) {
        ModelEntity entity = modelRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_NOT_FOUND));

        ModelDetailVO detail = modelMapper.toModelDetailVO(entity);
        detail.setVersionCount((int) versionRepository.countByModelIdAndIsDeletedFalse(id));

        versionRepository.findFirstByModelIdAndIsDeletedFalseOrderByCreatedAtDesc(id)
                .ifPresent(v -> detail.setLatestVersion(modelMapper.toVersionVO(v)));

        return detail;
    }

    @Transactional
    public ModelVO updateModel(Long id, ModelUpdateDTO dto) {
        ModelEntity entity = modelRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_NOT_FOUND));

        if ("PUBLISHED".equals(entity.getStatus()) || "DEPRECATED".equals(entity.getStatus())) {
            throw new BusinessException(400, "已发布或已弃用的模型不可修改");
        }

        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        if (dto.getTags() != null) entity.setTags(dto.getTags());

        entity = modelRepository.save(entity);
        return modelMapper.toModelVO(entity);
    }

    @Transactional
    public void deleteModel(Long id) {
        ModelEntity entity = modelRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_NOT_FOUND));

        if ("PUBLISHED".equals(entity.getStatus())) {
            throw new BusinessException(400, "已发布的模型不能删除，请先弃用");
        }

        modelRepository.delete(entity);
        log.info("模型已删除: id={}", id);
    }
}
