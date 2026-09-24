"""MAIDC AI Worker Service - FastAPI Application"""
import logging

from fastapi import FastAPI
from app.api.health import router as health_router
from app.api.inference import router as inference_router
from app.api.llm import router as llm_router
from app.api.serving import router as serving_router
from app.api.workers import router as workers_router
from app.core.config import settings
from app.core.logging import setup_logging
from app.core.trace import TraceContextMiddleware

setup_logging()
logger = logging.getLogger(__name__)

app = FastAPI(
    title="MAIDC AI Worker",
    version="1.1.0",
    description="Model inference, evaluation, serving and disease-KB LLM service",
)

# 链路追踪：入站 X-Trace-Id → ContextVar → 日志，响应头回写（须先于路由注册）
app.add_middleware(TraceContextMiddleware)

app.include_router(inference_router, tags=["inference"])
app.include_router(health_router, tags=["health"])
app.include_router(serving_router, tags=["serving"])
app.include_router(workers_router, tags=["workers"])
app.include_router(llm_router, tags=["llm"])


@app.on_event("startup")
async def startup():
    logger.info("MAIDC AI Worker started on port %s", settings.port)
    logger.info("Model cache dir: %s", settings.model_cache_dir)
    logger.info("GPU enabled: %s", settings.gpu_enabled)
    logger.info("LLM enabled: %s (model=%s)", settings.llm_enabled, settings.llm_model)
    logger.info("KB RAG store: %s", "configured" if settings.kb_pg_dsn else "NOT configured")
