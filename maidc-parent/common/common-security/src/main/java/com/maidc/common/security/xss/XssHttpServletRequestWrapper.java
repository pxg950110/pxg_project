package com.maidc.common.security.xss;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.Set;

/**
 * XSS 过滤请求包装器
 * 对请求参数值进行 HTML 转义
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    /**
     * 标准 HTTP 请求头，不应被 HTML 转义
     */
    private static final Set<String> SKIP_ESCAPE_HEADERS = Set.of(
            "origin", "host", "referer", "content-type", "accept",
            "accept-encoding", "accept-language", "connection",
            "user-agent", "authorization", "cookie", "x-forwarded-for",
            "x-forwarded-proto", "x-real-ip"
    );

    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return value != null ? XssProtectionUtils.escapeHtml(value) : null;
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) {
            return null;
        }
        String[] escaped = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            escaped[i] = values[i] != null ? XssProtectionUtils.escapeHtml(values[i]) : null;
        }
        return escaped;
    }

    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        if (value == null) {
            return null;
        }
        // 跳过标准 HTTP 头，只对自定义头做 XSS 转义
        if (name != null && SKIP_ESCAPE_HEADERS.contains(name.toLowerCase())) {
            return value;
        }
        return XssProtectionUtils.escapeHtml(value);
    }
}
