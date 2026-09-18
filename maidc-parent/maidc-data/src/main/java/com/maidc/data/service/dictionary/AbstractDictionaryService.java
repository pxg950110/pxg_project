package com.maidc.data.service.dictionary;

import com.maidc.common.core.exception.BusinessException;
import com.maidc.common.jpa.base.BaseEntity;
import com.maidc.data.repository.dictionary.DictionaryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 医学字典服务模板基类：固化五套字典共同的分页检索、详情、创建查重、
 * 非空合并更新与逻辑删除逻辑。子类提供仓储、实体名、编码与检索属性、
 * 排序规则，并各自补充列表过滤条件与特有查询（树/章节/分类等）。
 */
@Slf4j
public abstract class AbstractDictionaryService<E extends BaseEntity> {

    /** 更新合并时永不拷贝的字段：主键、审计、软删、租户；业务编码由 codeProperty() 单独排除 */
    private static final List<String> NON_MERGE_PROPERTIES = List.of(
        "id", "createdBy", "createdAt", "updatedBy", "updatedAt", "isDeleted", "orgId");

    protected abstract DictionaryRepository<E> repository();

    /** 实体中文名（如 "药品"），用于错误消息与日志 */
    protected abstract String entityName();

    /** 业务唯一编码属性名（如 "drugCode"），创建查重且更新不可变 */
    protected abstract String codeProperty();

    /** 关键词模糊检索属性（含编码属性），统一按 LOWER(col) LIKE %kw% 匹配 */
    protected abstract List<String> searchProperties();

    /** 列表默认排序 */
    protected abstract Sort defaultSort();

    // ==================== 通用查询 ====================

    /**
     * 分页列表：keyword 非空时与过滤条件取交集（此前行为是关键词检索时丢弃全部过滤条件）。
     */
    public Page<E> list(Specification<E> spec, String keyword, int page, int size) {
        PageRequest request = PageRequest.of(Math.max(page - 1, 0), size, defaultSort());
        if (keyword != null && !keyword.isBlank()) {
            return repository().findAll(withKeyword(spec, keyword), request);
        }
        return repository().findAll(spec, request);
    }

    public Page<E> search(String keyword, int page, int size) {
        if (keyword == null || keyword.isBlank()) {
            throw new BusinessException(400, "搜索关键词不能为空");
        }
        return repository().findAll(withKeyword(null, keyword),
            PageRequest.of(Math.max(page - 1, 0), size, defaultSort()));
    }

    public E getById(Long id) {
        return repository().findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new BusinessException(404, entityName() + "不存在: " + id));
    }

    // ==================== 通用写路径 ====================

    @Transactional
    public E create(E entity) {
        BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(entity);
        String code = propertyValue(wrapper, codeProperty());
        if (code == null || code.isBlank()) {
            throw new BusinessException(400, entityName() + "编码不能为空");
        }
        if (codeInUse(code)) {
            throw new BusinessException(400, entityName() + "编码已存在: " + code);
        }
        if (entity.getOrgId() == null) entity.setOrgId(0L);
        String status = propertyValue(wrapper, "status");
        if (status == null || status.isBlank()) wrapper.setPropertyValue("status", "ACTIVE");
        if (entity.getIsDeleted() == null) entity.setIsDeleted(false);

        E saved = repository().save(entity);
        log.info("{}创建成功: id={}, code={}", entityName(), saved.getId(), code);
        return saved;
    }

    @Transactional
    public E update(Long id, E updates) {
        E entity = getById(id);
        List<String> ignored = new ArrayList<>(NON_MERGE_PROPERTIES);
        ignored.add(codeProperty());
        BeanUtils.copyProperties(updates, entity, collectIgnoredProperties(updates, ignored));
        E saved = repository().save(entity);
        log.info("{}更新成功: id={}", entityName(), saved.getId());
        return saved;
    }

    /**
     * 逻辑删除（is_deleted=true）：行数据保留、编码可复用，与系统字典删除语义一致。
     */
    @Transactional
    public void delete(Long id) {
        E entity = getById(id);
        entity.setIsDeleted(true);
        repository().save(entity);
        log.info("{}已删除(逻辑): id={}", entityName(), id);
    }

    // ==================== 供子类复用的构件 ====================

    /** 属性等值过滤；值为 null 或空白字符串时返回 null（不参与组合） */
    protected Specification<E> eqIfPresent(String property, Object value) {
        if (value == null || (value instanceof String s && s.isBlank())) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get(property), value);
    }

    /** 组合多个 Specification，忽略 null 元素；全部为 null 时返回 null */
    @SafeVarargs
    protected final Specification<E> allOf(Specification<E>... specs) {
        Specification<E> result = null;
        for (Specification<E> spec : specs) {
            if (spec == null) {
                continue;
            }
            result = result == null ? spec : result.and(spec);
        }
        return result;
    }

    private Specification<E> withKeyword(Specification<E> spec, String keyword) {
        Specification<E> keywordSpec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> parts = new ArrayList<>();
            String pattern = "%" + keyword.toLowerCase() + "%";
            for (String property : searchProperties()) {
                parts.add(cb.like(cb.lower(root.get(property)), pattern));
            }
            return cb.or(parts.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return spec == null ? keywordSpec : spec.and(keywordSpec);
    }

    private boolean codeInUse(String code) {
        Specification<E> spec = eqIfPresent(codeProperty(), code);
        return spec != null && repository().count(spec) > 0;
    }

    private String propertyValue(BeanWrapper wrapper, String property) {
        Object value = wrapper.getPropertyValue(property);
        return value == null ? null : value.toString();
    }

    /** 忽略清单 = 始终忽略字段 ∪ 源对象中为 null 的属性（null 不覆盖目标值） */
    private String[] collectIgnoredProperties(Object source, List<String> alwaysIgnore) {
        BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(source);
        List<String> ignored = new ArrayList<>(alwaysIgnore);
        Arrays.stream(wrapper.getPropertyDescriptors())
            .map(PropertyDescriptor::getName)
            .filter(name -> !ignored.contains(name))
            .filter(name -> wrapper.getPropertyValue(name) == null)
            .forEach(ignored::add);
        return ignored.toArray(new String[0]);
    }
}
