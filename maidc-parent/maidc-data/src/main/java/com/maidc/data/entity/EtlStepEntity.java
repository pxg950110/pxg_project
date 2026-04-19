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
@Table(name = "r_etl_step", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.r_etl_step SET is_deleted = true WHERE id = ?")
public class EtlStepEntity extends BaseEntity {

    @Column(name = "pipeline_id", nullable = false)
    private Long pipelineId;

    @Column(name = "step_name", nullable = false, length = 128)
    private String stepName;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "step_type", nullable = false, length = 16)
    private String stepType = "ONE_TO_ONE";

    @Column(name = "source_schema", length = 32)
    private String sourceSchema;

    @Column(name = "source_table", nullable = false, length = 128)
    private String sourceTable;

    @Column(name = "target_schema", length = 32)
    private String targetSchema;

    @Column(name = "target_table", nullable = false, length = 128)
    private String targetTable;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "join_config", columnDefinition = "jsonb")
    private JsonNode joinConfig;

    @Column(name = "filter_condition", columnDefinition = "TEXT")
    private String filterCondition;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "transform_config", columnDefinition = "jsonb")
    private JsonNode transformConfig;

    @Column(name = "pre_sql", columnDefinition = "TEXT")
    private String preSql;

    @Column(name = "post_sql", columnDefinition = "TEXT")
    private String postSql;

    @Column(name = "on_error", nullable = false, length = 16)
    private String onError = "ABORT";

    @Column(name = "sync_mode", nullable = false, length = 16)
    private String syncMode = "INCREMENTAL";

    @Column(name = "last_sync_time")
    private LocalDateTime lastSyncTime;
}
