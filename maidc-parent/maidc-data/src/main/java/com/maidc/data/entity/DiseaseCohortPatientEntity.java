package com.maidc.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "c_disease_cohort_patient", schema = "cdr")
public class DiseaseCohortPatientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cohort_id", nullable = false)
    private Long cohortId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "match_source", nullable = false, length = 16)
    private String matchSource;

    @Column(name = "matched_at", nullable = false)
    private LocalDateTime matchedAt = LocalDateTime.now();
}
