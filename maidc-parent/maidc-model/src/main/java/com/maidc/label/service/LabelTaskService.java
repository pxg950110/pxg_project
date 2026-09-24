package com.maidc.label.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.core.result.PageResult;
import com.maidc.common.mq.model.MaidcMessage;
import com.maidc.common.mq.producer.BaseMessageProducer;
import com.maidc.label.config.LabelRabbitMqConfig;
import com.maidc.label.dto.LabelTaskCreateDTO;
import com.maidc.label.dto.LabelTaskUpdateDTO;
import com.maidc.label.entity.LabelRecordEntity;
import com.maidc.label.entity.LabelTaskEntity;
import com.maidc.label.mapper.LabelMapper;
import com.maidc.label.repository.LabelRecordRepository;
import com.maidc.label.repository.LabelTaskRepository;
import com.maidc.label.vo.LabelStatsVO;
import com.maidc.label.vo.LabelTaskDetailVO;
import com.maidc.label.vo.LabelTaskVO;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LabelTaskService extends BaseMessageProducer {

    private final LabelTaskRepository labelTaskRepository;
    private final LabelRecordRepository labelRecordRepository;
    private final LabelMapper labelMapper;
    private final ObjectMapper objectMapper;
    private final LabelRabbitMqConfig labelRabbitMqConfig;

    /**
     * Create a new label task
     */
    @Transactional
    public LabelTaskVO createTask(LabelTaskCreateDTO dto) {
        LabelTaskEntity entity = new LabelTaskEntity();
        entity.setName(dto.getName());
        entity.setTaskType(dto.getTaskType());
        entity.setDatasetId(dto.getDatasetId());
        entity.setAssigneeId(dto.getAssigneeId());
        entity.setStatus("PENDING");
        entity.setTotalCount(0);
        entity.setLabeledCount(0);
        entity.setVerifiedCount(0);

        // Convert labels list to JsonNode
        if (dto.getLabels() != null && !dto.getLabels().isEmpty()) {
            try {
                entity.setLabels(objectMapper.valueToTree(dto.getLabels()));
            } catch (Exception e) {
                log.error("Failed to convert labels to JsonNode", e);
            }
        }

        entity.setGuidelines(dto.getGuidelines());

        LabelTaskEntity saved = labelTaskRepository.save(entity);
        log.info("标注任务已创建: id={}, name={}", saved.getId(), saved.getName());
        return labelMapper.toTaskVO(saved);
    }

    /**
     * Update an existing label task
     */
    @Transactional
    public LabelTaskVO updateTask(Long id, LabelTaskUpdateDTO dto) {
        LabelTaskEntity entity = labelTaskRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (dto.getName() != null) {
            entity.setName(dto.getName());
        }
        if (dto.getAssigneeId() != null) {
            entity.setAssigneeId(dto.getAssigneeId());
        }
        if (dto.getStatus() != null) {
            validateStatusTransition(entity.getStatus(), dto.getStatus());
            entity.setStatus(dto.getStatus());
        }
        if (dto.getLabels() != null) {
            try {
                entity.setLabels(objectMapper.valueToTree(dto.getLabels()));
            } catch (Exception e) {
                log.error("Failed to convert labels to JsonNode", e);
            }
        }
        if (dto.getGuidelines() != null) {
            entity.setGuidelines(dto.getGuidelines());
        }

        LabelTaskEntity updated = labelTaskRepository.save(entity);
        log.info("标注任务已更新: id={}, status={}", updated.getId(), updated.getStatus());
        return labelMapper.toTaskVO(updated);
    }

    /**
     * Get task detail by id
     */
    public LabelTaskDetailVO getTask(Long id) {
        LabelTaskEntity entity = labelTaskRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return labelMapper.toTaskDetailVO(entity);
    }

    /**
     * List tasks with pagination and optional filters
     */
    public PageResult<LabelTaskVO> listTasks(String status, String taskType, int page, int pageSize) {
        Specification<LabelTaskEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isFalse(root.get("isDeleted")));

            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (taskType != null && !taskType.isEmpty()) {
                predicates.add(cb.equal(root.get("taskType"), taskType));
            }

            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<LabelTaskEntity> pageData = labelTaskRepository.findAll(spec,
                PageRequest.of(page - 1, pageSize));

        Page<LabelTaskVO> voPage = pageData.map(labelMapper::toTaskVO);
        return PageResult.of(voPage);
    }

    /**
     * Soft delete a task
     */
    @Transactional
    public void deleteTask(Long id) {
        LabelTaskEntity entity = labelTaskRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        entity.setIsDeleted(true);
        labelTaskRepository.save(entity);
        log.info("标注任务已删除: id={}", id);
    }

    /**
     * Get task statistics
     */
    public LabelStatsVO getTaskStats(Long id) {
        LabelTaskEntity entity = labelTaskRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        LabelStatsVO stats = new LabelStatsVO();
        stats.setTaskId(id);
        stats.setTotalCount(entity.getTotalCount() != null ? entity.getTotalCount() : 0);
        stats.setLabeledCount(entity.getLabeledCount() != null ? entity.getLabeledCount() : 0);
        stats.setVerifiedCount(entity.getVerifiedCount() != null ? entity.getVerifiedCount() : 0);

        // Build label distribution from records
        List<LabelRecordEntity> records = labelRecordRepository.findByTaskIdAndIsDeletedFalse(id.toString());
        Map<String, Integer> byLabel = new LinkedHashMap<>();
        for (LabelRecordEntity record : records) {
            String label = record.getLabel();
            if (label != null) {
                byLabel.merge(label, 1, Integer::sum);
            }
        }
        stats.setByLabel(byLabel);

        return stats;
    }

    /**
     * Trigger AI pre-annotation by sending an MQ message
     */
    public void triggerAiPreAnnotate(Long id) {
        LabelTaskEntity entity = labelTaskRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", id);
        payload.put("datasetId", entity.getDatasetId());
        payload.put("taskType", entity.getTaskType());

        MaidcMessage message = MaidcMessage.of("AI_PRE_ANNOTATE", payload, "maidc-label");
        send(labelRabbitMqConfig.LABEL_EXCHANGE,
                labelRabbitMqConfig.PREPROCESSING_ROUTING_KEY,
                message);

        log.info("AI预标注任务已触发: taskId={}", id);
    }

    /**
     * Validate status transition
     */
    private void validateStatusTransition(String current, String target) {
        Set<String> validStatuses = Set.of("PENDING", "IN_PROGRESS", "COMPLETED", "CANCELLED");
        if (!validStatuses.contains(target)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        // PENDING -> IN_PROGRESS -> COMPLETED or CANCELLED (any -> CANCELLED)
        if ("COMPLETED".equals(target) && !"IN_PROGRESS".equals(current)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
    }
}
