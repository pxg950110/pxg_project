package com.maidc.task.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.task.entity.TaskEntity;
import com.maidc.task.entity.TaskExecutionEntity;
import com.maidc.task.mapper.TaskMapper;
import com.maidc.task.repository.TaskExecutionRepository;
import com.maidc.task.repository.TaskRepository;
import com.maidc.task.vo.TaskExecutionVO;
import com.maidc.task.vo.TaskVO;
import com.maidc.common.core.result.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskExecutionTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskExecutionRepository executionRepository;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskService taskService;

    @Test
    void resumeTask_pausedTask_changesToRunning() {
        // Arrange
        String taskId = "task-paused-1";
        TaskEntity entity = new TaskEntity();
        entity.setTaskName("数据导入任务");
        entity.setStatus("PAUSED");

        TaskVO expectedVO = new TaskVO();
        expectedVO.setId(taskId);
        expectedVO.setTaskName("数据导入任务");
        expectedVO.setStatus("RUNNING");

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(entity));
        when(taskRepository.save(any(TaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(taskMapper.toVO(any(TaskEntity.class))).thenReturn(expectedVO);

        // Act
        TaskVO result = taskService.resumeTask(taskId);

        // Assert
        assertNotNull(result);
        assertEquals("RUNNING", result.getStatus());
        verify(taskRepository).save(argThat(e -> "RUNNING".equals(e.getStatus())));
    }

    @Test
    void resumeTask_nonPausedTask_throwsBusinessException() {
        // Arrange
        String taskId = "task-running-1";
        TaskEntity entity = new TaskEntity();
        entity.setStatus("RUNNING");

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(entity));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> taskService.resumeTask(taskId));

        assertEquals(ErrorCode.TASK_NOT_PAUSED.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("暂停"));
        verify(taskRepository, never()).save(any());
    }

    @Test
    void deleteTask_existingTask_softDeletes() {
        // Arrange
        String taskId = "task-delete-1";
        TaskEntity entity = new TaskEntity();
        entity.setTaskName("待删除任务");
        entity.setIsDeleted(false);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(entity));
        when(taskRepository.save(any(TaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        taskService.deleteTask(taskId);

        // Assert
        verify(taskRepository).save(argThat(e -> Boolean.TRUE.equals(e.getIsDeleted())));
    }

    @Test
    void getExecutions_returnsExecutionHistory() {
        // Arrange
        String taskId = "task-exec-1";

        TaskExecutionEntity execution1 = new TaskExecutionEntity();
        execution1.setId("exec-1");
        execution1.setTaskId(taskId);
        execution1.setStatus("COMPLETED");
        execution1.setTriggerType("MANUAL");
        execution1.setStartTime(LocalDateTime.of(2025, 3, 1, 10, 0));
        execution1.setDuration(5000L);
        execution1.setRecordsProcessed(1000L);

        TaskExecutionEntity execution2 = new TaskExecutionEntity();
        execution2.setId("exec-2");
        execution2.setTaskId(taskId);
        execution2.setStatus("FAILED");
        execution2.setTriggerType("SCHEDULED");
        execution2.setStartTime(LocalDateTime.of(2025, 3, 2, 10, 0));
        execution2.setErrorMessage("Connection timeout");

        Page<TaskExecutionEntity> mockPage = new PageImpl<>(List.of(execution1, execution2));

        TaskExecutionVO vo1 = new TaskExecutionVO();
        vo1.setId("exec-1");
        vo1.setStatus("COMPLETED");
        vo1.setTriggerType("MANUAL");

        TaskExecutionVO vo2 = new TaskExecutionVO();
        vo2.setId("exec-2");
        vo2.setStatus("FAILED");
        vo2.setTriggerType("SCHEDULED");

        when(executionRepository.findByTaskIdOrderByStartTimeDesc(eq(taskId), any(Pageable.class)))
                .thenReturn(mockPage);
        when(taskMapper.toExecutionVO(execution1)).thenReturn(vo1);
        when(taskMapper.toExecutionVO(execution2)).thenReturn(vo2);

        // Act
        PageResult<TaskExecutionVO> result = taskService.getExecutions(taskId, 1, 20);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotal());
        assertEquals(2, result.getItems().size());
        assertEquals("COMPLETED", result.getItems().get(0).getStatus());
        assertEquals("FAILED", result.getItems().get(1).getStatus());
        verify(executionRepository).findByTaskIdOrderByStartTimeDesc(eq(taskId), any(Pageable.class));
    }

    @Test
    void triggerTask_createsExecutionRecord() {
        // Arrange
        String taskId = "task-trigger-exec";
        TaskEntity entity = new TaskEntity();
        entity.setTaskName("ETL Pipeline");
        entity.setStatus("CREATED");

        TaskVO expectedVO = new TaskVO();
        expectedVO.setId(taskId);
        expectedVO.setStatus("RUNNING");

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(entity));
        when(executionRepository.save(any(TaskExecutionEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(taskRepository.save(any(TaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(taskMapper.toVO(any(TaskEntity.class))).thenReturn(expectedVO);

        // Act
        TaskVO result = taskService.triggerTask(taskId);

        // Assert
        assertEquals("RUNNING", result.getStatus());
        verify(executionRepository).save(argThat(exec ->
                "RUNNING".equals(exec.getStatus())
                && taskId.equals(exec.getTaskId())
                && "MANUAL".equals(exec.getTriggerType())
                && exec.getStartTime() != null
        ));
        verify(taskRepository).save(argThat(e ->
                "RUNNING".equals(e.getStatus())
                && e.getLastExecutionTime() != null
        ));
    }
}
