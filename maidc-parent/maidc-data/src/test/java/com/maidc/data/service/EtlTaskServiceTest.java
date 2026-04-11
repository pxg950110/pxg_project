package com.maidc.data.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.dto.EtlTaskCreateDTO;
import com.maidc.data.entity.EtlTaskEntity;
import com.maidc.data.mapper.DataMapper;
import com.maidc.data.repository.EtlTaskRepository;
import com.maidc.data.vo.EtlTaskVO;
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
class EtlTaskServiceTest {

    @Mock
    private EtlTaskRepository etlTaskRepository;

    @Mock
    private DataMapper dataMapper;

    @InjectMocks
    private EtlTaskService etlTaskService;

    @Test
    void createTask_validDTO_savesWithIdleStatus() {
        // Arrange
        EtlTaskCreateDTO dto = EtlTaskCreateDTO.builder()
                .name("HL7导入任务")
                .sourceType("HL7")
                .targetType("FHIR")
                .cronExpression("0 0 2 * * ?")
                .orgId(10L)
                .build();

        EtlTaskVO expectedVO = EtlTaskVO.builder()
                .id(1L)
                .name("HL7导入任务")
                .sourceType("HL7")
                .targetType("FHIR")
                .status("IDLE")
                .cronExpression("0 0 2 * * ?")
                .build();

        when(etlTaskRepository.save(any(EtlTaskEntity.class))).thenAnswer(inv -> {
            EtlTaskEntity e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });
        when(dataMapper.toEtlTaskVO(any(EtlTaskEntity.class))).thenReturn(expectedVO);

        // Act
        EtlTaskVO result = etlTaskService.createTask(dto);

        // Assert
        assertNotNull(result);
        assertEquals("HL7导入任务", result.getName());
        assertEquals("IDLE", result.getStatus());
        verify(etlTaskRepository).save(argThat(e ->
                "IDLE".equals(e.getStatus())
                && "HL7导入任务".equals(e.getName())
                && "HL7".equals(e.getSourceType())
                && "FHIR".equals(e.getTargetType())
        ));
    }

    @Test
    void triggerTask_idleTask_changesToRunning() {
        // Arrange
        Long taskId = 5L;
        EtlTaskEntity entity = new EtlTaskEntity();
        entity.setId(taskId);
        entity.setName("数据导入");
        entity.setStatus("IDLE");

        EtlTaskVO expectedVO = EtlTaskVO.builder()
                .id(taskId)
                .name("数据导入")
                .status("RUNNING")
                .build();

        when(etlTaskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(entity));
        when(etlTaskRepository.save(any(EtlTaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(dataMapper.toEtlTaskVO(any(EtlTaskEntity.class))).thenReturn(expectedVO);

        // Act
        EtlTaskVO result = etlTaskService.triggerTask(taskId);

        // Assert
        assertNotNull(result);
        assertEquals("RUNNING", result.getStatus());
        verify(etlTaskRepository).save(argThat(e ->
                "RUNNING".equals(e.getStatus())
                && e.getLastExecutionTime() != null
        ));
    }

    @Test
    void triggerTask_runningTask_throwsBusinessException() {
        // Arrange
        Long taskId = 5L;
        EtlTaskEntity entity = new EtlTaskEntity();
        entity.setId(taskId);
        entity.setName("正在运行的任务");
        entity.setStatus("RUNNING");

        when(etlTaskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(entity));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> etlTaskService.triggerTask(taskId));

        assertEquals(ErrorCode.TASK_ALREADY_RUNNING.getCode(), exception.getCode());
        verify(etlTaskRepository, never()).save(any());
    }

    @Test
    void pauseTask_runningTask_changesToPaused() {
        // Arrange
        Long taskId = 6L;
        EtlTaskEntity entity = new EtlTaskEntity();
        entity.setId(taskId);
        entity.setName("数据同步");
        entity.setStatus("RUNNING");

        EtlTaskVO expectedVO = EtlTaskVO.builder()
                .id(taskId)
                .name("数据同步")
                .status("PAUSED")
                .build();

        when(etlTaskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(entity));
        when(etlTaskRepository.save(any(EtlTaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(dataMapper.toEtlTaskVO(any(EtlTaskEntity.class))).thenReturn(expectedVO);

        // Act
        EtlTaskVO result = etlTaskService.pauseTask(taskId);

        // Assert
        assertNotNull(result);
        assertEquals("PAUSED", result.getStatus());
        verify(etlTaskRepository).save(argThat(e -> "PAUSED".equals(e.getStatus())));
    }

    @Test
    void pauseTask_nonRunningTask_throwsBusinessException() {
        // Arrange
        Long taskId = 7L;
        EtlTaskEntity entity = new EtlTaskEntity();
        entity.setId(taskId);
        entity.setName("空闲任务");
        entity.setStatus("IDLE");

        when(etlTaskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(entity));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> etlTaskService.pauseTask(taskId));

        assertEquals(ErrorCode.TASK_NOT_RUNNING.getCode(), exception.getCode());
        verify(etlTaskRepository, never()).save(any());
    }

    @Test
    void getTask_nonExisting_throwsNotFound() {
        // Arrange
        Long nonExistingId = 999L;
        when(etlTaskRepository.findByIdAndIsDeletedFalse(nonExistingId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> etlTaskService.getTask(nonExistingId));

        assertEquals(ErrorCode.NOT_FOUND.getCode(), exception.getCode());
        verifyNoInteractions(dataMapper);
    }

    @Test
    void deleteTask_existingId_deletesSuccessfully() {
        // Arrange
        Long taskId = 8L;
        EtlTaskEntity entity = new EtlTaskEntity();
        entity.setId(taskId);
        entity.setName("待删除任务");
        entity.setStatus("IDLE");

        when(etlTaskRepository.findByIdAndIsDeletedFalse(taskId)).thenReturn(Optional.of(entity));

        // Act
        etlTaskService.deleteTask(taskId);

        // Assert
        verify(etlTaskRepository).delete(entity);
    }
}
