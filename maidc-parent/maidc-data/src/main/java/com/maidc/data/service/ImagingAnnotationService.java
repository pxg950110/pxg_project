package com.maidc.data.service;

import com.maidc.data.entity.ImagingAnnotationEntity;
import com.maidc.data.repository.ImagingAnnotationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImagingAnnotationService {

    private final ImagingAnnotationRepository imagingAnnotationRepository;

    public ImagingAnnotationEntity getAnnotation(Long id) {
        return imagingAnnotationRepository.findById(id).orElse(null);
    }

    public Page<ImagingAnnotationEntity> listAnnotations(int page, int size) {
        return imagingAnnotationRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public ImagingAnnotationEntity createAnnotation(ImagingAnnotationEntity entity) {
        return imagingAnnotationRepository.save(entity);
    }

    @Transactional
    public void deleteAnnotation(Long id) {
        imagingAnnotationRepository.deleteById(id);
    }
}
