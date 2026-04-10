package com.maidc.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRuleVO {

    private Long id;

    private String ruleName;

    private String ruleType;

    private String targetType;

    private Long targetId;

    private String severity;

    private Boolean enabled;

    private LocalDateTime lastTriggeredAt;
}
