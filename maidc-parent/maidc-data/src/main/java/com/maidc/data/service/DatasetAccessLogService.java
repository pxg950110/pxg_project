package com.maidc.data.service;

import com.maidc.data.entity.DatasetAccessLogEntity;
import com.maidc.data.repository.DatasetAccessLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatasetAccessLogService {

    private final DatasetAccessLogRepository datasetAccessLogRepository;

    public DatasetAccessLogEntity getAccessLog(Long id) {
        return datasetAccessLogRepository.findById(id).orElse(null);
    }

    public Page<DatasetAccessLogEntity> listAccessLogs(int page, int size) {
        return datasetAccessLogRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public DatasetAccessLogEntity createAccessLog(DatasetAccessLogEntity entity) {
        return datasetAccessLogRepository.save(entity);
    }

    @Transactional
    public void deleteAccessLog(Long id) {
        datasetAccessLogRepository.deleteById(id);
    }
}
