package com.maidc.data.service;

import com.maidc.data.entity.PatientBedEntity;
import com.maidc.data.repository.PatientBedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientBedService {

    private final PatientBedRepository patientBedRepository;

    public PatientBedEntity getPatientBed(Long id) {
        return patientBedRepository.findById(id).orElse(null);
    }

    public Page<PatientBedEntity> listPatientBeds(int page, int size) {
        return patientBedRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public PatientBedEntity createPatientBed(PatientBedEntity entity) {
        return patientBedRepository.save(entity);
    }

    @Transactional
    public void deletePatientBed(Long id) {
        patientBedRepository.deleteById(id);
    }
}
