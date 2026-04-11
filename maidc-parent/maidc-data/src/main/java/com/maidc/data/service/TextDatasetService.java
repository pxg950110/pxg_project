package com.maidc.data.service;

import com.maidc.data.entity.TextDatasetEntity;
import com.maidc.data.repository.TextDatasetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TextDatasetService {

    private final TextDatasetRepository textDatasetRepository;

    public TextDatasetEntity getTextDataset(Long id) {
        return textDatasetRepository.findById(id).orElse(null);
    }

    public Page<TextDatasetEntity> listTextDatasets(int page, int size) {
        return textDatasetRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public TextDatasetEntity createTextDataset(TextDatasetEntity entity) {
        return textDatasetRepository.save(entity);
    }

    @Transactional
    public void deleteTextDataset(Long id) {
        textDatasetRepository.deleteById(id);
    }
}
