package com.maidc.data.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.EncounterEntity;
import com.maidc.data.mapper.DataMapper;
import com.maidc.data.repository.EncounterRepository;
import com.maidc.data.vo.EncounterVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EncounterServiceTest {

    @Mock
    private EncounterRepository encounterRepository;

    @Mock
    private DataMapper dataMapper;

    @InjectMocks
    private EncounterService encounterService;

    @Test
    void getEncounter_existingId_returnsEncounterVO() {
        // Arrange
        Long encounterId = 10L;
        EncounterEntity entity = new EncounterEntity();
        entity.setId(encounterId);
        entity.setPatientId(1L);
        entity.setEncounterType("INPATIENT");
        entity.setDepartment("内科");
        entity.setAdmissionTime(LocalDateTime.of(2025, 3, 10, 8, 0));

        EncounterVO expectedVO = EncounterVO.builder()
                .id(encounterId)
                .patientId(1L)
                .encounterType("INPATIENT")
                .department("内科")
                .admissionTime(LocalDateTime.of(2025, 3, 10, 8, 0))
                .build();

        when(encounterRepository.findByIdAndIsDeletedFalse(encounterId)).thenReturn(Optional.of(entity));
        when(dataMapper.toEncounterVO(entity)).thenReturn(expectedVO);

        // Act
        EncounterVO result = encounterService.getEncounter(encounterId);

        // Assert
        assertNotNull(result);
        assertEquals(encounterId, result.getId());
        assertEquals("INPATIENT", result.getEncounterType());
        assertEquals("内科", result.getDepartment());
        verify(encounterRepository).findByIdAndIsDeletedFalse(encounterId);
        verify(dataMapper).toEncounterVO(entity);
    }

    @Test
    void getEncounter_nonExistingId_throwsNotFound() {
        // Arrange
        Long nonExistingId = 999L;
        when(encounterRepository.findByIdAndIsDeletedFalse(nonExistingId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> encounterService.getEncounter(nonExistingId));

        assertEquals(ErrorCode.NOT_FOUND.getCode(), exception.getCode());
        verify(encounterRepository).findByIdAndIsDeletedFalse(nonExistingId);
        verifyNoInteractions(dataMapper);
    }

    @Test
    void findByPatientId_returnsMappedEncounters() {
        // Arrange
        Long patientId = 1L;

        EncounterEntity encounter1 = new EncounterEntity();
        encounter1.setId(10L);
        encounter1.setPatientId(patientId);
        encounter1.setEncounterType("OUTPATIENT");

        EncounterEntity encounter2 = new EncounterEntity();
        encounter2.setId(11L);
        encounter2.setPatientId(patientId);
        encounter2.setEncounterType("INPATIENT");

        List<EncounterEntity> entities = List.of(encounter1, encounter2);

        EncounterVO vo1 = EncounterVO.builder().id(10L).encounterType("OUTPATIENT").build();
        EncounterVO vo2 = EncounterVO.builder().id(11L).encounterType("INPATIENT").build();

        when(encounterRepository.findByPatientIdAndIsDeletedFalseOrderByAdmissionTimeDesc(patientId))
                .thenReturn(entities);
        when(dataMapper.toEncounterVO(encounter1)).thenReturn(vo1);
        when(dataMapper.toEncounterVO(encounter2)).thenReturn(vo2);

        // Act
        List<EncounterVO> results = encounterService.findByPatientId(patientId);

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("OUTPATIENT", results.get(0).getEncounterType());
        assertEquals("INPATIENT", results.get(1).getEncounterType());
        verify(encounterRepository).findByPatientIdAndIsDeletedFalseOrderByAdmissionTimeDesc(patientId);
    }

    @Test
    void createEncounter_savesAndReturnsVO() {
        // Arrange
        EncounterEntity entity = new EncounterEntity();
        entity.setPatientId(1L);
        entity.setEncounterType("INPATIENT");
        entity.setDepartment("外科");

        EncounterEntity savedEntity = new EncounterEntity();
        savedEntity.setId(100L);
        savedEntity.setPatientId(1L);
        savedEntity.setEncounterType("INPATIENT");
        savedEntity.setDepartment("外科");

        EncounterVO expectedVO = EncounterVO.builder()
                .id(100L)
                .patientId(1L)
                .encounterType("INPATIENT")
                .department("外科")
                .build();

        when(encounterRepository.save(entity)).thenReturn(savedEntity);
        when(dataMapper.toEncounterVO(savedEntity)).thenReturn(expectedVO);

        // Act
        EncounterVO result = encounterService.createEncounter(entity);

        // Assert
        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("外科", result.getDepartment());
        verify(encounterRepository).save(entity);
        verify(dataMapper).toEncounterVO(savedEntity);
    }
}
