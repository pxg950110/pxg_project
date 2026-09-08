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
@Table(name = "c_clinical_note", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_clinical_note SET is_deleted = true WHERE id = ?")
public class ClinicalNoteEntity extends BaseEntity {

    @Column(name = "encounter_id", nullable = false)
    private Long encounterId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "note_type", nullable = false, length = 64)
    private String noteType;

    @Column(name = "note_category", length = 64)
    private String noteCategory;

    @Column(name = "title", length = 128)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "structured_data", columnDefinition = "jsonb")
    private String structuredData;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "author", length = 64)
    private String author;

    @Column(name = "note_date")
    private LocalDateTime noteDate;

    @Column(name = "sign_status", nullable = false, length = 16)
    private String signStatus = "UNSIGNED";

    @Column(name = "signed_by", length = 64)
    private String signedBy;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "urgency", nullable = false, length = 16)
    private String urgency = "NORMAL";

    @Column(name = "source", nullable = false, length = 32)
    private String source = "MANUAL";

    @Column(name = "version")
    private Integer version = 1;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "is_error", nullable = false)
    private Boolean isError = false;
}
