package com.maidc.task.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.task.dto.PersonalTaskCreateDTO;
import com.maidc.task.entity.PersonalTaskEntity;
import com.maidc.task.mapper.PersonalTaskMapper;
import com.maidc.task.repository.PersonalTaskRepository;
import com.maidc.task.vo.PersonalTaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalTaskService {

    private final PersonalTaskRepository personalTaskRepository;
    private final PersonalTaskMapper personalTaskMapper;

    @Transactional
    public PersonalTaskVO createTask(PersonalTaskCreateDTO dto) {
        PersonalTaskEntity entity = personalTaskMapper.toEntity(dto);
        entity.setIsDeleted(false);
        PersonalTaskEntity saved = personalTaskRepository.save(entity);
        log.info("Personal task created: id={}, title={}, assignee={}", saved.getId(), saved.getTitle(), saved.getAssigneeId());
        return personalTaskMapper.toVO(saved);
    }

    @Transactional
    public PersonalTaskVO completeTask(Long id) {
        PersonalTaskEntity entity = getTaskOrThrow(id);
        entity.setStatus("COMPLETED");
        personalTaskRepository.save(entity);
        return personalTaskMapper.toVO(entity);
    }

    public List<PersonalTaskVO> getPendingTasks(Long assigneeId) {
        List<String> pendingStatuses = List.of("PENDING", "IN_PROGRESS");
        List<PersonalTaskEntity> entities = personalTaskRepository
                .findByAssigneeIdAndStatusInOrderByPriorityDescCreatedAtDesc(assigneeId, pendingStatuses);
        return entities.stream().map(personalTaskMapper::toVO).toList();
    }

    private PersonalTaskEntity getTaskOrThrow(Long id) {
        return personalTaskRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
