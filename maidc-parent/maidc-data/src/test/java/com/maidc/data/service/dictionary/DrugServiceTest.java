package com.maidc.data.service.dictionary;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.dictionary.DrugCategoryEntity;
import com.maidc.data.repository.dictionary.DrugCategoryRepository;
import com.maidc.data.repository.dictionary.DrugRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DrugService 分类管理：树构建（含子分类挂载）、删除守卫与层级计算。
 */
@ExtendWith(MockitoExtension.class)
class DrugServiceTest {

    @Mock
    private DrugRepository drugRepository;

    @Mock
    private DrugCategoryRepository categoryRepository;

    @InjectMocks
    private DrugService service;

    @Test
    void getCategoryTree_attachesChildren() {
        DrugCategoryEntity root = new DrugCategoryEntity();
        root.setId(1L);
        root.setCode("ATC-A");
        root.setName("消化系统及代谢药");
        DrugCategoryEntity child = new DrugCategoryEntity();
        child.setId(2L);
        child.setCode("ATC-A01");
        child.setParentId(1L);
        when(categoryRepository.findAllOrderBySortOrder()).thenReturn(List.of(root, child));

        List<DrugCategoryEntity> tree = service.getCategoryTree();

        assertEquals(1, tree.size());
        assertEquals(1, tree.get(0).getChildren().size());
        assertEquals("ATC-A01", tree.get(0).getChildren().get(0).getCode());
    }

    @Test
    void createCategory_underParent_inheritsLevelPlusOne() {
        DrugCategoryEntity parent = new DrugCategoryEntity();
        parent.setId(1L);
        parent.setLevel(2);
        DrugCategoryEntity entity = new DrugCategoryEntity();
        entity.setCode("ATC-A01");
        entity.setParentId(1L);
        when(categoryRepository.existsByCodeAndIsDeletedFalse("ATC-A01")).thenReturn(false);
        when(categoryRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(parent));
        when(categoryRepository.save(any(DrugCategoryEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        DrugCategoryEntity saved = service.createCategory(entity);

        assertEquals(3, saved.getLevel());
    }

    @Test
    void deleteCategory_hasDrugs_throws400() {
        DrugCategoryEntity entity = new DrugCategoryEntity();
        entity.setId(1L);
        when(categoryRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(entity));
        when(categoryRepository.findByParentId(1L)).thenReturn(List.of());
        when(drugRepository.countByCategory(1L)).thenReturn(3L);

        assertThrows(BusinessException.class, () -> service.deleteCategory(1L));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void deleteCategory_noChildrenNoDrugs_logicalDelete() {
        DrugCategoryEntity entity = new DrugCategoryEntity();
        entity.setId(1L);
        when(categoryRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(entity));
        when(categoryRepository.findByParentId(1L)).thenReturn(List.of());
        when(drugRepository.countByCategory(1L)).thenReturn(0L);

        service.deleteCategory(1L);

        assertTrue(entity.getIsDeleted());
        verify(categoryRepository).save(entity);
        verify(categoryRepository, never()).delete(any(DrugCategoryEntity.class));
    }
}
