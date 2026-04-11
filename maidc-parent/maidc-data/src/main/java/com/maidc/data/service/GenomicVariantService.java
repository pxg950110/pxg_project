package com.maidc.data.service;

import com.maidc.data.entity.GenomicVariantEntity;
import com.maidc.data.repository.GenomicVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenomicVariantService {

    private final GenomicVariantRepository genomicVariantRepository;

    public GenomicVariantEntity getGenomicVariant(Long id) {
        return genomicVariantRepository.findById(id).orElse(null);
    }

    public Page<GenomicVariantEntity> listGenomicVariants(int page, int size) {
        return genomicVariantRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public GenomicVariantEntity createGenomicVariant(GenomicVariantEntity entity) {
        return genomicVariantRepository.save(entity);
    }

    @Transactional
    public void deleteGenomicVariant(Long id) {
        genomicVariantRepository.deleteById(id);
    }
}
