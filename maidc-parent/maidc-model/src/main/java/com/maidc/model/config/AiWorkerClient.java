package com.maidc.model.config;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class AiWorkerClient {

    private final RestTemplate restTemplate;

    @Value("${maidc.aiworker.url:http://localhost:8090}")
    private String aiWorkerUrl;

    public AiWorkerClient(RestTemplateBuilder restTemplateBuilder) {
        // RestTemplateBuilder 自动应用 common-log 的 TraceRestTemplateCustomizer，
        // 调用 aiworker 时自动透传 X-Trace-Id 等链路头
        this.restTemplate = restTemplateBuilder.build();
    }

    public JsonNode predict(String endpointUrl, JsonNode input) {
        String url = aiWorkerUrl + "/api/v1/predict";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<JsonNode> request = new HttpEntity<>(input, headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url, HttpMethod.POST, request, JsonNode.class);
        log.info("AI Worker predict response: status={}", response.getStatusCode());
        return response.getBody();
    }
}
