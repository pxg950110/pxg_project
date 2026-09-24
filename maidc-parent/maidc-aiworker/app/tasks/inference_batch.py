"""Celery task: Batch inference"""
import logging
import time
from app.core.celery_app import celery_app

logger = logging.getLogger(__name__)


@celery_app.task(name="app.tasks.inference_batch.run_batch")
def run_batch_inference(model_code: str, version: str, dataset_path: str,
                        output_path: str, params: dict):
    """Run batch inference on a dataset.

    1. Load model
    2. Read input data
    3. Process in batches
    4. Save results to output_path
    """
    logger.info("Starting batch inference: model=%s, dataset=%s", model_code, dataset_path)

    # TODO: Implement actual batch inference
    time.sleep(3)

    return {
        "status": "COMPLETED",
        "total_records": 0,
        "processed_records": 0,
        "output_path": output_path,
    }
