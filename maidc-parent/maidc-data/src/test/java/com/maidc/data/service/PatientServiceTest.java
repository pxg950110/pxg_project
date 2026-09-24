package com.maidc.data.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.core.result.PageResult;
import com.maidc.data.dto.PatientQueryDTO;
import com.maidc.data.entity.PatientEntity;
import com.maidc.data.mapper.DataMapper;
import com.maidc.data.repository.PatientRepository;
import com.maidc.data.vo.PatientVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DataMapper dataMapper;

    @InjectMocks
    private PatientService patientService;

    @Test
    void getPatient_existingId_returnsPatient() {
        // Arrange
        Long patientId = 1L;
        PatientEntity entity = new PatientEntity();
        entity.setId(patientId);
        entity.setName("张三");
        entity.setGender("男");
        entity.setBirthDate(LocalDate.of(1990, 1, 15));
        entity.setAddress("北京市海淀区");

        PatientVO expectedVO = PatientVO.builder()
                .id(patientId)
                .name("张三")
                .gender("男")
                .birthDate(LocalDate.of(1990, 1, 15))
                .address("北京市海淀区")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(patientRepository.findByIdAndIsDeletedFalse(patientId)).thenReturn(Optional.of(entity));
        when(dataMapper.toPatientVO(entity)).thenReturn(expectedVO);

        // Act
        PatientVO result = patientService.getPatient(patientId);

        // Assert
        assertNotNull(result);
        assertEquals(patientId, result.getId());
        assertEquals("张三", result.getName());
        assertEquals("男", result.getGender());
        verify(patientRepository).findByIdAndIsDeletedFalse(patientId);
        verify(dataMapper).toPatientVO(entity);
    }

    @Test
    void getPatient_nonExistingId_throws() {
        // Arrange
        Long nonExistingId = 999L;
        when(patientRepository.findByIdAndIsDeletedFalse(nonExistingId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> patientService.getPatient(nonExistingId));

        assertEquals(ErrorCode.PATIENT_NOT_FOUND.getCode(), exception.getCode());
        verify(patientRepository).findByIdAndIsDeletedFalse(nonExistingId);
        verifyNoInteractions(dataMapper);
    }

    // ==================== PII 脱敏（列表/详情出口不回明文证件号/手机号，对齐就诊流） ====================

    @Test
    void getPatient_masksIdCardAndPhone() {
        PatientEntity entity = new PatientEntity();
        entity.setId(1L);
        entity.setName("张三");
        entity.setIdCardNo("320102199001011234");
        entity.setPhone("13812345678");
        when(patientRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(entity));
        when(dataMapper.toPatientVO(entity)).thenReturn(PatientVO.builder()
                .id(1L).name("张三")
                .idCardNo("320102199001011234").phone("13812345678")
                .build());

        PatientVO vo = patientService.getPatient(1L);

        assertEquals("320***********1234", vo.getIdCardNo());
        assertEquals("138****5678", vo.getPhone());
    }

    @Test
    void listPatients_masksIdCardAndPhone() {
        PatientEntity entity = new PatientEntity();
        entity.setId(2L);
        entity.setIdCardNo("320102199001011234");
        entity.setPhone("13812345678");
        Page<PatientEntity> page = new PageImpl<>(List.of(entity));
        when(patientRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(dataMapper.toPatientVO(any(PatientEntity.class))).thenReturn(PatientVO.builder()
                .id(2L).idCardNo("320102199001011234").phone("13812345678")
                .build());

        PageResult<PatientVO> result = patientService.listPatients(new PatientQueryDTO());

        assertEquals("320***********1234", result.getItems().get(0).getIdCardNo());
        assertEquals("138****5678", result.getItems().get(0).getPhone());
    }
}
