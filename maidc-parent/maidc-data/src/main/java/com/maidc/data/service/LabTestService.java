package com.maidc.data.service;

import com.maidc.data.entity.LabTestEntity;
import com.maidc.data.repository.LabTestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LabTestService {

    private final LabTestRepository labTestRepository;

    public LabTestEntity getLabTest(Long id) {
        return labTestRepository.findById(id).orElse(null);
    }

    public Page<LabTestEntity> listLabTests(Long encounterId, int page, int size) {
        return labTestRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public LabTestEntity createLabTest(LabTestEntity entity) {
        return labTestRepository.save(entity);
    }

    @Transactional
    public void deleteLabTest(Long id) {
        labTestRepository.deleteById(id);
    }
}
