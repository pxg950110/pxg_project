package com.maidc.task.consumer;

import com.maidc.common.mq.model.MaidcMessage;
import com.maidc.common.mq.trace.TraceMessageHelper;
import com.maidc.task.dto.PersonalTaskCreateDTO;
import com.maidc.task.service.PersonalTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PersonalTaskConsumer {

    private final PersonalTaskService personalTaskService;

    @RabbitListener(queues = "approval.notify")
    public void onApprovalNotify(MaidcMessage message) {
        String traceId = TraceMessageHelper.restore(message);
        try {
            log.info("Received approval notify: traceId={}, eventType={}", traceId, message.getEventType());
            handleNotify(message, "APPROVAL");
        } finally {
            TraceMessageHelper.clear(traceId);
        }
    }

    @RabbitListener(queues = "label.notify")
    public void onLabelNotify(MaidcMessage message) {
        String traceId = TraceMessageHelper.restore(message);
        try {
            log.info("Received label notify: traceId={}, eventType={}", traceId, message.getEventType());
            handleNotify(message, "LABELING");
        } finally {
            TraceMessageHelper.clear(traceId);
        }
    }

    private void handleNotify(MaidcMessage message, String taskType) {
        Map<String, Object> payload = message.getPayload();
        Long assigneeId = extractLong(payload, "assigneeId");
        String title = extractString(payload, "title");
        Long sourceId = extractLong(payload, "bizId");

        if (assigneeId == null || title == null) {
            log.warn("Missing required fields in message: {}", message);
            return;
        }

        PersonalTaskCreateDTO dto = PersonalTaskCreateDTO.builder()
                .title(title)
                .taskType(taskType)
                .assigneeId(assigneeId)
                .sourceId(sourceId)
                .sourceType(taskType)
                .priority(extractString(payload, "priority"))
                .build();

        personalTaskService.createTask(dto);
        log.info("Created personal task: type={}, assignee={}", taskType, assigneeId);
    }

    private Long extractLong(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try { return Long.parseLong(value.toString()); } catch (NumberFormatException e) { return null; }
    }

    private String extractString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value != null ? value.toString() : null;
    }
}
