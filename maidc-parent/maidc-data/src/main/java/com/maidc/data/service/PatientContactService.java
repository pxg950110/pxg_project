package com.maidc.data.service;

import com.maidc.data.entity.PatientContactEntity;
import com.maidc.data.repository.PatientContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientContactService {

    private final PatientContactRepository patientContactRepository;

    public PatientContactEntity getPatientContact(Long id) {
        return patientContactRepository.findById(id).orElse(null);
    }

    public Page<PatientContactEntity> listPatientContacts(int page, int size) {
        return patientContactRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public PatientContactEntity createPatientContact(PatientContactEntity entity) {
        return patientContactRepository.save(entity);
    }

    @Transactional
    public void deletePatientContact(Long id) {
        patientContactRepository.deleteById(id);
    }
}
