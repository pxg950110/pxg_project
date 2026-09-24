package com.maidc.common.log.web;

import com.maidc.common.log.trace.TraceIds;
import org.slf4j.MDC;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

/**
 * 服务间 HTTP 调用自动透传链路上下文：经 RestTemplateBuilder 构建的所有
 * RestTemplate 都会自动携带当前 MDC 中的 traceId 与用户上下文头，调用方零改动。
 */
public class TraceRestTemplateCustomizer implements RestTemplateCustomizer {

    @Override
    public void customize(RestTemplate restTemplate) {
        restTemplate.getInterceptors().add(new TraceHeaderInterceptor());
    }

    static class TraceHeaderInterceptor implements ClientHttpRequestInterceptor {

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                            ClientHttpRequestExecution execution) throws IOException {
            addIfAbsent(request, TraceIds.TRACE_HEADER, MDC.get(TraceIds.MDC_TRACE_ID));
            addIfAbsent(request, TraceIds.USER_ID_HEADER, MDC.get(TraceIds.MDC_USER_ID));
            addIfAbsent(request, TraceIds.ORG_ID_HEADER, MDC.get(TraceIds.MDC_ORG_ID));
            addIfAbsent(request, TraceIds.USERNAME_HEADER, MDC.get(TraceIds.MDC_USERNAME));
            return execution.execute(request, body);
        }

        private void addIfAbsent(HttpRequest request, String header, String value) {
            if (value != null && !value.isBlank() && !request.getHeaders().containsKey(header)) {
                request.getHeaders().set(header, value);
            }
        }
    }
}
