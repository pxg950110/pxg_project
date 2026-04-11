package com.maidc.model.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.model.config.AiWorkerClient;
import com.maidc.model.dto.InferenceRequestDTO;
import com.maidc.model.entity.DeploymentEntity;
import com.maidc.model.entity.InferenceLogEntity;
import com.maidc.model.repository.DeploymentRepository;
import com.maidc.model.repository.InferenceLogRepository;
import com.maidc.model.vo.InferenceResultVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InferenceServiceTest {

    @Mock
    private DeploymentRepository deploymentRepository;

    @Mock
    private InferenceLogRepository inferenceLogRepository;

    @Mock
    private AiWorkerClient aiWorkerClient;

    @InjectMocks
    private InferenceService inferenceService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void inference_runningDeployment_callsAiWorkerAndLogsSuccess() throws Exception {
        // Arrange
        Long deploymentId = 1L;
        DeploymentEntity deployment = new DeploymentEntity();
        deployment.setId(deploymentId);
        deployment.setStatus("RUNNING");
        deployment.setEndpointUrl("http://localhost:8090/api/v1/predict");
        deployment.setOrgId(100L);

        JsonNode input = objectMapper.readTree("{\"features\":[1.0,2.0,3.0]}");
        JsonNode aiWorkerResult = objectMapper.readTree("{\"prediction\":\"positive\",\"score\":0.95}");

        InferenceRequestDTO dto = InferenceRequestDTO.builder()
                .requestId("req-001")
                .patientId(200L)
                .encounterId(300L)
                .input(input)
                .build();

        when(deploymentRepository.findByIdAndIsDeletedFalse(deploymentId))
                .thenReturn(Optional.of(deployment));
        when(aiWorkerClient.predict(eq("http://localhost:8090/api/v1/predict"), eq(input)))
                .thenReturn(aiWorkerResult);
        when(inferenceLogRepository.save(any(InferenceLogEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        InferenceResultVO result = inferenceService.inference(deploymentId, dto);

        // Assert
        assertNotNull(result);
        assertEquals("req-001", result.getRequestId());
        assertEquals(aiWorkerResult, result.getResults());
        assertNotNull(result.getLatencyMs());
        assertEquals("latest", result.getModelVersion());

        // Verify log saved with SUCCESS
        ArgumentCaptor<InferenceLogEntity> logCaptor = ArgumentCaptor.forClass(InferenceLogEntity.class);
        verify(inferenceLogRepository).save(logCaptor.capture());
        InferenceLogEntity savedLog = logCaptor.getValue();
        assertEquals(deploymentId, savedLog.getDeploymentId());
        assertEquals("req-001", savedLog.getRequestId());
        assertEquals("SUCCESS", savedLog.getStatus());
        assertEquals(aiWorkerResult, savedLog.getOutputResult());
        assertNotNull(savedLog.getLatencyMs());
        assertEquals(100L, savedLog.getOrgId());

        // Verify AiWorkerClient was called
        verify(aiWorkerClient).predict("http://localhost:8090/api/v1/predict", input);
    }

    @Test
    void inference_stoppedDeployment_throws() {
        // Arrange
        Long deploymentId = 2L;
        DeploymentEntity deployment = new DeploymentEntity();
        deployment.setId(deploymentId);
        deployment.setStatus("STOPPED");

        InferenceRequestDTO dto = InferenceRequestDTO.builder()
                .requestId("req-002")
                .input(objectMapper.createArrayNode())
                .build();

        when(deploymentRepository.findByIdAndIsDeletedFalse(deploymentId))
                .thenReturn(Optional.of(deployment));

        // Act & Assert
        assertThrows(BusinessException.class, () ->
                inferenceService.inference(deploymentId, dto));

        // Verify no inference log saved
        verify(inferenceLogRepository, never()).save(any());
        verify(aiWorkerClient, never()).predict(any(), any());
    }

    @Test
    void inference_aiWorkerThrows_logsFailure() throws Exception {
        // Arrange
        Long deploymentId = 3L;
        DeploymentEntity deployment = new DeploymentEntity();
        deployment.setId(deploymentId);
        deployment.setStatus("RUNNING");
        deployment.setEndpointUrl("http://localhost:8090/api/v1/predict");
        deployment.setOrgId(100L);

        JsonNode input = objectMapper.readTree("{\"features\":[1.0]}");

        InferenceRequestDTO dto = InferenceRequestDTO.builder()
                .requestId("req-003")
                .patientId(400L)
                .input(input)
                .build();

        when(deploymentRepository.findByIdAndIsDeletedFalse(deploymentId))
                .thenReturn(Optional.of(deployment));
        when(aiWorkerClient.predict(any(), any()))
                .thenThrow(new RuntimeException("Connection refused"));
        when(inferenceLogRepository.save(any(InferenceLogEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
                inferenceService.inference(deploymentId, dto));
        assertTrue(thrown.getMessage().contains("推理失败"));

        // Verify log saved with FAILED status
        ArgumentCaptor<InferenceLogEntity> logCaptor = ArgumentCaptor.forClass(InferenceLogEntity.class);
        verify(inferenceLogRepository).save(logCaptor.capture());
        InferenceLogEntity savedLog = logCaptor.getValue();
        assertEquals("FAILED", savedLog.getStatus());
        assertEquals("Connection refused", savedLog.getErrorMessage());
        assertEquals(deploymentId, savedLog.getDeploymentId());
    }
}
