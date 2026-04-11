package com.maidc.data.service;

import com.maidc.data.entity.MedicationEntity;
import com.maidc.data.repository.MedicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicationService {

    private final MedicationRepository medicationRepository;

    public MedicationEntity getMedication(Long id) {
        return medicationRepository.findById(id).orElse(null);
    }

    public Page<MedicationEntity> listMedications(Long encounterId, int page, int size) {
        return medicationRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public MedicationEntity createMedication(MedicationEntity entity) {
        return medicationRepository.save(entity);
    }

    @Transactional
    public void deleteMedication(Long id) {
        medicationRepository.deleteById(id);
    }
}
