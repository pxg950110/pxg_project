package com.maidc.data.service;

import com.maidc.data.entity.LabPanelEntity;
import com.maidc.data.repository.LabPanelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LabPanelService {

    private final LabPanelRepository labPanelRepository;

    public LabPanelEntity getLabPanel(Long id) {
        return labPanelRepository.findById(id).orElse(null);
    }

    public Page<LabPanelEntity> listLabPanels(Long testId, int page, int size) {
        return labPanelRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public LabPanelEntity createLabPanel(LabPanelEntity entity) {
        return labPanelRepository.save(entity);
    }

    @Transactional
    public void deleteLabPanel(Long id) {
        labPanelRepository.deleteById(id);
    }
}
