package com.maidc.data.service;

import com.maidc.data.entity.ImagingFindingEntity;
import com.maidc.data.repository.ImagingFindingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImagingFindingService {

    private final ImagingFindingRepository imagingFindingRepository;

    public ImagingFindingEntity getImagingFinding(Long id) {
        return imagingFindingRepository.findById(id).orElse(null);
    }

    public Page<ImagingFindingEntity> listImagingFindings(int page, int size) {
        return imagingFindingRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public ImagingFindingEntity createImagingFinding(ImagingFindingEntity entity) {
        return imagingFindingRepository.save(entity);
    }

    @Transactional
    public void deleteImagingFinding(Long id) {
        imagingFindingRepository.deleteById(id);
    }
}
