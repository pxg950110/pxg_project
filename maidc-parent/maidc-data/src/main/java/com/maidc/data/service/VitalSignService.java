package com.maidc.data.service;

import com.maidc.data.entity.VitalSignEntity;
import com.maidc.data.repository.VitalSignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VitalSignService {

    private final VitalSignRepository vitalSignRepository;

    public VitalSignEntity getVitalSign(Long id) {
        return vitalSignRepository.findById(id).orElse(null);
    }

    public Page<VitalSignEntity> listVitalSigns(Long encounterId, int page, int size) {
        return vitalSignRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public VitalSignEntity createVitalSign(VitalSignEntity entity) {
        return vitalSignRepository.save(entity);
    }

    @Transactional
    public void deleteVitalSign(Long id) {
        vitalSignRepository.deleteById(id);
    }
}
