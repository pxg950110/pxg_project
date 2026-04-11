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
@Table(name = "c_imaging_finding", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_imaging_finding SET is_deleted = true WHERE id = ?")
public class ImagingFindingEntity extends BaseEntity {

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "finding_type", length = 32)
    private String findingType;

    @Column(name = "finding_desc", columnDefinition = "TEXT")
    private String findingDesc;

    @Column(name = "laterality", length = 16)
    private String laterality;

    @Column(name = "severity", length = 16)
    private String severity;

    @Column(name = "region", length = 64)
    private String region;

    @Column(name = "annotation_data", columnDefinition = "jsonb")
    private String annotationData;
}
