"""OpenAI 兼容 LLM 客户端：chat（含流式）与 embeddings。

未配置 MAIDC_LLM_BASE_URL / MAIDC_LLM_API_KEY 时视为不可用，
调用方（Java DiseaseKnowledgeAiService / 本模块路由）负责降级。
"""
import json
from typing import AsyncIterator

import httpx

from app.core.config import settings


class LlmNotConfiguredError(RuntimeError):
    pass


def _require_llm() -> None:
    if not settings.llm_enabled:
        raise LlmNotConfiguredError("LLM 未配置（MAIDC_LLM_BASE_URL / MAIDC_LLM_API_KEY）")


async def chat(messages: list[dict], json_mode: bool = False, temperature: float = 0.3) -> str:
    """非流式对话，返回完整文本。"""
    _require_llm()
    body: dict = {
        "model": settings.llm_model,
        "messages": messages,
        "temperature": temperature,
        "stream": False,
    }
    if json_mode:
        body["response_format"] = {"type": "json_object"}
    async with httpx.AsyncClient(timeout=settings.llm_timeout_seconds) as client:
        resp = await client.post(
            f"{settings.llm_base_url}/chat/completions",
            headers={"Authorization": f"Bearer {settings.llm_api_key}"},
            json=body,
        )
        resp.raise_for_status()
        return resp.json()["choices"][0]["message"]["content"]


async def chat_stream(messages: list[dict], temperature: float = 0.3) -> AsyncIterator[str]:
    """流式对话，逐 token 产出文本片段。"""
    _require_llm()
    body = {
        "model": settings.llm_model,
        "messages": messages,
        "temperature": temperature,
        "stream": True,
    }
    async with httpx.AsyncClient(timeout=settings.llm_timeout_seconds) as client:
        async with client.stream(
            "POST",
            f"{settings.llm_base_url}/chat/completions",
            headers={"Authorization": f"Bearer {settings.llm_api_key}"},
            json=body,
        ) as resp:
            resp.raise_for_status()
            async for line in resp.aiter_lines():
                if not line.startswith("data:"):
                    continue
                payload = line[5:].strip()
                if payload == "[DONE]":
                    break
                try:
                    delta = json.loads(payload)["choices"][0]["delta"].get("content")
                except (json.JSONDecodeError, KeyError, IndexError):
                    continue
                if delta:
                    yield delta


async def embed(texts: list[str]) -> list[list[float]]:
    """批量向量化，返回与输入等长的向量列表。"""
    _require_llm()
    async with httpx.AsyncClient(timeout=settings.llm_timeout_seconds) as client:
        resp = await client.post(
            f"{settings.llm_base_url}/embeddings",
            headers={"Authorization": f"Bearer {settings.llm_api_key}"},
            json={"model": settings.embedding_model, "input": texts},
        )
        resp.raise_for_status()
        data = resp.json()["data"]
        return [item["embedding"] for item in sorted(data, key=lambda d: d["index"])]
