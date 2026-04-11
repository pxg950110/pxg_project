package com.maidc.data.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.dto.DatasetCreateDTO;
import com.maidc.data.entity.DatasetEntity;
import com.maidc.data.entity.DatasetVersionEntity;
import com.maidc.data.mapper.DataMapper;
import com.maidc.data.repository.DatasetRepository;
import com.maidc.data.repository.DatasetVersionRepository;
import com.maidc.data.vo.DatasetDetailVO;
import com.maidc.data.vo.DatasetVO;
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
class DatasetServiceTest {

    @Mock
    private DatasetRepository datasetRepository;

    @Mock
    private DatasetVersionRepository datasetVersionRepository;

    @Mock
    private DataMapper dataMapper;

    @InjectMocks
    private DatasetService datasetService;

    @Test
    void createDataset_validDTO_savesAndReturnsVO() {
        // Arrange
        DatasetCreateDTO dto = DatasetCreateDTO.builder()
                .projectId(1L)
                .name("测试数据集")
                .description("用于测试")
                .dataType("IMAGE")
                .orgId(100L)
                .build();

        DatasetVO expectedVO = DatasetVO.builder()
                .id(1L)
                .projectId(1L)
                .name("测试数据集")
                .dataType("IMAGE")
                .versionCount(0)
                .sampleCount(0L)
                .sizeBytes(0L)
                .build();

        when(datasetRepository.save(any(DatasetEntity.class))).thenAnswer(inv -> {
            DatasetEntity e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });
        when(dataMapper.toDatasetVO(any(DatasetEntity.class))).thenReturn(expectedVO);

        // Act
        DatasetVO result = datasetService.createDataset(dto);

        // Assert
        assertNotNull(result);
        assertEquals("测试数据集", result.getName());
        assertEquals("IMAGE", result.getDataType());
        assertEquals(0, result.getVersionCount());
        verify(datasetRepository).save(argThat(e ->
                e.getProjectId().equals(1L)
                && "测试数据集".equals(e.getName())
                && "IMAGE".equals(e.getDataType())
                && e.getVersionCount() == 0
                && e.getSampleCount() == 0L
                && e.getSizeBytes() == 0L
        ));
    }

    @Test
    void getDataset_existingId_returnsDatasetVO() {
        // Arrange
        Long datasetId = 1L;
        DatasetEntity entity = new DatasetEntity();
        entity.setId(datasetId);
        entity.setProjectId(10L);
        entity.setName("心脏影像数据集");
        entity.setDataType("DICOM");

        DatasetVO expectedVO = DatasetVO.builder()
                .id(datasetId)
                .projectId(10L)
                .name("心脏影像数据集")
                .dataType("DICOM")
                .build();

        when(datasetRepository.findByIdAndIsDeletedFalse(datasetId)).thenReturn(Optional.of(entity));
        when(dataMapper.toDatasetVO(entity)).thenReturn(expectedVO);

        // Act
        DatasetVO result = datasetService.getDataset(datasetId);

        // Assert
        assertNotNull(result);
        assertEquals(datasetId, result.getId());
        assertEquals("心脏影像数据集", result.getName());
        verify(datasetRepository).findByIdAndIsDeletedFalse(datasetId);
    }

    @Test
    void getDataset_nonExistingId_throwsDatasetNotFound() {
        // Arrange
        Long nonExistingId = 999L;
        when(datasetRepository.findByIdAndIsDeletedFalse(nonExistingId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> datasetService.getDataset(nonExistingId));

        assertEquals(ErrorCode.DATASET_NOT_FOUND.getCode(), exception.getCode());
        verify(datasetRepository).findByIdAndIsDeletedFalse(nonExistingId);
        verifyNoInteractions(dataMapper);
    }

    @Test
    void getDatasetDetail_withVersions_returnsDetailVO() {
        // Arrange
        Long datasetId = 5L;
        DatasetEntity entity = new DatasetEntity();
        entity.setId(datasetId);
        entity.setProjectId(1L);
        entity.setName("CT扫描数据集");
        entity.setDescription("胸部CT");
        entity.setDataType("DICOM");
        entity.setVersionCount(2);
        entity.setSampleCount(1000L);
        entity.setSizeBytes(2048000L);

        DatasetVersionEntity versionEntity = new DatasetVersionEntity();
        versionEntity.setId(1L);
        versionEntity.setDatasetId(datasetId);
        versionEntity.setVersionNo("v1.0");
        versionEntity.setSampleCount(500L);

        DatasetDetailVO.DatasetVersionVO versionVO = DatasetDetailVO.DatasetVersionVO.builder()
                .id(1L)
                .versionNo("v1.0")
                .sampleCount(500L)
                .build();

        when(datasetRepository.findByIdAndIsDeletedFalse(datasetId)).thenReturn(Optional.of(entity));
        when(datasetVersionRepository.findByDatasetIdAndIsDeletedFalseOrderByVersionNoDesc(datasetId))
                .thenReturn(List.of(versionEntity));
        when(dataMapper.toDatasetVersionVO(versionEntity)).thenReturn(versionVO);

        // Act
        DatasetDetailVO result = datasetService.getDatasetDetail(datasetId);

        // Assert
        assertNotNull(result);
        assertEquals(datasetId, result.getId());
        assertEquals("CT扫描数据集", result.getName());
        assertEquals(2, result.getVersionCount());
        assertEquals(1, result.getVersions().size());
        assertEquals("v1.0", result.getVersions().get(0).getVersionNo());
    }

    @Test
    void deleteDataset_existingId_deletesEntity() {
        // Arrange
        Long datasetId = 3L;
        DatasetEntity entity = new DatasetEntity();
        entity.setId(datasetId);
        entity.setName("待删除数据集");

        when(datasetRepository.findByIdAndIsDeletedFalse(datasetId)).thenReturn(Optional.of(entity));

        // Act
        datasetService.deleteDataset(datasetId);

        // Assert
        verify(datasetRepository).findByIdAndIsDeletedFalse(datasetId);
        verify(datasetRepository).delete(entity);
    }

    @Test
    void deleteDataset_nonExistingId_throwsDatasetNotFound() {
        // Arrange
        Long nonExistingId = 888L;
        when(datasetRepository.findByIdAndIsDeletedFalse(nonExistingId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> datasetService.deleteDataset(nonExistingId));

        assertEquals(ErrorCode.DATASET_NOT_FOUND.getCode(), exception.getCode());
        verify(datasetRepository, never()).delete(any(DatasetEntity.class));
    }
}
