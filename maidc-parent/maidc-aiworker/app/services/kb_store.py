"""专病知识库 RAG 检索：pgvector 余弦相似度召回（只读）。"""
import asyncio
import json
from contextlib import contextmanager

import psycopg

from app.core.config import settings
from app.services import llm_client


class KbStoreNotConfiguredError(RuntimeError):
    pass


@contextmanager
def _conn():
    if not settings.kb_pg_dsn:
        raise KbStoreNotConfiguredError("知识库检索未配置（MAIDC_KB_PG_DSN）")
    with psycopg.connect(settings.kb_pg_dsn) as conn:
        yield conn


def _query(vec_str: str, space_id: int, top_k: int):
    sql = """
        SELECT c.id AS chunk_id, c.chunk_text, i.id AS item_id, i.title,
               1 - (c.embedding <=> %(vec)s::vector) AS score
        FROM cdr.c_disease_kb_item_chunk c
        JOIN cdr.c_disease_kb_item i ON i.id = c.item_id
        WHERE i.space_id = %(space_id)s
          AND i.status = 'PUBLISHED'
          AND i.is_deleted = false
          AND c.is_deleted = false
        ORDER BY c.embedding <=> %(vec)s::vector
        LIMIT %(k)s
    """
    with _conn() as conn, conn.cursor() as cur:
        cur.execute(sql, {"vec": vec_str, "space_id": space_id, "k": top_k})
        return cur.fetchall()


async def search_chunks(space_id: int, question: str, top_k: int | None = None) -> list[dict]:
    """按问题向量召回指定空间已发布条目的分块。

    :return: [{chunkId, itemId, title, snippet, score}]
    """
    vector = (await llm_client.embed([question]))[0]
    vec_str = "[" + ",".join(f"{v:.6f}" for v in vector) + "]"

    rows = await asyncio.to_thread(_query, vec_str, space_id, top_k or settings.kb_rag_top_k)

    return [
        {
            "chunkId": chunk_id,
            "itemId": item_id,
            "title": title,
            "snippet": chunk_text[:200],
            "score": round(float(score), 4),
        }
        for chunk_id, chunk_text, item_id, title, score in rows
    ]


def to_json(value) -> str:
    return json.dumps(value, ensure_ascii=False)
