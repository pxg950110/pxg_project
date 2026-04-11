package com.maidc.data.service;

import com.maidc.data.entity.StudySubjectEntity;
import com.maidc.data.repository.StudySubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudySubjectService {

    private final StudySubjectRepository studySubjectRepository;

    public StudySubjectEntity getSubject(Long id) {
        return studySubjectRepository.findById(id).orElse(null);
    }

    public Page<StudySubjectEntity> listSubjects(int page, int size) {
        return studySubjectRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public StudySubjectEntity createSubject(StudySubjectEntity entity) {
        return studySubjectRepository.save(entity);
    }

    @Transactional
    public void deleteSubject(Long id) {
        studySubjectRepository.deleteById(id);
    }
}
