package com.maidc.data.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class ClinicalSearchRequest {
    private String keyword;
    private ClinicalSearchDomain domain;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private String department;
    private String diagnosis;
    private String status;
    private int page = 1;
    private int pageSize = 20;
}
