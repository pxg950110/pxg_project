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
    rabbitmq_vhost: str = "%2F"  # URL 编码；"/" 是 broker 实际存在的默认 vhost（未创建过 maidc vhost）

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

    # LLM（OpenAI 兼容协议；未配置时 /llm/* 返回 503，调用方降级）
    llm_base_url: str = ""          # 如 https://api.openai.com/v1 或本地 vLLM/Ollama 地址
    llm_api_key: str = ""
    llm_model: str = "gpt-4o-mini"
    llm_timeout_seconds: int = 120
    embedding_model: str = "text-embedding-3-large"
    embedding_dim: int = 1024       # 必须与 c_disease_kb_item_chunk.embedding 维度一致

    # 专病知识库 RAG 检索（pgvector，只读）
    kb_pg_dsn: str = ""             # 如 postgresql://maidc:maidc123@localhost:5432/maidc
    kb_rag_top_k: int = 6
    kb_chunk_chars: int = 800       # 与 Java 侧分块保持一致

    @property
    def llm_enabled(self) -> bool:
        return bool(self.llm_base_url and self.llm_api_key)

    @property
    def rabbitmq_url(self) -> str:
        return f"amqp://{self.rabbitmq_user}:{self.rabbitmq_password}@{self.rabbitmq_host}:{self.rabbitmq_port}/{self.rabbitmq_vhost}"

    class Config:
        env_prefix = "MAIDC_"
        env_file = ".env"


settings = Settings()
