package com.maidc.common.log.filter;

import com.maidc.common.log.trace.TraceIds;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class TraceContextFilterTest {

    private final TraceContextFilter filter = new TraceContextFilter();

    @AfterEach
    void cleanUp() {
        MDC.clear();
    }

    @Test
    void shouldInjectTraceAndUserContextIntoMdcDuringChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIds.TRACE_HEADER, "550E8400-E29B-41D4-A716-446655440000");
        request.addHeader(TraceIds.USER_ID_HEADER, "42");
        request.addHeader(TraceIds.ORG_ID_HEADER, "7");
        request.addHeader(TraceIds.USERNAME_HEADER, "alice");

        MdcSnapshot insideChain = new MdcSnapshot();
        FilterChain chain = (req, res) -> {
            insideChain.traceId = MDC.get(TraceIds.MDC_TRACE_ID);
            insideChain.userId = MDC.get(TraceIds.MDC_USER_ID);
            insideChain.orgId = MDC.get(TraceIds.MDC_ORG_ID);
            insideChain.username = MDC.get(TraceIds.MDC_USERNAME);
        };

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(insideChain.traceId).isEqualTo("550e8400e29b41d4a716446655440000");
        assertThat(insideChain.userId).isEqualTo("42");
        assertThat(insideChain.orgId).isEqualTo("7");
        assertThat(insideChain.username).isEqualTo("alice");
        // 请求结束后清理，不污染线程池复用线程
        assertThat(MDC.get(TraceIds.MDC_TRACE_ID)).isNull();
        assertThat(MDC.get(TraceIds.MDC_USER_ID)).isNull();
    }

    @Test
    void shouldGenerateTraceIdWhenHeaderMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();

        MdcSnapshot insideChain = new MdcSnapshot();
        FilterChain chain = (req, res) ->
                insideChain.traceId = MDC.get(TraceIds.MDC_TRACE_ID);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(insideChain.traceId).matches("^[0-9a-f]{32}$");
        assertThat(MDC.get(TraceIds.MDC_TRACE_ID)).isNull();
    }

    @Test
    void shouldCleanUpWhenChainThrows() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIds.TRACE_HEADER, TraceIds.generate());

        FilterChain chain = (req, res) -> {
            throw new IllegalStateException("boom");
        };

        try {
            filter.doFilter(request, new MockHttpServletResponse(), chain);
        } catch (IllegalStateException expected) {
            // ignore
        }
        assertThat(MDC.get(TraceIds.MDC_TRACE_ID)).isNull();
    }

    private static class MdcSnapshot {
        String traceId;
        String userId;
        String orgId;
        String username;
    }
}
