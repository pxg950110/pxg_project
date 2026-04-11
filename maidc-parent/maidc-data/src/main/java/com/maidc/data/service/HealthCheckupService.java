package com.maidc.data.service;

import com.maidc.data.entity.HealthCheckupEntity;
import com.maidc.data.repository.HealthCheckupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckupService {

    private final HealthCheckupRepository healthCheckupRepository;

    public HealthCheckupEntity getHealthCheckup(Long id) {
        return healthCheckupRepository.findById(id).orElse(null);
    }

    public Page<HealthCheckupEntity> listHealthCheckups(int page, int size) {
        return healthCheckupRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public HealthCheckupEntity createHealthCheckup(HealthCheckupEntity entity) {
        return healthCheckupRepository.save(entity);
    }

    @Transactional
    public void deleteHealthCheckup(Long id) {
        healthCheckupRepository.deleteById(id);
    }
}
