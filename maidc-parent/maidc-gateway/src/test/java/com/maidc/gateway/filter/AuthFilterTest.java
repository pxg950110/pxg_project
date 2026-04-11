package com.maidc.gateway.filter;

import io.jsonwebtoken.Jwts;
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

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static io.jsonwebtoken.security.Keys.hmacShaKeyFor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthFilter.
 */
class AuthFilterTest {

    private static final String SECRET = "maidc-secret-key-for-jwt-token-generation-must-be-at-least-256-bits";
    private static final SecretKey TEST_KEY = hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private AuthFilter authFilter;
    private GatewayFilterChain chain;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // Default: token is not blacklisted
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        authFilter = new AuthFilter(SECRET, redisTemplate);
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void whiteListPath_login_passesThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/auth/login").build());

        Mono<Void> result = authFilter.filter(exchange, chain);

        StepVerifier.create(result).verifyComplete();
        verify(chain).filter(any());
    }

    @Test
    void whiteListPath_refresh_passesThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/auth/refresh").build());

        Mono<Void> result = authFilter.filter(exchange, chain);

        StepVerifier.create(result).verifyComplete();
        verify(chain).filter(any());
    }

    @Test
    void whiteListPath_captcha_passesThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/auth/captcha").build());

        Mono<Void> result = authFilter.filter(exchange, chain);

        StepVerifier.create(result).verifyComplete();
        verify(chain).filter(any());
    }

    @Test
    void missingAuthHeader_returns401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/some/protected").build());

        Mono<Void> result = authFilter.filter(exchange, chain);

        StepVerifier.create(result).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void invalidBearerToken_returns401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/some/protected")
                        .header("Authorization", "Bearer invalid-token")
                        .build());

        Mono<Void> result = authFilter.filter(exchange, chain);

        StepVerifier.create(result).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void validToken_passesThroughWithHeaders() {
        // Build a real JWT with the test secret
        String token = Jwts.builder()
                .subject("testuser")
                .claim("userId", "1001")
                .claim("orgId", "2001")
                .signWith(TEST_KEY)
                .compact();

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/data/datasets")
                        .header("Authorization", "Bearer " + token)
                        .build());

        Mono<Void> result = authFilter.filter(exchange, chain);

        StepVerifier.create(result).verifyComplete();
        verify(chain).filter(any());
    }

    @Test
    void orderIsHighestPrecedencePlus1() {
        assertEquals(org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 1, authFilter.getOrder());
    }
}
