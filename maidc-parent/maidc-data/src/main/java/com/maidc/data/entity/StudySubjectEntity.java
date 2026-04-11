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

import java.time.LocalDate;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "r_study_subject", schema = "rdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE rdr.r_study_subject SET is_deleted = true WHERE id = ?")
public class StudySubjectEntity extends BaseEntity {

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "cohort_id", nullable = false)
    private Long cohortId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "subject_no", length = 32)
    private String subjectNo;

    @Column(name = "enrollment_date")
    private LocalDate enrollmentDate;

    @Column(name = "withdrawal_date")
    private LocalDate withdrawalDate;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "ENROLLED";
}
