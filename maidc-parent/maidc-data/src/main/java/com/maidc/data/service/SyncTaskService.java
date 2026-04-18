package com.maidc.data.service;

import com.maidc.data.entity.SyncTaskEntity;
import com.maidc.data.repository.SyncTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncTaskService {

    private final SyncTaskRepository syncTaskRepository;

    public SyncTaskEntity getSyncTask(Long id) {
        return syncTaskRepository.findById(id).orElse(null);
    }

    public Page<SyncTaskEntity> listSyncTasks(int page, int size) {
        return syncTaskRepository.findAll(PageRequest.of(page - 1, size));
    }

    public List<SyncTaskEntity> getSyncTasksBySourceId(Long sourceId) {
        return syncTaskRepository.findBySourceIdOrderByCreatedAtDesc(sourceId);
    }

    @Transactional
    public SyncTaskEntity createSyncTask(SyncTaskEntity entity) {
        return syncTaskRepository.save(entity);
    }

    @Transactional
    public SyncTaskEntity updateSyncTaskStatus(Long id, String status) {
        SyncTaskEntity entity = syncTaskRepository.findById(id).orElse(null);
        if (entity == null) return null;
        entity.setStatus(status);
        return syncTaskRepository.save(entity);
    }
}
