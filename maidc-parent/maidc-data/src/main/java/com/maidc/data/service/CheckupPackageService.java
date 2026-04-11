package com.maidc.data.service;

import com.maidc.data.entity.CheckupPackageEntity;
import com.maidc.data.repository.CheckupPackageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckupPackageService {

    private final CheckupPackageRepository checkupPackageRepository;

    public CheckupPackageEntity getCheckupPackage(Long id) {
        return checkupPackageRepository.findById(id).orElse(null);
    }

    public Page<CheckupPackageEntity> listCheckupPackages(int page, int size) {
        return checkupPackageRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public CheckupPackageEntity createCheckupPackage(CheckupPackageEntity entity) {
        return checkupPackageRepository.save(entity);
    }

    @Transactional
    public void deleteCheckupPackage(Long id) {
        checkupPackageRepository.deleteById(id);
    }
}
