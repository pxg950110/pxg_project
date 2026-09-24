package com.maidc.data.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MicrobiologyVO {
    private Long id;
    private Long encounterId;
    private LocalDateTime chartDate;
    private String specTypeDesc;
    private String testName;
    private String orgName;
    private Integer isolateNum;
    private String abName;
    private String dilutionText;
    private String dilutionComparison;
    private BigDecimal dilutionValue;
    private String interpretation;
    private String quantity;
}
