package com.maidc.data.service;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.ClinicalNoteEntity;
import com.maidc.data.repository.ClinicalNoteRepository;
import com.maidc.data.repository.ClinicalNoteSpecification;
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
        return clinicalNoteRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Clinical note not found: " + id));
    }

    public Page<ClinicalNoteEntity> listClinicalNotes(int page, int size) {
        return clinicalNoteRepository.findAll(PageRequest.of(page - 1, size));
    }

    public Page<ClinicalNoteEntity> listByEncounter(Long encounterId, int page, int size) {
        return clinicalNoteRepository.findByEncounterId(encounterId, PageRequest.of(page - 1, size));
    }

    public Page<ClinicalNoteEntity> searchNotes(
            Long encounterId, Long patientId, String noteType,
            String noteCategory, String signStatus, String urgency,
            String source, String keyword, int page, int size) {
        return clinicalNoteRepository.findAll(
                ClinicalNoteSpecification.buildSearchSpec(
                        encounterId, patientId, noteType, noteCategory,
                        signStatus, urgency, source, keyword),
                PageRequest.of(page - 1, size));
    }

    @Transactional
    public ClinicalNoteEntity createClinicalNote(ClinicalNoteEntity entity) {
        if (entity.getOrgId() == null) entity.setOrgId(0L);
        ClinicalNoteEntity saved = clinicalNoteRepository.save(entity);
        log.info("Clinical note created: id={}, type={}", saved.getId(), saved.getNoteType());
        return saved;
    }

    @Transactional
    public ClinicalNoteEntity updateClinicalNote(Long id, ClinicalNoteEntity updates) {
        ClinicalNoteEntity entity = getClinicalNote(id);
        if (updates.getTitle() != null) entity.setTitle(updates.getTitle());
        if (updates.getContent() != null) entity.setContent(updates.getContent());
        if (updates.getStructuredData() != null) entity.setStructuredData(updates.getStructuredData());
        if (updates.getNoteCategory() != null) entity.setNoteCategory(updates.getNoteCategory());
        if (updates.getUrgency() != null) entity.setUrgency(updates.getUrgency());
        return clinicalNoteRepository.save(entity);
    }

    @Transactional
    public void deleteClinicalNote(Long id) {
        clinicalNoteRepository.deleteById(id);
        log.info("Clinical note deleted (logical): id={}", id);
    }

    @Transactional
    public ClinicalNoteEntity signNote(Long id, String signedBy) {
        ClinicalNoteEntity entity = getClinicalNote(id);
        entity.setSignStatus("SIGNED");
        entity.setSignedBy(signedBy);
        entity.setSignedAt(java.time.LocalDateTime.now());
        log.info("Clinical note signed: id={}, signedBy={}", id, signedBy);
        return clinicalNoteRepository.save(entity);
    }

    @Transactional
    public ClinicalNoteEntity countersignNote(Long id, String signedBy) {
        ClinicalNoteEntity entity = getClinicalNote(id);
        entity.setSignStatus("COUNTERSIGNED");
        entity.setSignedBy(signedBy);
        entity.setSignedAt(java.time.LocalDateTime.now());
        log.info("Clinical note countersigned: id={}, signedBy={}", id, signedBy);
        return clinicalNoteRepository.save(entity);
    }
}
