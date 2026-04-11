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
@Table(name = "r_etl_task_log", schema = "rdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE rdr.r_etl_task_log SET is_deleted = true WHERE id = ?")
public class EtlTaskLogEntity extends BaseEntity {

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "run_no", nullable = false)
    private Integer runNo;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "records_read")
    private Long recordsRead;

    @Column(name = "records_written")
    private Long recordsWritten;

    @Column(name = "error_count")
    private Integer errorCount;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "log_file_path", length = 256)
    private String logFilePath;
}
