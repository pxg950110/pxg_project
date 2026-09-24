package com.maidc.data.service;

import com.maidc.data.entity.DiseaseCohortEventEntity;
import com.maidc.data.repository.DiseaseCohortEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 专病队列动态事件（PRD FR6 cohortDigest 数据源）。
 * 记录为 best-effort：埋点失败仅 log，不影响队列同步/知识库发布主流程。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiseaseCohortEventService {

    public static final String TYPE_SYNC_DONE = "SYNC_DONE";
    public static final String TYPE_KB_ITEM_PUBLISHED = "KB_ITEM_PUBLISHED";
    public static final String TYPE_AI_SUGGEST_PENDING = "AI_SUGGEST_PENDING";

    private final DiseaseCohortEventRepository eventRepository;

    public void record(String eventType, Long cohortId, Long orgId, String title) {
        try {
            DiseaseCohortEventEntity event = new DiseaseCohortEventEntity();
            event.setEventType(eventType);
            event.setCohortId(cohortId);
            event.setOrgId(orgId == null ? 0L : orgId);
            event.setEventTitle(title);
            event.setIsDeleted(false);
            eventRepository.save(event);
        } catch (Exception e) {
            log.warn("Cohort event record failed (type={}, cohort={}): {}", eventType, cohortId, e.getMessage());
        }
    }

    /** 工作台队列动态：最近 3 条；查询失败降级为空列表 */
    public List<DiseaseCohortEventEntity> latest(Long orgId) {
        try {
            return eventRepository.findTop3ByOrgIdAndIsDeletedFalseOrderByCreatedAtDesc(orgId == null ? 0L : orgId);
        } catch (Exception e) {
            log.warn("Cohort digest unavailable: {}", e.getMessage());
            return List.of();
        }
    }
}
