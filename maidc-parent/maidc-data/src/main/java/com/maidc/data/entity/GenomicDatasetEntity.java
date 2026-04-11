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
@Table(name = "r_genomic_dataset", schema = "rdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE rdr.r_genomic_dataset SET is_deleted = true WHERE id = ?")
public class GenomicDatasetEntity extends BaseEntity {

    @Column(name = "dataset_id", nullable = false)
    private Long datasetId;

    @Column(name = "sample_id", nullable = false, length = 64)
    private String sampleId;

    @Column(name = "genome_build", nullable = false, length = 16)
    private String genomeBuild;

    @Column(name = "file_format", nullable = false, length = 16)
    private String fileFormat;

    @Column(name = "file_path", length = 256)
    private String filePath;
}
