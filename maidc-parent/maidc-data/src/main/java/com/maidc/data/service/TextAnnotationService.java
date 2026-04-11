package com.maidc.data.service;

import com.maidc.data.entity.TextAnnotationEntity;
import com.maidc.data.repository.TextAnnotationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TextAnnotationService {

    private final TextAnnotationRepository textAnnotationRepository;

    public TextAnnotationEntity getTextAnnotation(Long id) {
        return textAnnotationRepository.findById(id).orElse(null);
    }

    public Page<TextAnnotationEntity> listTextAnnotations(int page, int size) {
        return textAnnotationRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public TextAnnotationEntity createTextAnnotation(TextAnnotationEntity entity) {
        return textAnnotationRepository.save(entity);
    }

    @Transactional
    public void deleteTextAnnotation(Long id) {
        textAnnotationRepository.deleteById(id);
    }
}
