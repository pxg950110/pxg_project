package com.maidc.data.service;

import com.maidc.data.entity.PathologyEntity;
import com.maidc.data.repository.PathologyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PathologyService {

    private final PathologyRepository pathologyRepository;

    public PathologyEntity getPathology(Long id) {
        return pathologyRepository.findById(id).orElse(null);
    }

    public Page<PathologyEntity> listPathologies(int page, int size) {
        return pathologyRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public PathologyEntity createPathology(PathologyEntity entity) {
        return pathologyRepository.save(entity);
    }

    @Transactional
    public void deletePathology(Long id) {
        pathologyRepository.deleteById(id);
    }
}
