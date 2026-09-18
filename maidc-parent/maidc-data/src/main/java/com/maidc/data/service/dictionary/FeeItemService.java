package com.maidc.data.service.dictionary;

import com.maidc.data.entity.dictionary.FeeItemEntity;
import com.maidc.data.repository.dictionary.DictionaryRepository;
import com.maidc.data.repository.dictionary.FeeItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeeItemService extends AbstractDictionaryService<FeeItemEntity> {

    private final FeeItemRepository repository;

    @Override
    protected DictionaryRepository<FeeItemEntity> repository() {
        return repository;
    }

    @Override
    protected String entityName() {
        return "收费项目";
    }

    @Override
    protected String codeProperty() {
        return "feeCode";
    }

    @Override
    protected List<String> searchProperties() {
        return List.of("feeCode", "name", "namePinyin");
    }

    @Override
    protected Sort defaultSort() {
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    public Page<FeeItemEntity> list(String feeCategory, String feeType, String keyword, int page, int size) {
        return list(allOf(
            eqIfPresent("feeCategory", feeCategory),
            eqIfPresent("feeType", feeType)), keyword, page, size);
    }

    public List<String> getAllCategories() {
        return repository.findAllCategories();
    }
}
