package com.maidc.data.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.security.context.PermissionContext;
import com.maidc.common.security.scope.DataScope;
import com.maidc.common.security.store.PermissionStore;
import com.maidc.data.dto.EncounterDetailDTO;
import com.maidc.data.dto.PatientEncounterListDTO;
import com.maidc.data.entity.EncounterEntity;
import com.maidc.data.entity.InstitutionEntity;
import com.maidc.data.entity.PatientEntity;
import com.maidc.data.repository.DiagnosisRepository;
import com.maidc.data.repository.EncounterRepository;
import com.maidc.data.repository.ImagingExamRepository;
import com.maidc.data.repository.InstitutionRepository;
import com.maidc.data.repository.LabPanelRepository;
import com.maidc.data.repository.LabTestRepository;
import com.maidc.data.repository.MedicationRepository;
import com.maidc.data.repository.OperationRepository;
import com.maidc.data.repository.PatientRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * DEPT 数据范围过滤（fail-closed，∃ 语义）：至少一条就诊科室 == 用户科室 → 可见；
 * 无匹配/零就诊 → 抛与真实"未找到"相同的异常（同 code 同 message，不暴露存在性）
 */
@ExtendWith(MockitoExtension.class)
class PatientEncounterServiceTest {

    private static final Long USER_ID = 9L;
    private static final Long DEPT_ID = 3L;
    private static final String DEPT_NAME = "内科";

    @Mock
    private PatientRepository patientRepository;
    @Mock
    private EncounterRepository encounterRepository;
    @Mock
    private DiagnosisRepository diagnosisRepository;
    @Mock
    private LabTestRepository labTestRepository;
    @Mock
    private LabPanelRepository labPanelRepository;
    @Mock
    private ImagingExamRepository imagingExamRepository;
    @Mock
    private MedicationRepository medicationRepository;
    @Mock
    private OperationRepository operationRepository;
    @Mock
    private PermissionStore permissionStore;
    @Mock
    private InstitutionRepository institutionRepository;

