"""Configuration settings for AI Worker"""
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # Server
    port: int = 8090
    host: str = "0.0.0.0"

    # RabbitMQ
    rabbitmq_host: str = "localhost"
    rabbitmq_port: int = 5672
    rabbitmq_user: str = "maidc"
    rabbitmq_password: str = "maidc123"
    rabbitmq_vhost: str = "maidc"

    # MinIO
    minio_endpoint: str = "localhost:9000"
    minio_access_key: str = "maidc"
    minio_secret_key: str = "maidc12345"

    # Model serving
    model_cache_dir: str = "/data/models"
    max_batch_size: int = 32
    gpu_enabled: bool = False
    inference_timeout_ms: int = 60000

    # Worker
    worker_concurrency: int = 4

    @property
    def rabbitmq_url(self) -> str:
        return f"amqp://{self.rabbitmq_user}:{self.rabbitmq_password}@{self.rabbitmq_host}:{self.rabbitmq_port}/{self.rabbitmq_vhost}"

    class Config:
        env_prefix = "MAIDC_"
        env_file = ".env"


settings = Settings()
