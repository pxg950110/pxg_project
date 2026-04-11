package com.maidc.data.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.core.result.PageResult;
import com.maidc.data.dto.EtlTaskCreateDTO;
import com.maidc.data.dto.EtlTaskQueryDTO;
import com.maidc.data.entity.EtlTaskEntity;
import com.maidc.data.mapper.DataMapper;
import com.maidc.data.repository.EtlTaskRepository;
import com.maidc.data.repository.EtlTaskSpecification;
import com.maidc.data.vo.EtlTaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EtlTaskService {

    private final EtlTaskRepository etlTaskRepository;
    private final DataMapper dataMapper;

    @Transactional
    public EtlTaskVO createTask(EtlTaskCreateDTO dto) {
        EtlTaskEntity entity = new EtlTaskEntity();
        entity.setName(dto.getName());
        entity.setSourceType(dto.getSourceType());
        entity.setTargetType(dto.getTargetType());
        entity.setCronExpression(dto.getCronExpression());
        entity.setConfig(dto.getConfig());
        entity.setStatus("IDLE");
        entity.setOrgId(dto.getOrgId() != null ? dto.getOrgId() : 0L);

        entity = etlTaskRepository.save(entity);
        log.info("ETL任务创建成功: id={}, name={}", entity.getId(), entity.getName());
        return dataMapper.toEtlTaskVO(entity);
    }

    public EtlTaskVO getTask(Long id) {
        EtlTaskEntity entity = etlTaskRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return dataMapper.toEtlTaskVO(entity);
    }

    public PageResult<EtlTaskVO> listTasks(EtlTaskQueryDTO query) {
        Specification<EtlTaskEntity> spec = EtlTaskSpecification.buildSearchSpec(
                query.getOrgId(), query.getKeyword(), query.getStatus(), query.getSourceType());

        Page<EtlTaskEntity> page = etlTaskRepository.findAll(spec,
                PageRequest.of(query.getPage() - 1, query.getPageSize()));

        Page<EtlTaskVO> voPage = page.map(dataMapper::toEtlTaskVO);
        return PageResult.of(voPage);
    }

    @Transactional
    public EtlTaskVO triggerTask(Long id) {
        EtlTaskEntity entity = etlTaskRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if ("RUNNING".equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.TASK_ALREADY_RUNNING);
        }

        entity.setStatus("RUNNING");
        entity.setLastExecutionTime(LocalDateTime.now());
        entity = etlTaskRepository.save(entity);

        log.info("ETL任务已触发: id={}, name={}", entity.getId(), entity.getName());
        return dataMapper.toEtlTaskVO(entity);
    }

    @Transactional
    public EtlTaskVO pauseTask(Long id) {
        EtlTaskEntity entity = etlTaskRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (!"RUNNING".equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.TASK_NOT_RUNNING);
        }

        entity.setStatus("PAUSED");
        entity = etlTaskRepository.save(entity);

        log.info("ETL任务已暂停: id={}, name={}", entity.getId(), entity.getName());
        return dataMapper.toEtlTaskVO(entity);
    }

    @Transactional
    public void deleteTask(Long id) {
        EtlTaskEntity entity = etlTaskRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        etlTaskRepository.delete(entity);
        log.info("ETL任务已删除: id={}", id);
    }
}
