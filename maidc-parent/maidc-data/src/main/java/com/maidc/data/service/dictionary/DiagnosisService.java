package com.maidc.data.service.dictionary;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.entity.dictionary.DiagnosisEntity;
import com.maidc.data.repository.dictionary.DiagnosisDictRepository;
import com.maidc.data.repository.dictionary.DictionaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiagnosisService extends AbstractDictionaryService<DiagnosisEntity> {

    private final DiagnosisDictRepository repository;

    @Override
    protected DictionaryRepository<DiagnosisEntity> repository() {
        return repository;
    }

    @Override
    protected String entityName() {
        return "诊断";
    }

    @Override
    protected String codeProperty() {
        return "diagnosisCode";
    }

    @Override
    protected List<String> searchProperties() {
        return List.of("diagnosisCode", "name", "icd10Code", "namePinyin");
    }

    @Override
    protected Sort defaultSort() {
        return Sort.by(Sort.Direction.ASC, "icd10Code");
    }

    public Page<DiagnosisEntity> list(String chapterCode, String status, String keyword, int page, int size) {
        return list(allOf(
            eqIfPresent("chapterCode", chapterCode),
            eqIfPresent("status", status)), keyword, page, size);
    }

    public DiagnosisEntity getByIcd10Code(String icd10Code) {
        return repository.findByIcd10Code(icd10Code)
            .orElseThrow(() -> new BusinessException(404, "诊断不存在: " + icd10Code));
    }

    public List<DiagnosisEntity> getTree() {
        return repository.findRootDiagnoses();
    }

    public List<DiagnosisEntity> getByChapter(String chapterCode) {
        return repository.findByChapterCode(chapterCode);
    }

    public List<DiagnosisEntity> getChildren(Long parentId) {
        return repository.findByParentId(parentId);
    }
}
