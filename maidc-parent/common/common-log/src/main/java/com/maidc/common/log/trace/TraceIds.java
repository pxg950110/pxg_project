package com.maidc.common.log.trace;

import java.util.Locale;
import java.util.UUID;

/**
 * traceId 生成与规范化的唯一实现。全链路各入口（网关、Servlet 过滤器、MQ 消费、
 * Python aiworker）必须保持同一格式：规范形态为 32 位小写 hex，长度上限 64（对齐
 * 审计表 trace_id 列宽）。规则：有值且合法则沿用（规范化），否则生成。
 */
public final class TraceIds {

    public static final String TRACE_HEADER = "X-Trace-Id";
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String ORG_ID_HEADER = "X-Org-Id";
    public static final String USERNAME_HEADER = "X-Username";

    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_USER_ID = "userId";
    public static final String MDC_ORG_ID = "orgId";
    public static final String MDC_USERNAME = "username";

    private static final int MAX_LENGTH = 64;

    private TraceIds() {
    }

    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 规范化为小写 hex 并去掉 UUID 横线；空值、超长或含非 hex 字符时返回 null。
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().replace("-", "").toLowerCase(Locale.ROOT);
        if (value.isEmpty() || value.length() > MAX_LENGTH || !isHex(value)) {
            return null;
        }
        return value;
    }

    /**
     * 链路入口统一采用：入站 traceId 合法则规范化沿用，否则生成新值。
     */
    public static String resolve(String raw) {
        String normalized = normalize(raw);
        return normalized != null ? normalized : generate();
    }

    public static boolean isValid(String raw) {
        return normalize(raw) != null;
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
