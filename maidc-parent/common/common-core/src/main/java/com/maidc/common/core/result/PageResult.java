package com.maidc.common.core.result;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 分页结果封装
 *
 * @param <T> 分页数据类型
 */
@Data
public class PageResult<T> {

    private List<T> items;
    private long total;
    private int page;
    private int pageSize;
    private int totalPages;

    /**
     * 基于 Spring Data Page 对象构建分页结果
     */
    public static <T> PageResult<T> of(Page<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setItems(page.getContent());
        result.setTotal(page.getTotalElements());
        result.setPage(page.getNumber() + 1);
        result.setPageSize(page.getSize());
        result.setTotalPages(page.getTotalPages());
        return result;
    }
}
