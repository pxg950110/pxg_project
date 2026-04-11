package com.maidc.data.service;

import com.maidc.data.entity.GenomicDatasetEntity;
import com.maidc.data.repository.GenomicDatasetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenomicDatasetService {

    private final GenomicDatasetRepository genomicDatasetRepository;

    public GenomicDatasetEntity getGenomicDataset(Long id) {
        return genomicDatasetRepository.findById(id).orElse(null);
    }

    public Page<GenomicDatasetEntity> listGenomicDatasets(int page, int size) {
        return genomicDatasetRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public GenomicDatasetEntity createGenomicDataset(GenomicDatasetEntity entity) {
        return genomicDatasetRepository.save(entity);
    }

    @Transactional
    public void deleteGenomicDataset(Long id) {
        genomicDatasetRepository.deleteById(id);
    }
}
