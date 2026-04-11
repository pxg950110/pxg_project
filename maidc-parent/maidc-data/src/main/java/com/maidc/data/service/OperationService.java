package com.maidc.data.service;

import com.maidc.data.entity.OperationEntity;
import com.maidc.data.repository.OperationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationService {

    private final OperationRepository operationRepository;

    public OperationEntity getOperation(Long id) {
        return operationRepository.findById(id).orElse(null);
    }

    public Page<OperationEntity> listOperations(int page, int size) {
        return operationRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public OperationEntity createOperation(OperationEntity entity) {
        return operationRepository.save(entity);
    }

    @Transactional
    public void deleteOperation(Long id) {
        operationRepository.deleteById(id);
    }
}
