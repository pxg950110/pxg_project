package com.maidc.model.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maidc.model.dto.DeploymentCreateDTO;
import com.maidc.model.entity.DeploymentEntity;
import com.maidc.model.entity.ModelVersionEntity;
import com.maidc.model.repository.DeploymentRepository;
import com.maidc.model.repository.VersionRepository;
import com.maidc.model.mq.ModelMessageProducer;
import com.maidc.model.mapper.ModelMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeploymentServiceTest {

    @Mock private DeploymentRepository deploymentRepository;
    @Mock private VersionRepository versionRepository;
    @Mock private ModelMessageProducer messageProducer;
    @Mock private ModelMapper modelMapper;
    @InjectMocks private DeploymentService deploymentService;

    @Test
    void createDeployment_sendsResourceConfigToMQ() {
        ModelVersionEntity version = new ModelVersionEntity();
        version.setModelId(1L);
        when(versionRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(version));

        ObjectMapper mapper = new ObjectMapper();
        JsonNode resourceConfig = mapper.createObjectNode().put("cpu", "2").put("memory", "4Gi");

        DeploymentCreateDTO dto = new DeploymentCreateDTO();
        dto.setModelVersionId(1L);
        dto.setDeploymentName("test-deploy");
        dto.setEnvironment("PROD");
        dto.setResourceConfig(resourceConfig);
        dto.setReplicas(2);

        DeploymentEntity savedEntity = new DeploymentEntity();
        savedEntity.setId(100L);
        when(deploymentRepository.save(any(DeploymentEntity.class))).thenReturn(savedEntity);
        when(modelMapper.toDeploymentVO(any())).thenReturn(null);

        deploymentService.createDeployment(dto);

        verify(messageProducer).sendDeploymentTask(eq(100L), eq(1L), argThat(map ->
            !map.isEmpty() && map.containsKey("cpu")
        ));
    }
}
