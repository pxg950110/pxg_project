package com.maidc.model.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.maidc.common.jpa.base.BaseEntity;
import com.maidc.common.jpa.converter.JsonNodeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "m_alert_rule", schema = "model")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE model.m_alert_rule SET is_deleted = true WHERE id = ?")
public class AlertRuleEntity extends BaseEntity {

    @Column(name = "rule_name", nullable = false, length = 128)
    private String ruleName;

    @Column(name = "rule_type", length = 32)
    private String ruleType;

    @Column(name = "target_type", length = 32)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "condition_expr", columnDefinition = "jsonb")
    private JsonNode conditionExpr;

    @Column(name = "severity", length = 16)
    private String severity;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "notify_channels", columnDefinition = "jsonb")
    private JsonNode notifyChannels;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "last_triggered_at")
    private LocalDateTime lastTriggeredAt;
}
