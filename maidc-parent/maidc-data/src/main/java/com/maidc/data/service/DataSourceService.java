package com.maidc.data.service;

import com.maidc.data.entity.DataSourceEntity;
import com.maidc.data.repository.DataSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceService {

    private final DataSourceRepository dataSourceRepository;

    public DataSourceEntity getDataSource(Long id) {
        return dataSourceRepository.findById(id).orElse(null);
    }

    public Page<DataSourceEntity> listDataSources(int page, int size) {
        return dataSourceRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public DataSourceEntity createDataSource(DataSourceEntity entity) {
        return dataSourceRepository.save(entity);
    }

    @Transactional
    public DataSourceEntity updateDataSource(Long id, DataSourceEntity entity) {
        DataSourceEntity existing = dataSourceRepository.findById(id).orElse(null);
        if (existing == null) return null;
        entity.setId(id);
        entity.setCreatedBy(existing.getCreatedBy());
        entity.setCreatedAt(existing.getCreatedAt());
        return dataSourceRepository.save(entity);
    }

    @Transactional
    public void deleteDataSource(Long id) {
        dataSourceRepository.deleteById(id);
    }
}
