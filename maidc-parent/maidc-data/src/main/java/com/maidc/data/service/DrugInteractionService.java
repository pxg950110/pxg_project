package com.maidc.data.service;

import com.maidc.data.entity.DrugInteractionEntity;
import com.maidc.data.repository.DrugInteractionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrugInteractionService {

    private final DrugInteractionRepository drugInteractionRepository;

    public List<DrugInteractionEntity> list(Long drug1, Long drug2, String severity) {
        if (drug1 != null && drug2 != null) {
            List<DrugInteractionEntity> results = drugInteractionRepository.findBetween(drug1, drug2);
            if (severity != null && !severity.isBlank()) {
                results = results.stream()
                        .filter(d -> severity.equals(d.getSeverity()))
                        .toList();
            }
            return results;
        }
        return drugInteractionRepository.findAllByIsDeletedFalse();
    }

    public List<DrugInteractionEntity> checkPair(Long drug1, Long drug2) {
        return drugInteractionRepository.findBetween(drug1, drug2);
    }

    public List<DrugInteractionEntity> checkList(List<Long> drugIds) {
        if (drugIds == null || drugIds.size() < 2) {
            return Collections.emptyList();
        }
        return drugInteractionRepository.findInPairSet(drugIds);
    }

    @Transactional
    public DrugInteractionEntity create(DrugInteractionEntity entity) {
        // Normalize: ensure drugConceptId1 < drugConceptId2
        if (entity.getDrugConceptId1() > entity.getDrugConceptId2()) {
            Long tmp = entity.getDrugConceptId1();
            entity.setDrugConceptId1(entity.getDrugConceptId2());
            entity.setDrugConceptId2(tmp);
        }
        if (entity.getOrgId() == null) entity.setOrgId(0L);
        DrugInteractionEntity saved = drugInteractionRepository.save(entity);
        log.info("药物相互作用创建成功: id={}, drug1={}, drug2={}",
                saved.getId(), saved.getDrugConceptId1(), saved.getDrugConceptId2());
        return saved;
    }
}
