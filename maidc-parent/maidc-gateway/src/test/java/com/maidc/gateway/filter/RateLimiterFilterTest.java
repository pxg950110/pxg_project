package com.maidc.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RateLimiterFilter.
 */
class RateLimiterFilterTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RateLimiterFilter filter;
    private GatewayFilterChain chain;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        filter = new RateLimiterFilter(redisTemplate);
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void withinLimit_passesThrough() {
        // Current count is 5, under the 100 limit
        when(valueOperations.get(anyString())).thenReturn("5");
        when(valueOperations.increment(anyString())).thenReturn(6L);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/data/datasets")
                        .remoteAddress(new InetSocketAddress("192.168.1.1", 12345))
                        .build());

        Mono<Void> result = filter.filter(exchange, chain);

        StepVerifier.create(result).verifyComplete();
        verify(chain).filter(any());
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    void exceedsLimit_returns429() {
        // Current count is 100, at the limit
        when(valueOperations.get(contains("ratelimit"))).thenReturn("100");

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/data/datasets")
                        .remoteAddress(new InetSocketAddress("192.168.1.1", 12345))
                        .build());

        Mono<Void> result = filter.filter(exchange, chain);

        StepVerifier.create(result).verifyComplete();
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void firstRequest_setsExpiry() {
        // No prior count
        when(valueOperations.get(anyString())).thenReturn(null);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/data/datasets")
                        .remoteAddress(new InetSocketAddress("10.0.0.1", 8080))
                        .build());

        Mono<Void> result = filter.filter(exchange, chain);

        StepVerifier.create(result).verifyComplete();
        verify(redisTemplate).expire(anyString(), eq(Duration.ofSeconds(60)));
    }

    @Test
    void redisException_passesThrough() {
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis down"));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/data/datasets")
                        .remoteAddress(new InetSocketAddress("192.168.1.1", 12345))
                        .build());

        Mono<Void> result = filter.filter(exchange, chain);

        StepVerifier.create(result).verifyComplete();
        verify(chain).filter(any());
    }

    @Test
    void orderIsHighestPrecedencePlus2() {
        assertEquals(org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 2, filter.getOrder());
    }
}
