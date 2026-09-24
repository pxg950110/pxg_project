package com.maidc.data.service.dictionary;

import com.maidc.data.entity.dictionary.ExamItemEntity;
import com.maidc.data.repository.dictionary.DictionaryRepository;
import com.maidc.data.repository.dictionary.ExamItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamItemService extends AbstractDictionaryService<ExamItemEntity> {

    private final ExamItemRepository repository;

    @Override
    protected DictionaryRepository<ExamItemEntity> repository() {
        return repository;
    }

    @Override
    protected String entityName() {
        return "检查项目";
    }

    @Override
    protected String codeProperty() {
        return "examCode";
    }

    @Override
    protected List<String> searchProperties() {
        return List.of("examCode", "name", "namePinyin");
    }

    @Override
    protected Sort defaultSort() {
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    public Page<ExamItemEntity> list(String examType, String bodySite, String keyword, int page, int size) {
        return list(allOf(
            eqIfPresent("examType", examType),
            eqIfPresent("bodySiteCode", bodySite)), keyword, page, size);
    }

    public List<String> getAllExamTypes() {
        return repository.findAllExamTypes();
    }
}
