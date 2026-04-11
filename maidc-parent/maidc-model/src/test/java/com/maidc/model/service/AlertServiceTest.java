package com.maidc.model.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.model.entity.AlertRuleEntity;
import com.maidc.model.mapper.ModelMapper;
import com.maidc.model.repository.AlertRecordRepository;
import com.maidc.model.repository.AlertRuleRepository;
import com.maidc.model.vo.AlertRuleVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock private AlertRuleRepository alertRuleRepository;
    @Mock private AlertRecordRepository alertRecordRepository;
    @Mock private ModelMapper modelMapper;
    @InjectMocks private AlertService alertService;

    @Test
    void toggleAlertRule_disabledRule_enablesIt() {
        AlertRuleEntity rule = new AlertRuleEntity();
        rule.setId(1L);
        rule.setEnabled(false);

        when(alertRuleRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(rule));
        when(alertRuleRepository.save(any(AlertRuleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelMapper.toAlertRuleVO(any(AlertRuleEntity.class))).thenReturn(new AlertRuleVO());

        alertService.toggleAlertRule(1L);

        verify(alertRuleRepository).save(argThat(saved -> saved.getEnabled()));
    }

    @Test
    void toggleAlertRule_enabledRule_disablesIt() {
        AlertRuleEntity rule = new AlertRuleEntity();
        rule.setId(2L);
        rule.setEnabled(true);

        when(alertRuleRepository.findByIdAndIsDeletedFalse(2L)).thenReturn(Optional.of(rule));
        when(alertRuleRepository.save(any(AlertRuleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelMapper.toAlertRuleVO(any(AlertRuleEntity.class))).thenReturn(new AlertRuleVO());

        alertService.toggleAlertRule(2L);

        verify(alertRuleRepository).save(argThat(saved -> !saved.getEnabled()));
    }

    @Test
    void toggleAlertRule_nonExistentRule_throws() {
        when(alertRuleRepository.findByIdAndIsDeletedFalse(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                alertService.toggleAlertRule(99L));

        assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
        verify(alertRuleRepository, never()).save(any());
    }
}
