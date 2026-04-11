package com.maidc.data.service;

import com.maidc.data.entity.DataQualityResultEntity;
import com.maidc.data.repository.DataQualityResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataQualityResultService {

    private final DataQualityResultRepository dataQualityResultRepository;

    public DataQualityResultEntity getDataQualityResult(Long id) {
        return dataQualityResultRepository.findById(id).orElse(null);
    }

    public Page<DataQualityResultEntity> listDataQualityResults(int page, int size) {
        return dataQualityResultRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public DataQualityResultEntity createDataQualityResult(DataQualityResultEntity entity) {
        return dataQualityResultRepository.save(entity);
    }

    @Transactional
    public void deleteDataQualityResult(Long id) {
        dataQualityResultRepository.deleteById(id);
    }
}
