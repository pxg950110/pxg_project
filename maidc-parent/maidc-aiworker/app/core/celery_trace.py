"""Celery 任务的链路上下文继承：
优先从 Celery 消息 headers（X-Trace-Id）、kwargs、或首个 MaidcMessage 形态的 dict
参数中恢复 traceId，缺失时按统一规则生成，任务结束后清理。
废除历史上伪造 traceId（如 eval-{id}）的做法。
"""
from celery.signals import task_postrun, task_prerun

from app.core.trace import (
    generate_trace_id,
    normalize_trace_id,
    set_trace_id,
)


@task_prerun.connect
def _restore_trace(task=None, args=None, kwargs=None, **_):
    headers = getattr(getattr(task, "request", None), "headers", None) or {}
    trace_id = normalize_trace_id(headers.get("X-Trace-Id") or headers.get("traceId"))
    if not trace_id and kwargs:
        trace_id = normalize_trace_id(kwargs.get("traceId") or kwargs.get("trace_id"))
    if not trace_id and args:
        first = args[0]
        if isinstance(first, dict):
            trace_id = normalize_trace_id(first.get("traceId"))
    set_trace_id(trace_id or generate_trace_id())


@task_postrun.connect
def _clear_trace(**_):
    set_trace_id("")
