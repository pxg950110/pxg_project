package com.maidc.data.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maidc.data.entity.DiseaseKbItemEntity;
import com.maidc.data.entity.DiseaseKbQaMessageEntity;
import com.maidc.data.repository.DiseaseKbItemRepository;
import com.maidc.data.repository.DiseaseKbQaMessageRepository;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ai-worker 编排：用 JDK 内置 HttpServer 桩模拟 /llm/summary、/embedding、/rag/chat，
 * 验证摘要回写、失败标记 FAILED、SSE 透传与 ASSISTANT 消息落库。
 */
@ExtendWith(MockitoExtension.class)
class DiseaseKnowledgeAiServiceTest {

    @Mock
    private DiseaseKbItemRepository itemRepository;
    @Mock
    private DiseaseKbQaMessageRepository qaMessageRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private DiseaseKnowledgeAiService aiService;
    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void setUp() throws Exception {
        aiService = new DiseaseKnowledgeAiService(itemRepository, qaMessageRepository, jdbcTemplate, new ObjectMapper());
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        ReflectionTestUtils.setField(aiService, "aiWorkerBaseUrl", baseUrl);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private void respond(int status, String body) {
        server.createContext("/", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
    }

    private static DiseaseKbItemEntity publishedItem() {
        DiseaseKbItemEntity item = new DiseaseKbItemEntity();
        item.setId(1L);
        item.setSpaceId(5L);
        item.setItemType("GUIDELINE");
        item.setTitle("中国2型糖尿病防治指南");
        item.setContent("正文内容");
        item.setStatus("PUBLISHED");
        return item;
    }

    // ==================== 摘要回写 ====================

    @Test
    void summarize_ok_writesBackDone() {
        respond(200, "{\"summary\":\"推荐二甲双胍作为一线治疗\",\"extract\":{\"key_recommendations\":[\"x\"]}}");
        DiseaseKbItemEntity item = publishedItem();
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        aiService.summarizeBestEffort(1L);

        ArgumentCaptor<DiseaseKbItemEntity> captor = ArgumentCaptor.forClass(DiseaseKbItemEntity.class);
        verify(itemRepository).save(captor.capture());
        assertEquals("DONE", captor.getValue().getAiStatus());
        assertEquals("推荐二甲双胍作为一线治疗", captor.getValue().getAiSummary());
        assertNotNull(captor.getValue().getAiExtract());
    }

    @Test
    void summarize_aiWorkerDown_marksFailed() {
        ReflectionTestUtils.setField(aiService, "aiWorkerBaseUrl", "http://127.0.0.1:1"); // 不可达端口
        when(itemRepository.findById(1L)).thenReturn(Optional.of(publishedItem()));
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        aiService.summarizeBestEffort(1L);

        ArgumentCaptor<DiseaseKbItemEntity> captor = ArgumentCaptor.forClass(DiseaseKbItemEntity.class);
        verify(itemRepository, atLeastOnce()).save(captor.capture());
        assertEquals("FAILED", captor.getValue().getAiStatus());
    }

    // ==================== 分块向量化 ====================

    @Test
    void summarize_publishedItem_indexesChunks() {
        server.createContext("/llm/summary", exchange -> respondString(exchange, 200,
                "{\"summary\":\"s\",\"extract\":{}}"));
        server.createContext("/embedding", exchange -> respondString(exchange, 200,
                "{\"vectors\":[[0.1,0.2],[0.3,0.4]]}"));
        DiseaseKbItemEntity item = publishedItem();
        item.setContent("x".repeat(900)); // 2 chunks
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        aiService.summarizeBestEffort(1L);

        verify(jdbcTemplate).update(eq("DELETE FROM cdr.c_disease_kb_item_chunk WHERE item_id = ?"), eq(1L));
        verify(jdbcTemplate, times(2)).update(
                eq("INSERT INTO cdr.c_disease_kb_item_chunk (item_id, chunk_index, chunk_text, tokens, embedding) "
                        + "VALUES (?, ?, ?, ?, ?::vector)"),
                eq(1L), anyInt(), anyString(), anyInt(), anyString());
    }

    private static void respondString(com.sun.net.httpserver.HttpExchange exchange, int status, String body) {
        try {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (Exception ignored) {
        }
    }

    // ==================== SSE 问答透传 ====================

    @Test
    void streamAnswer_deltasCollected_assistantMessageSaved() throws Exception {
        String sse = "data:{\"type\":\"citations\",\"citations\":[{\"itemId\":1,\"title\":\"指南\"}]}\n\n"
                + "data:{\"type\":\"delta\",\"text\":\"根据\"}\n\n"
                + "data:{\"type\":\"delta\",\"text\":\"指南…\"}\n\n"
                + "data:{\"type\":\"done\"}\n\n";
        server.createContext("/rag/chat", exchange -> {
            byte[] bytes = sse.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        when(qaMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        aiService.streamAnswer(5L, 2L, "一线用药原则？");

        // SseEmitter 无容器上下文时 complete() 不触发回调，改用 Mockito 轮询验证落库
        ArgumentCaptor<DiseaseKbQaMessageEntity> captor = ArgumentCaptor.forClass(DiseaseKbQaMessageEntity.class);
        verify(qaMessageRepository, timeout(10_000)).save(captor.capture());
        DiseaseKbQaMessageEntity msg = captor.getValue();
        assertEquals("ASSISTANT", msg.getRole());
        assertEquals("根据指南…", msg.getContent());
        assertNotNull(msg.getCitations());
        assertEquals(1, msg.getCitations().size());
        assertEquals(1L, msg.getCitations().get(0).path("itemId").asLong());
    }

    @Test
    void streamAnswer_errorFrame_nothingSaved() throws Exception {
        String sse = "data:{\"type\":\"error\",\"message\":\"LLM 未配置，AI 问答不可用\"}\n\n";
        server.createContext("/rag/chat", exchange -> {
            byte[] bytes = sse.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        aiService.streamAnswer(5L, 2L, "问题");

        // 本地桩即时完成：短暂等待后确认未落任何消息
        Thread.sleep(1500);
        verify(qaMessageRepository, never()).save(any(DiseaseKbQaMessageEntity.class));
    }

    @Test
    void streamAnswer_connectionRefused_emitsError_nothingSaved() throws Exception {
        ReflectionTestUtils.setField(aiService, "aiWorkerBaseUrl", "http://127.0.0.1:1");

        aiService.streamAnswer(5L, 2L, "问题");

        Thread.sleep(1500);
        verify(qaMessageRepository, never()).save(any(DiseaseKbQaMessageEntity.class));
    }
}
