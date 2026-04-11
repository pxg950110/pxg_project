package com.maidc.model.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.model.dto.EvaluationCreateDTO;
import com.maidc.model.entity.EvaluationEntity;
import com.maidc.model.entity.ModelVersionEntity;
import com.maidc.model.mapper.ModelMapper;
import com.maidc.model.mq.ModelMessageProducer;
import com.maidc.model.repository.EvaluationRepository;
import com.maidc.model.repository.VersionRepository;
import com.maidc.model.vo.EvaluationVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock private EvaluationRepository evaluationRepository;
    @Mock private VersionRepository versionRepository;
    @Mock private ModelMessageProducer messageProducer;
    @Mock private ModelMapper modelMapper;
    @InjectMocks private EvaluationService evaluationService;

    @Test
    void createEvaluation_sendsMQAndUpdatesVersionStatus() {
        ModelVersionEntity version = new ModelVersionEntity();
        version.setModelId(10L);
        version.setStatus("APPROVED");
        when(versionRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(version));

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode metricsConfig = objectMapper.createObjectNode().put("accuracy", "true");

        EvaluationCreateDTO dto = EvaluationCreateDTO.builder()
                .modelVersionId(1L)
                .evalName("Accuracy Test")
                .evalType("OFFLINE")
                .datasetId(100L)
                .metricsConfig(metricsConfig)
                .build();

        EvaluationEntity savedEval = new EvaluationEntity();
        savedEval.setId(200L);
        when(evaluationRepository.save(any(EvaluationEntity.class))).thenAnswer(invocation -> {
            EvaluationEntity entity = invocation.getArgument(0);
            entity.setId(200L);
            return entity;
        });

        when(versionRepository.save(any(ModelVersionEntity.class))).thenReturn(version);
        when(modelMapper.toEvaluationVO(any(EvaluationEntity.class))).thenReturn(
                EvaluationVO.builder().id(200L).status("PENDING").build());

        evaluationService.createEvaluation(dto);

        // Verify MQ message sent with evaluation ID
        verify(messageProducer).sendEvaluationTask(eq(200L), eq(1L), eq(100L), anyMap());

        // Verify version status updated to EVALUATING
        verify(versionRepository).save(argThat(v ->
                "EVALUATING".equals(v.getStatus())
        ));
    }

    @Test
    void getEvaluation_nonExisting_throws() {
        when(evaluationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> evaluationService.getEvaluation(999L));
    }

    @Test
    void getEvaluationReportUrl_notCompleted_throws() {
        EvaluationEntity eval = new EvaluationEntity();
        eval.setId(1L);
        eval.setStatus("PENDING");
        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(eval));

        assertThrows(BusinessException.class, () -> evaluationService.getEvaluationReportUrl(1L));
    }
}
