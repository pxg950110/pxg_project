"""Model serving management API"""
from typing import Any

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

from app.core.config import settings

router = APIRouter()

# In-memory model registry
_loaded_models: dict[str, dict[str, Any]] = {}


class LoadModelRequest(BaseModel):
    model_code: str
    version: str = "latest"
    device: str = "cpu"


class ModelStatus(BaseModel):
    model_code: str
    version: str
    device: str
    status: str
    memory_mb: float = 0.0


@router.post("/v1/serving/load")
async def load_model(request: LoadModelRequest):
    """Load a model into memory for serving."""
    model_key = f"{request.model_code}:{request.version}"

    if model_key in _loaded_models:
        return {"status": "ALREADY_LOADED", "model_key": model_key}

    # TODO: Implement actual model loading from MinIO cache
    _loaded_models[model_key] = {
        "model_code": request.model_code,
        "version": request.version,
        "device": request.device,
        "status": "LOADED",
        "loaded_at": __import__("time").time(),
    }

    return {"status": "LOADED", "model_key": model_key}


@router.get("/v1/serving/status")
async def serving_status():
    """List all loaded models."""
    return {
        "loaded_models": list(_loaded_models.keys()),
        "count": len(_loaded_models),
    }
