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
 * Unit tests for RequestLogFilter.
 */
class RequestLogFilterTest {

    private RequestLogFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new RequestLogFilter();
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void logsRequest_completesSuccessfully() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/data/datasets")
                        .header("X-Trace-Id", "test-trace-123")
                        .build());

        Mono<Void> result = filter.filter(exchange, chain);

        StepVerifier.create(result).verifyComplete();
        verify(chain).filter(any());

        // Verify start time attribute was set
        Long startTime = exchange.getAttribute("requestStartTime");
        assertNotNull(startTime);
        assertTrue(startTime > 0);
    }

    @Test
    void logsPostRequest_completesSuccessfully() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/auth/login")
                        .build());

        Mono<Void> result = filter.filter(exchange, chain);

        StepVerifier.create(result).verifyComplete();
        verify(chain).filter(any());
        assertNotNull(exchange.getAttribute("requestStartTime"));
    }

    @Test
    void orderIsLowestPrecedence() {
        assertEquals(org.springframework.core.Ordered.LOWEST_PRECEDENCE, filter.getOrder());
    }
}
