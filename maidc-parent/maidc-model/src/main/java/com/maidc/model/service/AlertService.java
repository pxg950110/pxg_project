package com.maidc.model.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.core.result.PageResult;
import com.maidc.model.dto.AlertRuleCreateDTO;
import com.maidc.model.entity.AlertRecordEntity;
import com.maidc.model.entity.AlertRuleEntity;
import com.maidc.model.mapper.ModelMapper;
import com.maidc.model.repository.AlertRecordRepository;
import com.maidc.model.repository.AlertRuleRepository;
import com.maidc.model.vo.AlertRecordVO;
import com.maidc.model.vo.AlertRuleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertRecordRepository alertRecordRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public AlertRuleVO createAlertRule(AlertRuleCreateDTO dto) {
        AlertRuleEntity rule = new AlertRuleEntity();
        rule.setRuleName(dto.getRuleName());
        rule.setRuleType(dto.getRuleType());
        rule.setTargetType(dto.getTargetType());
        rule.setTargetId(dto.getTargetId());
        rule.setConditionExpr(dto.getConditionExpr());
        rule.setSeverity(dto.getSeverity());
        rule.setNotifyChannels(dto.getNotifyChannels());
        rule.setEnabled(true);

        rule = alertRuleRepository.save(rule);
        log.info("告警规则创建: id={}, name={}", rule.getId(), dto.getRuleName());
        return modelMapper.toAlertRuleVO(rule);
    }

    public List<AlertRuleVO> listAlertRules(Long deploymentId) {
        List<AlertRuleEntity> rules;
        if (deploymentId != null) {
            rules = alertRuleRepository.findByTargetIdAndIsDeletedFalse(deploymentId);
        } else {
            rules = alertRuleRepository.findByIsDeletedFalse();
        }
        return rules.stream().map(modelMapper::toAlertRuleVO).toList();
    }

    @Transactional
    public AlertRuleVO updateAlertRule(Long id, AlertRuleCreateDTO dto) {
        AlertRuleEntity rule = alertRuleRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (dto.getRuleName() != null) rule.setRuleName(dto.getRuleName());
        if (dto.getConditionExpr() != null) rule.setConditionExpr(dto.getConditionExpr());
        if (dto.getSeverity() != null) rule.setSeverity(dto.getSeverity());
        if (dto.getNotifyChannels() != null) rule.setNotifyChannels(dto.getNotifyChannels());

        rule = alertRuleRepository.save(rule);
        return modelMapper.toAlertRuleVO(rule);
    }

    public PageResult<AlertRecordVO> listAlerts(String status, int page, int pageSize) {
        Page<AlertRecordEntity> result = alertRecordRepository
                .findByStatusOrderByTriggeredAtDesc(status, PageRequest.of(page - 1, pageSize));
        return PageResult.of(result.map(modelMapper::toAlertRecordVO));
    }

    @Transactional
    public AlertRecordVO acknowledgeAlert(Long id, Long userId) {
        AlertRecordEntity record = alertRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        record.setStatus("ACKNOWLEDGED");
        record.setAcknowledgedBy(userId);
        record.setAcknowledgedAt(LocalDateTime.now());
        record = alertRecordRepository.save(record);
        log.info("告警已确认: id={}", id);
        return modelMapper.toAlertRecordVO(record);
    }

    @Transactional
    public AlertRuleVO toggleAlertRule(Long id) {
        AlertRuleEntity rule = alertRuleRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        rule.setEnabled(!rule.getEnabled());
        rule = alertRuleRepository.save(rule);
        log.info("告警规则状态切换: id={}, enabled={}", id, rule.getEnabled());
        return modelMapper.toAlertRuleVO(rule);
    }

    public PageResult<AlertRecordVO> getAlertHistory(Long ruleId, int page, int pageSize) {
        Page<AlertRecordEntity> result = alertRecordRepository
                .findByRuleIdOrderByTriggeredAtDesc(ruleId, PageRequest.of(page - 1, pageSize));
        return PageResult.of(result.map(modelMapper::toAlertRecordVO));
    }
}
