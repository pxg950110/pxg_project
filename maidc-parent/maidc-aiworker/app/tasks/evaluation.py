"""Celery task: Model evaluation"""
import json
import logging
import time
from celery import Task
from app.core.celery_app import celery_app
from app.core.trace import get_trace_id

logger = logging.getLogger(__name__)


class EvaluationTask(Task):
    """Base evaluation task with error handling."""

    def on_failure(self, exc, task_id, args, kwargs, einfo):
        # Send result back to model service via RabbitMQ
        logger.error("Evaluation task %s failed: %s", task_id, exc)


@celery_app.task(name="app.tasks.evaluation.run_evaluation", base=EvaluationTask)
def run_evaluation(evaluation_id: int, version_id: int, dataset_id: int,
                   metrics_config: dict):
    """Run model evaluation.

    1. Download model file from MinIO
    2. Load dataset
    3. Run inference on dataset
    4. Calculate metrics (AUC, F1, precision, recall, confusion matrix)
    5. Generate PDF report
    6. Send results back via MQ
    """
    logger.info("Starting evaluation: id=%s, version=%s, dataset=%s",
                evaluation_id, version_id, dataset_id)

    # TODO: Implement actual evaluation pipeline
    # For now, simulate the process
    time.sleep(5)

    result = {
        "evaluationId": evaluation_id,
        "status": "COMPLETED",
        "metrics": {
            "auc": 0.9234,
            "f1": 0.8912,
            "precision": 0.9045,
            "recall": 0.8786,
        },
        "confusionMatrix": {
            "TP": 442, "FP": 46, "FN": 61, "TN": 951
        },
        "reportUrl": f"/evaluations/{evaluation_id}/report",
    }

    # Send result to model.evaluation.result queue；
    # traceId 继承自触发消息（task_prerun 信号恢复），而非伪造 eval-{id}
    from app.core.celery_app import celery_app
    with celery_app.connection_or_acquire() as conn:
        conn.default_channel.basic_publish(
            exchange="maidc.model",
            routing_key="evaluation.result",
            body=json.dumps({
                "traceId": get_trace_id(),
                "eventType": "EVALUATION_RESULT",
                "payload": result,
                "source": "maidc-aiworker",
            }),
            properties={"content_type": "application/json"},
        )

    return result
