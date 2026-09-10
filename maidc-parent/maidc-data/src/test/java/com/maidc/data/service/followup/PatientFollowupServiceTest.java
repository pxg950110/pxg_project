package com.maidc.data.service.followup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.FollowupProtocolEntity;
import com.maidc.data.entity.FollowupTaskEntity;
import com.maidc.data.entity.PatientFollowupEntity;
import com.maidc.data.repository.DiseaseCohortPatientRepository;
import com.maidc.data.repository.FollowupTaskRepository;
import com.maidc.data.repository.PatientFollowupRepository;
import com.maidc.data.repository.ScaleDefinitionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/** 建档 / 任务生成 / 状态机 / 升级方案 语义单测 */
@ExtendWith(MockitoExtension.class)
class PatientFollowupServiceTest {

    @Mock private PatientFollowupRepository followupRepository;
    @Mock private FollowupTaskRepository taskRepository;
    @Mock private DiseaseCohortPatientRepository cohortPatientRepository;
    @Mock private ScaleDefinitionRepository scaleRepository;
    @Mock private FollowupProtocolService protocolService;
    @Mock private com.maidc.data.service.followup.DeptScopeService deptScopeService;
    @Mock private com.maidc.data.repository.EncounterRepository encounterRepository;
    @Mock private com.maidc.data.service.followup.FollowupDisplayNameResolver displayNameResolver;
    @Spy  private ObjectMapper objectMapper = new ObjectMapper();

    @org.junit.jupiter.api.BeforeEach
    void setUpScope() {
        // 默认无 DEPT 过滤（SELF/ALL），DEPT 语义由 DeptScopeService 契约测试覆盖
        org.mockito.Mockito.lenient()
                .when(deptScopeService.deptFilterName(org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Optional.empty());
    }

    @InjectMocks private PatientFollowupService service;

    private static final String STAGES = """
        [{"stageCode":"BASELINE","name":"基线","offsetDays":0,"requiredScales":["SNOT22"],"optionalScales":[]},
         {"stageCode":"M3","name":"3个月随访","offsetDays":90,"requiredScales":["SNOT22"],"optionalScales":[]},
         {"stageCode":"M6","name":"6个月随访","offsetDays":180,"requiredScales":["SNOT22"],"optionalScales":[]},
         {"stageCode":"M12","name":"12个月随访","offsetDays":365,"requiredScales":["SNOT22"],"optionalScales":[]}]
        """;

    private FollowupProtocolEntity protocol(int version) {
        FollowupProtocolEntity p = new FollowupProtocolEntity();
        p.setId(10L);
        p.setCohortId(1L);
        p.setVersion(version);
        p.setStatus("PUBLISHED");
        p.setStages(STAGES);
        return p;
    }

    private void stubProtocol(int version) {
        when(protocolService.latestPublished(1L)).thenReturn(protocol(version));
        lenient().when(scaleRepository.findFirstByScaleCodeAndStatusAndIsDeletedFalseOrderByVersionDesc("SNOT22", "ACTIVE"))
                .thenReturn(Optional.of(new com.maidc.data.entity.ScaleDefinitionEntity()));
    }

    @Test
    void enroll_patient_not_in_cohort_404() {
        when(cohortPatientRepository.existsByCohortIdAndPatientId(1L, 9001L)).thenReturn(false);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.enroll(1L, Map.of("patientId", 9001, "doctorId", 5)));
        assertTrue(ex.getMessage().contains("不在该专病队列"));
    }

