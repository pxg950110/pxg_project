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
@Table(name = "r_dataset_access_log", schema = "rdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE rdr.r_dataset_access_log SET is_deleted = true WHERE id = ?")
public class DatasetAccessLogEntity extends BaseEntity {

    @Column(name = "dataset_id", nullable = false)
    private Long datasetId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "access_type", nullable = false, length = 32)
    private String accessType;

    @Column(name = "purpose", columnDefinition = "TEXT")
    private String purpose;

    @Column(name = "record_count")
    private Integer recordCount;

    @Column(name = "accessed_at", nullable = false)
    private LocalDateTime accessedAt = LocalDateTime.now();
}
