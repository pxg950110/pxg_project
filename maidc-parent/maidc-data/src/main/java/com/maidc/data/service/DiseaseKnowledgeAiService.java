package com.maidc.data.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.maidc.data.entity.DiseaseKbItemEntity;
import com.maidc.data.entity.DiseaseKbQaMessageEntity;
import com.maidc.data.repository.DiseaseKbItemRepository;
import com.maidc.data.repository.DiseaseKbQaMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;

/**
 * ai-worker（maidc-aiworker，FastAPI :8090）编排：摘要/知识抽取 + 分块向量化 + RAG 流式问答。
 * <p>契约（app/api/llm.py）：
 * <pre>
 * POST /llm/summary  {"text":..., "item_type":...} → {"summary":..., "extract":{...}}
 * POST /embedding    {"texts":[...]}               → {"vectors":[[...]]}
 * POST /rag/chat     {"space_id":..., "session_id":..., "question":...}
 *                   → SSE，每帧一行 data:{"type":"delta"|"citations"|"done"|"error", ...}
 * </pre>
 * 所有调用失败仅降级（条目 ai_status=FAILED / SSE 发 error 事件），绝不阻塞内容管理主流程。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiseaseKnowledgeAiService {

    private final DiseaseKbItemRepository itemRepository;
    private final DiseaseKbQaMessageRepository qaMessageRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Value("${diseasekb.ai-worker.base-url:http://localhost:8090}")
    private String aiWorkerBaseUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    // ==================== 摘要 / 知识抽取 / 分块向量化 ====================

    /** best-effort：失败只标记 ai_status=FAILED，不抛出（发布/编辑/重算路径均安全调用） */
    public void summarizeBestEffort(Long itemId) {
        try {
            summarizeNow(itemId);
            indexBestEffort(itemId);
        } catch (Exception e) {
            log.warn("KB item {} AI summarize failed: {}", itemId, e.getMessage());
            itemRepository.findById(itemId).ifPresent(item -> {
                item.setAiStatus("FAILED");
                itemRepository.save(item);
            });
        }
    }

    private void summarizeNow(Long itemId) throws IOException, InterruptedException {
        DiseaseKbItemEntity item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("item not found: " + itemId));
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("text", joinNonNull(item.getTitle(), item.getSummary(), item.getContent(), item.getAiSummary()));
        payload.put("item_type", item.getItemType());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(aiWorkerBaseUrl + "/llm/summary"))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("ai-worker /llm/summary HTTP " + response.statusCode());
        }
        JsonNode body = objectMapper.readTree(response.body());
        item.setAiSummary(body.path("summary").asText(null));
        if (body.has("extract") && !body.path("extract").isNull()) {
            item.setAiExtract(body.get("extract"));
        }
        item.setAiStatus("DONE");
        itemRepository.save(item);
    }

    /** 分块 → /embedding → 重写 c_disease_kb_item_chunk（派生数据，硬删重建）；失败仅告警 */
    void indexBestEffort(Long itemId) {
        try {
            indexNow(itemId);
        } catch (Exception e) {
            log.warn("KB item {} chunk indexing failed: {}", itemId, e.getMessage());
        }
    }

    private static final int CHUNK_CHARS = 800;
    private static final int CHUNK_OVERLAP = 100;

    private void indexNow(Long itemId) throws IOException, InterruptedException {
        DiseaseKbItemEntity item = itemRepository.findById(itemId).orElse(null);
        if (item == null || !"PUBLISHED".equals(item.getStatus())) {
            return;
        }
        List<String> chunks = splitChunks(joinNonNull(item.getTitle(), item.getAiSummary(), item.getContent()));
        jdbcTemplate.update("DELETE FROM cdr.c_disease_kb_item_chunk WHERE item_id = ?", itemId);
        if (chunks.isEmpty()) {
            return;
        }
        ObjectNode payload = objectMapper.createObjectNode();
        payload.set("texts", objectMapper.valueToTree(chunks));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(aiWorkerBaseUrl + "/embedding"))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("ai-worker /embedding HTTP " + response.statusCode());
        }
        JsonNode vectors = objectMapper.readTree(response.body()).path("vectors");
        if (!vectors.isArray() || vectors.size() != chunks.size()) {
            throw new IllegalStateException("embedding 数量不匹配: " + vectors.size() + "/" + chunks.size());
        }
        for (int i = 0; i < chunks.size(); i++) {
            String vecStr = vectors.get(i).toString();
            jdbcTemplate.update(
                    "INSERT INTO cdr.c_disease_kb_item_chunk (item_id, chunk_index, chunk_text, tokens, embedding) "
                            + "VALUES (?, ?, ?, ?, ?::vector)",
                    itemId, i, chunks.get(i), chunks.get(i).length() / 2, vecStr);
        }
        log.info("KB item {} indexed {} chunks", itemId, chunks.size());
    }

    private static List<String> splitChunks(String text) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return result;
        }
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + CHUNK_CHARS, text.length());
            result.add(text.substring(start, end));
            if (end == text.length()) {
                break;
            }
            start = end - CHUNK_OVERLAP;
        }
        return result;
    }

    // ==================== RAG 流式问答（SSE 透传） ====================

    public SseEmitter streamAnswer(Long spaceId, Long sessionId, String question) {
        SseEmitter emitter = new SseEmitter(300_000L);
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("space_id", spaceId);
        if (sessionId != null) {
            payload.put("session_id", sessionId);
        }
        payload.put("question", question);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(aiWorkerBaseUrl + "/rag/chat"))
                .timeout(Duration.ofSeconds(280))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        AnswerCollector collector = new AnswerCollector(emitter, sessionId);
        httpClient.sendAsync(request, BodyHandlers.fromLineSubscriber(collector))
                .whenComplete((resp, err) -> {
                    if (err != null) {
                        collector.fail("AI 服务连接失败，请稍后重试");
                    } else if (resp.statusCode() != 200) {
                        collector.fail("AI 服务返回 " + resp.statusCode() + "，请稍后重试");
                    }
                });
        return emitter;
    }

    /**
     * 逐行收集 ai-worker SSE 帧并转发给前端；
     * 正常结束落 ASSISTANT 消息（含 citations），异常只发 error 事件不落库。
     */
    private final class AnswerCollector implements Flow.Subscriber<String> {

        private final SseEmitter emitter;
        private final Long sessionId;
        private final StringBuilder frame = new StringBuilder();
        private final StringBuilder answer = new StringBuilder();
        private JsonNode citations;
        private boolean errored;

        private AnswerCollector(SseEmitter emitter, Long sessionId) {
            this.emitter = emitter;
            this.sessionId = sessionId;
        }

        @Override
        public void onSubscribe(Flow.Subscription s) {
            s.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(String line) {
            if (errored) return;
            try {
                if (line.startsWith("data:")) {
                    frame.append(line, 5, line.length());
                } else if (line.isEmpty() && frame.length() > 0) {
                    dispatch(frame.toString());
                    frame.setLength(0);
                }
            } catch (Exception e) {
                fail("AI 响应解析失败");
            }
        }

        private void dispatch(String json) throws IOException {
            JsonNode node = objectMapper.readTree(json);
            switch (node.path("type").asText()) {
                case "delta" -> {
                    answer.append(node.path("text").asText());
                    send(node);
                }
                case "citations" -> {
                    citations = node.path("citations");
                    send(node);
                }
                case "error" -> {
                    errored = true;
                    send(node);
                    emitter.complete();
                }
                case "done" -> send(node);
                default -> { /* 未知事件类型：透传忽略 */ }
            }
        }

        @Override
        public void onError(Throwable throwable) {
            fail("AI 服务连接中断");
        }

        @Override
        public void onComplete() {
            if (errored) return;
            if (frame.length() > 0) {
                try {
                    dispatch(frame.toString());
                } catch (Exception ignored) {
                }
            }
            if (answer.length() == 0 && citations == null) {
                fail("AI 未返回有效回答，请稍后重试");
                return;
            }
            DiseaseKbQaMessageEntity msg = new DiseaseKbQaMessageEntity();
            msg.setSessionId(sessionId);
            msg.setRole("ASSISTANT");
            msg.setContent(answer.toString());
            msg.setCitations(citations);
            qaMessageRepository.save(msg);
            try {
                emitter.send(SseEmitter.event().data("{\"type\":\"done\"}"));
                emitter.complete();
            } catch (IOException e) {
                log.warn("SSE complete send failed: {}", e.getMessage());
            }
        }

        void fail(String message) {
            if (errored) return;
            errored = true;
            try {
                ObjectNode err = objectMapper.createObjectNode();
                err.put("type", "error");
                err.put("message", message);
                emitter.send(SseEmitter.event().data(err.toString()));
            } catch (IOException ignored) {
            } finally {
                emitter.complete();
            }
        }

        private void send(JsonNode node) throws IOException {
            emitter.send(SseEmitter.event().data(node.toString()));
        }
    }

    private static String joinNonNull(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                if (sb.length() > 0) sb.append('\n');
                sb.append(p);
            }
        }
        return sb.toString();
    }
}
