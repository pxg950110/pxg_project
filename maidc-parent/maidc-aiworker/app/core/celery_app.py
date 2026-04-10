"""Celery application configuration"""
from celery import Celery
from app.core.config import settings

celery_app = Celery(
    "maidc-aiworker",
    broker=settings.rabbitmq_url,
    backend="rpc://",
    include=["app.tasks.evaluation", "app.tasks.inference_batch", "app.tasks.preprocessing"],
)

celery_app.conf.update(
    task_serializer="json",
    accept_content=["json"],
    result_serializer="json",
    timezone="Asia/Shanghai",
    enable_utc=True,
    task_routes={
        "app.tasks.evaluation.*": {"queue": "model.evaluation"},
        "app.tasks.inference_batch.*": {"queue": "model.inference.batch"},
        "app.tasks.preprocessing.*": {"queue": "model.preprocessing"},
    },
    worker_prefetch_multiplier=1,
    task_acks_late=True,
    worker_concurrency=settings.worker_concurrency,
)
