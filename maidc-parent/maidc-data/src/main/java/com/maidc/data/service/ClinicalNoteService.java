package com.maidc.data.service;

import com.maidc.data.entity.ClinicalNoteEntity;
import com.maidc.data.repository.ClinicalNoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClinicalNoteService {

    private final ClinicalNoteRepository clinicalNoteRepository;

    public ClinicalNoteEntity getClinicalNote(Long id) {
        return clinicalNoteRepository.findById(id).orElse(null);
    }

    public Page<ClinicalNoteEntity> listClinicalNotes(int page, int size) {
        return clinicalNoteRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public ClinicalNoteEntity createClinicalNote(ClinicalNoteEntity entity) {
        return clinicalNoteRepository.save(entity);
    }

    @Transactional
    public void deleteClinicalNote(Long id) {
        clinicalNoteRepository.deleteById(id);
    }
}
