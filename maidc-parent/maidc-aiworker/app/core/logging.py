"""统一日志配置：console 输出携带 traceId/userId，语义对齐 Java 侧
nacos 共享配置 maidc-shared.yaml 的日志 pattern（%X{traceId}/%X{userId}）。
"""
import logging.config

from app.core.trace import TraceIdFilter


def setup_logging(level: int = logging.INFO) -> None:
    logging.config.dictConfig({
        "version": 1,
        "disable_existing_loggers": False,
        "filters": {
            "trace_id": {"()": TraceIdFilter},
        },
        "formatters": {
            "console": {
                "format": "[%(asctime)s] [traceId:%(traceId)s] [userId:%(userId)s] %(levelname)s %(name)s - %(message)s",  # noqa: E501
                "datefmt": "%Y-%m-%d %H:%M:%S",
            },
        },
        "handlers": {
            "console": {
                "class": "logging.StreamHandler",
                "formatter": "console",
                "filters": ["trace_id"],
            },
        },
        "loggers": {
            "uvicorn": {"handlers": ["console"], "level": level, "propagate": False},
            "uvicorn.error": {"handlers": ["console"], "level": level, "propagate": False},
            "uvicorn.access": {"handlers": ["console"], "level": logging.WARNING, "propagate": False},
        },
        "root": {"handlers": ["console"], "level": level},
    })
