package com.maidc.data.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.minio.service.MinioService;
import com.maidc.data.entity.DiseaseCohortEntity;
import com.maidc.data.entity.DiseaseKbItemEntity;
import com.maidc.data.entity.DiseaseKbQaMessageEntity;
import com.maidc.data.entity.DiseaseKbQaSessionEntity;
import com.maidc.data.entity.DiseaseKbSpaceEntity;
import com.maidc.data.repository.DiseaseCohortRepository;
import com.maidc.data.repository.DiseaseKbItemRepository;
import com.maidc.data.repository.DiseaseKbQaMessageRepository;
import com.maidc.data.repository.DiseaseKbQaSessionRepository;
import com.maidc.data.repository.DiseaseKbSpaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 专病知识库：空间 CRUD 唯一名校验、条目状态机、队列联动（含悬空引用）、附件校验、/qa/ask 降级语义
 */
@ExtendWith(MockitoExtension.class)
class DiseaseKnowledgeServiceTest {

    @Mock
    private DiseaseKbSpaceRepository spaceRepository;
    @Mock
    private DiseaseKbItemRepository itemRepository;
    @Mock
    private DiseaseKbQaSessionRepository qaSessionRepository;
    @Mock
    private DiseaseKbQaMessageRepository qaMessageRepository;
    @Mock
    private DiseaseCohortRepository cohortRepository;
    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Mock
    private MinioService minioService;
    @Mock
    private DiseaseKnowledgeAiService aiService;
    @Mock
    private com.maidc.data.service.DiseaseCohortEventService cohortEventService;

    @InjectMocks
    private DiseaseKnowledgeService service;

    // ==================== 空间 ====================

