package com.maidc.data.service;

import com.maidc.data.entity.ImagingExamEntity;
import com.maidc.data.repository.ImagingExamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImagingExamService {

    private final ImagingExamRepository imagingExamRepository;

    public ImagingExamEntity getImagingExam(Long id) {
        return imagingExamRepository.findById(id).orElse(null);
    }

    public Page<ImagingExamEntity> listImagingExams(int page, int size) {
        return imagingExamRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public ImagingExamEntity createImagingExam(ImagingExamEntity entity) {
        return imagingExamRepository.save(entity);
    }

    @Transactional
    public void deleteImagingExam(Long id) {
        imagingExamRepository.deleteById(id);
    }
}
