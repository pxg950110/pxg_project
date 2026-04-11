package com.maidc.data.service;

import com.maidc.data.entity.AllergyEntity;
import com.maidc.data.repository.AllergyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AllergyService {

    private final AllergyRepository allergyRepository;

    public AllergyEntity getAllergy(Long id) {
        return allergyRepository.findById(id).orElse(null);
    }

    public Page<AllergyEntity> listAllergies(int page, int size) {
        return allergyRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public AllergyEntity createAllergy(AllergyEntity entity) {
        return allergyRepository.save(entity);
    }

    @Transactional
    public void deleteAllergy(Long id) {
        allergyRepository.deleteById(id);
    }
}
