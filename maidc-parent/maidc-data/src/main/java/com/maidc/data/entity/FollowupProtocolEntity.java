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
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "c_followup_protocol", schema = "cdr")
@Where(clause = "is_deleted = false")
@SQLDelete(sql = "UPDATE cdr.c_followup_protocol SET is_deleted = true WHERE id = ?")
public class FollowupProtocolEntity extends BaseEntity {

    @Column(name = "cohort_id", nullable = false)
    private Long cohortId;
    @Column(name = "name", nullable = false, length = 128)
    private String name;
    @Column(name = "version", nullable = false)
    private Integer version;
    @Column(name = "status", nullable = false, length = 16)
    private String status;
    @Column(name = "stages", nullable = false, columnDefinition = "jsonb")
    private String stages;
}
