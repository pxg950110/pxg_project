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
@Table(name = "r_imaging_dataset", schema = "rdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE rdr.r_imaging_dataset SET is_deleted = true WHERE id = ?")
public class ImagingDatasetEntity extends BaseEntity {

    @Column(name = "dataset_id", nullable = false)
    private Long datasetId;

    @Column(name = "exam_id")
    private Long examId;

    @Column(name = "image_format", nullable = false, length = 16)
    private String imageFormat;

    @Column(name = "resolution", columnDefinition = "jsonb")
    private String resolution;

    @Column(name = "body_part", length = 64)
    private String bodyPart;

    @Column(name = "annotation_status", nullable = false, length = 16)
    private String annotationStatus = "NONE";
}
