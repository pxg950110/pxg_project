"""链路追踪上下文：traceId 生成/规范化与 HTTP 中间件。

格式必须与 Java 侧 common-log 的 TraceIds 保持一致：
规范形态为 32 位小写 hex，长度上限 64；规则为「有值且合法则沿用（规范化），否则生成」。
"""
import logging
import uuid
from contextvars import ContextVar

from starlette.datastructures import Headers, MutableHeaders

TRACE_HEADER = "X-Trace-Id"
USER_ID_HEADER = "X-User-Id"
MAX_LENGTH = 64

_trace_id: ContextVar[str] = ContextVar("trace_id", default="")
_request_user: ContextVar[str] = ContextVar("request_user", default="")


def generate_trace_id() -> str:
    return uuid.uuid4().hex


def normalize_trace_id(raw) -> str | None:
    if not raw:
        return None
    value = str(raw).strip().replace("-", "").lower()
    if not value or len(value) > MAX_LENGTH or any(c not in "0123456789abcdef" for c in value):
        return None
    return value


def resolve_trace_id(raw) -> str:
    return normalize_trace_id(raw) or generate_trace_id()


def set_trace_id(trace_id: str):
    return _trace_id.set(trace_id or "")


def get_trace_id() -> str:
    return _trace_id.get()


def set_request_user(user_id: str):
    return _request_user.set(user_id or "")


class TraceIdFilter(logging.Filter):
    """把 ContextVar 中的链路上下文注入每条 LogRecord（供日志格式 %(traceId)s 使用）。"""

    def filter(self, record: logging.LogRecord) -> bool:
        record.traceId = _trace_id.get() or "-"
        record.userId = _request_user.get() or "-"
        return True


class TraceContextMiddleware:
    """纯 ASGI 中间件（不用 BaseHTTPMiddleware，避免下游任务上下文复制问题）：
    入站 X-Trace-Id 写入 ContextVar，响应头回写同一 traceId。
    """

    def __init__(self, app):
        self.app = app

    async def __call__(self, scope, receive, send):
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return

        headers = Headers(scope=scope)
        trace_id = resolve_trace_id(headers.get(TRACE_HEADER))
        trace_token = set_trace_id(trace_id)
        set_request_user(headers.get(USER_ID_HEADER, ""))

        async def send_with_trace(message):
            if message["type"] == "http.response.start":
                MutableHeaders(scope=message).append(TRACE_HEADER, trace_id)
            await send(message)

        try:
            await self.app(scope, receive, send_with_trace)
        finally:
            set_trace_id("")
            _trace_id.reset(trace_token)
