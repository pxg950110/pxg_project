package com.maidc.data.controller;

import com.maidc.common.core.result.R;
import com.maidc.common.security.annotation.RequirePermission;
import com.maidc.data.entity.DiseaseKbItemEntity;
import com.maidc.data.entity.DiseaseKbQaSessionEntity;
import com.maidc.data.entity.DiseaseKbSpaceEntity;
import com.maidc.data.service.DiseaseKnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 专病知识库 REST 接口。
 * <p>权限三权限点：cdr:diseasekb:read（浏览/检索）、cdr:diseasekb:manage（空间与条目维护）、cdr:diseasekb:ai（AI 问答）。
 * <p>/qa/ask 为 SSE 流式返回（透传 ai-worker /rag/chat）；ai-worker 或 LLM 不可用时流内发 error 帧。
 */
@RestController
@RequestMapping("/api/v1/cdr/disease-kb")
@RequiredArgsConstructor
public class DiseaseKnowledgeController {

    private final DiseaseKnowledgeService service;

    // ==================== 知识空间 ====================

    @GetMapping("/spaces")
    @RequirePermission("cdr:diseasekb:read")
    public R<Page<DiseaseKbSpaceEntity>> listSpaces(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int page_size) {
        return R.ok(service.listSpaces(keyword, status, page, page_size));
    }

    @PostMapping("/spaces")
    @RequirePermission("cdr:diseasekb:manage")
    public R<DiseaseKbSpaceEntity> createSpace(@RequestBody DiseaseKbSpaceEntity entity) {
        return R.ok(service.createSpace(entity));
    }

    @GetMapping("/spaces/{id}")
    @RequirePermission("cdr:diseasekb:read")
    public R<Map<String, Object>> getSpace(@PathVariable Long id) {
        return R.ok(service.getSpaceDetail(id));
    }

    @PutMapping("/spaces/{id}")
    @RequirePermission("cdr:diseasekb:manage")
    public R<DiseaseKbSpaceEntity> updateSpace(@PathVariable Long id, @RequestBody DiseaseKbSpaceEntity entity) {
        return R.ok(service.updateSpace(id, entity));
    }

    @DeleteMapping("/spaces/{id}")
    @RequirePermission("cdr:diseasekb:manage")
    public R<Void> deleteSpace(@PathVariable Long id) {
        service.deleteSpace(id);
        return R.ok();
    }

    /** 队列 → 关联知识空间（专病详情页跳转用；未关联返回 null） */
    @GetMapping("/cohorts/{cohortId}/space")
    @RequirePermission("cdr:diseasekb:read")
    public R<DiseaseKbSpaceEntity> getSpaceByCohort(@PathVariable Long cohortId) {
        return R.ok(service.getSpaceByCohort(cohortId));
    }

    // ==================== 知识条目 ====================

    @GetMapping("/spaces/{id}/items")
    @RequirePermission("cdr:diseasekb:read")
    public R<Page<DiseaseKbItemEntity>> listItems(
            @PathVariable Long id,
            @RequestParam(required = false) String item_type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int page_size) {
        return R.ok(service.listItems(id, item_type, status, keyword, page, page_size));
    }

    @PostMapping("/spaces/{id}/items")
    @RequirePermission("cdr:diseasekb:manage")
    public R<DiseaseKbItemEntity> createItem(@PathVariable Long id, @RequestBody DiseaseKbItemEntity entity) {
        return R.ok(service.createItem(id, entity));
    }

    @GetMapping("/items/{id}")
    @RequirePermission("cdr:diseasekb:read")
    public R<DiseaseKbItemEntity> getItem(@PathVariable Long id) {
        return R.ok(service.getItem(id));
    }

    @PutMapping("/items/{id}")
    @RequirePermission("cdr:diseasekb:manage")
    public R<DiseaseKbItemEntity> updateItem(@PathVariable Long id, @RequestBody DiseaseKbItemEntity entity) {
        return R.ok(service.updateItem(id, entity));
    }

    @DeleteMapping("/items/{id}")
    @RequirePermission("cdr:diseasekb:manage")
    public R<Void> deleteItem(@PathVariable Long id) {
        service.deleteItem(id);
        return R.ok();
    }

    @PostMapping("/items/{id}/publish")
    @RequirePermission("cdr:diseasekb:manage")
    public R<DiseaseKbItemEntity> publishItem(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return R.ok(service.publishItem(id, body.get("action")));
    }

    @PostMapping("/items/{id}/file")
    @RequirePermission("cdr:diseasekb:manage")
    public R<DiseaseKbItemEntity> uploadFile(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return R.ok(service.uploadItemFile(id, file));
    }

    @PostMapping("/items/{id}/recompute")
    @RequirePermission("cdr:diseasekb:manage")
    public R<DiseaseKbItemEntity> recompute(@PathVariable Long id) {
        return R.ok(service.recompute(id));
    }

    // ==================== 检索 ====================

    @GetMapping("/search")
    @RequirePermission("cdr:diseasekb:read")
    public R<List<com.maidc.data.dto.DiseaseKbSearchVO>> search(
            @RequestParam String keyword,
            @RequestParam(required = false) Long space_id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int page_size) {
        return R.ok(service.search(keyword, space_id, page, page_size));
    }

    // ==================== AI 问答 ====================

    @GetMapping("/qa/sessions")
    @RequirePermission("cdr:diseasekb:ai")
    public R<List<DiseaseKbQaSessionEntity>> listSessions(@RequestParam Long space_id) {
        return R.ok(service.listSessions(space_id));
    }

    @PostMapping("/qa/sessions")
    @RequirePermission("cdr:diseasekb:ai")
    public R<DiseaseKbQaSessionEntity> createSession(@RequestBody Map<String, Long> body) {
        Long spaceId = body.get("space_id");
        if (spaceId == null) {
            return R.fail(400, "space_id 不能为空");
        }
        return R.ok(service.createSession(spaceId));
    }

    @GetMapping("/qa/sessions/{id}/messages")
    @RequirePermission("cdr:diseasekb:ai")
    public R<?> listMessages(@PathVariable Long id) {
        return R.ok(service.listMessages(id));
    }

    @DeleteMapping("/qa/sessions/{id}")
    @RequirePermission("cdr:diseasekb:ai")
    public R<Void> deleteSession(@PathVariable Long id) {
        service.deleteSession(id);
        return R.ok();
    }

    /** SSE 流式问答：透传 ai-worker /rag/chat（引用随流下发） */
    @PostMapping(value = "/qa/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequirePermission("cdr:diseasekb:ai")
    public SseEmitter ask(@RequestBody Map<String, Object> body) {
        Long sessionId = body.get("session_id") == null ? null : Long.valueOf(body.get("session_id").toString());
        String question = (String) body.get("question");
        return service.ask(sessionId, question);
    }
}
