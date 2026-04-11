package com.maidc.data.service;

import com.maidc.data.entity.NursingRecordEntity;
import com.maidc.data.repository.NursingRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NursingRecordService {

    private final NursingRecordRepository nursingRecordRepository;

    public NursingRecordEntity getNursingRecord(Long id) {
        return nursingRecordRepository.findById(id).orElse(null);
    }

    public Page<NursingRecordEntity> listNursingRecords(int page, int size) {
        return nursingRecordRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public NursingRecordEntity createNursingRecord(NursingRecordEntity entity) {
        return nursingRecordRepository.save(entity);
    }

    @Transactional
    public void deleteNursingRecord(Long id) {
        nursingRecordRepository.deleteById(id);
    }
}
