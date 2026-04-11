package com.maidc.data.service;

import com.maidc.data.entity.FeatureDictionaryEntity;
import com.maidc.data.repository.FeatureDictionaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureDictionaryService {

    private final FeatureDictionaryRepository featureDictionaryRepository;

    public FeatureDictionaryEntity getDictionary(Long id) {
        return featureDictionaryRepository.findById(id).orElse(null);
    }

    public Page<FeatureDictionaryEntity> listDictionaries(int page, int size) {
        return featureDictionaryRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public FeatureDictionaryEntity createDictionary(FeatureDictionaryEntity entity) {
        return featureDictionaryRepository.save(entity);
    }

    @Transactional
    public void deleteDictionary(Long id) {
        featureDictionaryRepository.deleteById(id);
    }
}