    @InjectMocks
    private PatientEncounterService patientEncounterService;

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void mockUser(DataScope scope, Long deptId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", String.valueOf(USER_ID));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        lenient().when(permissionStore.load(USER_ID)).thenReturn(PermissionContext.builder()
                .userId(USER_ID).dataScope(scope).deptId(deptId).build());
    }

    private void mockInstitution(String name) {
        InstitutionEntity institution = new InstitutionEntity();
        institution.setId(DEPT_ID);
        institution.setName(name);
        lenient().when(institutionRepository.findById(DEPT_ID)).thenReturn(Optional.of(institution));
    }

    private EncounterEntity encounter(Long id, String department) {
        EncounterEntity e = new EncounterEntity();
        e.setId(id);
        e.setDepartment(department);
        return e;
    }

    // ==================== getEncounterDetail ====================

    @Test
    void getEncounterDetail_deptScope_matchingDept_returnsDetail() {
        mockUser(DataScope.DEPT, DEPT_ID);
        mockInstitution(DEPT_NAME);
        EncounterEntity e = encounter(10L, DEPT_NAME);
        when(encounterRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(e));

        EncounterDetailDTO dto = patientEncounterService.getEncounterDetail(10L);

        assertNotNull(dto);
        assertNotNull(dto.getBasicInfo());
        assertEquals(DEPT_NAME, dto.getBasicInfo().getDeptName());
    }

    @Test
    void getEncounterDetail_deptScope_mismatchedDept_sameAsNotFound() {
        mockUser(DataScope.DEPT, DEPT_ID);
        mockInstitution(DEPT_NAME);
        when(encounterRepository.findByIdAndIsDeletedFalse(10L))
                .thenReturn(Optional.of(encounter(10L, "外科")));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> patientEncounterService.getEncounterDetail(10L));

        // 与真实"就诊未找到"完全一致（同 code 同 message），不暴露存在性
        assertEquals(ErrorCode.ENCOUNTER_NOT_FOUND.getCode(), ex.getCode());
        assertEquals(ErrorCode.ENCOUNTER_NOT_FOUND.getMessage(), ex.getMessage());
        verifyNoInteractions(diagnosisRepository, labTestRepository);
    }

    @Test
    void getEncounterDetail_deptScope_nullDeptId_sameAsNotFound() {
        mockUser(DataScope.DEPT, null);
        when(encounterRepository.findByIdAndIsDeletedFalse(10L))
                .thenReturn(Optional.of(encounter(10L, DEPT_NAME)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> patientEncounterService.getEncounterDetail(10L));

        assertEquals(ErrorCode.ENCOUNTER_NOT_FOUND.getCode(), ex.getCode());
        verifyNoInteractions(institutionRepository, diagnosisRepository);
    }

    @Test
    void getEncounterDetail_deptScope_institutionMissing_sameAsNotFound() {
        mockUser(DataScope.DEPT, DEPT_ID);
        lenient().when(institutionRepository.findById(DEPT_ID)).thenReturn(Optional.empty());
        when(encounterRepository.findByIdAndIsDeletedFalse(10L))
                .thenReturn(Optional.of(encounter(10L, DEPT_NAME)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> patientEncounterService.getEncounterDetail(10L));

        assertEquals(ErrorCode.ENCOUNTER_NOT_FOUND.getCode(), ex.getCode());
        verifyNoInteractions(diagnosisRepository);
    }

    @Test
    void getEncounterDetail_deptScope_nullDepartment_sameAsNotFound() {
        mockUser(DataScope.DEPT, DEPT_ID);
        mockInstitution(DEPT_NAME);
        // department 为 null 不计为匹配 → 拒绝
        when(encounterRepository.findByIdAndIsDeletedFalse(10L))
                .thenReturn(Optional.of(encounter(10L, null)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> patientEncounterService.getEncounterDetail(10L));

        assertEquals(ErrorCode.ENCOUNTER_NOT_FOUND.getCode(), ex.getCode());
        verifyNoInteractions(diagnosisRepository);
    }

    @Test
    void getEncounterDetail_allScope_noFilter() {
        mockUser(DataScope.ALL, DEPT_ID);
        when(encounterRepository.findByIdAndIsDeletedFalse(10L))
                .thenReturn(Optional.of(encounter(10L, "外科")));

        EncounterDetailDTO dto = patientEncounterService.getEncounterDetail(10L);

        assertNotNull(dto);
        verifyNoInteractions(institutionRepository);
    }

    @Test
    void getEncounterDetail_noUserContext_skipsFilter() {
        // 无 X-User-Id（内部调用）：数据范围过滤不生效，认证由网关/切面负责
        when(encounterRepository.findByIdAndIsDeletedFalse(10L))
                .thenReturn(Optional.of(encounter(10L, "外科")));

        EncounterDetailDTO dto = patientEncounterService.getEncounterDetail(10L);

        assertNotNull(dto);
        verifyNoInteractions(permissionStore, institutionRepository);
    }

    // ==================== getPatientEncounterList ====================

    @Test
    void getPatientEncounterList_deptScope_allEncountersInDept_returnsList() {
        mockUser(DataScope.DEPT, DEPT_ID);
        mockInstitution(DEPT_NAME);
        PatientEntity patient = new PatientEntity();
        patient.setId(1L);
        patient.setName("张三");
        when(patientRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(patient));
        when(encounterRepository.findByPatientIdAndIsDeletedFalseOrderByAdmissionTimeDesc(1L))
                .thenReturn(List.of(encounter(10L, DEPT_NAME), encounter(11L, DEPT_NAME)));
        Pageable pageable = PageRequest.of(0, 10);
        when(encounterRepository.findByPatientId(1L, pageable)).thenReturn(Page.empty());

        PatientEncounterListDTO dto = patientEncounterService.getPatientEncounterList(1L, pageable);

        assertNotNull(dto);
        assertEquals(1L, dto.getPatientId());
        assertEquals("张**", dto.getPatientName());
    }

    @Test
    void getPatientEncounterList_deptScope_mixedEncounters_visible() {
        mockUser(DataScope.DEPT, DEPT_ID);
        mockInstitution(DEPT_NAME);
        PatientEntity patient = new PatientEntity();
        patient.setId(1L);
        patient.setName("张三");
        when(patientRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(patient));
        // ∃ 语义：[本科室, 其他科室] 混合就诊 → 可见（转科患者不消失）
        when(encounterRepository.findByPatientIdAndIsDeletedFalseOrderByAdmissionTimeDesc(1L))
                .thenReturn(List.of(encounter(10L, DEPT_NAME), encounter(11L, "外科")));
        Pageable pageable = PageRequest.of(0, 10);
        when(encounterRepository.findByPatientId(1L, pageable)).thenReturn(Page.empty());

        PatientEncounterListDTO dto = patientEncounterService.getPatientEncounterList(1L, pageable);

        assertNotNull(dto);
        assertEquals(1L, dto.getPatientId());
        assertEquals("张**", dto.getPatientName());
        verify(encounterRepository).findByPatientId(1L, pageable);
    }

    @Test
    void getPatientEncounterList_deptScope_noEncounterInDept_sameAsNotFound() {
        mockUser(DataScope.DEPT, DEPT_ID);
        mockInstitution(DEPT_NAME);
        PatientEntity patient = new PatientEntity();
        patient.setId(1L);
        patient.setName("张三");
        when(patientRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(patient));
        // 一条就诊都不在用户科室 → 拒绝
        when(encounterRepository.findByPatientIdAndIsDeletedFalseOrderByAdmissionTimeDesc(1L))
                .thenReturn(List.of(encounter(10L, "外科"), encounter(11L, "儿科")));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> patientEncounterService.getPatientEncounterList(1L, PageRequest.of(0, 10)));

        // 与真实"患者未找到"完全一致（同 code 同 message），不暴露存在性
        assertEquals(ErrorCode.PATIENT_NOT_FOUND.getCode(), ex.getCode());
        assertEquals(ErrorCode.PATIENT_NOT_FOUND.getMessage(), ex.getMessage());
        verify(encounterRepository, never()).findByPatientId(anyLong(), any(Pageable.class));
    }

    @Test
    void getPatientEncounterList_deptScope_noEncounters_sameAsNotFound() {
        mockUser(DataScope.DEPT, DEPT_ID);
        mockInstitution(DEPT_NAME);
        PatientEntity patient = new PatientEntity();
        patient.setId(1L);
        patient.setName("张三");
        when(patientRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(patient));
        // 零就诊记录：无科室证据，fail-closed → 拒绝
        when(encounterRepository.findByPatientIdAndIsDeletedFalseOrderByAdmissionTimeDesc(1L))
                .thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> patientEncounterService.getPatientEncounterList(1L, PageRequest.of(0, 10)));

        assertEquals(ErrorCode.PATIENT_NOT_FOUND.getCode(), ex.getCode());
        assertEquals(ErrorCode.PATIENT_NOT_FOUND.getMessage(), ex.getMessage());
        verify(encounterRepository, never()).findByPatientId(anyLong(), any(Pageable.class));
    }
}
