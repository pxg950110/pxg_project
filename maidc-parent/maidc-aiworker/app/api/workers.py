"""Worker status API"""
from fastapi import APIRouter
from app.core.config import settings

router = APIRouter()


@router.get("/v1/workers")
async def list_workers():
    """List available AI worker instances."""
    return {
        "workers": [
            {
                "id": "worker-0",
                "status": "IDLE",
                "gpu_enabled": settings.gpu_enabled,
                "models_loaded": 0,
                "max_batch_size": settings.max_batch_size,
            }
        ],
        "total": 1,
    }
