package com.maidc.gateway.filter;

import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Locale;
import java.util.UUID;

/**
 * 网关是 HTTP 流量 traceId 的唯一规范生成点：入站 X-Trace-Id 合法则沿用（规范化为
 * 32 位小写 hex，兼容前端带横线的 UUID），否则生成新值；携带该头的请求对象必须
 * 传入过滤器链向下游转发，响应头回写同一 traceId。格式须与 common-log 的
 * TraceIds 保持一致（网关为 WebFlux，不依赖 common 模块，故就地实现）。
 */
@Component
public class TraceFilter implements GlobalFilter, Ordered {

    private static final String TRACE_ID = "traceId";
    private static final String TRACE_HEADER = "X-Trace-Id";
    private static final int MAX_LENGTH = 64;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = resolveTraceId(exchange.getRequest().getHeaders().getFirst(TRACE_HEADER));

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(exchange.getRequest().mutate().header(TRACE_HEADER, traceId).build())
                .build();
        if (!exchange.getResponse().getHeaders().containsKey(TRACE_HEADER)) {
            exchange.getResponse().getHeaders().add(TRACE_HEADER, traceId);
        }

        MDC.put(TRACE_ID, traceId);
        return chain.filter(mutatedExchange)
                .doFinally(signalType -> {
                    // WebFlux 下 doFinally 可能运行在其它事件循环线程，
                    // 仅在值仍匹配时清理，避免误清并发请求的上下文
                    if (traceId.equals(MDC.get(TRACE_ID))) {
                        MDC.remove(TRACE_ID);
                    }
                });
    }

    private String resolveTraceId(String inbound) {
        if (inbound != null) {
            String normalized = inbound.trim().replace("-", "").toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty() && normalized.length() <= MAX_LENGTH && isHex(normalized)) {
                return normalized;
            }
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    private boolean isHex(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c < '0' || c > '9') && (c < 'a' || c > 'f') && (c < 'A' || c > 'F')) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
