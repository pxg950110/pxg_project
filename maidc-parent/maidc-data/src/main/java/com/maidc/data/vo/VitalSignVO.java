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
public class VitalSignVO {
    private Long id;
    private Long encounterId;
    private String signType;
    private BigDecimal signValue;
    private String unit;
    private LocalDateTime measuredAt;
    private String measuredBy;
}
