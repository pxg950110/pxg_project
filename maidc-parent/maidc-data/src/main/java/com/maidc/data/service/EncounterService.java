package com.maidc.data.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.EncounterEntity;
import com.maidc.data.mapper.DataMapper;
import com.maidc.data.repository.EncounterRepository;
import com.maidc.data.vo.EncounterVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EncounterService {

    private final EncounterRepository encounterRepository;
    private final DataMapper dataMapper;

    public EncounterVO getEncounter(Long id) {
        EncounterEntity entity = encounterRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return dataMapper.toEncounterVO(entity);
    }

    public List<EncounterVO> findByPatientId(Long patientId) {
        List<EncounterEntity> encounters = encounterRepository
                .findByPatientIdAndIsDeletedFalseOrderByAdmitTimeDesc(patientId);
        return encounters.stream().map(dataMapper::toEncounterVO).toList();
    }

    @Transactional
    public EncounterVO createEncounter(EncounterEntity entity) {
        // encounter_no 为 DDL NOT NULL 列：调用方未提供时自动生成
        if (entity.getEncounterNo() == null || entity.getEncounterNo().isBlank()) {
            entity.setEncounterNo("E" + System.currentTimeMillis());
        }
        entity = encounterRepository.save(entity);
        return dataMapper.toEncounterVO(entity);
    }
}
