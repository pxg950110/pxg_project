package com.maidc.common.log.filter;

import com.maidc.common.log.trace.TraceIds;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 服务入站请求统一注入链路上下文（traceId/userId/orgId/username → MDC），
 * 使现有日志 pattern 的 %X 占位符与 R.traceId、OperationLogAspect 生效。
 * 请求结束仅清理本过滤器写入的键，避免污染线程池中复用的线程。
 */
public class TraceContextFilter extends OncePerRequestFilter implements Ordered {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        MDC.put(TraceIds.MDC_TRACE_ID, TraceIds.resolve(request.getHeader(TraceIds.TRACE_HEADER)));
        putIfPresent(TraceIds.MDC_USER_ID, request.getHeader(TraceIds.USER_ID_HEADER));
        putIfPresent(TraceIds.MDC_ORG_ID, request.getHeader(TraceIds.ORG_ID_HEADER));
        putIfPresent(TraceIds.MDC_USERNAME, request.getHeader(TraceIds.USERNAME_HEADER));
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TraceIds.MDC_TRACE_ID);
            MDC.remove(TraceIds.MDC_USER_ID);
            MDC.remove(TraceIds.MDC_ORG_ID);
            MDC.remove(TraceIds.MDC_USERNAME);
        }
    }

    private void putIfPresent(String mdcKey, String headerValue) {
        if (headerValue != null && !headerValue.isBlank()) {
            MDC.put(mdcKey, headerValue);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
