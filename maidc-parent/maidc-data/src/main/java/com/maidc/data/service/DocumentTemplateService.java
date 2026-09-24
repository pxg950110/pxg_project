package com.maidc.data.service;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.DocumentTemplateEntity;
import com.maidc.data.repository.DocumentTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentTemplateService {

    private final DocumentTemplateRepository documentTemplateRepository;

    public DocumentTemplateEntity getTemplate(Long id) {
        return documentTemplateRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Document template not found: " + id));
    }

    public DocumentTemplateEntity getTemplateByCode(String code) {
        return documentTemplateRepository.findByCodeAndIsDeletedFalse(code).orElse(null);
    }

    public Page<DocumentTemplateEntity> listTemplates(String noteType, int page, int size) {
        if (noteType != null && !noteType.isBlank()) {
            return documentTemplateRepository.findByNoteTypeAndIsDeletedFalse(noteType, PageRequest.of(page - 1, size));
        }
        return documentTemplateRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public DocumentTemplateEntity createTemplate(DocumentTemplateEntity entity) {
        if (documentTemplateRepository.existsByCodeAndIsDeletedFalse(entity.getCode())) {
            throw new BusinessException(400, "Template code already exists: " + entity.getCode());
        }
        if (entity.getOrgId() == null) entity.setOrgId(0L);
        DocumentTemplateEntity saved = documentTemplateRepository.save(entity);
        log.info("Document template created: id={}, code={}", saved.getId(), saved.getCode());
        return saved;
    }

    @Transactional
    public DocumentTemplateEntity updateTemplate(Long id, DocumentTemplateEntity updates) {
        DocumentTemplateEntity entity = getTemplate(id);
        if (updates.getName() != null) entity.setName(updates.getName());
        if (updates.getDescription() != null) entity.setDescription(updates.getDescription());
        if (updates.getSchemaDef() != null) entity.setSchemaDef(updates.getSchemaDef());
        if (updates.getNoteType() != null) entity.setNoteType(updates.getNoteType());
        if (updates.getStatus() != null) entity.setStatus(updates.getStatus());
        return documentTemplateRepository.save(entity);
    }

    @Transactional
    public void deleteTemplate(Long id) {
        documentTemplateRepository.deleteById(id);
        log.info("Document template deleted (logical): id={}", id);
    }
}
