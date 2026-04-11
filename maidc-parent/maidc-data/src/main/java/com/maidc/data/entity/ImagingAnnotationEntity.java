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
@Table(name = "r_imaging_annotation", schema = "rdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE rdr.r_imaging_annotation SET is_deleted = true WHERE id = ?")
public class ImagingAnnotationEntity extends BaseEntity {

    @Column(name = "imaging_dataset_id", nullable = false)
    private Long imagingDatasetId;

    @Column(name = "annotator_id", nullable = false)
    private Long annotatorId;

    @Column(name = "annotation_type", nullable = false, length = 32)
    private String annotationType;

    @Column(name = "annotation_data", columnDefinition = "jsonb")
    private String annotationData;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "DRAFT";
}
