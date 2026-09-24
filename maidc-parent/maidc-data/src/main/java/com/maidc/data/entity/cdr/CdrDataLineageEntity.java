package com.maidc.data.entity.cdr;

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

/**
 * CDR数据血缘实体
 * 记录数据从源系统到CDR的完整血缘链路
 */
@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "cdr_data_lineage", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.cdr_data_lineage SET is_deleted = true WHERE id = ?")
public class CdrDataLineageEntity extends BaseEntity {

    @Column(name = "lineage_code", nullable = false, unique = true, length = 64)
    private String lineageCode;

    @Column(name = "target_schema", nullable = false, length = 32)
    private String targetSchema;

    @Column(name = "target_table", nullable = false, length = 64)
    private String targetTable;

    @Column(name = "target_column", nullable = false, length = 64)
    private String targetColumn;

    @Column(name = "source_system", nullable = false, length = 64)
    private String sourceSystem;

    @Column(name = "source_schema", length = 64)
    private String sourceSchema;

    @Column(name = "source_table", nullable = false, length = 64)
    private String sourceTable;

    @Column(name = "source_column", nullable = false, length = 64)
    private String sourceColumn;

    @Column(name = "etl_job_id")
    private Long etlJobId;

    @Column(name = "etl_job_name", length = 128)
    private String etlJobName;

    @Column(name = "transform_type", length = 32)
    private String transformType; // DIRECT, DERIVED, AGGREGATED, JOINED

    @Column(name = "transform_expression", columnDefinition = "TEXT")
    private String transformExpression;

    @Column(name = "transform_logic", columnDefinition = "TEXT")
    private String transformLogic;

    @Column(name = "quality_check_id")
    private Long qualityCheckId;

    @Column(name = "quality_passed")
    private Boolean qualityPassed;

    @Column(name = "first_sync_time")
    private LocalDateTime firstSyncTime;

    @Column(name = "last_sync_time")
    private LocalDateTime lastSyncTime;

    @Column(name = "sync_count", nullable = false)
    private Integer syncCount = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata; // 扩展元数据JSON
}