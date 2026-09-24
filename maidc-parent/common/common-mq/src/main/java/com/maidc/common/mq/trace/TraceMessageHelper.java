package com.maidc.common.mq.trace;

import com.maidc.common.mq.model.MaidcMessage;
import org.slf4j.MDC;

import java.util.Locale;
import java.util.UUID;

/**
 * MQ 消费入口的链路上下文还原：消息携带 traceId 则沿用（规范化），否则生成兜底值，
 * 保证消费期间 MDC 中 traceId 非空（下游审计 trace_id 为 NOT NULL，空值会导致违约进 DLQ）。
 * traceId 格式须与 common-log 的 TraceIds 保持一致（32 位小写 hex，上限 64 字符）。
 */
public final class TraceMessageHelper {

    private static final int MAX_LENGTH = 64;

    private TraceMessageHelper() {
    }

    /**
     * 还原 MDC 并返回本次写入的 traceId，供消费结束后传给 {@link #clear(String)}。
     */
    public static String restore(MaidcMessage message) {
        String traceId = normalize(message != null ? message.getTraceId() : null);
        if (traceId == null) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put("traceId", traceId);
        return traceId;
    }

    /**
     * 值匹配才清理，避免误清线程上其它任务的上下文。
     */
    public static void clear(String traceId) {
        if (traceId != null && traceId.equals(MDC.get("traceId"))) {
            MDC.remove("traceId");
        }
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().replace("-", "").toLowerCase(Locale.ROOT);
        if (value.isEmpty() || value.length() > MAX_LENGTH || !isHex(value)) {
            return null;
        }
        return value;
    }

    private static boolean isHex(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c < '0' || c > '9') && (c < 'a' || c > 'f') && (c < 'A' || c > 'F')) {
                return false;
            }
        }
        return true;
    }
}
