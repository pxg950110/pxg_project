package com.maidc.data.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabPanelItemVO {
    private Long id;
    private Long testId;
    private String itemCode;
    private String itemName;
    private String resultValue;
    private String resultUnit;
    private String referenceRange;
    private Boolean abnormalFlag;
}
