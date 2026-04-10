package com.maidc.model.mq;

import com.maidc.common.mq.model.MaidcMessage;
import com.maidc.common.mq.producer.BaseMessageProducer;
import com.maidc.model.config.ModelRabbitMqConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class ModelMessageProducer extends BaseMessageProducer {

    public void sendEvaluationTask(Long evaluationId, Long versionId, Long datasetId, Map<String, Object> metricsConfig) {
        MaidcMessage message = MaidcMessage.of("EVALUATION",
                Map.of(
                        "evaluationId", evaluationId,
                        "versionId", versionId,
                        "datasetId", datasetId,
                        "metricsConfig", metricsConfig
                ), "maidc-model");
        send(ModelRabbitMqConfig.MODEL_EXCHANGE, ModelRabbitMqConfig.EVALUATION_KEY, message);
    }

    public void sendDeploymentTask(Long deploymentId, Long versionId, Map<String, Object> resourceConfig) {
        MaidcMessage message = MaidcMessage.of("DEPLOYMENT",
                Map.of(
                        "deploymentId", deploymentId,
                        "versionId", versionId,
                        "resourceConfig", resourceConfig
                ), "maidc-model");
        send(ModelRabbitMqConfig.MODEL_EXCHANGE, ModelRabbitMqConfig.DEPLOYMENT_KEY, message);
    }
}
