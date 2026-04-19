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
@Table(name = "r_etl_execution", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.r_etl_execution SET is_deleted = true WHERE id = ?")
public class EtlExecutionEntity extends BaseEntity {

    @Column(name = "pipeline_id", nullable = false)
    private Long pipelineId;

    @Column(name = "step_id")
    private Long stepId;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "PENDING";

    @Column(name = "engine_config", columnDefinition = "TEXT")
    private String engineConfig;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "rows_read")
    private Long rowsRead = 0L;

    @Column(name = "rows_written")
    private Long rowsWritten = 0L;

    @Column(name = "rows_skipped")
    private Long rowsSkipped = 0L;

    @Column(name = "rows_error")
    private Long rowsError = 0L;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "log_path", length = 256)
    private String logPath;

    @Column(name = "trigger_type", nullable = false, length = 16)
    private String triggerType = "MANUAL";

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "execution_snapshot", columnDefinition = "jsonb")
    private JsonNode executionSnapshot;
}
