package com.maidc.data.service;

import com.maidc.data.entity.CheckupSummaryEntity;
import com.maidc.data.repository.CheckupSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckupSummaryService {

    private final CheckupSummaryRepository checkupSummaryRepository;

    public CheckupSummaryEntity getCheckupSummary(Long id) {
        return checkupSummaryRepository.findById(id).orElse(null);
    }

    public Page<CheckupSummaryEntity> listCheckupSummaries(int page, int size) {
        return checkupSummaryRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public CheckupSummaryEntity createCheckupSummary(CheckupSummaryEntity entity) {
        return checkupSummaryRepository.save(entity);
    }

    @Transactional
    public void deleteCheckupSummary(Long id) {
        checkupSummaryRepository.deleteById(id);
    }
}
