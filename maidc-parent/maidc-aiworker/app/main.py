"""MAIDC AI Worker Service - FastAPI Application"""
from fastapi import FastAPI
from app.api.inference import router as inference_router
from app.api.health import router as health_router
from app.api.serving import router as serving_router
from app.api.workers import router as workers_router
from app.core.config import settings

app = FastAPI(
    title="MAIDC AI Worker",
    version="1.0.0",
    description="Model inference, evaluation, and serving service",
)

app.include_router(inference_router, tags=["inference"])
app.include_router(health_router, tags=["health"])
app.include_router(serving_router, tags=["serving"])
app.include_router(workers_router, tags=["workers"])


@app.on_event("startup")
async def startup():
    print(f"MAIDC AI Worker started on port {settings.port}")
    print(f"Model cache dir: {settings.model_cache_dir}")
    print(f"GPU enabled: {settings.gpu_enabled}")
