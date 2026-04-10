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
@Table(name = "r_dataset_version", schema = "rdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE rdr.r_dataset_version SET is_deleted = true WHERE id = ?")
public class DatasetVersionEntity extends BaseEntity {

    @Column(name = "dataset_id", nullable = false)
    private Long datasetId;

    @Column(name = "version_no", nullable = false, length = 32)
    private String versionNo;

    @Column(name = "file_path", length = 512)
    private String filePath;

    @Column(name = "checksum", length = 128)
    private String checksum;

    @Column(name = "sample_count")
    private Long sampleCount = 0L;
}
