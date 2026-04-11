package com.maidc.data.service;

import com.maidc.data.entity.ImagingDatasetEntity;
import com.maidc.data.repository.ImagingDatasetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImagingDatasetService {

    private final ImagingDatasetRepository imagingDatasetRepository;

    public ImagingDatasetEntity getImagingDataset(Long id) {
        return imagingDatasetRepository.findById(id).orElse(null);
    }

    public Page<ImagingDatasetEntity> listImagingDatasets(int page, int size) {
        return imagingDatasetRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public ImagingDatasetEntity createImagingDataset(ImagingDatasetEntity entity) {
        return imagingDatasetRepository.save(entity);
    }

    @Transactional
    public void deleteImagingDataset(Long id) {
        imagingDatasetRepository.deleteById(id);
    }
}
