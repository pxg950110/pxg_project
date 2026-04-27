package com.maidc.data.entity;

import com.maidc.common.jpa.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Where;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "m_import_task", schema = "masterdata")
@Where(clause = "is_deleted = false")
public class ImportTaskEntity extends BaseEntity {

    @Column(name = "code_system_id", nullable = false)
    private Long codeSystemId;

    @Column(name = "task_type", nullable = false, length = 32)
    private String taskType = "CONCEPT";

    @Column(name = "file_name", nullable = false, length = 256)
    private String fileName;

    @Column(name = "file_path", length = 512)
    private String filePath;

    @Column(name = "total_rows")
    private Integer totalRows = 0;

    @Column(name = "processed_rows")
    private Integer processedRows = 0;

    @Column(name = "failed_rows")
    private Integer failedRows = 0;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "PENDING";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
