package com.maidc.label.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.label.config.LabelRabbitMqConfig;
import com.maidc.label.entity.LabelTaskEntity;
import com.maidc.label.mapper.LabelMapper;
import com.maidc.label.repository.LabelRecordRepository;
import com.maidc.label.repository.LabelTaskRepository;
import com.maidc.label.vo.LabelTaskDetailVO;
import com.maidc.label.vo.LabelTaskVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabelTaskServiceTest {

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

    @Test
    void getLabelTask_existingId_succeeds() {
        // Arrange
        LabelTaskEntity entity = new LabelTaskEntity();
        entity.setName("CT Scan Annotation");
        entity.setStatus("IN_PROGRESS");

        LabelTaskDetailVO detailVO = new LabelTaskDetailVO();
        detailVO.setId(1L);
        detailVO.setName("CT Scan Annotation");
        detailVO.setStatus("IN_PROGRESS");

        when(labelTaskRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(labelMapper.toTaskDetailVO(entity)).thenReturn(detailVO);

        // Act
        LabelTaskDetailVO result = labelTaskService.getTask(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("CT Scan Annotation", result.getName());
        assertEquals("IN_PROGRESS", result.getStatus());
        verify(labelTaskRepository).findById(1L);
        verify(labelMapper).toTaskDetailVO(entity);
    }

    @Test
    void getLabelTask_nonExisting_throws() {
        // Arrange
        when(labelTaskRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BusinessException.class, () -> labelTaskService.getTask(999L));
        verify(labelMapper, never()).toTaskDetailVO(any());
    }
}
