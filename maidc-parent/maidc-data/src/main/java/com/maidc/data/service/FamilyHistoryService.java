package com.maidc.data.service;

import com.maidc.data.entity.FamilyHistoryEntity;
import com.maidc.data.repository.FamilyHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FamilyHistoryService {

    private final FamilyHistoryRepository familyHistoryRepository;

    public FamilyHistoryEntity getFamilyHistory(Long id) {
        return familyHistoryRepository.findById(id).orElse(null);
    }

    public Page<FamilyHistoryEntity> listFamilyHistories(int page, int size) {
        return familyHistoryRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public FamilyHistoryEntity createFamilyHistory(FamilyHistoryEntity entity) {
        return familyHistoryRepository.save(entity);
    }

    @Transactional
    public void deleteFamilyHistory(Long id) {
        familyHistoryRepository.deleteById(id);
    }
}
