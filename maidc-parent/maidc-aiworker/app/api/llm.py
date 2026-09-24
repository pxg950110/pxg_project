"""专病知识库 AI 路由：摘要/知识抽取、向量化、RAG 流式问答。

契约（与 Java DiseaseKnowledgeAiService 对应）：
  POST /llm/summary  {"text":..., "item_type":...} → {"summary":..., "extract":{...}}
  POST /embedding    {"texts":[...]}               → {"vectors":[[...]]}
  POST /rag/chat     {"space_id":..., "session_id":..., "question":...}
                    → SSE，每帧一行 data:{"type":"delta"|"citations"|"done"|"error", ...}

LLM 未配置（MAIDC_LLM_BASE_URL / MAIDC_LLM_API_KEY 缺失）时：
  摘要/向量化返回 503，问答流内发 error 帧——调用方按降级语义处理。
"""
import json

from fastapi import APIRouter, HTTPException
from fastapi.responses import StreamingResponse
from pydantic import BaseModel

from app.core.config import settings
from app.services import kb_store, llm_client
from app.services.llm_client import LlmNotConfiguredError

router = APIRouter()

# 按条目类型的知识抽取提示词（输出 JSON 字段约定见各提示）
_EXTRACT_PROMPTS = {
    "GUIDELINE": "提取该指南/共识的关键信息：recommended_grading（推荐等级要点列表）、evidence_level（证据级别要点）、key_recommendations（核心推荐列表，每项一句话）",
    "LITERATURE": "提取该文献的关键信息：study_design（研究类型）、sample_size（样本量，未知为null）、population（研究对象）、conclusion（主要结论，两句话内）",
    "PATHWAY": "提取该诊疗路径的关键信息：stages（阶段节点列表，每项含 name 与 description）、key_examinations（关键检查列表）、referral_criteria（转诊指征）",
    "SCALE": "提取该量表/表单的关键信息：dimensions（评估维度列表）、scoring_rule（评分规则概述）、interpretation（结果解读要点）",
}

_SYSTEM_RAG = (
    "你是医院专病知识库助手。仅依据提供的知识片段回答用户问题，"
    "回答末尾不要编造引用；若片段不足以回答，明确说明知识库中未找到相关依据。"
    "涉及用药剂量/手术方案等高风险内容时，提醒以正式指南原文与临床决策为准。"
)


class SummaryRequest(BaseModel):
    text: str
    item_type: str = "GUIDELINE"


class EmbeddingRequest(BaseModel):
    texts: list[str]


class RagChatRequest(BaseModel):
    space_id: int
    session_id: int | None = None
    question: str


def _sse(payload: dict) -> str:
    return f"data: {json.dumps(payload, ensure_ascii=False)}\n\n"


@router.post("/llm/summary")
async def llm_summary(req: SummaryRequest):
    try:
        raw = await llm_client.chat(
            [
                {"role": "system", "content": "你是医学知识整理助手，输出严格 JSON。"},
                {"role": "user", "content": (
                    f"为以下内容生成不超过200字的中文摘要，并{_EXTRACT_PROMPTS.get(req.item_type, _EXTRACT_PROMPTS['GUIDELINE'])}。"
                    f'输出 JSON：{{"summary":"...","extract":{{...}}}}\n\n内容：\n{req.text[:12000]}'
                )},
            ],
            json_mode=True,
        )
        parsed = json.loads(raw)
        return {
            "summary": parsed.get("summary", ""),
            "extract": parsed.get("extract") or {},
        }
    except LlmNotConfiguredError:
        raise HTTPException(503, "LLM 未配置，无法生成摘要")
    except json.JSONDecodeError:
        # 非 JSON 输出降级：全文当摘要，抽取为空
        return {"summary": raw[:500], "extract": {}}


@router.post("/embedding")
async def embedding(req: EmbeddingRequest):
    if not req.texts:
        return {"vectors": []}
    try:
        vectors = await llm_client.embed(req.texts)
    except LlmNotConfiguredError:
        raise HTTPException(503, "LLM 未配置，无法向量化")
    return {"vectors": vectors}


@router.post("/rag/chat")
async def rag_chat(req: RagChatRequest):
    async def event_stream():
        if not settings.llm_enabled:
            yield _sse({"type": "error", "message": "LLM 未配置，AI 问答不可用"})
            return
        # 1. 检索（失败按无依据处理，不让整条流崩掉）
        try:
            hits = await kb_store.search_chunks(req.space_id, req.question)
        except Exception as e:  # noqa: BLE001
            yield _sse({"type": "error", "message": f"知识检索失败: {e}"})
            return
        if not hits:
            yield _sse({"type": "delta", "text": "知识库中未找到与该问题相关的已发布内容。"})
            yield _sse({"type": "citations", "citations": []})
            yield _sse({"type": "done"})
            return
        yield _sse({"type": "citations", "citations": hits})

        # 2. 组装上下文，流式生成
        context = "\n\n".join(
            f"[{idx + 1}] 《{h['title']}》\n{h['snippet']}" for idx, h in enumerate(hits)
        )
        messages = [
            {"role": "system", "content": _SYSTEM_RAG},
            {"role": "user", "content": f"知识片段：\n{context}\n\n问题：{req.question}"},
        ]
        try:
            async for delta in llm_client.chat_stream(messages):
                yield _sse({"type": "delta", "text": delta})
        except LlmNotConfiguredError:
            yield _sse({"type": "error", "message": "LLM 未配置，AI 问答不可用"})
            return
        except Exception as e:  # noqa: BLE001
            yield _sse({"type": "error", "message": f"生成中断: {e}"})
            return
        yield _sse({"type": "done"})

    return StreamingResponse(event_stream(), media_type="text/event-stream")
