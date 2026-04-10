"""Celery task: Data preprocessing"""
import time
from app.core.celery_app import celery_app


@celery_app.task(name="app.tasks.preprocessing.run_preprocessing")
def run_preprocessing(task_type: str, input_path: str, output_path: str, config: dict):
    """Run data preprocessing task.

    Supports: DICOM conversion, text NLP preprocessing, feature extraction
    """
    print(f"Starting preprocessing: type={task_type}, input={input_path}")

    # TODO: Implement actual preprocessing pipeline
    time.sleep(2)

    return {
        "status": "COMPLETED",
        "input_path": input_path,
        "output_path": output_path,
        "records_processed": 0,
    }
