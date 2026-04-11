package com.maidc.task.service;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.task.entity.TaskEntity;
import com.maidc.task.entity.TaskExecutionEntity;
import com.maidc.task.mapper.TaskMapper;
import com.maidc.task.repository.TaskExecutionRepository;
import com.maidc.task.repository.TaskRepository;
import com.maidc.task.vo.TaskVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskExecutionRepository executionRepository;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskService taskService;

    @Test
    void triggerTask_pendingTask_changesToRunning() {
        // Arrange
        TaskEntity entity = new TaskEntity();
        entity.setTaskName("ETL Import");
        entity.setStatus("PENDING");

        TaskVO returnedVO = new TaskVO();
        returnedVO.setId("task-1");
        returnedVO.setTaskName("ETL Import");
        returnedVO.setStatus("RUNNING");

        when(taskRepository.findById("task-1")).thenReturn(Optional.of(entity));
        when(executionRepository.save(any(TaskExecutionEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(taskRepository.save(any(TaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(taskMapper.toVO(any(TaskEntity.class))).thenReturn(returnedVO);

        // Act
        TaskVO result = taskService.triggerTask("task-1");

        // Assert
        assertEquals("RUNNING", result.getStatus());
        verify(taskRepository).save(argThat(e -> "RUNNING".equals(e.getStatus())));
        verify(executionRepository).save(argThat(e -> "RUNNING".equals(e.getStatus()) && "MANUAL".equals(e.getTriggerType())));
    }

    @Test
    void pauseTask_runningTask_changesToPaused() {
        // Arrange
        TaskEntity entity = new TaskEntity();
        entity.setTaskName("Batch Job");
        entity.setStatus("RUNNING");

        TaskVO returnedVO = new TaskVO();
        returnedVO.setId("task-2");
        returnedVO.setTaskName("Batch Job");
        returnedVO.setStatus("PAUSED");

        when(taskRepository.findById("task-2")).thenReturn(Optional.of(entity));
        when(taskRepository.save(any(TaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(taskMapper.toVO(any(TaskEntity.class))).thenReturn(returnedVO);

        // Act
        TaskVO result = taskService.pauseTask("task-2");

        // Assert
        assertEquals("PAUSED", result.getStatus());
        verify(taskRepository).save(argThat(e -> "PAUSED".equals(e.getStatus())));
    }

    @Test
    void getTask_nonExisting_throws() {
        // Arrange
        when(taskRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BusinessException.class, () -> taskService.getTask("nonexistent"));
        verify(taskMapper, never()).toVO(any());
    }
}
