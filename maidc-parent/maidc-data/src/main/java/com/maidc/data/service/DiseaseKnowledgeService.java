package com.maidc.data.service;

import com.maidc.common.core.enums.ErrorCode;
import com.maidc.common.core.exception.BusinessException;
import com.maidc.data.dto.DiseaseKbSearchVO;
import com.maidc.data.entity.DiseaseKbItemEntity;
import com.maidc.data.entity.DiseaseKbQaMessageEntity;
import com.maidc.data.entity.DiseaseKbQaSessionEntity;
import com.maidc.data.entity.DiseaseKbSpaceEntity;
import com.maidc.data.repository.DiseaseCohortRepository;
import com.maidc.data.repository.DiseaseKbItemRepository;
import com.maidc.data.repository.DiseaseKbQaMessageRepository;
import com.maidc.data.repository.DiseaseKbQaSessionRepository;
import com.maidc.data.repository.DiseaseKbSpaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;

/**
 * 专病知识库：知识空间 / 知识条目 / 中文检索 / 队列联动 / 问答会话数据层。
 * <p>数据落 cdr 域（与 c_disease_cohort 同域）；条目状态机 DRAFT→PUBLISHED→ARCHIVED。
 * <p>AI 能力（摘要/向量化/RAG 问答）本切片未接入：内容变更只标记 ai_status=PENDING，
 * 由后续 AI 切片（ai-worker 扩展 + DiseaseKnowledgeAiService）统一补算；/qa/ask 暂按降级语义返回。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiseaseKnowledgeService {

    public static final Set<String> ITEM_TYPES = Set.of("GUIDELINE", "LITERATURE", "PATHWAY", "SCALE");
    public static final Set<String> ITEM_STATUSES = Set.of("DRAFT", "PUBLISHED", "ARCHIVED");

    private final DiseaseKbSpaceRepository spaceRepository;
    private final DiseaseKbItemRepository itemRepository;
    private final DiseaseKbQaSessionRepository qaSessionRepository;
    private final DiseaseKbQaMessageRepository qaMessageRepository;
    private final DiseaseCohortRepository cohortRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final com.maidc.common.minio.service.MinioService minioService;

    // ==================== 知识空间 ====================

    public Page<DiseaseKbSpaceEntity> listSpaces(String keyword, String status, int page, int size) {
        Specification<DiseaseKbSpaceEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.like(root.get("name"), "%" + keyword + "%"));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
        return spaceRepository.findAll(spec, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "updatedAt")));
    }

    @Transactional
    public DiseaseKbSpaceEntity createSpace(DiseaseKbSpaceEntity entity) {
        validateSpaceName(entity.getName(), null);
        validateCohortExists(entity.getCohortId());
        if (entity.getOrgId() == null) entity.setOrgId(0L);
        return spaceRepository.save(entity);
    }

    @Transactional
    public DiseaseKbSpaceEntity updateSpace(Long id, DiseaseKbSpaceEntity input) {
        DiseaseKbSpaceEntity space = spaceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.KB_SPACE_NOT_FOUND));
        validateSpaceName(input.getName(), id);
        validateCohortExists(input.getCohortId());
        space.setName(input.getName());
        space.setDescription(input.getDescription());
        space.setIcdCodes(input.getIcdCodes());
        space.setCohortId(input.getCohortId());
        space.setIconColor(input.getIconColor());
        space.setStatus(input.getStatus());
        return spaceRepository.save(space);
    }

    @Transactional
    public void deleteSpace(Long id) {
        DiseaseKbSpaceEntity space = spaceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.KB_SPACE_NOT_FOUND));
        spaceRepository.delete(space);
        // 级联软删条目（量级小，逐条软删可接受）
        itemRepository.findBySpaceId(space.getId(), PageRequest.of(0, Integer.MAX_VALUE)).forEach(itemRepository::delete);
    }

    public Map<String, Object> getSpaceDetail(Long id) {
        DiseaseKbSpaceEntity space = spaceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.KB_SPACE_NOT_FOUND));
        Map<String, Long> counts = new HashMap<>();
        long total = 0;
        for (DiseaseKbItemRepository.TypeCount tc : itemRepository.countBySpaceGroupByType(id)) {
            counts.put(tc.getType(), tc.getCnt());
            total += tc.getCnt();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("space", space);
        result.put("counts", counts);
        result.put("itemTotal", total);
        result.put("cohort", buildCohortSummary(space.getCohortId()));
        return result;
    }

    /** 队列 → 知识空间（专病详情页跳转用；未关联返回 null，前端引导创建） */
    public DiseaseKbSpaceEntity getSpaceByCohort(Long cohortId) {
        return spaceRepository.findByCohortIdAndIsDeletedFalse(cohortId).orElse(null);
    }

    private Map<String, Object> buildCohortSummary(Long cohortId) {
        if (cohortId == null) return null;
        return cohortRepository.findById(cohortId)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .map(cohort -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("cohortId", cohort.getId());
                    m.put("name", cohort.getName());
                    m.put("patientCount", cohort.getPatientCount());
                    m.put("status", cohort.getStatus());
                    return m;
                })
                .orElseGet(() -> {
                    // 队列已被删除：悬空引用按未关联展示，不报错（测试注意事项 #7）
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("cohortId", cohortId);
                    m.put("deleted", true);
                    return m;
                });
    }

    private void validateSpaceName(String name, Long excludeId) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "专病名称不能为空");
        }
        boolean duplicated = excludeId == null
                ? spaceRepository.existsByNameAndIsDeletedFalse(name.trim())
                : spaceRepository.existsByNameAndIsDeletedFalseAndIdNot(name.trim(), excludeId);
        if (duplicated) {
            throw new BusinessException(ErrorCode.KB_SPACE_NAME_DUPLICATED);
        }
    }

    private void validateCohortExists(Long cohortId) {
        if (cohortId != null && cohortRepository.findById(cohortId).isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "关联的专病队列不存在");
        }
    }

    // ==================== 知识条目 ====================

    public Page<DiseaseKbItemEntity> listItems(Long spaceId, String itemType, String status,
                                               String keyword, int page, int size) {
        Specification<DiseaseKbItemEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("spaceId"), spaceId));
            if (itemType != null && !itemType.isBlank()) {
                predicates.add(cb.equal(root.get("itemType"), itemType));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword + "%";
                predicates.add(cb.or(cb.like(root.get("title"), like), cb.like(root.get("summary"), like)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return itemRepository.findAll(spec, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    public DiseaseKbItemEntity getItem(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.KB_ITEM_NOT_FOUND));
    }

    @Transactional
    public DiseaseKbItemEntity createItem(Long spaceId, DiseaseKbItemEntity entity) {
        spaceRepository.findById(spaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.KB_SPACE_NOT_FOUND));
        validateItem(entity);
        entity.setId(null);
        entity.setSpaceId(spaceId);
        entity.setStatus("DRAFT");
        entity.setAiStatus("PENDING");
        if (entity.getOrgId() == null) entity.setOrgId(0L);
        return itemRepository.save(entity);
    }

    @Transactional
    public DiseaseKbItemEntity updateItem(Long id, DiseaseKbItemEntity input) {
        DiseaseKbItemEntity item = getItem(id);
        validateItem(input);
        boolean contentChanged = !strEq(item.getTitle(), input.getTitle())
                || !strEq(nullToEmpty(item.getSummary()), nullToEmpty(input.getSummary()))
                || !strEq(nullToEmpty(item.getContent()), nullToEmpty(input.getContent()))
                || !strEq(nullToEmpty(item.getFileName()), nullToEmpty(input.getFileName()));
        item.setTitle(input.getTitle());
        item.setItemType(input.getItemType());
        item.setSummary(input.getSummary());
        item.setContent(input.getContent());
        item.setSource(input.getSource());
        item.setAuthors(input.getAuthors());
        item.setPublishDate(input.getPublishDate());
        item.setVersionNo(input.getVersionNo());
        item.setTags(input.getTags());
        if (input.getFileName() != null) {
            item.setFileUrl(input.getFileUrl());
            item.setFileName(input.getFileName());
        }
        if (contentChanged) {
            item.setAiStatus("PENDING");
            item.setAiSummary(null);
            item.setAiExtract(null);
        }
        return itemRepository.save(item);
    }

    @Transactional
    public void deleteItem(Long id) {
        itemRepository.delete(getItem(id));
    }

    /** 发布 / 下架；发布标记 ai_status=PENDING 待 AI 切片补算 */
    @Transactional
    public DiseaseKbItemEntity publishItem(Long id, String action) {
        DiseaseKbItemEntity item = getItem(id);
        switch (action == null ? "" : action) {
            case "PUBLISH" -> {
                item.setStatus("PUBLISHED");
                item.setAiStatus("PENDING");
            }
            case "ARCHIVE" -> item.setStatus("ARCHIVED");
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "action 仅支持 PUBLISH / ARCHIVE");
        }
        return itemRepository.save(item);
    }

    @Transactional
    public DiseaseKbItemEntity recompute(Long id) {
        DiseaseKbItemEntity item = getItem(id);
        item.setAiStatus("PENDING");
        return itemRepository.save(item);
    }

    private void validateItem(DiseaseKbItemEntity entity) {
        if (entity.getTitle() == null || entity.getTitle().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "标题不能为空");
        }
        if (entity.getItemType() == null || !ITEM_TYPES.contains(entity.getItemType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "条目类型仅支持 " + ITEM_TYPES);
        }
    }

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx", "xlsx");

    /** 附件上传：≤50MB，PDF/DOCX/XLSX；成功后回写地址并标记待 AI 处理 */
    @Transactional
    public DiseaseKbItemEntity uploadItemFile(Long id, org.springframework.web.multipart.MultipartFile file) {
        DiseaseKbItemEntity item = getItem(id);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "附件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "附件不能超过 50MB");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase()
                : "";
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "附件仅支持 PDF/DOCX/XLSX");
        }
        String objectName = "diseasekb/" + id + "/" + System.currentTimeMillis() + "-" + original;
        try (java.io.InputStream in = file.getInputStream()) {
            minioService.uploadFile("diseasekb", objectName, in, file.getContentType(), file.getSize());
        } catch (Exception e) {
            log.warn("KB item {} file upload failed: {}", id, e.getMessage());
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "附件上传失败");
        }
        item.setFileUrl(objectName);
        item.setFileName(original);
        item.setAiStatus("PENDING");
        return itemRepository.save(item);
    }

    // ==================== 中文检索（zhparser FTS，空结果回退 ILIKE） ====================

    public List<DiseaseKbSearchVO> search(String keyword, Long spaceId, int page, int size) {
        if (keyword == null || keyword.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "检索关键词不能为空");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("kw", keyword.trim());
        params.put("spaceId", spaceId);
        params.put("limit", size);
        params.put("offset", (page - 1) * (long) size);

        String ftsExpr = "to_tsvector('zh', coalesce(i.title,'') || ' ' || coalesce(i.summary,'') || ' ' || coalesce(i.content,''))";
        String commonWhere = "FROM cdr.c_disease_kb_item i JOIN cdr.c_disease_kb_space s ON s.id = i.space_id " +
                "WHERE i.is_deleted = false AND i.status = 'PUBLISHED' " +
                "AND (:spaceId::bigint IS NULL OR i.space_id = :spaceId)";

        String ftsSql = "SELECT i.id, i.space_id, s.name AS space_name, i.item_type, i.title, i.status, i.publish_date, " +
                "ts_headline('zh', coalesce(i.title,'') || ' ' || coalesce(i.summary,'') || ' ' || coalesce(i.content,''), plainto_tsquery('zh', :kw), 'StartSel=<em>,StopSel=</em>') AS snippet " +
                commonWhere + " AND " + ftsExpr + " @@ plainto_tsquery('zh', :kw) " +
                "ORDER BY ts_rank(" + ftsExpr + ", plainto_tsquery('zh', :kw)) DESC, i.id DESC LIMIT :limit OFFSET :offset";

        List<DiseaseKbSearchVO> result = querySearch(ftsSql, params);
        if (!result.isEmpty()) {
            return result;
        }
        // 分词未命中（如错别字/混合串）回退子串匹配
        String likeSql = "SELECT i.id, i.space_id, s.name AS space_name, i.item_type, i.title, i.status, i.publish_date, " +
                "left(coalesce(i.summary, i.title), 120) AS snippet " +
                commonWhere + " AND (i.title ILIKE '%' || :kw || '%' OR coalesce(i.summary,'') ILIKE '%' || :kw || '%') " +
                "ORDER BY i.id DESC LIMIT :limit OFFSET :offset";
        return querySearch(likeSql, params);
    }

    private List<DiseaseKbSearchVO> querySearch(String sql, Map<String, Object> params) {
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            DiseaseKbSearchVO vo = new DiseaseKbSearchVO();
            vo.setId(rs.getLong("id"));
            vo.setSpaceId(rs.getLong("space_id"));
            vo.setSpaceName(rs.getString("space_name"));
            vo.setItemType(rs.getString("item_type"));
            vo.setTitle(rs.getString("title"));
            vo.setSnippet(rs.getString("snippet"));
            vo.setStatus(rs.getString("status"));
            vo.setPublishDate(rs.getDate("publish_date") == null ? null : rs.getDate("publish_date").toLocalDate());
            return vo;
        });
    }

    // ==================== AI 问答（会话管理 + SSE 编排） ====================

    public List<DiseaseKbQaSessionEntity> listSessions(Long spaceId) {
        return qaSessionRepository.findBySpaceIdAndIsDeletedFalseOrderByCreatedAtDesc(spaceId);
    }

    @Transactional
    public DiseaseKbQaSessionEntity createSession(Long spaceId) {
        DiseaseKbSpaceEntity space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.KB_SPACE_NOT_FOUND));
        if (!"ACTIVE".equals(space.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "知识空间已停用，AI 问答关闭");
        }
        DiseaseKbQaSessionEntity session = new DiseaseKbQaSessionEntity();
        session.setSpaceId(spaceId);
        if (session.getOrgId() == null) session.setOrgId(0L);
        return qaSessionRepository.save(session);
    }

    public List<DiseaseKbQaMessageEntity> listMessages(Long sessionId) {
        requireSession(sessionId);
        return qaMessageRepository.findBySessionIdAndIsDeletedFalseOrderByCreatedAtAsc(sessionId);
    }

    @Transactional
    public void deleteSession(Long sessionId) {
        qaSessionRepository.delete(requireSession(sessionId));
    }

    /** 提问：AI 切片未接入前按降级语义拒绝（会话/消息数据层已就绪，接入后改为 SSE 流式） */
    public SseEmitter ask(Long sessionId, String question) {
        DiseaseKbQaSessionEntity session = requireSession(sessionId);
        DiseaseKbSpaceEntity space = spaceRepository.findById(session.getSpaceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.KB_SPACE_NOT_FOUND));
        if (!"ACTIVE".equals(space.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "知识空间已停用，AI 问答关闭");
        }
        if (question == null || question.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "问题不能为空");
        }
        throw new BusinessException(ErrorCode.KB_AI_UNAVAILABLE);
    }

    private DiseaseKbQaSessionEntity requireSession(Long sessionId) {
        return qaSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND.getCode(), "问答会话不存在"));
    }

    private static boolean strEq(String a, String b) {
        return java.util.Objects.equals(a, b);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
