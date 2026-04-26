package com.maidc.data.service;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.dto.DataElementCreateDTO;
import com.maidc.data.entity.DataElementEntity;
import com.maidc.data.repository.DataElementMappingRepository;
import com.maidc.data.repository.DataElementRepository;
import com.maidc.data.repository.DataElementValueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataElementServiceTest {

    @Mock
    private DataElementRepository dataElementRepository;
    @Mock
    private DataElementValueRepository valueRepository;
    @Mock
    private DataElementMappingRepository mappingRepository;

    @InjectMocks
    private DataElementService service;

    @Test
    void getById_existingId_returnsEntity() {
        DataElementEntity entity = new DataElementEntity();
        entity.setId(1L);
        entity.setElementCode("CV04.50.005");
        entity.setName("姓名");
        when(dataElementRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(entity));

        DataElementEntity result = service.getById(1L);
        assertNotNull(result);
        assertEquals("CV04.50.005", result.getElementCode());
        assertEquals("姓名", result.getName());
    }

    @Test
    void getById_nonExistingId_throws() {
        when(dataElementRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.getById(999L));
    }

    @Test
    void create_withValidDTO_savesAndReturns() {
        DataElementCreateDTO dto = DataElementCreateDTO.builder()
                .elementCode("CV04.50.005").name("姓名").definition("在报告中受检者姓名")
                .dataType("ST").category("人口学").build();
        when(dataElementRepository.existsByElementCodeAndIsDeletedFalse("CV04.50.005")).thenReturn(false);
        when(dataElementRepository.save(any(DataElementEntity.class))).thenAnswer(inv -> {
            DataElementEntity e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        DataElementEntity result = service.create(dto);
        assertNotNull(result);
        assertEquals("CV04.50.005", result.getElementCode());
        assertEquals("DRAFT", result.getRegistrationStatus());
        verify(dataElementRepository).save(any());
    }

    @Test
    void create_withDuplicateCode_throws() {
        DataElementCreateDTO dto = DataElementCreateDTO.builder()
                .elementCode("CV04.50.005").name("姓名").definition("test").dataType("ST").build();
        when(dataElementRepository.existsByElementCodeAndIsDeletedFalse("CV04.50.005")).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.create(dto));
    }

    @Test
    void delete_existingId_softDeletes() {
        DataElementEntity entity = new DataElementEntity();
        entity.setId(1L);
        entity.setElementCode("CV04.50.005");
        when(dataElementRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(entity));

        service.delete(1L);
        assertEquals("RETIRED", entity.getStatus());
        verify(dataElementRepository).save(entity);
        verify(dataElementRepository).delete(entity);
    }

    @Test
    void list_withKeyword_returnsFilteredPage() {
        DataElementEntity entity = new DataElementEntity();
        entity.setId(1L);
        entity.setName("姓名");
        Page<DataElementEntity> page = new PageImpl<>(List.of(entity));
        when(dataElementRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<DataElementEntity> result = service.list(null, null, "姓名", null, 1, 20);
        assertEquals(1, result.getTotalElements());
    }
}
