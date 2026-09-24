package com.maidc.data.service.dictionary;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.dictionary.LabItemEntity;
import com.maidc.data.repository.dictionary.DictionaryRepository;
import com.maidc.data.repository.dictionary.LabItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LabItemService extends AbstractDictionaryService<LabItemEntity> {

    private final LabItemRepository repository;

    @Override
    protected DictionaryRepository<LabItemEntity> repository() {
        return repository;
    }

    @Override
    protected String entityName() {
        return "检验项目";
    }

    @Override
    protected String codeProperty() {
        return "labCode";
    }

    @Override
    protected List<String> searchProperties() {
        return List.of("labCode", "name", "loincCode", "namePinyin");
    }

    @Override
    protected Sort defaultSort() {
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    public Page<LabItemEntity> list(String labCategory, String specimenType, String keyword, int page, int size) {
        return list(allOf(
            eqIfPresent("labCategory", labCategory),
            eqIfPresent("specimenTypeCode", specimenType)), keyword, page, size);
    }

    public LabItemEntity getByLoincCode(String loincCode) {
        return repository.findByLoincCode(loincCode)
            .orElseThrow(() -> new BusinessException(404, "检验项目不存在: " + loincCode));
    }

    public List<String> getAllCategories() {
        return repository.findAllCategories();
    }

    public List<LabItemEntity> getChildren(Long parentId) {
        return repository.findByParentId(parentId);
    }
}
