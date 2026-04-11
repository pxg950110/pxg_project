package com.maidc.data.service;

import com.maidc.data.entity.ClinicalFeatureEntity;
import com.maidc.data.repository.ClinicalFeatureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClinicalFeatureService {

    private final ClinicalFeatureRepository clinicalFeatureRepository;

    public ClinicalFeatureEntity getFeature(Long id) {
        return clinicalFeatureRepository.findById(id).orElse(null);
    }

    public Page<ClinicalFeatureEntity> listFeatures(int page, int size) {
        return clinicalFeatureRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public ClinicalFeatureEntity createFeature(ClinicalFeatureEntity entity) {
        return clinicalFeatureRepository.save(entity);
    }

    @Transactional
    public void deleteFeature(Long id) {
        clinicalFeatureRepository.deleteById(id);
    }
}
