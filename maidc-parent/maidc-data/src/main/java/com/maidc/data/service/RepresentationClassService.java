package com.maidc.data.service;

import com.maidc.data.entity.RepresentationClassEntity;
import com.maidc.data.repository.RepresentationClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RepresentationClassService {

    private final RepresentationClassRepository repository;

    public List<RepresentationClassEntity> listActive() {
        return repository.findByIsDeletedFalseAndIsActiveTrueOrderBySortOrderAscIdAsc();
    }
}
