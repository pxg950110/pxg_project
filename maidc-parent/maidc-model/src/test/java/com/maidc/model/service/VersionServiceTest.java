package com.maidc.model.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.model.entity.ModelVersionEntity;
import com.maidc.model.mapper.ModelMapper;
import com.maidc.model.repository.ModelRepository;
import com.maidc.model.repository.VersionRepository;
import com.maidc.model.vo.VersionCompareVO;
import com.maidc.model.vo.VersionVO;
import com.maidc.common.minio.service.MinioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VersionServiceTest {

    @Mock
    private VersionRepository versionRepository;

    @Mock
    private ModelRepository modelRepository;

    @Mock
    private MinioService minioService;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private VersionService versionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void compareVersions_sameModel_returnsFileSizeDelta() throws Exception {
        // Arrange — two versions of the same model with different file sizes
        ModelVersionEntity v1 = new ModelVersionEntity();
        v1.setId(1L);
        v1.setModelId(100L);
        v1.setVersionNo("v1.0");
        v1.setModelFileSize(1024L);

        ModelVersionEntity v2 = new ModelVersionEntity();
        v2.setId(2L);
        v2.setModelId(100L);
        v2.setVersionNo("v2.0");
        v2.setModelFileSize(2048L);

        when(versionRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(v1));
        when(versionRepository.findByIdAndIsDeletedFalse(2L)).thenReturn(Optional.of(v2));
        when(modelMapper.toVersionVO(any(ModelVersionEntity.class))).thenReturn(new VersionVO());

        // Act
        VersionCompareVO result = versionService.compareVersions(100L, 1L, 2L);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getDiff());
        assertEquals(1024L, result.getDiff().get("file_size_delta"));
    }

    @Test
    void compareVersions_differentModels_throws() {
        // Arrange — v1 belongs to model 100, v2 belongs to model 200
        ModelVersionEntity v1 = new ModelVersionEntity();
        v1.setId(1L);
        v1.setModelId(100L);
        v1.setVersionNo("v1.0");
        v1.setModelFileSize(1024L);

        ModelVersionEntity v2 = new ModelVersionEntity();
        v2.setId(2L);
        v2.setModelId(200L);
        v2.setVersionNo("v1.0");
        v2.setModelFileSize(2048L);

        when(versionRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(v1));
        when(versionRepository.findByIdAndIsDeletedFalse(2L)).thenReturn(Optional.of(v2));

        // Act & Assert — requesting compare under modelId=100 but v2 belongs to 200
        BusinessException ex = assertThrows(BusinessException.class,
                () -> versionService.compareVersions(100L, 1L, 2L));
        assertEquals(ErrorCode.VERSION_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void compareVersions_withMetrics_returnsMetricsDiff() throws Exception {
        // Arrange — both versions have training metrics
        JsonNode metrics1 = objectMapper.readTree("{\"accuracy\": 0.85, \"loss\": 0.30}");
        JsonNode metrics2 = objectMapper.readTree("{\"accuracy\": 0.92, \"loss\": 0.15}");

        ModelVersionEntity v1 = new ModelVersionEntity();
        v1.setId(1L);
        v1.setModelId(100L);
        v1.setVersionNo("v1.0");
        v1.setModelFileSize(1024L);
        v1.setTrainingMetrics(metrics1);

        ModelVersionEntity v2 = new ModelVersionEntity();
        v2.setId(2L);
        v2.setModelId(100L);
        v2.setVersionNo("v2.0");
        v2.setModelFileSize(2048L);
        v2.setTrainingMetrics(metrics2);

        when(versionRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(v1));
        when(versionRepository.findByIdAndIsDeletedFalse(2L)).thenReturn(Optional.of(v2));
        when(modelMapper.toVersionVO(any(ModelVersionEntity.class))).thenReturn(new VersionVO());

        // Act
        VersionCompareVO result = versionService.compareVersions(100L, 1L, 2L);

        // Assert
        assertNotNull(result.getDiff().get("metrics_diff"));
        @SuppressWarnings("unchecked")
        Map<String, Object> metricsDiff = (Map<String, Object>) result.getDiff().get("metrics_diff");

        // accuracy delta: 0.92 - 0.85 = 0.07
        double accuracyDelta = ((Number) metricsDiff.get("accuracy")).doubleValue();
        assertEquals(0.07, accuracyDelta, 0.0001);

        // loss delta: 0.15 - 0.30 = -0.15
        double lossDelta = ((Number) metricsDiff.get("loss")).doubleValue();
        assertEquals(-0.15, lossDelta, 0.0001);
    }
}
