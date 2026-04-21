package com.maidc.task.service;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.task.dto.PersonalTaskCreateDTO;
import com.maidc.task.entity.PersonalTaskEntity;
import com.maidc.task.mapper.PersonalTaskMapper;
import com.maidc.task.repository.PersonalTaskRepository;
import com.maidc.task.vo.PersonalTaskVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalTaskServiceTest {

    @Mock
    private PersonalTaskRepository personalTaskRepository;

    @Mock
    private PersonalTaskMapper personalTaskMapper;

    @InjectMocks
    private PersonalTaskService personalTaskService;

    @Test
    void createTask_validInput_returnsVO() {
        PersonalTaskCreateDTO dto = PersonalTaskCreateDTO.builder()
                .title("审批模型 v2.1")
                .taskType("APPROVAL")
                .assigneeId(1L)
                .priority("HIGH")
                .build();

        PersonalTaskEntity entity = new PersonalTaskEntity();
        entity.setTitle(dto.getTitle());
        entity.setTaskType(dto.getTaskType());

        PersonalTaskVO vo = new PersonalTaskVO();
        vo.setId(1L);
        vo.setTitle(dto.getTitle());

        when(personalTaskMapper.toEntity(dto)).thenReturn(entity);
        when(personalTaskRepository.save(any(PersonalTaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(personalTaskMapper.toVO(any(PersonalTaskEntity.class))).thenReturn(vo);

        PersonalTaskVO result = personalTaskService.createTask(dto);

        assertNotNull(result);
        assertEquals("审批模型 v2.1", result.getTitle());
        verify(personalTaskRepository).save(any(PersonalTaskEntity.class));
    }

    @Test
    void completeTask_pendingTask_changesToCompleted() {
        PersonalTaskEntity entity = new PersonalTaskEntity();
        entity.setId(1L);
        entity.setStatus("PENDING");
        entity.setTitle("Test task");

        PersonalTaskVO vo = new PersonalTaskVO();
        vo.setId(1L);
        vo.setStatus("COMPLETED");

        when(personalTaskRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(personalTaskRepository.save(any(PersonalTaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(personalTaskMapper.toVO(any(PersonalTaskEntity.class))).thenReturn(vo);

        PersonalTaskVO result = personalTaskService.completeTask(1L);

        assertEquals("COMPLETED", result.getStatus());
        verify(personalTaskRepository).save(argThat(e -> "COMPLETED".equals(e.getStatus())));
    }

    @Test
    void completeTask_nonExisting_throws() {
        when(personalTaskRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> personalTaskService.completeTask(999L));
    }

    @Test
    void getPendingTasks_returnsList() {
        Long assigneeId = 1L;
        List<String> pendingStatuses = List.of("PENDING", "IN_PROGRESS");

        PersonalTaskEntity entity = new PersonalTaskEntity();
        entity.setId(1L);
        entity.setTitle("Task 1");
        entity.setStatus("PENDING");

        when(personalTaskRepository.findByAssigneeIdAndStatusInOrderByPriorityDescCreatedAtDesc(
                assigneeId, pendingStatuses)).thenReturn(List.of(entity));

        PersonalTaskVO vo = new PersonalTaskVO();
        vo.setId(1L);
        vo.setTitle("Task 1");
        when(personalTaskMapper.toVO(entity)).thenReturn(vo);

        List<PersonalTaskVO> result = personalTaskService.getPendingTasks(assigneeId);

        assertEquals(1, result.size());
        assertEquals("Task 1", result.get(0).getTitle());
    }
}
