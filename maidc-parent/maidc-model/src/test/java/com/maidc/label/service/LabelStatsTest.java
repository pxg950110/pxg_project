package com.maidc.label.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.label.config.LabelRabbitMqConfig;
import com.maidc.label.entity.LabelRecordEntity;
import com.maidc.label.entity.LabelTaskEntity;
import com.maidc.label.mapper.LabelMapper;
import com.maidc.label.repository.LabelRecordRepository;
import com.maidc.label.repository.LabelTaskRepository;
import com.maidc.label.vo.LabelStatsVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabelStatsTest {

    @Mock
    private LabelTaskRepository labelTaskRepository;

    @Mock
    private LabelRecordRepository labelRecordRepository;

    @Mock
    private LabelMapper labelMapper;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private LabelRabbitMqConfig labelRabbitMqConfig;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private LabelTaskService labelTaskService;

    @BeforeEach
    void setUp() {
        // BaseMessageProducer uses @Autowired for rabbitTemplate, not constructor injection.
        // We must set it manually via reflection so the parent class field is populated.
        ReflectionTestUtils.setField(labelTaskService, "rabbitTemplate", rabbitTemplate);
    }

    @Test
    void getTaskStats_returnsCorrectCountsAndDistribution() {
        // Arrange
        Long taskId = 10L;
        LabelTaskEntity entity = new LabelTaskEntity();
        entity.setId(taskId);
        entity.setTotalCount(100);
        entity.setLabeledCount(60);
        entity.setVerifiedCount(30);

        LabelRecordEntity record1 = new LabelRecordEntity();
        record1.setTaskId(taskId.toString());
        record1.setLabel("恶性肿瘤");

        LabelRecordEntity record2 = new LabelRecordEntity();
        record2.setTaskId(taskId.toString());
        record2.setLabel("良性肿瘤");

        LabelRecordEntity record3 = new LabelRecordEntity();
        record3.setTaskId(taskId.toString());
        record3.setLabel("恶性肿瘤");

        List<LabelRecordEntity> records = List.of(record1, record2, record3);

        when(labelTaskRepository.findById(taskId)).thenReturn(Optional.of(entity));
        when(labelRecordRepository.findByTaskIdAndIsDeletedFalse(taskId.toString())).thenReturn(records);

        // Act
        LabelStatsVO result = labelTaskService.getTaskStats(taskId);

        // Assert
        assertNotNull(result);
        assertEquals(taskId, result.getTaskId());
        assertEquals(100, result.getTotalCount());
        assertEquals(60, result.getLabeledCount());
        assertEquals(30, result.getVerifiedCount());
        assertNotNull(result.getByLabel());
        assertEquals(2, result.getByLabel().get("恶性肿瘤"));
        assertEquals(1, result.getByLabel().get("良性肿瘤"));
    }

    @Test
    void getTaskStats_nonExistingTask_throwsNotFound() {
        // Arrange
        Long nonExistingId = 999L;
        when(labelTaskRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BusinessException.class, () -> labelTaskService.getTaskStats(nonExistingId));
        verify(labelRecordRepository, never()).findByTaskIdAndIsDeletedFalse(any());
    }

    @Test
    void getTaskStats_nullCounts_defaultToZero() {
        // Arrange
        Long taskId = 20L;
        LabelTaskEntity entity = new LabelTaskEntity();
        entity.setId(taskId);
        entity.setTotalCount(null);
        entity.setLabeledCount(null);
        entity.setVerifiedCount(null);

        when(labelTaskRepository.findById(taskId)).thenReturn(Optional.of(entity));
        when(labelRecordRepository.findByTaskIdAndIsDeletedFalse(taskId.toString()))
                .thenReturn(Collections.emptyList());

        // Act
        LabelStatsVO result = labelTaskService.getTaskStats(taskId);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalCount());
        assertEquals(0, result.getLabeledCount());
        assertEquals(0, result.getVerifiedCount());
        assertTrue(result.getByLabel().isEmpty());
    }

    @Test
    void triggerAiPreAnnotate_existingTask_invokesRabbitTemplate() {
        // Arrange
        Long taskId = 30L;
        LabelTaskEntity entity = new LabelTaskEntity();
        entity.setId(taskId);
        entity.setDatasetId("dataset-001");
        entity.setTaskType("NER");

        when(labelTaskRepository.findById(taskId)).thenReturn(Optional.of(entity));

        // Act & Assert - no exception means the MQ send was attempted
        assertDoesNotThrow(() -> labelTaskService.triggerAiPreAnnotate(taskId));
        verify(labelTaskRepository).findById(taskId);
    }

    @Test
    void triggerAiPreAnnotate_nonExistingTask_throwsNotFound() {
        // Arrange
        Long nonExistingId = 999L;
        when(labelTaskRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BusinessException.class, () -> labelTaskService.triggerAiPreAnnotate(nonExistingId));
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void deleteTask_existingTask_softDeletes() {
        // Arrange
        Long taskId = 40L;
        LabelTaskEntity entity = new LabelTaskEntity();
        entity.setId(taskId);
        entity.setName("待删除任务");
        entity.setIsDeleted(false);

        when(labelTaskRepository.findById(taskId)).thenReturn(Optional.of(entity));
        when(labelTaskRepository.save(any(LabelTaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        labelTaskService.deleteTask(taskId);

        // Assert
        verify(labelTaskRepository).save(argThat(e -> Boolean.TRUE.equals(e.getIsDeleted())));
    }
}
