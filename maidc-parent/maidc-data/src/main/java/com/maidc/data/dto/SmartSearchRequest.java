package com.maidc.data.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class SmartSearchRequest {
    private String keyword;
    private List<String> domains;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private int page = 1;
    private int pageSize = 20;
}
