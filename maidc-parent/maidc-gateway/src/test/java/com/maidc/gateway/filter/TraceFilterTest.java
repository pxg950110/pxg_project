package com.maidc.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TraceFilter.
 */
class TraceFilterTest {

    private TraceFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new TraceFilter();
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void addsTraceIdHeader() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/data/datasets").build());

        Mono<Void> result = filter.filter(exchange, chain);

        StepVerifier.create(result).verifyComplete();
        verify(chain).filter(any());

        // Response header should have the trace ID added
        String responseTraceId = exchange.getResponse().getHeaders().getFirst("X-Trace-Id");
        assertNotNull(responseTraceId);
        assertFalse(responseTraceId.isEmpty());
        assertEquals(32, responseTraceId.length()); // UUID without dashes
    }

    @Test
    void preservesExistingTraceId() {
        String existingTraceId = "abc123def456";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/data/datasets")
                        .header("X-Trace-Id", existingTraceId)
                        .build());

        Mono<Void> result = filter.filter(exchange, chain);

        StepVerifier.create(result).verifyComplete();
        verify(chain).filter(any());

        // Should preserve the existing trace ID in the response
        String responseTraceId = exchange.getResponse().getHeaders().getFirst("X-Trace-Id");
        assertEquals(existingTraceId, responseTraceId);
    }

    @Test
    void blankTraceIdIsReplaced() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/data/datasets")
                        .header("X-Trace-Id", "   ")
                        .build());

        Mono<Void> result = filter.filter(exchange, chain);

        StepVerifier.create(result).verifyComplete();

        String responseTraceId = exchange.getResponse().getHeaders().getFirst("X-Trace-Id");
        assertNotNull(responseTraceId);
        assertNotEquals("   ", responseTraceId);
        assertEquals(32, responseTraceId.length());
    }

    @Test
    void orderIsHighestPrecedence() {
        assertEquals(org.springframework.core.Ordered.HIGHEST_PRECEDENCE, filter.getOrder());
    }
}
