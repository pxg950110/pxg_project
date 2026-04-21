package com.maidc.data.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ClinicalSearchResult {
    private String domain;
    private List<Map<String, Object>> items;
    private long total;
    private int page;
    private int pageSize;
}