    @Test
    void enroll_duplicate_active_followup_409() {
        when(cohortPatientRepository.existsByCohortIdAndPatientId(1L, 9001L)).thenReturn(true);
        when(followupRepository.findByCohortIdAndPatientIdAndStatusAndIsDeletedFalse(1L, 9001L, "ACTIVE"))
                .thenReturn(Optional.of(new PatientFollowupEntity()));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.enroll(1L, Map.of("patientId", 9001, "doctorId", 5)));
        assertTrue(ex.getMessage().contains("已有进行中"));
    }

    @Test
    void enroll_generates_stage_tasks_with_offset_due_dates() {
        when(cohortPatientRepository.existsByCohortIdAndPatientId(1L, 9001L)).thenReturn(true);
        when(followupRepository.findByCohortIdAndPatientIdAndStatusAndIsDeletedFalse(1L, 9001L, "ACTIVE"))
                .thenReturn(Optional.empty());
        stubProtocol(1);
        when(followupRepository.save(any())).thenAnswer(inv -> {
            PatientFollowupEntity f = inv.getArgument(0);
            f.setId(101L);
            return f;
        });
        when(taskRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = service.enroll(1L, Map.of(
                "patientId", 9001, "doctorId", 5, "nurseId", 8, "enrollDate", "2026-09-01"));
        assertEquals(4, ((Number) result.get("taskCount")).intValue());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FollowupTaskEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(taskRepository).saveAll(captor.capture());
        List<FollowupTaskEntity> tasks = captor.getValue();
        assertEquals(LocalDate.parse("2026-09-01"), tasks.get(0).getDueDate());  // BASELINE 0 天
        assertEquals(LocalDate.parse("2026-11-30"), tasks.get(1).getDueDate());  // M3 90 天
        assertEquals(LocalDate.parse("2027-09-01"), tasks.get(3).getDueDate());  // M12 365 天
        assertEquals("PENDING", tasks.get(0).getStatus());
        assertTrue(tasks.get(0).getRequiredScales().contains("SNOT22"));
    }

    @Test
    void close_requires_reason() {
        PatientFollowupEntity f = new PatientFollowupEntity();
        f.setStatus("ACTIVE");
        lenient().when(followupRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(f));
        assertThrows(BusinessException.class, () -> service.close(1L, " "));
    }

    @Test
    void suspend_resume_state_machine_guards() {
        PatientFollowupEntity suspended = new PatientFollowupEntity();
        suspended.setStatus("SUSPENDED");
        when(followupRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(suspended));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.suspend(1L));
        assertTrue(ex.getMessage().contains("仅进行中的档案可暂停"));

        when(followupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertEquals("ACTIVE", service.resume(1L).getStatus());
    }

    @Test
    void upgrade_protocol_already_latest_400() {
        PatientFollowupEntity f = new PatientFollowupEntity();
        f.setId(1L); f.setCohortId(1L); f.setStatus("ACTIVE"); f.setProtocolVersion(2);
        when(followupRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(f));
        stubProtocol(2);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.upgradeProtocol(1L));
        assertTrue(ex.getMessage().contains("最新方案版本"));
    }

    @Test
    void upgrade_protocol_rebuilds_only_future_pending_tasks() {
        PatientFollowupEntity f = new PatientFollowupEntity();
        f.setId(1L); f.setCohortId(1L); f.setStatus("ACTIVE");
        f.setProtocolVersion(1); f.setEnrollDate(LocalDate.now().minusMonths(6));
        when(followupRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(f));
        stubProtocol(2);

        FollowupTaskEntity futurePending = new FollowupTaskEntity();
        futurePending.setId(20L); futurePending.setStatus("PENDING");
        futurePending.setStageCode("M3"); // 仅该阶段需重建（如 SKIPPED 的 BASELINE 不复活）
        futurePending.setDueDate(LocalDate.now().plusDays(30));
        when(taskRepository.findByFollowupIdAndDueDateGreaterThanEqualAndIsDeletedFalse(anyLong(), any(LocalDate.class)))
                .thenReturn(List.of(futurePending));
        when(taskRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(followupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = service.upgradeProtocol(1L);
        assertEquals(1, ((Number) result.get("removedTasks")).intValue());
        assertEquals(1, ((Number) result.get("rebuiltTasks")).intValue()); // 只重建 M3 对应阶段
        verify(taskRepository).delete(futurePending);
    }
}
