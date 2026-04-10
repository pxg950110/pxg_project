package com.maidc.data.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.core.result.PageResult;
import com.maidc.data.dto.DatasetCreateDTO;
import com.maidc.data.dto.DatasetQueryDTO;
import com.maidc.data.entity.DatasetEntity;
import com.maidc.data.entity.DatasetVersionEntity;
import com.maidc.data.mapper.DataMapper;
import com.maidc.data.repository.DatasetRepository;
import com.maidc.data.repository.DatasetSpecification;
import com.maidc.data.repository.DatasetVersionRepository;
import com.maidc.data.vo.DatasetDetailVO;
import com.maidc.data.vo.DatasetVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatasetService {

    private final DatasetRepository datasetRepository;
    private final DatasetVersionRepository datasetVersionRepository;
    private final DataMapper dataMapper;

    @Transactional
    public DatasetVO createDataset(DatasetCreateDTO dto) {
        DatasetEntity entity = new DatasetEntity();
        entity.setProjectId(dto.getProjectId());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setDataType(dto.getDataType());
        entity.setVersionCount(0);
        entity.setSampleCount(0L);
        entity.setSizeBytes(0L);
        entity.setOrgId(dto.getOrgId() != null ? dto.getOrgId() : 0L);

        entity = datasetRepository.save(entity);
        log.info("数据集创建成功: id={}, name={}", entity.getId(), entity.getName());
        return dataMapper.toDatasetVO(entity);
    }

    public DatasetVO getDataset(Long id) {
        DatasetEntity entity = datasetRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATASET_NOT_FOUND));
        return dataMapper.toDatasetVO(entity);
    }

    public DatasetDetailVO getDatasetDetail(Long id) {
        DatasetEntity entity = datasetRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATASET_NOT_FOUND));

        List<DatasetDetailVO.DatasetVersionVO> versions = datasetVersionRepository
                .findByDatasetIdAndIsDeletedFalseOrderByVersionNoDesc(id)
                .stream()
                .map(dataMapper::toDatasetVersionVO)
                .toList();

        return DatasetDetailVO.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .name(entity.getName())
                .description(entity.getDescription())
                .dataType(entity.getDataType())
                .versionCount(entity.getVersionCount())
                .sampleCount(entity.getSampleCount())
                .sizeBytes(entity.getSizeBytes())
                .versions(versions)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public PageResult<DatasetVO> listDatasets(DatasetQueryDTO query) {
        Specification<DatasetEntity> spec = DatasetSpecification.buildSearchSpec(
                query.getOrgId(), query.getProjectId(), query.getDataType(), query.getKeyword());

        Page<DatasetEntity> page = datasetRepository.findAll(spec,
                PageRequest.of(query.getPage() - 1, query.getPageSize()));

        Page<DatasetVO> voPage = page.map(dataMapper::toDatasetVO);
        return PageResult.of(voPage);
    }

    @Transactional
    public void deleteDataset(Long id) {
        DatasetEntity entity = datasetRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATASET_NOT_FOUND));
        datasetRepository.delete(entity);
        log.info("数据集已删除: id={}", id);
    }
}
