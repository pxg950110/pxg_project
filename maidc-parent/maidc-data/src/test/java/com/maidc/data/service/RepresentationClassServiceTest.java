package com.maidc.data.service;

import com.maidc.data.entity.RepresentationClassEntity;
import com.maidc.data.repository.RepresentationClassRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 表示类字典目录：仅返回有效（未删且 isActive）条目，按 sortOrder 升序（同序号按 id 稳定排序）
 */
@ExtendWith(MockitoExtension.class)
class RepresentationClassServiceTest {

    @Mock
    private RepresentationClassRepository repository;

    @InjectMocks
    private RepresentationClassService service;

    @Test
    void listActiveReturnsActiveSortedBySortOrder() {
        RepresentationClassEntity code = new RepresentationClassEntity();
        code.setCode("CODE");
        code.setName("代码");
        code.setSortOrder(2);
        RepresentationClassEntity amount = new RepresentationClassEntity();
        amount.setCode("AMOUNT");
        amount.setName("金额");
        amount.setSortOrder(1);
        when(repository.findByIsDeletedFalseAndIsActiveTrueOrderBySortOrderAscIdAsc())
                .thenReturn(List.of(amount, code));

        List<RepresentationClassEntity> result = service.listActive();

        assertEquals(2, result.size());
        assertEquals("AMOUNT", result.get(0).getCode());
        assertEquals("CODE", result.get(1).getCode());
        verify(repository).findByIsDeletedFalseAndIsActiveTrueOrderBySortOrderAscIdAsc();
    }
}
