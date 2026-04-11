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
@Table(name = "r_text_annotation", schema = "rdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE rdr.r_text_annotation SET is_deleted = true WHERE id = ?")
public class TextAnnotationEntity extends BaseEntity {

    @Column(name = "text_dataset_id", nullable = false)
    private Long textDatasetId;

    @Column(name = "annotator_id", nullable = false)
    private Long annotatorId;

    @Column(name = "annotation_type", nullable = false, length = 32)
    private String annotationType;

    @Column(name = "entities", columnDefinition = "jsonb")
    private String entities;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "DRAFT";
}
