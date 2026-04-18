package com.maidc.data.entity;

import com.maidc.common.jpa.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "r_desensitize_rule", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.r_desensitize_rule SET is_deleted = true WHERE id = ?")
public class DesensitizeRuleEntity extends BaseEntity {

    @Column(name = "rule_name", nullable = false, length = 128)
    private String ruleName;

    @Column(name = "field_type", nullable = false, length = 32)
    private String fieldType;

    @Column(name = "strategy", nullable = false, length = 32)
    private String strategy;

    @Column(name = "params", columnDefinition = "jsonb")
    private String params;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "description", length = 512)
    private String description;
}
