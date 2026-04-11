package com.maidc.data.service;

import com.maidc.data.entity.EtlTaskLogEntity;
import com.maidc.data.repository.EtlTaskLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EtlTaskLogService {

    private final EtlTaskLogRepository etlTaskLogRepository;

    public EtlTaskLogEntity getEtlTaskLog(Long id) {
        return etlTaskLogRepository.findById(id).orElse(null);
    }

    public Page<EtlTaskLogEntity> listEtlTaskLogs(int page, int size) {
        return etlTaskLogRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public EtlTaskLogEntity createEtlTaskLog(EtlTaskLogEntity entity) {
        return etlTaskLogRepository.save(entity);
    }

    @Transactional
    public void deleteEtlTaskLog(Long id) {
        etlTaskLogRepository.deleteById(id);
    }
}
