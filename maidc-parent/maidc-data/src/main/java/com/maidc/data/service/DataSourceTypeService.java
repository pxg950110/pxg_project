package com.maidc.data.service;

import com.maidc.data.entity.DataSourceTypeEntity;
import com.maidc.data.repository.DataSourceTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceTypeService {

    private final DataSourceTypeRepository dataSourceTypeRepository;

    public List<DataSourceTypeEntity> listAll() {
        return dataSourceTypeRepository.findAll();
    }

    public DataSourceTypeEntity getByTypeCode(String typeCode) {
        return dataSourceTypeRepository.findByTypeCode(typeCode).orElse(null);
    }

    @Transactional
    public DataSourceTypeEntity create(DataSourceTypeEntity entity) {
        return dataSourceTypeRepository.save(entity);
    }

    @Transactional
    public DataSourceTypeEntity update(String typeCode, DataSourceTypeEntity entity) {
        DataSourceTypeEntity existing = dataSourceTypeRepository.findByTypeCode(typeCode).orElse(null);
        if (existing == null) return null;
        entity.setId(existing.getId());
        entity.setCreatedBy(existing.getCreatedBy());
        entity.setCreatedAt(existing.getCreatedAt());
        return dataSourceTypeRepository.save(entity);
    }

    @Transactional
    public boolean delete(String typeCode) {
        DataSourceTypeEntity existing = dataSourceTypeRepository.findByTypeCode(typeCode).orElse(null);
        if (existing == null) return false;
        if (Boolean.TRUE.equals(existing.getIsBuiltin())) {
            throw new IllegalStateException("内置类型不可删除: " + typeCode);
        }
        dataSourceTypeRepository.deleteById(existing.getId());
        return true;
    }
}
