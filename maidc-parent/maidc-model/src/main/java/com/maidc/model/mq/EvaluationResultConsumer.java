package com.maidc.model.mq;

import com.maidc.common.mq.consumer.BaseMessageConsumer;
import com.maidc.common.mq.model.MaidcMessage;
import com.maidc.model.config.ModelRabbitMqConfig;
import com.maidc.model.entity.EvaluationEntity;
import com.maidc.model.repository.EvaluationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
public class EvaluationResultConsumer extends BaseMessageConsumer {

    private final EvaluationRepository evaluationRepository;

    public EvaluationResultConsumer(EvaluationRepository evaluationRepository) {
        this.evaluationRepository = evaluationRepository;
    }

    @RabbitListener(queues = ModelRabbitMqConfig.EVALUATION_RESULT_QUEUE)
    public void onMessage(MaidcMessage message) {
        handleMessage(message);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void processMessage(MaidcMessage message) {
        Map<String, Object> payload = message.getPayload();
        Long evaluationId = ((Number) payload.get("evaluationId")).longValue();

        EvaluationEntity eval = evaluationRepository.findById(evaluationId).orElse(null);
        if (eval == null) {
            log.warn("评估任务不存在: id={}", evaluationId);
            return;
        }

        // Idempotency: skip if already completed
        if (!"RUNNING".equals(eval.getStatus())) {
            log.info("评估任务已处理，跳过: id={}, status={}", evaluationId, eval.getStatus());
            return;
        }

        String status = (String) payload.get("status");
        if ("COMPLETED".equals(status)) {
            eval.setStatus("COMPLETED");
            eval.setMetrics(com.fasterxml.jackson.databind.JsonNode.class.cast(payload.get("metrics")));
            eval.setConfusionMatrix(com.fasterxml.jackson.databind.JsonNode.class.cast(payload.get("confusionMatrix")));
            eval.setReportUrl((String) payload.get("reportUrl"));
        } else {
            eval.setStatus("FAILED");
        }
        eval.setCompletedAt(LocalDateTime.now());
        evaluationRepository.save(eval);

        log.info("评估结果已更新: id={}, status={}", evaluationId, status);
    }
}
