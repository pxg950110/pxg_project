package com.maidc.data.service.dictionary;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.dictionary.DiagnosisEntity;
import com.maidc.data.repository.dictionary.DiagnosisDictRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

/**
 * 经 DiagnosisService 具体子类验证 AbstractDictionaryService 模板行为：
 * 编码查重、非空合并更新、逻辑删除与检索校验。
 */
@ExtendWith(MockitoExtension.class)
class DiagnosisServiceTest {

    @Mock
    private DiagnosisDictRepository repository;

    @InjectMocks
    private DiagnosisService service;

    // ==================== 查询 ====================

    @Test
    void getById_existingId_returnsEntity() {
        DiagnosisEntity entity = new DiagnosisEntity();
        entity.setId(1L);
        entity.setDiagnosisCode("DIAG-001");
        when(repository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(entity));

        assertEquals("DIAG-001", service.getById(1L).getDiagnosisCode());
    }

    @Test
    void getById_missing_throws404() {
        when(repository.findByIdAndIsDeletedFalse(404L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getById(404L));
        assertTrue(ex.getMessage().contains("诊断不存在"));
    }

    @Test
    void list_withFilters_returnsPage() {
        when(repository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(Page.empty());

        Page<DiagnosisEntity> result = service.list("A00-B99", "ACTIVE", null, 1, 20);

        assertNotNull(result);
        verify(repository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    void list_withoutFilters_returnsPage() {
        when(repository.findAll(nullable(Specification.class), any(PageRequest.class))).thenReturn(Page.empty());

        assertDoesNotThrow(() -> service.list(null, null, null, 1, 20));
    }

    @Test
    void search_blankKeyword_throws400() {
        assertThrows(BusinessException.class, () -> service.search("  ", 1, 20));
        verify(repository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    // ==================== 创建 ====================

    @Test
    void create_blankCode_throws400() {
        DiagnosisEntity entity = new DiagnosisEntity();

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(entity));
        assertTrue(ex.getMessage().contains("编码不能为空"));
        verify(repository, never()).save(any());
    }

    @Test
    void create_duplicateCode_throws400() {
        DiagnosisEntity entity = new DiagnosisEntity();
        entity.setDiagnosisCode("DIAG-001");
        entity.setName("肺炎");
        when(repository.count(any(Specification.class))).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(entity));
        assertTrue(ex.getMessage().contains("编码已存在"));
        verify(repository, never()).save(any());
    }

    @Test
    void create_newEntity_appliesDefaults() {
        DiagnosisEntity entity = new DiagnosisEntity();
        entity.setDiagnosisCode("DIAG-002");
        entity.setName("肺炎");
        when(repository.count(any(Specification.class))).thenReturn(0L);
        when(repository.save(any(DiagnosisEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        DiagnosisEntity saved = service.create(entity);

        assertEquals(0L, saved.getOrgId());
        assertEquals("ACTIVE", saved.getStatus());
        assertFalse(saved.getIsDeleted());
    }

    // ==================== 更新 ====================

    @Test
    void update_mergesNonNullFields_keepsCodeImmutable() {
        DiagnosisEntity existing = new DiagnosisEntity();
        existing.setId(1L);
        existing.setDiagnosisCode("DIAG-001");
        existing.setName("肺炎");
        existing.setIcd10Code("J18.9");

        DiagnosisEntity updates = new DiagnosisEntity();
        updates.setName("社区获得性肺炎");
        updates.setDiagnosisCode("DIAG-HACK");

        when(repository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(DiagnosisEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        DiagnosisEntity saved = service.update(1L, updates);

        assertEquals("社区获得性肺炎", saved.getName());
        assertEquals("DIAG-001", saved.getDiagnosisCode());
        assertEquals("J18.9", saved.getIcd10Code());
    }

    // ==================== 删除 ====================

    @Test
    void delete_existingId_logicalDelete_neverPhysicalRemove() {
        DiagnosisEntity existing = new DiagnosisEntity();
        existing.setId(1L);
        existing.setDiagnosisCode("DIAG-001");
        when(repository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(existing));

        service.delete(1L);

        assertTrue(existing.getIsDeleted());
        verify(repository).save(existing);
        verify(repository, never()).delete(any(DiagnosisEntity.class));
    }
}
