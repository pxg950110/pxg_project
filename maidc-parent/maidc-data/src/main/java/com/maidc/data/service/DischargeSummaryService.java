package com.maidc.data.service;

import com.maidc.data.entity.DischargeSummaryEntity;
import com.maidc.data.repository.DischargeSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DischargeSummaryService {

    private final DischargeSummaryRepository dischargeSummaryRepository;

    public DischargeSummaryEntity getDischargeSummary(Long id) {
        return dischargeSummaryRepository.findById(id).orElse(null);
    }

    public Page<DischargeSummaryEntity> listDischargeSummaries(int page, int size) {
        return dischargeSummaryRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public DischargeSummaryEntity createDischargeSummary(DischargeSummaryEntity entity) {
        return dischargeSummaryRepository.save(entity);
    }

    @Transactional
    public void deleteDischargeSummary(Long id) {
        dischargeSummaryRepository.deleteById(id);
    }
}
