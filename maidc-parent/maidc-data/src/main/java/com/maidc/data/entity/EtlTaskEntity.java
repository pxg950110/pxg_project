package com.maidc.data.entity;

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
@Table(name = "r_etl_task", schema = "rdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE rdr.r_etl_task SET is_deleted = true WHERE id = ?")
public class EtlTaskEntity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "source_type", length = 32)
    private String sourceType;

    @Column(name = "target_type", length = 32)
    private String targetType;

    @Column(name = "cron_expression", length = 64)
    private String cronExpression;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "IDLE";

    @Column(name = "last_execution_time")
    private LocalDateTime lastExecutionTime;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "config", columnDefinition = "jsonb")
    private JsonNode config;
}
