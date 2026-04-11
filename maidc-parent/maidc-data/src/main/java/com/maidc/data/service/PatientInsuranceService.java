package com.maidc.data.service;

import com.maidc.data.entity.PatientInsuranceEntity;
import com.maidc.data.repository.PatientInsuranceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientInsuranceService {

    private final PatientInsuranceRepository patientInsuranceRepository;

    public PatientInsuranceEntity getPatientInsurance(Long id) {
        return patientInsuranceRepository.findById(id).orElse(null);
    }

    public Page<PatientInsuranceEntity> listPatientInsurances(int page, int size) {
        return patientInsuranceRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public PatientInsuranceEntity createPatientInsurance(PatientInsuranceEntity entity) {
        return patientInsuranceRepository.save(entity);
    }

    @Transactional
    public void deletePatientInsurance(Long id) {
        patientInsuranceRepository.deleteById(id);
    }
}
