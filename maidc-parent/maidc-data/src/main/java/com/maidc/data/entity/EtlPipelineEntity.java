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

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "r_etl_pipeline", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.r_etl_pipeline SET is_deleted = true WHERE id = ?")
public class EtlPipelineEntity extends BaseEntity {

    @Column(name = "pipeline_name", nullable = false, length = 128)
    private String pipelineName;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "engine_type", nullable = false, length = 16)
    private String engineType = "EMBULK";

    @Column(name = "status", nullable = false, length = 16)
    private String status = "DRAFT";

    @Column(name = "sync_mode", nullable = false, length = 16)
    private String syncMode = "MANUAL";

    @Column(name = "cron_expression", length = 64)
    private String cronExpression;

    @Column(name = "last_run_time")
    private LocalDateTime lastRunTime;
}