    @Test
    void createSpace_blankName_throws400() {
        DiseaseKbSpaceEntity entity = new DiseaseKbSpaceEntity();
        entity.setName(" ");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createSpace(entity));
        assertEquals(400, ex.getCode());
    }

    @Test
    void createSpace_duplicateName_throws5033() {
        DiseaseKbSpaceEntity entity = space("2型糖尿病", null);
        when(spaceRepository.existsByNameAndIsDeletedFalse("2型糖尿病")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createSpace(entity));
        assertEquals(ErrorCode.KB_SPACE_NAME_DUPLICATED.getCode(), ex.getCode());
    }

    @Test
    void createSpace_cohortNotFound_throws400() {
        DiseaseKbSpaceEntity entity = space("2型糖尿病", 99L);
        when(spaceRepository.existsByNameAndIsDeletedFalse("2型糖尿病")).thenReturn(false);
        when(cohortRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createSpace(entity));
        assertEquals(400, ex.getCode());
    }

    @Test
    void createSpace_ok_orgIdDefaulted() {
        DiseaseKbSpaceEntity entity = space("2型糖尿病", null);
        when(spaceRepository.existsByNameAndIsDeletedFalse("2型糖尿病")).thenReturn(false);
        when(spaceRepository.save(any())).thenReturn(entity);

        DiseaseKbSpaceEntity saved = service.createSpace(entity);

        assertEquals(0L, saved.getOrgId());
        verify(spaceRepository).save(entity);
    }

    @Test
    void updateSpace_notFound_throws5031() {
        when(spaceRepository.findById(5L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateSpace(5L, space("x", null)));
        assertEquals(ErrorCode.KB_SPACE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void getSpaceDetail_withDeletedCohort_showsDeletedNotError() {
        DiseaseKbSpaceEntity entity = space("2型糖尿病", 7L);
        entity.setId(5L);
        when(spaceRepository.findById(5L)).thenReturn(Optional.of(entity));
        when(itemRepository.countBySpaceGroupByType(5L)).thenReturn(List.of());
        when(cohortRepository.findById(7L)).thenReturn(Optional.empty());

        Map<String, Object> detail = service.getSpaceDetail(5L);

        @SuppressWarnings("unchecked")
        Map<String, Object> cohort = (Map<String, Object>) detail.get("cohort");
        assertEquals(Boolean.TRUE, cohort.get("deleted"));
        assertEquals(0L, detail.get("itemTotal"));
    }

    @Test
    void getSpaceByCohort_none_returnsNull() {
        when(spaceRepository.findByCohortIdAndIsDeletedFalse(7L)).thenReturn(Optional.empty());
        assertNull(service.getSpaceByCohort(7L));
    }

    // ==================== 条目 ====================

    @Test
    void createItem_invalidType_throws400() {
        when(spaceRepository.findById(5L)).thenReturn(Optional.of(space("s", null)));
        DiseaseKbItemEntity item = item("指南A", "VIDEO");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createItem(5L, item));
        assertEquals(400, ex.getCode());
    }

    @Test
    void createItem_ok_draftAndPending_aiTriggered() {
        when(spaceRepository.findById(5L)).thenReturn(Optional.of(space("s", null)));
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        DiseaseKbItemEntity item = item("中国2型糖尿病防治指南", "GUIDELINE");

        DiseaseKbItemEntity saved = service.createItem(5L, item);

        assertEquals("DRAFT", saved.getStatus());
        assertEquals("PENDING", saved.getAiStatus());
        assertEquals(5L, saved.getSpaceId());
        verify(aiService).summarizeBestEffort(null); // mock save 未生成 id
    }

    @Test
    void publishItem_publish_setsPublishedAndTriggersAi() {
        DiseaseKbItemEntity item = item("指南A", "GUIDELINE");
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DiseaseKbItemEntity saved = service.publishItem(1L, "PUBLISH");

        assertEquals("PUBLISHED", saved.getStatus());
        verify(aiService).summarizeBestEffort(1L);
    }

    @Test
    void publishItem_archive_keepsAiUntouched() {
        DiseaseKbItemEntity item = item("指南A", "GUIDELINE");
        item.setAiStatus("DONE");
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("ARCHIVED", service.publishItem(1L, "ARCHIVE").getStatus());
        verify(aiService, never()).summarizeBestEffort(anyLong());
    }

    @Test
    void publishItem_invalidAction_throws400() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item("指南A", "GUIDELINE")));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.publishItem(1L, "XX"));
        assertEquals(400, ex.getCode());
    }

    @Test
    void updateItem_contentChanged_resetsAiFieldsAndRetriggers() {
        DiseaseKbItemEntity existing = item("旧标题", "GUIDELINE");
        existing.setAiStatus("DONE");
        existing.setAiSummary("旧摘要");
        when(itemRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DiseaseKbItemEntity input = item("新标题", "GUIDELINE");
        DiseaseKbItemEntity saved = service.updateItem(1L, input);

        assertEquals("新标题", saved.getTitle());
        assertEquals("PENDING", saved.getAiStatus());
        assertNull(saved.getAiSummary());
        verify(aiService).summarizeBestEffort(null);
    }

    @Test
    void updateItem_metadataOnly_keepsAi() {
        DiseaseKbItemEntity existing = item("标题", "GUIDELINE");
        existing.setSummary("摘要");
        existing.setContent("正文");
        existing.setAiStatus("DONE");
        when(itemRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DiseaseKbItemEntity input = item("标题", "GUIDELINE");
        input.setSummary("摘要");
        input.setContent("正文");
        DiseaseKbItemEntity saved = service.updateItem(1L, input);

        assertEquals("DONE", saved.getAiStatus());
        verify(aiService, never()).summarizeBestEffort(anyLong());
    }

    // ==================== 附件 ====================

    @Test
    void uploadFile_wrongExtension_throws400() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item("t", "GUIDELINE")));
        MockMultipartFile file = new MockMultipartFile("file", "virus.exe", "application/octet-stream", new byte[]{1});

        BusinessException ex = assertThrows(BusinessException.class, () -> service.uploadItemFile(1L, file));
        assertEquals(400, ex.getCode());
        verifyNoInteractions(minioService);
    }

    @Test
    void uploadFile_ok_writesBackAndMarksPending() throws Exception {
        DiseaseKbItemEntity item = item("t", "GUIDELINE");
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        MockMultipartFile file = new MockMultipartFile("file", "guide.pdf", "application/pdf", new byte[]{1, 2, 3});
        when(minioService.uploadFile(eq("diseasekb"), anyString(), any(), anyString(), anyLong()))
                .thenReturn("diseasekb/1/x-guide.pdf");

        DiseaseKbItemEntity saved = service.uploadItemFile(1L, file);

        assertEquals("guide.pdf", saved.getFileName());
        assertEquals("PENDING", saved.getAiStatus());
        verify(minioService).uploadFile(eq("diseasekb"), anyString(), any(), anyString(), anyLong());
    }

    // ==================== 检索 / 问答 ====================

    @Test
    void search_blankKeyword_throws400() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.search("  ", null, 1, 20));
        assertEquals(400, ex.getCode());
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void createSession_inactiveSpace_throws400() {
        DiseaseKbSpaceEntity space = space("s", null);
        space.setStatus("INACTIVE");
        when(spaceRepository.findById(5L)).thenReturn(Optional.of(space));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createSession(5L));
        assertEquals(400, ex.getCode());
    }

    @Test
    void ask_savesUserMessage_setsTitle_delegatesSse() {
        DiseaseKbQaSessionEntity session = new DiseaseKbQaSessionEntity();
        session.setSpaceId(5L);
        DiseaseKbSpaceEntity sp = space("s", null);
        sp.setId(5L);
        when(qaSessionRepository.findById(2L)).thenReturn(Optional.of(session));
        when(spaceRepository.findById(5L)).thenReturn(Optional.of(sp));
        when(qaMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        SseEmitter emitter = mock(SseEmitter.class);
        when(aiService.streamAnswer(5L, 2L, "一线用药原则是什么？")).thenReturn(emitter);
        ArgumentCaptor<DiseaseKbQaMessageEntity> msgCaptor = ArgumentCaptor.forClass(DiseaseKbQaMessageEntity.class);

        SseEmitter result = service.ask(2L, "一线用药原则是什么？");

        assertSame(emitter, result);
        verify(qaMessageRepository).save(msgCaptor.capture());
        assertEquals("USER", msgCaptor.getValue().getRole());
        assertEquals("一线用药原则是什么？", msgCaptor.getValue().getContent());
        assertEquals("一线用药原则是什么？", session.getTitle());
        verify(qaSessionRepository).save(session);
    }

    @Test
    void ask_longQuestion_truncatesTitle() {
        DiseaseKbQaSessionEntity session = new DiseaseKbQaSessionEntity();
        session.setSpaceId(5L);
        DiseaseKbSpaceEntity sp = space("s", null);
        sp.setId(5L);
        when(qaSessionRepository.findById(2L)).thenReturn(Optional.of(session));
        when(spaceRepository.findById(5L)).thenReturn(Optional.of(sp));
        when(qaMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(aiService.streamAnswer(eq(5L), eq(2L), anyString())).thenReturn(mock(SseEmitter.class));
        String longQuestion = "很".repeat(80);

        service.ask(2L, longQuestion);

        assertEquals(50, session.getTitle().length());
    }

    @Test
    void deleteSpace_cascadesItems() {
        DiseaseKbSpaceEntity space = space("s", null);
        space.setId(5L);
        when(spaceRepository.findById(5L)).thenReturn(Optional.of(space));
        DiseaseKbItemEntity item = item("t", "GUIDELINE");
        when(itemRepository.findBySpaceId(eq(5L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item)));

        service.deleteSpace(5L);

        verify(spaceRepository).delete(space);
        verify(itemRepository).delete(item);
    }

    // ==================== helpers ====================

    private static DiseaseKbSpaceEntity space(String name, Long cohortId) {
        DiseaseKbSpaceEntity e = new DiseaseKbSpaceEntity();
        e.setName(name);
        e.setCohortId(cohortId);
        return e;
    }

    private static DiseaseKbItemEntity item(String title, String type) {
        DiseaseKbItemEntity e = new DiseaseKbItemEntity();
        e.setTitle(title);
        e.setItemType(type);
        return e;
    }
}
