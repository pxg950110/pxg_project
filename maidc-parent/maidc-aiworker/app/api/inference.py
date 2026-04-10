"""Synchronous inference API"""
import time
import uuid
from typing import Any

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

router = APIRouter()


class InferenceRequest(BaseModel):
    request_id: str | None = None
    patient_id: int | None = None
    encounter_id: int | None = None
    input: dict[str, Any]
    parameters: dict[str, Any] | None = None


class InferenceResponse(BaseModel):
    request_id: str
    results: list[dict[str, Any]]
    latency_ms: int
    model_version: str


@router.post("/v1/infer/{model_code}", response_model=InferenceResponse)
async def infer(model_code: str, request: InferenceRequest):
    """Run synchronous inference on a loaded model."""
    start_time = time.time()

    request_id = request.request_id or str(uuid.uuid4())

    # TODO: Load model from cache and run actual inference
    # For now, return a placeholder response
    results = [
        {
            "label": "placeholder",
            "confidence": 0.0,
            "message": f"Model {model_code} not loaded. Configure model serving first.",
        }
    ]

    latency_ms = int((time.time() - start_time) * 1000)

    return InferenceResponse(
        request_id=request_id,
        results=results,
        latency_ms=latency_ms,
        model_version="latest",
    )
