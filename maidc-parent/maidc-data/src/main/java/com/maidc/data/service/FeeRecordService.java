package com.maidc.data.service;

import com.maidc.data.entity.FeeRecordEntity;
import com.maidc.data.repository.FeeRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeeRecordService {

    private final FeeRecordRepository feeRecordRepository;

    public FeeRecordEntity getFeeRecord(Long id) {
        return feeRecordRepository.findById(id).orElse(null);
    }

    public Page<FeeRecordEntity> listFeeRecords(int page, int size) {
        return feeRecordRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public FeeRecordEntity createFeeRecord(FeeRecordEntity entity) {
        return feeRecordRepository.save(entity);
    }

    @Transactional
    public void deleteFeeRecord(Long id) {
        feeRecordRepository.deleteById(id);
    }
}
