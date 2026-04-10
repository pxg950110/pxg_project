"""Celery task: Model evaluation"""
import json
import time
from celery import Task
from app.core.celery_app import celery_app


class EvaluationTask(Task):
    """Base evaluation task with error handling."""

    def on_failure(self, exc, task_id, args, kwargs, einfo):
        # Send result back to model service via RabbitMQ
        print(f"Evaluation task {task_id} failed: {exc}")


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
    print(f"Starting evaluation: id={evaluation_id}, version={version_id}, dataset={dataset_id}")

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

    # Send result to model.evaluation.result queue
    from app.core.celery_app import celery_app
    with celery_app.connection_or_acquire() as conn:
        conn.default_channel.basic_publish(
            exchange="maidc.model",
            routing_key="evaluation.result",
            body=json.dumps({
                "traceId": f"eval-{evaluation_id}",
                "eventType": "EVALUATION_RESULT",
                "payload": result,
                "source": "maidc-aiworker",
            }),
            properties={"content_type": "application/json"},
        )

    return result
