package com.maidc.data.service.connection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
public class HttpConnectionTester implements ConnectionTester {

    @Override
    public String getType() { return "HTTP"; }

    @Override
    public ConnectionTestResult test(Map<String, Object> params) {
        String url = (String) params.get("url");
        String method = (String) params.getOrDefault("method", "GET");
        String authType = (String) params.getOrDefault("auth_type", "NONE");
        String authToken = (String) params.get("auth_token");

        if (url == null || url.isBlank()) {
            return ConnectionTestResult.fail("接口地址不能为空");
        }

        long start = System.currentTimeMillis();
        try {
            var client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            var requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10));

            if ("BEARER".equals(authType) && authToken != null) {
                requestBuilder.header("Authorization", "Bearer " + authToken);
            } else if ("BASIC".equals(authType) && authToken != null) {
                requestBuilder.header("Authorization", "Basic " + authToken);
            }

            if ("POST".equals(method)) {
                requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
            } else {
                requestBuilder.GET();
            }

            HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            int latency = (int) (System.currentTimeMillis() - start);
            int statusCode = response.statusCode();

            if (statusCode >= 200 && statusCode < 400) {
                return ConnectionTestResult.ok(latency, Map.of("statusCode", statusCode));
            } else {
                return ConnectionTestResult.fail("HTTP " + statusCode);
            }
        } catch (Exception e) {
            log.warn("HTTP连接测试失败: {}", e.getMessage());
            return ConnectionTestResult.fail(e.getMessage());
        }
    }
}
