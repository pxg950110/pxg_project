package com.maidc.data.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.DictEntity;
import com.maidc.data.entity.DictTypeEntity;
import com.maidc.data.repository.DictRepository;
import com.maidc.data.repository.DictTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 数据字典服务：字典类型（分组）与字典项的 CRUD。
 * 类型存储于 system.s_dict_type，条目存储于 system.s_dict（dict_type = 类型编码）。
 */
@Service
@RequiredArgsConstructor
public class DictService {

    private final DictTypeRepository dictTypeRepository;
    private final DictRepository dictRepository;

    // ---------- 字典类型 ----------

    public Page<DictTypeEntity> listTypes(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size,
                Sort.by(Sort.Direction.ASC, "typeCode"));
        if (keyword != null && !keyword.isBlank()) {
            return dictTypeRepository.searchByKeyword(keyword, pageable);
        }
        return dictTypeRepository.findByIsDeletedFalse(pageable);
    }

    public DictTypeEntity getType(Long id) {
        return dictTypeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    @Transactional
    public DictTypeEntity createType(DictTypeEntity entity) {
        if (entity.getTypeCode() == null || entity.getTypeCode().isBlank()) {
            throw new BusinessException(400, "类型编码不能为空");
        }
        if (entity.getTypeName() == null || entity.getTypeName().isBlank()) {
            throw new BusinessException(400, "类型名称不能为空");
        }
        if (dictTypeRepository.existsByTypeCodeAndIsDeletedFalse(entity.getTypeCode())) {
            throw new BusinessException(400, "类型编码已存在: " + entity.getTypeCode());
        }
        if (entity.getOrgId() == null) entity.setOrgId(0L);
        if (entity.getCreatedBy() == null || entity.getCreatedBy().isBlank()) entity.setCreatedBy("system");
        entity.setIsDeleted(false);
        return dictTypeRepository.save(entity);
    }

    /** 类型编码不可变，仅允许修改名称与备注 */
    @Transactional
    public DictTypeEntity updateType(Long id, DictTypeEntity updates) {
        DictTypeEntity entity = getType(id);
        if (updates.getTypeName() != null && !updates.getTypeName().isBlank()) {
            entity.setTypeName(updates.getTypeName());
        }
        if (updates.getRemark() != null) {
            entity.setRemark(updates.getRemark());
        }
        return dictTypeRepository.save(entity);
    }

    /** 删除字典类型（逻辑删除）并级联逻辑删除其全部条目 */
    @Transactional
    public void deleteType(Long id) {
        DictTypeEntity entity = getType(id);
        entity.setIsDeleted(true);
        dictTypeRepository.save(entity);
        dictRepository.findByDictTypeAndIsDeletedFalseOrderBySortOrder(entity.getTypeCode())
                .forEach(item -> {
                    item.setIsDeleted(true);
                    dictRepository.save(item);
                });
    }

    // ---------- 字典项 ----------

    public List<DictEntity> listItems(Long typeId, String keyword) {
        DictTypeEntity type = getType(typeId);
        List<DictEntity> items =
                dictRepository.findByDictTypeAndIsDeletedFalseOrderBySortOrder(type.getTypeCode());
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            return items.stream()
                    .filter(i -> (i.getDictLabel() != null && i.getDictLabel().toLowerCase().contains(kw))
                            || (i.getDictCode() != null && i.getDictCode().toLowerCase().contains(kw)))
                    .toList();
        }
        return items;
    }

    @Transactional
    public DictEntity createItem(Long typeId, DictEntity item) {
        DictTypeEntity type = getType(typeId);
        if (item.getDictCode() == null || item.getDictCode().isBlank()) {
            throw new BusinessException(400, "字典编码不能为空");
        }
        if (item.getDictLabel() == null || item.getDictLabel().isBlank()) {
            throw new BusinessException(400, "字典名称不能为空");
        }
        boolean duplicated = dictRepository
                .findByDictTypeAndIsDeletedFalseOrderBySortOrder(type.getTypeCode()).stream()
                .anyMatch(i -> item.getDictCode().equals(i.getDictCode()));
        if (duplicated) {
            throw new BusinessException(400, "该类型下字典编码已存在: " + item.getDictCode());
        }
        item.setDictType(type.getTypeCode());
        if (item.getOrgId() == null) item.setOrgId(0L);
        if (item.getCreatedBy() == null || item.getCreatedBy().isBlank()) item.setCreatedBy("system");
        if (item.getCreatedAt() == null) item.setCreatedAt(java.time.LocalDateTime.now());
        item.setIsDeleted(false);
        return dictRepository.save(item);
    }

    @Transactional
    public DictEntity updateItem(Long id, DictEntity updates) {
        DictEntity item = dictRepository.findById(id)
                .filter(i -> !Boolean.TRUE.equals(i.getIsDeleted()))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (updates.getDictLabel() != null) {
            item.setDictLabel(updates.getDictLabel());
        }
        if (updates.getDictValue() != null) {
            item.setDictValue(updates.getDictValue());
        }
        if (updates.getSortOrder() != null) {
            item.setSortOrder(updates.getSortOrder());
        }
        if (updates.getIsEnabled() != null) {
            item.setIsEnabled(updates.getIsEnabled());
        }
        if (updates.getParentCode() != null) {
            item.setParentCode(updates.getParentCode());
        }
        if (updates.getRemark() != null) {
            item.setRemark(updates.getRemark());
        }
        item.setUpdatedAt(java.time.LocalDateTime.now());
        return dictRepository.save(item);
    }

    @Transactional
    public void deleteItem(Long id) {
        DictEntity item = dictRepository.findById(id)
                .filter(i -> !Boolean.TRUE.equals(i.getIsDeleted()))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        item.setIsDeleted(true);
        dictRepository.save(item);
    }
}
