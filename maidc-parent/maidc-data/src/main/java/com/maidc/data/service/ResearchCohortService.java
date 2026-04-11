package com.maidc.data.service;

import com.maidc.data.entity.ResearchCohortEntity;
import com.maidc.data.repository.ResearchCohortRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResearchCohortService {

    private final ResearchCohortRepository researchCohortRepository;

    public ResearchCohortEntity getCohort(Long id) {
        return researchCohortRepository.findById(id).orElse(null);
    }

    public Page<ResearchCohortEntity> listCohorts(int page, int size) {
        return researchCohortRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public ResearchCohortEntity createCohort(ResearchCohortEntity entity) {
        return researchCohortRepository.save(entity);
    }

    @Transactional
    public void deleteCohort(Long id) {
        researchCohortRepository.deleteById(id);
    }
}
