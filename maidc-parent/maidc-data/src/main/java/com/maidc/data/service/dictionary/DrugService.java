package com.maidc.data.service.dictionary;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.dictionary.DrugCategoryEntity;
import com.maidc.data.entity.dictionary.DrugEntity;
import com.maidc.data.repository.dictionary.DrugCategoryRepository;
import com.maidc.data.repository.dictionary.DrugRepository;
import com.maidc.data.repository.dictionary.DictionaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrugService extends AbstractDictionaryService<DrugEntity> {

    private final DrugRepository drugRepository;
    private final DrugCategoryRepository categoryRepository;

    @Override
    protected DictionaryRepository<DrugEntity> repository() {
        return drugRepository;
    }

    @Override
    protected String entityName() {
        return "药品";
    }

    @Override
    protected String codeProperty() {
        return "drugCode";
    }

    @Override
    protected List<String> searchProperties() {
        return List.of("drugCode", "name", "tradeName", "namePinyin");
    }

    @Override
    protected Sort defaultSort() {
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    public Page<DrugEntity> list(Long categoryId, String status, String insuranceType, String keyword, int page, int size) {
        return list(allOf(
            eqIfPresent("categoryId", categoryId),
            eqIfPresent("status", status),
            eqIfPresent("insuranceType", insuranceType)), keyword, page, size);
    }

    public DrugEntity getByCode(String code) {
        return drugRepository.findByDrugCodeAndIsDeletedFalse(code)
            .orElseThrow(() -> new BusinessException(404, "药品不存在: " + code));
    }

    public List<DrugEntity> getByAtcCode(String atcCode) {
        return drugRepository.findByAtcCode(atcCode);
    }

    // ==================== 分类管理 ====================

    public List<DrugCategoryEntity> getCategoryTree() {
        return buildTree(categoryRepository.findAllOrderBySortOrder(), null);
    }

    public List<DrugCategoryEntity> getRootCategories() {
        return categoryRepository.findRootCategories();
    }

    public List<DrugCategoryEntity> getChildCategories(Long parentId) {
        return categoryRepository.findByParentId(parentId);
    }

    @Transactional
    public DrugCategoryEntity createCategory(DrugCategoryEntity entity) {
        if (categoryRepository.existsByCodeAndIsDeletedFalse(entity.getCode())) {
            throw new BusinessException(400, "分类编码已存在: " + entity.getCode());
        }

        if (entity.getParentId() != null) {
            DrugCategoryEntity parent = categoryRepository.findByIdAndIsDeletedFalse(entity.getParentId())
                .orElseThrow(() -> new BusinessException(404, "父分类不存在"));
            entity.setLevel(parent.getLevel() + 1);
        } else {
            entity.setLevel(1);
        }

        if (entity.getOrgId() == null) entity.setOrgId(0L);
        if (entity.getStatus() == null) entity.setStatus("ACTIVE");

        DrugCategoryEntity saved = categoryRepository.save(entity);
        log.info("药品分类创建成功: id={}, code={}", saved.getId(), saved.getCode());
        return saved;
    }

    @Transactional
    public void deleteCategory(Long id) {
        DrugCategoryEntity entity = categoryRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new BusinessException(404, "分类不存在"));

        if (!categoryRepository.findByParentId(id).isEmpty()) {
            throw new BusinessException(400, "该分类下有子分类，不能删除");
        }

        if (drugRepository.countByCategory(id) > 0) {
            throw new BusinessException(400, "该分类下有药品，不能删除");
        }

        entity.setIsDeleted(true);
        categoryRepository.save(entity);
        log.info("药品分类已删除(逻辑): id={}", id);
    }

    private List<DrugCategoryEntity> buildTree(List<DrugCategoryEntity> all, Long parentId) {
        List<DrugCategoryEntity> tree = new ArrayList<>();
        for (DrugCategoryEntity entity : all) {
            if ((parentId == null && entity.getParentId() == null) ||
                (parentId != null && parentId.equals(entity.getParentId()))) {
                entity.setChildren(buildTree(all, entity.getId()));
                tree.add(entity);
            }
        }
        return tree;
    }
}
