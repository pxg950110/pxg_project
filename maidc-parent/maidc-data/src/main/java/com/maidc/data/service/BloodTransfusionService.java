package com.maidc.data.service;

import com.maidc.data.entity.BloodTransfusionEntity;
import com.maidc.data.repository.BloodTransfusionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BloodTransfusionService {

    private final BloodTransfusionRepository bloodTransfusionRepository;

    public BloodTransfusionEntity getBloodTransfusion(Long id) {
        return bloodTransfusionRepository.findById(id).orElse(null);
    }

    public Page<BloodTransfusionEntity> listBloodTransfusions(int page, int size) {
        return bloodTransfusionRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public BloodTransfusionEntity createBloodTransfusion(BloodTransfusionEntity entity) {
        return bloodTransfusionRepository.save(entity);
    }

    @Transactional
    public void deleteBloodTransfusion(Long id) {
        bloodTransfusionRepository.deleteById(id);
    }
}
