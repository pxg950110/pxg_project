package com.maidc.data.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class SmartSearchResult {
    private List<SmartSearchItem> items;
    private long total;
    private int page;
    private int pageSize;
    private Map<String, Long> aggregations;
}
