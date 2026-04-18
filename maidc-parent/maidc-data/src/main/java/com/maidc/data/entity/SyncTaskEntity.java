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
@Table(name = "r_sync_task", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.r_sync_task SET is_deleted = true WHERE id = ?")
public class SyncTaskEntity extends BaseEntity {

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "task_name", nullable = false, length = 128)
    private String taskName;

    @Column(name = "sync_type", nullable = false, length = 16)
    private String syncType;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "PENDING";

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "total_records")
    private Long totalRecords = 0L;

    @Column(name = "success_records")
    private Long successRecords = 0L;

    @Column(name = "failed_records")
    private Long failedRecords = 0L;

    @Column(name = "error_msg", length = 1024)
    private String errorMsg;

    @Column(name = "config", columnDefinition = "jsonb")
    private String config;
}
