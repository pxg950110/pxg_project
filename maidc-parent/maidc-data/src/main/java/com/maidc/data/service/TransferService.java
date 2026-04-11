package com.maidc.data.service;

import com.maidc.data.entity.TransferEntity;
import com.maidc.data.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferRepository transferRepository;

    public TransferEntity getTransfer(Long id) {
        return transferRepository.findById(id).orElse(null);
    }

    public Page<TransferEntity> listTransfers(int page, int size) {
        return transferRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public TransferEntity createTransfer(TransferEntity entity) {
        return transferRepository.save(entity);
    }

    @Transactional
    public void deleteTransfer(Long id) {
        transferRepository.deleteById(id);
    }
}
