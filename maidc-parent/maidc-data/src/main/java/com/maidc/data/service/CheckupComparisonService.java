package com.maidc.data.service;

import com.maidc.data.entity.CheckupComparisonEntity;
import com.maidc.data.repository.CheckupComparisonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckupComparisonService {

    private final CheckupComparisonRepository checkupComparisonRepository;

    public CheckupComparisonEntity getCheckupComparison(Long id) {
        return checkupComparisonRepository.findById(id).orElse(null);
    }

    public Page<CheckupComparisonEntity> listCheckupComparisons(int page, int size) {
        return checkupComparisonRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public CheckupComparisonEntity createCheckupComparison(CheckupComparisonEntity entity) {
        return checkupComparisonRepository.save(entity);
    }

    @Transactional
    public void deleteCheckupComparison(Long id) {
        checkupComparisonRepository.deleteById(id);
    }
}
