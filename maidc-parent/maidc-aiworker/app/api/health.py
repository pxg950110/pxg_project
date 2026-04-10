"""Health check API"""
from fastapi import APIRouter

router = APIRouter()


@router.get("/health")
async def health():
    """Health check endpoint."""
    return {"status": "UP", "service": "maidc-aiworker"}
