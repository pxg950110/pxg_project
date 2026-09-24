package com.maidc.label.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.core.result.PageResult;
import com.maidc.label.entity.LabelRecordEntity;
import com.maidc.label.entity.LabelTaskEntity;
import com.maidc.label.repository.LabelRecordRepository;
import com.maidc.label.repository.LabelTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 标注工作台条目服务（前端 LabelWorkspace 四件套）。
 * <p>r_label_record 一行即一个待标数据项（taskId + dataId），
 * annotation jsonb 直接承载 {id,label,x,y,w,h}[] 数组；
 * verification_status 兼做条目状态机：PENDING → LABELED → SUBMITTED / SKIPPED。
 */
@Service
@RequiredArgsConstructor
public class LabelItemService {

    private final LabelRecordRepository labelRecordRepository;
    private final LabelTaskRepository labelTaskRepository;

    /** 分页列出任务条目；status 过滤 verification_status（null 视为 PENDING） */
    public PageResult<LabelRecordEntity> listItems(Long taskId, String status, int page, int pageSize) {
        requireTask(taskId);
        List<LabelRecordEntity> all = labelRecordRepository.findByTaskIdAndIsDeletedFalse(taskId.toString());
        List<LabelRecordEntity> filtered = (status == null || status.isBlank()) ? all
                : all.stream().filter(r -> status.equalsIgnoreCase(effectiveStatus(r))).toList();
        int size = Math.max(pageSize, 1);
        int from = Math.min(Math.max(page - 1, 0) * size, filtered.size());
        int to = Math.min(from + size, filtered.size());

        PageResult<LabelRecordEntity> result = new PageResult<>();
        result.setItems(filtered.subList(from, to));
        result.setTotal(filtered.size());
        result.setPage(Math.max(page, 1));
        result.setPageSize(size);
        result.setTotalPages(size == 0 ? 0 : (filtered.size() + size - 1) / size);
        return result;
    }

    /** 读取条目标注数组；无标注返回空列表 */
    public Object getAnnotations(Long taskId, Long itemId) {
        LabelRecordEntity record = requireItemOfTask(taskId, itemId);
        JsonNode annotation = record.getAnnotation();
        return annotation != null && annotation.isArray() ? annotation : List.of();
    }

    /** 保存标注数组（未提交状态置 LABELED） */
    @Transactional
    public LabelRecordEntity saveAnnotations(Long taskId, Long itemId, JsonNode annotations) {
        LabelRecordEntity record = requireItemOfTask(taskId, itemId);
        record.setAnnotation(annotations);
        if (!"SUBMITTED".equals(effectiveStatus(record)) && !"SKIPPED".equals(effectiveStatus(record))) {
            record.setVerificationStatus("LABELED");
        }
        record.setLabeledAt(LocalDateTime.now());
        return labelRecordRepository.save(record);
    }

    /** 提交条目：置 SUBMITTED 并推进任务 labeledCount（幂等，封顶 totalCount） */
    @Transactional
    public LabelRecordEntity submit(Long taskId, Long itemId) {
        LabelRecordEntity record = requireItemOfTask(taskId, itemId);
        boolean alreadySubmitted = "SUBMITTED".equals(effectiveStatus(record));
        record.setVerificationStatus("SUBMITTED");
        record.setLabeledAt(LocalDateTime.now());
        record = labelRecordRepository.save(record);

        if (!alreadySubmitted) {
            labelTaskRepository.findById(taskId).ifPresent(t -> {
                int labeled = t.getLabeledCount() != null ? t.getLabeledCount() : 0;
                int total = t.getTotalCount() != null ? t.getTotalCount() : Integer.MAX_VALUE;
                t.setLabeledCount(Math.min(labeled + 1, total));
                labelTaskRepository.save(t);
            });
        }
        return record;
    }

    /** 跳过条目：置 SKIPPED，不计入 labeledCount */
    @Transactional
    public LabelRecordEntity skip(Long taskId, Long itemId) {
        LabelRecordEntity record = requireItemOfTask(taskId, itemId);
        record.setVerificationStatus("SKIPPED");
        return labelRecordRepository.save(record);
    }

    /** 校验条目归属任务并返回（不匹配时与未找到同码，不暴露存在性） */
    private LabelRecordEntity requireItemOfTask(Long taskId, Long itemId) {
        LabelRecordEntity record = labelRecordRepository.findByIdAndIsDeletedFalse(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!record.getTaskId().equals(taskId.toString())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return record;
    }

    private LabelTaskEntity requireTask(Long taskId) {
        return labelTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private String effectiveStatus(LabelRecordEntity record) {
        return record.getVerificationStatus() != null ? record.getVerificationStatus() : "PENDING";
    }
}
