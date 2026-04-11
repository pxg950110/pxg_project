package com.maidc.data.service;

import com.maidc.data.entity.OrgEntity;
import com.maidc.data.repository.OrgRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrgService {

    private final OrgRepository orgRepository;

    public OrgEntity getOrg(Long id) {
        return orgRepository.findById(id).orElse(null);
    }

    public Page<OrgEntity> listOrgs(int page, int size) {
        return orgRepository.findAll(PageRequest.of(page - 1, size));
    }

    @Transactional
    public OrgEntity createOrg(OrgEntity entity) {
        return orgRepository.save(entity);
    }

    @Transactional
    public void deleteOrg(Long id) {
        orgRepository.deleteById(id);
    }
}
