package com.maidc.common.core.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

/**
 * 统一响应体
 *
 * @param <T> 响应数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> {

    private int code;
    private String message;
    private T data;
    private String traceId;

    /**
     * 成功响应（带数据）
     */
    public static <T> R<T> ok(T data) {
        return new R<>(200, "success", data, MDC.get("traceId"));
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> R<T> ok() {
        return new R<>(200, "success", null, MDC.get("traceId"));
    }

    /**
     * 失败响应
     */
    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null, MDC.get("traceId"));
    }
}
