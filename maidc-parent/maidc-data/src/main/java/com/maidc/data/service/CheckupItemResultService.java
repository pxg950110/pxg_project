package com.maidc.data.service;

import com.maidc.data.entity.CheckupItemResultEntity;
import com.maidc.data.repository.CheckupItemResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckupItemResultService {

    private final CheckupItemResultRepository checkupItemResultRepository;

    public CheckupItemResultEntity getCheckupItemResult(Long id) {
        return checkupItemResultRepository.findById(id).orElse(null);
    }

    public Page<CheckupItemResultEntity> listCheckupItemResults(int page, int size) {
        return checkupItemResultRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public CheckupItemResultEntity createCheckupItemResult(CheckupItemResultEntity entity) {
        return checkupItemResultRepository.save(entity);
    }

    @Transactional
    public void deleteCheckupItemResult(Long id) {
        checkupItemResultRepository.deleteById(id);
    }
}
