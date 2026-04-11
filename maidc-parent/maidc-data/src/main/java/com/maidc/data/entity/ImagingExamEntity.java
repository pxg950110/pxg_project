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
@Table(name = "c_imaging_exam", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_imaging_exam SET is_deleted = true WHERE id = ?")
public class ImagingExamEntity extends BaseEntity {

    @Column(name = "encounter_id", nullable = false)
    private Long encounterId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "accession_no", length = 32)
    private String accessionNo;

    @Column(name = "exam_type", nullable = false, length = 32)
    private String examType;

    @Column(name = "body_part", length = 64)
    private String bodyPart;

    @Column(name = "study_date")
    private LocalDateTime studyDate;

    @Column(name = "modality", length = 16)
    private String modality;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "ORDERED";

    @Column(name = "report_text", columnDefinition = "TEXT")
    private String reportText;

    @Column(name = "dicom_bucket_path", length = 256)
    private String dicomBucketPath;
}
