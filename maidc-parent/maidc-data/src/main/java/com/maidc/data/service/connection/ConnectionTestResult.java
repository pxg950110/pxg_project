package com.maidc.data.service.connection;

import java.util.Map;

public record ConnectionTestResult(
    boolean success,
    String message,
    int latencyMs,
    Map<String, Object> details
) {
    public static ConnectionTestResult ok(int latencyMs) {
        return new ConnectionTestResult(true, "连接成功", latencyMs, Map.of());
    }
    public static ConnectionTestResult ok(int latencyMs, Map<String, Object> details) {
        return new ConnectionTestResult(true, "连接成功", latencyMs, details);
    }
    public static ConnectionTestResult fail(String message) {
        return new ConnectionTestResult(false, message, -1, Map.of());
    }
}
